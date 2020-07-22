package com.portgo.manager;

import android.content.ContentResolver;
import android.database.Cursor;
import android.net.Uri;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.portgo.database.CursorHepperWrapper;

public class CursorHelper {
    static public void closeCursor(Cursor cursor){
        if(cursor!=null&&!cursor.isClosed()){
            cursor.close();
        }
    }

    public static synchronized Cursor resolverQuery(ContentResolver resolver,@NonNull Uri uri,
                                       @Nullable String[] projection, @Nullable String selection,
                                       @Nullable String[] selectionArgs, @Nullable String sortOrder){
        Cursor cursor = null;
        try {
            cursor = resolver.query(uri,projection,selection,selectionArgs,sortOrder);
        }catch (Exception e) {
            e.printStackTrace();
            closeCursor(cursor);
            cursor = null;
        }
        return new CursorHepperWrapper(cursor);
    }

    static public boolean moveCursorToFirst(Cursor cursor){
        if(cursor!=null&&!cursor.isClosed()){
            return cursor.moveToFirst();
        }
        return false;
    }

    static public boolean moveCursorToPosition(Cursor cursor,int position){
        if(cursor!=null&&!cursor.isClosed()&&position>0){
            return cursor.moveToPosition(position);
        }
        return false;
    }

    static public boolean moveCursorToNext(Cursor cursor){
        if(cursor!=null&&!cursor.isClosed()){
            return cursor.moveToNext();
        }
        return false;
    }

    static public int getCount(Cursor cursor){
        if(cursor!=null&&!cursor.isClosed()){
            return cursor.getCount();
        }
        return 0;
    }
}
