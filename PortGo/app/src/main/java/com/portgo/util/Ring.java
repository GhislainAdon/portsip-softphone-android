package com.portgo.util;

import android.content.Context;
import android.media.AudioManager;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.media.ToneGenerator;
import android.net.Uri;
import android.os.Vibrator;
import android.text.TextUtils;

import com.portgo.R;
import com.portgo.manager.ConfigurationManager;

public class Ring {

    private static final int TONE_RELATIVE_VOLUME = 70;
    private ToneGenerator mRingbackPlayer;
    protected Ringtone mRingtonePlayer;

    int ringRef = 0;
    int viabrateRef = 0;
    boolean ringDefault;
    boolean vibrateDefault;
    static private Context mContext;
    Vibrator mVibrator = null;
    static Ring instance = new Ring();
    private Ring(){
    }

    public void init(Context context){
        mContext = context.getApplicationContext();
        mVibrator = (Vibrator)mContext.getSystemService(Context.VIBRATOR_SERVICE);
        ringDefault =mContext.getResources().getBoolean(R.bool.prefrence_ring_default);
        vibrateDefault =mContext.getResources().getBoolean(R.bool.prefrence_vibrate_default);
        try {
            RingtoneManager.getActualDefaultRingtoneUri(mContext,RingtoneManager.TYPE_NOTIFICATION);
        }catch (SecurityException security){

        }

    }
    public void uninit(){
        mContext = null;
    }

    static public Ring getInstance() {
        return instance;
    }

    public boolean stop() {
        stopRingBackTone();
        stopRingTone();
        return true;
    }

    private boolean needVibrate(int audioMode){
        if(mContext==null) {
            throw new NullPointerException("not initialize");
        };
        ConfigurationManager configurationManager = ConfigurationManager.getInstance();
        boolean vibrate =configurationManager.getBooleanValue(mContext,ConfigurationManager.PRESENCE_ENABLE_VIBRATE,vibrateDefault);
        if(audioMode==AudioManager.RINGER_MODE_SILENT){
            return false;
        }
        else {
            return vibrate;
        }
    }

    private boolean needRing(int audioMode){
        if(mContext==null) {
            throw new NullPointerException("not initialize");
        }
        ConfigurationManager configurationManager = ConfigurationManager.getInstance();

        boolean ring = configurationManager.getBooleanValue(mContext,ConfigurationManager.PRESENCE_ENABLE_RING,ringDefault);
        if(audioMode==AudioManager.RINGER_MODE_SILENT||audioMode==AudioManager.RINGER_MODE_VIBRATE){//静音模式，直接返回，不响铃，不振动。
            return false;
        }
        else {
            return ring;
        }
    }
    public void startRingTone() {
        if(mContext==null) {
            throw new NullPointerException("not initialize");
        }
        ConfigurationManager configurationManager = ConfigurationManager.getInstance();

        AudioManager audioManager = (AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);
        int ringerMode = audioManager.getRingerMode();
        if (!needRing(ringerMode) &&
                !needVibrate(ringerMode)) {
            return;
        }


        if (needVibrate(ringerMode) && mVibrator.hasVibrator()) {
            viabrateRef++;
            mVibrator.vibrate(new long[]{1000, 2000, 1000, 2000}, 1);
        } else {
            mVibrator.cancel();
        }

        if (needRing(ringerMode)) {
            if (mRingtonePlayer != null && mRingtonePlayer.isPlaying()) {
                ringRef++;
                return;
            }

            if (mRingtonePlayer == null && mContext != null) {
                try {
                    Uri ringUri = RingtoneManager.getActualDefaultRingtoneUri(mContext, RingtoneManager.TYPE_RINGTONE);
                    String ringPath = configurationManager.getStringValue(mContext,ConfigurationManager.PRESENCE_CALLRING, null);
                    if (!TextUtils.isEmpty(ringPath)) {
                        ringUri = Uri.parse(ringPath);
                    }

                    mRingtonePlayer = RingtoneManager.getRingtone(mContext, ringUri);
                } catch (Exception e) {
                    e.printStackTrace();
                    return;
                }
            }

            if (mRingtonePlayer != null) {
                synchronized (mRingtonePlayer) {
                    ringRef++;
                    mRingtonePlayer.play();

                }
            }
        } else {
            if (mRingtonePlayer != null) {
                synchronized (mRingtonePlayer) {
                    ringRef = 0;
                    if (mRingtonePlayer.isPlaying()) {
                        mRingtonePlayer.stop();
                        mRingtonePlayer = null;
                    }
                }
            }
        }
    }

    public void stopRingTone() {
        if(mContext==null) {
            throw new NullPointerException("not initialize");
        }
        AudioManager audioManager = (AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);
        int ringerMode = audioManager.getRingerMode();
        if(!needVibrate(ringerMode)){
            viabrateRef=0;
            mVibrator.cancel();
        }else{
            if(--viabrateRef<=0){
                mVibrator.cancel();
                viabrateRef=0;
            }
        }
        if(needRing(ringerMode)) {
            if (mRingtonePlayer != null) {
                synchronized (mRingtonePlayer) {

                    if (--ringRef <= 0) {
                        mRingtonePlayer.stop();
                        mRingtonePlayer = null;
                    }
                }
            }
        }else{
            if (mRingtonePlayer != null) {
                synchronized (mRingtonePlayer) {

                    ringRef= 0;
                    mRingtonePlayer.stop();
                    mRingtonePlayer = null;
                }
            }
        }
    }


    public void startRingBackTone() {

        if(mContext==null) {
            throw new NullPointerException("not initialize");
        }
        if (mRingbackPlayer == null) {
            try {
                mRingbackPlayer = new ToneGenerator(AudioManager.STREAM_VOICE_CALL, TONE_RELATIVE_VOLUME);
            } catch (RuntimeException e) {
                mRingbackPlayer = null;
            }
        }

        if(mRingbackPlayer != null){
            synchronized(mRingbackPlayer){
                mRingbackPlayer.startTone(ToneGenerator.TONE_SUP_RINGTONE);
            }
        }
    }

    public void stopRingBackTone() {
        if(mContext==null) {
            throw new NullPointerException("not initialize");
        }
        if(mRingbackPlayer != null){
            synchronized(mRingbackPlayer){
                mRingbackPlayer.stopTone();
                mRingbackPlayer =null;
            }
        }
    }

}
