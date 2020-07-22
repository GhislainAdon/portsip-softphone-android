package com.portgo.ui;

import android.app.Activity;
import android.app.Dialog;
import android.app.LoaderManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.CursorLoader;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.Loader;
import android.content.res.Configuration;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.PowerManager;
import android.provider.Settings;
import androidx.annotation.DrawableRes;
import androidx.annotation.IdRes;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.viewpager.widget.ViewPager;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.GridLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.portgo.BuildConfig;
import com.portgo.PortApplication;
import com.portgo.R;
import com.portgo.adapter.AduioDeviceAdapter;
import com.portgo.adapter.IncallViewPagerAdapter;
import com.portgo.customwidget.CustomDialog;
import com.portgo.database.DBHelperBase;
import com.portgo.manager.CallManager;
import com.portgo.manager.Contact;
import com.portgo.manager.CursorHelper;
import com.portgo.manager.HistoryAVCallEvent;
import com.portgo.manager.ConfigurationManager;
import com.portgo.manager.PermissionManager;
import com.portgo.manager.PortLoaderManager;
import com.portgo.manager.PortSipCall;
import com.portgo.manager.PortSipSdkWrapper;
import com.portgo.util.ContactQueryTask;
import com.portgo.util.NgnMediaType;
import com.portgo.util.NgnStringUtils;
import com.portgo.util.NgnUriUtils;
import com.portgo.util.NotifyData;
import com.portgo.util.UnitConversion;
import com.portgo.view.MinimunWindowUtil;
import com.portgo.view.MyGridLayout;
import com.portgo.view.PopupTipView;
import com.portgo.view.RoundedImageView;
import com.portgo.view.TextViewClock;
import com.portsip.PortSIPVideoRenderer;
import com.portsip.PortSipEnumDefine;
import com.portsip.PortSipErrorcode;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Observable;
import java.util.Observer;

import static com.portgo.BuildConfig.ENABLEVIDEO;
import static com.portgo.BuildConfig.HASSIPTAILER;
import static com.portgo.BuildConfig.HASVIDEO;
import static com.portgo.manager.PortSipCall.InviteState.TERMINATED;


public class ActivityIncallFragment extends PortBaseFragment implements CompoundButton.OnCheckedChangeListener,
        View.OnClickListener,ViewPager.OnPageChangeListener,Observer,DialogInterface.OnClickListener,
        LoaderManager.LoaderCallbacks<Cursor>, View.OnTouchListener ,PopupTipView.PopupListListener{

    PortSipSdkWrapper sdk;

    PortSipCall mSipcall;
    private PowerManager.WakeLock mWakeLock;
    private PortSIPVideoRenderer mRemoteSurfaceView = null;
    private PortSIPVideoRenderer mLocalSurfaceView = null;
    private int enum_dtmfType = PortSipEnumDefine.ENUM_DTMF_MOTHOD_INFO;
    private boolean mPlaytone = true;
    private ArrayList<View> mListViews;
    private ViewPager mPager;
    private IncallViewPagerAdapter mPagerAdapter;
    private  Handler mHandler;
    private WindowManager wm;

    private LinearLayout llRemote,mImcomingBtBar,mIncallBtBar,mDtmfBtBar,mCalloutBtBar;
    private LinearLayout bottombar_container;
    private HashMap<String,Bitmap> mAvartarCache = new HashMap<>();
    private LayoutInflater mLayoutInflater = null;
    private String mRecordFilePath = null;

    private View mSingle_Line,mMulti_Line;
    private final int HIDE_DELAY = 1000*5;

    private Runnable mHideRunable = null;
    private Runnable mUpdateViewRunable = null;

    private  List<PortSipEnumDefine.AudioDevice> mAudioDevices = new ArrayList<>();
    private PortSipEnumDefine.AudioDevice mCurrentDevice = PortSipEnumDefine.AudioDevice.EARPIECE;
    private AudioChangeReceiver mAudioChangeReceiver = new AudioChangeReceiver();
    class  AudioChangeReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            if(BuildConfig.PORT_ACTION_AUDIODEVICE.equals(intent.getAction())){

                mAudioDevices.clear();
                List list = AduioDeviceAdapter.audioDeviceSort(baseActivity.mCallMgr.getAudioDevices());
                mAudioDevices.clear();
                mAudioDevices.addAll(list);
                if(audioAdapter!=null){
                    audioAdapter.notifyDataSetChanged();
                }
                updateAudioIcon();
            }
        }
    }

    void updateAudioIcon(){
        ImageButton speaker=null;
        speaker= bottombar_container.findViewById(R.id.fragment_incall_speaker);
        PortSipEnumDefine.AudioDevice currentAudioDevice = sdk.getSelectedAudioDevice();
        if(speaker!=null) {
            AduioDeviceAdapter.setAudioIcon(speaker, currentAudioDevice, true);
        }

        speaker= bottombar_container.findViewById(R.id.fragment_callout_speaker);
        if(speaker!=null) {
            AduioDeviceAdapter.setAudioIcon(speaker, currentAudioDevice, true);
        }
    }
    AduioDeviceAdapter audioAdapter;
    Bitmap logo;

    final int LOADER_FIRSTLINE = 45538;

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        PortLoaderManager.initLoader(baseActivity,loadMgr,LOADER_FIRSTLINE,null,this);
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mHandler.removeCallbacks(mUpdateViewRunable);
        mHandler.removeCallbacks(mHideRunable);
    }

    AlterViewReceiver alterViewReceiver;
    class AlterViewReceiver extends BroadcastReceiver{

        @Override
        public void onReceive(Context context, Intent intent) {
            if(BuildConfig.PORT_ACTION_ALTERVIEW.equals(intent.getAction())){
                mHandler.post(mUpdateViewRunable);
            }
        }
    }
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                          Bundle savedInstanceState) {

        super.onCreateView(inflater, container, savedInstanceState);

        sdk = PortSipSdkWrapper.getInstance();
        logo = BitmapFactory.decodeResource(getResources(),R.drawable.head_portrait);
        popupWindow = new PopupTipView(getActivity());
        mAudioDevices.clear();
        List list = AduioDeviceAdapter.audioDeviceSort(baseActivity.mCallMgr.getAudioDevices());
        mAudioDevices.clear();
        mAudioDevices.addAll(list);
        sdk.getSelectedAudioDevice();

        audioAdapter = new AduioDeviceAdapter(getActivity(),mAudioDevices);

        actionItems = new HashMap<>();
        mListViews = new ArrayList<View>();
        mPagerAdapter = new IncallViewPagerAdapter(mListViews);

        mLayoutInflater =inflater;
        generateAllActionView(mLayoutInflater);
        View myView = inflater.inflate(R.layout.activity_fragment_dialing, null);
        mRemoteSurfaceView = myView.findViewById(R.id.remote_render);
        mRemoteSurfaceView.setScalingType(PortSIPVideoRenderer.ScalingType.SCALE_ASPECT_FIT);
        mSingle_Line = myView.findViewById(R.id.single_line);
        mMulti_Line = myView.findViewById(R.id.multi_lines);

        mSipcall = baseActivity.mCallMgr.getPendingCall();
        if(mSipcall==null) {
            mSipcall = baseActivity.mCallMgr.getActiveCall();
        }
        if(mSipcall==null){
            mSipcall = baseActivity.mCallMgr.getDefaultCall();
        }

        mHideRunable = new Runnable() {
            @Override
            public void run() {
                if(bottombar_container.findViewById(R.id.fragment_incall_bottombar)!=null||
                        bottombar_container.findViewById(R.id.view_dtmf)!=null){
                    hideDevicePopWindow();
                    if(singleVideoOrConferenceVideo()) {
                        bottombar_container.removeAllViews();
                    }
                }
            }
        };

        mUpdateViewRunable = new Runnable() {
            @Override
            public void run() {
                updateView(mSipcall);
            }
        };
        alterViewReceiver = new AlterViewReceiver();
        getActivity().registerReceiver(alterViewReceiver,new IntentFilter(BuildConfig.PORT_ACTION_ALTERVIEW));
        refreshHideRunable();
        String dirName = getString(R.string.prefrence_record_filepath_default);
        String defaultdir = baseActivity.getApplicationContext().getExternalFilesDir(dirName).getAbsolutePath();
        mRecordFilePath = baseActivity.mConfigurationService.getStringValue(getActivity(),ConfigurationManager.PRESENCE_RECORD_DIR, defaultdir);

        wm = baseActivity.getWindowManager();
        myView.setOnTouchListener(this);
        myView.findViewById(R.id.fragment_dialing_min).setOnClickListener(this);
        myView.findViewById(R.id.fragment_dialing_addcall).setOnClickListener(this);
        bottombar_container = (LinearLayout) myView.findViewById(R.id.fragment_bottombar_container);
        llRemote = (LinearLayout) myView.findViewById(R.id.fragment_dialing_remote);
        myView.findViewById(R.id.fragment_dailing_root).setOnClickListener(this);

        llRemote.setOnClickListener(this);

        if (baseActivity.mConfigurationService.getIntergerValue(getActivity(),ConfigurationManager.PRESENCE_DTMF,getResources().getInteger(R.integer.prefrence_dtmf_type_default)) == 0) {
            enum_dtmfType = PortSipEnumDefine.ENUM_DTMF_MOTHOD_RFC2833;
        } else {
            enum_dtmfType = PortSipEnumDefine.ENUM_DTMF_MOTHOD_INFO;
        }

        mPlaytone = baseActivity.mConfigurationService.getBooleanValue(getActivity(),ConfigurationManager.PRESENCE_DTMF_BACK, false);
        //////////
        getActivity().registerReceiver(mAudioChangeReceiver,new IntentFilter(BuildConfig.PORT_ACTION_AUDIODEVICE));
        return myView;
    }

    boolean inputState = false;

    private void hideDevicePopWindow(){
        if(popupWindow!=null){
            popupWindow.hidePopupListWindow();
        }
    }
    @Override
    public boolean showPopupList(View adapterView, View contextView, int contextPosition) {
        mAudioDevices.clear();
        List list = AduioDeviceAdapter.audioDeviceSort(baseActivity.mCallMgr.getAudioDevices());
        mAudioDevices.clear();
        mAudioDevices.addAll(list);
        audioAdapter.notifyDataSetChanged();

        if(mAudioDevices.size()>2) {
            return true;
        }else{
            if(mAudioDevices.size()==2) {
                PortSipEnumDefine.AudioDevice device = sdk.getSelectedAudioDevice();
                int index = mAudioDevices.indexOf(device);
                if (index == 1) {
                    device = mAudioDevices.get(0);
                } else {
                    device = mAudioDevices.get(1);
                }
                sdk.setAudioDevice(device);
            }
            return false;
        }
    }

    @Override
    public void onPopupListClick(View contextView, int contextPosition, int position) {


        if(mAudioDevices!=null&&mAudioDevices.size()>position){
            PortSipEnumDefine.AudioDevice device = mAudioDevices.get(position);
            sdk.setAudioDevice(device);
        }
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mRemoteSurfaceView.setZOrderOnTop(false);

    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        dettachLocalVideo();
        if(isResumed()) {
            baseActivity.mCallMgr.setRemoteViewID(-1,null);//
            baseActivity.mSipMgr.disablePlayLocalVideo();
            sdk.setLocalVideoWindow(null);
        }
        mHandler.removeCallbacks(mUpdateViewRunable);
        mHandler.removeCallbacks(mHideRunable);
        getActivity().unregisterReceiver(mAudioChangeReceiver);
        mShowToolBar = true;
        if (mLocalSurfaceView != null) {
            mLocalSurfaceView.release();
        }
        if (mRemoteSurfaceView != null) {
            mRemoteSurfaceView.release();
        }

        getActivity().unregisterReceiver(alterViewReceiver);
    }

    @Override
    public void onResume() {
        aquireScreenOn();
        baseActivity.mCallMgr.getObservableCalls().addObserver(this);
        mShowToolBar= false;
        update(null,null);//
        super.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
        releaseScreen();
        baseActivity.mCallMgr.getObservableCalls().deleteObserver(this);
        dettachLocalVideo();
        mHandler.removeCallbacks(mUpdateViewRunable);
        mHandler.removeCallbacks(mHideRunable);

        CallManager.getInstance().setConferenceView(null);

        baseActivity.mSipMgr.disablePlayLocalVideo();
        sdk.setLocalVideoWindow(null);

    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        mHandler = new Handler();
        super.onCreate(savedInstanceState);
    }

    WindowManager.LayoutParams wmParams = new WindowManager.LayoutParams();
    LinearLayout mFloatLayout;

    public boolean isScreenPortail(){
        return  this.getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT;
    }

    private void createFloatView()
    {
        if(!Settings.canDrawOverlays(getActivity())){
            if(!ConfigurationManager.getInstance().getBooleanValue(getActivity(),ConfigurationManager.PRESENCE_ALTER_SET,false)){
                PermissionManager.portgoRequestSpecialPermission(getActivity());
            }
            return;
        }
        if(Build.VERSION.SDK_INT>=26) {
            wmParams.type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        }else{
            wmParams.type = WindowManager.LayoutParams.TYPE_SYSTEM_ALERT;
        }
        wmParams.format = PixelFormat.RGBA_8888;
        wmParams.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;
        wmParams.gravity = Gravity.LEFT | Gravity.TOP;
        if(wmParams.x == 0)
            wmParams.x = wm.getDefaultDisplay().getWidth()-wmParams.width -10;

        wmParams.height = UnitConversion.dp2px(baseActivity,90);
        wmParams.width = UnitConversion.dp2px(baseActivity,90);

        LayoutInflater inflater = getActivity().getLayoutInflater();
        if(mFloatLayout!=null) {
            if(mSipcall!=null) {
                baseActivity.mCallMgr.setRemoteViewID(mSipcall.getSessionId(), null);//
            }
            wm.removeView(mFloatLayout); //return;
        }
        if(mLocalSurfaceView!=null){
            baseActivity.mSipMgr.disablePlayLocalVideo();
            sdk.setLocalVideoWindow(null);
            mLocalSurfaceView.release();
        }
        mFloatLayout = (LinearLayout) inflater.inflate(R.layout.view_localvideo, null);

        mLocalSurfaceView = mFloatLayout.findViewById(R.id.local_render);
        try {
            wm.addView(mFloatLayout, wmParams);
        }catch (WindowManager.BadTokenException e){

        }
        PortSipSdkWrapper.getInstance().setLocalVideoWindow(mLocalSurfaceView);
        mLocalSurfaceView.setOnTouchListener(new View.OnTouchListener()
        {

            @Override
            public boolean onTouch(View v, MotionEvent event)
            {
                if(event.getAction() == MotionEvent.ACTION_MOVE) {
                    if(mFloatLayout!=null&&wm!=null) {
                        wmParams.x = (int) event.getRawX() - mFloatLayout.getWidth() / 2;
                        wmParams.y = (int) event.getRawY() - mFloatLayout.getHeight() / 2 - 40;
                        wm.updateViewLayout(mFloatLayout, wmParams);
                    }
                }
                return true;
            }
        });

    }
    public void updateVideo() {}


    @Override
    public void update(Observable observable, Object data) {
        PortSipCall call = null;
        if(data==null){
            mHandler.post(mUpdateViewRunable);
            return;
        }
        if (data instanceof NotifyData) {//
            NotifyData notifyData = (NotifyData) data;
            Object observalData = notifyData.getObject();
            switch (notifyData.getAction()) {
                case ACTIONT_ADD:
                    call = (PortSipCall) observalData;
                    if (mSipcall == null || call.getCallId() != mSipcall.getCallId()) {
                        mSipcall = call;
                    }
                    break;
                case ACTIONT_ADD_CONNECTION:
                    break;
                case ACTIONT_REMOVE:
                    call = (PortSipCall) observalData;
                    if(call==null){
                        mSipcall=baseActivity.mCallMgr.getDefaultCall();
                    }else {
                        if (mSipcall == null || call.getCallId() == mSipcall.getCallId()) {
                            PortSipCall backCall = baseActivity.mCallMgr.getCallByIdNot(call.getSessionId());
                            if (backCall != null) {
                                mSipcall = backCall;
                            }
                        }
                    }
                    mHandler.post(mUpdateViewRunable);
                    break;
                case ACTIONT_REMOVE_CONNECTION:
                    break;
                case ACTIONT_CLEAR:
                    break;
                case ACTIONT_UPDATE:
                    mHandler.post(mUpdateViewRunable);
                    break;
            }
        }
    }

    private void refreshHideRunable(){
        mHandler.removeCallbacks(mHideRunable);
        if(bottombar_container!=null&&bottombar_container.getChildCount()==0) {
            showBootomTool();
        }
        mHandler.postDelayed(mHideRunable,HIDE_DELAY);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        dettachLocalVideo();
    }

    EditText ternsferDestInput;
    @Override
    public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
        switch (compoundButton.getId()) {
            case R.id.fragment_callout_mute:
            case R.id.fragment_incall_mute:
                if (baseActivity.mSipMgr.isConference()) {
                    baseActivity.mSipMgr.setConferenceMuteState(b);
                } else {
                    mSipcall.setMuteMicrophone(b);
                }
                break;
        }
    }

    Dialog tansferDlg=null;
    TextView dtmfMessage;
    private  void initDtmfView(View demfView){
        demfView.findViewById(R.id.fragment_dialing_digits_back).setOnClickListener(this);
        MyGridLayout digits = (MyGridLayout) demfView.findViewById(R.id.fragment_dialing_digits);
        initNumpad(digits);
        int size = digits.getChildCount();
        for (int index =0;index<size;index++) {
            digits.getChildAt(index).setOnClickListener(this);
        }
        dtmfMessage= (TextView) demfView.findViewById(R.id.fragment_dialing_digits_message);
    }

    private void initNumpad(MyGridLayout view){
        int KEY_SIZE = view.getChildCount();
        String[] numbers= getResources().getStringArray(R.array.keynumber_values);
        String[] letters= getResources().getStringArray(R.array.keyletter_values);
        for(int i=0;i<KEY_SIZE;i++){
            View diallayout = view.getChildAt(i);
            diallayout.setOnClickListener(this);
            TextView numberView = (TextView) diallayout.findViewById(R.id.keynumber);
            TextView letterView = (TextView) diallayout.findViewById(R.id.keyletter);
            numberView.setText(numbers[i]);
            letterView.setText(letters[i]);
        }
    }

    static boolean mShowToolBar = true;
    final int SELECT_TRANSFER_CONTACT = 4721;

    PopupTipView popupWindow = null;
    @Override
    public void onClick(View view) {
        refreshHideRunable();
        switch (view.getId()) {
            case R.id.transfer_dest_selector:
                Intent trasferintent = new Intent();
                trasferintent.setClass(baseActivity,PortActivityPhoneNumberSelect.class);
                baseActivity.startActivityForResult(trasferintent,SELECT_TRANSFER_CONTACT);
                break;
            case R.id.fragment_dialing_digits_back:
                mSipcall.setDtmfStatus(false);
//                InitViewPager(incallBtBar);
                showBootomTool(getInCallBtBar());
                break;
            case R.id.second_line://switch activityview
                mSipcall.hold();
                mSipcall = baseActivity.mCallMgr.getCallByIdNot(mSipcall.getSessionId());
                mSipcall.unHold();
                updateView(mSipcall);
                break;

            case R.id.fragment_dialing_addcall:
                Intent intent = new Intent(baseActivity,PortActivityMain.class);
                baseActivity.startActivity(intent);
                break;

            case R.id.fragment_dialing_min:
//                performHomeClick();
//                dettachLocalVideo();
//                MinimunWindowUtil.showPopupWindow(baseActivity);
                getActivity().finish();
                break;
            case R.id.fragment_incoming_audiocall:
                if(mSipcall!=null){
                    if(PortSipErrorcode.ECoreErrorNone==mSipcall.accept(baseActivity,null,false)) {
                        boolean defautRecord = getResources().getBoolean(R.bool.prefrence_record_default);
                        baseActivity.mCallMgr.holdAllCallExcept(mSipcall);
                        boolean record = baseActivity.mConfigurationService.getBooleanValue(getActivity(),ConfigurationManager.PRESENCE_CALLING_RECORD, defautRecord);
                        if (record == true) {
                            mSipcall.startMediaRecord(getActivity(),mRecordFilePath);
                        }
                    }
                }
                break;
            case R.id.fragment_incoming_videocall:
                if(mSipcall!=null){
                    if(PortSipErrorcode.ECoreErrorNone==mSipcall.accept(baseActivity,null,true)) {
                        baseActivity.mCallMgr.holdAllCallExcept(mSipcall);
                        boolean defautRecord = getResources().getBoolean(R.bool.prefrence_record_default);
                        boolean record = baseActivity.mConfigurationService.getBooleanValue(getActivity(),ConfigurationManager.PRESENCE_CALLING_RECORD, defautRecord);
                        if (record == true) {
                            mSipcall.startMediaRecord(getActivity(),mRecordFilePath);
                        }
                    }
                }
                break;


            case R.id.fragment_dialing_hanup:
            case R.id.fragment_incall_hanup:
                if(mSipcall!=null){
                    mSipcall.terminate(baseActivity);
                }
                break;
            case R.id.fragment_incoming_hanup:
                if(mSipcall!=null){
                    mSipcall.reject(baseActivity);
                }
                break;

            case R.id.dtmf_number0:
            case R.id.dtmf_number1:
            case R.id.dtmf_number2:
            case R.id.dtmf_number3:
            case R.id.dtmf_number4:
            case R.id.dtmf_number5:
            case R.id.dtmf_number6:
            case R.id.dtmf_number7:
            case R.id.dtmf_number8:
            case R.id.dtmf_number9:
            case R.id.dtmf_numberstar:
            case R.id.dtmf_numbersharp:
                String dtmfinfo = ((TextView)(view.findViewById(R.id.keynumber))).getText().toString();
                sendDtmf(dtmfinfo);
                break;
        }
    }

    Animation mShowAction = new TranslateAnimation(Animation.RELATIVE_TO_SELF, 0.0f,
            Animation.RELATIVE_TO_SELF, 0.0f, Animation.RELATIVE_TO_SELF,
            -1.0f, Animation.RELATIVE_TO_SELF, 0.0f);
    Animation mHiddenAction = new TranslateAnimation(Animation.RELATIVE_TO_SELF,
            0.0f, Animation.RELATIVE_TO_SELF, 0.0f,
            Animation.RELATIVE_TO_SELF, 0.0f, Animation.RELATIVE_TO_SELF,
            -1.0f);

    Animation mBottomShowAction = new TranslateAnimation(Animation.RELATIVE_TO_SELF, 0.0f,
            Animation.RELATIVE_TO_SELF, 0.0f, Animation.RELATIVE_TO_SELF,
            1.0f, Animation.RELATIVE_TO_SELF, 0.0f);
    Animation mBottomHiddenAction = new TranslateAnimation(Animation.RELATIVE_TO_SELF,
            0.0f, Animation.RELATIVE_TO_SELF, 0.0f,
            Animation.RELATIVE_TO_SELF,0.0f, Animation.RELATIVE_TO_SELF,
            1.0f);

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode){
            case SELECT_TRANSFER_CONTACT:
                if(resultCode== Activity.RESULT_OK) {
                    String select = data.getStringExtra(PortActivityPhoneNumberSelect.PHONE_NUMBER);
                    if (!NgnStringUtils.isNullOrEmpty(select)){
                        if(ternsferDestInput!=null){
                            ternsferDestInput.setText(select);
                        }
                    }
                }
                break;
        }
    }

    private void dettachLocalVideo(){
        if(mFloatLayout!=null) {
            wm.removeView(mFloatLayout);
            mFloatLayout.setOnTouchListener(null);
            mFloatLayout.removeAllViews();
            mFloatLayout = null;
        }

    }

    @Override
    public void onPageSelected(int position) {

        switch (position) {
            case 0: {
                if(getInCallBtBar()!=null){
                    ImageView view = (ImageView)getInCallBtBar().findViewById(R.id.avcall_pager_indicate_left);
                    view.setImageResource(R.drawable.indicate_current_selector);
                    view = (ImageView)getInCallBtBar().findViewById(R.id.avcall_pager_indicate_right);
                    view.setImageResource(R.drawable.indicate_default_selector);
                }
            }
            break;
            case 1: {
                if(getInCallBtBar()!=null){
                    ImageView view =  (ImageView)getInCallBtBar().findViewById(R.id.avcall_pager_indicate_left);
                    view.setImageResource(R.drawable.indicate_default_selector);

                    view =(ImageView)getInCallBtBar().findViewById(R.id.avcall_pager_indicate_right);
                    view.setImageResource(R.drawable.indicate_current_selector);
                }
                break;
            }
        }

    }

    @Override
    public void onPageScrollStateChanged(int state) {

    }

    @Override
    public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

    }

    void sendDtmf(String dtmfInfo) {
        if(TextUtils.isEmpty(dtmfInfo))
            return;
        if(baseActivity.mSipMgr.isConference()){
            baseActivity.mCallMgr.sendDtmfConference(enum_dtmfType, dtmfInfo.charAt(0),mPlaytone);
            if (dtmfMessage != null) {
                dtmfMessage.append(dtmfInfo);
            }
        }else {
            if (mSipcall != null)
                mSipcall.sendDTMF(enum_dtmfType, dtmfInfo.charAt(0),mPlaytone);
            if (dtmfMessage != null) {
                dtmfMessage.append(dtmfInfo);
            }
        }
    }

    private void releaseScreen(){
        if(mWakeLock != null && mWakeLock.isHeld()){
            mWakeLock.release();
        }
    }

    private void aquireScreenOn(){
        final PowerManager powerManager = (PowerManager) getActivity().getSystemService(Context.POWER_SERVICE);
        if(powerManager != null && mWakeLock == null){
            mWakeLock = powerManager.newWakeLock(PowerManager.ON_AFTER_RELEASE
                            | PowerManager.SCREEN_BRIGHT_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP
                    , this.getClass().getCanonicalName());
        }

        if(mWakeLock != null){
            if(baseActivity.mSipMgr.isConference()||(mSipcall!=null&&mSipcall.isVideoNegotiateSucess())) {
                if(!mWakeLock.isHeld())
                    mWakeLock.acquire();
            }else{
                if(mWakeLock.isHeld())
                    mWakeLock.release();
            }
        }
    }


    private void showBootomTool(){
        if(mSipcall!=null){
            switch (mSipcall.getState()){
                case INCALL:
                case EARLY_MEDIA:
                    if(mSipcall.isDtmfStatus()) {
                        showBootomTool(getDtmfBtBar());
                    }else{
                        showBootomTool(getInCallBtBar());
                    }
                    break;
                case INPROGRESS://等待对方接听的过程中
                case REMOTE_RINGING:
                    showBootomTool(getCalloutBtBar());
                    break;
                case INCOMING:
                    showBootomTool(getIncomingBar());
                    break;
                case TERMINATING:
                    break;
            }
        }
    }

    private void showBootomTool(LinearLayout content) {
        hideDevicePopWindow();
        if (inputState) {
            return;
        }
        if (content.getParent() != null) {
            ((ViewGroup) content.getParent()).removeAllViews();
        }
        bottombar_container.removeAllViews();

        LinearLayout.LayoutParams params;

        if (content.findViewById(R.id.fragment_dialing_digits_back) != null) {//判断是不是dtmfbar，用这种方法判断，可以避免在不需要的时候装载dtmfbar
            MyGridLayout digits = (MyGridLayout) content.findViewById(R.id.fragment_dialing_digits);
            if(isScreenPortail()) {
                params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                digits.setLayoutStandard(MyGridLayout.LAYOUTSTARD.PORT);
                digits.setLayoutParams(params);
            }else{
                params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.MATCH_PARENT);
                params.gravity=Gravity.LEFT;

                digits.setLayoutStandard(MyGridLayout.LAYOUTSTARD.LAND);
                digits.setLayoutParams(params);
            }
        } else {
            params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        }

        bottombar_container.addView(content,params);

    }

    private  Rect getViewHeight(View view ){
        int height =View.MeasureSpec.makeMeasureSpec(0,View.MeasureSpec.UNSPECIFIED);
        int width =View.MeasureSpec.makeMeasureSpec(0,View.MeasureSpec.UNSPECIFIED);
        view.measure(width,height);
        return new Rect(0,0,view.getMeasuredWidth(),view.getMeasuredHeight());
    }

    final  int maxWord = 16;
    private void updateView(PortSipCall call){
        if(call==null)
            return;
        if(this.isResumed()==false){
            mHandler.postDelayed(mUpdateViewRunable,200);
            return;
        }

        int callsize = baseActivity.mCallMgr.getCallsSize();
        switch (call.getState()) {
            case INCALL:
            case EARLY_MEDIA:
                if (singleVideoOrConferenceVideo()) {
                    relayoutStatus(true);
                } else {
                    relayoutStatus(false);
                }

                getView().findViewById(R.id.fragment_dialing_min).setEnabled(true);
                getView().findViewById(R.id.fragment_dialing_addcall).setEnabled(true);
                if (singleVideoOrConferenceVideo()&&HASVIDEO&&ENABLEVIDEO) {
                    dettachLocalVideo();
                    if (videoConference()) {
                        baseActivity.mCallMgr.setConferenceView(mRemoteSurfaceView);

                        baseActivity.mSipMgr.disPlayLocalVideo();
                    } else {
                        if (mSipcall.isSendVideo()) {
                            createFloatView();
                            baseActivity.mSipMgr.disPlayLocalVideo();
                            baseActivity.mCallMgr.setRemoteViewID(call.getSessionId(), mRemoteSurfaceView);
                            mSipcall.setSendVideo(mSipcall.isSendVideo(),false);
                        } else {
                            baseActivity.mSipMgr.disablePlayLocalVideo();
                        }

                    }
                    llRemote.setVisibility(View.VISIBLE);
                    updateActions(ViewPagerMode.VIDEO_MODE);
                    //
                } else {
                    updateActions(ViewPagerMode.AUDIO_MODE);
                    llRemote.setVisibility(View.INVISIBLE);
                    if (mFloatLayout != null) {
                        wm.removeView(mFloatLayout);
                        mFloatLayout.removeAllViews();
                        mFloatLayout = null;
                    }

                }
                if (callsize > 1) {
                    mMulti_Line.setVisibility(View.VISIBLE);
                    mSingle_Line.setVisibility(View.GONE);
                    getView().findViewById(R.id.fragment_dialing_avatar).setVisibility(View.GONE);

                    TextViewClock clock = (TextViewClock) mMulti_Line.findViewById(R.id.first_line_connecttime);
                    Calendar calendar = null;
                    String disName = "";
                    PortSipCall secondcall = null;
                    if (mSipcall != null) {
                        calendar = mSipcall.getCallTime();
                        disName = mSipcall.getRemoteDisName();
                        secondcall = baseActivity.mCallMgr.getCallByIdNot(mSipcall.getSessionId());
                    }
                    clock.setCurrentTime(calendar);
                    clock.setVisibility(View.VISIBLE);

                    TextView textView = (TextView) mMulti_Line.findViewById(R.id.first_line_remote);
                    textView.setText(disName.length() > maxWord ? disName.substring(0, maxWord) : disName);

                    if (secondcall != null) {
                        mMulti_Line.findViewById(R.id.second_line).setOnClickListener(this);
                        clock = (TextViewClock) mMulti_Line.findViewById(R.id.second_line_connecttime);
                        calendar = secondcall.getCallTime();
                        disName = secondcall.getRemoteDisName();
                    }
                    clock.setCurrentTime(calendar);
                    clock.setVisibility(View.VISIBLE);
                    textView = (TextView) mMulti_Line.findViewById(R.id.second_line_remote);
                    textView.setText(disName.length() > maxWord ? disName.substring(0, maxWord) : disName);

                } else {
                    mMulti_Line.setVisibility(View.GONE);
                    mSingle_Line.setVisibility(View.VISIBLE);
                    if (mSipcall == null) {
                        return;
                    }
                    TextViewClock clock = (TextViewClock) getView().findViewById(R.id.fragment_dialing_time);
                    Calendar calendar = mSipcall.getCallTime();
                    clock.setCurrentTime(calendar);

                    if (videoSingle()) {
                        updateStatus(false, false, true, false,
                                false, true, mSipcall.isRemoteHold() ? getString(R.string.remote_hold) : null);
                    } else {
                        updateStatus(false, true, true, true,
                                false, true, mSipcall.isRemoteHold() ? getString(R.string.remote_hold) : null);
                    }

                }
                if (mSipcall.isDtmfStatus()) {
                    showBootomTool(getDtmfBtBar());
                } else {
                    showBootomTool(getInCallBtBar());
                }

                break;
            case INPROGRESS://
            case REMOTE_RINGING:
            case TRIING:
                relayoutStatus(false);
//                if(!isScreenPortail()){
//                    getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
//                    break;
//                }

                startLoaderForContact(call);
                getView().findViewById(R.id.fragment_dialing_min).setEnabled(false);
                getView().findViewById(R.id.fragment_dialing_addcall).setEnabled(false);

                if (call.isVideoCall()) {
                    baseActivity.mCallMgr.setRemoteViewID(call.getSessionId(),null);
                    baseActivity.mSipMgr.disPlayLocalVideo();
                    updateStatus(false, false, true, true, true, false, getString(R.string.tring));
                } else {
                    llRemote.setVisibility(View.INVISIBLE);

                    updateStatus(false, true, true, true, true, false, getString(R.string.tring));
                }


                showBootomTool(getCalloutBtBar());
                break;

            case INCOMING:
                relayoutStatus(false);

                if (call != null) {
                    startLoaderForContact(call);
                    CallManager.getInstance().setRemoteViewID(call.getSessionId(), null);
                    llRemote.setVisibility(View.INVISIBLE);
                }

                getView().findViewById(R.id.fragment_dialing_min).setEnabled(false);
                getView().findViewById(R.id.fragment_dialing_addcall).setEnabled(false);

                if (mFloatLayout != null && mFloatLayout.getParent() != null) {
                    wm.removeView(mFloatLayout);
                }

                updateStatus(false, true, true, true, true, false, getString(R.string.tring));
                if (mSipcall.isVideoNegotiateSucess()&&BuildConfig.HASVIDEO) {
                    getIncomingBar().findViewById(R.id.fragment_incoming_videocall).setVisibility(View.VISIBLE);
                } else {
                    getIncomingBar().findViewById(R.id.fragment_incoming_videocall).setVisibility(View.GONE);
                }
                showBootomTool(getIncomingBar());
                break;
            case TERMINATED:
                relayoutStatus(false);
                PortSipCall transferdCall = baseActivity.mCallMgr.getTrnsferCall();
                if (transferdCall != null) {
                    if (transferdCall.getState() == TERMINATED) {
                        ternsferDestInput = null;
                        if (tansferDlg != null) {
                            tansferDlg.dismiss();
                        }
                    }
                }

                baseActivity.mSipMgr.disablePlayLocalVideo();
                break;
        }
    }

    void relayoutStatus(boolean videoSatus){


        View callStatus = getView().findViewById(R.id.fragment_dialing_callstatus);
        RelativeLayout.LayoutParams params=null;
        LinearLayout.LayoutParams childParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,ViewGroup.LayoutParams.WRAP_CONTENT);

        if(videoSatus){
            params = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,ViewGroup.LayoutParams.WRAP_CONTENT);
            params.addRule(RelativeLayout.BELOW,R.id.fragment_dialing_min);
            params.addRule(RelativeLayout.ALIGN_PARENT_LEFT);

            getView().findViewById(R.id.fragment_dialing_avatar).setVisibility(View.GONE);

            childParams.gravity=Gravity.LEFT;
            params.setMargins((int) getResources().getDimension(R.dimen.fragment_marginLeft),0,0,0);
            callStatus.findViewById(R.id.fragment_dialing_name).setLayoutParams(childParams);
            params.setMargins((int) getResources().getDimension(R.dimen.fragment_marginLeft),(int) getResources().getDimension(R.dimen.fragment_dialing_number_marginTop),0,0);
            callStatus.findViewById(R.id.fragment_dialing_number).setLayoutParams(childParams);
            params.setMargins((int) getResources().getDimension(R.dimen.fragment_marginLeft),(int) getResources().getDimension(R.dimen.fragment_dialing_time_marginTop),0,0);
            callStatus.findViewById(R.id.fragment_dialing_time).setLayoutParams(childParams);
            params.setMargins((int) getResources().getDimension(R.dimen.fragment_marginLeft),(int) getResources().getDimension(R.dimen.fragment_dialing_stage_marginTop),0,0);
            callStatus.findViewById(R.id.fragment_dialing_stage).setLayoutParams(childParams);

        }else {
            params = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,ViewGroup.LayoutParams.WRAP_CONTENT);
            params.addRule(RelativeLayout.CENTER_IN_PARENT);
            getView().findViewById(R.id.fragment_dialing_avatar).setVisibility(View.VISIBLE);

            childParams.gravity=Gravity.CENTER;
            callStatus.findViewById(R.id.fragment_dialing_name).setLayoutParams(childParams);
            params.setMargins(0,(int) getResources().getDimension(R.dimen.fragment_dialing_number_marginTop),0,0);
            callStatus.findViewById(R.id.fragment_dialing_number).setLayoutParams(childParams);
            params.setMargins(0,(int) getResources().getDimension(R.dimen.fragment_dialing_time_marginTop),0,0);
            callStatus.findViewById(R.id.fragment_dialing_time).setLayoutParams(childParams);
            params.setMargins(0,(int) getResources().getDimension(R.dimen.fragment_dialing_stage_marginTop),0,0);
            callStatus.findViewById(R.id.fragment_dialing_stage).setLayoutParams(childParams);
        }

        callStatus.setLayoutParams(params);
    }

    /**
     * @param hideAll
     * @param showAvart
     * @param showName
     * @param showRemote
     * @param showStage
     * @param showtime
     * @param stagePrompt
     */
    void updateStatus(boolean hideAll,boolean showAvart,
                                   boolean showName,boolean showRemote,
                                   boolean showStage,boolean showtime,String stagePrompt){
        String disName = null;
        HistoryAVCallEvent event = mSipcall.getHistoryEvent();
        Contact contact = mSipcall.getAttachContact();
        if(hideAll){
            getView().findViewById(R.id.fragment_dialing_callstatus).setVisibility(View.INVISIBLE);
            getView().findViewById(R.id.fragment_dialing_avatar).setVisibility(View.INVISIBLE);
            return;
        }
        disName= event.getDisplayName();
        if(contact!=null&&contact.getId()!=Contact.INVALIDE_ID) {
            disName = contact.getDisplayName();
        }
        if(showAvart) {
            getView().findViewById(R.id.fragment_dialing_avatar).setVisibility(View.VISIBLE);
            RoundedImageView avatarImg = (RoundedImageView) getView().findViewById(R.id.user_avatar_image);
            TextView avatarText = (TextView) getView().findViewById(R.id.user_avatar_text);

            Bitmap bitmap = null;
            if(contact!=null&&contact.getId()!=Contact.INVALIDE_ID) {
                int photoId = contact.getContactAvatarId();
                if(photoId>0) {
                    String key =""+contact.getId()+""+photoId;
                    Object ob = mAvartarCache.get(key);
                    if(ob==null) {
                        bitmap = contact.getAvatar();
                        mAvartarCache.put(key,bitmap);
                    }else{
                        bitmap = (Bitmap) ob;
                    }
                }
            }
            float textsize = (int) getResources().getDimension(R.dimen.fragment_dailing_useravatar_textsize);

            avatarText.setTextSize(UnitConversion.px2sp(baseActivity,textsize));

            if(bitmap!=null||contact==null) {
                if(contact==null){
                    bitmap = logo;
                }
                avatarImg.setImageBitmap(bitmap);
                avatarImg.setVisibility(View.VISIBLE);
                avatarText.setVisibility(View.GONE);
            }else{
                avatarImg.setVisibility(View.GONE);
                avatarText.setVisibility(View.VISIBLE);
                avatarText.setText(NgnStringUtils.getAvatarText(disName));
            }
        }else{
            getView().findViewById(R.id.fragment_dialing_avatar).setVisibility(View.INVISIBLE);
        }
        getView().findViewById(R.id.fragment_dialing_callstatus).setVisibility(View.VISIBLE);

        TextView remoteDidName = (TextView) getView().findViewById(R.id.fragment_dialing_name);
        if(showName) {
            remoteDidName.setVisibility(View.VISIBLE);
            remoteDidName.setText(disName);
        }else{
            remoteDidName.setVisibility(View.INVISIBLE);
        }

        TextView remoteNumber = (TextView) getView().findViewById(R.id.fragment_dialing_number);
        if(showRemote) {
            remoteNumber.setVisibility(View.VISIBLE);
            if(!HASSIPTAILER){
                remoteNumber.setText(NgnUriUtils.getUserName(event.getRemoteUri()));
            }else{
                remoteNumber.setText(event.getRemoteUri());
            }

        }else{
            remoteNumber.setVisibility(View.GONE);
        }
        TextView callStage = (TextView) getView().findViewById(R.id.fragment_dialing_stage);
        if(showStage) {
            if(TextUtils.isEmpty(stagePrompt))
                stagePrompt="";
            callStage.setText(stagePrompt);
            callStage.setVisibility(View.VISIBLE);
        }else{
            if(NgnStringUtils.isNullOrEmpty(stagePrompt)) {
                callStage.setVisibility(View.GONE);
            }else{
                callStage.setText(stagePrompt);
                callStage.setVisibility(View.VISIBLE);
            }
        }
        TextViewClock clock = (TextViewClock) getView().findViewById(R.id.fragment_dialing_time);
        if(showtime) {
            Calendar calendar =mSipcall.getCallTime();
            clock.setCurrentTime(calendar);
            clock.setVisibility(View.VISIBLE);
        }else {
            clock.setVisibility(View.GONE);
        }
    }

    private void updateViewpageItem(View item, @DrawableRes int drawable, @StringRes int description){
        ImageView imageView = (ImageView) item.findViewById(R.id.viewpager_item_image);
        imageView.setImageResource(drawable);
        TextView title = (TextView) item.findViewById(R.id.viewpager_item_title);
        title.setText(description);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        LoadCallId =-1;
        int mRemoteId  =-1;
        String where = DBHelperBase.RemoteColumns._ID + "=?";
        if(bundle!=null) {
            LoadCallId = bundle.getInt("CALLID",-1);
            PortSipCall call = baseActivity.mCallMgr.getCallByCallId( LoadCallId);
            if (call != null) {
                mRemoteId = call.getHistoryEvent().getRemoteID();
            }
        }
        String[] args = new String[]{"" + mRemoteId};
        return new CursorLoader(baseActivity, DBHelperBase.RemoteColumns.CONTENT_URI, null, where, args, null);
    }
    int LoadCallId =0;
    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        int contactId=-1;
        int remoteID = 0;
        PortSipCall call = baseActivity.mCallMgr.getCallByCallId(LoadCallId);

        if(call!=null) {
            String remoteUri = null;
            while (CursorHelper.moveCursorToFirst(cursor)) {
                contactId = cursor.getInt(cursor.getColumnIndex(DBHelperBase.RemoteColumns.REMOTE_CONTACT_ID));
                remoteID = cursor.getInt(cursor.getColumnIndex(DBHelperBase.RemoteColumns._ID));
                remoteUri = cursor.getString(cursor.getColumnIndex(DBHelperBase.RemoteColumns.REMOTE_URI));
                break;
            }

            Bitmap remote = null;
            Contact contact = null;
            if (contactId > 0) {
                contact = baseActivity.mContactMgr.getObservableContacts().get(contactId);
            }else{
                if(remoteID>=0&&!TextUtils.isEmpty(remoteUri)) {
                    new ContactQueryTask().execute(baseActivity, remoteID, remoteUri);
                }
            }
            call.setAttactContact(contact);
            call.notifyObserverUpdate();
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {

    }

    @Override
    public boolean onTouch(View view, MotionEvent motionEvent) {
        refreshHideRunable();
        switch (view.getId()) {
            case R.id.vPager:
                if (motionEvent.getAction() == MotionEvent.ACTION_MOVE && !isScreenPortail()) {
                    return true;
                }
                break;
            default:
                if (motionEvent.getAction() == MotionEvent.ACTION_UP) {
                    mHandler.post(mUpdateViewRunable);
                }
                break;
        }
        return false;
    }

    enum ViewPagerMode{
        AUDIO_MODE,
        VIDEO_MODE,
        CONFERENCE_MODE,
        MODE_NONE
    }

    HashMap<Integer,View> actionItems;
    ViewPagerMode viewPagerMode = ViewPagerMode.MODE_NONE;

    int PORTRAL_SCREEN_ITEMS =4;
    private void updateActions(ViewPagerMode mode){
        getInCallBtBar();
        int clumIndex = 0;
        boolean screenPort = isScreenPortail();
        firstPager.removeAllViews();
        secondPager.removeAllViews();

        firstPager.setRowCount(1);
        if(HASVIDEO) {
            if(mode == ViewPagerMode.AUDIO_MODE&&!screenPort){
                inflateGridView(screenPort, 0, clumIndex++, new TextView(baseActivity));
            }
            inflateGridView(screenPort,0, clumIndex++, getActionItem(R.id.action_audiovideo));
        }
        inflateGridView(screenPort,0,clumIndex++,getActionItem(R.id.action_digits));
        if(HASVIDEO&&mode == ViewPagerMode.VIDEO_MODE) {
            inflateGridView( screenPort,0, clumIndex++, getActionItem(R.id.action_closecamera));
            inflateGridView(screenPort,0, clumIndex++, getActionItem(R.id.action_switchcamera));
        }

        inflateGridView(screenPort,0,clumIndex++,getActionItem(R.id.action_add_conference));


        inflateGridView(screenPort,0,clumIndex++,getActionItem(R.id.action_hold));
        inflateGridView(screenPort,0,clumIndex++,getActionItem(R.id.action_record));
        inflateGridView(screenPort,0,clumIndex++,getActionItem(R.id.action_transfer));


        mListViews.clear();
        getInCallBtBar().findViewById(R.id.avcall_pager_indicate).setVisibility(View.INVISIBLE);
        if(screenPort) {
            firstPager.setColumnCount(PORTRAL_SCREEN_ITEMS);
            secondPager.setColumnCount(PORTRAL_SCREEN_ITEMS);
            mListViews.add(firstPager);
            secondPager.getChildCount();
            int childCount = secondPager.getChildCount();
            if (childCount > 0){
                int fills = (PORTRAL_SCREEN_ITEMS-childCount);
                for(int i=0;i<fills;i++){
                    inflateGridView(screenPort,0,PORTRAL_SCREEN_ITEMS+childCount+i,new TextView(baseActivity));
                }
                getInCallBtBar().findViewById(R.id.avcall_pager_indicate).setVisibility(View.VISIBLE);
                mListViews.add(secondPager);
            }
        }else{
            if(HASVIDEO&&mode == ViewPagerMode.AUDIO_MODE) {
                inflateGridView(screenPort,0,clumIndex++,new TextView(baseActivity));
            }
            firstPager.setColumnCount(clumIndex);
            mListViews.add(firstPager);
        }

        viewPagerMode = mode;
        mPagerAdapter.notifyDataSetChanged();
        if (mSipcall != null) {

            CheckBox box = (CheckBox) mIncallBtBar.findViewById(R.id.fragment_incall_mute);
            box.setChecked(mSipcall.isMicMute());
            ImageButton speaker = (ImageButton) mIncallBtBar.findViewById(R.id.fragment_incall_speaker);
            AduioDeviceAdapter.setAudioIcon(speaker,sdk.getSelectedAudioDevice(),true);
            popupWindow.bindViewAndClick(speaker,audioAdapter,this);

            updateAudioVideoAction(getActionItem(R.id.action_audiovideo), !singleVideoOrConferenceVideo());
            updateHoldAction(getActionItem(R.id.action_hold), mSipcall.isLocalHeld());
            updateRecodeAction(getActionItem(R.id.action_record), mSipcall.isMediaRecord());

            if (baseActivity.mSipMgr.isConference()) {
                updateConferenceAction(getActionItem(R.id.action_add_conference), false, false, true);
            } else if (baseActivity.mCallMgr.getCallsSize() > 1) {
                updateConferenceAction(getActionItem(R.id.action_add_conference), false, true, false);
            } else {
                updateConferenceAction(getActionItem(R.id.action_add_conference), true, false, false);
            }


            if (getActionItem(R.id.action_switchcamera).getVisibility() == View.VISIBLE) {
                updateViewpageItem(getActionItem(R.id.action_switchcamera)
                        , R.drawable.calling_switchcamera_selector, R.string.switch_camera);
                updateCloseVideoAction(getActionItem(R.id.action_closecamera), mSipcall.isSendVideo());
            } else {
                if (singleVideoOrConferenceVideo()) {
                    updateCloseVideoAction(getActionItem(R.id.action_closecamera), mSipcall.isSendVideo());
                }
            }

            if (baseActivity.mSipMgr.isConference()) {
                getActionItem(R.id.action_audiovideo).findViewById(R.id.viewpager_item_image).setEnabled(false);
            } else {
                getActionItem(R.id.action_audiovideo).findViewById(R.id.viewpager_item_image).setEnabled(ENABLEVIDEO);
            }

            updateViewpageItem(getActionItem(R.id.action_digits)
                    , R.drawable.calling_dtmf_selector, R.string.show_dialpad);
            updateViewpageItem(getActionItem(R.id.action_transfer)
                    , R.drawable.calling_transfer_selector, R.string.transfer_call);
        }
    }

    /***
     * @return
     */
    boolean singleVideoOrConferenceVideo(){
        return videoSingle()||videoConference();
    }

    boolean videoSingle(){
        return !baseActivity.mSipMgr.isConference()&&mSipcall!=null&&mSipcall.isVideoCall()&&mSipcall.isVideoNegotiateSucess();
    }
    boolean videoConference(){
        return baseActivity.mSipMgr.isConference()&&!baseActivity.mSipMgr.isAudioConference();
    }

    private void setActionContent(View view,int imageRes,int promptRes){
        CheckBox box = (CheckBox) view;
        box.setText(promptRes);
        box.setCompoundDrawablesWithIntrinsicBounds(null,baseActivity.getResources().getDrawable(imageRes),null,null);
    }

    GridLayout firstPager,secondPager;
    private void InitViewPager(View parent) {
        if(mPager==null) {
            mPager = ((ViewPager) parent.findViewById(R.id.vPager));
            mPager.setOnTouchListener(this);
            parent.findViewById(R.id.fragment_incall_hanup).setOnClickListener(this);
            CheckBox box = (CheckBox) parent.findViewById(R.id.fragment_incall_mute);
            box.setOnCheckedChangeListener(this);
            ImageButton speaker = parent.findViewById(R.id.fragment_incall_speaker);
            speaker.setOnClickListener(this);
            AduioDeviceAdapter.setAudioIcon(speaker,mCurrentDevice,true);

            mPager.setAdapter(mPagerAdapter);
            firstPager = (GridLayout) LayoutInflater.from(baseActivity).inflate(R.layout.viewpager, null);
            secondPager = (GridLayout) LayoutInflater.from(baseActivity).inflate(R.layout.viewpager, null);

            mListViews.clear();
            mListViews.add(firstPager);
            mListViews.add(secondPager);

            mPager.setOnPageChangeListener(this);
            mPager.setCurrentItem(0);
        }
    }


    void updateHoldAction(View item,boolean hold){
        if(hold){
            updateViewpageItem(item,R.drawable.calling_resume_selector,R.string.resume_call);
        }else{
            updateViewpageItem(item,R.drawable.calling_hold_selector,R.string.hold_call);
        }
    }

    void updateAudioVideoAction(View item,boolean audio){
        if(audio){
            updateViewpageItem(item,R.drawable.calling_video_selector,R.string.changeto_video);
        }else{
            updateViewpageItem(item,R.drawable.calling_audio_selector,R.string.changeto_audio);
        }
    }

    void updateCloseVideoAction(View item,boolean close){
        if(close){
            updateViewpageItem(item,R.drawable.calling_closecamera_selector,R.string.close_camera);
        }else{
            updateViewpageItem(item,R.drawable.calling_opencamera_selector,R.string.open_camera);
        }
    }

    void updateRecodeAction(View item,boolean record){
        if(record){
            updateViewpageItem(item,R.drawable.calling_record_stop_selector,R.string.stop_record);
        }else{
            updateViewpageItem(item,R.drawable.calling_record_start_selector,R.string.start_record);
        }
    }

    void updateConferenceAction(View item,boolean add,boolean createConference,boolean destroyConference){
        if(add){
            updateViewpageItem(item,R.drawable.calling_add_selector,R.string.add_call);
        }else if(createConference){
            updateViewpageItem(item,R.drawable.calling_confrence_selector,R.string.create_conference);
        }else if(destroyConference) {
            updateViewpageItem(item,R.drawable.calling_unconfrence_selector, R.string.destory_conference);
        }
    }
    @Override
    public void onClick(DialogInterface dialogInterface, int i) {
        switch (i){
            case DialogInterface.BUTTON_NEGATIVE:
                ternsferDestInput = null;
                break;
            case DialogInterface.BUTTON_POSITIVE:
                ternsferDestInput = null;
                break;
            case DialogInterface.BUTTON_NEUTRAL:
                break;
        }
    }


    class  ViewPagerItemClickListener implements View.OnClickListener{
        @Override
        public void onClick(View view) {

                refreshHideRunable();
                View parent = (View) view.getParent();
                switch (parent.getId()) {
                    case R.id.action_add_conference:
                        if (baseActivity.mSipMgr.isConference()) {
                            PortIncallActivity activity = (PortIncallActivity) getActivity();
                            baseActivity.mSipMgr.destroyConfrence();
                            baseActivity.mCallMgr.holdAllCallExcept(mSipcall);
                            updateConferenceAction(parent, false, true, false);
                            getActionItem(R.id.action_audiovideo).
                                    findViewById(R.id.viewpager_item_image).setEnabled(true);
                            llRemote.setVisibility(View.INVISIBLE);
                            updateView(mSipcall);
                        } else if (baseActivity.mCallMgr.getCallsSize() > 1) {

                            PortIncallActivity activity = (PortIncallActivity) getActivity();
                            if (mSipcall.isVideoCall() && mSipcall.isVideoNegotiateSucess()) {
                                baseActivity.mCallMgr.setRemoteViewID(-1,null);
                                baseActivity.mSipMgr.createConfrence(mRemoteSurfaceView, 352, 288);
                            } else {
                                baseActivity.mSipMgr.createAudioConfrence();
                            }

                            baseActivity.mCallMgr.addAllCallsToConfrence();
                            updateConferenceAction(parent, false, false, true);
                            updateView(mSipcall);

                        } else {
                            getActionItem(R.id.action_audiovideo).
                                    findViewById(R.id.viewpager_item_image).setEnabled(ENABLEVIDEO);
                            updateConferenceAction(parent, true, false, false);
                            if (baseActivity.mCallMgr.setAddCall(true)) {
                                Intent intent = new Intent(getActivity(), PortActivityMain.class);
                                startActivity(intent);
                                getActivity().finish();
                                break;
                            }

                        }

                        break;
                    case R.id.action_hold:
                        if (baseActivity.mSipMgr.isConference()) {

                            if (baseActivity.mSipMgr.isConferenceHold()) {
                                baseActivity.mCallMgr.unholdAllCallExcept(null);
                                baseActivity.mSipMgr.setConferenceHold(false);
                            } else {
                                baseActivity.mSipMgr.setConferenceHold(true);
                                baseActivity.mCallMgr.holdAllCallExcept(null);
                            }
                        } else {
                            if (mSipcall.isLocalHeld()) {
                                mSipcall.unHold();
                                updateHoldAction(parent, false);
                            } else {
                                mSipcall.hold();
                                updateHoldAction(parent, true);
                            }
                        }
                        break;
                    case R.id.action_switchcamera:
                        baseActivity.mSipMgr.disPlayLocalVideo(!baseActivity.mSipMgr.isForntCamera());
                        break;
                    case R.id.action_closecamera:
                        if ((!baseActivity.mSipMgr.isConference() && mSipcall.isVideoCall() && mSipcall.isVideoNegotiateSucess())
                                || (baseActivity.mSipMgr.isConference() && !baseActivity.mSipMgr.isAudioConference())) {
                            if (mSipcall.isSendVideo()) {
                                baseActivity.mSipMgr.disablePlayLocalVideo();
                                mSipcall.setSendVideo(false,true);
                                dettachLocalVideo();
                                updateCloseVideoAction(parent, false);
                            } else {
                                createFloatView();

                                mSipcall.setSendVideo(true,true);
//                            sdk.displayLocalVideo(true);
                                baseActivity.mSipMgr.disPlayLocalVideo();
                                updateCloseVideoAction(parent, true);
                            }

                        }
                        break;
                    case R.id.action_digits:
                        showBootomTool(getDtmfBtBar());
                        mSipcall.setDtmfStatus(true);
                        break;
                    case R.id.action_record:
                        if (mSipcall.isMediaRecord()) {
                            mSipcall.stopMediaRecord();
                            updateRecodeAction(parent, false);

                        } else {
                            String dirName = getString(R.string.prefrence_record_filepath_default);
                            String defaultdir = baseActivity.getApplicationContext().getExternalFilesDir(dirName).getAbsolutePath();
                            String dir = baseActivity.mConfigurationService.getStringValue(getActivity(), ConfigurationManager.PRESENCE_RECORD_DIR, defaultdir);
                            mSipcall.startMediaRecord(getActivity(), dir);
                            updateRecodeAction(parent, true);
                        }

                        break;
                    case R.id.action_transfer:
                        baseActivity.mCallMgr.setTrnsferCall(mSipcall);
                        if (baseActivity.mCallMgr.setAddCall(false)) {

                            if (tansferDlg != null && tansferDlg.isShowing()) {
                                tansferDlg.dismiss();
                            }
                            tansferDlg = CustomDialog.showTransferDialog(getActivity(), transferDialogListener);
                            ternsferDestInput = (EditText) tansferDlg.findViewById(R.id.transfer_dest_input);

                        }
                        break;
                    case R.id.action_audiovideo:
                        if (mSipcall.isVideoCall() && mSipcall.isVideoNegotiateSucess()) {
                            int result = sdk.updateCall(mSipcall.getSessionId(), true, false);
                            if (result == PortSipErrorcode.ECoreErrorNone) {
                                mSipcall.setSendVideo(false,false);
                                mSipcall.setVideoNegotiateResult(false);
                                baseActivity.mSipMgr.disablePlayLocalVideo();
                                mSipcall.portSipUpdate(getActivity(), NgnMediaType.Audio.ordinal(), mRecordFilePath);
                            }
                        } else {
                            int result = sdk.updateCall(mSipcall.getSessionId(), true, true);
                            if (result == PortSipErrorcode.ECoreErrorNone) {
                                mSipcall.setSendVideo(true,false);
                                baseActivity.mSipMgr.disPlayLocalVideo();
                                mSipcall.setVideoNegotiateResult(true);

                                if (mSipcall != null && mSipcall.isSendVideo() && mSipcall.isVideoCall()) {
                                    mSipcall.setSendVideo(true,false);
                                }
                                mSipcall.portSipUpdate(getActivity(), NgnMediaType.AudioVideo.ordinal(), mRecordFilePath);
                            }
                        }
                        break;
                }
            }
    }

    TransferDialogListener transferDialogListener = new TransferDialogListener();
    class  TransferDialogListener implements  View.OnClickListener{

        @Override
        public void onClick(View view) {
            switch (view.getId()) {
                case R.id.transfer_dest_selector:
                    Intent trasferintent = new Intent();
                    trasferintent.setClass(baseActivity, PortActivityPhoneNumberSelect.class);
                    baseActivity.startActivityForResult(trasferintent, SELECT_TRANSFER_CONTACT);
                    break;
                case R.id.transfer_ok:
                    String referTo = "";
                    if(ternsferDestInput!=null){
                        referTo = ternsferDestInput.getEditableText().toString();
                        if(!NgnUriUtils.isValidUriString(referTo)){
                            Toast.makeText(getActivity(),R.string.please_inputorselect_correct_dest,Toast.LENGTH_LONG).show();
                            break;
                        }else{
                            PortSipCall call = baseActivity.mCallMgr.getTrnsferCall();
                            if(call!=null&&call.getSessionId()>-1) {
                                sdk.refer(call.getSessionId(), referTo);
                            }
                        }
                    }
                    tansferDlg.dismiss();
                    ternsferDestInput= null;

                    break;
                case R.id.transfer_cancel:
                    ternsferDestInput= null;
                    tansferDlg.dismiss();
                    break;
            }

        }
    }
    void startLoaderForContact(PortSipCall call){
        if(call.attachedContact())
            return;
        final int callid = call.getCallId();

            Bundle bundle = new Bundle();
            bundle.putInt("CALLID",callid);
            PortLoaderManager.restartLoader(baseActivity,loadMgr,LOADER_FIRSTLINE,bundle,ActivityIncallFragment.this);
    }


    void inflateIncomingBar(){
        mImcomingBtBar = (LinearLayout)mLayoutInflater .inflate(R.layout.view_incoming_bottombar,null);
        mImcomingBtBar.findViewById(R.id.fragment_incoming_audiocall).setOnClickListener(this);
        mImcomingBtBar.findViewById(R.id.fragment_incoming_videocall).setOnClickListener(this);
        mImcomingBtBar.findViewById(R.id.fragment_incoming_hanup).setOnClickListener(this);
    }

    void inflateDtmfBar(){
        mDtmfBtBar = (LinearLayout) mLayoutInflater.inflate(R.layout.view_incall_dtmf,null);
        mDtmfBtBar.findViewById(R.id.view_dtmf).setOnClickListener(this);
        mDtmfBtBar.findViewById(R.id.fragment_dialing_digits_back).setOnClickListener(this);
        initDtmfView(mDtmfBtBar);
    }

    void inflateInCallBTBar() {
        mIncallBtBar = (LinearLayout) mLayoutInflater.inflate(R.layout.view_incall_bottombar, null);
    }



    void inflateCalloutBtBar() {
        mCalloutBtBar = (LinearLayout) mLayoutInflater.inflate(R.layout.view_callout_bottombar, null);
        mCalloutBtBar.findViewById(R.id.fragment_dialing_hanup).setOnClickListener(this);
        ImageButton speaker = mCalloutBtBar.findViewById(R.id.fragment_callout_speaker);
        popupWindow.bindViewAndClick(speaker,audioAdapter,this);
        CheckBox box = (CheckBox)mCalloutBtBar.findViewById(R.id.fragment_callout_mute);
        box.setOnCheckedChangeListener(this);

    }

    private  LinearLayout getDtmfBtBar(){
        if(mDtmfBtBar==null){
            inflateDtmfBar();
        }
        return mDtmfBtBar;
    }

    private  LinearLayout getIncomingBar(){
        if(mImcomingBtBar==null){
            inflateIncomingBar();
        }
        return mImcomingBtBar;
    }

    private  LinearLayout getCalloutBtBar(){
        if(mCalloutBtBar==null){
            inflateCalloutBtBar();
        }
        return mCalloutBtBar;
    }

    private  LinearLayout getInCallBtBar(){
        if(mIncallBtBar==null){
            inflateInCallBTBar();
            InitViewPager(mIncallBtBar);
        }
        return mIncallBtBar;
    }

    private int currVolume = 0;

    private void inflateGridView(boolean screenPort,int row,int column,View view){
        int clumindex = column;
        GridLayout gridLayout;
        if(screenPort) {
            if(clumindex>=4){
                gridLayout = secondPager;
                clumindex -=4;
            }else{
                gridLayout = firstPager;
            }
        }else {
            gridLayout = firstPager;
        }

        GridLayout.Spec rowSpec = GridLayout.spec(row, 1f);
        GridLayout.Spec columnSpec = GridLayout.spec(clumindex, 1f);
        GridLayout.LayoutParams layoutParams = new GridLayout.LayoutParams(rowSpec, columnSpec);
        layoutParams.height = 0;
        layoutParams.width = 0;
        gridLayout.addView(view, layoutParams);
    }
    private View generateActionView(LayoutInflater inflater,@IdRes int id,ViewPagerItemClickListener clickListener){
        View view = inflater.inflate(R.layout.viewpager_item,null);
        view.setId(id);
        view.findViewById(R.id.viewpager_item_image).setOnClickListener(clickListener);
        return view;
    }

    private void generateAllActionView(LayoutInflater inflater){
        ViewPagerItemClickListener listener = new ViewPagerItemClickListener();
        View view = generateActionView(inflater,R.id.action_audiovideo,listener);
        actionItems.put(R.id.action_audiovideo,view);

        view = generateActionView(inflater,R.id.action_record,listener);
        actionItems.put(R.id.action_record,view);

        view = generateActionView(inflater,R.id.action_transfer,listener);
        actionItems.put(R.id.action_transfer,view);

        view = generateActionView(inflater,R.id.action_digits,listener);
        actionItems.put(R.id.action_digits,view);

        view = generateActionView(inflater,R.id.action_switchcamera,listener);
        actionItems.put(R.id.action_switchcamera,view);

        view = generateActionView(inflater,R.id.action_closecamera,listener);
        actionItems.put(R.id.action_closecamera,view);

        view = generateActionView(inflater,R.id.action_hold,listener);
        actionItems.put(R.id.action_hold,view);

        view = generateActionView(inflater,R.id.action_add_conference,listener);
        actionItems.put(R.id.action_add_conference,view);

    }

    private  View getActionItem(int id){
        return actionItems.get(id);
    }
}
