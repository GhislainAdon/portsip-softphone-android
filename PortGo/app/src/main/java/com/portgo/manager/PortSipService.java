package com.portgo.manager;

import android.Manifest;
import android.app.AlertDialog;
import android.app.Service;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.PowerManager;

import android.provider.ContactsContract;
import android.provider.ContactsContract.StatusUpdates;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import android.text.TextUtils;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.WindowManager;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.InstanceIdResult;
import com.portgo.BuildConfig;
import com.portgo.PortApplication;
import com.portgo.R;

import com.portgo.database.DBHelperBase;
import com.portgo.database.DataBaseManager;
import com.portgo.database.RemoteRecord;
import com.portgo.exception.LineBusyException;
import com.portgo.exception.OutOfMaxLineException;
import com.portgo.ui.PortActivityAlterDialog;
import com.portgo.ui.PortActivityLogin;
import com.portgo.ui.PortActivityMain;
import com.portgo.ui.PortActivityReferDialog;

import com.portgo.ui.PortIncallActivity;
import com.portgo.util.CallReferTask;
import com.portgo.util.ContactQueryTask;

import com.portgo.util.NgnMediaType;
import com.portgo.util.NgnObservableList;
import com.portgo.util.NgnStringUtils;
import com.portgo.util.NgnUriUtils;
import com.portgo.util.OkHttpHelper;

import com.portgo.util.Ring;
import com.portgo.view.emotion.data.EmotionDataManager;
import com.portsip.OnAudioManagerEvents;
import com.portsip.OnPortSIPEvent;
import com.portsip.PortSipEnumDefine;
import com.portsip.PortSipErrorcode;
import com.portsip.PortSipSdk;

import org.webrtc.apprtc.AppRTCAudioManager;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Observable;
import java.util.Observer;
import java.util.Random;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


import static com.portgo.BuildConfig.ENABLEVIDEO;
import static com.portgo.BuildConfig.HASPRESENCE;
import static com.portgo.BuildConfig.HASVIDEO;
import static com.portgo.BuildConfig.PORT_ACTION_REJECT;
import static com.portgo.manager.Contact.INVALIDE_ID;
import static com.portgo.manager.ContactManager.PORTSIP_IM_PROTOCAL;

public class PortSipService extends Service implements OnPortSIPEvent,Observer, OnAudioManagerEvents,
        MediaPlayer.OnCompletionListener, MediaPlayer.OnErrorListener,MediaPlayer.OnPreparedListener {
    InComingMessageProcessor messageProcessor;
    PortSipSdk sipSdk = null;
    String appName =null;
    private PowerManager.WakeLock mCpuLock;

    final int REGIST_TIMER_PERIOD = 20*1000;//
    final int REGIST_TIMER_ATONCE = 300;
    final int REGIST_MAX_RETRY = 15;
    int retryTime = 0;
    boolean enableRing ,enableVibrate;

    public PortSipService(){
        super();
    }
    PortsipBinder portsipBinder = new PortsipBinder();
    @Override
    public IBinder onBind(Intent intent) {
        return portsipBinder;
    }

    @Override
    public void onCompletion(MediaPlayer mp) {
    }

    @Override
    public boolean onError(MediaPlayer mp, int what, int extra) {
        return false;
    }

    @Override
    public void onPrepared(MediaPlayer mp) {
        mp.start();
    }

    public class PortsipBinder extends Binder {
        public void uploadFile(long messageid,String mimetype,String fileName) {
            uploadStreamFile(messageid,mimetype,fileName);
        }

        public void downloadFile(long messageid,String filePath,String fileName,String url) {
            downloadStreamFile(messageid,filePath,fileName,url);
        }

        public void setDataSource(String file){
            if(TextUtils.isEmpty(file))
            {
                return;
            }
            File source = new File(file);
            if(source.exists())
            {
                setDataSource(source);
            }
        }
        public void setDataSource(File file){
            if(file.exists()&&mediaPlayer!=null){
                stopPlay();
                preparePlay(file);
            }
        }

        private void preparePlay(@NonNull File file){
            try {
                FileInputStream fis = new FileInputStream(file);
                mediaPlayer.setDataSource(fis.getFD());
//                mediaPlayer.setDataSource(file);
                mediaPlayer.setLooping(false);
                mediaPlayer.prepare();
                mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
//                mediaPlayer.seekTo(0);
            } catch (IOException e) {
                mediaPlayer.reset();
                e.printStackTrace();
            }
        }

        public void playPause(){
            if(mediaPlayer!=null){
                if(mediaPlayer.isPlaying()){
                    mediaPlayer.pause();
                }else {
                    mediaPlayer.start();
                }
            }
        }

        public void stopPlay(){
            if(mediaPlayer!=null){
                if(mediaPlayer.isPlaying()){
                    mediaPlayer.stop();
                }
                mediaPlayer.reset();
            }
        }
    }

    @Override
    public void onAudioDeviceChanged(PortSipEnumDefine.AudioDevice audioDevice, Set<PortSipEnumDefine.AudioDevice> set) {
        Intent intent = new Intent();
        intent.setAction(BuildConfig.PORT_ACTION_AUDIODEVICE);
        sendBroadcast(intent);
        CallManager.getInstance().setAudioDevices(set);
    }

    private  int initSDK(UserAccount defaultUser ){
        PortSipSdkWrapper sdk = PortSipSdkWrapper.getInstance();
        if(sdk.isInitialized()) {
            sdk.displayLocalVideo(false,false);
//            sdk.displayLocalVideo(false);
            sdk.setConferenceVideoWindow(null);
            sdk.setLocalVideoWindow(null);
            sdk.uninitialize();
        }

        CallManager.getInstance().terminateAllCalls(PortSipService.this);
        CallManager.getInstance().setRemoteViewID(-1,null);
        CallManager.getInstance().clear();
        AccountManager.getInstance().setLoginState(UserAccount.STATE_NOTONLINE,AccountManager.NETWORK_UNAVAILABLE, getString(R.string.network_not_availabe),false);

        ConfigurationManager configMgr = ConfigurationManager.getInstance();
        boolean enableDebug = configMgr.getBooleanValue(this,ConfigurationManager.PRESENCE_DEBUG,this.getResources().getBoolean(R.bool.debug_default));
        boolean tlsCert = configMgr.getBooleanValue(this,ConfigurationManager.PRESENCE_TLS_CERT,this.getResources().getBoolean(R.bool.tls_cert));

        int maxLine = CallManager.getMaxLine();

        int logMod = PortSipEnumDefine.ENUM_LOG_LEVEL_NONE;
        if(enableDebug){
            logMod = PortSipEnumDefine.ENUM_LOG_LEVEL_DEBUG;
        }
        String localIP = "0.0.0.0";

        String dirName = "/log";
        String logpath = this.getExternalFilesDir(null).getAbsolutePath()+dirName;
        File file=new File(logpath);
        if(!file.exists()) {
            file.mkdirs();
        }
        int result = PortSipErrorcode.ECoreErrorNone;
        try {
            result = sdk.initialize(this, defaultUser.getTransType(), localIP, AccountManager.mLocalPort,
            logMod, logpath,
            maxLine, "", 0,
            0, null, null, tlsCert, null, this);
        }catch (RuntimeException e){
            e.printStackTrace();
            result = PortSipErrorcode.ECoreSDKObjectNull;
        }
        if(result==PortSipErrorcode.ECoreErrorNone) {
            sdk.enableCallbackSignaling(false,false);
                sdk.enable3GppTags(true);
                configMgr.setMediaConfig(this, sdk);
        }
        sdk.getAudioDevices();
        return result;
    }

    Runnable unregiestRefresher = new Runnable() {

        @Override
        public void run() {
            handler.removeCallbacks(regiestRefresher);
            AccountManager accountManager = AccountManager.getInstance();
            if(accountManager.getLoginState()!=UserAccount.STATE_NOTONLINE) {
                final NetworkManager networkManagerMgr = NetworkManager.getNetWorkmanager();
                accountManager.unregister(networkManagerMgr, AccountManager.NETWORK_FORBIDDEN, getPackageName());
//                onRegisterFailure(getString(R.string.network_not_availabe),8888,"");
            }
            onRegisterFailure("",8888,"");
//            onRegisterFailure(getString(R.string.network_not_availabe),8888,"");
        }
    };

    Runnable regiestRefresher = new Runnable() {
        @Override
        public void run() {
            synchronized(PortSipService.this) {
                PortSipEngine engine = PortSipEngine.getInstance(PortSipService.this);
                AccountManager accountManager = AccountManager.getInstance();
                int reason = accountManager.getOfflineReason();
                final ConfigurationManager configurationMgr = ConfigurationManager.getInstance();
                final NetworkManager networkManagerMgr = NetworkManager.getNetWorkmanager();

                if (reason == AccountManager.EXIT) {
                    return;
                }
                long result;

                switch (accountManager.getLoginState()) {
                    case UserAccount.STATE_NOTONLINE: {

                        UserAccount defaultUser = AccountManager.getDefaultUser(PortSipService.this);
                        if (reason == AccountManager.NETWORK_UNAVAILABLE) {

                            if (networkManagerMgr.checkNetWorkStatus(PortSipService.this,configurationMgr)) {

                                result = accountManager.autoRegister(PortSipService.this, engine.getSipManager());
                                if(PortSipErrorcode.ECoreErrorNone !=result){
                                    handler.postDelayed(regiestRefresher, REGIST_TIMER_ATONCE);
                                }
                            }else{
                                handler.postDelayed(regiestRefresher, REGIST_TIMER_PERIOD * (++retryTime));
                            }
                        } else if (reason == AccountManager.CORE_ERROR) {//核心错误，重新初始化sdk

                            if (defaultUser != null && defaultUser.isValidate()) {

                                result = initSDK(defaultUser);
                                if (result == PortSipErrorcode.ECoreErrorNone) {

                                    result = accountManager.autoRegister(PortSipService.this, engine.getSipManager());
                                    if(PortSipErrorcode.ECoreErrorNone !=result){
                                        handler.postDelayed(regiestRefresher, REGIST_TIMER_ATONCE);
                                    }
                                } else {
                                    accountManager.setLoginState(UserAccount.STATE_NOTONLINE, AccountManager.CORE_ERROR, "init failed " + result,true);
                                    handler.postDelayed(regiestRefresher, REGIST_TIMER_PERIOD * (++retryTime));
                                }
                            }
                        } else if (reason == AccountManager.NETWORK_FORBIDDEN) {//这个机制基本不会走，目前，需要反注册退出程序，才能更改3g开关

                            if (networkManagerMgr.checkNetWorkStatus(PortSipService.this,configurationMgr)) {//因为禁止3g导致自动下线。重新登陆录

                                if (defaultUser != null && defaultUser.isValidate()) {
                                    result = initSDK(defaultUser);

                                    if (result == PortSipErrorcode.ECoreErrorNone) {
                                        result = accountManager.autoRegister(PortSipService.this, engine.getSipManager());
                                        if(PortSipErrorcode.ECoreErrorNone !=result){
                                            handler.postDelayed(regiestRefresher, REGIST_TIMER_ATONCE);
                                            //handler.postDelayed(regiestRefresher, REGIST_TIMER_PERIOD * (++retryTime));
                                        }
                                    } else {
                                        accountManager.setLoginState(UserAccount.STATE_NOTONLINE, AccountManager.CORE_ERROR, "init failed " + result,true);
                                        handler.postDelayed(regiestRefresher, REGIST_TIMER_PERIOD * (++retryTime));
                                    }
                                }
                            }
                        } else if (reason == AccountManager.PUSH) {

                        } else if (reason == AccountManager.EXIT) {
                        }
                    }

                    break;
                    case UserAccount.STATE_ONLINE:

                        String pakageName = getPackageName();
                        boolean useWifi = configurationMgr.getBooleanValue(PortSipService.this, ConfigurationManager.PRESENCE_USEWIFI
                                , getResources().getBoolean(R.bool.wifi_default));
                        boolean use3G = configurationMgr.getBooleanValue(PortSipService.this, ConfigurationManager.PRESENCE_VOIP
                                , getResources().getBoolean(R.bool.prefrence_voipcall_default));

                        if (!use3G && !useWifi) {

                            accountManager.unregister(networkManagerMgr, AccountManager.NETWORK_FORBIDDEN, pakageName);
                        } else {
                            accountManager.setLoginState(UserAccount.STATE_LOGIN,getResources().getString(R.string.network_not_availabe));
                            PortSipSdkWrapper.getInstance().refreshRegistration(0);
                        }
                        break;
                    case UserAccount.STATE_LOGIN:
                        break;
                }

            }
        }
    } ;
    Handler handler = null;
    private MediaPlayer mediaPlayer;
    @Override
    synchronized  public void onCreate() {
        super.onCreate();

        enableRing = getResources().getBoolean(R.bool.prefrence_ring_default);
        enableVibrate= getResources().getBoolean(R.bool.prefrence_vibrate_default);

        PortSipEngine engine = PortSipEngine.getInstance(this);
        messageProcessor = new InComingMessageProcessor(this);
        sipSdk = engine.mSdk;
        sipSdk.setOnPortSIPEvent(this);
        mediaPlayer = new  MediaPlayer();
        mediaPlayer.setOnPreparedListener(this);
        mediaPlayer.setOnErrorListener(this);
        mediaPlayer.setOnCompletionListener(this);
        Ring ring = Ring.getInstance();
        ring.init(this);

        handler = new Handler();
        if(HASPRESENCE) {
            presenceTask = new PresenceTask();
        }

        CallManager.getInstance().getObservableCalls().addObserver(PortSipService.this);
        AccountManager.getInstance().addObserver(PortSipService.this);
        NetworkManager networkManager  = NetworkManager.getNetWorkmanager();
        if(networkManager.start(this)){
            networkManager.setNetWorkChangeListner(new MyNetWorkChangerListener());
        }
        appName = getString(R.string.app_name);

        Intent intent = new Intent(this, PortActivityLogin.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        NotificationUtils.getInstance(this).showAppNotification(this,R.drawable.offline,appName,
                getString(R.string.app_offline),intent);


        if(BuildConfig.SUPPORTPUSH) {
            try {
                FirebaseInstanceId.getInstance().getInstanceId().addOnCompleteListener(new OnCompleteListener<InstanceIdResult>(){
                    @Override
                    public void onComplete(@NonNull Task<InstanceIdResult> task) {
                        if (!task.isSuccessful()) {
                            return;
                        }

                        String pushToken = task.getResult().getToken();
                        Log.d("pushToken",pushToken);
                        AccountManager accountManager = AccountManager.getInstance();
                        accountManager.setTokenRefresh(pushToken);
                        accountManager.enablePBXPush(getPackageName(),accountManager.getOfflineReason()!=AccountManager.EXIT);

                        if (accountManager.getLoginState() == UserAccount.STATE_ONLINE) {
                            sipSdk.refreshRegistration(0);
                        }

                    }
                });
            }catch (IllegalStateException e){
            }
        }

    }
    private void registPhonestateListener(PhoneStateReceiver stateChangeListener){
        if(stateChangeListener!=null){
            IntentFilter filter = new IntentFilter();
            filter.addAction("android.intent.action.NEW_OUTGOING_CALL");
            filter.addAction("android.intent.action.PHONE_STATE");
            filter.setPriority(Integer.MAX_VALUE);//-1000,1000
            registerReceiver(stateChangeListener,filter);
        }
    }

    private void unregisterPhonestateListener(PhoneStateReceiver receiver) {
        if(receiver!=null) {
            unregisterReceiver(receiver);
        }
    }

    void uploadStreamFile(long messageid,String mimetype,String filePath){
        File file = new File(filePath);
        if(file.exists()) {
            OkHttpHelper.upLoadFile(messageid,mimetype,filePath,messageProcessor);
        }
    }

    void downloadStreamFile(long messageid,String FilePath,String fileName,String url){
        OkHttpHelper.downLoadFile(url,messageid,fileName,FilePath,messageProcessor);

    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        this.stopSelf();
    }

    @Override
    synchronized public void onDestroy() {
        handler.removeCallbacks(regiestRefresher);
        handler.removeCallbacks(unregiestRefresher);
        //logutil.release();
        if(presenceTask!=null&& presenceTask.getStatus() == AsyncTask.Status.RUNNING) {
            presenceTask.cancel(true);
        }
//        unregisterPhonestateListener(stateChangeListener);
        stopForeground(true);
        AccountManager.getInstance().setLoginState(UserAccount.STATE_NOTONLINE,null);
        CallManager.getInstance().terminateAllCalls(this);
        CallManager.getInstance().clear();
        PortSipEngine engine = PortSipEngine.getInstance(this);
        PortApplication application = (PortApplication)getApplication();
        engine.unInit();
        application.closeActivitys();
        NotificationUtils.getInstance(this).cancelAllNotification(this);

        EmotionDataManager manager = EmotionDataManager.getInstance();
        manager.unloadEmotion();
        NetworkManager.getNetWorkmanager().stop();
        Ring.getInstance().uninit();
        OkHttpHelper.cancellAll();
        DataBaseManager.upDataProcessingMessagetoFail(this);
        messageProcessor = null;
        super.onDestroy();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent,flags,startId);

        AccountManager accountManager = AccountManager.getInstance();
        UserAccount defaultUser = AccountManager.getDefaultUser(this);
        PortSipSdkWrapper sipSdk = PortSipSdkWrapper.getInstance();
        String appname = getPackageName();

        if(intent!=null) {
            if (BuildConfig.PORT_ACTION_TOKEN.equals(intent.getAction())) {
                String token = intent.getStringExtra(PortSipService.TOKEN_REFRESH);
                if (!TextUtils.isEmpty(token)) {
                    accountManager.setTokenRefresh(token);
                    accountManager.enablePBXPush(getPackageName(),accountManager.getOfflineReason()!=AccountManager.EXIT);
                    if (accountManager.getLoginState() == UserAccount.STATE_ONLINE) {
                        sipSdk.refreshRegistration(0);
                    }
                }
            } else if (BuildConfig.PORT_ACTION_REGIEST.equals(intent.getAction())&&defaultUser != null && defaultUser.isValidate()) {
                if ( accountManager.getLoginState() != UserAccount.STATE_ONLINE||!sipSdk.isInitialized()) {
                    int result = initSDK(defaultUser);
                    if(result==PortSipErrorcode.ECoreErrorNone) {
                        accountManager.setLoginState(UserAccount.STATE_NOTONLINE,AccountManager.NETWORK_UNAVAILABLE,getString(R.string.network_not_availabe),false);
                    }else{
                        accountManager.setLoginState(UserAccount.STATE_NOTONLINE,AccountManager.CORE_ERROR,"init failed "+result,true);
                    }
                    handler.removeCallbacks(regiestRefresher);
                    handler.post(regiestRefresher);
                }
            } else if (BuildConfig.PORT_ACTION_UNREGIEST.equals(intent.getAction())) {
                handler.removeCallbacks(regiestRefresher);
                handler.removeCallbacks(unregiestRefresher);

                SharedPreferences sharedPreferences = getSharedPreferences("",Context.MODE_PRIVATE);
                sharedPreferences.edit().putString("svrip","").commit();

                accountManager.unregister(NetworkManager.getNetWorkmanager(),AccountManager.EXIT,appname);

                PortSipEngine engine = PortSipEngine.getInstance(this);
                engine.unInit();

            } else if(BuildConfig.PORT_ACTION_AUTOLOGIN.equals(intent.getAction())&&defaultUser != null && defaultUser.isValidate()){
                if ( accountManager.getLoginState() != UserAccount.STATE_ONLINE) {//
                    int result = PortSipErrorcode.ECoreErrorNone;
                    if(!sipSdk.isInitialized()) {
                        result = initSDK(defaultUser);
                    }
                    if (result != PortSipErrorcode.ECoreErrorNone) {
                        accountManager.setLoginState(UserAccount.STATE_NOTONLINE, AccountManager.CORE_ERROR, "init failed " + result, true);
                    }
                    handler.removeCallbacks(regiestRefresher);
                    handler.post(regiestRefresher);
                }
            }else if(PORT_ACTION_REJECT.equals(intent.getAction())){
                int callid = intent.getIntExtra("CALLID",PortSipErrorcode.INVALID_SESSION_ID);
                NotificationUtils.getInstance(this).cancelPendingCallNotification(this);
                PortSipCall sipCall = CallManager.getInstance().getCallByCallId(callid);
                if(sipCall!=null){
                    sipCall.reject(this);
                }
            }
        }
        NotificationUtils.getInstance(this).cancelAllNotification(this);
        return START_STICKY;
    }

    @Override
    public void startActivity(Intent intent) {

        super.startActivity(intent);
    }

    @Override
    public void onReferAccepted(long arg0) {

    }

    @Override
    public void onReferRejected(long arg0, String arg1, int arg2) {
        PortSipCall call =  CallManager.getInstance().getCallBySessionId(arg0);
        if(call!=null){
            showAlertDialog(this.getApplicationContext(), call.getRemoteDisName(), "refer failed!" + arg1, "refer");
        }
    }
    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    @Override
    public void onTransferRinging(long arg0) {
        InputStream in = getResources().openRawResource(R.raw.ringtone);
        String url = getExternalFilesDir(null).getAbsolutePath()+"/ringtone.wav";
        File file = new File(url);
        if(!file.exists()) {
            try {
                FileOutputStream outputStream = new FileOutputStream(file);
                int lenght = in.available();
                byte[]  buffer = new byte[lenght];
                in.read(buffer);
                outputStream.write(buffer);
                outputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        sipSdk.playAudioFileToRemote(arg0,file.getAbsolutePath(),8000,true);
    }

    @Override
    public void onTransferTrying(long arg0) {
    }

    @Override
    public void onACTVTransferSuccess(long arg0) {
        sipSdk.stopPlayAudioFileToRemote(arg0);

        PortSipCall call =  CallManager.getInstance().getCallBySessionId(arg0);
        if(call!=null){
            showAlertDialog(this.getApplicationContext(),call.getRemoteDisName(),"refer success!","refer");
            call.terminate(PortSipService.this);
        }
    }

    @Override
    public void onACTVTransferFailure(long arg0, String arg1, int arg2) {
        sipSdk.stopPlayAudioFileToRemote(arg0);
        PortSipCall call =  CallManager.getInstance().getCallBySessionId(arg0);
        if(call!=null){
            showAlertDialog(this.getApplicationContext(), call.getRemoteDisName(), "refer failed!" + arg1, "refer");
        }
    }

    @Override
    public void onAudioRawCallback(long arg0, int arg1, byte[] arg2, int arg3,
                                   int arg4) {

    }

    @Override
    public void onInviteAnswered(long sessionId, String callerDisplayName,String caller,
                                 String calleeDisplayName, String callee,
                                 String audioCodecs, String videoCodecs, boolean existsAudio,
                                 boolean existsVideo,String conferenceFocus){
        PortSipEngine engine = PortSipEngine.getInstance(this);
        final ConfigurationManager configurationMgr = ConfigurationManager.getInstance();
        CallManager callManager = CallManager.getInstance();
        PortSipCall call =  callManager.getCallBySessionId(sessionId);
        if(call!=null){
            if(engine.getSipManager().isConference()){
                call.joinToConference();
            }else {
                callManager.holdAllCallExcept(call);
            }

            call.remoteAccept(PortSipService.this,existsVideo,null);
            boolean defautRecord = getResources().getBoolean(R.bool.prefrence_record_default);
            boolean record = configurationMgr.getBooleanValue(this,ConfigurationManager.PRESENCE_CALLING_RECORD, defautRecord);
            if (record) {
                String dirName = getString(R.string.prefrence_record_filepath_default);
                String defaultdir;
                try {
                    defaultdir = getApplicationContext().getExternalFilesDir(dirName).getAbsolutePath();
                }catch (NullPointerException e){
                    defaultdir = "";
                }

                String dir = configurationMgr.getStringValue(this,ConfigurationManager.PRESENCE_RECORD_DIR, defaultdir);
                call.startMediaRecord(this,dir);
            }
        }
    }

    @Override
    public void onInviteFailure(long sessionId, String reason, int code, String sipmessage) {
        PortSipCall call =  CallManager.getInstance().getCallBySessionId(sessionId);
        if(call!=null){
            showAlertDialog(this.getApplicationContext(),call.getRemoteDisName(),reason,"failed");
            call.remoteTerminate(PortSipService.this,reason);
        }
    }

    @Override
    public void onInviteBeginingForward(String arg0) {

    }

    @Override
    public void onInviteClosed(long sessionId) {
        CallManager callManager = CallManager.getInstance();
        PortSipCall call =  callManager.getCallBySessionId(sessionId);
        if(call!=null) {
            call.stopMediaRecord();
            call.remoteTerminate(PortSipService.this, "closed");
            Calendar calltime = call.getCallTime();
            showAlertDialog(this.getApplicationContext(), call.getRemoteDisName(),
                    "" + DateFormat.format("HH:mm:ss", calltime), "closed");
        }

    }

    @Override
    public void onInviteConnected(long sessionId) {
        CallManager callManager = CallManager.getInstance();
        SipManager sipManager = SipManager.getSipManager();
        PortSipCall call =  callManager.getCallBySessionId(sessionId);

        if(call!=null) {

            if(call.isOutGoingCall()&&!call.isConnect()){
                onInviteAnswered(sessionId,null,null,call.getRemoteDisName(),call.getRemoteParty(),null,null,true,call.isVideoCall(),null);
                call.remoteAccept(PortSipService.this,call.isVideoCall(),null);
            }

            sipManager.setSendVideo(sessionId, call.isSendVideo());
            if(sipManager.isConference()){
                call.joinToConference();
            }
        }
    }


    @Override
    public void onRegisterSuccess(String reason, int code, String sipmessage) {

        synchronized(PortSipService.this) {

            setCpuRun(true);//lock cpu on wake state
            PortSipEngine engine = PortSipEngine.getInstance(this);
            AccountManager accountManager = AccountManager.getInstance();
            retryTime = 0;

            handler.removeCallbacks(regiestRefresher);
            String support = engine.mSdk.getSipMessageHeaderValue(sipmessage,"x-p-push");
            String useragent = engine.mSdk.getSipMessageHeaderValue(sipmessage, "User-Agent");

            engine.getSipManager().setServerAgent(useragent, support);
            if (accountManager != null) {
				accountManager.setLoginState(UserAccount.STATE_ONLINE, AccountManager.NETWORK_UNAVAILABLE, reason,true);

                UserAccount account = AccountManager.getDefaultUser(this);
				OkHttpHelper.setAccount(account);
                OkHttpHelper.offlineMsgContactList("0","UNREAD",messageProcessor);
            }

            if (presenceTask != null && presenceTask.getStatus() != AsyncTask.Status.RUNNING) {
                presenceTask.doInBackground(null);
            }
        }

    }

    @Override
    public void onRegisterFailure(String reason, int code, String sipmessage) {

        synchronized(PortSipService.this) {
            setCpuRun(false);//release cpu lock
            AccountManager accountManager = AccountManager.getInstance();
            final ConfigurationManager configurationMgr = ConfigurationManager.getInstance();
            CallManager callManager = CallManager.getInstance();

            if (presenceTask != null && presenceTask.getStatus() == AsyncTask.Status.RUNNING) {
                presenceTask.cancel(true);
            }
            retryTime = retryTime > REGIST_MAX_RETRY ? REGIST_MAX_RETRY : ++retryTime;
            callManager.clearSubScribedTime();
            callManager.clearSubScribeId();

            callManager.terminateAllCalls(PortSipService.this);
            callManager.clear();
            switch (code) {
                case 403://forbidden
                case 404://not found
                case 401://unauthorized
                    accountManager.setLoginState(UserAccount.STATE_NOTONLINE, AccountManager.ACCOUNT_UNAVAILABLE, reason,true);
                    break;
                case 8888:
                    if(accountManager.getOfflineReason()!=AccountManager.EXIT) {
                        accountManager.setLoginState(UserAccount.STATE_NOTONLINE, AccountManager.NETWORK_UNAVAILABLE, reason, false);
                        handler.removeCallbacks(regiestRefresher);
                        handler.postDelayed(regiestRefresher, 500);
                    }
                    break;

                default://
                    if(accountManager.getOfflineReason()!=AccountManager.EXIT) {
                        accountManager.setLoginState(UserAccount.STATE_NOTONLINE, AccountManager.NETWORK_UNAVAILABLE, reason, true);
                        handler.removeCallbacks(regiestRefresher);
                        handler.postDelayed(regiestRefresher, REGIST_TIMER_PERIOD * retryTime);

                    }
                    break;

            }
        }
        Intent intent = new Intent(this, PortActivityLogin.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);

        NotificationUtils.getInstance(this).showAppNotification(this,R.drawable.offline,appName,
                getString(R.string.app_offline),intent);

    }


    PresenceTask presenceTask= null;


    class PresenceTask extends AsyncTask{

        @Override
        protected Object doInBackground(Object[] objects) {
            synchronized (PortSipService.this) {
                if (PermissionManager.testDangercePermission(PortSipService.this, Manifest.permission.WRITE_CONTACTS) &&
                        PermissionManager.testDangercePermission(PortSipService.this, Manifest.permission.READ_CONTACTS)) {
                    PortSipEngine engine = PortSipEngine.getInstance(PortSipService.this);
                    SipManager sipManager = engine.getSipManager();
                    CallManager callManager = CallManager.getInstance();
                    UserAccount userAccount = AccountManager.getDefaultUser(PortSipService.this);
                    String status = "Hello";
                    if (userAccount != null) {
                        status = getResources().getString(userAccount.getPresenceCommandRes());
                    }
                    HashMap<Integer, String> imFriends = new HashMap<>();
                    if (!isCancelled()) {
                        Uri uriRight = android.provider.ContactsContract.Data.CONTENT_URI;
                        String[] projection = new String[]{ContactsContract.Data.RAW_CONTACT_ID, ContactsContract.CommonDataKinds.Im.DATA1};
                        String selection = android.provider.ContactsContract.Data.MIMETYPE + "=?" + " AND "
                                + android.provider.ContactsContract.CommonDataKinds.Im.CUSTOM_PROTOCOL + "=?";
                        String[] selectionArg = new String[]{ContactsContract.CommonDataKinds.Im.CONTENT_ITEM_TYPE, PORTSIP_IM_PROTOCAL};

                        Cursor cursor = CursorHelper.resolverQuery(getContentResolver(),uriRight, projection, selection, selectionArg, null);
                        while (CursorHelper.moveCursorToNext(cursor)) {
                            int rawContactId = cursor.getInt(0);
                            String address = cursor.getString(1);
                            if (!TextUtils.isEmpty(address)) {
                                imFriends.put(rawContactId, address);
                            }
                        }
                        CursorHelper.closeCursor(cursor);


                        if (isCancelled()) {
                            return null;
                        }
                        clearPresenceStatus(getContentResolver(), imFriends);
                        if (isCancelled()) {
                            return null;
                        }
                        Iterator iterator = imFriends.entrySet().iterator();
                        while (iterator.hasNext()) {
                            Map.Entry entry = (Map.Entry) iterator.next();
                            String handle = (String) entry.getValue();
                            Integer rawContactid = (Integer) entry.getKey();
                            if (isCancelled()) {
                                break;
                            }
                            String[]array =  handle.split("@");
                            if(array.length>0) {
                                sipManager.presenceSubscribe(array[0], status);
                            }
                            callManager.putsubScribedTime(rawContactid, System.currentTimeMillis());
                        }

                        sipManager.setPresenceStatus(-1, getString(userAccount.getPresenceCommandRes()));
                    }
                }
                return null;
            }
        }
    }

    @Override
    public void onInviteIncoming(long sessionId,
                                 String callerDisplayName,
                                 String caller,
                                 String calleeDisplayName,
                                 String callee,
                                 String audioCodecs,
                                 String videoCodecs,
                                 boolean existsAudio,
                                 boolean existsVideo,
                                 String message)
    {

        int mediaType =PortSipCall.MEDIATYPE_AUDIO;
        PortSipEngine engine = PortSipEngine.getInstance(this);
        if(existsAudio&&existsVideo){
            mediaType = PortSipCall.MEDIATYPE_AUDIOVIDEO;
        }else if(existsAudio){
            mediaType =PortSipCall.MEDIATYPE_AUDIO;
        }else if(existsVideo){
            if(HASVIDEO&&ENABLEVIDEO) {
                mediaType = PortSipCall.MEDIATYPE_VIDEO;
            }else{
                mediaType = PortSipCall.MEDIATYPE_AUDIO;
            }
        }
        //mSipManager.startRing();//
        SipManager sipManager = engine.getSipManager();

        if(caller!=null&&caller.contains(";")){
            caller = caller.split(";")[0];
        }

        if(callee!=null&&callee.contains(";")){
            callee = callee.split(";")[0];
        }

        int callid =PortSipErrorcode.INVALID_SESSION_ID;
        caller = caller.replaceFirst(NgnUriUtils.SIP_HEADER,"");
        RemoteRecord remoteRecord = RemoteRecord.getRemoteRecord(getContentResolver(),caller,callerDisplayName);
        PortSipCall call =null;
        UserAccount userAccount = AccountManager.getDefaultUser(PortSipService.this);
        String disName = NgnStringUtils.isNullOrEmpty(callerDisplayName)?NgnUriUtils.getDisplayName(caller,PortSipService.this):callerDisplayName;
        try {

            CallReferTask referTask = null;
            CallManager callManager = CallManager.getInstance();
            if(UserAccount.FORWARD_NOANSWER==userAccount.getFowardMode()&&userAccount.getFowardNoAnswerTime()>0&&!TextUtils.isEmpty(userAccount.getFowardTo())){
                referTask = new CallReferTask(sessionId,userAccount.getFowardNoAnswerTime()*1000,userAccount.getFowardTo(),sipManager,callManager,this);
            }

            call  = callManager.portSipCallIn(this,sipManager,referTask,sessionId,userAccount.getFullAccountReamName(), (int) remoteRecord.getRowID(), caller,disName,  mediaType);
            callid = call.getCallId();
            sipManager.startRing();//
            new ContactQueryTask().execute(this,remoteRecord.getRowID(),caller);
            PortApplication mApp =(PortApplication)this.getApplicationContext();

            if((Build.VERSION.SDK_INT> Build.VERSION_CODES.P)&&!mApp.isForeground()){
                Intent intent = new Intent(this, PortIncallActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                NotificationUtils.getInstance(this).showPendingCallNotification(this,callid,R.drawable.online,appName,disName,intent);
            }else{
                mApp.startAVActivity(callid);
            }
        } catch (OutOfMaxLineException e) {
            String local = userAccount.getFullAccountReamName();
            final String constrain = DBHelperBase.HistoryColumns.HISTORY_CONNECTED+"=0 AND "
                    +DBHelperBase.HistoryColumns.HISTORY_LOCAL + "=? AND "+DBHelperBase.HistoryColumns.HISTORY_SEEN+"=0 AND "
                    +DBHelperBase.HistoryColumns.HISTORY_CALLOUT+"=0";
            Cursor cursor = CursorHelper.resolverQuery(getContentResolver(), DBHelperBase.ViewHistoryColumns.CONTENT_URI,
                    null, constrain, new String[]{local}, DBHelperBase.ViewHistoryColumns.DEFAULT_ORDER);
            int unreadCall  =1;
            if(CursorHelper.moveCursorToFirst(cursor)) {
                unreadCall = cursor.getCount();
            }
            CursorHelper.closeCursor(cursor);

            NotificationUtils.getInstance(this).showMessageNotification(this,R.drawable.pending_call, disName,
                    ""+unreadCall+getString(R.string.missed_call));
            e.printStackTrace();
        } catch (LineBusyException e) {
            e.printStackTrace();
        }

    }
    /**
     *
     */
    @Override
    public void onInviteRinging(long sessionId,
                                String statusText,
                                int statusCode,
                                String message){
        PortSipEngine engine = PortSipEngine.getInstance(this);
        final PortSipCall call = CallManager.getInstance().getCallBySessionId(sessionId);
        if (call != null) {
            call.remoteRing();
        }

        engine.getSipManager().startRingback();
    }
    /**
     *
     */
    @Override
    public void onInviteSessionProgress(long sessionId,
                                        String audioCodecs,
                                        String videoCodecs,
                                        boolean existsEarlyMedia,
                                        boolean existsAudio,
                                        boolean existsVideo,
                                        String message) {
        final PortSipCall call =  CallManager.getInstance().getCallBySessionId(sessionId);
        if (call != null) {
            call.setearlyMedia(existsEarlyMedia);
        }

        if (existsVideo) {
        }
        if (existsAudio) {
        }
    }
    /**
     *
     */
    @Override
    public void onInviteTrying(long sessionId) {
        final PortSipCall call = CallManager.getInstance().getCallBySessionId(sessionId);
        if (call != null) {
            call.remoteTring();
        }
    }

    /**
     * @param sessionId
     * @param audioCodecs
     * @param videoCodecs
     * @param existsAudio
     * @param existsVideo
     * @param message
     */
    @Override
    public void onInviteUpdated(long sessionId,
                                String audioCodecs,
                                String videoCodecs,
                                boolean existsAudio,
                                boolean existsVideo,
                                String message){
        final PortSipCall call =  CallManager.getInstance().getCallBySessionId(sessionId);
        final ConfigurationManager configurationMgr = ConfigurationManager.getInstance();
        if(call!=null){
            String dirName = getString(R.string.prefrence_record_filepath_default);
            String defaultdir;
            try {
                defaultdir = getApplicationContext().getExternalFilesDir(dirName).getAbsolutePath();
            }catch (NullPointerException e){
                defaultdir=null;
            }
            String recordFilePath = configurationMgr.getStringValue(this,ConfigurationManager.PRESENCE_RECORD_DIR, defaultdir);

            if(!HASVIDEO||!ENABLEVIDEO) {
                existsVideo = false;//
            }
            call.setVideoNegotiateResult(existsVideo);
            if(existsVideo&&existsAudio){
                call.setCallType(NgnMediaType.AudioVideo);
            }else if(existsVideo) {
                call.setCallType(NgnMediaType.Video);
            }else if(existsAudio) {
                call.setCallType(NgnMediaType.Audio);
            }

            call.portSipUpdate(this,call.getCallType(),recordFilePath);
            call.notifyObserverUpdate();
        }
    }

    @Override
    public void onPlayAudioFileFinished(long arg0, String arg1) {
        // TODO Auto-generated method stub
    }

    @Override
    public void onPlayVideoFileFinished(long arg0) {
        // TODO Auto-generated method stub

    }

    //
    @Override
    public void onPresenceOffline(String fromDisplayName, String from) {
        if (!HASPRESENCE||!PermissionManager.testDangercePermission(PortSipService.this, Manifest.permission.WRITE_CONTACTS)) {
            return ;
        }


        int resLable = R.string.status_offline, resIcon = R.drawable.mid_content_status_offline_ico;
        int presenceMode =StatusUpdates.OFFLINE;
        from= from.replaceFirst(NgnUriUtils.SIP_HEADER,"");

        try {
            String[] handles={from};
            StatusUpdatesHelper.insertStatusUpdate(this,-1,presenceMode,null,System.currentTimeMillis(),handles,resLable,resIcon,getPackageName());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    //
    @Override
    public void onPresenceOnline(String fromDisplayName, String from, String stateText) {
        if (!HASPRESENCE||!PermissionManager.testDangercePermission(PortSipService.this, Manifest.permission.WRITE_CONTACTS)) {
            return ;
        }


        from= from.replace("sip:","");
        ContentResolver resolver = getContentResolver();
        Uri uri= ContactsContract.Data.CONTENT_URI;
        String[]projection =new String[]{ContactsContract.Data._ID,ContactsContract.Data.RAW_CONTACT_ID};
        String selection = ContactsContract.Data.MIMETYPE + "=?" + " AND "
                + ContactsContract.CommonDataKinds.Im.CUSTOM_PROTOCOL+"=?"
                + " AND " + ContactsContract.CommonDataKinds.Im.DATA1+"=?";
        String[]selectionArg = new String[]{ContactsContract.CommonDataKinds.Im.CONTENT_ITEM_TYPE, PORTSIP_IM_PROTOCAL,from};
        Cursor cursor = CursorHelper.resolverQuery(resolver,uri,projection,selection,selectionArg,null);
        int frindNum = 0;
        if(cursor!=null) {
            frindNum = cursor.getCount();
        }
        CursorHelper.closeCursor(cursor);
        if(frindNum>0) {//
            int resLable = R.string.status_online,
                    resIcon = R.drawable.mid_content_status_online_ico;
            int presenceMode = 0;
            if (!TextUtils.isEmpty(stateText)) {
                if (stateText.equals(getString(R.string.cmd_presence_online))) {
                    resIcon = R.drawable.mid_content_status_online_ico;
                    resLable = R.string.status_online;
                    presenceMode = ContactsContract.StatusUpdates.AVAILABLE;
                } else if (stateText.equals(getString(R.string.cmd_presence_away))) {
                    resIcon = R.drawable.mid_content_status_away_ico;
                    resLable = R.string.status_away;
                    presenceMode = ContactsContract.StatusUpdates.AWAY;
                } else if (stateText.equals(getString(R.string.cmd_presence_nodisturb))) {
                    resIcon = R.drawable.mid_content_status_nodisturb_ico;
                    resLable = R.string.status_nodistrub;
                    presenceMode = ContactsContract.StatusUpdates.DO_NOT_DISTURB;
                } else if (stateText.equals(getString(R.string.cmd_presence_busy))) {
                    resIcon = R.drawable.mid_content_status_busy_ico;
                    resLable = R.string.status_busy;
                    presenceMode = ContactsContract.StatusUpdates.INVISIBLE;
                } else if (stateText.equals(getString(R.string.cmd_presence_offline))) {
                    resIcon = R.drawable.mid_content_status_offline_ico;
                    resLable = R.string.status_offline;
                    presenceMode = ContactsContract.StatusUpdates.OFFLINE;
                } else {
                    resIcon = R.drawable.mid_content_status_online_ico;
                    resLable = R.string.status_online;
                    presenceMode = ContactsContract.StatusUpdates.AVAILABLE;
                }
            }else{
                presenceMode = ContactsContract.StatusUpdates.OFFLINE;
                resIcon = R.drawable.mid_content_status_offline_ico;
                resLable = R.string.status_offline;
                stateText = getString(R.string.status_offline);
            }

            try {
                String[] handles={from};
                StatusUpdatesHelper.insertStatusUpdate(this,-1,presenceMode,stateText,System.currentTimeMillis(),handles,resLable,resIcon,getPackageName());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        /*http://apiminer.org/doc/reference/android/provider/ContactsContract.StatusUpdates.html*/
    }

    void clearPresenceStatus(ContentResolver resolver,HashMap<Integer,String> friends){
        if (!HASPRESENCE||!PermissionManager.testDangercePermission(PortSipService.this, Manifest.permission.WRITE_CONTACTS)) {
            return ;
        }

        try {
            StatusUpdatesHelper.insertStatusUpdate(this,0,StatusUpdates.OFFLINE,null,System.currentTimeMillis(),
                    friends.values().toArray(new String[friends.size()]),R.string.status_offline,R.drawable.mid_content_status_offline_ico,getPackageName());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onPresenceRecvSubscribe(long subscribeId, String
            fromDisplayName, String from, String subject) {
        if(true) {
            return;//
        }else{
            if (!HASPRESENCE || TextUtils.isEmpty(from))
                return;

            if (!PermissionManager.testDangercePermission(PortSipService.this, Manifest.permission.WRITE_CONTACTS)) {
                return;
            }

            if (TextUtils.isEmpty(fromDisplayName)) {
                fromDisplayName = NgnUriUtils.getUserName(from);
            }
            PortSipEngine engine = PortSipEngine.getInstance(this);
            ContentResolver resolver = getContentResolver();
            from = from.replace("sip:", "");

            Uri uri = ContactsContract.Data.CONTENT_URI;
            String[] projection = new String[]{ContactsContract.Data._ID, ContactsContract.Data.RAW_CONTACT_ID};
            String selection = ContactsContract.Data.MIMETYPE + "=?" + " AND "
                    + ContactsContract.CommonDataKinds.Im.CUSTOM_PROTOCOL + "=?"
                    + " AND " + ContactsContract.CommonDataKinds.Im.DATA1 + "=?";
            String[] selectionArg = new String[]{ContactsContract.CommonDataKinds.Im.CONTENT_ITEM_TYPE, PORTSIP_IM_PROTOCAL, from};
            Cursor cursor = CursorHelper.resolverQuery(resolver, uri, projection, selection, selectionArg, null);
            int rawContactid = Contact.INVALIDE_ID;
            if (CursorHelper.moveCursorToFirst(cursor)) {
                rawContactid = cursor.getInt(1);
            }
            CursorHelper.closeCursor(cursor);


            CallManager callManager = CallManager.getInstance();
            if (rawContactid > Contact.INVALIDE_ID) {

                SipManager sipManager = engine.getSipManager();
                sipManager.presenceAcceptSubscribe(subscribeId);//
                int resCommand = R.string.status_online;
                UserAccount def = AccountManager.getDefaultUser(this);
                if (def != null) {
                    resCommand = def.getPresenceCommandRes();
                }
                Long time = callManager.getSubscribedTime(rawContactid);
                if (time == null || time == 0 || Math.abs(System.currentTimeMillis() - time) > 1000 * 10) {
                    //
                    sipManager.presenceSubscribe(from, "Hello me");//
                    callManager.putsubScribedTime(rawContactid, System.currentTimeMillis());
                }
//            Toast.makeText(this,"Receive Sub from:" + from,Toast.LENGTH_SHORT).show();
                sipManager.setPresenceStatus(subscribeId, getString(resCommand));//
                callManager.putsubScribeId(subscribeId, rawContactid);
            } else {//
                //
                uri = DBHelperBase.SubscribeColumns.CONTENT_URI;

                projection = new String[]{DBHelperBase.SubscribeColumns._ID, DBHelperBase.SubscribeColumns.SUBSCRIB_ACCTION};
                selection = DBHelperBase.SubscribeColumns.SUBSCRIB_REMOTE + "=?";
                selectionArg = new String[]{from};

                cursor = CursorHelper.resolverQuery(resolver, uri, projection, selection, selectionArg, null);

                int subjectid = Contact.INVALIDE_ID;
                int action = 0;
                if (CursorHelper.moveCursorToFirst(cursor)) {
                    subjectid = cursor.getInt(0);
                    action = cursor.getInt(1);
                }
                CursorHelper.closeCursor(cursor);

                ContentValues contentValues = new ContentValues();
                contentValues.put(ContactsContract.StatusUpdates.PROTOCOL, ContactsContract.CommonDataKinds.Im.PROTOCOL_CUSTOM);
                contentValues.put(ContactsContract.StatusUpdates.CUSTOM_PROTOCOL, PORTSIP_IM_PROTOCAL);
                contentValues.put(ContactsContract.StatusUpdates.IM_HANDLE, from);
                contentValues.put(ContactsContract.StatusUpdates.STATUS_TIMESTAMP, System.currentTimeMillis());
                contentValues.put(ContactsContract.StatusUpdates.STATUS, subject);
                contentValues.put(ContactsContract.StatusUpdates.PRESENCE, ContactsContract.StatusUpdates.OFFLINE);


                if (subjectid > Contact.INVALIDE_ID) {//
                    if (rawContactid == INVALIDE_ID && action == DBHelperBase.SubscribeColumns.ACTION_ACCEPTED) {

                        ContentValues values = new ContentValues(1);
                        values.put(DBHelperBase.SubscribeColumns.SUBSCRIB_ACCTION, DBHelperBase.SubscribeColumns.ACTION_NONE);
                        values.put(DBHelperBase.SubscribeColumns.SUBSCRIB_SEEN, DBHelperBase.SubscribeColumns.UN_SEEN);
                        uri = ContentUris.withAppendedId(DBHelperBase.SubscribeColumns.CONTENT_URI, subjectid);
                        resolver.update(uri, values, null, null);
                        callManager.putsubScribeId(subscribeId, (int) subjectid + 5000);
                    } else {

                    }
                } else {
                    ContentValues values = new ContentValues();

                    values.put(DBHelperBase.SubscribeColumns.SUBSCRIB_DESC, subject);
                    values.put(DBHelperBase.SubscribeColumns.SUBSCRIB_REMOTE, from);
                    values.put(DBHelperBase.SubscribeColumns.SUBSCRIB_NAME, fromDisplayName);
                    values.put(DBHelperBase.SubscribeColumns.SUBSCRIB_TIME, System.currentTimeMillis());
                    Uri insertUri = resolver.insert(DBHelperBase.SubscribeColumns.CONTENT_URI, values);
                    if (insertUri != null) {
                        long subID = ContentUris.parseId(insertUri);
                        if (subID > 0) {
                            callManager.putsubScribeId(subscribeId, (int) subID + 5000);
                        }
                    }

                    NotificationUtils.getInstance(this).showSubNotification(this, R.drawable.app_icon, fromDisplayName,
                            String.format(getString(R.string.subscrib_tips), fromDisplayName, subject));
                }
            }
        }
    }

    //This event will be triggered on sending SUBSCRIBE failure.
    @Override
    public void onSubscriptionFailure(long subscribeId, int
            statusCode){
    }

    @Override
    public void onSubscriptionTerminated(long subscribeId) {
        CallManager.getInstance().removeSubScribeIdBySubId(subscribeId);
    }

    @Override
    public void onReceivedRTPPacket(long arg0, boolean arg1, byte[] arg2,
                                    int arg3) {

    }

    @Override
    public void onReceivedRefer(long sessionId, final long referId, final String to,
                                String from, final String referSipMessage) {

        final PortSipCall call = CallManager.getInstance().getCallBySessionId(sessionId);
        if (call == null) {
            sipSdk.rejectRefer(referId);
            return;
        }

        PortActivityReferDialog.showReferDialog(this,sessionId, referId, to,
        from, referSipMessage);
    }

    @Override
    public void onReceivedSignaling(long arg0, String arg1) {

        SharedPreferences sharedPreferences = getSharedPreferences("",Context.MODE_PRIVATE);
        String serviceIp = sharedPreferences.getString("svrip","");
        String head ="WWW-Authenticate: Digest realm=\"";
        String pattern = head+"((2[0-4]\\d|25[0-5]|[01]?\\d\\d?)\\.){3}(2[0-4]\\d|25[0-5]|[01]?\\d\\d?)";
        Pattern r = Pattern.compile(pattern);
        Matcher m = r.matcher(arg1);
        String result=null;
        while (m.find()) {
            result=m.group();
            result = result.replace(head,"");
        }

       if(!TextUtils.isEmpty(result)){
           if(serviceIp.isEmpty()){
               if(arg1.indexOf("expires=0") == -1) {//
                   sharedPreferences.edit().putString("svrip", result).commit();
               }
           }else{
               if(!result.equals(serviceIp)){
                   handler.removeCallbacks(unregiestRefresher);
                   handler.removeCallbacks(regiestRefresher);
                   handler.postDelayed(unregiestRefresher,500);
               }
           }
       }
    }

    @Override
    public void onRecvDtmfTone(long sessionId, int tone) {
        PortSipEngine engine = PortSipEngine.getInstance(this);
        if(engine.getSipManager()!=null){
            engine.getSipManager().portSipRevDtmf(sessionId, tone);
        }
    }

    @Override
    public void onRecvInfo(String arg0) {
        // TODO Auto-generated method stub

    }

    @Override
    public void onRecvNotifyOfSubscription(long l, String s, byte[] bytes, int i) {

    }

    void showGloableDialog(String message, String strPositive,
                           DialogInterface.OnClickListener positiveListener, String strNegative,
                           DialogInterface.OnClickListener negativeListener) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(message);
        if (positiveListener != null) {
            builder.setPositiveButton(strPositive, positiveListener);
        }

        if (negativeListener != null) {
            builder.setNegativeButton(strNegative, negativeListener);
        }

        AlertDialog ad = builder.create();
        ad.getWindow().setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT);
        ad.setCanceledOnTouchOutside(false);
        ad.show();
    }
    @Override
    public void onRecvMessage(long arg0, String arg1, String arg2, byte[] arg3,
                              int arg4) {
    }

    @Override
    public void onRecvOptions(String arg0) {
    }

    @Override
    public void onRecvOutOfDialogMessage(String fromDisplayName, String from,
                                         String toDisplayName, String to, String mimeType,
                                         String subMimeType, byte[] messageData, int messageDataLength,String sipMessage) {
        String pushid = PortSipSdkWrapper.getInstance().getSipMessageHeaderValue(sipMessage,"portsip-push-id");
        String xpushid = PortSipSdkWrapper.getInstance().getSipMessageHeaderValue(sipMessage,"x-push-id");
        long messageid;

        if(TextUtils.isEmpty(pushid)){
            pushid = xpushid;
        }
        if(TextUtils.isEmpty(pushid)){
            messageid = new Random().nextLong();
        }else{
            try {
                messageid = Long.parseLong(pushid);
            }catch (Exception e) {
                messageid = new Random().nextLong();
            }
        }
        messageProcessor.onRecvOutOfDialogMessage(fromDisplayName, from,toDisplayName,  to,  mimeType,
                 subMimeType, messageData,  messageDataLength, sipMessage,messageid, new Date().getTime());
    }

    @Override
    public void onRemoteHold(long sessionId) {
        PortSipCall call =  CallManager.getInstance().getCallBySessionId(sessionId);
        if(call!=null){
            call.setRemoteHold(true);
        }
    }

    @Override
    public void onRemoteUnHold(long sessionId, String audioCodecs,
                               String videoCodecs, boolean existsAudio, boolean existsVideo) {

        PortSipCall call =  CallManager.getInstance().getCallBySessionId(sessionId);
        if(call!=null){
            call.setRemoteHold(false);
        }
    }

    @Override
    public void onSendMessageFailure(long sessionId, long messageId,
                                     String reason, int code) {
    }

    @Override
    public void onSendMessageSuccess(long sessionId, long messageId) {
    }

    @Override
    public void onSendOutOfDialogMessageSuccess(long messageId,
                                                String fromDisplayName, String from, String toDisplayName, String to) {
        DataBaseManager.upDataMessageStatus(this,messageId,MessageEvent.MessageStatus.SUCCESS);
    }

    @Override
    public void onSendOutOfDialogMessageFailure(long messageId,
                                                String fromDisplayName, String from, String toDisplayName,
                                                String to, String reason, int code) {
        if(code == 480){
            DataBaseManager.upDataMessageStatus(this,messageId,MessageEvent.MessageStatus.SUCCESS);
        }else {
            DataBaseManager.upDataMessageStatus(this, messageId, MessageEvent.MessageStatus.Failed);
        }
    }

    @Override
    public void onSendingRTPPacket(long arg0, boolean arg1, byte[] arg2,
                                   int arg3) {
    }

    @Override
    public void onSendingSignaling(long arg0, String arg1) {
        if(TextUtils.isEmpty(arg1)){
           return;
        }

        SharedPreferences sharedPreferences = getSharedPreferences("",Context.MODE_PRIVATE);
        String serviceIp = sharedPreferences.getString("svrip","");
        String head ="realm=\"";
        String pattern = head+"((2[0-4]\\d|25[0-5]|[01]?\\d\\d?)\\.){3}(2[0-4]\\d|25[0-5]|[01]?\\d\\d?)";
        Pattern r = Pattern.compile(pattern);
        Matcher m = r.matcher(arg1);
        String result=null;
        while (m.find()) {
            result=m.group();
            result = result.replace(head,"");
        }

        if(!TextUtils.isEmpty(result)){
            if(serviceIp.isEmpty()){
                if(arg1.indexOf("expires=0") == -1) {
                    sharedPreferences.edit().putString("svrip", result).commit();
                }
            }else{
                if(!result.equals(serviceIp)){
                    handler.removeCallbacks(unregiestRefresher);
                    handler.removeCallbacks(regiestRefresher);
                    handler.postDelayed(unregiestRefresher,500);
                }
            }
        }
    }

    @Override
    public void onVideoRawCallback(long arg0, int arg1, int arg2, int arg3,
                                   byte[] arg4, int arg5) {
    }

    @Override
    public void onWaitingFaxMessage(String arg0, int arg1, int arg2, int arg3,
                                    int arg4) {

    }

    @Override
    public void onWaitingVoiceMessage(String arg0, int arg1, int arg2,
                                      int arg3, int arg4) {
        UserAccount userAccount = AccountManager.getDefaultUser(this);
        if(userAccount!=null) {
            AccountManager.updateMailSize(this,userAccount.getId(),arg1+arg3);
        }
    }

    @Override
    public void onDialogStateUpdated(String BLFMonitoredUri,
                                     String BLFDialogState,
                                     String BLFDialogId,
                                     String BLFDialogDirection){
        String text = "The user ";
        text += BLFMonitoredUri;
        text += " dialog state is updated: ";
        text += BLFDialogState;
        text += ", dialog id: ";
        text += BLFDialogId;
        text += ", direction: ";
        text += BLFDialogDirection;

    }

    @Override
    public void update(Observable observable, Object data) {
        int tipsId = R.string.app_offline;
        int iconid = R.drawable.offline;
        Intent intent = null;
        AccountManager accountManager = AccountManager.getInstance();
        switch (accountManager.getLoginState())
        {
            case UserAccount.STATE_NOTONLINE:
                tipsId = R.string.app_offline;
                iconid = R.drawable.offline;
                intent = new Intent(this, PortActivityLogin.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                break;
            case UserAccount.STATE_ONLINE:
                tipsId = R.string.app_online;
                iconid = R.drawable.online;
                intent = new Intent(this, PortActivityMain.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                break;
            case UserAccount.STATE_LOGIN:
                tipsId = R.string.app_login;
                iconid = R.drawable.login;
                intent = new Intent(this, PortActivityLogin.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                break;
        }
        if(observable instanceof NgnObservableList) {
            if( CallManager.getInstance().getCallsSize()>0){
                intent = new Intent(this, PortIncallActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP|Intent.FLAG_ACTIVITY_NEW_TASK);
                NotificationUtils.getInstance(this).updateAppNotification(this,iconid,appName,getString(R.string.app_incall),intent);
            }else{
                NotificationUtils.getInstance(this).cancelPendingCallNotification(this);
                NotificationUtils.getInstance(this).showAppNotification(this,iconid,appName,
                        getString(tipsId),intent);

            }
        }else if(observable instanceof AccountManager){
            NotificationUtils.getInstance(this).showAppNotification(this,iconid,appName,
                    getString(tipsId),intent);
        }
    }


    private void showAlertDialog(Context mContext,String remote,String time,String event) {
        PortActivityAlterDialog.showAutoDissmissDialog(mContext,event,time,remote);
    }

    public void setCpuRun(boolean bOn){
        PowerManager powerManager = (PowerManager)getSystemService(Context.POWER_SERVICE);
        if(bOn){ //open
            if(mCpuLock == null){
                if((mCpuLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "portgo:CpuLock")) == null){
                    return;
                }
                mCpuLock.setReferenceCounted(false);
            }

            synchronized(mCpuLock){
                if(!mCpuLock.isHeld()){
                    mCpuLock.acquire();
                }
            }
        }else{//
            if(mCpuLock != null){
                synchronized(mCpuLock){
                    if(mCpuLock.isHeld()){
                        mCpuLock.release();
                    }
                }
            }
        }
    }

    public static void startServiceCompatibility(@NonNull Context context,@NonNull Intent intent){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent);
        }else {
            context.startService(intent);
        }
    }


    final String SUBSCRIBE_ID = "subscribe_id";
    final String SUBSCRIBE_CONTACT_ID = "subscribe_contactid";
    final String SUBSCRIBE_FROM = "subscribe_from";
    final String SUBSCRIBE_DISNAME = "subscribe_disname";
    public static final String TOKEN_REFRESH = "token";

    class MyNetWorkChangerListener implements  NetworkManager.NetWorkChangeListner{
        @Override
        public void handleNetworkChangeEvent(boolean ethernet,boolean wifiConnect,boolean mobileConnect,boolean netTypeChange) {
            final ConfigurationManager configurationMgr = ConfigurationManager.getInstance();

            boolean useWifi = configurationMgr.getBooleanValue(PortSipService.this,ConfigurationManager.PRESENCE_USEWIFI
                    , getResources().getBoolean(R.bool.wifi_default));
            boolean use3G = configurationMgr.getBooleanValue(PortSipService.this,ConfigurationManager.PRESENCE_VOIP
                    , getResources().getBoolean(R.bool.prefrence_voipcall_default));

            if(ethernet||(useWifi&&wifiConnect)||(use3G&&mobileConnect)) {// net work available
                handler.removeCallbacks(unregiestRefresher);
                handler.removeCallbacks(regiestRefresher);
                retryTime = 0;

                handler.post(regiestRefresher);
            }else { // net work not available
                handler.removeCallbacks(regiestRefresher);
                handler.postDelayed(unregiestRefresher,10*1000);
            }
        }
    }
}
