package com.portgo.adapter;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.provider.ContactsContract;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.CheckBox;
import android.widget.CursorAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.portgo.R;
import com.portgo.database.DBHelperBase;
import com.portgo.manager.ContactManager;
import com.portgo.manager.MessageEvent;
import com.portgo.util.DateTimeUtils;
import com.portgo.util.NgnStringUtils;
import com.portgo.view.ViewHolder;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;

public class MessageCursorAdapter extends CursorAdapter {
    private LayoutInflater mInflater;
    private Context mContext;

    public MessageCursorAdapter(Context context, Cursor c, int flag) {
        super(context, c, flag);
        mContext= context;
        mInflater = LayoutInflater.from(context);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder.MessageViewHolder holder = new ViewHolder.MessageViewHolder();
        if(convertView==null) {
            convertView = mInflater.inflate(R.layout.activity_messages_item, null);
            holder.tvRemote = (TextView) convertView.findViewById(R.id.messages_item_textView_remote);
            holder.tvDate = (TextView) convertView.findViewById(R.id.messages_item_textView_date);
            holder.tvContent = (TextView) convertView.findViewById(R.id.messages_item_textView_content);
//            holder.tvUnSeen = (TextView) convertView.findViewById(R.id.messages_item_textView_unseen);
            holder.tvAvatar = (TextView) convertView.findViewById(R.id.user_avatar_text);
            holder.imgAvatar = (ImageView) convertView.findViewById(R.id.user_avatar_image);
            holder.message_item_radiobox= (CheckBox) convertView.findViewById(R.id.message_item_radiobox);
            convertView.setTag(holder);
        }else{
            holder = (ViewHolder.MessageViewHolder) convertView.getTag();
        }
        Cursor cursor = (Cursor) getItem(position);
        updateMesaageItemView(holder,cursor, (ListView) parent);
        return convertView;
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup viewGroup) {
        return null;
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {
     return;
    }


    private void updateMesaageItemView(ViewHolder.MessageViewHolder holder, Cursor cursor, ListView parent){
        int unSeenCount = 0;
        MessageEvent event = MessageEvent.messageFromCursor(cursor);
        int indexUNSEECOUNT =cursor.getColumnIndex(DBHelperBase.MessageColumns.TEMP_CLUMN_UNSEECOUNT);
        if(indexUNSEECOUNT>0){
            unSeenCount = cursor.getInt(indexUNSEECOUNT);
        }
        String DisName = event.getDisplayName();
        String mime = event.getMime();
        long messageTime = event.getMessageTime();
        String content;
        if("text/plain".equals(mime)) {
            content = event.getContent();
        }else{
            content = mContext.getString(R.string.unknow_message_format);
        }

        holder.tvRemote.setText(DisName);
        final String date = DateTimeUtils.getFriendlyDateString(new Date(messageTime), mContext);
        holder.tvDate.setText(date);
        holder.tvContent.setText(NgnStringUtils.isNullOrEmpty(content)
                ? NgnStringUtils.emptyValue() : content);

        if(unSeenCount>0) {
//            holder.tvUnSeen.setText(""+unSeenCount);
//            holder.tvUnSeen.setVisibility(View.VISIBLE);
        }else{
//            holder.tvUnSeen.setVisibility(View.INVISIBLE);
        }


        Bitmap remote = null;
        String disName = null;

        if(remote!=null){
            holder.imgAvatar.setImageBitmap(remote);
            holder.imgAvatar.setVisibility(View.VISIBLE);
            holder.tvAvatar.setVisibility(View.GONE);
        }else{
            holder.imgAvatar.setVisibility(View.GONE);
            holder.tvAvatar.setVisibility(View.VISIBLE);
            if(TextUtils.isEmpty(disName)){
                disName = event.getDisplayName();
            }

            holder.tvAvatar.setText(NgnStringUtils.getAvatarText(disName));
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
