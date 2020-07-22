package com.portgo.database;


import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;

import com.portgo.PortApplication;
import com.portgo.manager.AccountManager;
import com.portgo.manager.ChatSession;
import com.portgo.manager.CursorHelper;
import com.portgo.manager.HistoryEvent;
import com.portgo.manager.MessageEvent;
import com.portgo.manager.UserAccount;
import com.portgo.util.MIMEType;
import com.portgo.util.NgnUriUtils;

import java.util.Date;

public class DataBaseManager {
    static public void updateMessageReadStatus(Context context,boolean read,long messageid){
        ContentValues contentValues = new ContentValues();
                        contentValues.put(DBHelperBase.MessageColumns.MESSAGE_SEEN, read?0:1);
        String where = DBHelperBase.MessageColumns.MESSAGE_ID + "=?";
        context.getContentResolver().update(DBHelperBase.MessageColumns.CONTENT_URI, contentValues, where, new String[]{""+ messageid});
    }

    static public boolean messageExitsByMessageId(Context context,long messageid){
        String where = DBHelperBase.MessageColumns.MESSAGE_ID + "=?";
        Cursor cursor = CursorHelper.resolverQuery(context.getContentResolver(),DBHelperBase.MessageColumns.CONTENT_URI,null,where, new String[]{""+ messageid},DBHelperBase.MessageColumns.DEFAULT_ORDER);
        if(CursorHelper.moveCursorToFirst(cursor)){
            CursorHelper.closeCursor(cursor);
            return true;
        }
        return false;
    }

    static public void upDataProcessingMessagetoFail(Context context) {

        ContentResolver contentResolver = context.getContentResolver();
        ContentValues values = new ContentValues();

        values.put(DBHelperBase.MessageColumns.MESSAGE_STATUS, MessageEvent.MessageStatus.Failed.ordinal());
        contentResolver.update(DBHelperBase.MessageColumns.CONTENT_URI, values,
                DBHelperBase.MessageColumns.MESSAGE_STATUS + "=" + MessageEvent.MessageStatus.PROCESSING.ordinal(), null);

    }

    static public void updateMessageDeleteTag(Context context,MessageEvent event) {
        long rowId = event.getId();
        updateMessageDeleteTag(context,rowId);
    }

    static public void updateMessageDeleteTag(Context context,long messageRowId) {

        Uri uri = ContentUris.withAppendedId(DBHelperBase.MessageColumns.CONTENT_URI, messageRowId);
        ContentValues values = new ContentValues();
        values.put(DBHelperBase.MessageColumns.MESSAGE_DELETE,1);
        context.getContentResolver().update(uri,values,null,null);
    }

    static public void updateMessagesDeleteTag(Context context,Long[] messageRowIds) {

        if(messageRowIds==null||messageRowIds.length<1)
            return;
        if(messageRowIds.length==1){
            updateMessageDeleteTag(context,messageRowIds[0]);
        }else{

            String where = DBHelperBase.MessageColumns._ID;
            final StringBuilder sb = new StringBuilder();
            sb.append(" IN (");
            sb.append(messageRowIds[0]);
            for (int count = 1; count < messageRowIds.length; count++) {
                sb.append(",");
                sb.append(messageRowIds[count]);
            }
            sb.append(")");
            where+=sb;

            ContentValues values = new ContentValues();
            values.put(DBHelperBase.MessageColumns.MESSAGE_DELETE,1);
            context.getContentResolver().update(DBHelperBase.MessageColumns.CONTENT_URI,values,where,null);
        }
    }

    static public void deleteMessage(Context context,MessageEvent event) {
        long rowId = event.getId();
        deleteMessage(context,rowId);
    }

    static public void deleteMessage(Context context,long messageRowId) {
        Uri uri = ContentUris.withAppendedId(DBHelperBase.MessageColumns.CONTENT_URI, messageRowId);
        context.getContentResolver().delete(uri,null,null);
    }
    static public void deleteMessages(Context context,Long[] messageRowIds) {

        if(messageRowIds==null||messageRowIds.length<1)
            return;
        if(messageRowIds.length==1){
            deleteMessage(context,messageRowIds[0]);
        }else{

            String where = DBHelperBase.MessageColumns._ID;
            final StringBuilder sb = new StringBuilder();
            sb.append(" IN (");
            sb.append(messageRowIds[0]);
            for (int count = 1; count < messageRowIds.length; count++) {
                sb.append(",");
                sb.append(messageRowIds[count]);
            }
            sb.append(")");
            where+=sb;
            context.getContentResolver().delete(DBHelperBase.MessageColumns.CONTENT_URI,where,null);
        }
    }

    static public void deleteSession(Context context,long sessionRowId) {
       updateSessionDeleteTag(context, sessionRowId,true);
        //context.getContentResolver().delete(uri,null,null);
    }

    static public void deleteSessions(Context context,Long[] sessionRowIds) {
        if(sessionRowIds==null||sessionRowIds.length<1)
            return;
        for (long sessionRowid : sessionRowIds){
            deleteSession(context, sessionRowid);
        }

    }

    static public void updateSessionsDeleteTags(Context context,Long[] sessionRowIds,boolean delete) {
        if(sessionRowIds==null||sessionRowIds.length<1)
            return;
        if(sessionRowIds.length==1){
            updateSessionDeleteTag(context,sessionRowIds[0],delete);
        }else{

            String where = DBHelperBase.ChatSessionColumns._ID;
            final StringBuilder sb = new StringBuilder();
            sb.append(" IN (");
            sb.append(sessionRowIds[0]);
            for (int count = 1; count < sessionRowIds.length; count++) {
                sb.append(",");
                sb.append(sessionRowIds[count]);
            }
            sb.append(")");
            where+=sb;
            ContentValues values = new ContentValues(1);
            values.put(DBHelperBase.ChatSessionColumns.SESSION_DELETE,delete?1:0);
            if(delete) {
                values.put(DBHelperBase.ChatSessionColumns.SESSION_UNREAD,0);
            }
            context.getContentResolver().update(DBHelperBase.ChatSessionColumns.CONTENT_URI,values,where,null);
        }
    }

    static public void upDataMessageStatus(Context context,long messageId,MessageEvent.MessageStatus status ) {
        if(messageExitsByMessageId(context,messageId)) {
            ContentResolver contentResolver = context.getContentResolver();
            ContentValues values = new ContentValues();
            values.put(DBHelperBase.MessageColumns.MESSAGE_STATUS, status.ordinal());

            contentResolver.update(DBHelperBase.MessageColumns.CONTENT_URI, values, DBHelperBase.MessageColumns.MESSAGE_ID + "=" + messageId, null);
        }
    }

    static public void upDataMessageIDStatus(Context context,long messageId,long messageRowID,MessageEvent.MessageStatus status) {
        Uri uri = ContentUris.withAppendedId(DBHelperBase.MessageColumns.CONTENT_URI,messageRowID);
        ContentResolver contentResolver = context.getContentResolver();
        ContentValues values = new ContentValues();
        values.put(DBHelperBase.MessageColumns.MESSAGE_ID, messageId);
        values.put(DBHelperBase.MessageColumns.MESSAGE_STATUS, status.ordinal());
        contentResolver.update(uri, values,null, null);
    }

    static public void upDataHistorySeenStatus(Context context,String LocalUri,int remoteId,int eventID,Boolean hasSeen) {
        UserAccount account = AccountManager.getDefaultUser(context);
        if(account!=null) {
            String local = account.getFullAccountReamName();
            String where = "";
            where = DBHelperBase.HistoryColumns.HISTORY_REMOTE_ID + "=?" + " AND " + DBHelperBase.HistoryColumns.HISTORY_LOCAL + "=?"
                    + " AND " + DBHelperBase.HistoryColumns._ID + " <= " + eventID;

            ContentValues values = new ContentValues(1);
            String[] args =new String[]{""+remoteId, local};
            values.put(DBHelperBase.HistoryColumns.HISTORY_SEEN, hasSeen ? 1 : 0);
            context.getContentResolver().update(DBHelperBase.HistoryColumns.CONTENT_URI, values, where, args);
        }
    }

    static public void upDataMessageByRowID(Context context,long messageRowID,ContentValues values) {
        Uri uri = ContentUris.withAppendedId(DBHelperBase.MessageColumns.CONTENT_URI,messageRowID);
        ContentResolver contentResolver = context.getContentResolver();
        contentResolver.update(uri, values,null, null);
    }

    static public void upDataMessageDurationStatus(Context context,long messageId,int duration,MessageEvent.MessageStatus status ,byte[] content) {
        if(messageExitsByMessageId(context,messageId)) {
            ContentResolver contentResolver = context.getContentResolver();
            ContentValues values = new ContentValues();
            values.put(DBHelperBase.MessageColumns.MESSAGE_STATUS, status.ordinal());
            values.put(DBHelperBase.MessageColumns.MESSAGE_LEN, duration);
            values.put(DBHelperBase.MessageColumns.MESSAGE_CONTENT, content);
            contentResolver.update(DBHelperBase.MessageColumns.CONTENT_URI, values, DBHelperBase.MessageColumns.MESSAGE_ID + "=" + messageId, null);
        }
    }

    static public MessageEvent findMessageByMessageRowId(Context context,long messageid){
        Uri uri = ContentUris.withAppendedId(DBHelperBase.MessageColumns.CONTENT_URI,messageid);
        Cursor cursor = CursorHelper.resolverQuery(context.getContentResolver(),uri,null,null,null,null);
        MessageEvent messageEvent = null;
        if(CursorHelper.moveCursorToFirst(cursor)){
            messageEvent = MessageEvent.messageFromCursor(cursor);
            CursorHelper.closeCursor(cursor);
            return messageEvent;
        }
        CursorHelper.closeCursor(cursor);
        return messageEvent;
    }

    static public MessageEvent findMessageByMessageId(Context context,long messageid){
        String where = DBHelperBase.MessageColumns.MESSAGE_ID + "=?";
        Cursor cursor = CursorHelper.resolverQuery(context.getContentResolver(),DBHelperBase.MessageColumns.CONTENT_URI,null,where, new String[]{""+ messageid},DBHelperBase.MessageColumns.DEFAULT_ORDER);
        MessageEvent messageEvent = null;
        if(CursorHelper.moveCursorToFirst(cursor)){
            messageEvent = MessageEvent.messageFromCursor(cursor);
            CursorHelper.closeCursor(cursor);
            return messageEvent;
        }
        CursorHelper.closeCursor(cursor);
        return messageEvent;
    }


    static public long lastReceivedMessageId(Context context,String sender,String receiver){
        long eventId = -1;
        MessageEvent msgEvent = null;
        ChatSession session = findChatSession(context,receiver,sender);
        if(session!=null) {
            long time = new Date().getTime();
            time = time - 6*1000;
            String where = DBHelperBase.MessageColumns.MESSAGE_SESSION_ID + "=? AND "+  DBHelperBase.MessageColumns.MESSAGE_SENDOUT + "=? AND "
                    +DBHelperBase.MessageColumns.MESSAGE_TIME+"<?";
            String[] args = new String[]{"" + session.getId(),""+0,""+time};
            Cursor cursor = CursorHelper.resolverQuery(context.getContentResolver(), DBHelperBase.MessageColumns.CONTENT_URI, null, where, args, null);
            if (CursorHelper.moveCursorToPosition (cursor,CursorHelper.getCount(cursor)-1)) {
                msgEvent = MessageEvent.messageFromCursor(cursor);
            }
            CursorHelper.closeCursor(cursor);
        }
        if(msgEvent!=null)
        {
            eventId = msgEvent.getSMSId();
        }
        return eventId;
    }

    static public void updateSessionMessageHasReadExceptAudio(Context context,long sessionID) {
        ContentValues contentValues = new ContentValues();
        contentValues.put(DBHelperBase.MessageColumns.MESSAGE_SEEN, 0);//
        String where = DBHelperBase.MessageColumns.MESSAGE_TYPE + " not like ? AND " + DBHelperBase.MessageColumns.MESSAGE_SESSION_ID + "=? AND " + DBHelperBase.MessageColumns.MESSAGE_SEEN + ">0";
        context.getContentResolver().update(DBHelperBase.MessageColumns.CONTENT_URI, contentValues, where, new String[]{MessageEvent.MESSAGE_TYPE_AUDIO, "" + sessionID});
    }
    static public void updateSessionReadCount(Context context,int count,long sessionID) {
        ContentValues contentValues = new ContentValues();
        contentValues.put(DBHelperBase.ChatSessionColumns.SESSION_UNREAD , count);//
        String where = DBHelperBase.ChatSessionColumns._ID + "=?";
        context.getContentResolver().update(DBHelperBase.ChatSessionColumns.CONTENT_URI, contentValues, where, new String[]{""+ sessionID});
    }

    //messageid
    static public MessageEvent getNextReceivedAudioMessage(Context context,long messageid,long messagetime){
        MessageEvent event= null;
        String where = DBHelperBase.MessageColumns.MESSAGE_TIME +" >? AND "+DBHelperBase.MessageColumns.MESSAGE_SENDOUT+"=? AND "+DBHelperBase.MessageColumns.MESSAGE_MIME+" like ?";
        String[]args = new String[]{""+messagetime,""+0,MIMEType.MIMETYPE_audio+ '%'};
        Cursor cursor= CursorHelper.resolverQuery(context.getContentResolver(), DBHelperBase.MessageColumns.CONTENT_URI,null,where,args, DBHelperBase.MessageColumns.DEFAULT_ORDER);
        if(CursorHelper.moveCursorToFirst(cursor)){
            event = MessageEvent.messageFromCursor(cursor);
        }
        CursorHelper.closeCursor(cursor);
        return event;
    }
    public static ChatSession findChatSessionByID(Context context,long seesionID){
        ChatSession session = null;
        ContentResolver resolver = context.getContentResolver();
        Uri uri = ContentUris.withAppendedId(DBHelperBase.ViewChatSessionColumns.CONTENT_URI,seesionID);
        Cursor cursor = CursorHelper.resolverQuery(resolver, uri, null, null,null, null);
        if (CursorHelper.moveCursorToFirst(cursor)) {
            session = ChatSession.ChatSessionFormViewCursor(cursor);
        }
        CursorHelper.closeCursor(cursor);
        return session;
    }

    public static ChatSession findChatSession(Context context,String localUri, String remoteUri){
        ChatSession session = null;
        ContentResolver resolver = context.getContentResolver();
        String where = DBHelperBase.RemoteColumns.REMOTE_URI + "=? AND " + DBHelperBase.ChatSessionColumns.SESSION_LOCAL_URI + "=?";
        Cursor cursor = CursorHelper.resolverQuery(resolver, DBHelperBase.ViewChatSessionColumns.CONTENT_URI, null, where, new String[]{remoteUri, localUri}, null);
        if (CursorHelper.moveCursorToFirst(cursor)) {
            session = ChatSession.ChatSessionFormViewCursor(cursor);
        }
        CursorHelper.closeCursor(cursor);
        return session;
    }

    public static int insertMessage(Context context,MessageEvent event){
        if(event!=null){
            event.getDescription(context);
            ContentValues values = event.getContentValues();
            context.getContentResolver().insert(DBHelperBase.MessageColumns.CONTENT_URI, values);
        }
        return  -1;
    }
    static public void updateSessionDeleteTag(Context context,long sessionRowId,boolean delete) {
        Uri uri = ContentUris.withAppendedId(DBHelperBase.ChatSessionColumns.CONTENT_URI, sessionRowId);
        ContentValues values = new ContentValues();
        values.put(DBHelperBase.ChatSessionColumns.SESSION_DELETE,delete?1:0);
        if(delete) {
            values.put(DBHelperBase.ChatSessionColumns.SESSION_UNREAD,0);
        }
        context.getContentResolver().update(uri,values,null,null);
        //context.getContentResolver().delete(uri,null,null);
    }

    public static ChatSession getChatSession(Context context,String localUri, String remoteUri, String disName, int contactid) {
        int sessionId = 0;
        ChatSession session = null;
        ContentResolver resolver = context.getContentResolver();
        session = findChatSession(context,localUri,remoteUri);
        if (session == null) {
            if(TextUtils.isEmpty(disName)){
                disName = NgnUriUtils.getDisplayName(remoteUri,context);
            }
            RemoteRecord remoteRecord =RemoteRecord.getRemoteRecord(context.getContentResolver(),remoteUri,disName,contactid);
            if (remoteRecord!= null) {
                ContentValues values = new ContentValues();
                values.put(DBHelperBase.ChatSessionColumns.SESSION_LOCAL_URI, localUri);
                values.put(DBHelperBase.ChatSessionColumns.SESSION_REMOTE_ID, remoteRecord.mID);
                Uri resultUri = resolver.insert(DBHelperBase.ChatSessionColumns.CONTENT_URI, values);
                sessionId = (int) ContentUris.parseId(resultUri);
                if (sessionId > 0) {
                    session = new ChatSession(sessionId);
                    session.setRemoteID((int) remoteRecord.mID);
                    session.setRemoteUri(remoteUri);
                    session.setLocalUri(localUri);
                }
            }
        }else{
            if(session.isDelete()){
                updateSessionDeleteTag(context,session.getId(),false);
            }
        }
        return session;
    }
}
