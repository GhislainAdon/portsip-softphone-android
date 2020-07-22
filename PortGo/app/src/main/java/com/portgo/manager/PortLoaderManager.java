package com.portgo.manager;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.app.LoaderManager.LoaderCallbacks;
import android.app.LoaderManager;
import android.os.Looper;

public class PortLoaderManager {
    public static<D> void initLoader(Activity activityContext,final LoaderManager loadMgr,final int id,final Bundle args,final LoaderCallbacks<D> callback){
        if(activityContext==null||loadMgr==null||callback==null){
            return;
        }

        if(!checkIsOnMainThread()) {
            activityContext.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    loadMgr.initLoader(id, args, callback);
                }
            });

        }else{
            loadMgr.initLoader(id, args, callback);
        }
    }

    public static<D> void restartLoader(Activity activityContext,final LoaderManager loadMgr,final int id,final Bundle args,final LoaderCallbacks<D> callback){
        if(activityContext==null||loadMgr==null||callback==null){
            return;
        }
        if(!checkIsOnMainThread()) {
            activityContext.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    loadMgr.restartLoader(id, args, callback);
                }
            });

        }else{
            loadMgr.restartLoader(id, args, callback);
        }
    }

    public static boolean checkIsOnMainThread() {
        if (Thread.currentThread() != Looper.getMainLooper().getThread()) {
            return false;
        }
        return true;

    }
}
