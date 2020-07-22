package com.portgo.database;

import android.content.Context;
import android.content.ContextWrapper;
import android.database.DatabaseErrorHandler;
import android.database.sqlite.SQLiteDatabase;
import android.os.Environment;

import java.io.File;

public class DBContext extends ContextWrapper {

    private static final String DEBUG_CONTEXT = "DatabaseContext";

    public DBContext(Context base) {
        super(base);
    }

    @Override
    public File getDatabasePath(String name)  {

        String dbfile=getFilesDir()+File.separator;
        dbfile =  dbfile+ "config" + File.separator + name;
        File result = new File(dbfile);

        if (!result.getParentFile().exists()) {
            result.getParentFile().mkdirs();
        }

        return result;
    }

    /* this version is called for android devices >= api-11. thank to @damccull for fixing this. */
    @Override
    public SQLiteDatabase openOrCreateDatabase(String name, int mode, SQLiteDatabase.CursorFactory factory, DatabaseErrorHandler errorHandler) {
        return openOrCreateDatabase(name,mode, factory);
    }

    /* this version is called for android devices < api-11 */
    @Override
    public SQLiteDatabase openOrCreateDatabase(String name, int mode, SQLiteDatabase.CursorFactory factory) {
        SQLiteDatabase result = SQLiteDatabase.openOrCreateDatabase(getDatabasePath(name), null);
        return result;
    }
}