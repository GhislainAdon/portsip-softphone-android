package com.portgo.manager;

import android.Manifest;
import android.app.Activity;
import android.app.AppOpsManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Build;
import android.os.PowerManager;
import android.provider.Settings;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;

import com.portgo.BuildConfig;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static android.content.Context.WINDOW_SERVICE;


/**
 * Created by huacai on 2017/11/3.
 */

public class PermissionManager {
    //权限分特殊权限：悬浮窗，系统设置更改。 //必须进设置界面授权
    //危险权限： 联系人，摄像头，麦克风，心跳传感器，存储卡读写，拨号，日历，定位，短信 //弹出框授权
    //普通权限 普通传感器，网路，nfc，蓝牙，等其他 //安装时授权

    static  private boolean addFlowLayoutOnWindow(Context context) {//在23上面出现一只返回
        //获取WindowManager对象
        WindowManager windowManager = ((WindowManager) context.getSystemService(WINDOW_SERVICE));
        //创建WindowManager的布局参数对象
        WindowManager.LayoutParams wmParams = new WindowManager.LayoutParams();
        //设置添加View的类型

        if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.O) {
            wmParams.type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        }else{
            wmParams.type = WindowManager.LayoutParams.TYPE_SYSTEM_ALERT;
        }
        //设置添加View的标识
        wmParams.flags = WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS |
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;
        wmParams.gravity = Gravity.LEFT | Gravity.TOP;
        //设置添加View的默认坐标值
        wmParams.x = 50;
        wmParams.y = 50;
        //设置添加View的宽高
        wmParams.width = WindowManager.LayoutParams.WRAP_CONTENT;
        wmParams.height = WindowManager.LayoutParams.WRAP_CONTENT;
        wmParams.format = 1;
        TextView view = new TextView(context);
        try {
            windowManager.addView(view, wmParams);
            windowManager.removeViewImmediate(view);
        }
        catch (RuntimeException e){
            return false;
        }
        return true;
    }

    public static boolean testSpecialPermission(Context activityContext, String permissionAction) {
        if (Settings.ACTION_MANAGE_OVERLAY_PERMISSION.equals(permissionAction)) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {// Build.VERSION_CODES.O_MR1 ==27
                    return Settings.canDrawOverlays(activityContext);
                } else {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        if (Settings.canDrawOverlays(activityContext)) {
                            return true;
                        } else {
                            //在23上面会出现canDrawOverlays返回一直为false的情况。所以使用强制添加一个窗口，通过是否异常来判断权限
                           return addFlowLayoutOnWindow(activityContext);
//                            AppOpsManager appOpsMgr = (AppOpsManager) activityContext.getSystemService(Context.APP_OPS_SERVICE);
//                            int mode = appOpsMgr.checkOpNoThrow("android:system_alert_window",
//                                    android.os.Process.myUid(), activityContext.getPackageName());
//                            switch (mode){
//                                case AppOpsManager.MODE_ERRORED://如果用户禁止
//                                case AppOpsManager.MODE_DEFAULT:
//                                    map.put(activityContext.getPackageName(),false);
//                                    return false;
//                                case AppOpsManager.MODE_IGNORED://如果，用户直接返回，不更改开关值 ，返回设置前保存的值
//                                    Boolean result = map.get(activityContext.getPackageName());
//                                    return result!=null?result:false;
//                                case AppOpsManager.MODE_ALLOWED://如果授权
//                                    map.put(activityContext.getPackageName(),true);
//                                    return true;
//                            }
//                            return false;
                        }
                    }
//                return Settings.canDrawOverlays(activityContext);
                }
            }
            return true;
//        } else if (Settings.ACTION_MANAGE_WRITE_SETTINGS.equals(permissionAction)) {
//            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
//                return Settings.System.canWrite(activityContext);
//            }
//            return true;

        }
        return true;
    }

    static  HashMap<String,Boolean> map = new HashMap();
    static boolean canDraw = false;
    //
//    int checkSelfPermission(String permission)
//    void requestPermissions(String[] permissions, int requestCode) 进行用来检测应用是否已经具有权限请求单个或多个权限
//    void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) 用户对请求作出响应后的回调

    /**
     * @param context
     * @param permissionAction
     * @return true GRANTED false DENIED
     */
    static public boolean testDangercePermission(Context context, String permissionAction) {
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
//            return PackageManager.PERMISSION_GRANTED == activityContext.checkSelfPermission(permissionAction);
//        }else {
            return PackageManager.PERMISSION_GRANTED == ContextCompat.checkSelfPermission(context, permissionAction);
//        }
    }

    /**
     * @param activityContext
     * @return true all GRANTED, false some DENIED
     */

    static final String[] phonePermissions = new String[]{
            Manifest.permission.CALL_PHONE,
            Manifest.permission.PROCESS_OUTGOING_CALLS,
    };
    static public boolean testCallPhonePermissions(Context activityContext) {
        return testDangercePermissions(activityContext,phonePermissions);
    }
    static public void requestCallPhonePermissions(Activity activityContext) {
        ActivityCompat.requestPermissions(activityContext,
                phonePermissions, REQUEST_PORTGO_ALLDANGERS);
    }
    /**
     * @param activityContext
     * @param permissions
     * @return true all GRANTED, false some DENIED
     */
    static public boolean testDangercePermissions(Context activityContext, String... permissions) {
        for (String permission : permissions) {
            if (!testDangercePermission(activityContext, permission))
                return false;
        }
        return true;
    }

    static public boolean testPowerSavePermissions(Context context){
        String packageName = context.getPackageName();
        PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M&&!ConfigurationManager.getInstance().getBooleanValue(context,"IgnoringBattery",false)
                &&!pm.isIgnoringBatteryOptimizations(packageName)){
            return false;
        }
        return true;
    }

    static public boolean startPowerSavePermissions(Activity activityContext){
        String packageName = activityContext.getPackageName();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
            Intent intent = new Intent();
            intent.setAction(android.provider.Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
//                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.setData(Uri.parse("package:" + packageName));
            if (PermissionManager.isIntentAvailable(activityContext, intent)) {
                activityContext.startActivity(intent);
            }
            ConfigurationManager.getInstance().setBooleanValue(activityContext,"IgnoringBattery",true);
        }
        return true;
    }

    static public  boolean pemissionNotPrompt(Activity activityContext, String... permissions){
        for (String permission : permissions) {
            if (!ActivityCompat.shouldShowRequestPermissionRationale(activityContext,permission))
                return true;
        }
        return false;
    }
    static public void requestAlterWindowSettings(Activity activityContext, int requestCode) {
        //canDraw = testSpecialPermission(activityContext,Settings.ACTION_MANAGE_OVERLAY_PERMISSION);//将进入设置界面,设置前的状态保存。
        Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION);
        //map.put(activityContext.getPackageName(),canDraw);
        intent.setData(Uri.parse("package:" + activityContext.getPackageName()));
        activityContext.startActivityForResult(intent, requestCode);
    }

    static public void requestWriteSettings(Activity activityContext, int requestCode) {
        Intent intent = new Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS);
        intent.setData(Uri.parse("package:" + activityContext.getPackageName()));
        activityContext.startActivityForResult(intent, requestCode);
    }

    //special
    public static final int REQUEST_OVERLAY_PERMISSION = 88;
    public static final int REQUEST_WRITE_SETTINGS = 89;
    //danger
    public static final int REQUEST_WRITE_CONTACTS = 100;//按组授权的，读写联系人，GET_ACCOUNTS,随便申请一个，联系人授权就有了
    public static final int REQUEST_EXTERNAL_STORAGE = 101;
    public static final int REQUEST_MICPHONE = 102;
    public static final int REQUEST_CAMERA = 103;
//xx     public static final int REQUEST_CALL_PHONE = 103;

    public static final int REQUEST_PORTGO_ALLDANGERS = 200;

    static public void portgoRequestSpecialPermission(Activity activityContext) {
        if (!testSpecialPermission(activityContext, Settings.ACTION_MANAGE_OVERLAY_PERMISSION)) {
            requestAlterWindowSettings(activityContext, REQUEST_OVERLAY_PERMISSION);
        }
    }

    public static boolean isIntentAvailable(Context context, Intent intent) {
        PackageManager packageManager = context.getPackageManager();
        List<ResolveInfo> resolveInfo = packageManager.queryIntentActivities(
                intent, PackageManager.MATCH_DEFAULT_ONLY);
        return resolveInfo.size() > 0;
    }

    static public void portgoRequestDangersPermissionVersionM(Activity activityContext, Map<Integer, String> explains) {

    }

    static public String[] PORTGO_MUST_PERMISSION = new String[]{
            Manifest.permission.WRITE_CONTACTS,
            Manifest.permission.READ_CONTACTS,
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
//xx             Manifest.permission.CALL_PHONE,
    };

    static public void portgoRequestDangersPermission(Activity activityContext, Map<Integer, String> explains) {
        ArrayList<String> requestMessions = new ArrayList<>();
        //读写联系人
        if (!testDangercePermission(activityContext, Manifest.permission.WRITE_CONTACTS)) {

            if (ActivityCompat.shouldShowRequestPermissionRationale(activityContext,
                    Manifest.permission.WRITE_CONTACTS)) {//上一次是否拒绝，如果上一次拒绝。这里可以弹toast解释
                if (explains != null) {
                    String tip = explains.get(REQUEST_WRITE_CONTACTS);
                    if (!TextUtils.isEmpty(tip)) {
                        Toast.makeText(activityContext, tip, Toast.LENGTH_LONG).show();
                    }
                }
            }
            requestMessions.add(Manifest.permission.WRITE_CONTACTS);
        }

        //拍照
        if (!testDangercePermission(activityContext, Manifest.permission.CAMERA)) {

            if (ActivityCompat.shouldShowRequestPermissionRationale(activityContext,
                    Manifest.permission.CAMERA)) {
                if (explains != null) {
                    String tip = explains.get(REQUEST_CAMERA);
                    if (!TextUtils.isEmpty(tip)) {
                        Toast.makeText(activityContext, tip, Toast.LENGTH_LONG).show();
                    }
                }
            }
            requestMessions.add(Manifest.permission.CAMERA);
        }

        //录音
        if (!testDangercePermission(activityContext, Manifest.permission.RECORD_AUDIO)) {

            if (ActivityCompat.shouldShowRequestPermissionRationale(activityContext,
                    Manifest.permission.RECORD_AUDIO)) {
                if (explains != null) {
                    String tip = explains.get(REQUEST_MICPHONE);
                    if (!TextUtils.isEmpty(tip)) {
                        Toast.makeText(activityContext, tip, Toast.LENGTH_LONG).show();
                    }
                }
            }
            requestMessions.add(Manifest.permission.RECORD_AUDIO);
        }

        //存储
        if (!testDangercePermission(activityContext, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {

            if (ActivityCompat.shouldShowRequestPermissionRationale(activityContext,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                if (explains != null) {
                    String tip = explains.get(REQUEST_EXTERNAL_STORAGE);
                    if (!TextUtils.isEmpty(tip)) {
                        Toast.makeText(activityContext, tip, Toast.LENGTH_LONG).show();
                    }
                }
            }
            requestMessions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
        }

        //存储
        /*xx if (!testDangercePermission(activityContext, Manifest.permission.CALL_PHONE)) {

            if (ActivityCompat.shouldShowRequestPermissionRationale(activityContext,
                    Manifest.permission.CALL_PHONE)) {
                if (explains != null) {
                    String tip = explains.get(REQUEST_CALL_PHONE);
                    if (!TextUtils.isEmpty(tip)) {
                        Toast.makeText(activityContext, tip, Toast.LENGTH_LONG).show();
                    }
                }
            }
            requestMessions.add(Manifest.permission.CALL_PHONE);
        }*/
        if (requestMessions.size() > 0) {
            String[] needs = requestMessions.toArray(new String[requestMessions.size()]);
            ActivityCompat.requestPermissions(activityContext,
                    needs, REQUEST_PORTGO_ALLDANGERS);
        }

    }

    public  static void startAndroidPermissionSetting(Context context) {
        Intent intent = new Intent(Settings.ACTION_SETTINGS);
        context.startActivity (intent);
    }

    public static void start360PermissionSetting(Context context) {
        Intent intent = new Intent("android.intent.action.MAIN");
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.putExtra("packageName", BuildConfig.APPLICATION_ID);
        ComponentName comp = new ComponentName("com.qihoo360.mobilesafe", "com.qihoo360.mobilesafe.ui.index.AppEnterActivity");
        intent.setComponent(comp);
        context.startActivity(intent);
    }

    public static void startLeshiPermissionSetting(Context context) {
        Intent intent = new Intent();
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.putExtra("packageName", BuildConfig.APPLICATION_ID);
        ComponentName comp = new ComponentName("com.letv.android.letvsafe", "com.letv.android.letvsafe.PermissionAndApps");
        intent.setComponent(comp);
        context.startActivity(intent);
    }

    public static void startLGPermissionSetting(Context context) {
        Intent intent = new Intent("android.intent.action.MAIN");
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.putExtra("packageName", BuildConfig.APPLICATION_ID);
        ComponentName comp = new ComponentName("com.android.settings", "com.android.settings.Settings$AccessLockSummaryActivity");
        intent.setComponent(comp);
        context.startActivity(intent);
    }

    public static void startOppoPermissionSetting(Context context) {
        Intent intent = new Intent();
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.putExtra("packageName", BuildConfig.APPLICATION_ID);
        ComponentName comp = new ComponentName("com.color.safecenter", "com.color.safecenter.permission.PermissionManagerActivity");
        intent.setComponent(comp);
        context.startActivity(intent);
    }

    public static void startSonyPermissionSetting(Context context) {
        Intent intent = new Intent();
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.putExtra("packageName", BuildConfig.APPLICATION_ID);
        ComponentName comp = new ComponentName("com.sonymobile.cta", "com.sonymobile.cta.SomcCTAMainActivity");
        intent.setComponent(comp);
        context.startActivity(intent);
    }

    public static void startMEIZUPermissionSetting(Context context) {
        Intent intent = new Intent("com.meizu.safe.security.SHOW_APPSEC");
        intent.addCategory(Intent.CATEGORY_DEFAULT);
        intent.putExtra("packageName", BuildConfig.APPLICATION_ID);
        context.startActivity(intent);
    }

    public static void startHuaWeiPermissionSetting(Context context) {
        Intent intent = new Intent();
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.putExtra("packageName",BuildConfig.APPLICATION_ID);
        ComponentName comp = new ComponentName("com.huawei.systemmanager", "com.huawei.permissionmanager.ui.MainActivity");
        intent.setComponent(comp);

        context.startActivity(intent);
    }
    public static void startMiuiPermissionSetting(Context context){
        String rom = OSVersion.getRomName(OSVersion.KEY_MIUI_VERSION_NAME);
        Intent intent = null;
        final String ROM_MIUI_V5="V5";
        final String ROM_MIUI_V6="V6";
        final String ROM_MIUI_V7="V7";
        final String ROM_MIUI_V8="V8";

        if (ROM_MIUI_V5.equals(rom)) {

            Uri packageURI = Uri.parse("package:" + context.getApplicationInfo().packageName);
            intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, packageURI);


        } else if (ROM_MIUI_V6.equals(rom) || ROM_MIUI_V7.equals(rom)) {
            intent = new Intent("miui.intent.action.APP_PERM_EDITOR");
            intent.setClassName("com.miui.securitycenter", "com.miui.permcenter.permissions.AppPermissionsEditorActivity");
            intent.putExtra("extra_pkgname", context.getPackageName());
        } else if(ROM_MIUI_V8.equals(rom)){
            intent = new Intent("miui.intent.action.APP_PERM_EDITOR");
            intent.setClassName("com.miui.securitycenter", "com.miui.permcenter.permissions.PermissionsEditorActivity");
            intent.putExtra("extra_pkgname", context.getPackageName());
        }else{
        }
        context.startActivity(intent);
    }
}
