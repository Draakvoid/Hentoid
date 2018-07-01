package me.devsaki.hentoid.parsers.mikan;

import com.google.gson.annotations.Expose;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import me.devsaki.hentoid.database.domains.Attribute;
import me.devsaki.hentoid.database.domains.Content;
import me.devsaki.hentoid.enums.AttributeType;
import me.devsaki.hentoid.enums.Site;
import me.devsaki.hentoid.util.AttributeMap;

public class MikanResponse implements Serializable {

    @Expose
    public String request;
    @Expose
    public boolean nextpage;
    @Expose
    public int maxpage;
    @Expose
    public List<MikanContent> result = new ArrayList<>();


    public List<Content> toContentList()
    {
        List<Content> res = new ArrayList<>();

        for (MikanContent mikanContent : result)
        {
            res.add(mikanContent.toContent());
        }

        return res;
    }
}
