package com.portgo.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Point;
import android.hardware.Camera;
import android.hardware.SensorManager;
import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Config;
import android.util.Log;
import android.view.MotionEvent;
import android.view.OrientationEventListener;
import android.view.Surface;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import com.portgo.PortApplication;
import com.portgo.manager.ConfigurationManager;
import com.portgo.util.Constants;
import com.portgo.R;
import com.portgo.util.MIMEType;
import com.portgo.util.VideoThumbnailUtils;
import com.portgo.util.camera.SensorControler;
import com.portgo.util.gpufilter.SlideGpuFilterGroup;
import com.portgo.util.gpufilter.helper.MagicFilterType;
import com.portgo.view.VideoPress;

import com.portgo.view.widget.FocusImageView;
import com.portgo.view.widget.CameraView;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.UUID;


public class RecordedActivity extends AppCompatActivity implements View.OnClickListener, View.OnTouchListener, SensorControler.SensorListener, SlideGpuFilterGroup.OnFilterChangeListener, VideoPress.OnRecordListener {

    private CameraView mCameraView;
    private VideoPress mCapture;
    private FocusImageView mFocus;
    private ImageView mBtnBack;
    private ImageView mBtnConfirm;
    private ImageView mCameraChange;
    private boolean pausing = false;
    private boolean recordFlag = false;

    private boolean autoPausing = false;
//    ExecutorService executorService;
    private SensorControler mSensorControler;
    OrientationEventListener mOrientationListener;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recorde);
        mSensorControler = SensorControler.getInstance(this);
        mSensorControler.setCameraFocusListener(this);

        mOrientationListener = new OrientationEventListener(this,SensorManager.SENSOR_DELAY_NORMAL) {
            @Override

            public void onOrientationChanged(int orientation) {

                if (orientation == OrientationEventListener.ORIENTATION_UNKNOWN) {

                }else if (orientation > 350 || orientation < 10) { //
                    orientation = 0;

                } else if (orientation > 80 && orientation < 100) { //
                    orientation = 90;
                } else if (orientation > 170 && orientation < 190) { //
                    orientation = 180;
                } else if (orientation > 260 && orientation < 280) { //
                    orientation = 270;
                } else {
                    return;
                }
                mCameraView.setOriatation(orientation);
            }
        };
        initView();
    }

    private void initView() {
        mCameraView = (CameraView) findViewById(R.id.camera_view);
        mCapture = (VideoPress) findViewById(R.id.btn_camera_record);
        mFocus = (FocusImageView) findViewById(R.id.focusImageView);
        mBtnBack = (ImageView) findViewById(R.id.btn_camera_back);
        mBtnConfirm = (ImageView) findViewById(R.id.btn_camera_confirm);
        mCameraChange = (ImageView) findViewById(R.id.btn_camera_switch);
        mBtnConfirm.setVisibility(View.INVISIBLE);
        mBtnBack.setVisibility(View.INVISIBLE);
        mBtnBack.setOnClickListener(this);
        mCameraView.setOnTouchListener(this);
        mCameraChange.setOnClickListener(this);
        mCapture.setOnRecordListener(this);
        mBtnConfirm.setOnClickListener(this);
    }
    @Override
    public boolean onTouch(View v, MotionEvent event) {
//        mCameraView.onTouch(event);
        if (mCameraView.getCameraId() == 1) {
            return false;
        }
        switch (event.getAction()) {
            case MotionEvent.ACTION_UP:
                float sRawX = event.getRawX();
                float sRawY = event.getRawY();
                float rawY = sRawY * Constants.screenWidth / Constants.screenHeight;
                float temp = sRawX;
                float rawX = rawY;
                rawY = (Constants.screenWidth - temp) * Constants.screenHeight / Constants.screenWidth;

                Point point = new Point((int) rawX, (int) rawY);
                mCameraView.onFocus(point, callback);
                mFocus.startFocus(new Point((int) sRawX, (int) sRawY));
        }
        return true;
    }

    Camera.AutoFocusCallback callback = new Camera.AutoFocusCallback() {
        @Override
        public void onAutoFocus(boolean success, Camera camera) {
            if (success) {
                mFocus.onFocusSuccess();
            } else {
                mFocus.onFocusFailed();

            }
        }
    };
    @Override
    public void stayAfterMove() {
        if (mCameraView.getCameraId() == 1) {
            return;
        }
        Point point = new Point(Constants.screenWidth / 2, Constants.screenHeight / 2);
        mCameraView.onFocus(point, callback);
    }

    @Override
    public void oriatationChanger(float[] values) {
    }

    public static int getDisplayRotation(Activity activity) {
        if(activity == null)
            return 0;

        int rotation = activity.getWindowManager().getDefaultDisplay()
                .getRotation();
        switch (rotation) {
            case Surface.ROTATION_0:
                return 0;
            case Surface.ROTATION_90:
                return 90;
            case Surface.ROTATION_180:
                return 180;
            case Surface.ROTATION_270:
                return 270;
        }
        return 0;
    }
    @Override
    public void onBackPressed() {
        if (recordFlag) {
            recordFlag = false;
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        mCameraView.onResume();
        if (recordFlag && autoPausing) {
            mCameraView.resume(true);
            autoPausing = false;
        }
        mSensorControler.onStart();

        if (mOrientationListener.canDetectOrientation()) {
            mOrientationListener.enable();
        } else {
            mOrientationListener.disable();
        }
    }
    @Override
    protected void onPause() {
        mSensorControler.onStop();
        mOrientationListener.disable();
        super.onPause();
        if (recordFlag && !pausing) {
            mCameraView.pause(true);
            autoPausing = true;
        }
        mCameraView.onPause();
    }
    @Override
    public void onFilterChange(final MagicFilterType type) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (type == MagicFilterType.NONE){
                }else {
                }
            }
        });

    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.btn_camera_switch:
                mCameraView.switchCamera();
                if (mCameraView.getCameraId() == 1){

                    mCameraView.changeBeautyLevel(0);
                }else {
                    mCameraView.changeBeautyLevel(0);
                }
                break;
            case R.id.btn_camera_back:
                if(!TextUtils.isEmpty(recordFilePath)) {
                    File file = new File(recordFilePath);
                    if(file.exists()){
                        file.delete();
                    }
                }
                setResult(Activity.RESULT_CANCELED);
                mCameraView.startPreview();

                mCapture.setVisibility(View.VISIBLE);
                mCameraChange.setVisibility(View.VISIBLE);

                mBtnConfirm.setVisibility(View.INVISIBLE);
                mBtnBack.setVisibility(View.INVISIBLE);

                break;
            case R.id.btn_camera_confirm:
                Intent resultIntent = new Intent();
                resultIntent.putExtra(PortActivityVideoReorder.RECORDER_NAME,recordFilePath);
                resultIntent.putExtra(PortActivityVideoReorder.RECORDER_LEN,0);

                setResult(Activity.RESULT_OK,resultIntent);

                this.finish();
                break;
        }
    }

    public void takePhoto(){
        mCameraView.takePicture(new Camera.PictureCallback() {
            @Override
            public void onPictureTaken(byte[] data, Camera camera) {
                try {
                    camera.stopPreview();
                    Bitmap bitmap= BitmapFactory.decodeByteArray(data,0,data.length);
                    bitmap = mCameraView.rotatePic(bitmap);
                    String videoPath = recordFilePath;
                    if(!TextUtils.isEmpty(videoPath)) {
                        FileOutputStream fileOutputStream = new FileOutputStream(videoPath);
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 85, fileOutputStream);
                        fileOutputStream.close();
                    }
                    mBtnConfirm.setVisibility(View.VISIBLE);

                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }finally {
                    mBtnBack.setVisibility(View.VISIBLE);
                }
            }
        });
    }

    @Override
    public void onShortClick() {
        recordFilePath = ConfigurationManager.getInstance().getStringValue(this,ConfigurationManager.PRESENCE_VIDEO_PATH, getExternalFilesDir(null).getAbsolutePath());
        recordFilePath+="/"+ UUID.randomUUID().toString()+ MIMEType.MIMETYPE_imagejpg_EXT;
        File jpgfile = new File(recordFilePath);
        if(!jpgfile.getParentFile().exists()){
            jpgfile.getParentFile().mkdirs();
        }

        mCameraView.setSavePath(recordFilePath);

        takePhoto();
        mCameraChange.setVisibility(View.INVISIBLE);
        mCapture.setVisibility(View.INVISIBLE);
    }

    String recordFilePath;
    @Override
    public void OnRecordStartClick() {
        recordFilePath = ConfigurationManager.getInstance().getStringValue(this,
                ConfigurationManager.PRESENCE_VIDEO_PATH, getExternalFilesDir(null).getAbsolutePath());
        recordFilePath+="/"+ UUID.randomUUID().toString()+ MIMEType.MIMETYPE_videomp4_EXT;
        File mp4file = new File(recordFilePath);
        if(!mp4file.getParentFile().exists()){
            mp4file.getParentFile().mkdirs();
        }


        mCameraView.setSavePath(recordFilePath);
        mCameraView.startRecord();
    }

    @Override
    public void OnFinish(int resultCode) {
        mCameraView.stopRecord();
        mCapture.setVisibility(View.INVISIBLE);
        mCameraChange.setVisibility(View.INVISIBLE);
        mBtnBack.setVisibility(View.VISIBLE);
        mBtnConfirm.setVisibility(View.VISIBLE);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mCameraView.stopRecord();
        mCameraView.onDestroy();
        mSensorControler= null;
    }
}
