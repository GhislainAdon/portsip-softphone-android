package com.portgo.util;

import android.app.Service;
import android.content.ContentResolver;
import android.content.Context;
import android.graphics.Point;
import android.hardware.display.DisplayManager;
import android.util.TypedValue;
import android.view.WindowManager;

import com.portgo.R;

/**
 * Created by huacai on 2017/8/22.
 */

public class UnitConversion {

    static public int dp2px(Context context,float dpValue){
        float scale=context.getResources().getDisplayMetrics().density;
        return (int)(dpValue*scale+0.5f);
    }

    static public int px2dp(Context context,float pxValue){
        float scale=context.getResources().getDisplayMetrics().density;
        return (int)(pxValue/scale+0.5f);
    }

    static public int sp2px(Context context,float spValue){
        float fontScale=context.getResources().getDisplayMetrics().scaledDensity;
        return (int) (spValue*fontScale+0.5f);
    }

    static public int px2sp(Context context, float pxValue){
        float fontScale=context.getResources().getDisplayMetrics().scaledDensity;
        return (int) (pxValue/fontScale+0.5f);
    }

    static public int dp2px(Context context,int dpValue){
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,dpValue,context.getResources().getDisplayMetrics());
    }
    static public int sp2px(Context context,int spValue){
        return (int)TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP,spValue,context.getResources().getDisplayMetrics());
    }
    static int screenwidth = 0;
    static public int getScreenWidth(Context context){
        if(screenwidth>0){
            return screenwidth;
        }
        try {
            WindowManager winManager = (WindowManager)context.getSystemService(Context.WINDOW_SERVICE);
            Point point = new Point();
            winManager.getDefaultDisplay().getSize(point);
            screenwidth = point.x;
        }
        catch(Exception e) {
            e.printStackTrace();
        }
        return screenwidth;
    }

    static int screenHeight = 0;
    static public int getScreenHeight(Context context){
        if(screenHeight>0){
            return screenHeight;
        }
        try {
            WindowManager winManager = (WindowManager)context.getSystemService(Context.WINDOW_SERVICE);
            Point point = new Point();
            winManager.getDefaultDisplay().getSize(point);
            screenHeight = point.y;
        }
        catch(Exception e) {
            e.printStackTrace();
        }
        return screenHeight;
    }
    public static String getResourceUriString(Context context, int resource) {
        String resUri;
        try {
            resUri = ContentResolver.SCHEME_ANDROID_RESOURCE + "://"
                    + context.getResources().getResourcePackageName(resource) + "/"
                    + context.getResources().getResourceTypeName(resource) + "/"
                    + context.getResources().getResourceEntryName(resource);
        }catch (Exception e) {
            resource = R.drawable.app_icon;
            resUri = ContentResolver.SCHEME_ANDROID_RESOURCE + "://"
                    + context.getResources().getResourcePackageName(resource) + "/"
                    + context.getResources().getResourceTypeName(resource) + "/"
                    + context.getResources().getResourceEntryName(resource);
        }

        return resUri;
    }
}
