package com.portgo.customwidget;

import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.Transformation;

public class AnimateDrawable extends ProxyDrawable
{
  private Animation mAnimation;
  private Transformation mTransformation = new Transformation();

  public AnimateDrawable(Drawable paramDrawable)
  {
    super(paramDrawable);
  }

  public AnimateDrawable(Drawable paramDrawable, Animation paramAnimation)
  {
    super(paramDrawable);
    this.mAnimation = paramAnimation;
  }

  public void draw(Canvas paramCanvas)
  {
    Drawable localDrawable = getProxy();
    if (localDrawable != null)
    {
      int i = paramCanvas.save();
      Animation localAnimation = this.mAnimation;
      if (localAnimation != null)
      {
        localAnimation.getTransformation(AnimationUtils.currentAnimationTimeMillis(), this.mTransformation);
        paramCanvas.concat(this.mTransformation.getMatrix());
      }
      localDrawable.draw(paramCanvas);
      paramCanvas.restoreToCount(i);
    }
  }

  public Animation getAnimation()
  {
    return this.mAnimation;
  }

  public boolean hasEnded()
  {
    return (this.mAnimation == null) || (this.mAnimation.hasEnded());
  }

  public boolean hasStarted()
  {
    return (this.mAnimation != null) && (this.mAnimation.hasStarted());
  }

  public void setAnimation(Animation paramAnimation)
  {
    this.mAnimation = paramAnimation;
  }
}