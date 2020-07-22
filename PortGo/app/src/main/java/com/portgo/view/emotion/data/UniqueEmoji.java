package com.portgo.view.emotion.data;

import android.content.res.Resources;
import android.net.Uri;
import android.text.SpannableString;
import android.widget.TextView;

/**
 * Created by Administrator on 2015/11/16.
 */
public class UniqueEmoji implements Emoticon {

    private int resourceId;
    private String path;

    public UniqueEmoji(int resourseId) {
        this.resourceId = resourseId;
    }

    public UniqueEmoji(String path) {
        this.path = path;
    }

    @Override
    public int getResourceId() {
        return resourceId;
    }

    @Override
    public String getImagePath() {
        return path;
    }

    @Override
    public Uri getUri() {
        return null;
    }

    @Override
    public String getDesc() {
        return path;
    }

    @Override
    public SpannableString getSpanelText(Resources res, int textsize) {
        return null;
    }

    @Override
    public SpannableString getSpanelText(TextView tv) {
        return null;
    }
    @Override
    public EmoticonType getEmoticonType() {
        return EmoticonType.UNIQUE;
    }
}
