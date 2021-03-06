package me.devsaki.hentoid.parsers;

import android.webkit.URLUtil;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

import javax.annotation.Nullable;

import me.devsaki.hentoid.database.domains.Attribute;
import me.devsaki.hentoid.database.domains.Content;
import me.devsaki.hentoid.enums.AttributeType;
import me.devsaki.hentoid.enums.StatusContent;
import me.devsaki.hentoid.util.AttributeMap;
import timber.log.Timber;

public abstract class BaseParser implements ContentParser {

    static final int TIMEOUT = 30000; // 30 seconds

    @Nullable
    protected abstract Content parseContent(Document doc);

    protected abstract List<String> parseImages(Content content) throws Exception;

    @Nullable
    public Content parseContent(String urlString) throws IOException {
        Document doc = Jsoup.connect(urlString).timeout(TIMEOUT).get();

        Content content = parseContent(doc);

        if (content != null) {
            content.populateAuthor();
            content.setStatus(StatusContent.SAVED);
        }

        return content;
    }

    void parseAttributes(AttributeMap map, AttributeType type, Elements elements) {
        parseAttributes(map, type, elements, false);
    }

    void parseAttributes(AttributeMap map, AttributeType type, Elements elements, boolean filterCount) {
        for (Element a : elements) {
            String name = a.text();
            if (filterCount) {
                // Remove counters from metadata (e.g. "Futanari (2660)" => "Futanari")
                int bracketPos = name.lastIndexOf("(");
                if (bracketPos > 1 && ' ' == name.charAt(bracketPos - 1)) bracketPos--;
                if (bracketPos > -1) name = name.substring(0, bracketPos);
            }
            Attribute attribute = new Attribute(type, name, a.attr("href"));

            map.add(attribute);
        }
    }

    public List<String> parseImageList(Content content) {
        String readerUrl = content.getReaderUrl();
        List<String> imgUrls = Collections.emptyList();

        if (!URLUtil.isValidUrl(readerUrl)) {
            Timber.e("Invalid gallery URL : %s", readerUrl);
            return imgUrls;
        }
        Timber.d("Gallery URL: %s", readerUrl);

        try {
            imgUrls = parseImages(content);
        } catch (IOException e) {
            Timber.e(e, "I/O Error while attempting to connect to: %s", readerUrl);
        } catch (Exception e) {
            Timber.e(e, "Unexpected Error while attempting to connect to: %s", readerUrl);
        }
        Timber.d("%s", imgUrls);

        return imgUrls;
    }

}
