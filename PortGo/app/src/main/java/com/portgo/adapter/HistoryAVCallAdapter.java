package com.portgo.adapter;

import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.provider.ContactsContract;
import android.text.TextUtils;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.CursorAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.portgo.BuildConfig;
import com.portgo.R;
import com.portgo.manager.Contact;
import com.portgo.manager.ContactManager;
import com.portgo.manager.HistoryAVCallEvent;
import com.portgo.manager.HistoryAVCallEventForList;
import com.portgo.util.DateTimeUtils;
import com.portgo.util.NgnMediaType;
import com.portgo.util.NgnStringUtils;
import com.portgo.util.NgnUriUtils;
import com.portgo.view.RoundedImageView;
import com.portgo.view.ViewHolder;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import static com.portgo.BuildConfig.HASSIPTAILER;

public class HistoryAVCallAdapter extends BaseAdapter{
    LayoutInflater mInflater;
    Context mContext;
    View.OnClickListener mMoreDetailsClick;
    Drawable audioDrawable,videoDrawable,callIndrawable,callOutDrawable,missedDrawable;
    private List<HistoryAVCallEventForList> mEvents;
    public HistoryAVCallAdapter(Context context, List<HistoryAVCallEventForList> events, View.OnClickListener more) {
        super();
        mContext= context;
        mEvents = events;
        mMoreDetailsClick = more;
        mInflater = LayoutInflater.from(context);
        audioDrawable = mContext.getResources().getDrawable(R.drawable.recent_call_style_audio_ico);
        audioDrawable.setBounds(0, 0, audioDrawable.getIntrinsicWidth(), audioDrawable.getIntrinsicHeight());
        videoDrawable = mContext.getResources().getDrawable(R.drawable.recent_call_style_video_ico);
        videoDrawable.setBounds(0, 0, videoDrawable.getIntrinsicWidth(), videoDrawable.getIntrinsicHeight());
        callIndrawable = mContext.getResources().getDrawable(R.drawable.recent_call_in_answer_ico);
        callIndrawable.setBounds(0, 0, callIndrawable.getIntrinsicWidth(), callIndrawable.getIntrinsicHeight());
        callOutDrawable = mContext.getResources().getDrawable(R.drawable.recent_call_outr_ico);
        callOutDrawable.setBounds(0, 0, callOutDrawable.getIntrinsicWidth(), callOutDrawable.getIntrinsicHeight());
        missedDrawable = mContext.getResources().getDrawable(R.drawable.recent_call_in_noanswer_ico);
        missedDrawable.setBounds(0, 0, missedDrawable.getIntrinsicWidth(), missedDrawable.getIntrinsicHeight());
    }

    @Override
    public int getCount() {
        if(mEvents==null)
            return 0;
        return mEvents.size();
    }

    @Override
    public Object getItem(int i) {
        return mEvents.get(i);
    }

    @Override
    public long getItemId(int i) {
        return mEvents.get(i).getEvent().getId();
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder.HistroyViewHolder holder;
        if(convertView==null) {
            convertView = mInflater.inflate(R.layout.history_detail_item, null);
            holder = new ViewHolder.HistroyViewHolder();

            holder.tvCallName = (TextView) convertView.findViewById(R.id.history_detail_call_name);
            holder.tvCallNumber= (TextView) convertView.findViewById(R.id.history_detail_call_number);
            holder.tvStartTime = (TextView) convertView.findViewById(R.id.history_detail_starttime);
            holder.imgAvatar = (RoundedImageView) convertView.findViewById(R.id.user_avatar_image);
            holder.tvAvatar = (TextView) convertView.findViewById(R.id.user_avatar_text);
            holder.moreDetails = convertView.findViewById(R.id.history_detail_more);
            holder.history_item_radiobox = (CheckBox) convertView.findViewById(R.id.history_item_radiobox);
            holder.convertView = convertView;
            convertView.setTag(holder);
        }else{
            holder = (ViewHolder.HistroyViewHolder) convertView.getTag();
        }

        HistoryAVCallEventForList item= (HistoryAVCallEventForList) getItem(position);

        updateItemView(holder,item, (ListView) parent);
        return convertView;

    }

    private void updateItemView(ViewHolder.HistroyViewHolder holder, HistoryAVCallEventForList historyItem, ListView parent){

        int count = 0,contactId =0,contactType=0;
        HistoryAVCallEvent history = historyItem.getEvent();
        holder.moreDetails.setOnClickListener(mMoreDetailsClick);
        holder.moreDetails.setTag(historyItem);

        long startTime = history.getStartTime();
        final String date = DateTimeUtils.getFriendlyDateStringShort(new Date(startTime), mContext);
        boolean callOut = history.getCallOut();
        boolean connected = history.getConnect();
        NgnMediaType calltype = history.getMediaType();

        holder.tvStartTime.setText(date);
        Contact contact = historyItem.getAttachContact();

        switch (calltype) {
            case Audio:
                holder.tvCallName.setCompoundDrawables(null,null,audioDrawable,null);
                break;
            case Video:
            case AudioVideo:
                holder.tvCallName.setCompoundDrawables(null,null,videoDrawable,null);
                break;
        }
        if(!HASSIPTAILER){
            holder.tvCallNumber.setText(NgnUriUtils.getUserName(history.getRemoteUri()));
        }else{
            holder.tvCallNumber.setText(history.getRemoteUri());
        }
        if(callOut){
            holder.tvCallNumber.setCompoundDrawables(callOutDrawable,null,null,null);
        }else {
            holder.tvCallNumber.setCompoundDrawables(callIndrawable,null,null,null);
        }
        if(connected){
            holder.tvCallName.setTextColor(mContext.getResources().getColor(android.R.color.black));
        }else{
            holder.tvCallName.setTextColor(mContext.getResources().getColor(R.color.portgo_color_red));
        }
       if(!callOut&&!connected) {
           holder.tvCallNumber.setCompoundDrawables(missedDrawable,null,null,null);
       }

        String disName = null;
        Bitmap avatar = null;
        if(contact!=null){
            disName = contact.getDisplayName();
            avatar = contact.getAvatar();

        }

        if(TextUtils.isEmpty(disName)) {
            disName = history.getDisplayName();
        }

        if(avatar!=null){
            holder.imgAvatar.setImageBitmap(avatar);
            holder.imgAvatar.setVisibility(View.VISIBLE);
            holder.tvAvatar.setVisibility(View.GONE);
        }else{
            holder.imgAvatar.setVisibility(View.GONE);
            holder.tvAvatar.setVisibility(View.VISIBLE);
            holder.tvAvatar.setText(NgnStringUtils.getAvatarText(disName));
        }

        if(historyItem.getCount()>1) {
            holder.tvCallName.setText(disName+"("+historyItem.getCount()+")");
        }else {
            holder.tvCallName.setText(disName);
        }

        if(parent.getChoiceMode()== AbsListView.CHOICE_MODE_MULTIPLE){
            holder.history_item_radiobox.setVisibility(View.VISIBLE);
            if(isItemChecked(history.getId())){
                holder.history_item_radiobox.setChecked(true);
            }else{
                holder.history_item_radiobox.setChecked(false);
            }
        }else {
            holder.history_item_radiobox.setVisibility(View.GONE);
        }
    }

    HashMap<Integer,Boolean> selectItems = new HashMap();
    public void setItemCheck(int id,boolean check){
        if(check) {
            selectItems.put(id, check);
        }else {
            selectItems.remove(id);
        }
        notifyDataSetChanged();
    }

    public boolean isItemChecked(int id){
        boolean result= false;
        try {
            result =selectItems.get(id);
        }catch (NullPointerException e){

        }
        return  result;

    }

    public ArrayList<Integer> getSelectItems(){
        ArrayList<Integer> result = new ArrayList<Integer>();
        result.addAll(selectItems.keySet());
        return result;
    }

    public void setSelectALL(){
        selectItems.clear();
        int count  = getCount();
        for (int i=0;i<count;i++) {
            int id = (int) getItemId(i);
            selectItems.put(id, true);
        }
    }

    public boolean isAllSelect(){
        int count  = getCount();
        for (int i=0;i<count;i++){
            int id = (int) getItemId(i);
            if(!selectItems.containsKey(id)){
                return false;
            }
        }
        return true;
    }

    public void setSelectNone(){
        selectItems.clear();
    }

    public void setSelectItems(ArrayList<Integer> items){
        selectItems.clear();
        for (Integer id:items) {
            selectItems.put(id,true);
        }
    }
}
