package me.devsaki.hentoid.collection;

import java.util.ArrayList;
import java.util.List;

import me.devsaki.hentoid.database.domains.Attribute;
import me.devsaki.hentoid.database.domains.Content;
import me.devsaki.hentoid.enums.AttributeType;
import me.devsaki.hentoid.enums.Language;
import me.devsaki.hentoid.enums.Site;
import me.devsaki.hentoid.listener.AttributeListener;
import me.devsaki.hentoid.listener.ContentListener;

public abstract class BaseCollectionAccessor implements CollectionAccessor {

    protected static final String USAGE_RECENT_BOOKS = "recentBooks";
    protected static final String USAGE_BOOK_PAGES = "bookPages";
    protected static final String USAGE_SEARCH = "search";

    // Common Utils
    protected static List<Attribute> extractByType(List<Attribute> attrs, AttributeType type)
    {
        return extractByType(attrs, new AttributeType[] { type });
    }
    protected static List<Attribute> extractByType(List<Attribute> attrs, AttributeType[] types)
    {
        List<Attribute> result = new ArrayList<>();

        for (Attribute a : attrs) {
            for (AttributeType type : types) {
                if (a.getType().equals(type)) result.add(a);
            }
        }

        return result;
    }


    @Override
    public abstract void getRecentBooks(Site site, Language language, int page, boolean showMostRecentFirst, ContentListener listener);

    @Override
    public abstract void getPages(Content content, ContentListener listener);

    @Override
    public void searchBooks(String query, List<Attribute> metadata, int booksPerPage, int orderStyle, ContentListener listener)
    {
        searchBooks(query, metadata, 1, booksPerPage, orderStyle, listener);
    }

    @Override
    public abstract void searchBooks(String query, List<Attribute> metadata, int page, int booksPerPage, int orderStyle, ContentListener listener);

    @Override
    public void getAttributeMasterData(AttributeType attr, AttributeListener listener) {
        getAttributeMasterData(attr, "", listener);
    }

    @Override
    public abstract void getAttributeMasterData(AttributeType attr, String filter, AttributeListener listener);
}
