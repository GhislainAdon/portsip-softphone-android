package com.portgo.view;


import android.content.Context;
import android.util.AttributeSet;
import android.view.Gravity;
import android.widget.LinearLayout;

import com.portgo.R;

/**
 * Created by huacai on 2017/4/26.
 */

public class SquareRelativeLayout extends LinearLayout {
    public SquareRelativeLayout(Context context) {
        this(context,null);
    }

    public SquareRelativeLayout(Context context, AttributeSet attrs) {
        this(context, attrs,0);
    }

    public SquareRelativeLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        setBackgroundResource(R.color.portgo_color_blue);
        setGravity(Gravity.CENTER);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int size1 = getDefaultSize(0,widthMeasureSpec);
        int size2 = getDefaultSize(0,heightMeasureSpec);

        int childWidthSize = getMeasuredWidth();
        int childHeightSize = getMeasuredHeight();
        int size = childWidthSize>childHeightSize?childHeightSize:childWidthSize;
        setMeasuredDimension(size,size);
        heightMeasureSpec = widthMeasureSpec = MeasureSpec.makeMeasureSpec(
                size, MeasureSpec.EXACTLY);
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        setMeasuredDimension(size1,size2);
    }
}