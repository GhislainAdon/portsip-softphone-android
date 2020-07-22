package com.portgo.view;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ExpandableListAdapter;
import android.widget.ExpandableListView;

/**
 * Created by huacai on 2017/5/3.
 */
public class MeasureExpandListView extends AllExpandListView{

    public MeasureExpandListView(Context context) {
        super(context);
    }

    public MeasureExpandListView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public MeasureExpandListView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public void setAdapter(ExpandableListAdapter adapter,boolean itemViewInSameSize,boolean expand) {
        setListViewHeightBasedOnChildren(adapter,itemViewInSameSize);
        this.setAdapter(adapter,expand);
    }

    private void setListViewHeightBasedOnChildren(ExpandableListAdapter listAdapter, boolean itemViewInSameSize) {
        int totalHeight = 0;
        int count=0;

        if (listAdapter == null) {return;}
        if(itemViewInSameSize){
            int groupcount = listAdapter.getGroupCount();
            int grouItemHeight = 0;
            if(groupcount>0){
                int desiredWidth = MeasureSpec.makeMeasureSpec(getWidth(), MeasureSpec.AT_MOST);
                View groupView =  listAdapter.getGroupView(0,false,null,this);
                groupView.measure(desiredWidth, 0);
                grouItemHeight += groupView.getMeasuredHeight();
            }
            int childcount=0;
            int childItemHeight = 0;
            for(int group=0;group<groupcount;group++){
                int childs = listAdapter.getChildrenCount(group);
                childcount+=childs;
                if(childs>0&&childItemHeight==0) {
                    int desiredWidth = MeasureSpec.makeMeasureSpec(getWidth(), MeasureSpec.AT_MOST);
                    View childView = listAdapter.getChildView(0,0,false, null, this);
                    childView.measure(desiredWidth, 0);
                    childItemHeight += childView.getMeasuredHeight();
                }
            }
            count =childcount;
            totalHeight= childItemHeight*childcount+grouItemHeight*groupcount;
        }else {

                int groupcount = listAdapter.getGroupCount();
                int grouItemHeight = 0;
                for(int group=0;group<groupcount;group++){
                    int desiredWidth = MeasureSpec.makeMeasureSpec(getWidth(), MeasureSpec.AT_MOST);
                    View groupView =  listAdapter.getGroupView(group,false,null,this);
                    groupView.measure(desiredWidth, 0);
                    grouItemHeight += groupView.getMeasuredHeight();
                }

                int childItemHeight = 0;
                for(int group=0;group<groupcount;group++){
                    int childcount = listAdapter.getChildrenCount(group);
                    count+=childcount;
                    for(int child = 0;child<childcount;child++){
                        int desiredWidth = MeasureSpec.makeMeasureSpec(getWidth(), MeasureSpec.AT_MOST);
                        View childView = listAdapter.getChildView(group,child,false, null, this);
                        childView.measure(desiredWidth, 0);
                        childItemHeight += childView.getMeasuredHeight();
                    }
                }

                totalHeight= childItemHeight+grouItemHeight;
        }

        ViewGroup.LayoutParams params = getLayoutParams();

        params.height = totalHeight+ getDividerHeight() * (Math.abs(count - 1));

        setLayoutParams(params);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int expandSpec = MeasureSpec.makeMeasureSpec(Integer.MAX_VALUE >> 2,
                MeasureSpec.AT_MOST);
        super.onMeasure(widthMeasureSpec, expandSpec);
    }

}