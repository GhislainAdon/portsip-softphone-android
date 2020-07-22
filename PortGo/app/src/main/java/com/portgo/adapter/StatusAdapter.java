package com.portgo.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.portgo.R;
import java.util.HashMap;


/**
 * Created by huacai on 2017/5/24.
 */

public class StatusAdapter extends BaseAdapter{
    private final LayoutInflater mInflater;
    private final Context mContext;
    private final HashMap<Integer,String> data;

    public StatusAdapter(Context context, HashMap<Integer,String> status) {
        mContext = context;
        mInflater = LayoutInflater.from(mContext);
        data = status;
    }

    boolean online;
    public void setOnline(boolean online){
        this.online = online;
    }
    @Override
    public int getCount() {
        return data.size();
    }

    @Override
    public Object getItem(int i) {
        return data.get(data.keySet().toArray()[i]);
    }

    @Override
    public long getItemId(int i) {
        return data.keySet().toArray(new Integer[data.size()])[i];
    }

    @Override
    public View getView(int i, View view, ViewGroup viewGroup) {

        view = mInflater.inflate(R.layout.view_status_item,null);
        ImageView imageView = (ImageView) view.findViewById(R.id.status_icon);
        TextView textView = (TextView) view.findViewById(R.id.status_description);
        CheckBox checkBox = (CheckBox) view.findViewById(R.id.status_checkbox);
        ListView listView = (ListView) viewGroup;
        if(listView.isItemChecked(i))
        {
            checkBox.setVisibility(View.VISIBLE);
            checkBox.setChecked(true);
        }else {
            checkBox.setVisibility(View.GONE);
        }
        int iconId = (int)getItemId(i);
        String description = data.get(iconId);
        imageView.setImageResource(iconId);
        textView.setText(description);
        if(online){
            view.setBackgroundResource(R.color.portgo_color_white);
        }else{
            view.setBackgroundResource(R.color.portgo_color_lightgray);
        }
        return view;
    }
}