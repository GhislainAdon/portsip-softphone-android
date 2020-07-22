package com.portgo.view;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import android.text.style.DynamicDrawableSpan;
import android.text.style.ImageSpan;
import android.util.Log;
import android.widget.EditText;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.RequestBuilder;
import com.bumptech.glide.request.target.SimpleTarget;
import com.bumptech.glide.request.transition.Transition;
import com.portgo.R;

import java.io.File;
import java.lang.reflect.Field;

import static android.util.TypedValue.COMPLEX_UNIT_PX;

public class GlideLoadImageSpan extends ImageSpan {

	private String url;
	private TextView tv;
    RequestBuilder requestBuilder;
	private boolean picShowed;
    SimpleTarget target;

	public GlideLoadImageSpan(Context context, String url, TextView tv) {
		super(context,R.color.portgo_color_white);
        requestBuilder = Glide.with(tv.getContext()).asBitmap().load(url);

        target = new ImageTarget((int)tv.getTextSize(), (int)tv.getTextSize() );
		this.tv = tv;
	}
    public GlideLoadImageSpan(Context context, File file, TextView tv) {
        super(context, R.color.portgo_color_white);
        target = new ImageTarget((int)tv.getTextSize(), (int)tv.getTextSize() );
        requestBuilder = Glide.with(tv.getContext()).asBitmap().load(file);
        this.tv = tv;
    }
    public GlideLoadImageSpan(Context context, int resId, TextView tv) {
        super(context, R.color.portgo_color_white);
        target = new ImageTarget((int)tv.getTextSize(), (int)tv.getTextSize() );
        requestBuilder = Glide.with(tv.getContext()).asBitmap().load(resId);
        this.tv = tv;
    }

    public GlideLoadImageSpan(Context context, int resId, TextView tv,int width,int height) {
        super(context,R.color.portgo_color_white);
        target = new ImageTarget((int)tv.getTextSize(), (int)tv.getTextSize() );
        requestBuilder = Glide.with(tv.getContext()).asBitmap().load(resId);
        this.tv = tv;
    }

	@Override
	public Drawable getDrawable() {
		if (!picShowed) {
            requestBuilder.into(target);
		}
		return super.getDrawable();
	}
    static boolean changersize = false;
	class ImageTarget extends SimpleTarget<Bitmap>{
        public ImageTarget(int width, int height) {
            super(width,height);
        }
        public ImageTarget() {
            super();
        }
        @Override
        public void onResourceReady(@NonNull Bitmap resource, @Nullable Transition<? super Bitmap> transition){
            Resources resources = tv.getContext().getResources();
            BitmapDrawable b = new BitmapDrawable(resources, resource);
            b.setBounds(0, 0, b.getIntrinsicWidth(), b.getIntrinsicHeight());

            Field mDrawable;
            Field mDrawableRef;
            try {
                mDrawable = ImageSpan.class.getDeclaredField("mDrawable");
                mDrawable.setAccessible(true);
                mDrawable.set(GlideLoadImageSpan.this, b);

                mDrawableRef = DynamicDrawableSpan.class.getDeclaredField("mDrawableRef");
                mDrawableRef.setAccessible(true);
                mDrawableRef.set(GlideLoadImageSpan.this, null);

                picShowed = true;
                float textsize = tv.getTextSize();
                textsize = changersize?textsize -0.00001f:textsize +0.00001f;
                changersize=!changersize;
                tv.setTextSize(COMPLEX_UNIT_PX,textsize);
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            } catch (NoSuchFieldException e) {
                e.printStackTrace();
            }
        }
    }

}