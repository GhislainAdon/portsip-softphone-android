package com.portgo.adapter;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.drawable.AnimationDrawable;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import android.text.TextUtils;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.CheckBox;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;


import com.bumptech.glide.Glide;
import com.bumptech.glide.RequestBuilder;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.RequestOptions;
import com.portgo.R;
import com.portgo.adapter.recycle.RecyclerViewCursorAdapter;

import com.portgo.manager.MessageEvent;
import com.portgo.util.DateTimeUtils;
import com.portgo.util.NgnStringUtils;
import com.portgo.util.UnitConversion;
import com.portgo.view.RoundedImageView;
import com.portgo.view.emotion.data.EmotionDataManager;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Observable;
import java.util.Observer;

/**
 * Created by zoom on 2016/3/30.
 */
public class ChatRecyclerCursoAdapter extends RecyclerViewCursorAdapter<ChatRecyclerCursoAdapter.MessageViewHolder> implements View.OnCreateContextMenuListener{
    final int TIME_INTERVAL = 3;
    private LayoutInflater inflater;
    final int minwidth = 200,minheight = 200,audiominwidth = 60,audiowidth = 160;
    final int DefaultWidth= 240,DefaultHeight=320;
    long contextMenuId = -1;
    View.OnCreateContextMenuListener contextMenuListener;
    OnMessageViewClickLister mMessageViewClickLister;
    PlayObservable PlayOber =  new PlayObservable();
    final int maxLen;

//    private Drawable failPic,drawableUnknow,drawableDir,drawablePng,drawableDoc,drawableMusic,drawablePPT,drawablePDF,drawableMov,drawableTXT;
    public interface OnMessageViewClickLister{
        void onMessageViewClick(View view,long messageid,long messageRowId,long messageTime,String mimeType,boolean read, boolean local,JSONObject content, MessageEvent.MessageStatus staus);
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        if(contextMenuListener!=null){
            menuInfo = new AdapterView.AdapterContextMenuInfo(v,0,contextMenuId);
            contextMenuListener.onCreateContextMenu(menu,v,menuInfo);
        }
    }

    public void setOnMessageViewClickLister(OnMessageViewClickLister messageViewClickLister) {
        mMessageViewClickLister = messageViewClickLister;
    }

    public long getContextMenuId(){
        return contextMenuId;
    }
    public void setonCreateContextMenuListener(View.OnCreateContextMenuListener listener){
        contextMenuListener = listener;
    }

    public enum ITEM_TYPE {
        ITEM_TYPE_IMAGE_SEND,
        ITEM_TYPE_IMAGE_RCV,
        ITEM_TYPE_TEXT_SEND,
        ITEM_TYPE_TEXT_RCV,
        ITEM_TYPE_AUDIO_SEND,
        ITEM_TYPE_AUDIO_RCV,
        ITEM_TYPE_VIDEO_SEND,
        ITEM_TYPE_VIDEO_RCV,
        ITEM_TYPE_FILE_SEND,
        ITEM_TYPE_FILE_RCV,
    }
    static ITEM_TYPE getItemType(int ordinal){
        if(ordinal>=0&&ITEM_TYPE.values().length>ordinal){
            return ITEM_TYPE.values()[ordinal];
        }
        return null;
    }

    Context mContext;
//    String videoPath,thumbnailPath;
    public ChatRecyclerCursoAdapter(Context context, Cursor c, int flags) {
        super(context, c, flags);
        mContext = context;
        inflater= LayoutInflater.from(context);
        maxLen = UnitConversion.getScreenWidth(context)*3/5;
//        videoPath = ConfigurationManager.getInstance().getStringValue(mContext, ConfigurationManager.PRESENCE_VIDEO_PATH, mContext.getExternalFilesDir(null).getAbsolutePath());
//        thumbnailPath =videoPath+ File.separator+"thumbnails";

    }

    @Override
    public void onBindViewHolder(MessageViewHolder holder, Cursor cursor) {
        int timeIntervalPre = TIME_INTERVAL;

        MessageEvent preEvent,curEvent,nextEvent=null;
        if(cursor.isFirst()){
            preEvent = null;
        }
        else{
            cursor.moveToPrevious();
            preEvent = MessageEvent.messageFromCursor(cursor);
            cursor.moveToNext();
        }
        curEvent = MessageEvent.messageFromCursor(cursor);
        if(cursor.moveToNext()){
            nextEvent = MessageEvent.messageFromCursor(cursor);
            cursor.moveToPrevious();//将光标放回当前位置
        }

        if(preEvent!=null){
            timeIntervalPre =  (int)(curEvent.getMessageTime()-preEvent.getMessageTime())/(1000*60);
        }
        holder.setMessageID(curEvent.getSMSId());
        if(selectItems.containsKey(curEvent.getSMSId())){
            holder.setMessageSelect(true);
        }

        //和上一条的时间超过时间间隔，需要显示时间
        if(timeIntervalPre>=TIME_INTERVAL){
            holder.setMessageTime(curEvent.getMessageTime(),true);
        }else {
            holder.setMessageTime(curEvent.getMessageTime(),false);
        }
        //和上一条的类型不同，需要添加间隔
        if(preEvent!=null&&preEvent.getSendOut()!=curEvent.getSendOut()){
            holder.setNeedGap(true);
        }

        holder.setSelectMode(mSelectMode);
        boolean needAvatar = getNeedShowAvatar(preEvent,curEvent);
        int type = getContentBackGroundType(preEvent,curEvent,nextEvent);

        holder.setMessageStatus(curEvent.getMessageStatus(),curEvent.isMessageRead());
        String mime = curEvent.getMime();
        JSONObject content = curEvent.getJsonContent();

        if(holder instanceof TextSendHolder){
            holder.setContent(mime,content,0);
            holder.setOnCreateContextMenuListener(ChatRecyclerCursoAdapter.this);
            holder.setBackGroudRes(type);
        }else if(holder instanceof TextRecvHolder){
            ((TextRecvHolder) holder).showAvartar(needAvatar);
            holder.setContent(mime,content,0);
            holder.setOnCreateContextMenuListener(ChatRecyclerCursoAdapter.this);
            holder.setBackGroudRes(type);
        }else if(holder instanceof AudioSendHolder){
            holder.setContent(mime,content,curEvent.getMessageDuration());
            holder.setOnCreateContextMenuListener(ChatRecyclerCursoAdapter.this);
            holder.setBackGroudRes(type);
            PlayOber.notifyAllObj();
        }else if(holder instanceof AudioRecvHolder){
            ((AudioRecvHolder) holder).showAvartar(needAvatar);
            holder.setContent(mime,content,curEvent.getMessageDuration());
            holder.setOnCreateContextMenuListener(ChatRecyclerCursoAdapter.this);
            holder.setBackGroudRes(type);
            PlayOber.notifyAllObj();
        }else if(holder instanceof ImageSendHolder){
            holder.setContent(mime,content,0);
            ((ImageSendHolder)holder).setOnCreateContextMenuListener(ChatRecyclerCursoAdapter.this);
            holder.setBackGroudRes(MessageViewHolder.SHOW_NONE);
        }else if(holder instanceof ImageRecvHolder){
            ((ImageRecvHolder) holder).showAvartar(needAvatar);
            holder.setOnCreateContextMenuListener(ChatRecyclerCursoAdapter.this);
            holder.setContent(mime,content,0);
            holder.setBackGroudRes(MessageViewHolder.SHOW_NONE);
        }else if(holder instanceof VideoSendHolder){
            holder.setContent(mime,content,curEvent.getMessageDuration());
            ((VideoSendHolder)holder).setOnCreateContextMenuListener(ChatRecyclerCursoAdapter.this);
            holder.setBackGroudRes(MessageViewHolder.SHOW_NONE);
        }else if(holder instanceof VideoRecvHolder){
            ((VideoRecvHolder) holder).showAvartar(needAvatar);
            ((VideoRecvHolder) holder).setContent(mime,content,curEvent.getMessageDuration());
            holder.setOnCreateContextMenuListener(ChatRecyclerCursoAdapter.this);
            holder.setBackGroudRes(MessageViewHolder.SHOW_NONE);
        }
        else if(holder instanceof FileSendHolder){
//            ((FileSendHolder) holder).showAvartar(needAvatar);
            holder.setContent(mime,content,curEvent.getMessageDuration());
            holder.setOnCreateContextMenuListener(ChatRecyclerCursoAdapter.this);
            holder.setBackGroudRes(type);
        }
        else if(holder instanceof FileRecvHolder){
            ((FileRecvHolder) holder).showAvartar(needAvatar);
            holder.setContent(mime,content,curEvent.getMessageDuration());
            holder.setOnCreateContextMenuListener(ChatRecyclerCursoAdapter.this);
            holder.setBackGroudRes(type);
        }
    }


    private boolean getNeedShowAvatar(MessageEvent preEvent,MessageEvent curEvent) {
        boolean need =false;
        boolean sendOut = curEvent.getSendOut();
        int timeIntervalPre = TIME_INTERVAL;
        if(preEvent!=null){
            timeIntervalPre =  (int)(curEvent.getMessageTime()-preEvent.getMessageTime())/(1000*60);
        }

        if (!sendOut) {
            if(preEvent == null || preEvent.getSendOut() != sendOut || timeIntervalPre >= TIME_INTERVAL) {
                need = true;
            }
        }else{
            need = false;
        }
        return need;
    }

    private int getContentBackGroundType(MessageEvent preEvent,MessageEvent curEvent,MessageEvent nextEvent){
        int timeIntervalPre = TIME_INTERVAL;
        int timeIntervalNext = TIME_INTERVAL;

        if(preEvent!=null){
            timeIntervalPre =  (int)(curEvent.getMessageTime()-preEvent.getMessageTime())/(1000*60);
        }
        if(nextEvent!=null){
            timeIntervalNext = (int)(nextEvent.getMessageTime()-curEvent.getMessageTime())/(1000*60);
        }
        int bakType = MessageViewHolder.SHOW_FIRST;
        boolean sendOut = curEvent.getSendOut();

        if(preEvent==null|| sendOut!=preEvent.getSendOut()||timeIntervalPre>=TIME_INTERVAL){
            bakType = MessageViewHolder.SHOW_FIRST;//
        }else if(preEvent!=null&&nextEvent!=null&& (preEvent.getSendOut()==sendOut&&sendOut==nextEvent.getSendOut()
                &&timeIntervalNext<TIME_INTERVAL&&timeIntervalPre<TIME_INTERVAL)){
            bakType = MessageViewHolder.SHOW_MIDDLE;//
        }else{//
            bakType = MessageViewHolder.SHOW_END;
        }
        return bakType;
    }

    @Override
    public int getItemViewType(int position) {
        Cursor cursor = getCursor();
        cursor.moveToPosition(position);
        MessageEvent event = MessageEvent.messageFromCursor(cursor);
        JSONObject jsonObject = event.getJsonContent();
        boolean local = event.getSendOut();
        String messageType;
        try {
            messageType =jsonObject.getString(MessageEvent.KEY_MESSAGE_TYPE);
        } catch (JSONException e) {
            e.printStackTrace();
            messageType = MessageEvent.MESSAGE_TYPE_TEXT;
        }
        // text>audio>image>video
        if(MessageEvent.MESSAGE_TYPE_TEXT.equals(messageType)){
            return local?ITEM_TYPE.ITEM_TYPE_TEXT_SEND.ordinal():ITEM_TYPE.ITEM_TYPE_TEXT_RCV.ordinal();
        }else if(MessageEvent.MESSAGE_TYPE_AUDIO.equals(messageType)){
            return local?ITEM_TYPE.ITEM_TYPE_AUDIO_SEND.ordinal():ITEM_TYPE.ITEM_TYPE_AUDIO_RCV.ordinal();
        }else if(MessageEvent.MESSAGE_TYPE_IMAGE.equals(messageType)){
            return local?ITEM_TYPE.ITEM_TYPE_IMAGE_SEND.ordinal():ITEM_TYPE.ITEM_TYPE_IMAGE_RCV.ordinal();
        }else if(MessageEvent.MESSAGE_TYPE_VIDEO.equals(messageType)){
            return local?ITEM_TYPE.ITEM_TYPE_VIDEO_SEND.ordinal():ITEM_TYPE.ITEM_TYPE_VIDEO_RCV.ordinal();
        }else if(MessageEvent.MESSAGE_TYPE_FILE.equals(messageType)){
            return local?ITEM_TYPE.ITEM_TYPE_FILE_SEND.ordinal():ITEM_TYPE.ITEM_TYPE_FILE_RCV.ordinal();
        } else{
            return local?ITEM_TYPE.ITEM_TYPE_TEXT_SEND.ordinal():ITEM_TYPE.ITEM_TYPE_TEXT_RCV.ordinal();
        }
    }

    @Override
    protected void onContentChanged() {}

    @Override
    public MessageViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        MessageViewHolder holder = null;
        View view=null,content = null;
        LinearLayout llContent = null;
        switch(getItemType(viewType)){
            case ITEM_TYPE_IMAGE_SEND:
                view = inflater.inflate(R.layout.view_message_sent,parent,false);
                llContent = view.findViewById(R.id.message_content);

                content = inflater.inflate(R.layout.view_message_sent_image,null);
                llContent.addView(content);
                holder = new ImageSendHolder(view);
                break;
            case ITEM_TYPE_IMAGE_RCV:
                view = inflater.inflate(R.layout.view_message_recv,parent,false);
                llContent = view.findViewById(R.id.message_content);

                content = inflater.inflate(R.layout.view_message_recv_image,null);
                llContent.addView(content);
                holder = new ImageRecvHolder(view);
                break;
            case ITEM_TYPE_TEXT_SEND:
                view = inflater.inflate(R.layout.view_message_sent,parent,false);
                llContent = view.findViewById(R.id.message_content);

                content = inflater.inflate(R.layout.view_message_sent_text,null);
                llContent.addView(content);
                holder = new TextSendHolder(view);
                break;
            case ITEM_TYPE_TEXT_RCV:
                view = inflater.inflate(R.layout.view_message_recv,parent,false);
                llContent = view.findViewById(R.id.message_content);

                content = inflater.inflate(R.layout.view_message_recv_text,null,false);
                llContent.addView(content);
                holder = new TextRecvHolder(view);
                break;
            case ITEM_TYPE_AUDIO_SEND:
                view = inflater.inflate(R.layout.view_message_sent,parent,false);
                llContent = view.findViewById(R.id.message_content);

                content = inflater.inflate(R.layout.view_message_sent_audio,null);
                llContent.addView(content);
                holder = new AudioSendHolder(view);
                break;
            case ITEM_TYPE_AUDIO_RCV:
                view = inflater.inflate(R.layout.view_message_recv,parent,false);
                llContent = view.findViewById(R.id.message_content);

                content = inflater.inflate(R.layout.view_message_recv_audio,null);
                llContent.addView(content);
                holder = new AudioRecvHolder(view);
                break;
            case ITEM_TYPE_VIDEO_SEND:
                view = inflater.inflate(R.layout.view_message_sent,parent,false);
                llContent = view.findViewById(R.id.message_content);

                content = inflater.inflate(R.layout.view_message_sent_video,null);
                llContent.addView(content);
                holder = new VideoSendHolder(view);
                break;
            case ITEM_TYPE_VIDEO_RCV:
                view = inflater.inflate(R.layout.view_message_recv,parent,false);
                llContent = view.findViewById(R.id.message_content);

                content = inflater.inflate(R.layout.view_message_recv_video,null);
                llContent.addView(content);
                holder = new VideoRecvHolder(view);
                break;

            case ITEM_TYPE_FILE_SEND:
                view = inflater.inflate(R.layout.view_message_sent,parent,false);
                llContent = view.findViewById(R.id.message_content);

                content = inflater.inflate(R.layout.view_message_sent_file,null);
                llContent.addView(content);
                holder = new FileSendHolder(view);
                break;
            case ITEM_TYPE_FILE_RCV:
                view = inflater.inflate(R.layout.view_message_recv,parent,false);
                llContent = view.findViewById(R.id.message_content);

                content = inflater.inflate(R.layout.view_message_recv_file,null);
                llContent.addView(content);
                holder = new FileRecvHolder(view);
                break;
        }
//        holder.setOnCreateContextMenuListener(this);

        return holder;
    }

    //recycleview
    //
    public class AudioSendHolder extends SentMessageViewHolder implements View.OnClickListener,Observer {
        TextView mTVPlayer;
        float audioDuration;

        AnimationDrawable anim;
        AudioSendHolder(View view){
            super(view);
            mTVPlayer = view.findViewById(R.id.message_sent_audio_player);
            mTVPlayer.setOnClickListener(this);
            Drawable[] drawables = mTVPlayer.getCompoundDrawables();
            anim = (AnimationDrawable)drawables[2];
            PlayOber.addObserver(this);
        }

        public void setOnCreateContextMenuListener(View.OnCreateContextMenuListener l) {
            mTVPlayer.setLongClickable(l==null?false:true);
            mTVPlayer.setOnLongClickListener(this);
            mTVPlayer.setOnCreateContextMenuListener(l);
        }

        @Override
        void setContent(String mime, JSONObject content,long msgLen) {
            mMIME =mime;
            mContent = content;

            try {
                audioDuration = content.getInt(MessageEvent.KEY_AV_DURATION);
            } catch (JSONException e) {
                audioDuration =msgLen;
            }
            float length = audiominwidth+(audioDuration/30)*audiowidth;
            final float scale = mContext.getResources().getDisplayMetrics().density;

            length = (length * scale + 0.5f);
            length = length>maxLen?maxLen:length;
            FrameLayout.LayoutParams layoutParams = (FrameLayout.LayoutParams) mTVPlayer.getLayoutParams();
            layoutParams.width = (int) length;
            mTVPlayer.setText(" "+(int)audioDuration+"\"");
            mTVPlayer.setLayoutParams(layoutParams);
        }

        @Override
        public void onClick(View v) {
            if(mMessageViewClickLister!=null){
                mMessageViewClickLister.onMessageViewClick(v,mMessageID,mMessageRowID,mMessageTime,mMIME,mRead,true,mContent,status);
            }
        }

        @Override
        public void update(Observable o, Object arg) {
            Long playId = (Long) arg;
            if(playId ==mMessageID){
                anim.start();
            }else{
                anim.selectDrawable(0);
                anim.stop();
            }
        }
    }

    long playID = -1;
    //
    public class AudioRecvHolder extends RecvMessageViewHolder implements View.OnClickListener,Observer{
        TextView mTVPlayer;
        float audioDuration;
        AnimationDrawable anim;
        AudioRecvHolder(View view) {
            super(view);
            mTVPlayer = view.findViewById(R.id.message_recv_audio_player);
            mTVPlayer.setOnClickListener(this);
            Drawable[]drawables  = mTVPlayer.getCompoundDrawables();
            anim = (AnimationDrawable)drawables[0];
            PlayOber.addObserver(this);
        }
        public void setOnCreateContextMenuListener(View.OnCreateContextMenuListener l) {
            mTVPlayer.setLongClickable(l==null?false:true);
            mTVPlayer.setOnLongClickListener(this);
            mTVPlayer.setOnCreateContextMenuListener(l);
        }
        @Override
        void setContent(String mime, JSONObject content,long msgLen){
            mMIME =mime;
            mContent = content;

            try {
                audioDuration = content.getInt(MessageEvent.KEY_AV_DURATION);
            } catch (JSONException e) {
                audioDuration =msgLen;
            }
            float length = audiominwidth+(audioDuration/30)*audiowidth;
            final float scale = mContext.getResources().getDisplayMetrics().density;

            length *= scale;
            length = length>maxLen?maxLen:length;
            FrameLayout.LayoutParams layoutParams = (FrameLayout.LayoutParams) mTVPlayer.getLayoutParams();
            layoutParams.width = (int) length;
            mTVPlayer.setText(" "+(int)audioDuration+"\"");
            mTVPlayer.setLayoutParams(layoutParams);
        }

        public void setMessageRead(boolean read) {
            if(read){
                mIVRead.setVisibility(View.INVISIBLE);
            }else {
                mIVRead.setVisibility(View.VISIBLE);
            }
            this.mRead = read;
        }

        @Override
        void setMessageStatus(MessageEvent.MessageStatus msgStatus, boolean read) {
            super.setMessageStatus(msgStatus, read);
            if(msgStatus== MessageEvent.MessageStatus.SUCCESS&&!read){
                mIVRead.setVisibility(View.VISIBLE);
            }
        }

        @Override
        public void onClick(View v) {
            if(mMessageViewClickLister!=null){
                mMessageViewClickLister.onMessageViewClick(v,mMessageID,mMessageRowID,mMessageTime,mMIME,mRead,false,mContent,status);
                setMessageRead(true);
            }
        }

        @Override
        public void update(Observable o, Object arg) {
            Long playId = (Long) arg;
            if(playId ==mMessageID){
                if(!mRead){
                    setMessageRead(true);
                }
                anim.start();
            }else{
                anim.selectDrawable(0);
                anim.stop();
            }
        }
    }
    class PlayObservable extends Observable {
        Long playid = -1L;
       public void setPlayMessageID(long playMsgid){
           playid = playMsgid;
            setChanged();
            notifyObservers(playid);
       }

        public void notifyAllObj(){
           setChanged();
           notifyObservers(playid);
        }
    }

    public  abstract class MessageViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener, View.OnLongClickListener {
        public static final int SHOW_FIRST =1;
        public static final int SHOW_MIDDLE =2;
        public static final int SHOW_END =3;
        public static final int SHOW_NONE =4;
        public long mMessageID=-1;
        public long mMessageTime=0;
        public long mMessageRowID=0;
        public JSONObject mContent;
        public String mMIME;
        public boolean mRead;
        int resBeging,resMid,resEnd;
        ImageView mIVFailed;
        TextView mTVTime,mTVGap;
        CheckBox mCBSelect;
        LinearLayout llContent;
        MessageEvent.MessageStatus status;
        ProgressBar progressBar;


        MessageViewHolder(View view) {
            super(view);
            mTVTime = view.findViewById(R.id.message_time);
            mTVGap = view.findViewById(R.id.message_gap);
            mCBSelect = view.findViewById(R.id.message_select);
            mCBSelect.setOnClickListener(this);
            llContent = view.findViewById(R.id.message_content);
            progressBar= view.findViewById(R.id.message_processing);
            mIVFailed  = view.findViewById(R.id.message_failed);

        }

        void setContent(String mime, JSONObject content, long msgLen){
            mMIME = mime;
            mContent = content;
        }
        void setMessageStatus(MessageEvent.MessageStatus msgStatus,boolean read){
            this.mRead = read;
            switch (msgStatus){
                case Failed:
                case ATTACH_FAILED:
                    mIVFailed.setVisibility(View.VISIBLE);
                    progressBar.setVisibility(View.INVISIBLE);
                    break;
                case SUCCESS:
                case ATTACH_SUCESS:
                    mIVFailed.setVisibility(View.INVISIBLE);
                    progressBar.setVisibility(View.INVISIBLE);
                    break;
                case PROCESSING:
                    progressBar.setVisibility(View.VISIBLE);
                    mIVFailed.setVisibility(View.INVISIBLE);
                    break;
            }
        }
        public void setSelectMode(boolean selectMode){
            mCBSelect.setVisibility(selectMode?View.VISIBLE:View.GONE);
        }

        public void setOnCreateContextMenuListener(View.OnCreateContextMenuListener l) {
            llContent.setLongClickable(l==null?false:true);
            llContent.setOnLongClickListener(this);
            llContent.setOnCreateContextMenuListener(l);
        }

        public void setMessageSelect(boolean check){
            //selectItems.put(mMessageID,check);
            setSelectMode(true);
        }

        public void setMessageTime(long time,boolean visibility){
            mMessageTime = time;
            if(visibility){
                String strTime= DateTimeUtils.getFriendlyDateString(new Date(time),mContext);
                mTVTime.setText(strTime);
            }
            mTVTime.setVisibility(visibility?View.VISIBLE:View.GONE);

        }
        public void setMessageID(long messageID){
            mMessageID = messageID;
            mMessageRowID = getItemId();
        }
        public void setNeedGap(boolean needGap){
            mTVGap.setVisibility(needGap?View.VISIBLE:View.GONE);
        }

        void setBackGroudRes(int type){
            switch (type){
                case SHOW_FIRST:
                    llContent.setBackgroundResource(resBeging);
                    break;
                case SHOW_MIDDLE:
                    llContent.setBackgroundResource(resMid);
                    break;
                case SHOW_END:
                    llContent.setBackgroundResource(resEnd);
                    break;
                case SHOW_NONE:
                    llContent.setBackgroundResource(android.R.color.transparent);
                    break;
            }
        }

        @Override
        public void onClick(View v) {
            if(mMessageViewClickLister!=null){
                mMessageViewClickLister.onMessageViewClick(v,mMessageID,mMessageRowID,mMessageTime,mMIME,mRead,false,mContent,status);
            }
//            setMessageSelect(!mCBSelect.isChecked());
        }

        @Override
        public boolean onLongClick(View v) {
            contextMenuId = getItemId();
            return false;

        }
    }
    public void  sendAudioPlayMessage(Long mesageId){
        PlayOber.setPlayMessageID(mesageId);
    }
    //recycleview
    //发送的消息
    abstract class SentMessageViewHolder extends MessageViewHolder{


        SentMessageViewHolder(View view) {
            super(view);
            resBeging = R.drawable.message_recive_begin_bk;
            resMid = R.drawable.message_recive_middle_bk;
            resEnd = R.drawable.message_recive_end_bk;

        }

        @Override
        void setMessageStatus(MessageEvent.MessageStatus msgStatus,boolean read) {
            this.status = msgStatus;
            super.setMessageStatus(msgStatus,read);
        }
    }

    //接收的消息
    abstract class RecvMessageViewHolder extends MessageViewHolder {
        ImageView mIVRead;
        FrameLayout flAvartar;
        RecvMessageViewHolder(View view) {
            super(view);
            resBeging = R.drawable.message_send_begin_bk;
            resMid = R.drawable.message_send_middle_bk;
            resEnd = R.drawable.message_send_end_bk;
            mIVRead = view.findViewById(R.id.message_read);
            flAvartar = view.findViewById(R.id.message_avatar);
        }
        void showAvartar(boolean show){
            if(show){
                RoundedImageView imgAvatar =  flAvartar.findViewById(R.id.user_avatar_image);
                TextView textAvatar = flAvartar.findViewById(R.id.user_avatar_text);
                if(remoteAvartar==null) {
                    imgAvatar.setVisibility(View.GONE);
                    textAvatar.setVisibility(View.VISIBLE);
                    textAvatar.setText(remoteTxAvartar);
                }else{
                    imgAvatar.setImageDrawable(remoteAvartar);
                    imgAvatar.setVisibility(View.VISIBLE);
                    textAvatar.setVisibility(View.GONE);
                }
            }
            flAvartar.setVisibility(show?View.VISIBLE:View.INVISIBLE);
        }

        @Override
        void setMessageStatus(MessageEvent.MessageStatus msgStatus,boolean read) {
            this.status = msgStatus;
            super.setMessageStatus(status,read);
        }

    }


    //recycleview
    //发送的文字
    public class TextSendHolder extends SentMessageViewHolder{
        TextView mTVContent;
        TextSendHolder(View view) {
            super(view);
            mTVContent = view.findViewById(R.id.message_sent_text_content);
        }

        @Override
        void setContent(String mime, JSONObject content,long msgLen) {
            super.setContent(mime,content,msgLen);

//            mTVContent.setText(content);

            EmotionDataManager manager = EmotionDataManager.getInstance();
            String text;
            try {
                text = content.getString(MessageEvent.KEY_TEXT_CONTENT);
            } catch (JSONException e) {
                text="";
            }
            mTVContent.setText(manager.getSpanelText(text,mTVContent));
            mTVContent.requestLayout();//防止控件复用导致的textview长度不对,控件长度比实际文字表情需要的长度长很多
        }

    }

    //接收的文字
    public class TextRecvHolder extends RecvMessageViewHolder{
        TextView mTVContent;
        TextRecvHolder(View view) {
            super(view);
            mTVContent = view.findViewById(R.id.message_recv_text_content);
        }

        @Override
        void setContent(String mime, JSONObject content,long msgLen) {
            super.setContent(mime,content,msgLen);
//            mTVContent.setText(content);
            EmotionDataManager manager = EmotionDataManager.getInstance();
            String text;
            try {
                text = content.getString(MessageEvent.KEY_TEXT_CONTENT);
            } catch (JSONException e) {
                text="";
            }
            mTVContent.setText(manager.getSpanelText(text, mTVContent));
            mTVContent.requestLayout();//防止控件复用导致的textview长度不对,控件长度比实际文字表情需要的长度长很多
        }

    }

    //recycleview
    //发送的文字
    public class FileSendHolder extends SentMessageViewHolder{
        TextView mTVFileName,mTvFileDesc;
        ImageView mIVFileIcon;

        FileSendHolder(View view) {
            super(view);
            view.findViewById(R.id.message_sent_file_rl).setOnClickListener(this);
            mTVFileName = view.findViewById(R.id.message_sent_file_name);
            mTvFileDesc = view.findViewById(R.id.message_sent_file_desc);
            mIVFileIcon = view.findViewById(R.id.message_sent_file_icon);
        }

        @Override
        void setContent(String mime, JSONObject content,long msgLen) {
            super.setContent(mime,content,msgLen);
            String path,fileName;
            long fileSize = 0;
            try {
                fileName = content.getString(MessageEvent.KEY_FILE_NAME);
                path = content.getString(MessageEvent.KEY_FILE_PATH);
                fileSize = content.getLong(MessageEvent.KEY_FILE_SIZE);
            } catch (JSONException e) {
                path="";
                fileName ="";
                fileSize = 0;
            }

            mTVFileName.setText(fileName);
            if(fileSize==0) {
                File file = new File(path);
                if (file != null && file.exists()) {
                    fileSize = file.length();
                }
            }
            mTvFileDesc.setText(NgnStringUtils.getReadAbleSize(fileSize));
            setFileDrawable(this,fileName,path);
        }

    }

    //
    public class FileRecvHolder extends RecvMessageViewHolder{
        TextView mTVFileName,mTvFileDesc;
        ImageView mIVFileIcon;

        FileRecvHolder(View view) {
            super(view);
            view.findViewById(R.id.message_recv_file_rl).setOnClickListener(this);
            view.setOnLongClickListener(this);
            mTVFileName = view.findViewById(R.id.message_recv_file_name);
            mTvFileDesc = view.findViewById(R.id.message_recv_file_desc);
            mIVFileIcon = view.findViewById(R.id.message_recv_file_icon);
        }

        @Override
        void setContent(String mime, JSONObject content,long msgLen) {
            super.setContent(mime,content,msgLen);
            String path,fileName;
            long fileSize;
            try {
                fileName = content.getString(MessageEvent.KEY_FILE_NAME);
                path = content.getString(MessageEvent.KEY_FILE_PATH);
                fileSize = content.getLong(MessageEvent.KEY_FILE_SIZE);
            } catch (JSONException e) {
                path="";
                fileName = "";
                fileSize = 0;
            }

            mTVFileName.setText(fileName);
            if(fileSize==0) {
                File file = new File(path);
                if (file != null && file.exists()) {
                    fileSize = file.length();
                }
            }
            mTvFileDesc.setText(NgnStringUtils.getReadAbleSize(fileSize));
            setFileDrawable(this,fileName,path);
        }
    }

    void setFileDrawable(@NonNull MessageViewHolder holder, @NonNull String name,String filePath){
        int result;
        RequestBuilder requestBuilder =null;
        if(TextUtils.isEmpty(name)){
            result = R.drawable.file_folder;
            requestBuilder = Glide.with(mContext).load(result);
        }else{
            int indexExt = name.lastIndexOf(".");
            String extName = null;
            if(indexExt!=-1&&indexExt<name.length()){
                extName = name.substring(indexExt+1);
            }


            if(TextUtils.isEmpty(extName)){
                result = R.drawable.file_unknow;
                requestBuilder = Glide.with(mContext).load(result);
            }else if(extName.equalsIgnoreCase("MP3")||extName.equalsIgnoreCase("arm")||extName.equalsIgnoreCase("wav")){
                result = R.drawable.file_music;
                requestBuilder = Glide.with(mContext).load(result);
            }else if(extName.equalsIgnoreCase("MP4")){
                result = R.drawable.file_movi;
                requestBuilder = Glide.with(mContext).load(result);
            }else if(extName.equalsIgnoreCase("jpg")||extName.equalsIgnoreCase("jpeg")||extName.equalsIgnoreCase("png")){
//                result = R.drawable.file_image;
                requestBuilder = Glide.with(mContext).load(new File(filePath))
                        .apply(new RequestOptions().diskCacheStrategy(DiskCacheStrategy.NONE).error(R.drawable.file_image).centerCrop());
            }else if(extName.equalsIgnoreCase("ppt")){
                result = R.drawable.file_ppt;
                requestBuilder = Glide.with(mContext).load(result);
            }else if(extName.equalsIgnoreCase("pdf")){
                result = R.drawable.file_pdf;
                requestBuilder = Glide.with(mContext).load(result);
            }else if(extName.equalsIgnoreCase("txt")||extName.equalsIgnoreCase("html")
                    ||extName.equalsIgnoreCase("log")||extName.equalsIgnoreCase("ini")){
                result = R.drawable.file_txt;
                requestBuilder = Glide.with(mContext).load(result);
            }else{
                result = R.drawable.file_unknow;
                requestBuilder = Glide.with(mContext).load(result);
            }
        }
        if(requestBuilder!=null) {
            if (holder instanceof FileRecvHolder) {
                requestBuilder.into(((FileRecvHolder) holder).mIVFileIcon);
            } else if (holder instanceof FileSendHolder) {
                requestBuilder.into(((FileSendHolder) holder).mIVFileIcon);
            }
        }
    }

    //recycleview
    public class VideoSendHolder extends SentMessageViewHolder implements View.OnClickListener {
        ImageView mIVContent,mIVPlayer;
        VideoSendHolder(View view) {
            super(view);
            mIVContent = view.findViewById(R.id.message_video_sent_content);
            mIVPlayer = view.findViewById(R.id.message_video_sent_play);
        }
        @Override
        public void setOnCreateContextMenuListener(View.OnCreateContextMenuListener l) {
            mIVContent.setLongClickable(l==null?false:true);
            mIVContent.setOnLongClickListener(this);
            mIVContent.setOnCreateContextMenuListener(l);
            mIVPlayer.setLongClickable(false);
        }
        @Override
        void setContent(String mime, JSONObject content,long msgLen) {
            super.setContent(mime,content,msgLen);

            String filePath ="",fileName = "";
            int height=0,width=0;
            try {
                filePath = content.getString(MessageEvent.KEY_FILE_PATH);
            } catch (JSONException e) {
                filePath="";
            }
            try {
                width = content.getInt(MessageEvent.KEY_RESOLUTION_WIDTH);
                height = content.getInt(MessageEvent.KEY_RESOLUTION_HEIGHT);
            }catch (JSONException e) {
                width =height=0 ;
            }

            File file = new File(filePath);

            if (!file.exists()) {
                RequestOptions options = new RequestOptions() .override(162, 125);
                Glide.with(mContext).load(R.drawable.pic_failed).apply(options).into(mIVContent);
            } else {
                height= (height==0?DefaultWidth:height);
                width= (width==0?DefaultHeight:width);

                if (height< minheight && width < minwidth) {
                    float hRate = height / (float) minheight;
                    float wRate = width / (float) minwidth;
                    if (wRate < hRate) {
                        width = (int) (width / hRate);
                        height = minheight;
                    } else {
                        width = minwidth;
                        height = (int) (height / wRate);
                    }
                }
                //.placeholder()
                RequestOptions options = new RequestOptions().fitCenter().diskCacheStrategy(DiskCacheStrategy.AUTOMATIC);
                options.override(width,height).error(R.drawable.pic_failed);
                Glide.with(mContext).load(Uri.fromFile( file )).apply(options).into(mIVContent);
            }
            mIVPlayer.setVisibility(View.VISIBLE);
            mIVPlayer.setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {
            if(mMessageViewClickLister!=null){
                mMessageViewClickLister.onMessageViewClick(v,mMessageID,mMessageRowID,mMessageTime,mMIME,mRead,true,mContent,status);
            }
//            ActivityOptionsCompat optionsCompat = ActivityOptionsCompat.makeScaleUpAnimation(mIVContent,
//                    mIVContent.getWidth()/2,mIVContent.getHeight()/2,0,0);//
//
//            Intent playIntent = new Intent(mContext, PortActivityVideoPlayer.class);
//            playIntent.putExtra(PortActivityVideoPlayer.PLAYER_NAME,mContent);
//            ActivityCompat.startActivity(mContext,playIntent,optionsCompat.toBundle());
        }

    }

    //接收的视频
    public class VideoRecvHolder extends RecvMessageViewHolder implements View.OnClickListener {
        ImageView mIVContent,mIVPlayer;
        VideoRecvHolder(View view) {
            super(view);

            mIVContent = view.findViewById(R.id.message_video_recv_content);
            mIVPlayer = view.findViewById(R.id.message_video_recv_play);

        }

        @Override
        public void setOnCreateContextMenuListener(View.OnCreateContextMenuListener l) {
            mIVContent.setLongClickable(l==null?false:true);
            mIVContent.setOnLongClickListener(this);
            mIVContent.setOnCreateContextMenuListener(l);
            mIVPlayer.setLongClickable(false);
        }

        @Override
        void setContent(String mime, JSONObject content,long msgLen) {
            super.setContent(mime,content,msgLen);

            String filePath ="";
            int height,width;
            try {
                filePath = content.getString(MessageEvent.KEY_FILE_PATH);
                filePath = filePath.toLowerCase();
            } catch (JSONException e) {
                filePath="";
            }
            try {
                width = content.getInt(MessageEvent.KEY_RESOLUTION_WIDTH);
                height = content.getInt(MessageEvent.KEY_RESOLUTION_HEIGHT);
            }catch (JSONException e) {
                width =height=0 ;
            }
            File file = new File(filePath);
            if (!file.exists()) {
                RequestOptions options = new RequestOptions() .override(162, 125);
                Glide.with(mContext).load(R.drawable.pic_failed).apply(options).into(mIVContent);
            } else {
                height= (height==0?DefaultWidth:height);
                width= (width==0?DefaultHeight:width);
                if (height< minheight && width < minwidth) {
                    float hRate = height / (float) minheight;
                    float wRate = width / (float) minwidth;
                    if (wRate < hRate) {
                        width = (int) (width / hRate);
                        height = minheight;
                    } else {
                        width = minwidth;
                        height = (int) (height / wRate);
                    }
                }
                RequestOptions options = new RequestOptions().fitCenter().diskCacheStrategy(DiskCacheStrategy.NONE);
//                options.error(R.drawable.pic_failed);
                options.override(width,height).error(R.drawable.pic_failed);
                Glide.with(mContext).load(Uri.fromFile( file )).apply(options).into(mIVContent);
//                mIVContent.setImageBitmap(bitmap);
            }
            mIVPlayer.setVisibility(View.VISIBLE);
            mIVPlayer.setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {
            if(mMessageViewClickLister!=null){
                mMessageViewClickLister.onMessageViewClick(v,mMessageID,mMessageRowID,mMessageTime,mMIME,mRead,false,mContent,status);
            }
        }

    }


    //recycleview
    //
    public class ImageSendHolder extends SentMessageViewHolder implements View.OnClickListener {
        ImageView mIVContent;
        ImageSendHolder(View view) {
            super(view);
            mIVContent  =view.findViewById(R.id.message_image_sent);
        }
        @Override
        public void setOnCreateContextMenuListener(View.OnCreateContextMenuListener l) {
            mIVContent.setLongClickable(l==null?false:true);
            mIVContent.setOnLongClickListener(this);
            mIVContent.setOnCreateContextMenuListener(l);
        }
        @Override
        void setContent(String mime, JSONObject content,long msgLen) {
            super.setContent(mime,content,msgLen);

            String filePath ="";
            int height=0,width=0;
            try {
                filePath = content.getString(MessageEvent.KEY_FILE_PATH);
            } catch (JSONException e) {
                filePath="";
            }
            try {
                width = content.getInt(MessageEvent.KEY_RESOLUTION_WIDTH);
                height = content.getInt(MessageEvent.KEY_RESOLUTION_HEIGHT);
            } catch (JSONException e) {
                width = height =0;
            }

            File file = new File(filePath);
            if (!file.exists()) {
                RequestOptions options = new RequestOptions() .override(162, 125);
                Glide.with(mContext).load(R.drawable.pic_failed).apply(options).into(mIVContent);
            } else {
                height= (height==0?DefaultHeight:height);
                width= (width==0?DefaultWidth:width);

                if (height< minheight && width < minwidth) {
//                    FrameLayout.LayoutParams layoutParams = (FrameLayout.LayoutParams) mIVContent.getLayoutParams();
                    float hRate = height / (float) minheight;
                    float wRate = width / (float) minwidth;
                    if (wRate < hRate) {
                        width = (int) (width / hRate);
                        height = minheight;
                    } else {
                        width = minwidth;
                        height = (int) (height / wRate);
                    }
                }
                RequestOptions options = new RequestOptions().fitCenter();
                options.override(width,height);
                Glide.with(mContext).load(file.getAbsoluteFile()).apply(options).into(mIVContent);
                mIVContent.setOnClickListener(this);
            }
        }

        @Override
        public void onClick(View v) {
            if(mMessageViewClickLister!=null){
                mMessageViewClickLister.onMessageViewClick(v,mMessageID,mMessageRowID,mMessageTime,mMIME,mRead,true,mContent,status);
            }

//            if(file.exists()) {
////                ActivityOptionsCompat optionsCompat = ActivityOptionsCompat.makeScaleUpAnimation(
////                        mIVContent, mIVContent.getWidth() / 2, mIVContent.getHeight() / 2, 0, 0);//新界面打开时的动画
//                Intent playIntent = new Intent(mContext, PortActivityImageView.class);
////                playIntent.setAction(Intent.ACTION_VIEW);
//                playIntent.setDataAndType(ImagePathUtils.aProviderUri(mContext,file),"image/*");
//                mContext.startActivity(playIntent);
//            }
        }

    }
    boolean mSelectMode = false;
    public void setSelectMode(boolean selectMode){
        mSelectMode = selectMode;
    }
    public boolean getSelectMode(){
        return mSelectMode;
    }

    //
    public class ImageRecvHolder extends RecvMessageViewHolder implements View.OnClickListener {
        ImageView mIVContent;
        ImageRecvHolder(View view) {
            super(view);
            mIVContent  =view.findViewById(R.id.message_image_recv);
        }
        @Override
        public void setOnCreateContextMenuListener(View.OnCreateContextMenuListener l) {
            mIVContent.setLongClickable(l==null?false:true);
            mIVContent.setOnLongClickListener(this);
            mIVContent.setOnCreateContextMenuListener(l);
        }
        @Override
        void setContent(String mime, JSONObject content,long msgLen) {
            super.setContent(mime,content,msgLen);

            String filePath ="",fileName = "";
            int height=0,width=0;
            try {
                filePath = content.getString(MessageEvent.KEY_FILE_PATH);
            } catch (JSONException e) {
                filePath="";
            }
            try {
                width = content.getInt(MessageEvent.KEY_RESOLUTION_WIDTH);
                height = content.getInt(MessageEvent.KEY_RESOLUTION_HEIGHT);
            } catch (JSONException e) {
                width = height =0;
            }

            File file = new File(filePath);

            if (!file.exists()) {
                RequestOptions options = new RequestOptions() .override(162, 125);
                Glide.with(mContext).load(R.drawable.pic_failed).apply(options).into(mIVContent);
            } else {
                height= (height==0?DefaultHeight:height);
                width= (width==0?DefaultWidth:width);
                RequestOptions options = new RequestOptions() .centerCrop();
                if (height< minheight && width < minwidth) {
//                    FrameLayout.LayoutParams layoutParams = (FrameLayout.LayoutParams) mIVContent.getLayoutParams();
                    float hRate = height / (float) minheight;
                    float wRate = width / (float) minwidth;
                    if (wRate < hRate) {
                        width = (int) (width / hRate);
                        height = minheight;
                    } else {
                        width = minwidth;
                        height = (int) (height / wRate);
                    }

                }
                options.override(width,height);

                Glide.with(mContext).load(file.getAbsoluteFile()).apply(options).into(mIVContent);
                mIVContent.setOnClickListener(this);
            }
        }

        @Override
        public void onClick(View v) {
            if(mMessageViewClickLister!=null){
                mMessageViewClickLister.onMessageViewClick(v,mMessageID,mMessageRowID,mMessageTime,mMIME,mRead,false,mContent,status);
            }
        }
    }

    @Override
    public void onViewRecycled(MessageViewHolder holder) {
        holder.setOnCreateContextMenuListener(null);
        super.onViewRecycled(holder);
    }

    String localTxAvartar,remoteTxAvartar;
    BitmapDrawable localAvartar=null,remoteAvartar=null;
    public void setUserAvartar(Bitmap local,String localText,Bitmap remote,String remoteText){
        if(local!=null) {
            localAvartar = new BitmapDrawable(local);
        }
        if(remote!=null) {
            remoteAvartar = new BitmapDrawable(remote);
        }
        if(!TextUtils.isEmpty(localText)) {
            localTxAvartar = NgnStringUtils.getAvatarText(localText);
        }
        if(!TextUtils.isEmpty(remoteText)) {
            remoteTxAvartar = NgnStringUtils.getAvatarText(remoteText);
        }
    }


    HashMap<Long,Boolean> selectItems = new HashMap();
    public void setItemCheck(View view,long id,boolean check){
        if(check) {
            selectItems.put(id, check);
        }else {
            selectItems.remove(id);
        }
        if(view!=null){
            CheckBox box = (CheckBox) view.findViewById(R.id.message_select);
            if(box!=null) {
                box.setChecked(check);
            }
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
        int count  = getItemCount();
        for (int i=0;i<count;i++) {
            long id =  getItemId(i);
            selectItems.put(id, true);
        }
    }

    public boolean isAllSelect(){
        int count  = getItemCount();
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

}
