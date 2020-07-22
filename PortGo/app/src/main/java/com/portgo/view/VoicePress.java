package com.portgo.view;

import android.app.Activity;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.speech.SpeechRecognizer;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.widget.Toast;

import com.portgo.R;
import com.portgo.manager.ConfigurationManager;
import com.portgo.util.UnitConversion;
import com.portgo.util.av.AuditRecorderConfiguration;
import com.portgo.util.av.ExtAudioRecorder;
import com.portgo.util.av.FailRecorder;

import java.io.File;
import java.util.ArrayList;
import java.util.UUID;

/**
 * Created by Administrator on 2015/11/10.
 */
public class VoicePress extends ChatInputBaseFragment {
    private View rootView = null;
    private TextView sayTextView;

    private PopupWindow popupWindow, popupCancelWindow;
    private View hintView, hintCancelView;

    private ImageView volume;
    private boolean hasPermission = false;

    private String mVoiceName;
    private int voiceDuration;
    private final int ANIMATION_DURATION = 250;
    private final int MAX_VOICE_LEN = 60*(1000/250);
    private boolean voiceCanceled;

    ExtAudioRecorder recorder;
    private voiceMessageListener listener = null;
    Handler handler = new Handler();
    public interface voiceMessageListener {
        public void onSendVoiceMessage(String path, int duration, String description);
        public boolean canStart();
    }

    public void setListener(voiceMessageListener listener){
        this.listener = listener;
    }

    @Override
    protected View onInitializeView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.voice_press, container, false);
        sayTextView = (TextView)rootView.findViewById(R.id.press_to_talk);
        init();

        Handler handler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
            }
        };
        ExtAudioRecorder.RecorderListener listener = new ExtAudioRecorder.RecorderListener() {
            @Override
            public void recordFailed(FailRecorder failRecorder) {
                if (failRecorder.getType() == FailRecorder.FailType.NO_PERMISSION) {
                    Toast.makeText(getActivity(), R.string.string_permission_error, Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(getActivity(), R.string.string_unknow_error, Toast.LENGTH_SHORT).show();
                }
            }
        };

        AuditRecorderConfiguration configuration = new AuditRecorderConfiguration.Builder()
                .recorderListener(listener)
                .handler(handler)
                .uncompressed(false)
                .builder();

        recorder = new ExtAudioRecorder(configuration);
        return rootView;
    }

    private void init() {
        initHintView();
        initListeners();
    }

    private void initHintView() {
        Activity activity = this.getActivity();
        hintView = LayoutInflater.from(activity).inflate(R.layout.avos_input_voice, null);
        volume = (ImageView) hintView.findViewById(R.id.id_voice_amplitude);
        hintCancelView = LayoutInflater.from(activity).inflate(R.layout.avos_voice_cancel, null);
        popupWindow = new PopupWindow(activity);
        popupWindow.setContentView(hintView);
        int width  = UnitConversion.dp2px(activity, 150);
        int height  = UnitConversion.dp2px(activity, 150);
        popupWindow.setWidth(width);
        popupWindow.setHeight(height);
        popupWindow.setFocusable(false);
        popupCancelWindow = new PopupWindow(activity);
        popupCancelWindow.setContentView(hintCancelView);
        popupCancelWindow.setFocusable(false);

        popupCancelWindow.setWidth(width);
        popupCancelWindow.setHeight(height);

    }

    MotionEvent lastEvent;
    View.OnTouchListener vpOnTouchListener;
    private void initListeners() {
        vpOnTouchListener =new View.OnTouchListener() {

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                lastEvent =event;
                float eventY = event.getY();
                if (event.getAction() == MotionEvent.ACTION_MOVE) {
                    if(!canStart){//
                        return true;
                    }
                    if (eventY < -100) {//
                        if (popupWindow.isShowing()) {
                            popupWindow.dismiss();
                        }
                        if (popupCancelWindow != null && !popupCancelWindow.isShowing()) {
                            popupCancelWindow.showAtLocation(v, Gravity.CENTER, 0, 0);
                        }
                    } else {//
                        if (popupCancelWindow.isShowing()) {
                            popupCancelWindow.dismiss();
                        }
                        if (popupWindow != null && !popupWindow.isShowing()) {
                            popupWindow.showAtLocation(v, Gravity.CENTER, 0, 0);
                        }
                    }
                }

                if (event.getAction() == MotionEvent.ACTION_UP||event.getAction() ==MotionEvent.ACTION_CANCEL) {
                    if(!canStart) {//
                        return true;
                    }
                    canStart= false;
                    if (popupWindow.isShowing()) {
                        popupWindow.dismiss();
                    }
                    if (popupCancelWindow.isShowing()) {
                        popupCancelWindow.dismiss();
                    }
                    ((TextView) v).setText(R.string.string_downtips);
                    v.setBackgroundResource(R.drawable.talk_btn_bg);

                    if (eventY > -100 && hasPermission) {//
                        voiceCanceled = false;
                        stop();
                    } else {
                        voiceCanceled = true;
                        cancel();
                    }
                } else if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    if(listener!=null&&!listener.canStart()){//
                        canStart= false;
                        return true;
                    }
                    canStart= true;
                    ((TextView) v).setText(R.string.string_uptips);
                    v.setBackgroundResource(R.drawable.talk_btn_bg_press);
                    long startVoiceT = SystemClock.currentThreadTimeMillis();
                    mVoiceName = startVoiceT + "";
                    if (popupWindow != null && !popupWindow.isShowing()) {
                        popupWindow.showAtLocation(v, Gravity.CENTER, 0, 0);
                    }
                    start();
                }
                return true;
            }
        };
        sayTextView.setOnTouchListener(vpOnTouchListener);
    }

    boolean canStart = false;
    String getVoiceNameWithoutSuffix() {
        return mVoiceName;
    }

    private void start() {
        voiceDuration = 0;
        if (!hasPermission) {
            if (!judgePermission()) {
                return;
            }
        }
        if(recorder.getState()!= ExtAudioRecorder.State.READY){
            recorder.reset();
        }
        String fileName = UUID.randomUUID().toString();
        String videoPath = ConfigurationManager.getInstance().getStringValue(this.getActivity(),ConfigurationManager.PRESENCE_VIDEO_PATH,
                this.getActivity().getExternalFilesDir(null).getAbsolutePath());
        recorder.setOutputFile(videoPath+ File.separator+fileName+".amr");
        recorder.prepare();
        recorder.start();
        handler.post(AmpUpdate);
    }


    private void cancel() {
        recorder.discardRecording();
        handler.removeCallbacks(AmpUpdate);
        recorder.reset();
        voiceDuration =0;
    }

    private void stop() {
        int recorerLen = recorder.stop();
        String filePath =recorder.getRecordFilePath();
        if(recorerLen>0){
            listener.onSendVoiceMessage(filePath,recorerLen,null);
        }else{
            Toast.makeText(getActivity(),R.string.voice_too_short,Toast.LENGTH_SHORT);
        }
        voiceDuration = 0;
        recorder.reset();
        handler.removeCallbacks(AmpUpdate);
    }

    private boolean judgePermission() {
        PackageInfo packageInfo;
        try {
            packageInfo = this.getActivity().getPackageManager().getPackageInfo(this.getActivity().getPackageName(), PackageManager.GET_PERMISSIONS);
            String permissions[] = packageInfo.requestedPermissions;
            ArrayList<String> list = new ArrayList<String>();
            for (int i = 0; i < permissions.length; i++) {
                list.add(permissions[i]);
            }
            if (list.contains("android.permission.RECORD_AUDIO")) {
                hasPermission = true;
            } else {
                hasPermission = false;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return hasPermission;
    }
    int ampIndex=0;
    Runnable AmpUpdate= new Runnable() {
        @Override
        public void run() {
            ampIndex=(ampIndex+1)%6;
            updateDisplay(ampIndex);
            handler.postDelayed(AmpUpdate,ANIMATION_DURATION);
            if(voiceDuration<MAX_VOICE_LEN){
                voiceDuration++;
            }else{
                lastEvent.setAction(MotionEvent.ACTION_UP);
                vpOnTouchListener.onTouch(sayTextView,lastEvent);
            }
        }
    };
    private void updateDisplay(double signalEMA) {

        switch ((int) signalEMA) {
            case 0:
                volume.setImageResource(R.drawable.amp1);
                break;
            case 1:
                volume.setImageResource(R.drawable.amp2);
                break;
            case 2:
                volume.setImageResource(R.drawable.amp3);
                break;
            case 4:
                volume.setImageResource(R.drawable.amp4);
                break;
            case 5:
                volume.setImageResource(R.drawable.amp5);
                break;
            case 6:
                volume.setImageResource(R.drawable.amp6);
                break;
            default:
                volume.setImageResource(R.drawable.amp7);
                break;
        }
    }


}