package com.portgo.view;


import android.content.Context;
import android.graphics.drawable.Drawable;

/**
 * 
 * @author baoyz
 * @date 2014-8-23
 * 
 */
public class SlideMenuItem {

	private int id;
	private Context mContext;
	private String title;
	private Drawable icon;
	private Drawable background;
	private int titleColor;
	private int titleSize;
	private int width;

	public SlideMenuItem(Context context) {
		mContext = context;
	}

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public int getTitleColor() {
		return titleColor;
	}

	public int getTitleSize() {
		return titleSize;
	}

	public void setTitleSize(int titleSize) {
		this.titleSize = titleSize;
	}

	public void setTitleColor(int titleColor) {
		this.titleColor = titleColor;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public void setTitle(int resId) {
		setTitle(mContext.getString(resId));
	}

	public Drawable getIcon() {
		return icon;
	}

	public void setIcon(Drawable icon) {
		this.icon = icon;
	}

	public void setIcon(int resId) {
		this.icon = mContext.getResources().getDrawable(resId);
	}

	public Drawable getBackground() {
		return background;
	}

	public void setBackground(Drawable background) {
		this.background = background;
	}

	public void setBackground(int resId) {
		this.background = mContext.getResources().getDrawable(resId);
	}

	public int getWidth() {
		return width;
	}

	public void setWidth(int width) {
		this.width = width;
	}


	static public class Builder{
		private int id;
		private Context mContext;
		private String title="";
		private Drawable icon=null;
		private Drawable background=null;
		private int titleColor;
		private int titleSize;
		private int width;
		public Builder(Context context){
			mContext = context;
			titleColor = context.getResources().getColor(android.R.color.black);
			titleSize = 12;
			width = 90;
			background = context.getResources().getDrawable(android.R.drawable.btn_default);
		}
		private Builder(){}

		public SlideMenuItem build(){
            SlideMenuItem result = new SlideMenuItem(mContext);
			result.setBackground(background);
			result.setTitle(title);
			result.setIcon(icon);
			result.setId(id);
			result.setTitleColor(titleColor);
			result.setTitleSize(titleSize);
			result.setWidth(width);
			return result;
		}

		public Builder setWidth(int width) {
			this.width = width;
			return this;
		}

		public Builder setTitle(String title) {
			this.title = title;
			return this;
		}

		public Builder setIcon(Drawable icon) {
			this.icon = icon;
			return this;
		}

		public Builder setIcon(int iconDrawableId) {
			this.icon = mContext.getResources().getDrawable(iconDrawableId);
			return this;
		}

		public Builder setBackground(Drawable background) {
			this.background = background;
			return this;
		}

		public Builder setBackground(int drawableId) {
			this.background = mContext.getResources().getDrawable(drawableId);
			return this;
		}

		public Builder setId(int id) {
			this.id = id;
			return this;
		}

		public Builder setTitleColor(int titleColor) {
			this.titleColor = titleColor;
			return this;
		}

		public Builder setTitleSize(int titleSize) {
			this.titleSize = titleSize;
			return this;
		}
	}
}
