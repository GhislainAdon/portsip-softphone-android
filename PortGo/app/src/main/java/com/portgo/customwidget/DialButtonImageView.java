package com.portgo.customwidget;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.widget.ImageButton;

import com.portgo.R;

public class DialButtonImageView extends ImageButton
{
  Drawable normalDrawable;
  Drawable pressDrawable;

  public DialButtonImageView(Context context)
  {
    this(context,null);
  }

  public DialButtonImageView(Context context, AttributeSet paramAttributeSet)
  {
     this(context,paramAttributeSet, 0);
  }

  public DialButtonImageView(Context paramContext, AttributeSet paramAttributeSet, int paramInt)
  {
        super(paramContext, paramAttributeSet, paramInt);
        init(paramAttributeSet, paramInt);
  }

  private void init(AttributeSet paramAttributeSet, int paramInt)
  {
        TypedArray localTypedArray = getContext().obtainStyledAttributes(paramAttributeSet, R.styleable.PortCustomWidgetDialBtn, paramInt, 0);
        this.pressDrawable = localTypedArray.getDrawable(R.styleable.PortCustomWidgetDialBtn_focusDrawble);
        localTypedArray.recycle();
        this.normalDrawable = getDrawable();
  }

  public boolean onTouchEvent(MotionEvent paramMotionEvent)
  {
    if (paramMotionEvent.getAction() == MotionEvent.ACTION_HOVER_ENTER) {
        setImageDrawable(this.pressDrawable);
    }
    else
    if (paramMotionEvent.getAction() == MotionEvent.ACTION_HOVER_EXIT){
        setImageDrawable(this.normalDrawable);
    }
      return super.onTouchEvent(paramMotionEvent);
  }
}