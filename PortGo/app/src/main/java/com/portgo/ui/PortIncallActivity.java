package com.portgo.ui;

import android.app.Fragment;
import android.app.admin.DevicePolicyManager;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.PowerManager;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

import com.portgo.BuildConfig;
import com.portgo.R;
import com.portgo.manager.CallManager;
import com.portgo.manager.NotificationUtils;
import com.portgo.manager.PortSipCall;
import com.portgo.manager.PortSipSdkWrapper;
import com.portsip.PortSipErrorcode;
import com.portsip.PortSipSdk;

import java.util.Observable;
import java.util.Observer;

import static com.portgo.BuildConfig.PORT_ACTION_ACCEPT;
import static com.portgo.BuildConfig.PORT_ACTION_REGIEST;
import static com.portgo.BuildConfig.PORT_ACTION_REJECT;

public class PortIncallActivity extends PortGoBaseActivity implements Observer {
	PortSipSdkWrapper sdk;
	final int MENU_QUIT = 0;
	public static final int ACTIVE_CALL = 0xa7840;
	public static final int MAKE_CONFERENCE =ACTIVE_CALL+1;
	public static final int DES_CONFERENCE =MAKE_CONFERENCE+1;
	Context mContext;
	ActivityIncallFragment videoCallFragment = null;
	ScreenFragment screenoffFragment = null;
	Fragment frontFragment;
	View contentView = null;
	private MyProxSensor mProxSensor;
    PortSipCall mSipcall = null;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		final Window win = getWindow();
		win.addFlags( WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                | WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
                | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
				| WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);

		setContentView(R.layout.mainview);
		mContext = this;
		contentView = findViewById(R.id.content);
		update(null,null);
//        loadIncomingFragment();
        mCallMgr.getObservableCalls().addObserver(this);
		setVolumeControlStream(AudioManager.STREAM_VOICE_CALL);
		setNeedMinimunWindow(false);

		Intent intent = getIntent();
		if(PORT_ACTION_ACCEPT.equals(intent.getAction())){

			int callid = intent.getIntExtra("CALLID", PortSipErrorcode.INVALID_SESSION_ID);
			boolean video = intent.getBooleanExtra("video",false);
			NotificationUtils.getInstance(this).cancelPendingCallNotification(this);
			PortSipCall sipCall = CallManager.getInstance().getCallByCallId(callid);
			if(sipCall!=null){
				sipCall.accept(this,null,video);
			}
		}
	}

	@Override
	synchronized protected void onDestroy() {

		mCallMgr.getObservableCalls().deleteObserver(this);
		setVolumeControlStream(AudioManager.STREAM_RING);
		super.onDestroy();
	}

	@Override
	protected void onResume() {
		setNeedMinimunWindow(false);
		super.onResume();
        mSipcall = mCallMgr.getPendingCall();
		if(mSipcall==null){
            mSipcall = mCallMgr.getDefaultCall();
            if(mSipcall==null) {
                this.finish();
            }
		}
		if(mProxSensor == null){
			mProxSensor = new MyProxSensor();
		}
		mProxSensor.start();

		loadVideoFragment();
	}

	@Override
	protected void onPause() {
		setNeedMinimunWindow(true);
		super.onPause();
		if(mProxSensor != null){
			mProxSensor.stop();
		}
		loadScreenOffFragment();

	}

	private void loadVideoFragment() {
		if (videoCallFragment == null) {
			videoCallFragment = new ActivityIncallFragment();
		}

		if(frontFragment != videoCallFragment) {
			frontFragment= videoCallFragment;
            getFragmentManager().beginTransaction()
                    .replace(R.id.content, videoCallFragment).commitAllowingStateLoss();
        }
	}
	private void loadScreenOffFragment() {
		if (screenoffFragment == null) {
			screenoffFragment = new ScreenFragment();
		}
        if(frontFragment != screenoffFragment) {
			frontFragment= screenoffFragment;
            getFragmentManager().beginTransaction()
                    .replace(R.id.content, screenoffFragment).commitAllowingStateLoss();
        }
	}



	PowerManager.WakeLock mScreenWakeLock;
	class MyProxSensor implements SensorEventListener
	{
		private final SensorManager mSensorManager;
		private Sensor mSensor;
		private final int SCREENOFF_DISTANCE=5;
		private float mMaxRange;

		MyProxSensor(){
			mSensorManager = (SensorManager) PortIncallActivity.this.getSystemService(SENSOR_SERVICE);
		}

		void start(){
			if(mSensorManager != null && mSensor == null){
				if((mSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY)) != null){
					mMaxRange = mSensor.getMaximumRange();
					mSensorManager.registerListener(this, mSensor, SensorManager.SENSOR_DELAY_UI);
				}
			}
		}

		void stop(){
			if(mSensorManager != null && mSensor != null){
				mSensorManager.unregisterListener(this);
				mSensor = null;
			}
		}

		@Override
		public void onAccuracyChanged(Sensor sensor, int accuracy) {
		}

		@Override
		public void onSensorChanged(SensorEvent event) {
			try{ // Keep it until we get a phone supporting this feature
				if(event.values != null && event.values.length >0){
					if((event.values[0] < this.mMaxRange)&&event.values[0] <SCREENOFF_DISTANCE){
                        if(frontFragment!=screenoffFragment) {
							loadScreenOffFragment();
						}
					}
					else{
						loadVideoFragment();
					}

				}
			}
			catch(Exception e){
				e.printStackTrace();
			}
		}
	}


	@Override
	synchronized public void update(Observable observable, final Object data) {
		if(!isDestroyed()) {
			if (mCallMgr.getCallsSize() < 1) {//
				this.finish();
				return;
			}
		}
		return;
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if(videoCallFragment!=null){
			videoCallFragment.onActivityResult(requestCode, resultCode, data);
		}
	}
}
