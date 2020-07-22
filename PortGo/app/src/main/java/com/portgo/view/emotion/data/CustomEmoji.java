package com.portgo.view.emotion.data;

import android.content.res.Resources;
import android.net.Uri;
import android.text.SpannableString;
import android.widget.TextView;

/**
 * Created by Administrator on 2015/11/16.
 */
public class CustomEmoji implements Emoticon {
    private String path;

    public CustomEmoji(String path) {
        this.path = path;
    }

    @Override
    public int getResourceId() {
        return 0;
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
        return EmoticonType.NORMAL;
    }
}
