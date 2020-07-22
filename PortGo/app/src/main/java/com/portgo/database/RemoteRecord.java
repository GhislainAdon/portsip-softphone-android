package com.portgo.database;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.provider.ContactsContract;
import android.text.TextUtils;

import com.portgo.manager.CursorHelper;
import com.portgo.util.NgnUriUtils;

/**
 * Created by huacai on 2017/10/17.
 */
public class RemoteRecord extends Object{
    long mID;
    String mRemoteUri;
    String mRemoteDisname;
    long mContactId = 0;
    int mContactType = 0;;

    public long getRowID() {
        return mID;
    }

    public long getContactId(){
        return mContactId;
    }

    public static RemoteRecord remoteRecordFromCursor(Cursor cursor){
        RemoteRecord remoteRecord= new RemoteRecord();
        int indexCONTACT_ID = cursor.getColumnIndex(DBHelperBase.RemoteColumns.REMOTE_CONTACT_ID);
        int indexREMOTE_URI = cursor.getColumnIndex(DBHelperBase.RemoteColumns.REMOTE_URI);
        int indexCONTACT_TYPE = cursor.getColumnIndex(DBHelperBase.RemoteColumns.REMOTE_CONTACT_TYPE);
        int indexREMOTE_DISPPLAY_NAME = cursor.getColumnIndex(DBHelperBase.RemoteColumns.REMOTE_DISPPLAY_NAME);

        remoteRecord.mID = cursor.getInt(cursor.getColumnIndex(DBHelperBase.RemoteColumns._ID));
        remoteRecord.mContactId = cursor.getInt(indexCONTACT_ID);
        remoteRecord.mContactType = cursor.getInt(indexCONTACT_TYPE);
        remoteRecord.mRemoteDisname = cursor.getString(indexREMOTE_DISPPLAY_NAME);
        remoteRecord.mRemoteUri = cursor.getString(indexREMOTE_URI);
        return remoteRecord;

    }
    public static RemoteRecord getRemoteRecord(ContentResolver resolver,int rowId){
        RemoteRecord remoteRecord=null;
    Uri uriWithId = ContentUris.withAppendedId(DBHelperBase.RemoteColumns.CONTENT_URI , rowId);

    Cursor cursor =CursorHelper.resolverQuery(resolver,uriWithId, null, null, null, null);
        if(CursorHelper.moveCursorToFirst(cursor)){
            remoteRecord = remoteRecordFromCursor(cursor);

        }
        CursorHelper.closeCursor(cursor);
        return remoteRecord;
    }

    public static RemoteRecord getRemoteRecord(ContentResolver resolver, String remoteUri, String remoteDisName){
        RemoteRecord remoteRecord = null;
        Cursor remoteCursor = CursorHelper.resolverQuery(resolver,DBHelperBase.RemoteColumns.CONTENT_URI,null,
                DBHelperBase.RemoteColumns.REMOTE_URI +"=?",new String[]{remoteUri},null);

        if(CursorHelper.moveCursorToFirst(remoteCursor)){//已经存在，获取
            remoteRecord = RemoteRecord.remoteRecordFromCursor(remoteCursor);
        }else{//不存在，创建
            if(TextUtils.isEmpty(remoteDisName)) {
                remoteDisName = NgnUriUtils.getUserName(remoteUri);
            }
            ContentValues values = new ContentValues();
            values.put(DBHelperBase.RemoteColumns.REMOTE_URI,remoteUri);
            values.put(DBHelperBase.RemoteColumns.REMOTE_DISPPLAY_NAME,remoteDisName);
            Uri resultUri = resolver.insert(DBHelperBase.RemoteColumns.CONTENT_URI,values);
            long remoteId = ContentUris.parseId(resultUri);

            remoteRecord = new RemoteRecord();
            remoteRecord.mRemoteDisname = remoteDisName;
            remoteRecord.mRemoteUri = remoteUri;
            remoteRecord.mID = remoteId;
            remoteRecord.mContactId = 0;
            remoteRecord.mContactType = 0;
        }
        CursorHelper.closeCursor(remoteCursor);
        return remoteRecord;
    }

    public static RemoteRecord getRemoteRecord(ContentResolver resolver, String remoteUri, String remoteDisName,int contactId){
        RemoteRecord remoteRecord = null;
        Cursor remoteCursor = CursorHelper.resolverQuery(resolver,DBHelperBase.RemoteColumns.CONTENT_URI,null,
                DBHelperBase.RemoteColumns.REMOTE_URI +"=?",new String[]{remoteUri},null);
        if(CursorHelper.moveCursorToFirst(remoteCursor)){
            remoteRecord = RemoteRecord.remoteRecordFromCursor(remoteCursor);
        }else{
            if(TextUtils.isEmpty(remoteDisName)) {
                remoteDisName = NgnUriUtils.getUserName(remoteUri);
            }
            ContentValues values = new ContentValues();
            values.put(DBHelperBase.RemoteColumns.REMOTE_URI,remoteUri);
            values.put(DBHelperBase.RemoteColumns.REMOTE_DISPPLAY_NAME,remoteDisName);
            values.put(DBHelperBase.RemoteColumns.REMOTE_CONTACT_ID,contactId);
            Uri resultUri = resolver.insert(DBHelperBase.RemoteColumns.CONTENT_URI,values);
            long remoteId = ContentUris.parseId(resultUri);
            if(remoteId>0) {
                remoteRecord = new RemoteRecord();
                remoteRecord.mContactId = contactId;
                remoteRecord.mRemoteUri = remoteUri;
                remoteRecord.mRemoteDisname = remoteDisName;
                remoteRecord.mID = remoteId;
            }

        }
        CursorHelper.closeCursor(remoteCursor);
        return remoteRecord;
    }
}
