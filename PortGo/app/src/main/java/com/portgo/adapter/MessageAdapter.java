package com.portgo.adapter;

import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.provider.ContactsContract;
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
import com.portgo.manager.ChatSessionForShow;
import com.portgo.manager.Contact;
import com.portgo.manager.ContactManager;
import com.portgo.manager.HistoryEvent;
import com.portgo.manager.MessageEvent;
import com.portgo.manager.ChatSession;
import com.portgo.util.DateTimeUtils;
import com.portgo.util.NgnStringUtils;
import com.portgo.view.TextDrawable;
import com.portgo.view.ViewHolder;
import com.portgo.view.emotion.data.EmotionDataManager;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

public class MessageAdapter extends BaseAdapter {
    private LayoutInflater mInflater;
    private Context mContext;
    List<ChatSessionForShow> mMessages=null;
    int drawableWidth,drawableHeight,textSize;
    public MessageAdapter(Context context,List<ChatSessionForShow> messages ) {
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
            convertView = mInflater.inflate(R.layout.activity_messages_item, null);
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
        ChatSessionForShow event = (ChatSessionForShow) getItem(position);
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
        return ((ChatSessionForShow)getItem(i)).getSession().getId();
    }

    private void updateMesaageItemView(ViewHolder.MessageViewHolder holder, ChatSessionForShow sessionForShow, ListView parent){

        ChatSession session = sessionForShow.getSession();
        int unSeenCount = session.getCount();

        String DisName = session.getDisplayName();
        long messageTime = session.getMessageTime();
        String content = session.getStatus();
        int contactId = session.getContactid();

        final String date = DateTimeUtils.getFriendlyDateString(new Date(messageTime), mContext);
        holder.tvDate.setText(date);
        EmotionDataManager manager = EmotionDataManager.getInstance();
        content = NgnStringUtils.isNullOrEmpty(content) ? NgnStringUtils.emptyValue() : content;
        holder.tvContent.setText(manager.getSpanelText(content,holder.tvContent));
//
        if(unSeenCount>0) {
            String unseeNum="";
            if(unSeenCount>99) {
                unseeNum+="..";
            }else{
                unseeNum+=unSeenCount;
            }

            TextDrawable  drawable=TextDrawable.builder().beginConfig()
                    .textColor(Color.WHITE)
                    .useFont(Typeface.SERIF)
                    .fontSize(textSize)
                    .bold()
                    .toUpperCase()
                    .height(drawableWidth)
                    .width(drawableWidth)
                    .endConfig()
                    .buildRound(""+unseeNum, mContext.getResources().getColor(R.color.portgo_color_red));
            holder.tvRemote.setCompoundDrawablesWithIntrinsicBounds(null,null,drawable,null);
        }else{
            holder.tvRemote.setCompoundDrawablesWithIntrinsicBounds(null,null,null,null);
        }
        Bitmap avatar = null;
        Contact contact = sessionForShow.getAttachContact();
        if(contact!=null) {
            DisName = contact.getDisplayName();
            avatar =contact.getAvatar();
        }
        if(avatar!=null){
            holder.imgAvatar.setImageBitmap(avatar);
            holder.imgAvatar.setVisibility(View.VISIBLE);
            holder.tvAvatar.setVisibility(View.GONE);
        }else{
            holder.imgAvatar.setVisibility(View.GONE);
            holder.tvAvatar.setVisibility(View.VISIBLE);
            if(TextUtils.isEmpty(DisName)){
                DisName = session.getDisplayName();
            }
            holder.tvAvatar.setText(NgnStringUtils.getAvatarText(DisName));
        }
        holder.tvRemote.setText(DisName);
        if(parent.getChoiceMode()== AbsListView.CHOICE_MODE_MULTIPLE){
            holder.message_item_radiobox.setVisibility(View.VISIBLE);
            if(isItemChecked(session.getId())){
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
