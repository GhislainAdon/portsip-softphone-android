package com.portgo.customwidget;

import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.PixelFormat;
import android.graphics.drawable.Drawable;

public class ProxyDrawable extends Drawable
{
  private boolean mMutated;
  private Drawable mProxy;

  public ProxyDrawable(Drawable paramDrawable)
  {
    this.mProxy = paramDrawable;
  }

  public void draw(Canvas paramCanvas)
  {
    if (this.mProxy != null)
      this.mProxy.draw(paramCanvas);
  }

  public int getIntrinsicHeight()
  {
    if (this.mProxy != null)
      return this.mProxy.getIntrinsicHeight();
    return -1;
  }

  public int getIntrinsicWidth()
  {
    if (this.mProxy != null)
      return this.mProxy.getIntrinsicWidth();
    return -1;
  }

  public int getOpacity()
  {
    if (this.mProxy != null)
      return this.mProxy.getOpacity();
    return PixelFormat.TRANSPARENT;
  }

  public Drawable getProxy()
  {
    return this.mProxy;
  }

  public Drawable mutate()
  {
    if ((this.mProxy != null) && (!this.mMutated) && (super.mutate() == this))
    {
      this.mProxy.mutate();
      this.mMutated = true;
    }
    return this;
  }

  public void setAlpha(int paramInt)
  {
    if (this.mProxy != null)
      this.mProxy.setAlpha(paramInt);
  }

  public void setColorFilter(ColorFilter paramColorFilter)
  {
    if (this.mProxy != null)
      this.mProxy.setColorFilter(paramColorFilter);
  }

  public void setDither(boolean paramBoolean)
  {
    if (this.mProxy != null)
      this.mProxy.setDither(paramBoolean);
  }

  public void setFilterBitmap(boolean paramBoolean)
  {
    if (this.mProxy != null)
      this.mProxy.setFilterBitmap(paramBoolean);
  }

  public void setProxy(Drawable paramDrawable)
  {
    if (paramDrawable != this)
      this.mProxy = paramDrawable;
  }
}