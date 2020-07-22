/* Copyright (C) 2010-2011, Mamadou Diop.
*  Copyright (C) 2011, Doubango Telecom.
*
* Contact: Mamadou Diop <diopmamadou(at)doubango(dot)org>
*	
* This file is part of imsdroid Project (http://code.google.com/p/imsdroid)
*
* imsdroid is free software: you can redistribute it and/or modify it under the terms of 
* the GNU General Public License as published by the Free Software Foundation, either version 3 
* of the License, or (at your option) any later version.
*	
* imsdroid is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
* without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  
* See the GNU General Public License for more details.
*	
* You should have received a copy of the GNU General Public License along 
* with this program; if not, write to the Free Software Foundation, Inc., 
* 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
*/
package com.portgo.manager;

import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import androidx.annotation.NonNull;
import android.text.TextUtils;
import android.view.KeyEvent;

import com.portgo.R;
import com.portgo.database.DBHelperBase;
import com.portgo.util.DateTimeUtils;
import com.portgo.util.NgnStringUtils;
import com.portgo.util.OkHttpHelper;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.Date;

public class MessageEvent {
	public final static String KEY_MESSAGE_TYPE = "type" ;//
	public final static String KEY_TEXT_CONTENT = "content" ;
	public final static String MESSAGE_TYPE_TEXT = "text" ;
	public final static String MESSAGE_TYPE_AUDIO = "audio" ;
	public final static String MESSAGE_TYPE_VIDEO = "video" ;
    public final static String MESSAGE_TYPE_IMAGE = "image";
	public final static String MESSAGE_TYPE_FILE = "file" ;
    public final static String KEY_FILE_NAME = "fileName" ;//
    public final static String KEY_FILE_PATH = "filePath" ;//
    public final static String KEY_FILE_SIZE = "fileSize" ;
    public final static String KEY_FILE_URL = "url" ;
    public final static String KEY_AV_DURATION = "duration" ;
    public final static String KEY_RESOLUTION_WIDTH = "width" ;//
    public final static String KEY_RESOLUTION_HEIGHT= "height" ;//
    public final static String KEY_MIME = "mime" ;//

	public enum MessageStatus{//
		Failed,
		SUCCESS,
		PROCESSING,
		ATTACH_FAILED,//
		ATTACH_SUCESS;//

		public static MessageEvent.MessageStatus valueof(int ordinal){
			if(ordinal>-1&&values().length>ordinal)
				return values()[ordinal];
			return values()[0];
		}
	}

    int mId;
	protected String mMIME;
	protected long mMessageTime;
	protected int mSeen =1;//
    protected boolean mSendout;//true=sendout，false=received
	protected MessageStatus mStatus;
	private String mDisplayName,description;
	protected String mContent;
	private long mSmsId = -1;
	int mSessionid=0;
    protected boolean mDelete = false;
	protected int mMessageDuration;
	protected JSONObject jsonContent;
	public MessageEvent(int sessionid,String remoteDisName, MessageStatus status,boolean sendOut) {
        mDisplayName = remoteDisName;
        mMessageTime = new Date().getTime();
        mSendout = sendOut;
		mSessionid = sessionid;
		if(mSendout) {
			mSeen = 0;
		}
		setMessageStatus(status);
	}

    public static  JSONObject constructVideoMessage(String filePath,String url,String mime,long fileSize,int duration) {
        if(TextUtils.isEmpty(url)) {
            url ="";
        }
        if(TextUtils.isEmpty(filePath)) {
            filePath ="";
        }

        JSONObject jMessage = new JSONObject();
        try {
            File file = new File(filePath);
            jMessage.put(KEY_MESSAGE_TYPE,MESSAGE_TYPE_VIDEO);
            jMessage.put(KEY_FILE_URL, url);
            jMessage.put(KEY_MIME,mime);
            jMessage.put(KEY_FILE_NAME, file.getName());
            jMessage.put(KEY_FILE_PATH,filePath);
            jMessage.put(KEY_AV_DURATION,duration);
            jMessage.put(KEY_FILE_SIZE,fileSize);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return jMessage;
    }
    public static  JSONObject constructImageMessage(String filePath,String url,String mime,long fileSize,int width,int height) {
        if(TextUtils.isEmpty(url)) {
            url ="";
        }
        if(TextUtils.isEmpty(filePath)) {
            filePath ="";
        }
        JSONObject jMessage = new JSONObject();
        try {
            File file = new File(filePath);
            jMessage.put(KEY_MESSAGE_TYPE,MESSAGE_TYPE_IMAGE);
            jMessage.put(KEY_FILE_URL, url);
            jMessage.put(KEY_FILE_PATH,filePath);
            jMessage.put(KEY_FILE_NAME, file.getName());
            jMessage.put(KEY_MIME,mime);
            jMessage.put(KEY_RESOLUTION_HEIGHT,width);
            jMessage.put(KEY_RESOLUTION_WIDTH,height);
            jMessage.put(KEY_FILE_SIZE,fileSize);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return jMessage;
    }
    public static  JSONObject constructAudioMessage(String filePath,String url,String mime,long fileSize,int duration) {
        if(TextUtils.isEmpty(url)) {
            url ="";
        }
        if(TextUtils.isEmpty(filePath)) {
            filePath ="";
        }
        JSONObject jMessage = new JSONObject();
        try {
            File file = new File(filePath);
            jMessage.put(KEY_MESSAGE_TYPE,MESSAGE_TYPE_AUDIO);
            jMessage.put(KEY_FILE_URL, url);
            jMessage.put(KEY_FILE_PATH,filePath);
            jMessage.put(KEY_FILE_NAME, file.getName());
            jMessage.put(KEY_MIME,mime);
            jMessage.put(KEY_AV_DURATION,duration);
            jMessage.put(KEY_FILE_SIZE,fileSize);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return jMessage;
    }

    public static  JSONObject constructTextMessage(@NonNull String text) {
        JSONObject jMessage = new JSONObject();
        try {
            jMessage.put(KEY_MESSAGE_TYPE,MESSAGE_TYPE_TEXT);
            jMessage.put(KEY_TEXT_CONTENT, text);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return jMessage;
    }

    public static  JSONObject constructFileMessage(String name,String filePath,String url,String mime,long fileSize) {
        JSONObject jMessage = new JSONObject();
        try {
            jMessage.put(KEY_MESSAGE_TYPE,MESSAGE_TYPE_FILE);
            jMessage.put(KEY_FILE_NAME, name);
            jMessage.put(KEY_MIME, mime);
            jMessage.put(KEY_FILE_PATH, filePath);
            jMessage.put(KEY_FILE_URL,url);
            jMessage.put(KEY_FILE_SIZE,fileSize);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return jMessage;
    }

	public static JSONObject parserMessage(String message){
        JSONObject jMessage =null;
	    if(!TextUtils.isEmpty(message)){
            try {
                jMessage = new JSONObject(message);
                jMessage.getString(KEY_MESSAGE_TYPE);
            }catch (JSONException e){
                jMessage = null;
            }

        }
        if(jMessage==null) {
            jMessage = new JSONObject();
            try {
                jMessage.put(KEY_MESSAGE_TYPE, MESSAGE_TYPE_TEXT);
                jMessage.put(KEY_TEXT_CONTENT, TextUtils.isEmpty(message)?"":message);
            } catch (JSONException e) {

            }
        }
        return jMessage;
    }

    public void setDelete(boolean isDelete) {
        this.mDelete = isDelete;
    }

    public boolean isDelete(){
	    return  mDelete;
    }
    public MessageEvent(int sessionid, String remoteDisName, MessageStatus status, boolean sendOut, long messagetime) {
        this(sessionid, remoteDisName,status,sendOut);
        mMessageTime = messagetime;
    }

    public JSONObject getJsonContent() {
        return jsonContent;
    }

    public JSONObject parserContent(String content){
		if(jsonContent==null){
			try {
				jsonContent = new JSONObject(content);//
			} catch (JSONException e) {
				jsonContent = null;
			}
			if(jsonContent==null) {
				try {
					jsonContent = new JSONObject();
					jsonContent.put(KEY_MESSAGE_TYPE, MESSAGE_TYPE_TEXT);
					jsonContent.put(KEY_TEXT_CONTENT,content);
				} catch (JSONException e) {

				}
			}
		}
        mContent=jsonContent.toString();
		return jsonContent;
	}

    public int getSessionid(){
		return mSessionid;
	}

    public String getDescription(Context context){
	    if(this.description==null||jsonContent==null){
	        if(jsonContent==null){
	            return "";
            }
            try {
                String messageType = jsonContent.getString(MessageEvent.KEY_MESSAGE_TYPE);
                if(MessageEvent.MESSAGE_TYPE_TEXT.equals(messageType)){
                    description = jsonContent.getString(MessageEvent.KEY_TEXT_CONTENT);
                    description = NgnStringUtils.getDescString(description);
                }else if(MessageEvent.MESSAGE_TYPE_FILE.equals(messageType)){
                    description = jsonContent.getString(MessageEvent.KEY_FILE_NAME);
                }else if(MessageEvent.MESSAGE_TYPE_VIDEO.equals(messageType)){
                    description = context.getString(R.string.string_video_message);
                }else if(MessageEvent.MESSAGE_TYPE_AUDIO.equals(messageType)){
                    description = context.getString(R.string.string_audio_message);
                }else if(MessageEvent.MESSAGE_TYPE_IMAGE.equals(messageType)){
                    description = context.getString(R.string.string_image_message);
                }
            } catch (JSONException e) {
                e.printStackTrace();
                description = "";
            }
        }
        return this.description;
    };

	public void setContent(String mime,String content) {
	    mMIME =mime;
	    parserContent(content);
	}
    public void setContent(String mime,JSONObject content){
	    mMIME =mime;
	    jsonContent =content;
	    mContent = jsonContent.toString();
    }

	public String getContent() {return this.mContent;}
	public void setSMSId(long messageId) {this.mSmsId = messageId;}
	public long getSMSId() {return this.mSmsId;}
	public long getId() {return this.mId;}
    public boolean getSendOut(){return mSendout;}
	public int getMessageDuration(){return mMessageDuration;}

	public static final int INVALIDATE_ID = -1;

	public static MessageEvent messageFromCursor(Cursor cursor) {
		int _ID = cursor.getColumnIndex(DBHelperBase.MessageColumns._ID);
        int MESSAGE_ID = cursor.getColumnIndex(DBHelperBase.MessageColumns.MESSAGE_ID);
		int SESSION_ID = cursor.getColumnIndex(DBHelperBase.MessageColumns.MESSAGE_SESSION_ID);
		int MESSAGE_TIME = cursor.getColumnIndex(DBHelperBase.MessageColumns.MESSAGE_TIME);
		int MESSAGE_DISPLAYNAME = cursor.getColumnIndex(DBHelperBase.MessageColumns.MESSAGE_DISPLAYNAME);
		int MESSAGE_SEEN = cursor.getColumnIndex(DBHelperBase.MessageColumns.MESSAGE_SEEN);
		int MESSAGE_STATUS = cursor.getColumnIndex(DBHelperBase.MessageColumns.MESSAGE_STATUS);
		int MESSAGE_CONTENT = cursor.getColumnIndex(DBHelperBase.MessageColumns.MESSAGE_CONTENT);
        int MESSAGE_SENDOUT = cursor.getColumnIndex(DBHelperBase.MessageColumns.MESSAGE_SENDOUT);
        int MESSAGE_MIME = cursor.getColumnIndex(DBHelperBase.MessageColumns.MESSAGE_MIME);
        int MESSAGE_DESCRIPTION = cursor.getColumnIndex(DBHelperBase.MessageColumns.MESSAGE_DESCRIPTION);
		int MESSAGE_LEN = cursor.getColumnIndex(DBHelperBase.MessageColumns.MESSAGE_LEN);
        int MESSAGE_DELETE = cursor.getColumnIndex(DBHelperBase.MessageColumns.MESSAGE_DELETE);
		int id = INVALIDATE_ID, Seen = 0, mStatus = 1,sendout = 0,sessionid=0;
		long messageId = 0;

		String Displayname = "", mime="",desc="";
		long messageTime =0;
		int meesagelen=0,meesageDel=0;
        byte[] content=null;
		if (_ID >=0)
			id = cursor.getInt(_ID);
		if (MESSAGE_TIME >=0)
            messageTime = cursor.getLong(MESSAGE_TIME);
		if (SESSION_ID >=0)
			sessionid = cursor.getInt(SESSION_ID);

		if (MESSAGE_DISPLAYNAME >=0)
			Displayname = cursor.getString(MESSAGE_DISPLAYNAME);
		if (MESSAGE_SEEN >=0)
			Seen = cursor.getInt(MESSAGE_SEEN);
		if (MESSAGE_STATUS >=0)
			mStatus = cursor.getInt(MESSAGE_STATUS);
		if (MESSAGE_CONTENT >=0)
			content = cursor.getBlob(MESSAGE_CONTENT);
        if(MESSAGE_ID>=0){
            messageId = cursor.getLong(MESSAGE_ID);
        }
        if(MESSAGE_SENDOUT>=0) {
            sendout = cursor.getInt(MESSAGE_SENDOUT);
        }
        if (MESSAGE_DESCRIPTION >=0)
            desc = cursor.getString(MESSAGE_DESCRIPTION);
        if (MESSAGE_MIME >=0)
            mime = cursor.getString(MESSAGE_MIME);

		if (MESSAGE_LEN >=0)
			meesagelen = cursor.getInt(MESSAGE_LEN);
        if (MESSAGE_DELETE >=0)
            meesageDel = cursor.getInt(MESSAGE_DELETE);
        MessageEvent event = new MessageEvent(sessionid,Displayname,
                MessageStatus.valueof(mStatus),sendout>0,messageTime);
		event.setContent(mime,new String(content));
        event.description = desc;
		event.setSMSId(messageId);
		event.setMessageRead(Seen == 0);
		event.setMessageDuration(meesagelen);
		event.mId = id;
		event.setDelete(meesageDel>0?true:false);
		return event;
	}
	public static MessageEvent messageFromID(Context context, long rowId) {
		MessageEvent event =null;
		Uri uri = ContentUris.withAppendedId(DBHelperBase.MessageColumns.CONTENT_URI, rowId);
		Cursor cursor = CursorHelper.resolverQuery(context.getContentResolver(),uri, null, null, null, null);
		if(CursorHelper.moveCursorToFirst(cursor)) {
			event = MessageEvent.messageFromCursor(cursor);
		}
		CursorHelper.closeCursor(cursor);
		return event;
	}
	public ContentValues getContentValues() {
		ContentValues values = new ContentValues();
        values.put(DBHelperBase.MessageColumns.MESSAGE_ID,getSMSId());
        values.put(DBHelperBase.MessageColumns.MESSAGE_DISPLAYNAME,getDisplayName());
        values.put(DBHelperBase.MessageColumns.MESSAGE_TIME,getMessageTime());//
        values.put(DBHelperBase.MessageColumns.MESSAGE_CONTENT,getContent());
        values.put(DBHelperBase.MessageColumns.MESSAGE_DESCRIPTION,description);
		values.put(DBHelperBase.MessageColumns.MESSAGE_SESSION_ID,mSessionid);
        values.put(DBHelperBase.MessageColumns.MESSAGE_MIME,getMime());
        values.put(DBHelperBase.MessageColumns.MESSAGE_SEEN,isMessageRead()?0:1);
        values.put(DBHelperBase.MessageColumns.MESSAGE_SENDOUT,getSendOut()?1:0);
        values.put(DBHelperBase.MessageColumns.MESSAGE_STATUS,getMessageStatus().ordinal());
		values.put(DBHelperBase.MessageColumns.MESSAGE_LEN,getMessageDuration());
        values.put(DBHelperBase.MessageColumns.MESSAGE_DELETE,isDelete()?1:0);

		String messageType=MESSAGE_TYPE_TEXT;
		if(getContent()!=null){
            try {
                messageType = jsonContent.getString(MessageEvent.KEY_MESSAGE_TYPE );
            } catch (JSONException e) {
                messageType =MESSAGE_TYPE_TEXT;
            }
        }
        values.put(DBHelperBase.MessageColumns.MESSAGE_TYPE,messageType);
		return  values;
	}

    public String getDisplayName(){return mDisplayName;	}
    public MessageStatus getMessageStatus(){return mStatus;}
 	public long getMessageTime() {	return mMessageTime;}
	public boolean isMessageRead(){return mSeen==0;}
    public void setMessageRead(boolean read){mSeen = (read?0:1);}
	public void setMessageDuration(int duration){mMessageDuration = duration;}

    public String getMime(){return mMIME;}
	public void setMessageStatus(MessageStatus status){mStatus = status;}
}
