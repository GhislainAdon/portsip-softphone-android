package com.portgo.ui;
//

import android.app.Activity;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.hardware.Camera;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Environment;
import android.util.DisplayMetrics;
import android.hardware.Camera.Size;
import android.hardware.Camera.Size;

import android.view.Display;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;

import com.portgo.R;
import com.portgo.manager.ConfigurationManager;
import com.portgo.util.VideoThumbnailUtils;
import com.portgo.view.VideoPress;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import java.util.Calendar;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

import static com.portgo.util.VideoThumbnailUtils.readPictureDegree;


public class PortActivityVideoReorder extends Activity implements SurfaceHolder.Callback,View.OnClickListener, View.OnTouchListener,VideoPress.OnRecordListener {
    private SurfaceView mSurfaceview;
    private VideoPress mBtnStartStop;
    private Button mBtnCancel,mBtnOk,mBtnSwitch;
    private boolean mStartedFlg = false;
    private boolean mIsPlay = false;
    private MediaRecorder mRecorder;
    private SurfaceHolder mSurfaceHolder;
    private Camera camera;
    private MediaPlayer mediaPlayer;
    private String path;
    protected static final String RECORDER_NAME  = "fdsafasa";
    protected static final String RECORDER_LEN  = "durantion";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_video_recorder);

        mSurfaceview = (SurfaceView) findViewById(R.id.surfaceview);
        mBtnStartStop =  findViewById(R.id.btnStartStop);
        mBtnStartStop.setOnRecordListener(this);
        mBtnSwitch = (Button) findViewById(R.id.btnSwitchCamera);
        mBtnCancel = (Button) findViewById(R.id.btnCancel);
        mBtnOk = (Button) findViewById(R.id.btnOk);
        mBtnSwitch.setOnClickListener(this);
        mBtnOk.setOnClickListener(this);
        mBtnCancel.setOnClickListener(this);

        SurfaceHolder holder = mSurfaceview.getHolder();
        holder.addCallback(this);

    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    boolean backCamara = true;
    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.btnOk:
                try {
                    if(videos.size()>1) {
                        String videoPath = ConfigurationManager.getInstance().getStringValue(this,ConfigurationManager.PRESENCE_VIDEO_PATH,getExternalFilesDir(null).getAbsolutePath());
                        path = videoPath + File.separator + UUID.randomUUID().toString() + ".mp4";
                        VideoThumbnailUtils.appendVideo(this, path, videos);
                        removeCandidateFile();
                    }else{

                    }
                }catch (IOException e){

                }

                File file = new File(path);
                Intent result = new Intent();
                if(file.exists()) {
                    result.putExtra(RECORDER_NAME, file.getName());
                    setResult(Activity.RESULT_OK, result);
                }else{
                    setResult(Activity.RESULT_CANCELED, result);
                }
                finish();
                break;
            case R.id.btnCancel:
                removeCandidateFile();

                mBtnCancel.setVisibility(View.INVISIBLE);
                mBtnOk.setVisibility(View.INVISIBLE);
                mBtnStartStop.setVisibility(View.VISIBLE);
                mBtnSwitch.setVisibility(View.VISIBLE);
                startCameraPreview();
                break;
            case R.id.btnSwitchCamera:
                switchCamara();
                break;
        }
    }

    void removeCandidateFile(){
        for(String path:videos){
            File candidateFile= new File(path);
            if(candidateFile.exists()){
                candidateFile.delete();
            }
        }
        videos.clear();
    }
    void getCamare(){
        int camareNum = Camera.CameraInfo.CAMERA_FACING_BACK;
        if(camera!=null){
            camera.stopPreview();
            camera.setPreviewCallback(null);
            camera.release();
        }
        if(Camera.getNumberOfCameras()>1) {
            camareNum=backCamara?Camera.CameraInfo.CAMERA_FACING_BACK:Camera.CameraInfo.CAMERA_FACING_FRONT;
            camera = Camera.open(camareNum);
        }else {
            camera = Camera.open();
        }

        if (camera != null) {
            deal(camera);
        }
    }

    void startRecord(){
        if(!mStartedFlg) {
            if (mRecorder == null) {
                mRecorder = new MediaRecorder();
            }

            getCamare();
            if(camera!=null){
                camera.unlock();
                mRecorder.setCamera(camera);
            }

            try {
                mRecorder.setAudioSource(MediaRecorder.AudioSource.CAMCORDER);
                mRecorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);

                mRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);

                mRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
                mRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.MPEG_4_SP);

                mRecorder.setVideoSize(pictureSize.width, pictureSize.height);
                mRecorder.setVideoFrameRate(15);
                mRecorder.setVideoEncodingBitRate(3 * 1024 * 1024);
                mRecorder.setOrientationHint(90);

                mRecorder.setMaxDuration(300 * 1000);
                mRecorder.setPreviewDisplay(mSurfaceHolder.getSurface());
                String fileName = UUID.randomUUID().toString();
                path = getExternalFilesDir(null).getAbsolutePath() + "/" + fileName + ".mp4";
                if (path != null) {
                    mRecorder.setOutputFile(path);
                    videos.add(path);
                    mRecorder.prepare();
                    mRecorder.start();
                    mStartedFlg = true;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    void stopRecord(){
        if (mStartedFlg) {
            try {
                mRecorder.stop();
                mRecorder.reset();
                mRecorder.release();
                mRecorder = null;
                if (camera != null) {
                    camera.release();
                    camera = null;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        mStartedFlg = false;
    }

    public void takePhoto(){
        camera.takePicture(null, null, new Camera.PictureCallback() {
            @Override
            public void onPictureTaken(byte[] data, Camera camera) {
                Bitmap bitmap= BitmapFactory.decodeByteArray(data,0,data.length);
                try {
                    camera.stopPreview();
                    android.hardware.Camera.CameraInfo info =
                            new android.hardware.Camera.CameraInfo();
                    android.hardware.Camera.getCameraInfo(backCamara?0:1, info);
                    bitmap = VideoThumbnailUtils.rotaingImageView(info.orientation,bitmap);

                    String videoPath = ConfigurationManager.getInstance().getStringValue(PortActivityVideoReorder.this,ConfigurationManager.PRESENCE_VIDEO_PATH, getExternalFilesDir(null).getAbsolutePath());
                    path = videoPath+ File.separator + UUID.randomUUID().toString() + ".jpg";
                    FileOutputStream fileOutputStream=new FileOutputStream(path);
                    bitmap.compress(Bitmap.CompressFormat.JPEG,85,fileOutputStream);
                    fileOutputStream.close();

                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
    }


    List<String > videos = new LinkedList<>();
    private void switchCamara(){
        {
            if (mIsPlay) {
                return;
            }
            backCamara = !backCamara;
            if (mStartedFlg){
                stopRecord();
                startRecord();
            }else{
                startCameraPreview();
            };
        }
    }

    public void setCameraDisplayOrientation(Activity activity,
                                                   int cameraId, android.hardware.Camera camera) {
        android.hardware.Camera.CameraInfo info =
                new android.hardware.Camera.CameraInfo();
        android.hardware.Camera.getCameraInfo(cameraId, info);

        int rotation = activity.getWindowManager().getDefaultDisplay()
                .getRotation();
        int degrees = 0;
        switch (rotation) {
            case Surface.ROTATION_0: degrees = 0; break;
            case Surface.ROTATION_90: degrees = 90; break;
            case Surface.ROTATION_180: degrees = 180; break;
            case Surface.ROTATION_270: degrees = 270; break;
        }

        int result;
        if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            result = (info.orientation + degrees) % 360;
            result = (360 - result) % 360;  // compensate the mirror
        } else {  // back-facing
            result = (info.orientation - degrees + 360) % 360;
        }
        camera.setDisplayOrientation(result);
    }
    Size pictureSize;
    Size previewSize ;
    public Camera deal(Camera camera){
        setCameraDisplayOrientation(this,backCamara?0:1,camera);
        Camera.Parameters parameters = camera.getParameters();

        //2.x
        int maxSize = 1080;

        pictureSize = getOptimalPictureSize(camera,maxSize);
        previewSize = getOptimalPreviewSize(camera,maxSize);

        parameters.setPictureSize(pictureSize.width, pictureSize.height);
        if (parameters.getSupportedFocusModes().contains(Camera.Parameters. FOCUS_MODE_CONTINUOUS_PICTURE )) {
            parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE) ;
        }
        if (parameters.getSupportedFocusModes().contains(Camera.Parameters. FOCUS_MODE_AUTO )) {
            parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO) ;
        }
        parameters.setPreviewSize(previewSize.width, previewSize.height);
        parameters.setExposureCompensation(6);
        parameters.setRecordingHint(true);
        camera.setParameters(parameters);
        return camera;
    }


    Size getOptimalPictureSize(Camera camera,int maxSize){
        List<Size> supportedPictureSizes
                = camera.getParameters().getSupportedPictureSizes();
        Size pictureSize = supportedPictureSizes.get(0);

        maxSize= maxSize<=0?640:maxSize;
        for(Size size : supportedPictureSizes){
            if(maxSize >= Math.max(size.width,size.height)){
                break;
            }
            pictureSize = size;
        }
        return pictureSize;
    }

    Size getOptimalPreviewSize(Camera camera,int maxSize){
        Size optimalSize = null;
        float optimalRate = 100;
        List<Size> supportedPreviewSizes
                = camera.getParameters().getSupportedPreviewSizes();

        WindowManager windowManager = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
        Display display = windowManager.getDefaultDisplay();
        DisplayMetrics displayMetrics = new DisplayMetrics();
        display.getMetrics(displayMetrics);

        int width = display.getWidth();
        int height= display.getHeight();
        final float realRate  = (float)width/height;

        for(Size size: supportedPreviewSizes){
            float tempRate  = (float)size.width/size.height;
            if(Math.abs(tempRate-realRate)<Math.abs(optimalRate-realRate)&&(maxSize >= Math.max(size.width,size.height))){
                optimalRate=tempRate;
                optimalSize = size;
            }
        }
        return  optimalSize;
    }

    public static String getDate() {
        Calendar ca = Calendar.getInstance();
        int year = ca.get(Calendar.YEAR);
        int month = ca.get(Calendar.MONTH);
        int day = ca.get(Calendar.DATE);
        int minute = ca.get(Calendar.MINUTE);
        int hour = ca.get(Calendar.HOUR);
        int second = ca.get(Calendar.SECOND);

        String date = "" + year + (month + 1) + day + hour + minute + second;
        return date;
    }

    private void startCameraPreview(){
        getCamare();
        if(camera!=null) {
            try {
                camera.setPreviewDisplay(mSurfaceHolder);
            } catch (IOException e) {
                e.printStackTrace();
            }
            camera.startPreview();
            camera.cancelAutoFocus();
        }
    }

    @Override
    public void surfaceCreated(SurfaceHolder surfaceHolder) {
        mSurfaceHolder = surfaceHolder;
        surfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        startCameraPreview();
    }

    @Override
    public void surfaceChanged(SurfaceHolder surfaceHolder, int i, int i1, int i2) {
        mSurfaceHolder = surfaceHolder;
        if(camera!=null) {
            try {
                camera.stopPreview();
                camera.setPreviewDisplay(mSurfaceHolder);
            } catch (IOException e) {
                e.printStackTrace();
            }
            camera.startPreview();
        }
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder surfaceHolder) {
        mSurfaceview = null;
        mSurfaceHolder = null;
        if (mRecorder != null) {
            mRecorder.release();
            mRecorder = null;
        }
        if (camera != null) {
            camera.release();
            camera = null;
        }
        if (mediaPlayer != null){
            mediaPlayer.release();
            mediaPlayer = null;
        }
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        return false;
    }

    @Override
    public void onShortClick() {
        takePhoto();

        mBtnCancel.setVisibility(View.VISIBLE);
        mBtnOk.setVisibility(View.VISIBLE);
        mBtnSwitch.setVisibility(View.INVISIBLE);
        mBtnStartStop.setVisibility(View.INVISIBLE);

    }

    @Override
    public void OnRecordStartClick() {
        startRecord();
    }

    @Override
    public void OnFinish(int resultCode) {
        mBtnCancel.setVisibility(View.VISIBLE);
        mBtnOk.setVisibility(View.VISIBLE);
        mBtnStartStop.setVisibility(View.INVISIBLE);
        mBtnSwitch.setVisibility(View.INVISIBLE);
        stopRecord();
    }


}