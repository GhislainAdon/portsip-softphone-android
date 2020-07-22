package com.portgo.view.emotion.data;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ImageSpan;

import com.portgo.R;

import java.lang.reflect.Field;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class EmoticonTextUtil {

    public static final String PATTEN_STR = "f0[0-9]{2}|f10[0-7]";

    public static SpannableString getExpressionString(Context context, String str, int textSize) {
        SpannableString spannableString = new SpannableString(str);
        Pattern sinaPatten = Pattern.compile(PATTEN_STR, Pattern.CASE_INSENSITIVE);
        try {
            dealExpression(context, spannableString, textSize, sinaPatten, 0);
        } catch (Exception e) {
//            PortApplication.getLogUtils().e("dealExpression", e.getMessage());
        }
        return spannableString;
    }

    public static void dealExpression(Context context, SpannableString spannableString, int textSize, Pattern patten, int start) throws Exception {
        Matcher matcher = patten.matcher(spannableString);
        while (matcher.find()) {
            String key = matcher.group();
            if (matcher.start() < start) {
                continue;
            }
            Field field = R.drawable.class.getDeclaredField(key);
            int resId = field.getInt(R.drawable.class);
            if (resId != 0) {
                Drawable d = context.getResources().getDrawable(resId);
                d.setBounds(0, 0, textSize, textSize);
                ImageSpan imageSpan = new ImageSpan(d);
                int end = matcher.start() + key.length();
                spannableString.setSpan(imageSpan, matcher.start(), end,
                        Spannable.SPAN_INCLUSIVE_EXCLUSIVE);
                if (end < spannableString.length()) {
                    dealExpression(context, spannableString, textSize, patten, end);
                }
                break;
            }
        }
    }
}
