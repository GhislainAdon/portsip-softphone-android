package com.portgo.customwidget;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.PorterDuff.Mode;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.NinePatchDrawable;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.LinearInterpolator;
import android.view.animation.RotateAnimation;
import android.view.animation.ScaleAnimation;
import android.view.animation.Transformation;

import com.portgo.R;

public class IncomgingProgressBar extends View
{
  private Drawable centerDrawable;
  private int defaultColor = -1;
  private int defaultHeight = 0;
  private int defaultWidth = 0;
  private float mAnimend = 0.0F;
  private float mAnimstart = 0.0F;
  private int mBorderColor = 0;
  private int mBorderInsideColor = 0;
  private int mBorderThickness = 0;
  private Context mContext;
  private Animation mRotationAnimation;
  private Animation mScaleAnimation;
  private Transformation mTransformation;
  private Bitmap rotateBitmap = null;
  private Drawable rotateDrawable = null;
  private Bitmap roundBitmap = null;

  public IncomgingProgressBar(Context paramContext)
  {
      this(paramContext, null);
  }

  public IncomgingProgressBar(Context paramContext, AttributeSet paramAttributeSet)
  {
      this(paramContext, paramAttributeSet, 0);
  }

  public IncomgingProgressBar(Context paramContext, AttributeSet paramAttributeSet, int paramInt)
  {
        super(paramContext, paramAttributeSet, paramInt);
        mContext = paramContext;
        setCustomAttributes(paramAttributeSet);
      if(rotateDrawable!=null&&rotateDrawable.getClass() != NinePatchDrawable.class){
          mRotationAnimation = new RotateAnimation(0.0F, -350.0F, 2, 0.5F, 2, 0.5F);
          mRotationAnimation.setInterpolator(new LinearInterpolator());
          mRotationAnimation.setRepeatCount(-1);
          mRotationAnimation.setDuration(3000L);
          mRotationAnimation.setRepeatMode(1);
      }

      if (mBorderColor != defaultColor) {
          mScaleAnimation = new ScaleAnimation(mAnimstart, mAnimend, mAnimstart, mAnimend, 1, 0.5F, 1, 0.5F);
          mScaleAnimation.setRepeatCount(-1);
          mScaleAnimation.setDuration(3000L);
          mScaleAnimation.setRepeatMode(1);
          mTransformation = new Transformation();
          mScaleAnimation.startNow();
      }
  }

  private void drawCircleBorder(Canvas paramCanvas, int paramInt1, int paramInt2)
  {
        if (mScaleAnimation != null)
        {
            mScaleAnimation.getTransformation(AnimationUtils.currentAnimationTimeMillis(), mTransformation);
            paramInt1 = (int)mTransformation.getMatrix().mapRadius(paramInt1);
        }else{
            return;
        }

        Paint localPaint = new Paint();
        localPaint.setAntiAlias(true);
        localPaint.setFilterBitmap(true);
        localPaint.setDither(true);
        localPaint.setColor(paramInt2);
//        localPaint.setAlpha(Alpha);
        localPaint.setStyle(Style.STROKE);
        localPaint.setStrokeWidth(mBorderThickness);
        paramCanvas.drawCircle(defaultWidth / 2, defaultHeight / 2, paramInt1, localPaint);
  }

  private void setCustomAttributes(AttributeSet paramAttributeSet)
  {
        TypedArray localTypedArray = mContext.obtainStyledAttributes(paramAttributeSet, R.styleable.PortCustomWidgetProgress);
        mBorderThickness = localTypedArray.getDimensionPixelSize(R.styleable.PortCustomWidgetProgress_boardwidth, 0);
        mBorderColor = localTypedArray.getColor( R.styleable.PortCustomWidgetProgress_boardcolor, defaultColor);
        mBorderInsideColor = localTypedArray.getColor(R.styleable.PortCustomWidgetProgress_boardcolor, defaultColor);
        centerDrawable = localTypedArray.getDrawable(R.styleable.PortCustomWidgetProgress_centerdrawble);
        rotateDrawable = localTypedArray.getDrawable(R.styleable.PortCustomWidgetProgress_rotationdrawble);
        mAnimstart = localTypedArray.getFloat(R.styleable.PortCustomWidgetProgress_animstart, 0.6F);
        mAnimend = localTypedArray.getFloat(R.styleable.PortCustomWidgetProgress_animend, 1.0F);
  }

    public Bitmap getCroppedRoundBitmap(Bitmap bmp, int radius) {
        Bitmap scaledSrcBmp;
        int diameter = radius * 2;

        int bmpWidth = bmp.getWidth();
        int bmpHeight = bmp.getHeight();
        int squareWidth = 0, squareHeight = 0;
        int x = 0, y = 0;
        Bitmap squareBitmap;
        if (bmpHeight > bmpWidth) {
            squareWidth = squareHeight = bmpWidth;
            x = 0;
            y = (bmpHeight - bmpWidth) / 2;

            squareBitmap = Bitmap.createBitmap(bmp, x, y, squareWidth,
                    squareHeight);
        } else if (bmpHeight < bmpWidth) {
            squareWidth = squareHeight = bmpHeight;
            x = (bmpWidth - bmpHeight) / 2;
            y = 0;
            squareBitmap = Bitmap.createBitmap(bmp, x, y, squareWidth,
                    squareHeight);
        } else {
            squareBitmap = bmp;
        }

        if (squareBitmap.getWidth() != diameter
                || squareBitmap.getHeight() != diameter) {
            scaledSrcBmp = Bitmap.createScaledBitmap(squareBitmap, diameter,
                    diameter, true);

        } else {
            scaledSrcBmp = squareBitmap;
        }
        Bitmap output = Bitmap.createBitmap(scaledSrcBmp.getWidth(),
                scaledSrcBmp.getHeight(), Config.ARGB_8888);
        Canvas canvas = new Canvas(output);

        Paint paint = new Paint();
        Rect rect = new Rect(0, 0, scaledSrcBmp.getWidth(),
                scaledSrcBmp.getHeight());

        paint.setAntiAlias(true);
        paint.setFilterBitmap(true);
        paint.setDither(true);
        canvas.drawARGB(0, 0, 0, 0);
        canvas.drawCircle(scaledSrcBmp.getWidth() / 2,
                scaledSrcBmp.getHeight() / 2, scaledSrcBmp.getWidth() / 2,
                paint);
        paint.setXfermode(new PorterDuffXfermode(Mode.SRC_IN));
        canvas.drawBitmap(scaledSrcBmp, rect, rect, paint);

        return output;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (centerDrawable == null||centerDrawable.getClass() == NinePatchDrawable.class) {
            return;
        }

        if (getWidth() == 0 || getHeight() == 0) {
            return;
        }
        measure(0, 0);


        if (defaultWidth == 0) {
            defaultWidth = getWidth();

        }
        if (defaultHeight == 0) {
            defaultHeight = getHeight();
        }

        int radius = 0;
        if (mBorderColor != defaultColor) {//
            radius = (defaultWidth < defaultHeight ? defaultWidth
                    : defaultHeight) / 2 - mBorderThickness;
            drawCircleBorder(canvas, radius + mBorderThickness / 2,
                    mBorderColor);
        } else {//
            radius = (defaultWidth < defaultHeight ? defaultWidth
                    : defaultHeight) / 2;
        }

        int j = defaultWidth / 2 - mBorderThickness;

        if(rotateBitmap == null&&rotateDrawable!=null
                &&rotateDrawable.getClass() != NinePatchDrawable.class) {
            Bitmap bitmap = ((BitmapDrawable) rotateDrawable).getBitmap().copy(Config.ARGB_8888, true);
            rotateBitmap = getCroppedRoundBitmap(bitmap, 5 + (int)(j * mAnimstart));
        }

        Bitmap localBitmap2 = ((BitmapDrawable)centerDrawable).getBitmap().copy(Config.ARGB_8888, true);
        if (mRotationAnimation != null)
        {
          if (!mRotationAnimation.hasStarted())
          {
            mRotationAnimation.initialize(localBitmap2.getWidth(), localBitmap2.getHeight(), defaultWidth, defaultHeight);
            mRotationAnimation.startNow();
          }
          mRotationAnimation.getTransformation(AnimationUtils.currentAnimationTimeMillis(), mTransformation);
          mTransformation.getMatrix().preTranslate((defaultWidth - rotateBitmap.getWidth()) / 2, (defaultHeight - rotateBitmap.getHeight()) / 2);
            canvas.drawBitmap(rotateBitmap, mTransformation.getMatrix(), null);
        }
        if (roundBitmap == null)
            roundBitmap = getCroppedRoundBitmap(localBitmap2, (int)(j * mAnimstart));
        canvas.drawBitmap(roundBitmap, defaultWidth / 2 - (int)(j * mAnimstart), defaultHeight / 2 - (int)(j * mAnimstart), null);
        invalidate();
    }

}