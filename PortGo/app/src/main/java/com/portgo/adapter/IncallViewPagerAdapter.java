package com.portgo.adapter;

import android.os.Parcelable;
import androidx.viewpager.widget.PagerAdapter;
import androidx.viewpager.widget.ViewPager;

import android.view.View;

import java.util.List;

public class IncallViewPagerAdapter extends PagerAdapter {
    public List<View> mListViews;

    public IncallViewPagerAdapter(List<View> views) {
        mListViews = views;
    }

    public void destroyItem(View paramView, int paramInt, Object paramObject) {
        ((ViewPager) paramView).removeView(mListViews.get(paramInt));
    }

    public void finishUpdate(View paramView) {
    }

    public int getCount() {
        return mListViews.size();
    }

    public Object instantiateItem(View paramView, int paramInt) {
        View view = mListViews.get(paramInt);
        ((ViewPager) paramView).addView(view, 0);
        return mListViews.get(paramInt);
    }

    public boolean isViewFromObject(View paramView, Object paramObject) {
        return paramView == paramObject;
    }

    public void restoreState(Parcelable paramParcelable, ClassLoader paramClassLoader) {
    }

    public Parcelable saveState() {
        return null;
    }

    public void startUpdate(View paramView) {
    }
}