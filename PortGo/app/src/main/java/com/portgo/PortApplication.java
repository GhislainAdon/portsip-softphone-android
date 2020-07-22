package com.portgo;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.Application;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.text.TextUtils;

import com.portgo.manager.NotificationUtils;
import com.portgo.manager.PortSipEngine;
import com.portgo.ui.PortIncallActivity;

import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

public class PortApplication extends Application implements IApplicationHandle{
    PendingIntent mInComingpi;
    private List<Activity> activityList=new LinkedList<Activity>();

    @Override  
    public void onCreate(){    
	    super.onCreate();

        Intent startActivity = new Intent();
        startActivity.setClass(this,PortIncallActivity.class);
        PendingIntent pi = PendingIntent.getActivity(this,0,startActivity,0);
        setIncomingPending(pi);
    }

    public void setIncomingPending(PendingIntent pi){
		if(mInComingpi==null)
			mInComingpi = pi;
	}
	
	public PendingIntent getIncomingPending(){
		return mInComingpi;
	}

    @Override
    public	void addActivity(Activity act){
        activityList.add(act);
    }

    @Override
    public	void removeActivity(Activity act){
        activityList.remove(act);
    }

    @Override
    public	void closeActivitys(){
        for(Activity activity:activityList)
        {
            activity.finish();
        }
    }

    public boolean isForeground(){
        String[] activitys;
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.KITKAT_WATCH) {
            activitys = getActivePackages(this);
        }else{
            activitys = getActivePackagesCompat(this);
        }
        if(activitys.length>0){
            String packagename = getPackageName();
            for(String activityname:activitys){

                if(activityname.contains(packagename)){
                    return true;
                }
            }
            return false;
        }
        return false;
    }
    @Override
    public void startAVActivity(int callID) {
            Intent it = new Intent();
            it.putExtra("CALLID",callID);
            try {
                getIncomingPending().send(getApplicationContext(), 0, it);
            } catch (PendingIntent.CanceledException e) {
                e.printStackTrace();
            }
    }

    @Override
    public PortSipEngine getPortSipEngine(){
        return PortSipEngine.getInstance(this);
    }

    @Override
    public void onTerminate() {
        super.onTerminate();
        NotificationUtils.getInstance(this).cancelAllNotification(this);
    }
    public static boolean isForeground(Activity activity) {
        return isForeground(activity, activity.getClass().getName());
    }

    public static boolean isForeground(Context context, String className) {
        if (context == null || TextUtils.isEmpty(className))
            return false;
        ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningTaskInfo> list = am.getRunningTasks(1);
        if (list != null && list.size() > 0) {
            ComponentName cpn = list.get(0).topActivity;
            if (className.equals(cpn.getClassName()))
                return true;
        }
        return false;
    }

    String[] getActivePackagesCompat(Context context) {
        ActivityManager mActivityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        final List<ActivityManager.RunningTaskInfo> taskInfo = mActivityManager.getRunningTasks(1);
        final ComponentName componentName = taskInfo.get(0).topActivity;
        final String[] activePackages = new String[1];
        activePackages[0] = componentName.getPackageName();
        return activePackages;
    }

    String[] getActivePackages(Context context) {
        final Set<String> activePackages = new HashSet<String>();
        ActivityManager mActivityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        final List<ActivityManager.RunningAppProcessInfo> processInfos = mActivityManager.getRunningAppProcesses();
        for (ActivityManager.RunningAppProcessInfo processInfo : processInfos) {
            if (processInfo.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND) {
                activePackages.addAll(Arrays.asList(processInfo.pkgList));
            }
        }
        return activePackages.toArray(new String[activePackages.size()]);
    }
}
