package com.portgo.view;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListAdapter;
import android.widget.ListView;

/**
 * Created by huacai on 2017/5/3.
 */
public class ExpandListView extends ListView {

    @Override
    public void focusableViewAvailable(View v) {
        super.focusableViewAvailable(v);
    }

    public ExpandListView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public ExpandListView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public ExpandListView(Context context) {
        super(context);
    }

    @Override
    public void setAdapter(ListAdapter adapter) {
        super.setAdapter(adapter);
        setListViewHeightBasedOnChildren(adapter,false);
    }

    public void setAdapter(ListAdapter adapter,boolean itemViewInSameSize) {
        super.setAdapter(adapter);
        setListViewHeightBasedOnChildren(adapter,itemViewInSameSize);
    }

    public void setListViewHeightBasedOnChildren(ListAdapter listAdapter,boolean itemViewInSameSize) {
        int totalHeight = 0;

        if (listAdapter == null) {return;}
        if(itemViewInSameSize){
            int count =listAdapter.getCount();
            if(0< count){
                View listItem = listAdapter.getView(0, null, this);
                int desiredWidth = MeasureSpec.makeMeasureSpec(getWidth(), MeasureSpec.AT_MOST);
                listItem.measure(desiredWidth, 0);

                totalHeight += listItem.getMeasuredHeight();
                totalHeight*= count;
            }
        }else {
            for (int i = 0; i < listAdapter.getCount(); i++) {
                View listItem = listAdapter.getView(i, null, this);
                int desiredWidth = MeasureSpec.makeMeasureSpec(getWidth(), MeasureSpec.AT_MOST);
                listItem.measure(desiredWidth, 0);

                totalHeight += listItem.getMeasuredHeight();
            }
        }

        ViewGroup.LayoutParams params = getLayoutParams();

        params.height = totalHeight
                + getDividerHeight() * (listAdapter.getCount() - 1);

        setLayoutParams(params);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int expandSpec = MeasureSpec.makeMeasureSpec(Integer.MAX_VALUE >> 2,
                MeasureSpec.AT_MOST);
        super.onMeasure(widthMeasureSpec, expandSpec);
    }

}