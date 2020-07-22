package com.portgo.view;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ExpandableListAdapter;
import android.widget.ExpandableListView;
import android.widget.ListAdapter;

/**
 * Created by huacai on 2017/5/3.
 */
public class AllExpandListView extends ExpandableListView implements ExpandableListView.OnGroupClickListener {
    boolean expandMode = true;
    public AllExpandListView(Context context) {
        super(context);
    }

    public AllExpandListView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public AllExpandListView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    public void setAdapter(ExpandableListAdapter adapter) {
        super.setAdapter(adapter);
        if(expandMode&&adapter!=null){
            expandAllGroup(adapter);
            super.setOnGroupClickListener(this);
        }
    }

    public void setAdapter(ExpandableListAdapter adapter,boolean expand) {
        expandMode = expand;
        this.setAdapter(adapter);
    }

    public boolean collapseGroup(int groupPos) {

        return expandMode;
    }
    public void expandAllGroup(ExpandableListAdapter adapter){
        for (int i=0;i<adapter.getGroupCount();i++) {
            expandGroup(i);
        }
    }

    @Override
    public boolean onGroupClick(ExpandableListView expandableListView, View view, int i, long l) {
        return true;
    }

}