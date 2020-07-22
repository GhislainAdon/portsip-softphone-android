package com.portgo.database;


import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.Uri;
import android.provider.BaseColumns;
import android.provider.ContactsContract;

import com.portgo.manager.AccountManager;
import com.portgo.manager.HistoryAVCallEvent;

import java.io.File;
import java.util.DuplicateFormatFlagsException;
import java.util.HashMap;

import static com.portgo.database.DBHelperBase.AccountColumns.ACCOUNT_LAST_TIME_LOGIN;
import static com.portgo.database.UriMactherHepler.AUTHORITY;

public class DBHelperBase {
	private DatabaseHelper mDbHelper;
	private static final String DATABASE_NAME = "PortSip.db";
    protected static final String TABLE_HISTORY ="newhistory";
    protected static final String TABLE_ACCOUNT ="account";
    protected static final String TABLE_RECORD ="records";
    protected static final String TABLE_MESSAGE ="message";
    protected static final String TABLE_CHATSESSION="session";
    protected static final String TABLE_REMOTE="remote";
    protected static final String TABLE_CONTACT_VERSION="version";

    protected static final String TABLE_CALLRULE="callrule";
    protected static final String TABLE_SUBSCRIB="subscribe";

    protected static final String VIEW_CHATSESSION="view_session";
    protected static final String VIEW_HISTORY="view_history";

	private static final int DATABASE_VERSION = 11;

    public DBHelperBase(Context context){
        mDbHelper = new DatabaseHelper(context);

    }

    public SQLiteDatabase getReadableDatabase()
    {
        return mDbHelper.getReadableDatabase();
    }

    public SQLiteDatabase getWritableDatabase() {
        return mDbHelper.getWritableDatabase();
    }
    private boolean mainTmpDirSet = false;
	private class DatabaseHelper extends SQLiteOpenHelper {
        final Context mContext;
		DatabaseHelper(Context context) {
			super(context, DATABASE_NAME, null, DATABASE_VERSION);
            mContext = context;
		}

        @Override
        public SQLiteDatabase getReadableDatabase() {
            if (!mainTmpDirSet) {
                //File syscache = mContext.getExternalFilesDir(null);
                File syscache = mContext.getCacheDir();
                String appcache= syscache.getAbsolutePath()+"/databases";
                boolean rs = new File(appcache).mkdirs();
                if(rs) {
                    super.getReadableDatabase().execSQL("PRAGMA temp_store_directory = '"+appcache+"'");
                }
                mainTmpDirSet = true;
                return super.getReadableDatabase();
            }
            return super.getReadableDatabase();
        }
		@Override
		public void onCreate(SQLiteDatabase db) {
            String sql = "CREATE TABLE " + TABLE_HISTORY + " ("
                    + HistoryColumns._ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"+ HistoryColumns.HISTORY_MIDIATYPE+ " INTEGER,"
                    + HistoryColumns.HISTORY_STARTTIME + " INTEGER,"+ HistoryColumns.HISTORY_ENDTIME + " INTEGER,"
                    + HistoryColumns.HISTORY_HASRECORD + " INTEGER,"+ HistoryColumns.HISTORY_GROUP + " INTEGER,"
                    + HistoryColumns.HISTORY_INCALLTIME + " INTEGER,"+ HistoryColumns.HISTORY_LOCAL + " TEXT,"
                    + HistoryColumns.HISTORY_REMOTE_ID + " INTEGER DEFAULT 0," + HistoryColumns.HISTORY_DISPLAYNAME + " TEXT,"
                    + HistoryColumns.HISTORY_CALLID + " INTEGER,"+ HistoryColumns.HISTORY_CALLOUT + " INTEGER,"
                    + HistoryColumns.HISTORY_SEEN + " INTEGER,"+ HistoryColumns.HISTORY_CONNECTED + " INTEGER" + ");";
            db.execSQL(sql);

            // 创建Account表
            sql = "CREATE TABLE " + TABLE_ACCOUNT + " ("
                    + AccountColumns._ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"+ AccountColumns.ACCOUNT_NAME + " TEXT,"                //dengluming ,zhang hao
                    + AccountColumns.ACCOUNT_DISPLAYNAME + " TEXT," + AccountColumns.ACCOUNT_PASSWORD + " TEXT,"            //mi ma
                    + AccountColumns.ACCOUNT_REALM + " TEXT,"+ AccountColumns.ACCOUNT_TRANS_TYPE + " INTEGER,"
                    + AccountColumns.ACCOUNT_PORT + " INTEGER,"+ AccountColumns.ACCOUNT_AUTHOR + " TEXT,"
                    + AccountColumns.ACCOUNT_DOMAIN + " TEXT,"+ AccountColumns.ACCOUNT_DEFAULT + " INTEGER,"
                    + AccountColumns.ACCOUNT_REMBER_PASSWORD + " INTEGER,"+ AccountColumns.ACCOUNT_LOG_ENABLE + " INTEGER,"
                    + AccountColumns.ACCOUNT_STUN_ENABLE + " INTEGER,"+ AccountColumns.ACCOUNT_STUN_SERVER+ " TEXT,"
                    + AccountColumns.ACCOUNT_STUN_PORT+ " INTEGER,"+ AccountColumns.ACCOUNT_FOWARD_MODE+ " INTEGER DEFAULT 0,"
                    + AccountColumns.ACCOUNT_FOWARD_TIME+ " INTEGER DEFAULT 20," + AccountColumns.ACCOUNT_DISTRUB_MODE+ " INTEGER DEFAULT 0,"
                    + AccountColumns.ACCOUNT_FOWARDTO+ " TEXT,"+ AccountColumns.ACCOUNT_VOICEMAIL+ " TEXT," + AccountColumns.ACCOUNT_MAILSIZE+ " INTEGER DEFAULT 0,"
                    +AccountColumns.ACCOUNT_AVATAR+ " BLOB,"+AccountColumns.ACCOUNT_LAST_TIME_LOGIN+ " INTEGER,"
                    + AccountColumns.ACCOUNT_PRESENCE+ " INTEGER DEFAULT 5,"+ AccountColumns.ACCOUNT_PRESENCE_STATUS+ " TEXT, "
            +AccountColumns.ACCOUNT_EMAIL+" TEXT, "+AccountColumns.ACCOUNT_EMAIL_PWD+" TEXT);";
            db.execSQL(sql);


            //
            sql = "CREATE TABLE " + TABLE_RECORD + " ("
                    + RecordColumns._ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"+ RecordColumns.RECORD_FILE_NAME + " TEXT,"                //dengluming ,zhang hao
                    + RecordColumns.RECORD_FROM + " TEXT," + RecordColumns.RECORD_TO + " TEXT,"            //mi ma
                    + RecordColumns.RECORD_TIME + " INTEGER,"+ RecordColumns.RECORD_DURATION + " INTEGER,"
                    + RecordColumns.RECORD_CALLID + " INTEGER,"+ RecordColumns.RECORD_MEDIATYPE + " INTEGER);";
            db.execSQL(sql);

            //

            sql = "CREATE TABLE " + TABLE_CHATSESSION + " ("+ChatSessionColumns._ID+ " INTEGER PRIMARY KEY AUTOINCREMENT,"
                    +ChatSessionColumns.SESSION_LOCAL+ " INTEGER,"+ChatSessionColumns.SESSION_LOCAL_URI+ " TEXT,"
                    +ChatSessionColumns.SESSION_REMOTE_ID+ " INTEGER,"+ChatSessionColumns.SESSION_DELETE+ " INTEGER DEFAULT 0,"
                    +ChatSessionColumns.SESSION_STATUS + " TEXT," +ChatSessionColumns.SESSION_LASTTIME + " INTEGER,"
                    +ChatSessionColumns.SESSION_UNREAD + " INTEGER DEFAULT 0);";
            db.execSQL(sql);

            sql = "CREATE TABLE " + TABLE_CONTACT_VERSION + " ("+ContactVersionColumns._ID+ " INTEGER PRIMARY KEY AUTOINCREMENT,"
                    +ContactVersionColumns.VERSION_CONTACT_ID+ " INTEGER,"+ContactVersionColumns.VERSION_RAW_CONTACT_ID+ " INTEGER,"+ContactVersionColumns.VERSION_CONTACT_TYPE+ " INTEGER,"
                    +ContactVersionColumns.VERSION_CONTACT_DATAVERSION+ " INTEGER,"
                    +ContactVersionColumns.VERSION_CONTACT_LASTCONTACT + " INTEGER);";
            db.execSQL(sql);

            sql = "CREATE TABLE " + TABLE_REMOTE + " ("+RemoteColumns._ID+ " INTEGER PRIMARY KEY AUTOINCREMENT,"
                    +RemoteColumns.REMOTE_CONTACT_ID+ " INTEGER DEFAULT 0,"+RemoteColumns.REMOTE_CONTACT_TYPE+ " INTEGER DEFAULT 0,"
                    +RemoteColumns.REMOTE_URI+ " TEXT,"+RemoteColumns.REMOTE_DISPPLAY_NAME+ " TEXT);";
            db.execSQL(sql);

            sql = "CREATE TABLE " + TABLE_MESSAGE + " ("+MessageColumns._ID+ " INTEGER PRIMARY KEY AUTOINCREMENT,"
                    +MessageColumns.MESSAGE_TIME + " INTEGER,"+MessageColumns.MESSAGE_LEN + " INTEGER,"+MessageColumns.MESSAGE_DELETE + " INTEGER DEFAULT 0,"
                    +MessageColumns.MESSAGE_SESSION_ID+ " INTEGER,"+MessageColumns.MESSAGE_SENDOUT + " INTEGER,"
                    +MessageColumns.MESSAGE_DISPLAYNAME + " TEXT,"+MessageColumns.MESSAGE_SEEN + " INTEGER,"
                    +MessageColumns.MESSAGE_STATUS + " INTEGER,"+MessageColumns.MESSAGE_MIME + " TEXT,"+MessageColumns.MESSAGE_DESCRIPTION + " TEXT,"
                    +MessageColumns.MESSAGE_CONTENT + " BLOB," +MessageColumns.MESSAGE_ID + " TEXT," +MessageColumns.MESSAGE_TYPE + " TEXT"+ ");";
            db.execSQL(sql);

            sql = "CREATE VIEW " + VIEW_CHATSESSION + " AS SELECT "+TABLE_CHATSESSION+"."+ChatSessionColumns._ID+ ","
                    +ChatSessionColumns.SESSION_LOCAL+ ","+ChatSessionColumns.SESSION_LOCAL_URI+ ","
                    +ChatSessionColumns.SESSION_REMOTE_ID+ ","
                    +ChatSessionColumns.SESSION_STATUS + "," +ChatSessionColumns.SESSION_LASTTIME + ","+ChatSessionColumns.SESSION_DELETE+ ","
                    +ChatSessionColumns.SESSION_UNREAD + ","+RemoteColumns.REMOTE_CONTACT_ID+ ","
                    +RemoteColumns.REMOTE_CONTACT_TYPE+ ","+RemoteColumns.REMOTE_URI+ ","+RemoteColumns.REMOTE_DISPPLAY_NAME
                    +" FROM "+TABLE_CHATSESSION+" JOIN "+TABLE_REMOTE +" ON "
                    +TABLE_CHATSESSION+"."+ChatSessionColumns.SESSION_REMOTE_ID+"="+TABLE_REMOTE+"."+RemoteColumns._ID+";";
            db.execSQL(sql);

            sql = "CREATE VIEW " + VIEW_HISTORY + " AS SELECT "+TABLE_HISTORY+"."+ HistoryColumns._ID + ","+ HistoryColumns.HISTORY_MIDIATYPE+ ","
                    + HistoryColumns.HISTORY_STARTTIME + ","+ HistoryColumns.HISTORY_ENDTIME + ","
                    + HistoryColumns.HISTORY_INCALLTIME + ","+ HistoryColumns.HISTORY_LOCAL + ","
                    + HistoryColumns.HISTORY_REMOTE_ID + "," + HistoryColumns.HISTORY_DISPLAYNAME + ","
                    + HistoryColumns.HISTORY_HASRECORD+ ", "+ HistoryColumns.HISTORY_GROUP + ","
                    + HistoryColumns.HISTORY_CALLID + ","+ HistoryColumns.HISTORY_CALLOUT + ","
                    + HistoryColumns.HISTORY_SEEN + ","+ HistoryColumns.HISTORY_CONNECTED + ","+RemoteColumns.REMOTE_CONTACT_ID
                    + ","+RemoteColumns.REMOTE_CONTACT_TYPE+ ","+RemoteColumns.REMOTE_URI+ ","+RemoteColumns.REMOTE_DISPPLAY_NAME
                    +" FROM "+TABLE_HISTORY+" JOIN "+TABLE_REMOTE +" ON "
                    +TABLE_HISTORY+"."+HistoryColumns.HISTORY_REMOTE_ID+"="+TABLE_REMOTE+"."+RemoteColumns._ID+";";
            db.execSQL(sql);

            sql = "CREATE TABLE " + TABLE_CALLRULE + " ("+CallRuleColumns._ID+ " INTEGER PRIMARY KEY AUTOINCREMENT,"
                    +CallRuleColumns.CALL_RULE_ACCOUNT_ID + " INTEGER,"+CallRuleColumns.CALL_RULE_NAME + " TEXT NOT NULL,"
                    +CallRuleColumns.CALL_RULE_MATCHER+ " TEXT,"+CallRuleColumns.CALL_RULE_ADDPREFIX+ " TEXT,"
                    +CallRuleColumns.CALL_RULE_PRIORITY+ " INTEGER DEFAULT 0,"+CallRuleColumns.CALL_RULE_ENABLE+ " INTEGER DEFAULT 1,"
                    +CallRuleColumns.CALL_RULE_REMOVEPREFIX + " TEXT );";
            db.execSQL(sql);

            sql = "CREATE TABLE " + TABLE_SUBSCRIB + " ("+SubscribeColumns._ID+ " INTEGER PRIMARY KEY AUTOINCREMENT,"
                    +SubscribeColumns.SUBSCRIB_DESC + " TEXT,"+SubscribeColumns.SUBSCRIB_NAME + " TEXT,"
                    +SubscribeColumns.SUBSCRIB_REMOTE+ " TEXT,"+SubscribeColumns.SUBSCRIB_LOCAL+ " TEXT,"
                    +SubscribeColumns.SUBSCRIB_SUBID+ " INTEGER,"+SubscribeColumns.SUBSCRIB_SEEN+ " INTEGER DEFAULT 0,"
                    +SubscribeColumns.SUBSCRIB_TIME+ " INTEGER,"+SubscribeColumns.SUBSCRIB_ACCTION+ " INTEGER DEFAULT 0);";

            db.execSQL(sql);

            String trigger1="create trigger message_insert_check " +
                    "before insert on " +TABLE_MESSAGE+" "+
                    "for each row " +
                    "begin " +
                    "select raise(rollback,'on session_id') " +
                    "where (select _id from session where _id=new.session_id) is null; " +
                    "end;";
            String trigger2="create trigger insert_unread " +
                    "after insert on " +TABLE_MESSAGE+" "+
                    "for each row " +
                    "when new."+MessageColumns.MESSAGE_SEEN+">0 "+
                    "begin " +
                    "update "+TABLE_CHATSESSION+" set "+ChatSessionColumns.SESSION_UNREAD+"="+ChatSessionColumns.SESSION_UNREAD+"+1 "+"where "+ChatSessionColumns._ID+"=new."+MessageColumns.MESSAGE_SESSION_ID +"; "+
                    "end;";
            String trigger3="create trigger insert_message " +
                    "after insert on " +TABLE_MESSAGE+" "+
                    "for each row " +
                    "begin " +
                    "update "+TABLE_CHATSESSION+" set "+ChatSessionColumns.SESSION_STATUS+"=new."+MessageColumns.MESSAGE_DESCRIPTION+", "+
                    ChatSessionColumns.SESSION_LASTTIME+"= new."+MessageColumns.MESSAGE_TIME+" "+ "where "+ChatSessionColumns._ID+"=new."+MessageColumns.MESSAGE_SESSION_ID +
                    "; "+
                    "end;";
            String trigger4="create trigger update_read " +
                    "before update on " +TABLE_MESSAGE+" "+
                    "for each row " +
                    "when old."+MessageColumns.MESSAGE_SEEN+">0 AND new."+MessageColumns.MESSAGE_SEEN+"<=0 "+
                    "begin " +
                    "update "+TABLE_CHATSESSION+" set "+ChatSessionColumns.SESSION_UNREAD+"="+ChatSessionColumns.SESSION_UNREAD+"-1 "+"where "+ChatSessionColumns._ID+"=new."+MessageColumns.MESSAGE_SESSION_ID +"; "+
                    "end;";


            String trigger5="create trigger update_unread " +
                    "before update on " +TABLE_MESSAGE+" "+
                    "for each row " +
                    "when old."+MessageColumns.MESSAGE_SEEN+"<=0 AND new."+MessageColumns.MESSAGE_SEEN+">0 "+
                    "begin " +
                    "update "+TABLE_CHATSESSION+" set "+ChatSessionColumns.SESSION_UNREAD+"="+ChatSessionColumns.SESSION_UNREAD+"+1 "+"where "+ChatSessionColumns._ID+"=new."+MessageColumns.MESSAGE_SESSION_ID +"; "+
                    "end;";

            String trigger6="create trigger session_delete " +
                    "before delete on " +TABLE_CHATSESSION+" "+
                    "for each row " +
                    "begin " +
                    "delete from " +TABLE_MESSAGE+" where "+MessageColumns.MESSAGE_SESSION_ID+"="+"old."+ChatSessionColumns._ID+"; " +
                    "end;";

            String triggerAccountinsert="create trigger defaut_account_insert " +
                    "before insert on " +TABLE_ACCOUNT+" "+
                    "for each row " +
                    "when new."+AccountColumns.ACCOUNT_DEFAULT+">0 "+
                    "begin " +
                    "update "+TABLE_ACCOUNT+" set "+AccountColumns.ACCOUNT_DEFAULT+"=0; "+
                    "end;";

            String triggerAccountupdate="create trigger defaut_account_update " +
                    "before update on " +TABLE_ACCOUNT+" "+
                    "for each row " +
                    "when new."+AccountColumns.ACCOUNT_DEFAULT+">0 AND old."+ AccountColumns.ACCOUNT_DEFAULT+"<=0 "+
                    "begin " +
                    "update "+TABLE_ACCOUNT+" set "+AccountColumns.ACCOUNT_DEFAULT+"=0; "+
                    "end;";

            String triggerAccountdel="create trigger count_del " +
                    "before delete on " +TABLE_ACCOUNT+" "+
                    "for each row " +
                    "begin " +
                    "delete from " +TABLE_CALLRULE+" where "+CallRuleColumns.CALL_RULE_ACCOUNT_ID+"="+"old."+AccountColumns._ID+"; " +
                    "end;";


            String triggerpriority="create trigger rulepriority " +
                    "after insert on " +TABLE_CALLRULE+" "+
                    "for each row " +
                    "begin " +
                    "update "+TABLE_CALLRULE+" set "+CallRuleColumns.CALL_RULE_PRIORITY+"=(SELECT MAX("+CallRuleColumns.CALL_RULE_PRIORITY+") FROM "+TABLE_CALLRULE+")+1"+ " WHERE "+CallRuleColumns._ID+"=new."+CallRuleColumns._ID +"; "+
                    "end;";

            String trigger9="create trigger session_update_delete " +
                    "before update on " +TABLE_CHATSESSION+" "+
                    "for each row " +
                    "when new."+ChatSessionColumns.SESSION_DELETE+">0 AND old."+ ChatSessionColumns.SESSION_DELETE+"<=0 "+
                    "begin " +
                    "delete from " +TABLE_MESSAGE+" where "+MessageColumns.MESSAGE_SESSION_ID+"="+"old."+ChatSessionColumns._ID
                    +" and " +MessageColumns._ID+"<(select max("+MessageColumns._ID+") from "+TABLE_MESSAGE+");"+
                    "update " +TABLE_MESSAGE+" set "+MessageColumns.MESSAGE_DELETE+"=1 "+" where "+MessageColumns.MESSAGE_SESSION_ID+"="+"old."+ChatSessionColumns._ID+"; " +
                    "end;";
            db.execSQL(trigger1);
            db.execSQL(trigger2);
            db.execSQL(trigger3);
            db.execSQL(trigger4);
            db.execSQL(trigger5);
            db.execSQL(trigger6);
            db.execSQL(triggerAccountinsert);
            db.execSQL(triggerAccountupdate);
            db.execSQL(triggerAccountdel);
            db.execSQL(triggerpriority);
            db.execSQL(trigger9);

        }

		@Override
		public void onOpen(SQLiteDatabase db){
            if(!db.isReadOnly()) { // Enable foreign key constraints
                db.execSQL("PRAGMA foreign_keys = ON;");
            }
		}

		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            if(oldVersion<=1){
                updateVer1t2(db);
            }
			if(oldVersion<=2) {
                updateVer2t3(db);
			}
			if(oldVersion<=3){
                updateVer3t4(db);
            }

            if(oldVersion<6){//lao
			    dropTriger(db);
			    dropView(db);
                dropTable(db);
                onCreate(db);
            }
            if(oldVersion<7){
                updateVer6t7(db);
            }
            if(oldVersion<8){
                updateVer7t8(db);
            }
            if(oldVersion<9){
                updateVer8t9(db);
            }
            if(oldVersion<10){
                updateVer9t10(db);
            }
            if(oldVersion<11){
                updateVer10t11(db);
            }
        }
	}

    private  void updateVer1t2(SQLiteDatabase db){
    }

    private  void updateVer2t3(SQLiteDatabase db){
    }
	private  void updateVer3t4(SQLiteDatabase db){

	}

    private  void updateVer6t7(SQLiteDatabase db){
        String ADD_COLUMNS_GROUP = "ALTER TABLE "+TABLE_HISTORY+" ADD "+HistoryColumns.HISTORY_GROUP+" INTEGER ";

        db.execSQL(ADD_COLUMNS_GROUP);
        Cursor cursor =db.rawQuery("SELECT * FROM "+TABLE_HISTORY+" ORDER BY "+HistoryColumns.DEFAULT_ORDER,null);
        HistoryAVCallEvent prEvent  = null,event=null;
        ContentValues values =  new ContentValues(1);
        int group=1;
        while(cursor.moveToNext()){
            if(prEvent==null){
                prEvent = HistoryAVCallEvent.historyAVCallEventFromCursor(cursor);
                values.put(HistoryColumns.HISTORY_GROUP,group);
                db.update(TABLE_HISTORY,values,HistoryColumns._ID+"="+prEvent.getId(),null);
            }else {
                event  = HistoryAVCallEvent.historyAVCallEventFromCursor(cursor);
                if(!event.sameGroup(prEvent)){
                    group++;
                }
                values.put(HistoryColumns.HISTORY_GROUP,group);
                db.update(TABLE_HISTORY,values,HistoryColumns._ID+"="+event.getId(),null);
            }
        }
        cursor.close();
        String sql = "DROP VIEW "+ VIEW_HISTORY ;
        db.execSQL(sql);

        sql = "CREATE VIEW " + VIEW_HISTORY + " AS SELECT "+TABLE_HISTORY+"."+ HistoryColumns._ID + ","+ HistoryColumns.HISTORY_MIDIATYPE+ ","
                + HistoryColumns.HISTORY_STARTTIME + ","+ HistoryColumns.HISTORY_ENDTIME + ","
                + HistoryColumns.HISTORY_INCALLTIME + ","+ HistoryColumns.HISTORY_LOCAL + ","
                + HistoryColumns.HISTORY_REMOTE_ID + "," + HistoryColumns.HISTORY_DISPLAYNAME + ","
                + HistoryColumns.HISTORY_HASRECORD+ ", "+ HistoryColumns.HISTORY_GROUP + ","
                + HistoryColumns.HISTORY_CALLID + ","+ HistoryColumns.HISTORY_CALLOUT + ","
                + HistoryColumns.HISTORY_SEEN + ","+ HistoryColumns.HISTORY_CONNECTED + ","+RemoteColumns.REMOTE_CONTACT_ID
                + ","+RemoteColumns.REMOTE_CONTACT_TYPE+ ","+RemoteColumns.REMOTE_URI+ ","+RemoteColumns.REMOTE_DISPPLAY_NAME
                +" FROM "+TABLE_HISTORY+" JOIN "+TABLE_REMOTE +" ON "
                +TABLE_HISTORY+"."+HistoryColumns.HISTORY_REMOTE_ID+"="+TABLE_REMOTE+"."+RemoteColumns._ID+";";
        db.execSQL(sql);
    }

    void updateVer7t8(SQLiteDatabase db){
        String sql = "ALTER TABLE "+TABLE_ACCOUNT+" RENAME TO temp_"+TABLE_HISTORY;
        db.execSQL(sql);

        sql = "CREATE TABLE " + TABLE_HISTORY + " ("
                + HistoryColumns._ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"+ HistoryColumns.HISTORY_MIDIATYPE+ " INTEGER,"
                + HistoryColumns.HISTORY_STARTTIME + " INTEGER,"+ HistoryColumns.HISTORY_ENDTIME + " INTEGER,"
                + HistoryColumns.HISTORY_HASRECORD + " INTEGER,"+ HistoryColumns.HISTORY_GROUP + " INTEGER,"
                + HistoryColumns.HISTORY_INCALLTIME + " INTEGER,"+ HistoryColumns.HISTORY_LOCAL + " TEXT,"
                + HistoryColumns.HISTORY_REMOTE_ID + " INTEGER DEFAULT 0," + HistoryColumns.HISTORY_DISPLAYNAME + " TEXT,"
                + HistoryColumns.HISTORY_CALLID + " INTEGER,"+ HistoryColumns.HISTORY_CALLOUT + " INTEGER,"
                + HistoryColumns.HISTORY_SEEN + " INTEGER,"+ HistoryColumns.HISTORY_CONNECTED + " INTEGER" + ");";
        db.execSQL(sql);

        sql ="INSERT INTO "+TABLE_HISTORY+" SELECT * FROM  temp_"+TABLE_HISTORY;
        db.execSQL(sql);
        //
        sql ="DROP TABLE temp_"+TABLE_HISTORY;
        db.execSQL(sql);
    }

    void updateVer8t9(SQLiteDatabase db){
        //
        String sql = "ALTER TABLE "+TABLE_ACCOUNT+" ADD COLUMN "+ AccountColumns.ACCOUNT_EMAIL+ " TEXT";
        db.execSQL(sql);
        sql = "ALTER TABLE "+TABLE_ACCOUNT+" ADD COLUMN "+ AccountColumns.ACCOUNT_EMAIL_PWD+ " TEXT";
        db.execSQL(sql);

    }

    void updateVer9t10(SQLiteDatabase db){
        //
        String sql = "ALTER TABLE "+TABLE_MESSAGE+" ADD COLUMN "+ MessageColumns.MESSAGE_LEN+ " INTEGER";
        db.execSQL(sql);

        sql = "ALTER TABLE "+TABLE_MESSAGE+" ADD COLUMN "+ MessageColumns.MESSAGE_DELETE+ " INTEGER DEFAULT 0";
        db.execSQL(sql);

        sql = "ALTER TABLE "+TABLE_CHATSESSION+" ADD COLUMN "+ ChatSessionColumns.SESSION_DELETE+ " INTEGER DEFAULT 0";
        db.execSQL(sql);
    }
    void updateVer10t11(SQLiteDatabase db){
        String sql = "ALTER TABLE "+TABLE_MESSAGE+" ADD COLUMN "+ MessageColumns.MESSAGE_TYPE+ " TEXT";
        db.execSQL(sql);
        try {
            sql = "drop VIEW "+VIEW_CHATSESSION;
            db.execSQL(sql);
        }catch (Exception e){ }


        try {
            sql = "DROP TRIGGER insert_message";
            db.execSQL(sql);
        }catch (Exception e){ }

        String trigger3="create trigger insert_message " +
                "after insert on " +TABLE_MESSAGE+" "+
                "for each row " +
                "begin " +
                "update "+TABLE_CHATSESSION+" set "+ChatSessionColumns.SESSION_STATUS+"=new."+MessageColumns.MESSAGE_DESCRIPTION+", "+
                ChatSessionColumns.SESSION_LASTTIME+"= new."+MessageColumns.MESSAGE_TIME+" "+ "where "+ChatSessionColumns._ID+"=new."+MessageColumns.MESSAGE_SESSION_ID +
                "; "+
                "end;";
        db.execSQL(trigger3);

        try {
            sql = "DROP TRIGGER session_update_delete";
            db.execSQL(sql);
        }catch (Exception e){ }

        String trigger9="create trigger session_update_delete " +
                "before update on " +TABLE_CHATSESSION+" "+
                "for each row " +
                "when new."+ChatSessionColumns.SESSION_DELETE+">0 AND old."+ ChatSessionColumns.SESSION_DELETE+"<=0 "+
                "begin " +
                "delete from " +TABLE_MESSAGE+" where "+MessageColumns.MESSAGE_SESSION_ID+"="+"old."+ChatSessionColumns._ID
                +" and " +MessageColumns._ID+"<(select max("+MessageColumns._ID+") from "+TABLE_MESSAGE+");"+
                "update " +TABLE_MESSAGE+" set "+MessageColumns.MESSAGE_DELETE+"=1 "+" where "+MessageColumns.MESSAGE_SESSION_ID+"="+"old."+ChatSessionColumns._ID+"; " +
                "end;";
        db.execSQL(trigger9);

        sql = "CREATE VIEW " + VIEW_CHATSESSION + " AS SELECT "+TABLE_CHATSESSION+"."+ChatSessionColumns._ID+ ","
                +ChatSessionColumns.SESSION_LOCAL+ ","+ChatSessionColumns.SESSION_LOCAL_URI+ ","
                +ChatSessionColumns.SESSION_REMOTE_ID+ ","
                +ChatSessionColumns.SESSION_STATUS + "," +ChatSessionColumns.SESSION_LASTTIME + ","+ChatSessionColumns.SESSION_DELETE+ ","
                +ChatSessionColumns.SESSION_UNREAD + ","+RemoteColumns.REMOTE_CONTACT_ID+ ","
                +RemoteColumns.REMOTE_CONTACT_TYPE+ ","+RemoteColumns.REMOTE_URI+ ","+RemoteColumns.REMOTE_DISPPLAY_NAME
                +" FROM "+TABLE_CHATSESSION+" JOIN "+TABLE_REMOTE +" ON "
                +TABLE_CHATSESSION+"."+ChatSessionColumns.SESSION_REMOTE_ID+"="+TABLE_REMOTE+"."+RemoteColumns._ID+";";
        db.execSQL(sql);
    }


    static void dropTable(SQLiteDatabase db) {
        Cursor cursor = db.rawQuery("SELECT name FROM sqlite_master WHERE type ='table' AND name != 'sqlite_sequence'", null);
        if (cursor != null) {
            while (cursor.moveToNext()) {
                db.execSQL("DROP TABLE " + cursor.getString(0));
            }
        }
        if (cursor != null) {
            cursor.close();
            cursor = null;
        }
    }

    static void dropView(SQLiteDatabase db) {
        Cursor cursor = db.rawQuery("SELECT name FROM sqlite_master WHERE type ='view'", null);
        if (cursor != null) {
            while (cursor.moveToNext()) {
                db.execSQL("DROP VIEW " + cursor.getString(0));
            }
        }
        if (cursor != null) {
            cursor.close();
            cursor = null;
        }
    }

    static void dropTriger(SQLiteDatabase db) {
        Cursor cursor = db.rawQuery("SELECT name FROM sqlite_master WHERE type ='trigger'", null);
        if (cursor != null) {
            while (cursor.moveToNext()) {
                db.execSQL("DROP TRIGGER " + cursor.getString(0));
            }
        }
        if (cursor != null) {
            cursor.close();
            cursor = null;
        }
    }
    public static final class AccountColumns implements BaseColumns {
        public static final String ACCOUNT_NAME = "name";
        public static final String ACCOUNT_DISPLAYNAME = "disname";
        public static final String ACCOUNT_PASSWORD = "pwd";
        public static final String ACCOUNT_REALM = "realm";
        public static final String ACCOUNT_TRANS_TYPE = "transport";
        public static final String ACCOUNT_PORT = "port";
        public static final String ACCOUNT_AUTHOR = "author";
        public static final String ACCOUNT_DOMAIN = "domain";
        public static final String ACCOUNT_DEFAULT = "defaul";
        public static final String ACCOUNT_AVATAR = "avatar";
        public static final String ACCOUNT_REMBER_PASSWORD = "rememberme";
        public static final String ACCOUNT_LOG_ENABLE = "enablelog";
        public static final String ACCOUNT_STUN_ENABLE = "enablestun";
        public static final String ACCOUNT_STUN_SERVER= "stunserver";
        public static final String ACCOUNT_STUN_PORT= "stunport";
        public static final String ACCOUNT_VOICEMAIL="voicemail";
        public static final String ACCOUNT_MAILSIZE="mailcount";

        public static final String ACCOUNT_DISTRUB_MODE= "distrubmode";
        public static final String ACCOUNT_FOWARDTO= "fowardto";
        public static final String ACCOUNT_FOWARD_MODE= "fowardmode";
        public static final String ACCOUNT_FOWARD_TIME= "fowardtime";
        public static final String ACCOUNT_LAST_TIME_LOGIN= "last_time_signin";
        public static final String ACCOUNT_PRESENCE= "presence";
        public static final String ACCOUNT_PRESENCE_STATUS= "presence_status";

        public static final String ACCOUNT_EMAIL= "emailaddr";
        public static final String ACCOUNT_EMAIL_PWD= "emailpwd";

        public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY +"/"+DBHelperBase.TABLE_ACCOUNT);
        public static final String  DEFAULT_ORDER= "";
        public static final String DEFAULT_GROUPBY= null;
        public static final String DEFAULT_HAVING= null;
        public static final String GROUPBY= DEFAULT_GROUPBY;
        public static final String HAVING= DEFAULT_HAVING;

    }


    public static final class RecordColumns implements BaseColumns {
        public static final String RECORD_FILE_NAME = "filename";
        public static final String RECORD_TIME = "recordtime";
        public static final String RECORD_DURATION = "duration";
        public static final String RECORD_FROM = "sipfrom";                 //
        public static final String RECORD_TO = "sipto";       //
        public static final String RECORD_MEDIATYPE = "mediatype";
        public static final String RECORD_CALLID = "sessionid";

        public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY +"/"+DBHelperBase.TABLE_RECORD);
    }

    public static final class HistoryColumns implements BaseColumns {
        public static final String HISTORY_MIDIATYPE= "mediatype";
        public static final String HISTORY_STARTTIME = "starttime";//start call
        public static final String HISTORY_ENDTIME= "endTime";//call end
        public static final String HISTORY_INCALLTIME= "incalltime";//call connect
        public static final String HISTORY_LOCAL= "localparty";
        public static final String HISTORY_CALLID= "callid";
        public static final String HISTORY_DISPLAYNAME= "displayname";
        public static final String HISTORY_SEEN= "seen";
        public static final String HISTORY_CONNECTED = "connected";
        public static final String HISTORY_REMOTE_ID = "remoteid";
        public static final String HISTORY_CALLOUT= "callout";
        public static final String HISTORY_HASRECORD= "hasrecords";
        public static final String HISTORY_GROUP= "_group";

        public static final String TEMP_CLUMN_CALLCOUNT = "callcount";
        public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY +"/"+DBHelperBase.TABLE_HISTORY);
//        public static final Uri CONTENT_URI_GROUPBY = Uri.parse("content://" + AUTHORITY +"/"+DBHelperBase.TABLE_HISTORY_GROUPBY);
        public static final String DEFAULT_ORDER= HISTORY_STARTTIME+ " DESC";
        public static final String DEFAULT_GROUPBY= HISTORY_REMOTE_ID;
        public static final String DEFAULT_HAVING= null;

        public static String GROUPBY= DEFAULT_GROUPBY;
        public static final String HAVING= DEFAULT_HAVING;

    }

    public static final class MessageColumns implements BaseColumns {
        public static final String MESSAGE_ID= "messgeid";
        public static final String MESSAGE_TIME = "messagetime";
        public static final String MESSAGE_LEN = "messagelen";
        public static final String MESSAGE_DELETE = "removed";
        public static final String MESSAGE_TYPE = "messagetype";
        public static final String MESSAGE_SESSION_ID = "session_id";
        public static final String MESSAGE_DISPLAYNAME= "Displayname";
        public static final String MESSAGE_SEEN= "seen";
        public static final String MESSAGE_STATUS = "mStatus";
        public static final String MESSAGE_MIME = "mime";
        public static final String MESSAGE_CONTENT = "content";
        public static final String MESSAGE_DESCRIPTION = "description";
        public static final String MESSAGE_SENDOUT = "sendout";

        public static final String TEMP_CLUMN_UNSEECOUNT = "unseecount";

        public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY +"/"+DBHelperBase.TABLE_MESSAGE);
//        public static final Uri CONTENT_URI_GROUPBY = Uri.parse("content://" + AUTHORITY +"/"+DBHelperBase.TABLE_MESSAGE_GROUPBY);
        public static final String DEFAULT_ORDER= MESSAGE_TIME+ " ASC";
//        public static final String DEFAULT_GROUPBY= MESSAGE_REMOTE;
        public static final String DEFAULT_HAVING= null;
        public static String GROUPBY= null;
        public static String HAVING= DEFAULT_HAVING;
    }

    public static final class CallRuleColumns implements BaseColumns {


        public static final String CALL_RULE_NAME= "rulename";
        public static final String CALL_RULE_MATCHER= "rulematcher";
        public static final String CALL_RULE_ADDPREFIX= "addprefix";
        public static final String CALL_RULE_REMOVEPREFIX= "delprefix";
        public static final String CALL_RULE_ACCOUNT_ID= "accountid";
        public static final String CALL_RULE_ENABLE= "ruleenable";
        public static final String CALL_RULE_PRIORITY= "rulepriority";

        public static final String DEFAULT_ORDER= CALL_RULE_PRIORITY+ " DESC";
        public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY +"/"+DBHelperBase.TABLE_CALLRULE );
    }

    public static final class SubscribeColumns implements BaseColumns {

        public static final int ACTION_NONE = 0;
        public static final int ACTION_ACCEPTED = 1;
        public static final int ACTION_REJECTED = 2;

        public static final int UN_SEEN= 0;
        public static final int SEEN = 1;

        public static final String SUBSCRIB_NAME= "subdisname";//
        public static final String SUBSCRIB_DESC= "subdesc";//
        public static final String SUBSCRIB_REMOTE= "subremote";//
        public static final String SUBSCRIB_LOCAL= "sublocal";//
        public static final String SUBSCRIB_ACCTION= "subaction";//
        public static final String SUBSCRIB_TIME= "subtime";
        public static final String SUBSCRIB_SUBID= "subid";
        public static final String SUBSCRIB_SEEN= "subseen";//

        public static final String DEFAULT_ORDER= SUBSCRIB_ACCTION+ " ASC";
        public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY +"/"+DBHelperBase.TABLE_SUBSCRIB );
    }



    public static final class ChatSessionColumns implements BaseColumns {
        public static final String SESSION_LOCAL= "account_id";
        public static final String SESSION_LOCAL_URI= "local_uri";

        public static final String SESSION_REMOTE_ID= "remote_id";

        public static final String SESSION_STATUS= "status";
        public static final String SESSION_LASTTIME = "last_time_connect";
        public static final String SESSION_UNREAD = "unread_count";
        public static final String SESSION_DELETE = "removed";
        public static final String DEFAULT_ORDER= SESSION_LASTTIME+ " DESC";
        public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY +"/"+DBHelperBase.TABLE_CHATSESSION );
    }



    public static final class RemoteColumns implements BaseColumns {
        public static final String REMOTE_CONTACT_ID= "rcontact_id";//remote uri attached contact id
        public static final String REMOTE_CONTACT_TYPE= "rcontact_type";
        public static final String REMOTE_URI= "remote_uri";
        public static final String REMOTE_DISPPLAY_NAME= "remote_display_name";

        public static final String DEFAULT_ORDER= REMOTE_CONTACT_ID+ " ASC";
        public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY +"/"+DBHelperBase.TABLE_REMOTE );
    }

    public static final class ContactVersionColumns implements BaseColumns {
        public static final String VERSION_ID= _ID;//remote uri attached contact id
        public static final String VERSION_CONTACT_TYPE= "contact_type";
        public static final String VERSION_RAW_CONTACT_ID= "rawcontact_id";
        public static final String VERSION_CONTACT_ID= "contact_id";
        public static final String VERSION_CONTACT_DATAVERSION= "contact_version";
        public static final String VERSION_CONTACT_LASTCONTACT= "";

        public static final String DEFAULT_ORDER= VERSION_CONTACT_ID+ " ASC";
        public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY +"/"+DBHelperBase.TABLE_CONTACT_VERSION );
    }

    public static final class ViewChatSessionColumns implements BaseColumns {
        public static final String DEFAULT_ORDER= ChatSessionColumns.SESSION_LASTTIME+ " DESC";
        public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY +"/"+DBHelperBase.VIEW_CHATSESSION );
    }

    public static final class ViewHistoryColumns implements BaseColumns {
        public static final String DEFAULT_ORDER= HistoryColumns.HISTORY_STARTTIME+ " DESC";
        public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY +"/"+DBHelperBase.VIEW_HISTORY );
    }
}
