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

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;

import com.portgo.database.DBHelperBase;
import com.portgo.util.DateTimeUtils;

import java.util.Date;

public class ChatSession {
//	MessageEvent event;
	String mLocal,mRemote,mRemoteDisname,mStatus;
	int mCount,mId,mRemoteId,mContactid;
    long mLastTimeConnect;
    boolean mDelete;

    public String getDisplayName() {
        return mRemoteDisname;
    }

    public long getMessageTime() {
        return mLastTimeConnect;
    }

    public enum MessageType{
		SMS_MESSAGE,
		PRESENCE_MESSAGE
	}

	public String getRemoteUri() {
		return mRemote;
	}
	public boolean isDelete() {
		return mDelete;
	}
	public String getLocalUri() {
		return mLocal;
	}

	public void setLocalUri(String localUri) {
		mLocal = localUri;
	}
	public void setRemoteID(int mRemoteId){this.mRemoteId = mRemoteId;}
	public void setRemoteUri(String remoteUri){this.mRemote =remoteUri;}
	public void setRemoteDisname(String remoteDisName){this.mRemoteDisname =remoteDisName;}
	public int getContactid(){
		return mContactid;
	}
	public ChatSession(int sessionId){
		mId = sessionId;
	}
//	public void setMessageType(MessageType messageType) {
//		this.messageType = messageType;
//	}
//
	public int getId() {
		return this.mId;
	}

	public void setCount(int mCount) {
		this.mCount = mCount;
	}

	public int getCount() {
		return mCount;
	}
	public int getRemoteId(){
		return mRemoteId;
	}

    public final  String getStatus() {
        return mStatus;
    }

	public static ChatSession ChatSessionFormCursor(Cursor cursor){
		int mId = cursor.getInt(cursor.getColumnIndex(DBHelperBase.ChatSessionColumns._ID));
		String localUri = cursor.getString(cursor.getColumnIndex(DBHelperBase.ChatSessionColumns.SESSION_LOCAL_URI));
		String mStatus= cursor.getString(cursor.getColumnIndex(DBHelperBase.ChatSessionColumns.SESSION_STATUS));
		int notRead= cursor.getInt(cursor.getColumnIndex(DBHelperBase.ChatSessionColumns.SESSION_UNREAD));
        long time= cursor.getLong(cursor.getColumnIndex(DBHelperBase.ChatSessionColumns.SESSION_LASTTIME));
		int mRemoteId = cursor.getInt(cursor.getColumnIndex(DBHelperBase.ChatSessionColumns.SESSION_REMOTE_ID));

		ChatSession session = new ChatSession(mId);
		session.mLocal=localUri;
		session.mStatus=mStatus;
		session.mCount=notRead;
        session.mLastTimeConnect =time;
		session.mRemoteId =mRemoteId;
		return session;
	}

	public static ChatSession ChatSessionById(Context context,long sessionID){
    	String where = DBHelperBase.ChatSessionColumns._ID + "=?";
		Cursor cursor = CursorHelper.resolverQuery(context.getContentResolver(), DBHelperBase.ViewChatSessionColumns.CONTENT_URI, null,
				where, new String[]{"" + sessionID}, null);
		ChatSession session = null;
		if (CursorHelper.moveCursorToFirst(cursor)) {
			session = ChatSession.ChatSessionFormViewCursor(cursor);
		}
		CursorHelper.closeCursor(cursor);
		return session;
	}
    public static ChatSession ChatSessionFormViewCursor(Cursor cursor){
        int mId = cursor.getInt(cursor.getColumnIndex(DBHelperBase.ChatSessionColumns._ID));
        String localUri = cursor.getString(cursor.getColumnIndex(DBHelperBase.ChatSessionColumns.SESSION_LOCAL_URI));
        String mStatus= cursor.getString(cursor.getColumnIndex(DBHelperBase.ChatSessionColumns.SESSION_STATUS));
        int notRead= cursor.getInt(cursor.getColumnIndex(DBHelperBase.ChatSessionColumns.SESSION_UNREAD));
        long time= cursor.getLong(cursor.getColumnIndex(DBHelperBase.ChatSessionColumns.SESSION_LASTTIME));
        String remoteUri = cursor.getString(cursor.getColumnIndex(DBHelperBase.RemoteColumns.REMOTE_URI));
        String disname = cursor.getString(cursor.getColumnIndex(DBHelperBase.RemoteColumns.REMOTE_DISPPLAY_NAME));
        int mContactid = cursor.getInt(cursor.getColumnIndex(DBHelperBase.RemoteColumns.REMOTE_CONTACT_ID));
        int mRemoteId = cursor.getInt(cursor.getColumnIndex(DBHelperBase.ChatSessionColumns.SESSION_REMOTE_ID));
		int mDelete = cursor.getInt(cursor.getColumnIndex(DBHelperBase.ChatSessionColumns.SESSION_DELETE));
        ChatSession session = new ChatSession(mId);
        session.mLocal=localUri;
        session.mRemote=remoteUri;
        session.mStatus=mStatus;
        session.mCount=notRead;
        session.mRemoteDisname= disname;
        session.mLastTimeConnect =time;
        session.mRemoteId = mRemoteId;
        session.mContactid =mContactid;
		session.mDelete = mDelete>0?true:false;
        return session;
    }

	public static ChatSession findChatSession(Context context, long sessionId){
		String where = DBHelperBase.ChatSessionColumns._ID+"=?";
        ChatSession session = null;
        Cursor cursor = CursorHelper.resolverQuery(context.getContentResolver(),DBHelperBase.ViewChatSessionColumns.CONTENT_URI,null,
                where,new String[]{""+sessionId},null);
        while (CursorHelper.moveCursorToFirst(cursor)) {
            session = ChatSession.ChatSessionFormViewCursor(cursor);
            break;
        }
        CursorHelper.closeCursor(cursor);

		return session;
	}
}
