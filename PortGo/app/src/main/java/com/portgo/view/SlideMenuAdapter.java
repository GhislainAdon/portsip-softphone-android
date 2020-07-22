package com.portgo.view;

import android.content.Context;
import android.database.DataSetObserver;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListAdapter;
import android.widget.WrapperListAdapter;

/**
 * 
 * @author baoyz
 * @date 2014-8-24
 * 
 */
public class SlideMenuAdapter implements WrapperListAdapter,
        SlideMenuView.OnSlideItemClickListener {

    private ListAdapter mAdapter;
    private Context mContext;
    private SlideMenuListView.OnMenuItemClickListener onMenuItemClickListener;

    public SlideMenuAdapter(Context context, ListAdapter adapter) {
        mAdapter = adapter;
        mContext = context;
    }

    @Override
    public int getCount() {
        return mAdapter.getCount();
    }

    @Override
    public Object getItem(int position) {
        return mAdapter.getItem(position);
    }

    @Override
    public long getItemId(int position) {
        return mAdapter.getItemId(position);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        SlideMenuLayout layout = null;
        if (convertView == null) {
            View contentView = mAdapter.getView(position, convertView, parent);
            SlideMenu menu = new SlideMenu(mContext);
            menu.setViewType(getItemViewType(position));
            createMenu(menu);
            SlideMenuView menuView = new SlideMenuView(menu,
                    (SlideMenuListView) parent);
            menuView.setOnSlideItemClickListener(this);
            SlideMenuListView listView = (SlideMenuListView) parent;
            layout = new SlideMenuLayout(contentView, menuView,
                    listView.getCloseInterpolator(),
                    listView.getOpenInterpolator());
            layout.setPosition(position);
        } else {
            layout = (SlideMenuLayout) convertView;
            layout.closeMenu();
            layout.setPosition(position);
            SlideMenuView menuView = layout.getMenuView();
            layout.removeView(menuView);

            SlideMenu menu = new SlideMenu(mContext);

            menu.setViewType(getItemViewType(position));
            createMenu(menu);
             menuView = new SlideMenuView(menu,
                    (SlideMenuListView) parent);
            menuView.setOnSlideItemClickListener(this);

//            layout.addView(menuView);

            layout.setmMenuView(menuView);
            View view = mAdapter.getView(position, layout.getContentView(),
                    parent);
        }
        if (mAdapter instanceof BaseSwipListAdapter) {
            boolean swipEnable = (((BaseSwipListAdapter) mAdapter).getSwipEnableByPosition(position));
            layout.setSwipEnable(swipEnable);
        }
        return layout;
    }

    public void createMenu(SlideMenu menu) {
        // Test Code
        SlideMenuItem item = new SlideMenuItem(mContext);
        item.setTitle("Item 1");
        item.setBackground(new ColorDrawable(Color.GRAY));
        item.setWidth(300);
        menu.addMenuItem(item);

        item = new SlideMenuItem(mContext);
        item.setTitle("Item 2");
        item.setBackground(new ColorDrawable(Color.RED));
        item.setWidth(300);
        menu.addMenuItem(item);
    }

    @Override
    public void onItemClick(SlideMenuView view, SlideMenu menu, int index) {
        if (onMenuItemClickListener != null) {
            onMenuItemClickListener.onMenuItemClick(view.getPosition(), menu,
                    index);
        }
    }

    public void setOnSwipeItemClickListener(
            SlideMenuListView.OnMenuItemClickListener onMenuItemClickListener) {
        this.onMenuItemClickListener = onMenuItemClickListener;
    }

    @Override
    public void registerDataSetObserver(DataSetObserver observer) {
        mAdapter.registerDataSetObserver(observer);
    }

    @Override
    public void unregisterDataSetObserver(DataSetObserver observer) {
        mAdapter.unregisterDataSetObserver(observer);
    }

    @Override
    public boolean areAllItemsEnabled() {
        return mAdapter.areAllItemsEnabled();
    }

    @Override
    public boolean isEnabled(int position) {
        return mAdapter.isEnabled(position);
    }

    @Override
    public boolean hasStableIds() {
        return mAdapter.hasStableIds();
    }

    @Override
    public int getItemViewType(int position) {
        return mAdapter.getItemViewType(position);
    }

    @Override
    public int getViewTypeCount() {
        return mAdapter.getViewTypeCount();
    }

    @Override
    public boolean isEmpty() {
        return mAdapter.isEmpty();
    }

    @Override
    public ListAdapter getWrappedAdapter() {
        return mAdapter;
    }

}
