package com.portgo.view.emotion.data;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ImageSpan;
import android.util.Log;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.RequestBuilder;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.load.resource.gif.GifDrawable;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.target.SimpleTarget;
import com.bumptech.glide.request.target.Target;
import com.bumptech.glide.request.transition.Transition;
import com.portgo.view.GlideLoadImageSpan;

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * Created by Administrator on 2015/11/15.
 */
public class Emoji implements Emoticon {
    private int drawableResId;
    private int decInt;
    private String dec="";
    private Context mContext;
    public Emoji(Context context,int drawableResId, int decInt) {
        this.drawableResId = drawableResId;
        this.decInt = decInt;
        mContext = context;
    }

    public Emoji(Context context,int drawableResId, String dec) {
        this.drawableResId = drawableResId;
        this.dec = dec;
        this.decInt =decInt;
        mContext = context;
    }
    @Override
    public int getResourceId() {
        return drawableResId;
    }

    @Override
    public String getImagePath() {
        return null;
    }

    @Override
    public Uri getUri() {
        return null;
    }

    @Override
    public String getDesc() {
        if(decInt==0){
            return "[:"+dec+"]";
        }
        return new String(Character.toChars(decInt));
    }


	@Override
    public SpannableString getSpanelText(Resources resources, int textSize) {
//        Drawable d = resources.getDrawable(drawableResId);
        InputStream is = resources.openRawResource(drawableResId);
        Drawable d=  new BitmapDrawable(is);

        d.setBounds(0, 0, textSize, textSize);
        SpannableString ss = new SpannableString(getDesc());
        ImageSpan span = new ImageSpan(d, ImageSpan.ALIGN_BOTTOM);
        ss.setSpan(span, 0, getDesc().length(),
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        return ss;
    }


    @Override
    public SpannableString getSpanelText( TextView textView){

        final  SpannableString ss = new SpannableString(getDesc());
        GlideLoadImageSpan imageSpan =  new GlideLoadImageSpan(mContext.getApplicationContext(),drawableResId,textView);
        ss.setSpan(imageSpan, 0, getDesc().length(),
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        return ss;
    }

    @Override
    public EmoticonType getEmoticonType() {
        return EmoticonType.NORMAL;
    }
}
