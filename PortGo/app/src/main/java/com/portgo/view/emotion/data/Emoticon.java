package com.portgo.view.emotion.data;

import android.content.Context;
import android.content.res.Resources;
import android.net.Uri;
import android.text.Spannable;
import android.text.SpannableString;
import android.widget.TextView;

/**
 * Created by Administrator on 2015/11/16.
 */
public interface Emoticon {

    int getResourceId();

    String getImagePath();

    Uri getUri();

    String getDesc();

    SpannableString getSpanelText(Resources res, int textsize);
    SpannableString getSpanelText(TextView textView);
    EmoticonType getEmoticonType();

    enum EmoticonType {
        NORMAL, UNIQUE
    }
}
