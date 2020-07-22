package com.portgo.view;

import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.List;

/**
 * 
 * @author baoyz
 * @date 2014-8-23
 * 
 */
public class SlideMenuView extends LinearLayout implements OnClickListener {

	private SlideMenuListView mListView;
	private SlideMenuLayout mLayout;
	private SlideMenu mMenu;
	private OnSlideItemClickListener onItemClickListener;
	private int position;

	public int getPosition() {
		return position;
	}

	public void setPosition(int position) {
		this.position = position;
	}

	public SlideMenuView(SlideMenu menu, SlideMenuListView listView) {
		super(menu.getContext());
		mListView = listView;
		mMenu = menu;
		List<SlideMenuItem> items = menu.getMenuItems();
		int id = 0;
		for (SlideMenuItem item : items) {
			addItem(item, id++);
		}
	}

	private void addItem(SlideMenuItem item, int id) {
		LayoutParams params = new LayoutParams(item.getWidth(),
				LayoutParams.MATCH_PARENT);
		LinearLayout parent = new LinearLayout(getContext());
		parent.setId(id);
		parent.setGravity(Gravity.CENTER);
		parent.setOrientation(LinearLayout.VERTICAL);
		parent.setLayoutParams(params);
		parent.setBackgroundDrawable(item.getBackground());
		parent.setOnClickListener(this);
		addView(parent);

		if (item.getIcon() != null) {
			parent.addView(createIcon(item));
		}
		if (!TextUtils.isEmpty(item.getTitle())) {
			parent.addView(createTitle(item));
		}

	}

	private ImageView createIcon(SlideMenuItem item) {
		ImageView iv = new ImageView(getContext());
		iv.setImageDrawable(item.getIcon());
		return iv;
	}

	private TextView createTitle(SlideMenuItem item) {
		TextView tv = new TextView(getContext());
		tv.setText(item.getTitle());
		tv.setGravity(Gravity.CENTER);
		tv.setSingleLine(true);
		tv.setMaxLines(1);
		tv.setEllipsize(TextUtils.TruncateAt.MARQUEE);
		tv.setTextSize(item.getTitleSize());
		tv.setTextColor(item.getTitleColor());
		return tv;
	}

	@Override
	public void onClick(View v) {
		if (onItemClickListener != null && mLayout.isOpen()) {
			onItemClickListener.onItemClick(this, mMenu, v.getId());
		}
	}

	public OnSlideItemClickListener getOnSlideItemClickListener() {
		return onItemClickListener;
	}

	public void setOnSlideItemClickListener(OnSlideItemClickListener onItemClickListener) {
		this.onItemClickListener = onItemClickListener;
	}

	public void setLayout(SlideMenuLayout mLayout) {
		this.mLayout = mLayout;
	}

	public interface OnSlideItemClickListener {
		void onItemClick(SlideMenuView view, SlideMenu menu, int index);
	}
}
