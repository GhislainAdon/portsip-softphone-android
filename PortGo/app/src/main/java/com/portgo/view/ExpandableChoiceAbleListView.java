package com.portgo.view;

/**
 * Created by huacai on 2014/11/11.
 */

import android.content.Context;
import android.os.Build;
import androidx.annotation.RequiresApi;
import android.util.AttributeSet;
import android.util.SparseBooleanArray;
import android.widget.AbsListView;
import android.widget.BaseAdapter;
import android.widget.ExpandableListView;

/**
 * @blog http://blog.csdn.net/xiaanming
 *
 * @author xiaanming
 *
 */

public class ExpandableChoiceAbleListView extends ExpandableListView {
    SparseBooleanArray booleanArray;
    int choiceMode = AbsListView.CHOICE_MODE_NONE;
    public ExpandableChoiceAbleListView(Context context) {
        this(context,null);
    }

    public ExpandableChoiceAbleListView(Context context, AttributeSet attrs) {
        super(context, attrs,0);
        booleanArray= new SparseBooleanArray();
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public ExpandableChoiceAbleListView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        booleanArray= new SparseBooleanArray();
    }

    @Override
    public void setChoiceMode(int choiceMode) {
        this.choiceMode = choiceMode;
        if(choiceMode==AbsListView.CHOICE_MODE_NONE)
            clearChoices();
    }

    @Override
    public int getChoiceMode() {
        return choiceMode;
    }

    @Override
    public void clearChoices() {
        if(getAdapter()!=null&&booleanArray!=null) {
            booleanArray.clear();
            if(getAdapter() instanceof BaseAdapter){
                BaseAdapter adapter = (BaseAdapter) getAdapter();
                adapter.notifyDataSetChanged();
                return;
            }
            if(getAdapter() instanceof ExpandableCursorTreeAdapter){
                ExpandableCursorTreeAdapter adapter = (ExpandableCursorTreeAdapter) getAdapter();
                adapter.notifyDataSetChanged();
                return;
            }
        }
    }

    @Override
    public void setItemChecked(int key, boolean value) {
        booleanArray.put(key,value);

    }

    @Override
    public boolean isItemChecked(int key) {
        return booleanArray.get(key);
    }

    public SparseBooleanArray getBooleanArray() {
        return booleanArray;
    }
}
