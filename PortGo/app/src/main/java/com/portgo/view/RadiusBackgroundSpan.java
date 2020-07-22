package com.portgo.view;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.text.style.ReplacementSpan;

/**
 * Created by huacai on 2017/4/26.
 */
public class RadiusBackgroundSpan extends ReplacementSpan {

    private int mSize;
    private int mColor;
    private int mRadius;

    public RadiusBackgroundSpan(int color, int radius) {
        mColor = color;
        mRadius = radius;
    }

    @Override
    public int getSize(Paint paint, CharSequence text, int start, int end, Paint.FontMetricsInt fm) {
        mSize = (int) (paint.measureText(text, start, end) + 2 * mRadius);
        return mSize;
    }

    @Override
    public void draw(Canvas canvas, CharSequence text, int start, int end, float x, int top, int y, int bottom, Paint paint) {
        int color = paint.getColor();//
        paint.setColor(mColor);//
        paint.setAntiAlias(true);//
        RectF oval = new RectF(x, y + paint.ascent(), x + mSize, y + paint.descent());
        canvas.drawRoundRect(oval, mRadius, mRadius, paint);
        paint.setColor(color);
        canvas.drawText(text, start, end, x + mRadius, y, paint);
    }
}