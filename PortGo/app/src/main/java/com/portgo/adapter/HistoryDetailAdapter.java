package com.portgo.adapter;

import android.content.Context;
import android.database.Cursor;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.BaseExpandableListAdapter;
import android.widget.CursorAdapter;
import android.widget.ExpandableListAdapter;
import android.widget.ImageView;
import android.widget.SimpleExpandableListAdapter;
import android.widget.TextView;

import com.portgo.R;
import com.portgo.manager.HistoryAVCallEvent;
import com.portgo.manager.IncomingCallReceiver;
import com.portgo.util.DateTimeUtils;
import com.portgo.util.NgnMediaType;
import com.portgo.view.ViewHolder;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

/**
 * Created by huacai on 2017/5/24.
 */

public class HistoryDetailAdapter extends BaseExpandableListAdapter {
    SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm:ss");
    SimpleDateFormat timeHMFormat = new SimpleDateFormat("HH:mm");
    LayoutInflater mInflater;
    List<HistoryAVCallEvent> callHistroys;
    HashMap<String,List<HistoryAVCallEvent>> mGroupData;
    Context mContext;

    public HistoryDetailAdapter(Context context,HashMap<String,List<HistoryAVCallEvent>> groupData) {
        mGroupData = groupData;
        mContext = context;
        mInflater = LayoutInflater.from(mContext);
    }

    private void updateItemView(ViewHolder.HistroyDetailCallHolder holder, HistoryAVCallEvent history){

        long startTime = history.getStartTime();
        final String strStartTime = timeHMFormat.format(startTime);
        Calendar connectTime =history.getCallTime();  //time of Connecting
        final String strConnectTime = ""+DateFormat.format("HH:mm:ss",connectTime);
        boolean callOut = history.getCallOut();
        boolean connected = history.getConnect();

        NgnMediaType calltype = history.getMediaType();

        switch (calltype) {
            case Audio:
                holder.ivType.setImageResource(R.drawable.recent_call_list_style_audio_ico);
                break;
            case Video:
            case AudioVideo:
                holder.ivType.setImageResource(R.drawable.recent_call_list_style_video_ico);
                break;
        }
        if(callOut){
            holder.tvInOut.setText(R.string.callout);
        }else{
            holder.tvInOut.setText(R.string.callin);
        }
        holder.tvStartTime.setText(strStartTime);
        if(callOut&&connected){
            holder.tvConnectTime.setText(strConnectTime);
        }else if(!callOut&&connected){
            holder.tvConnectTime.setText(strConnectTime);
        }else{
            holder.tvConnectTime.setText(R.string.unconnect);
        }
        if(!callOut&&!connected) {
            holder.tvConnectTime.setTextColor(mContext.getResources().getColor(R.color.portgo_color_red));
        }else{
            holder.tvConnectTime.setTextColor(mContext.getResources().getColor(R.color.portgo_color_darkgray));
        }

    }

    public static final int SHOW_ALL = Integer.MAX_VALUE;
    int showCount = SHOW_ALL;
    public void setShowCount(int count) {
        this.showCount = count;
        notifyDataSetChanged();
    }

    public boolean isExpand(){
        return showCount>callHistroys.size();
    }

    @Override
    public int getGroupCount() {
        return mGroupData.keySet().size();
    }

    @Override
    public int getChildrenCount(int i) {
        return ((List<HistoryAVCallEvent>)getGroup(i)).size();
    }

    @Override
    public Object getGroup(int i) {
        return mGroupData.get(mGroupData.keySet().toArray()[i]);
    }

    @Override
    public Object getChild(int i, int i1) {
        return ((List<HistoryAVCallEvent>)getGroup(i)).get(i1);
    }

    @Override
    public long getGroupId(int i) {
        return 1;
    }

    @Override
    public long getChildId(int i, int i1) {
        return i1;
    }

    @Override
    public boolean hasStableIds() {
        return false;
    }

    @Override
    public View getGroupView(int i, boolean b, View view, ViewGroup viewGroup) {
        TextView groupView = (TextView) mInflater.inflate(R.layout.history_detail_call_lvitem_group, null);
        String daytime = (String) mGroupData.keySet().toArray()[i];
        groupView.setText(daytime);
        return groupView;
    }

    @Override
    public View getChildView(int i, int i1, boolean b, View view, ViewGroup viewGroup) {
        ViewHolder.HistroyDetailCallHolder holder;
        if(view==null) {
            view = mInflater.inflate(R.layout.history_detail_call_lvitem, null);
            holder = new ViewHolder.HistroyDetailCallHolder();
            holder.tvStartTime = (TextView) view.findViewById(R.id.contact_details_listview_call_starttime);
            holder.ivType = (ImageView) view.findViewById(R.id.contact_details_listview_call_type);
            holder.tvConnectTime = (TextView) view.findViewById(R.id.contact_details_listview_call_connecttime);
            holder.tvInOut = (TextView) view.findViewById(R.id.contact_details_listview_call_inout);
            view.setTag(holder);
        }else{
            holder = (ViewHolder.HistroyDetailCallHolder) view.getTag();
        }
        updateItemView(holder, (HistoryAVCallEvent) getChild(i,i1));
        return view;
    }

    @Override
    public boolean isChildSelectable(int i, int i1) {
        return false;
    }

}

