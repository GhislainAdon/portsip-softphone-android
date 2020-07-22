package com.portgo.adapter;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Typeface;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.portgo.R;
import com.portgo.database.DataBaseManager;
import com.portgo.manager.ChatSession;
import com.portgo.manager.MessageEvent;
import com.portgo.util.DateTimeUtils;
import com.portgo.util.NgnStringUtils;
import com.portgo.view.TextDrawable;
import com.portgo.view.ViewHolder;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

public class MessageSearchAdapter extends BaseAdapter {
    private LayoutInflater mInflater;
    private Context mContext;
    List<MessageEvent> mMessages=null;
    int drawableWidth,drawableHeight,textSize;
    public MessageSearchAdapter(Context context, List<MessageEvent> messages ) {
        super();
        mMessages = messages;
        mContext= context;
        drawableWidth = (int) context.getResources().getDimension(R.dimen.textsize_16sp);
        drawableHeight= (int) context.getResources().getDimension(R.dimen.textsize_16sp);
        textSize= (int) context.getResources().getDimension(R.dimen.textsize_unsee);

        mInflater = LayoutInflater.from(context);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder.MessageViewHolder holder = new ViewHolder.MessageViewHolder();
        if(convertView==null) {
            convertView = mInflater.inflate(R.layout.messages_search_item, null);
            holder.tvRemote = (TextView) convertView.findViewById(R.id.messages_item_textView_remote);
            holder.tvDate = (TextView) convertView.findViewById(R.id.messages_item_textView_date);
            holder.tvContent = (TextView) convertView.findViewById(R.id.messages_item_textView_content);
            holder.tvAvatar = (TextView) convertView.findViewById(R.id.user_avatar_text);
            holder.imgAvatar = (ImageView) convertView.findViewById(R.id.user_avatar_image);
            holder.message_item_radiobox= (CheckBox) convertView.findViewById(R.id.message_item_radiobox);
            convertView.setTag(holder);
        }else{
            holder = (ViewHolder.MessageViewHolder) convertView.getTag();
        }
        MessageEvent event = (MessageEvent) getItem(position);
        updateMesaageItemView(holder,event, (ListView) parent);
        return convertView;
    }

    @Override
    public int getCount() {
        return mMessages==null?0:mMessages.size();
    }

    @Override
    public Object getItem(int i) {
        return mMessages==null?null:mMessages.get(i);
    }

    @Override
    public long getItemId(int i) {
        return ((MessageEvent)getItem(i)).getId();
    }

    private void updateMesaageItemView(ViewHolder.MessageViewHolder holder, MessageEvent event, ListView parent){

        ChatSession chatSession = DataBaseManager.findChatSessionByID(mContext,event.getSessionid());
        String content = event.getDescription(mContext);
        String DisName = chatSession.getDisplayName();
        long messageTime = event.getMessageTime();

        holder.tvRemote.setText(DisName);
        final String date = DateTimeUtils.getFriendlyDateString(new Date(messageTime), mContext);
        holder.tvDate.setText(date);
        holder.tvContent.setText(NgnStringUtils.isNullOrEmpty(content)
                ? NgnStringUtils.emptyValue() : content);
        Bitmap remote = null;

        if(remote!=null){
            holder.imgAvatar.setImageBitmap(remote);
            holder.imgAvatar.setVisibility(View.VISIBLE);
            holder.tvAvatar.setVisibility(View.GONE);
        }else{
            holder.imgAvatar.setVisibility(View.GONE);
            holder.tvAvatar.setVisibility(View.VISIBLE);
            holder.tvAvatar.setText(NgnStringUtils.getAvatarText(DisName));
        }

        if(parent.getChoiceMode()== AbsListView.CHOICE_MODE_MULTIPLE){
            holder.message_item_radiobox.setVisibility(View.VISIBLE);
            if(isItemChecked(event.getId())){
                holder.message_item_radiobox.setChecked(true);
            }else{
                holder.message_item_radiobox.setChecked(false);
            }
        }else {
            holder.message_item_radiobox.setVisibility(View.GONE);
        }
        return ;
    }

    @Override
    public void notifyDataSetChanged(){
        super.notifyDataSetChanged();
    }

    HashMap<Long,Boolean> selectItems = new HashMap();
    public void setItemCheck(View view,long id,boolean check){
        if(check) {
            selectItems.put(id, check);
        }else {
            selectItems.remove(id);
        }

        if(view!=null){
            CheckBox box = (CheckBox) view.findViewById(R.id.message_item_radiobox);
            box.setChecked(check);
        }
    }

    public boolean isItemChecked(long id){
        boolean result= false;
        try {
            result =selectItems.get(id);
        }catch (NullPointerException e){
        }
        return  result;
    }

    public ArrayList<Long> getSelectItems(){
        ArrayList<Long> result = new ArrayList<Long>();
        result.addAll(selectItems.keySet());
        return result;
    }

    public void setSelectAll(){
        selectItems.clear();
        int count = getCount();
        for (int position=0;position<count;position++) {
            long id = getItemId(position);
            selectItems.put(id,true);
        }
    }

    public void setSelectNone(){
        selectItems.clear();
    }

    public boolean isAllSelect(){
        int count = getCount();
        for (int position=0;position<count;position++) {
            long id = getItemId(position);
            if(!selectItems.containsKey(id)){
                return false;
            }
        }
        return true;
    }
    public void setSelectItems(ArrayList<Long> items){
        selectItems.clear();
        for (Long id:items) {
            selectItems.put(id,true);
        }
    }

}
