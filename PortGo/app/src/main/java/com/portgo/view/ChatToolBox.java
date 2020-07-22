package com.portgo.view;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.TextView;

import com.portgo.R;
import com.portgo.util.UnitConversion;

import java.util.List;

public class ChatToolBox extends GridView {
	
	private ChatToolBoxAdapter adapter;
	
	private static final int GRID_ITEM_WIDTH = 80;
	private static final int IMAGE_SIZE_DIP = 60;
	//private static final int COMMON_MARGIN = 10;
	
	public ChatToolBox(Context context) {
		this(context, null);
	}
	
	public ChatToolBox(Context context, AttributeSet attrs) {
		this(context, attrs, 0);
	}

	public ChatToolBox(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		init();
	}
	
	private void init() {
		adapter = new ChatToolBoxAdapter(this.getContext());
		this.setAdapter(adapter);
		this.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view,
					int position, long id) {
				ChatToolItem item = ((ChatToolBoxAdapter)parent.getAdapter()).getItem(position);
				if(null != item) {
					item.onItemSelected();
				}
			}
		});
		this.setSelector(android.R.color.transparent);

		int screenWidth = UnitConversion.getScreenWidth(this.getContext());
		int itemWidth = UnitConversion.dp2px(this.getContext(), GRID_ITEM_WIDTH);
		int imageSize = UnitConversion.dp2px(this.getContext(), IMAGE_SIZE_DIP);
		int numColums = screenWidth / itemWidth;

		this.setNumColumns(numColums);
		this.setStretchMode(GridView.STRETCH_COLUMN_WIDTH);
	}

	public void setData(List<ChatToolItem> items) {
		adapter.setData(items);
		adapter.notifyDataSetChanged();
	}
	
	public static interface ChatToolItem {
		public int getIcon();
		public String getName();
		public void onItemSelected();
	}
	
	private static class ChatToolBoxAdapter extends BaseAdapter {
		Context context;
		List<ChatToolItem> items;
		
		public ChatToolBoxAdapter(Context context) {
			this.context = context;
		}
		
		public void setData(List<ChatToolItem> items) {
			this.items = items;
		}

		@Override
		public int getCount() {
			if(null != items) {
				return items.size();
			}
			return 0;
		}

		@Override
		public ChatToolItem getItem(int position) {
			if(null != items) {
				return items.get(position);
			}
			return null;
		}

		@Override
		public long getItemId(int position) {
			return position;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			if(null == convertView) {
				convertView = inflate(context, R.layout.item_tool_box, null);
				ViewHolder holder = new ViewHolder();
				holder.txt = (TextView) convertView.findViewById(R.id.tool_text);
				holder.img = (ImageView) convertView.findViewById(R.id.tool_img);

				convertView.setTag(holder);
			}
			
			ViewHolder holder = (ViewHolder)convertView.getTag();
			ChatToolItem item = this.getItem(position);
			if(null == holder || null == item) {
				return convertView;
			}
			
			holder.img.setBackgroundResource(item.getIcon());
			holder.txt.setText(item.getName());
			return convertView;
		}
		
		static class ViewHolder {
			TextView txt;
			ImageView img;
		}
	}
}
