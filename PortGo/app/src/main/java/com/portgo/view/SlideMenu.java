package com.portgo.view;

import android.content.Context;

import java.util.ArrayList;
import java.util.List;

/**
 * 
 * @author baoyz
 * @date 2014-8-23
 * 
 */
public class SlideMenu {

	private Context mContext;
	private List<SlideMenuItem> mItems;
	private int mViewType;

	public SlideMenu(Context context) {
		mContext = context;
		mItems = new ArrayList<SlideMenuItem>();
	}

	public Context getContext() {
		return mContext;
	}

	public void addMenuItem(SlideMenuItem item) {
		mItems.add(item);
	}

	public void removeMenuItem(SlideMenuItem item) {
		mItems.remove(item);
	}

	public List<SlideMenuItem> getMenuItems() {
		return mItems;
	}

	public SlideMenuItem getMenuItem(int index) {
		return mItems.get(index);
	}

	public int getViewType() {
		return mViewType;
	}

	public void setViewType(int viewType) {
		this.mViewType = viewType;
	}

}
