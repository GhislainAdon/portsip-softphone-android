package com.portgo.adapter;

import android.content.Context;
import android.database.Cursor;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.portgo.R;
import com.portgo.manager.HistoryAVCallEvent;
import com.portgo.util.DateTimeUtils;
import com.portgo.util.NgnMediaType;
import com.portgo.view.ViewHolder;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

/**
 * Created by huacai on 2017/5/24.
 */

public class HistoryDetailCursorAdapter extends CursorAdapter{

    LayoutInflater mInflater;
    SimpleDateFormat formatter = new SimpleDateFormat("HH:mm:ss");
    Context mContext;

    public HistoryDetailCursorAdapter(Context context, Cursor c, int flag) {
        super(context, c, flag);
        mContext = context;
        mInflater = LayoutInflater.from(mContext);
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup viewGroup) {
        ViewHolder.HistroyDetailCallHolder holder;
        View convertView = mInflater.inflate(R.layout.history_detail_call_lvitem, null);
        holder = new ViewHolder.HistroyDetailCallHolder();
        holder.tvStartTime = (TextView) convertView.findViewById(R.id.contact_details_listview_call_starttime);
        holder.ivType = (ImageView) convertView.findViewById(R.id.contact_details_listview_call_type);
        holder.tvConnectTime = (TextView) convertView.findViewById(R.id.contact_details_listview_call_connecttime);
        holder.tvInOut = (TextView) convertView.findViewById(R.id.contact_details_listview_call_inout);
        convertView.setTag(holder);
        updateItemView(holder,cursor);
        return convertView;
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        ViewHolder.HistroyDetailCallHolder holder = (ViewHolder.HistroyDetailCallHolder) view.getTag();
        updateItemView(holder, cursor);
    }

    private void updateItemView(ViewHolder.HistroyDetailCallHolder holder, Cursor itemCursor){
        HistoryAVCallEvent event = HistoryAVCallEvent.historyAVCallEventFromCursor(itemCursor);
        long startTime = event.getStartTime();
        final String strStartTime = DateTimeUtils.getFriendlyDateString(new Date(startTime), mContext);
        Calendar connectTime =event.getCallTime();  //time of Connecting
        final String strConnectTime = ""+ DateFormat.format("HH:mm:ss",connectTime);

        boolean callOut = event.getCallOut();
        boolean connected = event.getConnect();

        NgnMediaType calltype = event.getMediaType();

        switch (calltype) {
            case Audio:
                holder.ivType.setImageResource(R.drawable.recent_call_style_audio_ico);
                break;
            case Video:
            case AudioVideo:
                holder.ivType.setImageResource(R.drawable.recent_call_style_video_ico);
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
        return showCount>super.getCount();
    }

    @Override
    public int getCount() {
        return super.getCount()>showCount?showCount:super.getCount();
    }
}

