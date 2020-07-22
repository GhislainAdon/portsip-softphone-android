
package com.portgo.database;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.provider.BaseColumns;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import android.text.TextUtils;

import com.portgo.manager.ChatSession;

import java.util.ArrayList;
import java.util.List;

import static com.portgo.database.UriMactherHepler.MATCH_ACCOUNT;
import static com.portgo.database.UriMactherHepler.MATCH_ACCOUNT_ALL;
import static com.portgo.database.UriMactherHepler.MATCH_CALL_RULE;
import static com.portgo.database.UriMactherHepler.MATCH_CALL_RULE_ALL;
import static com.portgo.database.UriMactherHepler.MATCH_CHAT_SESSION;
import static com.portgo.database.UriMactherHepler.MATCH_CHAT_SESSION_ALL;
import static com.portgo.database.UriMactherHepler.MATCH_CONTACT_VERSION;
import static com.portgo.database.UriMactherHepler.MATCH_CONTACT_VERSION_ALL;
import static com.portgo.database.UriMactherHepler.MATCH_HISTORY;
import static com.portgo.database.UriMactherHepler.MATCH_HISTORY_ALL;
//import static com.portgo.database.UriMactherHepler.MATCH_HISTORY_GROUPBY;
//import static com.portgo.database.UriMactherHepler.MATCH_HISTORY_GROUPBY_ALL;
import static com.portgo.database.UriMactherHepler.MATCH_MESSAGE;
import static com.portgo.database.UriMactherHepler.MATCH_MESSAGE_ALL;
import static com.portgo.database.UriMactherHepler.MATCH_RECORD;
import static com.portgo.database.UriMactherHepler.MATCH_RECORD_ALL;
import static com.portgo.database.UriMactherHepler.MATCH_REMOTE;
import static com.portgo.database.UriMactherHepler.MATCH_REMOTE_ALL;
import static com.portgo.database.UriMactherHepler.MATCH_SUBSCRIBE;
import static com.portgo.database.UriMactherHepler.MATCH_SUBSCRIBE_ALL;
import static com.portgo.database.UriMactherHepler.MATCH_VIEW_CHAT_SESSION;
import static com.portgo.database.UriMactherHepler.MATCH_VIEW_CHAT_SESSION_ALL;
import static com.portgo.database.UriMactherHepler.MATCH_VIEW_HISTORY;
import static com.portgo.database.UriMactherHepler.MATCH_VIEW_HISTORY_ALL;


public class PortgoProvider extends ContentProvider{
	DBHelperBase dbHelper;

	@Override
	public boolean onCreate() {
        dbHelper = new DBHelperBase(getContext());
        return false;
//		return dbHelper.getReadableDatabase() !=null;
	}

	@Nullable
	@Override
	public Cursor query(@NonNull Uri uri, @Nullable String[] projection, @Nullable String selection, @Nullable String[] selectionArgs, @Nullable String orderBy) {
        SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
        UriMatcher uriMatcher = UriMactherHepler.getUriMatcher();
        String groupby =null,having = null;
        switch (uriMatcher.match(uri)) { //
            case MATCH_ACCOUNT:                             // huoqu mou yi tiao
                qb.setTables(DBHelperBase.TABLE_ACCOUNT);
                qb.appendWhere(BaseColumns._ID + "=" + uri.getPathSegments().get(1));
//                qb.setProjectionMap(DBHelperBase.AccountColumns.accountProjectionMap);

                break;
            case MATCH_ACCOUNT_ALL:                         //huo qu suo you
                qb.setTables(DBHelperBase.TABLE_ACCOUNT);
//                qb.setProjectionMap(DBHelperBase.AccountColumns.accountProjectionMap);
                if (TextUtils.isEmpty(orderBy)) {
                    orderBy = DBHelperBase.AccountColumns.DEFAULT_ORDER;
                }
                break;
            case MATCH_CHAT_SESSION:                             // huoqu mou yi tiao
                qb.setTables(DBHelperBase.TABLE_CHATSESSION);
                qb.appendWhere(BaseColumns._ID + "=" + uri.getPathSegments().get(1));
//                qb.setProjectionMap(DBHelperBase.AccountColumns.accountProjectionMap);

                break;
            case MATCH_CHAT_SESSION_ALL:                         //huo qu suo you
                qb.setTables(DBHelperBase.TABLE_CHATSESSION);
//                qb.setProjectionMap(DBHelperBase.AccountColumns.accountProjectionMap);
                if (TextUtils.isEmpty(orderBy)) {
                    orderBy = DBHelperBase.ChatSessionColumns.DEFAULT_ORDER;
                }
                break;
            case MATCH_HISTORY:
                qb.setTables(DBHelperBase.TABLE_HISTORY);
                qb.appendWhere(BaseColumns._ID + "=" + uri.getPathSegments().get(1));
                break;
            case MATCH_HISTORY_ALL:
                qb.setTables(DBHelperBase.TABLE_HISTORY);
                break;
            case MATCH_REMOTE:
                qb.setTables(DBHelperBase.TABLE_REMOTE);
                qb.appendWhere(BaseColumns._ID + "=" + uri.getPathSegments().get(1));
                break;
            case MATCH_REMOTE_ALL:
                qb.setTables(DBHelperBase.TABLE_REMOTE);
                break;

            case MATCH_CONTACT_VERSION:
                qb.setTables(DBHelperBase.TABLE_CONTACT_VERSION);
                qb.appendWhere(BaseColumns._ID + "=" + uri.getPathSegments().get(1));
                break;
            case MATCH_CONTACT_VERSION_ALL:
                qb.setTables(DBHelperBase.TABLE_CONTACT_VERSION);
                break;

            case MATCH_VIEW_HISTORY:
                qb.setTables(DBHelperBase.VIEW_HISTORY);
                qb.appendWhere(BaseColumns._ID + "=" + uri.getPathSegments().get(1));
                break;

            case MATCH_VIEW_HISTORY_ALL:
                qb.setTables(DBHelperBase.VIEW_HISTORY);
                groupby= DBHelperBase.HistoryColumns.HISTORY_GROUP;
                break;

            case MATCH_CALL_RULE:
                qb.appendWhere(BaseColumns._ID + "=" + uri.getPathSegments().get(1));
                qb.setTables(DBHelperBase.TABLE_CALLRULE);
                break;

            case MATCH_CALL_RULE_ALL:
                qb.setTables(DBHelperBase.TABLE_CALLRULE);
                 break;

            case MATCH_RECORD:
                qb.appendWhere(BaseColumns._ID + "=" + uri.getPathSegments().get(1));
                qb.setTables(DBHelperBase.TABLE_RECORD);
                break;

            case MATCH_RECORD_ALL:
                qb.setTables(DBHelperBase.TABLE_RECORD);
                break;

            case MATCH_SUBSCRIBE:
                qb.appendWhere(BaseColumns._ID + "=" + uri.getPathSegments().get(1));
                qb.setTables(DBHelperBase.TABLE_SUBSCRIB);
                break;
            case MATCH_SUBSCRIBE_ALL:
                qb.setTables(DBHelperBase.TABLE_SUBSCRIB);
                break;
            case MATCH_VIEW_CHAT_SESSION:
                qb.setTables(DBHelperBase.VIEW_CHATSESSION);
                qb.appendWhere(BaseColumns._ID + "=" + uri.getPathSegments().get(1));
                break;

            case MATCH_VIEW_CHAT_SESSION_ALL:
                qb.setTables(DBHelperBase.VIEW_CHATSESSION);
                break;
            case MATCH_MESSAGE:
                qb.setTables(DBHelperBase.TABLE_MESSAGE);
                qb.appendWhere(BaseColumns._ID + "=" + uri.getPathSegments().get(1));
//                qb.setProjectionMap(DBHelperBase.MessageColumns.messagetroyProjectionMap);

                break;
            case MATCH_MESSAGE_ALL:
                qb.setTables(DBHelperBase.TABLE_MESSAGE);
//                qb.setProjectionMap(DBHelperBase.MessageColumns.messagetroyProjectionMap);
                if (TextUtils.isEmpty(orderBy)) {
                    orderBy = DBHelperBase.MessageColumns.DEFAULT_ORDER;
                }
                break;
        }

        // Get the database and run the query
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor =null;
        try {
            cursor = qb.query(db, projection, selection, selectionArgs, groupby, having, orderBy);
            cursor.setNotificationUri(getContext().getContentResolver(),uri);
        }catch (SQLException e){

        }
        return cursor;
    }

    @Nullable
	@Override
	public String getType(@NonNull Uri uri) {
		return null;
	}

	@Nullable
	@Override
	public Uri insert(@NonNull Uri uri, @Nullable ContentValues contentValues) {
        SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
        String tableName= "";
        Uri groupUri= null;
        UriMatcher uriMatcher = UriMactherHepler.getUriMatcher();
        List<Uri> notifylist = new ArrayList<>();
        switch (uriMatcher.match(uri)) { //

            case MATCH_ACCOUNT_ALL:
                tableName = DBHelperBase.TABLE_ACCOUNT;
                notifylist.add(uri);
                // Make sure that the fields are all set
                break;
            case MATCH_HISTORY_ALL:
                tableName = DBHelperBase.TABLE_HISTORY;
                notifylist.add(uri);
                notifylist.add(DBHelperBase.ViewHistoryColumns.CONTENT_URI);
                break;
            case MATCH_REMOTE_ALL:
                tableName = DBHelperBase.TABLE_REMOTE;
                notifylist.add(uri);
                notifylist.add(DBHelperBase.ViewHistoryColumns.CONTENT_URI);
                notifylist.add(DBHelperBase.ViewChatSessionColumns.CONTENT_URI);

                break;

            case MATCH_CONTACT_VERSION_ALL:
                tableName = DBHelperBase.TABLE_CONTACT_VERSION;//huo qu suo you
                notifylist.add(uri);
                break;

            case MATCH_MESSAGE_ALL:
                tableName = DBHelperBase.TABLE_MESSAGE;
                notifylist.add(uri);
                notifylist.add(DBHelperBase.ChatSessionColumns.CONTENT_URI);
                notifylist.add(DBHelperBase.ViewChatSessionColumns.CONTENT_URI);
                break;
            case MATCH_CHAT_SESSION_ALL:
                tableName = DBHelperBase.TABLE_CHATSESSION;
                notifylist.add(uri);
                break;

            case MATCH_CALL_RULE_ALL:
                tableName = DBHelperBase.TABLE_CALLRULE;
                notifylist.add(uri);
                break;
            case MATCH_RECORD_ALL:

                tableName = DBHelperBase.TABLE_RECORD;
                notifylist.add(uri);
                break;
            case MATCH_SUBSCRIBE_ALL:
                tableName = DBHelperBase.TABLE_SUBSCRIB;//huo qu suo you
                notifylist.add(uri);
                break;

            case MATCH_SUBSCRIBE:
            case MATCH_CALL_RULE:
            case MATCH_ACCOUNT:
            case MATCH_HISTORY:
            case MATCH_MESSAGE:
            case MATCH_CHAT_SESSION:
            case MATCH_VIEW_HISTORY:
            case MATCH_VIEW_HISTORY_ALL:
            case MATCH_VIEW_CHAT_SESSION:
            case MATCH_VIEW_CHAT_SESSION_ALL:
            case MATCH_REMOTE:
            case MATCH_CONTACT_VERSION:
            case MATCH_RECORD:
                default:
                throw new IllegalArgumentException("Invalidate URI " + uri);
        }

        // Get the database and run the query
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        long rowId = db.insert(tableName, null,contentValues);

        if (rowId > 0) {
            Uri noteUri = ContentUris.withAppendedId(uri, rowId);
            notifylist.add(noteUri);
            for (Uri notifyUri:notifylist) {
                getContext().getContentResolver().notifyChange(notifyUri, null);
            }
            return noteUri;
        }

        throw new SQLException("Failed to insert row into " + uri);
	}

	@Override
	public int delete(@NonNull Uri uri, @Nullable String where, @Nullable String[] whereArgs) {
        SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
        UriMatcher uriMatcher = UriMactherHepler.getUriMatcher();
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        String tableName=null,clumnId =null;
        List<Uri> notifylist = new ArrayList<>();
        notifylist.add(uri);
        switch (uriMatcher.match(uri)) { //

            case MATCH_ACCOUNT:                             // huoqu mou yi tiao
                tableName = DBHelperBase.TABLE_ACCOUNT;
                clumnId = uri.getPathSegments().get(1);
                where = BaseColumns._ID + "=" + clumnId + (!TextUtils.isEmpty(where) ? " AND (" + where + ')' : "");

                break;
            case MATCH_ACCOUNT_ALL:
//            case MATCH_HISTORY_GROUPBY_ALL:
                tableName = DBHelperBase.TABLE_ACCOUNT;//huo qu suo you
                break;

            case MATCH_CALL_RULE:
                tableName = DBHelperBase.TABLE_CALLRULE;//huo qu suo you
                clumnId = uri.getPathSegments().get(1);
                where = BaseColumns._ID + "=" + clumnId + (!TextUtils.isEmpty(where) ? " AND (" + where + ')' : "");
                break;

            case MATCH_CALL_RULE_ALL:
                tableName = DBHelperBase.TABLE_CALLRULE;//huo qu suo you
                break;

            case MATCH_CONTACT_VERSION:                             // huoqu mou yi tiao
                tableName = DBHelperBase.TABLE_CONTACT_VERSION;
                clumnId = uri.getPathSegments().get(1);
                where = BaseColumns._ID + "=" + clumnId + (!TextUtils.isEmpty(where) ? " AND (" + where + ')' : "");

                break;
            case MATCH_CONTACT_VERSION_ALL:
                tableName = DBHelperBase.TABLE_CONTACT_VERSION;//huo qu suo you
                break;

            case MATCH_RECORD:                             // huoqu mou yi tiao
                tableName = DBHelperBase.TABLE_RECORD;
                clumnId = uri.getPathSegments().get(1);
                where = BaseColumns._ID + "=" + clumnId + (!TextUtils.isEmpty(where) ? " AND (" + where + ')' : "");
                break;
            case MATCH_RECORD_ALL:
                tableName = DBHelperBase.TABLE_RECORD;//huo qu suo you
                break;

            case MATCH_SUBSCRIBE_ALL:
                tableName = DBHelperBase.TABLE_SUBSCRIB;//huo qu suo you
                break;
            case MATCH_SUBSCRIBE:
                tableName = DBHelperBase.TABLE_SUBSCRIB;
                clumnId = uri.getPathSegments().get(1);
                where = BaseColumns._ID + "=" + clumnId + (!TextUtils.isEmpty(where) ? " AND (" + where + ')' : "");
                break;

            case MATCH_REMOTE:
                tableName = DBHelperBase.TABLE_REMOTE;
                clumnId = uri.getPathSegments().get(1);
                where = BaseColumns._ID + "=" + clumnId + (!TextUtils.isEmpty(where) ? " AND (" + where + ')' : "");
                notifylist.add(DBHelperBase.ViewHistoryColumns.CONTENT_URI);
                notifylist.add(DBHelperBase.ViewChatSessionColumns.CONTENT_URI);
                break;
            case MATCH_REMOTE_ALL:
                tableName = DBHelperBase.TABLE_REMOTE;
                notifylist.add(DBHelperBase.ViewHistoryColumns.CONTENT_URI);
                notifylist.add(DBHelperBase.ViewChatSessionColumns.CONTENT_URI);
                break;
            case MATCH_HISTORY:
//            case MATCH_HISTORY_GROUPBY:
                tableName = DBHelperBase.TABLE_HISTORY;
                clumnId = uri.getPathSegments().get(1);
                where = BaseColumns._ID + "=" + clumnId + (!TextUtils.isEmpty(where) ? " AND (" + where + ')' : "");
                notifylist.add(DBHelperBase.ViewHistoryColumns.CONTENT_URI);
                break;
            case MATCH_HISTORY_ALL:
                tableName = DBHelperBase.TABLE_HISTORY;
                notifylist.add(DBHelperBase.ViewHistoryColumns.CONTENT_URI);
                break;
            case MATCH_MESSAGE:
                clumnId = uri.getPathSegments().get(1);
                where = BaseColumns._ID + "=" + clumnId + (!TextUtils.isEmpty(where) ? " AND (" + where + ')' : "");
                tableName = DBHelperBase.TABLE_MESSAGE;
                notifylist.add(DBHelperBase.ChatSessionColumns.CONTENT_URI);
                notifylist.add(DBHelperBase.ViewChatSessionColumns.CONTENT_URI);
                break;
            case MATCH_MESSAGE_ALL:
                tableName = DBHelperBase.TABLE_MESSAGE;
                notifylist.add(DBHelperBase.ChatSessionColumns.CONTENT_URI);
                notifylist.add(DBHelperBase.ViewChatSessionColumns.CONTENT_URI);
                break;
            case MATCH_CHAT_SESSION:
                clumnId = uri.getPathSegments().get(1);
                where = BaseColumns._ID + "=" + clumnId + (!TextUtils.isEmpty(where) ? " AND (" + where + ')' : "");
                tableName = DBHelperBase.TABLE_CHATSESSION;
                notifylist.add(DBHelperBase.MessageColumns.CONTENT_URI);
                notifylist.add(DBHelperBase.ViewChatSessionColumns.CONTENT_URI);

                break;
            case MATCH_CHAT_SESSION_ALL:
                tableName = DBHelperBase.TABLE_CHATSESSION;
                notifylist.add(DBHelperBase.MessageColumns.CONTENT_URI);
                notifylist.add(DBHelperBase.ViewChatSessionColumns.CONTENT_URI);
                break;
            case MATCH_VIEW_HISTORY:
            case MATCH_VIEW_HISTORY_ALL:
            case MATCH_VIEW_CHAT_SESSION:
            case MATCH_VIEW_CHAT_SESSION_ALL:
            default:
                throw new IllegalArgumentException("Invalidate URI " + uri);
        }

        // Get the database and run the query
        int count = db.delete(tableName, where, whereArgs);
        if(count>0) {
            for(Uri notify:notifylist) {
                getContext().getContentResolver().notifyChange(notify, null);
            }
        }
		return count;
	}

	@Override
	public int update(@NonNull Uri uri, @Nullable ContentValues contentValues, @Nullable String where, @Nullable String[] whereArgs) {
        SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
        UriMatcher uriMatcher = UriMactherHepler.getUriMatcher();
        String tableName=null,clumnId =null;
        List<Uri> notifylist = new ArrayList<>();
        notifylist.add(uri);
        switch (uriMatcher.match(uri)) { //
            case MATCH_ACCOUNT:                             //
                tableName = DBHelperBase.TABLE_ACCOUNT;
                clumnId = uri.getPathSegments().get(1);
                where = BaseColumns._ID + "=" + clumnId + (!TextUtils.isEmpty(where) ? " AND (" + where + ')' : "");
                break;
            case MATCH_ACCOUNT_ALL:                         //
                tableName = DBHelperBase.TABLE_ACCOUNT;
                break;
            case MATCH_CHAT_SESSION:                             //
                tableName = DBHelperBase.TABLE_CHATSESSION;
                clumnId = uri.getPathSegments().get(1);
                where = BaseColumns._ID + "=" + clumnId + (!TextUtils.isEmpty(where) ? " AND (" + where + ')' : "");
                notifylist.add(DBHelperBase.ViewChatSessionColumns.CONTENT_URI);
                break;
            case MATCH_CHAT_SESSION_ALL:                         //
                tableName = DBHelperBase.TABLE_CHATSESSION;
                break;

            case MATCH_RECORD:
                tableName = DBHelperBase.TABLE_RECORD;
                clumnId = uri.getPathSegments().get(1);
                where = BaseColumns._ID + "=" + clumnId + (!TextUtils.isEmpty(where) ? " AND (" + where + ')' : "");
                break;
            case MATCH_RECORD_ALL:
                tableName = DBHelperBase.TABLE_RECORD;
                break;

            case MATCH_SUBSCRIBE:
                tableName = DBHelperBase.TABLE_SUBSCRIB;
                clumnId = uri.getPathSegments().get(1);
                where = BaseColumns._ID + "=" + clumnId + (!TextUtils.isEmpty(where) ? " AND (" + where + ')' : "");
                break;

            case MATCH_SUBSCRIBE_ALL:
                tableName = DBHelperBase.TABLE_SUBSCRIB;
                break;

            case MATCH_CONTACT_VERSION:
                tableName = DBHelperBase.TABLE_CONTACT_VERSION;
                clumnId = uri.getPathSegments().get(1);
                where = BaseColumns._ID + "=" + clumnId + (!TextUtils.isEmpty(where) ? " AND (" + where + ')' : "");
                break;
            case MATCH_CONTACT_VERSION_ALL:
                tableName = DBHelperBase.TABLE_CONTACT_VERSION;
                break;

            case MATCH_REMOTE:
                tableName = DBHelperBase.TABLE_REMOTE;
                clumnId = uri.getPathSegments().get(1);
                where = BaseColumns._ID + "=" + clumnId + (!TextUtils.isEmpty(where) ? " AND (" + where + ')' : "");
                notifylist.add(DBHelperBase.ViewHistoryColumns.CONTENT_URI);
                notifylist.add(DBHelperBase.ViewChatSessionColumns.CONTENT_URI);
                break;
            case MATCH_REMOTE_ALL:
                tableName = DBHelperBase.TABLE_REMOTE;
                notifylist.add(DBHelperBase.ViewHistoryColumns.CONTENT_URI);
                notifylist.add(DBHelperBase.ViewChatSessionColumns.CONTENT_URI);
                break;

            case MATCH_CALL_RULE:
                tableName = DBHelperBase.TABLE_CALLRULE;//
                clumnId = uri.getPathSegments().get(1);
                where = BaseColumns._ID + "=" + clumnId + (!TextUtils.isEmpty(where) ? " AND (" + where + ')' : "");
                break;

            case MATCH_CALL_RULE_ALL:
                tableName = DBHelperBase.TABLE_CALLRULE;//
                break;

            case MATCH_HISTORY:
                tableName = DBHelperBase.TABLE_HISTORY;
                clumnId = uri.getPathSegments().get(1);
                where = BaseColumns._ID + "=" + clumnId + (!TextUtils.isEmpty(where) ? " AND (" + where + ')' : "");
                notifylist.add(DBHelperBase.ViewHistoryColumns.CONTENT_URI);
                break;
            case MATCH_HISTORY_ALL:
                tableName = DBHelperBase.TABLE_HISTORY;
                notifylist.add(DBHelperBase.ViewHistoryColumns.CONTENT_URI);
                break;
            case MATCH_MESSAGE:
                tableName = DBHelperBase.TABLE_MESSAGE;
                clumnId = uri.getPathSegments().get(1);
                where = BaseColumns._ID + "=" + clumnId + (!TextUtils.isEmpty(where) ? " AND (" + where + ')' : "");
                notifylist.add(DBHelperBase.ChatSessionColumns.CONTENT_URI);
                notifylist.add(DBHelperBase.ViewChatSessionColumns.CONTENT_URI);
                break;
            case MATCH_MESSAGE_ALL:
                tableName = DBHelperBase.TABLE_MESSAGE;
                notifylist.add(DBHelperBase.ChatSessionColumns.CONTENT_URI);
                notifylist.add(DBHelperBase.ViewChatSessionColumns.CONTENT_URI);
                break;
            case MATCH_VIEW_HISTORY:
            case MATCH_VIEW_HISTORY_ALL:
            case MATCH_VIEW_CHAT_SESSION:
            case MATCH_VIEW_CHAT_SESSION_ALL:
            default:
                throw new IllegalArgumentException("Invalidate URI " + uri);
        }

        // Get the database and run the query
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        int count = db.update(tableName, contentValues, where, whereArgs);
        if(count>0) {
            for(Uri notify:notifylist){
                getContext().getContentResolver().notifyChange(notify, null);
            }
        }
        return count;
    }
}
