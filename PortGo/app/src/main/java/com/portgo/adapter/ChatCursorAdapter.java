package com.portgo.adapter;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.CheckBox;
import android.widget.CursorAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.portgo.PortApplication;
import com.portgo.R;
import com.portgo.database.DBHelperBase;
import com.portgo.manager.MessageEvent;
import com.portgo.util.DateTimeUtils;
import com.portgo.util.MIMEType;
import com.portgo.view.MessageView;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;

public class ChatCursorAdapter extends CursorAdapter{
    Context mContext;
    LinkedList<MessageEvent> messageList = new LinkedList<>();

    public ChatCursorAdapter(Context context, Cursor c, int flag) {
        super(context, c, flag);
        mContext = context;
    }
    long playAduioId = -1;
    public void setOnPlayAudioItem(long playItem){
        playAduioId = playItem;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        Cursor cursor = (Cursor) getItem(position);
        MessageEvent cur = null,pre=null,next=null;
        cur = MessageEvent.messageFromCursor(cursor);
        if(!cursor.isFirst()){
            cursor.moveToPrevious();
            pre = MessageEvent.messageFromCursor(cursor);
            cursor.moveToNext();//current
        }

        if(!cursor.isLast()){
            cursor.moveToNext();
            next = MessageEvent.messageFromCursor(cursor);
        }
        convertView = createMessageView(pre,cur,next);
        CheckBox chat_radioBox = (CheckBox) convertView.findViewById(R.id.chat_item_radiobox);
        if(((ListView)parent).getChoiceMode()== AbsListView.CHOICE_MODE_MULTIPLE){
            chat_radioBox.setVisibility(View.VISIBLE);
            chat_radioBox.setChecked(isItemChecked(cur.getId()));
        }else{
            chat_radioBox.setVisibility(View.GONE);
        }

        return convertView;
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup viewGroup) {
        return null;
    }

    @Override
    public int getCount(){
        int count =super.getCount();
        return count;
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {//因为每条消息的界面几乎都不一样，所以就不复用，每次重建
        return ;
    }

    final int TIME_INTERVAL = 3;

    public View createMessageView(MessageEvent pre, MessageEvent cur, MessageEvent next) {
        View view = null;
        if(cur == null){
            return null;
        }
        final String mime = cur.getMime();
        String content;
        MessageView.Builder builder = new MessageView.Builder(mContext);
        if(MIMEType.MIMETYPE_textplain.equals(mime)) {
            content =cur.getContent();
            builder.setMessageType(MIMEType.MIMETYPE_textplain);
        }else if(MIMEType.MIMETYPE_audiompeg.equals(mime)||MIMEType.MIMETYPE_audioamr.equals(mime)||MIMEType.MIMETYPE_audiowav.equals(mime)){
            content =cur.getContent();
            builder.setMessageType(MIMEType.MIMETYPE_audiompeg);
            builder.setMessageDuration(cur.getMessageDuration());
            builder.setMessageReadstatus(cur.isMessageRead());

            builder.setAudioPlayStatus(playAduioId == cur.getSMSId());
        }else if(MIMEType.MIMETYPE_videompeg.equals(mime)||MIMEType.MIMETYPE_videomp4.equals(mime)) {
            content =cur.getContent();
            builder.setMessageType(MIMEType.MIMETYPE_videompeg);
        }else if(MIMEType.MIMETYPE_imagejpeg.equals(mime)) {
            content =cur.getContent();
            builder.setMessageType(MIMEType.MIMETYPE_imagejpeg);
        }else{
            content = mContext.getString(R.string.unknow_message_format);
        }

        final boolean bSent = cur.getSendOut();
        final boolean sucess = cur.getMessageStatus()== MessageEvent.MessageStatus.SUCCESS;

        builder.setLocal(bSent).setMessage(content);

        int timeIntervalPre = TIME_INTERVAL;
        int timeIntervalNext = TIME_INTERVAL;
        if(bSent){
            builder.setSendSuccess(sucess);
        }
        if(pre!=null){
            timeIntervalPre =  (int)(cur.getMessageTime()-pre.getMessageTime())/(1000*60);
        }
        if(next!=null){
            timeIntervalNext = (int)(next.getMessageTime()-cur.getMessageTime())/(1000*60);
        }

        if(timeIntervalPre>=TIME_INTERVAL){
            builder.setMessageTime(DateTimeUtils.getFriendlyDateString(new Date(cur.getMessageTime()),mContext));
        }
        if(bSent) {
            builder.setDisName(localTxAvartar);
        }else{
            if(TextUtils.isEmpty(remoteTxAvartar)) {
                builder.setDisName(cur.getDisplayName());
            }else{
                builder.setDisName(remoteTxAvartar);
            }
        }

        if(pre!=null&&pre.getSendOut()!=bSent){
            builder.setAddGap(true);
        }
        if((pre==null||pre.getSendOut()!=bSent||timeIntervalPre>=TIME_INTERVAL)&&!bSent){
            builder.setShowAvatar(true);
            if(!bSent&&remoteAvartar!=null){//remote
                builder.setAvatar(remoteAvartar);
            }
            if(bSent&&localAvartar!=null){//local
                builder.setAvatar(localAvartar);
            }
        }else{
            builder.setShowAvatar(false);
        }

        if(pre==null|| bSent!=pre.getSendOut()||timeIntervalPre>=TIME_INTERVAL){
            builder.setMessagePositon(MessageView.Builder.SHOW_FIRST);
        }else if(pre!=null&&next!=null&& (pre.getSendOut()==bSent&&bSent==next.getSendOut()
                &&timeIntervalNext<TIME_INTERVAL&&timeIntervalPre<TIME_INTERVAL)){
            builder.setMessagePositon(MessageView.Builder.SHOW_MIDDLE);
        }else{//
            builder.setMessagePositon(MessageView.Builder.SHOW_END);
        }

        view = builder.build();

        return view;
    }
    String localTxAvartar,remoteTxAvartar;
    Bitmap localAvartar=null,remoteAvartar=null;
    public void setUserAvartar(Bitmap local,String localText,Bitmap remote,String remoteText){
        localAvartar = local;
        remoteAvartar = remote;
        localTxAvartar = localText;
        remoteTxAvartar = remoteText;
    }


    HashMap<Long,Boolean> selectItems = new HashMap();
    public void setItemCheck(View view,long id,boolean check){
        if(check) {
            selectItems.put(id, check);
        }else {
            selectItems.remove(id);
        }
        if(view!=null){
            CheckBox box = (CheckBox) view.findViewById(R.id.chat_item_radiobox);
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

    public void setSelectALL(){
        selectItems.clear();
        int count  = getCount();
        for (int i=0;i<count;i++) {
            long id =  getItemId(i);
            selectItems.put(id, true);
        }
    }

    public boolean isAllSelect(){
        int count  = getCount();
        for (int i=0;i<count;i++){
            long id = getItemId(i);
            if(!selectItems.containsKey(id)){
                return false;
            }
        }
        return true;
    }

    public void setSelectNone(){
        selectItems.clear();
    }

    public void setSelectItems(ArrayList<Long> items){
        selectItems.clear();
        for (Long id:items) {
            selectItems.put(id,true);
        }
    }

    int playitem = -1;
}