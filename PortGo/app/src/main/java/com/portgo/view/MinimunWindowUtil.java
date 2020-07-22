package com.portgo.view;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.os.Build;
import android.provider.Settings;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnKeyListener;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.View.OnClickListener;
import android.view.WindowManager.LayoutParams;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.portgo.PortApplication;
import com.portgo.R;
import com.portgo.manager.CallManager;
import com.portgo.manager.PermissionManager;
import com.portgo.manager.PortSipCall;
import com.portgo.manager.PortSipEngine;
import com.portgo.manager.PortSipSdkWrapper;
import com.portgo.manager.SipManager;
import com.portgo.ui.PortIncallActivity;
import com.portgo.util.NotifyData;
import com.portsip.PortSIPVideoRenderer;
import com.portsip.PortSipSdk;

import java.util.Calendar;
import java.util.Date;
import java.util.Observable;
import java.util.Observer;
import java.util.TimeZone;
import android.os.Handler;

public class MinimunWindowUtil {
    private static final String LOG_TAG = "MinimunWindowUtil";
    private static View mView = null;
    private static WindowManager mWindowManager = null;
    private static Context mContext = null;
    public static Boolean isShown = false;
    private static PortSipEngine engine = null;
    private static SipManager sipManager = null;
    private static PortSipCall msipCall = null;
    private static PortApplication application;
    private static int windowWidth = 0;
    private static PortSIPVideoRenderer remoteRender= null;
    private static PortSIPVideoRenderer localRender= null;
    private static Object lock = new Object();

    private static Handler handler = null;


    static WindowManager.LayoutParams params;
    public static void showPopupWindow(final Activity context) {
        if (isShown) {
            return;
        }
        isShown = true;
        mContext = context;
        application = (PortApplication) mContext.getApplicationContext();
        mWindowManager = (WindowManager) mContext.getSystemService(Context.WINDOW_SERVICE);
        windowWidth = mWindowManager.getDefaultDisplay().getWidth();
        setUpView(context);
        params = new WindowManager.LayoutParams();
        if(Build.VERSION.SDK_INT>=26) {
            params.type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        }else{		
            params.type = WindowManager.LayoutParams.TYPE_SYSTEM_ALERT;
        }
        params.format = PixelFormat.RGBA_8888;
        params.flags = LayoutParams.FLAG_NOT_FOCUSABLE;
        params.gravity = Gravity.RIGHT | Gravity.TOP;
        params.x = 0;
        params.y = 0;
        params.width = 240;
        params.height = 320;

        mView.setOnTouchListener(new View.OnTouchListener()
        {
            int movex,movey;
            int posX,posY;
            @Override
            public boolean onTouch(View v, MotionEvent event)
            {
                switch (event.getAction()){
                    case  MotionEvent.ACTION_DOWN:
                        movex= 0;
                        movey= 0;
                        posX =(int)event.getRawX();
                        posY =(int)event.getRawY();
                        break;
                    case  MotionEvent.ACTION_MOVE:
                        params.x = windowWidth - (int)event.getRawX()- mView.getWidth() / 2;
                        params.y = (int) event.getRawY() - mView.getHeight() / 2;
                        mWindowManager.updateViewLayout(mView, params);
                        movex+=Math.abs(event.getRawX()-posX);
                        movex+=1;
                        movey+=Math.abs(event.getRawY()-posY);
                        movey+=1;
                        posX = (int) event.getRawX();
                        posY = (int) event.getRawY();
                        return true;
                    case  MotionEvent.ACTION_UP:
                        posX = 0;
                        posY= 0;
                        if(movex>50||movey>50){
                            return true;
                        }
                        movex= 0;
                        movey= 0;
                        break;
                }
                return false;

            }
        });
        mWindowManager.addView(mView, params);
        mView.requestLayout();
    }

    public static void hidePopupWindow() {
        if (isShown) {
            CallManager.getInstance().getObservableCalls().deleteObserver(callObserver);
            PortSipSdkWrapper sdk = PortSipSdkWrapper.getInstance();

            CallManager.getInstance().setConferenceView(null);
            if (remoteRender != null) {
                remoteRender.release();
                remoteRender = null;
            }
            SipManager.getSipManager().disablePlayLocalVideo();
            if (localRender != null) {
                sdk.setLocalVideoWindow(null);
                localRender.release();
                localRender = null;
            }
            if (mView != null&&mWindowManager!=null) {
                mWindowManager.removeView(mView);
                mView = null;
            }
            isShown = false;
        }
        callObserver = null;
    }

    private  static  Observer callObserver;
    private static View setUpView(final Activity context) {

        handler = new Handler();
        mView = LayoutInflater.from(context).inflate(R.layout.view_incall_minimun,
                null);
        localRender = mView.findViewById(R.id.mini_local_render);
        remoteRender = mView.findViewById(R.id.mini_remote_render);
        remoteRender.setScalingType(PortSIPVideoRenderer.ScalingType.SCALE_ASPECT_FIT);
        SipManager.getSipManager().disablePlayLocalVideo();
        PortSipSdkWrapper.getInstance().setLocalVideoWindow(null);

        engine = application.getPortSipEngine();
        sipManager= engine.getSipManager();
        callObserver = new Observer() {
            @Override
            public void update(Observable observable, Object data) {
                PortSipCall sipCall;
                if(data instanceof NotifyData) {
                    NotifyData notifyData = (NotifyData) data;
                    Object observalData = notifyData.getObject();
                    if(CallManager.getInstance().getCallsSize()<1) {
                        handler.post(new Runnable() {
                            @Override
                            public void run() {
                                MinimunWindowUtil.hidePopupWindow();
                            }
                        });

                        return;
                    }
                    switch (notifyData.getAction()){
                        case ACTIONT_ADD:
                            sipCall = (PortSipCall) observalData;
                            if((msipCall!=null)&&(sipCall!=null)&&(sipCall.getCallId()!=msipCall.getCallId())) {
                                msipCall = sipCall;
                            }
                            break;
                        case ACTIONT_ADD_CONNECTION:
                            break;
                        case ACTIONT_REMOVE:
                            sipCall = (PortSipCall) observalData;
                            if((msipCall!=null)&&(sipCall!=null)&&(sipCall.getCallId()!=msipCall.getCallId())){
                                msipCall = CallManager.getInstance().getDefaultCall();
                            }else{
                                handler.post(new Runnable() {
                                    @Override
                                    public void run() {
                                        updateView(msipCall);
                                    }
                                });

                            }

                            break;
                        case ACTIONT_REMOVE_CONNECTION:
                            break;
                        case ACTIONT_CLEAR:
                            break;
                        case ACTIONT_UPDATE:
                            handler.post(new Runnable() {
                                @Override
                                public void run() {
                                    updateView(msipCall);
                                }
                            });

                            break;
                    }
                }
            }
        };
        CallManager.getInstance().getObservableCalls().addObserver(callObserver);
        msipCall = CallManager.getInstance().getActiveCall();
        if(msipCall==null){
            msipCall = CallManager.getInstance().getDefaultCall();
        }

        updateView(msipCall);
        mView.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                MinimunWindowUtil.hidePopupWindow();
                mContext.startActivity(new Intent(mContext, PortIncallActivity.class));
            }
        });

        mView.setOnKeyListener(new OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                switch (keyCode) {
                    case KeyEvent.KEYCODE_BACK:
                        MinimunWindowUtil.hidePopupWindow();
                        mContext.startActivity(new Intent(mContext, PortIncallActivity.class));
                        return true;
                    default:
                        return false;
                }
            }
        });
        return mView;
    }

    static  private void updateView(PortSipCall call){

        if(mView!=null) {
            if (call != null) {
                RelativeLayout audio = (RelativeLayout) mView.findViewById(R.id.llAudio);
                FrameLayout video = (FrameLayout) mView.findViewById(R.id.flVideo);
                if ((!sipManager.isConference() && call.isConnect() && call.isVideoCall() && call.isVideoNegotiateSucess())
                        || (sipManager.isConference() && !sipManager.isAudioConference())) {
                    audio.setVisibility(View.INVISIBLE);
                    video.setVisibility(View.VISIBLE);
                    localRender.setZOrderOnTop(true);

                    if ((!sipManager.isConference() && call.isVideoCall() && call.isSendVideo() && call.isVideoNegotiateSucess()) || (sipManager.isConference() && !sipManager.isAudioConference())) {
                        PortSipSdkWrapper.getInstance().setLocalVideoWindow(localRender);
                        SipManager.getSipManager().disPlayLocalVideo();
                    }
                    if (sipManager.isConference() && !sipManager.isAudioConference()) {
                        CallManager.getInstance().setConferenceView(remoteRender);
                    } else {
                        if (!sipManager.isConference() && call.isVideoCall() && call.isVideoNegotiateSucess()) {
                            CallManager.getInstance().setRemoteViewID(call.getSessionId(),remoteRender);
                        }
                    }

                } else {
                    video.setVisibility(View.INVISIBLE);
                    audio.setVisibility(View.VISIBLE);
                    TextView view = (TextView) audio.findViewById(R.id.mini_view_dialing_name);
                    view.setText(call.getRemoteDisName());
                    TextViewClock clock = (TextViewClock) audio.findViewById(R.id.mini_view_dialing_connecttime);

                    long begin = 0;
                    if (msipCall.isConnect()) {
                        begin = msipCall.getConnectTime();
                    } else {
                        begin = msipCall.getStartTime();
                    }
                    begin = new Date().getTime() - begin;
                    Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("GMT+00:00"));
                    calendar.setTimeInMillis(begin);
                    clock.setCurrentTime(calendar);
                }
            }
        }
    }
}