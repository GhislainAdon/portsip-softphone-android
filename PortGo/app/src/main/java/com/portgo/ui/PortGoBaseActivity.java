package com.portgo.ui;

import android.app.ActivityManager;
import android.app.Fragment;
import android.app.Instrumentation;
import android.app.LoaderManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.os.Handler;
import android.provider.Settings;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.widget.Toast;

import com.portgo.BuildConfig;
import com.portgo.PortApplication;
import com.portgo.R;
import com.portgo.exception.OutOfMaxLineException;
import com.portgo.manager.AccountManager;
import com.portgo.manager.CallManager;
import com.portgo.manager.ConfigurationManager;
import com.portgo.manager.ContactManager;
import com.portgo.manager.NetworkManager;
import com.portgo.manager.PermissionManager;
import com.portgo.manager.PortSipCall;
import com.portgo.manager.PortSipEngine;
import com.portgo.manager.PortSipSdkWrapper;
import com.portgo.manager.PortSipService;
import com.portgo.manager.SipManager;
import com.portgo.manager.UserAccount;
import com.portgo.util.NgnStringUtils;
import com.portgo.util.NgnUriUtils;
import com.portgo.view.MinimunWindowUtil;
import com.portsip.PortSipSdk;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PortGoBaseActivity extends AppCompatActivity {
    public PortApplication mApp;
    public SipManager  mSipMgr;
    public CallManager mCallMgr;

    public ContactManager mContactMgr;
    public NetworkManager mNetworkMgr;
    public ConfigurationManager mConfigurationService;

    private static Map<Integer,Fragment> fragments;
    LoaderManager loadMgr;
    public static synchronized Map<Integer,Fragment> getFragments() {
        if (fragments == null) {
            fragments = new HashMap<Integer,Fragment>();
        }
        return fragments;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        loadMgr = getLoaderManager();
        if(!PortSipSdkWrapper.getInstance().isInitialized()){
            startActivity(new Intent(this,PortActivityLogin.class));
            this.finish();
        }

        mContactMgr = ContactManager.getInstance();
        if(!mContactMgr.isReady()){
            mContactMgr.start(this);
        }
        mNetworkMgr = NetworkManager.getNetWorkmanager();
        mCallMgr = CallManager.getInstance();
        mConfigurationService = ConfigurationManager.getInstance();

        mApp =(PortApplication)getApplicationContext();

        mSipMgr = SipManager.getSipManager();
        mApp.addActivity(this);

        alterHandler= new Handler();

    }

    AlterViewReceiver alterViewReceiver;
    class AlterViewReceiver extends BroadcastReceiver{

        @Override
        public void onReceive(Context context, Intent intent) {
            if(BuildConfig.PORT_ACTION_ALTERVIEW.equals(intent.getAction())){
                if(mCallMgr.getCallsSize()>0&&mCallMgr.getDefaultCall()!=null) {//
                    if(minimize) {
                        if(!Settings.canDrawOverlays(PortGoBaseActivity.this)){
                            if(!mConfigurationService.getBooleanValue(PortGoBaseActivity.this,ConfigurationManager.PRESENCE_ALTER_SET,false)){
                                PermissionManager.portgoRequestSpecialPermission(PortGoBaseActivity.this);
                            }
                        }else {
                            MinimunWindowUtil.showPopupWindow(PortGoBaseActivity.this);
                        }
                        return;
                    }
                }
            }
        }
    }
    @Override
    protected void onDestroy() {
        super.onDestroy();
        alterViewReceiver = null;
        mApp.removeActivity(this);
        mContactMgr = null;
        mSipMgr= null;
        mCallMgr=null;

        if(alterHandler!=null&&alterRunable!=null) {
            alterHandler.removeCallbacks(alterRunable);
        }
        alterHandler=null;
        alterRunable = null;
        mConfigurationService =null;
        mApp= null;
    }


    @Override
    protected void onStart() {
        super.onStart();
    }

    boolean checkCallCondition(String number){
        if (NgnStringUtils.isNullOrEmpty(number)) {
            Toast.makeText(this,
                    R.string.input_number_tips,
                    Toast.LENGTH_SHORT).show();
            return false;
        }
        AccountManager accountMgr = AccountManager.getInstance();
        switch (accountMgr.getLoginState()) {
            case UserAccount.STATE_LOGIN:
                Toast.makeText(this,
                        R.string.inlongin_tips,
                        Toast.LENGTH_SHORT).show();
                return false;
            case UserAccount.STATE_NOTONLINE:
                Toast.makeText(this, R.string.please_login_tips,
                        Toast.LENGTH_SHORT).show();
                return false;
        }
        return true;
    }

    @Override
    protected void onStop() {
        super.onStop();
    }
    Handler alterHandler;
    Runnable alterRunable;
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == PermissionManager.REQUEST_OVERLAY_PERMISSION){
            mConfigurationService.setBooleanValue(this,ConfigurationManager.PRESENCE_ALTER_SET,true);
            alterRunable = new Runnable() {
                @Override
                public void run() {
                    sendBroadcast(new Intent(BuildConfig.PORT_ACTION_ALTERVIEW));
                }
            };
            alterHandler.postDelayed(alterRunable,15000);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        alterViewReceiver = new AlterViewReceiver();
        registerReceiver(alterViewReceiver,new IntentFilter(BuildConfig.PORT_ACTION_ALTERVIEW));
        PortSipCall sipCall = mCallMgr.getPendingCall();
        if(minimize&&mCallMgr.getCallsSize()>0&&sipCall!=null&&(sipCall.getStatus()== PortSipCall.InviteState.INCOMING))
        {
            mApp.startAVActivity(sipCall.getCallId());
            MinimunWindowUtil.hidePopupWindow();
            return;
        }
        sipCall = mCallMgr.getDefaultCall();
        if(mCallMgr.getCallsSize()>0&&sipCall!=null&&(sipCall.getStatus()== PortSipCall.InviteState.INCALL)) {//
            if(minimize) {
                if(!Settings.canDrawOverlays(this)){
                    if(!mConfigurationService.getBooleanValue(this,ConfigurationManager.PRESENCE_ALTER_SET,false)){
                        PermissionManager.portgoRequestSpecialPermission(this);
                    }
                }else {
                    MinimunWindowUtil.showPopupWindow(this);
                }
                return;
            }else {
                MinimunWindowUtil.hidePopupWindow();
            }
        }else {
            MinimunWindowUtil.hidePopupWindow();
        }
    }

    public void makeCall(int remoteid,String number,int callType){
        AccountManager accountMgr = AccountManager.getInstance();
        PortSipCall call = null;
        if(TextUtils.isEmpty(number)) {
            Toast.makeText(this, R.string.empty_numaber_tips, Toast.LENGTH_SHORT).show();
            return;
        }
        try {
            String disName = NgnUriUtils.getDisplayName(number, this);
            UserAccount account = accountMgr.getDefaultUser(this);
            String remote = NgnUriUtils.getFormatUrif4Msg(number,account.getDomain());
            call = mCallMgr.portSipCallOut(this,mSipMgr,account.getId(),account.getFullAccountReamName(),disName,remoteid,remote,
                    callType);
            mApp.startAVActivity(call.getCallId());
        } catch (OutOfMaxLineException e) {
            Toast.makeText(this,R.string.toomanycalls,Toast.LENGTH_SHORT);
        }

    }
    @Override
    protected void onPause() {
        super.onPause();

        unregisterReceiver(alterViewReceiver);
        PortSipCall sipCall = mCallMgr.getDefaultCall();
        if(mCallMgr.getCallsSize()>0&&sipCall!=null&&sipCall.getStatus()== PortSipCall.InviteState.INCALL){//
            if(minimize) {
                if(!Settings.canDrawOverlays(this)){
                    if(!mConfigurationService.getBooleanValue(this,ConfigurationManager.PRESENCE_ALTER_SET,false)){
                        PermissionManager.portgoRequestSpecialPermission(this);
                    }
                }else {
                    MinimunWindowUtil.showPopupWindow(this);
                }
                return;
            }else {
                MinimunWindowUtil.hidePopupWindow();
            }
        }else {
            MinimunWindowUtil.hidePopupWindow();
        }
    }

    boolean minimize = true;
    protected void setNeedMinimunWindow(boolean minimunWindow)
    {
        minimize = minimunWindow;
    }

    public static boolean isServiceRunning(Context mContext, String className) {

        boolean isRunning = false;
        ActivityManager activityManager = (ActivityManager) mContext
                .getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningServiceInfo> serviceList = activityManager
                .getRunningServices(30);

        if (!(serviceList.size() > 0)) {
            return false;
        }

        for (int i = 0; i < serviceList.size(); i++) {
            if (serviceList.get(i).service.getClassName().equals(className) == true) {
                isRunning = true;
                break;
            }
        }
        return isRunning;
    }


    protected void showToolsbarAsHomeUp(String title) {
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolBar);
        if (toolbar != null) {
            toolbar.setBackgroundColor(getResources().getColor(R.color.portgo_color_toobar_gray));
            toolbar.setTitle(title);
            toolbar.setTitleTextAppearance(this, R.style.ToolBarTextAppearance);
            toolbar.setNavigationIcon(R.drawable.nav_back_ico);
            toolbar.setTitleMarginStart(0);
            setSupportActionBar(toolbar);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
    }

    protected  void goback(){
        new Thread () {
            public void run () {
                try {
                    Instrumentation inst= new Instrumentation();
                    inst.sendKeyDownUpSync(KeyEvent. KEYCODE_BACK);
                } catch(Exception e) {
                    e.printStackTrace();
                }
            }
        }.start();
    }
}
