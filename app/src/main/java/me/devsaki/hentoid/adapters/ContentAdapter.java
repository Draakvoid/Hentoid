package me.devsaki.hentoid.adapters;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.support.annotation.ColorInt;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.util.SortedList;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.util.SortedListAdapterCallback;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.annimon.stream.Collectors;
import com.annimon.stream.Objects;
import com.annimon.stream.Stream;
import com.annimon.stream.function.Consumer;
import com.annimon.stream.function.IntConsumer;
import com.bumptech.glide.Glide;
import com.bumptech.glide.RequestManager;
import com.bumptech.glide.request.RequestOptions;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import me.devsaki.hentoid.R;
import me.devsaki.hentoid.abstracts.DownloadsFragment;
import me.devsaki.hentoid.collection.CollectionAccessor;
import me.devsaki.hentoid.database.HentoidDB;
import me.devsaki.hentoid.database.domains.Attribute;
import me.devsaki.hentoid.database.domains.Content;
import me.devsaki.hentoid.database.domains.ImageFile;
import me.devsaki.hentoid.enums.AttributeType;
import me.devsaki.hentoid.enums.StatusContent;
import me.devsaki.hentoid.listener.ContentListener;
import me.devsaki.hentoid.listener.ItemClickListener;
import me.devsaki.hentoid.listener.ItemClickListener.ItemSelectListener;
import me.devsaki.hentoid.services.ContentQueueManager;
import me.devsaki.hentoid.ui.BlinkAnimation;
import me.devsaki.hentoid.util.FileHelper;
import me.devsaki.hentoid.util.Helper;
import timber.log.Timber;

/**
 * Created by avluis on 04/23/2016.
 * RecyclerView based Content Adapter
 * <p>
 * TODO issue: This class has too many responsibilities. The ideal is to only handle view inflation and data binding.
 * It is best to delegate behavior by using Observer pattern.
 * Data should be given to adapters but adapters must only view the data and not make changes.
 * Additional responsibilities include:
 * - item selection management such as {@link #toggleSelection(int)}, {@link #clearSelections()}, etc.
 * - data manipulation such as {@link #add(List)}, {@link #remove(Content)}, etc.
 * - download management such as {@link #downloadContent(Content)} & {@link #downloadAgain(Content)}
 * - item action handling behavior such as {@link #shareContent(Content)}, {@link #archiveContent(Content)}, {@link #deleteContent(Content)}, etc.
 * <p>
 * TODO issue: {@link #onBindViewHolder(ContentHolder, int)} is too inefficient.
 * It is best to do most work in {@link #onCreateViewHolder(ViewGroup, int)} such as setting click listeners.
 * Parsing data should not be done in onBindViewHolder because it may happen several times for one item.
 * A business object modeling parsed data can be created once and used several times to avoid parsing during binding.
 * Conditional handling should not be done during binding. Instead, different viewtypes and viewholders may be used.
 */
public class ContentAdapter extends RecyclerView.Adapter<ContentHolder> implements ContentListener {

    private static final int VISIBLE_THRESHOLD = 10;

    private final SortedList<Content> mSortedList = new SortedList<>(Content.class, new SortedListCallback(this));
    private final Context context;
    private final ItemSelectListener itemSelectListener;
    private final IntConsumer onContentRemovedListener;
    private final Runnable onContentsClearedListener;
    private final Consumer<Content> onItemSourceClickListener;
    private final CollectionAccessor collectionAccessor;
    private final int displayMode;
    private final RequestOptions glideRequestOptions;
    private RecyclerView libraryView; // Kept as reference for querying by Content through ID
    private Runnable onScrollToEndListener;
    private Comparator<Content> sortComparator;

    private ContentAdapter(Builder builder) {
        context = builder.context;
        itemSelectListener = builder.itemSelectListener;
        onContentRemovedListener = builder.onContentRemovedListener;
        onContentsClearedListener = builder.onContentsClearedListener;
        onItemSourceClickListener = builder.onItemSourceClickListener;
        collectionAccessor = builder.collectionAccessor;
        sortComparator = builder.sortComparator;
        displayMode = builder.displayMode;
        glideRequestOptions = new RequestOptions()
                .centerInside()
                .error(R.drawable.ic_placeholder);
        setHasStableIds(true);
    }

    public void setSortComparator(Comparator<Content> comparator) {
        sortComparator = comparator;
    }

    public void setOnScrollToEndListener(Runnable listener) {
        this.onScrollToEndListener = listener;
    }

    private void toggleSelection(int pos) {
        Content c = getItemAt(pos);

        if (c != null) {
            c.setSelected(!c.isSelected());
            notifyItemChanged(pos);
        }
    }

    public void clearSelections() {
        for (int i = 0; i < mSortedList.size(); i++) {
            mSortedList.get(i).setSelected(false);
            notifyDataSetChanged();
        }
    }

    private int getSelectedItemsCount() {
        int result = 0;
        for (int i = 0; i < mSortedList.size(); i++) {
            if (mSortedList.get(i).isSelected()) result++;
        }
        return result;
    }

    private List<Content> getSelectedContents() {
        List<Content> selectionList = new ArrayList<>();

        for (int i = 0; i < mSortedList.size(); i++) {
            if (mSortedList.get(i).isSelected()) selectionList.add(mSortedList.get(i));
            Timber.d("Added: %s to list.", mSortedList.get(i).getTitle());
        }

        return selectionList;
    }

    private boolean isSelectedAt(int pos) {
        Content c = getItemAt(pos);
        return null != c && c.isSelected();
    }

    @NonNull
    @Override
    public ContentHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        View view = inflater.inflate(R.layout.item_download, parent, false);
        return new ContentHolder(view);
    }

    @Override
    public void onAttachedToRecyclerView(@NonNull RecyclerView recyclerView) {
        libraryView = recyclerView;
    }

    /**
     * Using application context when glide loading to avoid crashes when activity is destroyed
     * https://stackoverflow.com/questions/39093730/you-cannot-start-a-load-for-a-destroyed-activity-in-relativelayout-image-using-g
     * <p>
     * TODO: binding should be done by the ViewHolder
     */
    @Override
    public void onBindViewHolder(@NonNull ContentHolder holder, final int pos) {
        if (pos == getItemCount() - VISIBLE_THRESHOLD && onScrollToEndListener != null) {
            onScrollToEndListener.run();
        }

        Content content = mSortedList.get(pos);

        holder.itemView.setSelected(content.isSelected());

        // TODO: a different ViewHolder with a different layout should be used for selected and unselected items
        if (content.isSelected()) {
            holder.itemLayout.setVisibility(View.GONE);
            holder.minimalLayout.setVisibility(View.VISIBLE);
            holder.tvTitle2.setText(extractTitle(content));

            Glide.with(context.getApplicationContext())
                    .load(FileHelper.getThumb(content))
                    .apply(glideRequestOptions)
                    .into(holder.ivCover2);
        } else {
            holder.itemLayout.setVisibility(View.VISIBLE);
            holder.minimalLayout.setVisibility(View.GONE);
            holder.tvTitle.setText(extractTitle(content));

            Glide.with(context.getApplicationContext())
                    .load(FileHelper.getThumb(content))
                    .apply(glideRequestOptions)
                    .into(holder.ivCover);

            holder.ivNew.setVisibility((0 == content.getReads()) ? View.VISIBLE : View.GONE);
            holder.tvSeries.setText(extractSeries(content));
            holder.tvArtist.setText(extractArtist(content));
            holder.tvTags.setText(extractTags(content));
        }

        attachSiteButtonIcon(holder, content);
        attachButtons(holder, content);
        attachOnClickListeners(holder, content, pos);
    }

    @Override
    public void onViewRecycled(@NonNull ContentHolder holder) {
        RequestManager requestManager = Glide.with(context.getApplicationContext());
        requestManager.clear(holder.ivCover);
        requestManager.clear(holder.ivCover2);
    }

    private CharSequence extractTitle(Content content) {
        if (content.getTitle() == null) {
            return context.getText(R.string.work_untitled);
        } else {
            return content.getTitle();
        }
    }

    private CharSequence extractSeries(Content content) {
        String series;
        List<Attribute> seriesAttributes = content.getAttributes().get(AttributeType.SERIE);
        if (seriesAttributes == null) {
            series = context.getResources().getString(R.string.work_untitled);
        } else {
            series = Stream.of(seriesAttributes)
                    .map(Attribute::getName)
                    .collect(Collectors.joining(", "));
        }
        String templateSeries = context.getResources().getString(R.string.work_series);
        return Helper.fromHtml(templateSeries.replace("@series@", series));
    }

    private CharSequence extractArtist(Content content) {
        String artists;
        List<Attribute> attributes = new LinkedList<>();
        List<Attribute> artistAttributes = content.getAttributes().get(AttributeType.ARTIST);
        if (artistAttributes != null) attributes.addAll(artistAttributes);
        List<Attribute> circleAttributes = content.getAttributes().get(AttributeType.CIRCLE);
        if (circleAttributes != null) attributes.addAll(circleAttributes);

        if (attributes.isEmpty()) {
            artists = context.getResources().getString(R.string.work_untitled);
        } else {
            artists = Stream.of(attributes)
                    .map(Attribute::getName)
                    .collect(Collectors.joining(", "));
        }

        String templateArtist = context.getResources().getString(R.string.work_artist);
        return Helper.fromHtml(templateArtist.replace("@artist@", artists));
    }

    private CharSequence extractTags(Content content) {
        List<Attribute> tagsAttributes = content.getAttributes().get(AttributeType.TAG);
        if (tagsAttributes == null) {
            return "";
        } else {
            CharSequence tags = Stream.of(tagsAttributes)
                    .map(Attribute::getName)
                    .filter(Objects::nonNull)
                    .collect(Collectors.joining(", "));
            String templateTags = context.getResources().getString(R.string.work_tags);
            return Helper.fromHtml(templateTags.replace("@tag@", tags));
        }
    }

    @ColorInt
    private int extractSiteButtonBg(Content content) {
        int bgColorRes;
        switch (content.getStatus()) {
            case DOWNLOADED:
                bgColorRes = R.color.card_item_src_normal;
                break;
            case MIGRATED:
                bgColorRes = R.color.card_item_src_migrated;
                break;
            case ONLINE:
                bgColorRes = R.color.card_item_src_other;
                break;
            default:
                bgColorRes = R.color.card_item_src_other;
                break;
        }
        return ContextCompat.getColor(context, bgColorRes);
    }

    private void attachSiteButtonIcon(ContentHolder holder, final Content content) {
        if (content.getSite() != null) {
            holder.ivSite.setImageResource(content.getSite().getIco());
            holder.ivSite.setOnClickListener(v -> onItemSourceClickListener.accept(content));
        } else {
            holder.ivSite.setImageResource(R.drawable.ic_stat_hentoid);
        }
        holder.ivSite.setBackgroundColor(extractSiteButtonBg(content));
    }

    private void attachButtons(ContentHolder holder, final Content content) {
        // Set source color
        if (content.getStatus() != null) {
            holder.ivFavourite.setVisibility((DownloadsFragment.MODE_LIBRARY == displayMode) ? View.VISIBLE : View.GONE);
            holder.ivError.setVisibility((DownloadsFragment.MODE_LIBRARY == displayMode) ? View.VISIBLE : View.GONE);
            holder.ivDownload.setVisibility((DownloadsFragment.MODE_MIKAN == displayMode) ? View.VISIBLE : View.GONE);

            StatusContent status = content.getStatus();
            if (DownloadsFragment.MODE_LIBRARY == displayMode) {
                // Favourite toggle
                if (content.isFavourite()) {
                    holder.ivFavourite.setImageResource(R.drawable.ic_fav_full);
                } else {
                    holder.ivFavourite.setImageResource(R.drawable.ic_fav_empty);
                }
                holder.ivFavourite.setOnClickListener(v -> {
                    if (getSelectedItemsCount() >= 1) {
                        clearSelections();
                        itemSelectListener.onItemClear(0);
                    }
                    if (content.isFavourite()) {
                        holder.ivFavourite.setImageResource(R.drawable.ic_fav_empty);
                    } else {
                        holder.ivFavourite.setImageResource(R.drawable.ic_fav_full);
                    }
                    favToggleItem(content);
                });

                // Error icon
                if (status == StatusContent.ERROR) {
                    holder.ivError.setVisibility(View.VISIBLE);
                    holder.ivError.setOnClickListener(v -> {
                        if (getSelectedItemsCount() >= 1) {
                            clearSelections();
                            itemSelectListener.onItemClear(0);
                        }
                        downloadAgain(content);
                    });
                } else {
                    holder.ivError.setVisibility(View.GONE);
                }
            } else { // Mikan mode

                // "Available online" icon
                if (status == StatusContent.ONLINE) {
                    holder.ivDownload.setImageResource(R.drawable.ic_action_download);
                    holder.ivDownload.setOnClickListener(v -> tryDownloadPages(content));
                }
                // "In queue" icon
                else if (status == StatusContent.DOWNLOADING || status == StatusContent.PAUSED) {
                    holder.ivDownload.setImageResource(R.drawable.ic_action_download);
                    holder.ivDownload.startAnimation(new BlinkAnimation(500, 100));
                    holder.ivDownload.setOnClickListener(v -> Helper.viewQueue(context));
                }
                // "In library" icon
                else if (status == StatusContent.DOWNLOADED || status == StatusContent.MIGRATED || status == StatusContent.ERROR) {
                    holder.ivDownload.setImageResource(R.drawable.ic_action_play);
                    holder.ivDownload.setOnClickListener(v -> FileHelper.openContent(context, content));
                }
            }

        } else {
            holder.ivSite.setVisibility(View.GONE);
        }
    }

    private void tryDownloadPages(Content content) {
        ContentHolder holder = holderByContent(content);
        if (holder != null) {
            holder.ivDownload.startAnimation(new BlinkAnimation(500, 100));
            holder.ivDownload.setOnClickListener(w -> Helper.viewQueue(context));
            collectionAccessor.getPages(content, this);
        }
    }

    // TODO : implement preview gallery for Mikan mode
    private void attachOnClickListeners(final ContentHolder holder, Content content, int pos) {
        // Libray mode only
        if (DownloadsFragment.MODE_LIBRARY != displayMode) return;

        // Simple click = open book
        holder.itemView.setOnClickListener(new ItemClickListener(context, content, pos, itemSelectListener) {

            @Override
            public void onClick(View v) {
                if (getSelectedItemsCount() > 0) { // Selection mode is on
                    int itemPos = holder.getLayoutPosition();
                    toggleSelection(itemPos);
                    setSelected(isSelectedAt(pos), getSelectedItemsCount());
                    onLongClick(v);
                } else {
                    clearSelections();
                    setSelected(false, 0);

                    super.onClick(v);

                    if (sortComparator.equals(Content.READ_DATE_INV_COMPARATOR)
                            || sortComparator.equals(Content.READS_ORDER_COMPARATOR)
                            || sortComparator.equals(Content.READS_ORDER_INV_COMPARATOR))
                        mSortedList.recalculatePositionOfItemAt(pos); // Reading the book has an effect on its position
                }
            }
        });

        // Long click = select item
        holder.itemView.setOnLongClickListener(new ItemClickListener(context, content, pos, itemSelectListener) {

            @Override
            public boolean onLongClick(View v) {
                int itemPos = holder.getLayoutPosition();
                toggleSelection(itemPos);
                setSelected(isSelectedAt(pos), getSelectedItemsCount());

                super.onLongClick(v);

                return true;
            }
        });
    }

    private void downloadAgain(final Content item) {
        int images;
        int imgErrors = 0;

        images = item.getImageFiles().size();

        for (ImageFile imgFile : item.getImageFiles()) {
            if (imgFile.getStatus() == StatusContent.ERROR) {
                imgErrors++;
            }
        }

        String message = context.getString(R.string.download_again_dialog_message).replace("@clean", images - imgErrors + "").replace("@error", imgErrors + "").replace("@total", images + "");
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(R.string.download_again_dialog_title)
                .setMessage(message)
                .setPositiveButton(android.R.string.yes,
                        (dialog, which) -> {
                            downloadContent(item);
                            remove(item);
                        })
                .setNegativeButton(android.R.string.no, null)
                .create().show();
    }

    private void downloadContent(Content item) {
        HentoidDB db = HentoidDB.getInstance(context);

        item.setDownloadDate(new Date().getTime());

        if (StatusContent.ONLINE == item.getStatus()) {
            item.setStatus(StatusContent.DOWNLOADING);
            for (ImageFile im : item.getImageFiles()) im.setStatus(StatusContent.SAVED);

            db.insertContent(item);
        } else {
            item.setStatus(StatusContent.DOWNLOADING);
            db.updateContentStatus(item);
        }

        List<Pair<Integer, Integer>> queue = db.selectQueue();
        int lastIndex = 1;
        if (queue.size() > 0) {
            lastIndex = queue.get(queue.size() - 1).second + 1;
        }
        db.insertQueue(item.getId(), lastIndex);

        ContentQueueManager.getInstance().resumeQueue(context);

        Helper.toast(context, R.string.add_to_queue);
    }

    private void shareContent(final Content item) {
        String url = item.getGalleryUrl();

        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_SEND);
        intent.setDataAndType(Uri.parse(url), "text/plain");
        intent.putExtra(Intent.EXTRA_SUBJECT, item.getTitle());
        intent.putExtra(Intent.EXTRA_TEXT, url);

        context.startActivity(Intent.createChooser(intent, context.getString(R.string.send_to)));
    }

    private void archiveContent(final Content item) {
        Helper.toast(R.string.packaging_content);
        FileHelper.archiveContent(context, item);
    }

    private void deleteContent(final Content item) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setMessage(R.string.ask_delete)
                .setPositiveButton(android.R.string.yes,
                        (dialog, which) -> {
                            clearSelections();
                            deleteItem(item);
                        })
                .setNegativeButton(android.R.string.no,
                        (dialog, which) -> {
                            clearSelections();
                            itemSelectListener.onItemClear(0);
                        })
                .create().show();
    }

    private void deleteContents(final List<Content> items) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setMessage(R.string.ask_delete_multiple)
                .setPositiveButton(android.R.string.yes,
                        (dialog, which) -> {
                            clearSelections();
                            deleteItems(items);
                        })
                .setNegativeButton(android.R.string.no,
                        (dialog, which) -> {
                            clearSelections();
                            itemSelectListener.onItemClear(0);
                        })
                .create().show();
    }

    private void favToggleItem(Content item) {
        item.setFavourite(!item.isFavourite());

        // Persist in it DB
        final HentoidDB db = HentoidDB.getInstance(context);
        db.updateContentFavourite(item);
    }

    /**
     * Change the state of the item relative to the given content to "downloaded" NB : Specific to
     * Mikan screen
     *
     * @param content content that has been downloaded
     */
    public void switchStateToDownloaded(Content content) {
        ContentHolder holder = holderByContent(content);

        if (holder != null) {
            holder.ivDownload.setImageResource(R.drawable.ic_action_play);
            holder.ivDownload.clearAnimation();
            holder.ivDownload.setOnClickListener(v -> FileHelper.openContent(context, content));
        }
    }

    @Nullable
    private ContentHolder holderByContent(Content content) {
        return (ContentHolder) libraryView.findViewHolderForItemId(content.getId());
    }

    @Override
    public long getItemId(int position) {
        return mSortedList.get(position).getId();
    }

    @Override
    public int getItemCount() {
        return mSortedList.size();
    }

    @Nullable
    private Content getItemAt(int pos) {
        if (mSortedList.size() <= pos) return null;
        else return mSortedList.get(pos);
    }

    public void sharedSelectedItems() {
        int itemCount = getSelectedItemsCount();
        if (itemCount > 0) {
            if (itemCount == 1) {
                Timber.d("Preparing to share selected item...");

                List<Content> items;
                items = getSelectedContents();

                if (!items.isEmpty()) {
                    shareContent(items.get(0));
                } else {
                    itemSelectListener.onItemClear(0);
                    Timber.d("Nothing to share!!");
                }
            } else {
                // TODO: Implement multi-item share
                Timber.d("How even?");
                Helper.toast("Not yet implemented!!");
            }
        } else {
            itemSelectListener.onItemClear(0);
            Timber.d("No items to share!!");
        }
    }

    public void purgeSelectedItems() {
        int itemCount = getSelectedItemsCount();
        if (itemCount > 0) {
            if (itemCount == 1) {
                Timber.d("Preparing to delete selected item...");

                List<Content> items;
                items = getSelectedContents();

                if (!items.isEmpty()) {
                    deleteContent(items.get(0));
                } else {
                    itemSelectListener.onItemClear(0);
                    Timber.d("Nothing to delete!!");
                }
            } else {
                Timber.d("Preparing to delete selected items...");

                List<Content> items;
                items = getSelectedContents();

                if (!items.isEmpty()) {
                    deleteContents(items);
                } else {
                    itemSelectListener.onItemClear(0);
                    Timber.d("No items to delete!!");
                }
            }
        } else {
            itemSelectListener.onItemClear(0);
            Timber.d("No items to delete!!");
        }
    }

    public void archiveSelectedItems() {
        int itemCount = getSelectedItemsCount();
        if (itemCount > 0) {
            if (itemCount == 1) {
                Timber.d("Preparing to archive selected item...");

                List<Content> items;
                items = getSelectedContents();

                if (!items.isEmpty()) {
                    archiveContent(items.get(0));
                } else {
                    itemSelectListener.onItemClear(0);
                    Timber.d("Nothing to archive!!");
                }
            } else {
                // TODO: Implement multi-item archival
                Timber.d("How even?");
                Helper.toast("Not yet implemented!!");
            }
        } else {
            itemSelectListener.onItemClear(0);
            Timber.d("No items to archive!!");
        }
    }

    private void deleteItem(final Content item) {
        remove(item);

        final HentoidDB db = HentoidDB.getInstance(context);
        AsyncTask.execute(() -> {
            FileHelper.removeContent(item);
            db.deleteContent(item);
            Timber.d("Removed item: %s from db and file system.", item.getTitle());
        });

        Helper.toast(context, context.getString(R.string.deleted).replace("@content", item.getTitle()));
    }

    private void deleteItems(final List<Content> contents) {
        mSortedList.beginBatchedUpdates();
        for (Content content : contents) {
            mSortedList.remove(content);
        }
        onContentRemovedListener.accept(contents.size());
        mSortedList.endBatchedUpdates();
        itemSelectListener.onItemClear(0);

        final HentoidDB db = HentoidDB.getInstance(context);

        AsyncTask.execute(() -> {
            for (Content item : contents) {
                FileHelper.removeContent(item);
                db.deleteContent(item);
                Timber.d("Removed item: %s from db and file system.", item.getTitle());
            }
        });

        Helper.toast(context, "Selected items have been deleted.");
    }

    private void remove(Content content) {
        mSortedList.remove(content);
        if (0 == mSortedList.size()) {
            if (onContentsClearedListener != null)
                onContentsClearedListener.run();
        } else {
            if (onContentRemovedListener != null)
                onContentRemovedListener.accept(1);
        }
        if (itemSelectListener != null) itemSelectListener.onItemClear(0);
    }

    public void removeAll() {
        replaceAll(new ArrayList<>());
        onContentsClearedListener.run();
    }

    public void replaceAll(List<Content> contents) {
        mSortedList.beginBatchedUpdates();
        for (int i = mSortedList.size() - 1; i >= 0; i--) {
            final Content content = mSortedList.get(i);
            if (!contents.contains(content)) {
                mSortedList.remove(content);
            } else {
                contents.remove(content);
            }
        }
        mSortedList.addAll(contents);
        mSortedList.endBatchedUpdates();
    }

    public void add(List<Content> contents) {
        mSortedList.beginBatchedUpdates();
        mSortedList.addAll(contents);
        mSortedList.endBatchedUpdates();
    }

    // ContentListener implementation
    @Override
    public void onContentReady(List<Content> results, int totalSelectedContent, int totalContent) { // Listener for pages retrieval in Mikan mode
        if (1 == results.size()) // 1 content with pages
        {
            downloadContent(results.get(0));
        }
    }

    @Override
    public void onContentFailed(Content content, String message) {
        Timber.w(message);
        Snackbar snackbar = Snackbar.make(libraryView, message, Snackbar.LENGTH_LONG);

        if (content != null) {
            ContentHolder holder = holderByContent(content);
            if (holder != null) {
                holder.ivDownload.clearAnimation();
                holder.ivDownload.setOnClickListener(v -> tryDownloadPages(content));
            }
            snackbar.setAction("RETRY", v -> tryDownloadPages(content));
        }
        snackbar.show();
    }

    private class SortedListCallback extends SortedListAdapterCallback<Content> {

        private SortedListCallback(RecyclerView.Adapter adapter) {
            super(adapter);
        }

        @Override
        public int compare(Content a, Content b) {
            return sortComparator.compare(a, b);
        }

        @Override
        public boolean areContentsTheSame(Content oldItem, Content newItem) {
            return oldItem.equals(newItem);
        }

        @Override
        public boolean areItemsTheSame(Content item1, Content item2) {
            return item1.getId() == item2.getId();
        }
    }

    public static class Builder {
        private Context context;
        private ItemSelectListener itemSelectListener;
        private IntConsumer onContentRemovedListener;
        private Runnable onContentsClearedListener;
        private Consumer<Content> onItemSourceClickListener;
        private CollectionAccessor collectionAccessor;
        private Comparator<Content> sortComparator;
        private int displayMode;

        public Builder setContext(Context context) {
            this.context = context;
            return this;
        }

        public Builder setItemSelectListener(ItemSelectListener itemSelectListener) {
            this.itemSelectListener = itemSelectListener;
            return this;
        }

        public Builder setCollectionAccessor(CollectionAccessor collectionAccessor) {
            this.collectionAccessor = collectionAccessor;
            return this;
        }

        public Builder setSortComparator(Comparator<Content> sortComparator) {
            this.sortComparator = sortComparator;
            return this;
        }

        public Builder setDisplayMode(int displayMode) {
            this.displayMode = displayMode;
            return this;
        }

        public Builder setOnContentRemovedListener(IntConsumer onContentRemovedListener) {
            this.onContentRemovedListener = onContentRemovedListener;
            return this;
        }

        public Builder setOnContentsClearedListener(Runnable onContentsClearedListener) {
            this.onContentsClearedListener = onContentsClearedListener;
            return this;
        }

        public Builder setOnItemSourceClickListener(Consumer<Content> onItemSourceClickListener) {
            this.onItemSourceClickListener = onItemSourceClickListener;
            return this;
        }

        public ContentAdapter build() {
            return new ContentAdapter(this);
        }
    }
}
