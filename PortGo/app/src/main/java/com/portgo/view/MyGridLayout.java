package com.portgo.view;

import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.res.TypedArray;
import androidx.annotation.Nullable;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;

import com.portgo.R;

/**
 * Created by huacai on 2017/4/26.
 */

public class MyGridLayout extends ViewGroup{

    int mWidth = 0;
    int mHeight = 0;
    int clumn = 2;
    int row = 2;
    int widthWeight = 1;
    int heightWeight = 1;
    boolean widthAsStandard =true;
	boolean matchParent = false;
    public enum LAYOUTSTARD{
        LAND,
        PORT
    }
    public MyGridLayout(Context context) {
        super(context);
    }

    public MyGridLayout(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public MyGridLayout(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        TypedArray localTypedArray = getContext().obtainStyledAttributes(attrs, R.styleable.GridViewStyle, defStyleAttr, 0);
        clumn = localTypedArray.getInt(R.styleable.GridViewStyle_clumn,clumn);
        row = localTypedArray.getInt(R.styleable.GridViewStyle_row,row);
        widthWeight = localTypedArray.getInt(R.styleable.GridViewStyle_width_Weight,widthWeight);
        heightWeight = localTypedArray.getInt(R.styleable.GridViewStyle_height_Weight,heightWeight);
        widthAsStandard= localTypedArray.getBoolean(R.styleable.GridViewStyle_horizontal_as_standard,widthAsStandard);
        matchParent= localTypedArray.getBoolean(R.styleable.GridViewStyle_full_Screen,matchParent);

        localTypedArray.recycle();
    }

	public void setFullScreen(boolean full ){
        matchParent = full;
    }

    public void setLayoutStandard(LAYOUTSTARD standard ){
        if(standard==LAYOUTSTARD.PORT){
            widthAsStandard=true;
        }else {
            widthAsStandard=false;
        }
    }
    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        LayoutParams pramas = getLayoutParams();

        mWidth = right - left;
        mHeight = bottom - top;
        int clumnWidth = mWidth/clumn;
        int rowHeight = mHeight/row;
        int clunmSpace = 0;
        int rowSpace  = 0;
        int actualWidth = clumnWidth;
        int actualHeight = rowHeight;
		if(!matchParent){
            if(widthAsStandard) {//
                actualHeight = actualWidth*heightWeight/widthWeight;
            }else{
                actualWidth = actualHeight*widthWeight/heightWeight;
            }

            clunmSpace = (clumnWidth-actualWidth)/2;
            rowSpace = (rowHeight-actualHeight)/2;
		}
        int childCount = getChildCount();

        for(int i=0;i<row;i++) {
            int visiableChildren = 0;
            for(int j=0;j<clumn;j++) {
                int childIndex =  i*clumn+j;
                View child = getChildAt(childIndex);
                if(child.getVisibility()!=GONE){
                    visiableChildren++;
                }
            }

            int rowclumnWidth =clumnWidth;
            if(visiableChildren!=clumn){
                rowclumnWidth=clumnWidth+(clumn-visiableChildren)*clumnWidth/visiableChildren;
            }

            int skip=0;
            for(int j=0;j<clumn;j++) {
                int childIndex =  i*clumn+j;
                if(childIndex>=childCount)//
                    return;

                View child = getChildAt(childIndex);
                if(child.getVisibility()!=GONE) {
                    MarginLayoutParams lp = (MarginLayoutParams) child.getLayoutParams();
                    child.layout((j-skip) * rowclumnWidth + clunmSpace + lp.leftMargin, i * rowHeight + rowSpace + lp.topMargin, (j + 1-skip) * rowclumnWidth - clunmSpace - lp.rightMargin, (i + 1) * rowHeight - rowSpace - lp.bottomMargin);
                }else{
                    skip++;
                }
            }
        }
    }

    @Override
    protected void onMeasure(int widthSpec, int heightSpec) {
        int width = MeasureSpec.getSize(widthSpec);
        int widmode = MeasureSpec.getMode(widthSpec);
        int height = MeasureSpec.getSize(heightSpec);
        int heightmode = MeasureSpec.getMode(heightSpec);

        int childWidth = width/clumn;
        int childheight = height/row;
		if(!matchParent){
        if(widthAsStandard) {//
            childheight = childWidth*heightWeight/widthWeight;
        }else{
            childWidth = childheight*widthWeight/heightWeight;
        }
		}

        int childCount = getChildCount();
        for (int i=0;i<childCount;i++){

            View child = getChildAt(i);
            MarginLayoutParams lp = (MarginLayoutParams)child.getLayoutParams();
            int widthspec =  MeasureSpec.makeMeasureSpec(childWidth-lp.leftMargin-lp.rightMargin,MeasureSpec.EXACTLY);
            int heightspec =  MeasureSpec.makeMeasureSpec(childheight-lp.bottomMargin-lp.topMargin,MeasureSpec.EXACTLY);
            child.measure(widthspec,heightspec);
        }
        height = childheight*row;
        width = childWidth*clumn;

        setMeasuredDimension(width,height);

    }

    @Override
    public LayoutParams generateLayoutParams(AttributeSet attrs) {
        return new MarginLayoutParams(getContext(),attrs);
    }
}
