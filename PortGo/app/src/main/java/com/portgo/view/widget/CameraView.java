package com.portgo.view.widget;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Point;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.opengl.GLSurfaceView;
import android.os.Build;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import com.portgo.util.VideoThumbnailUtils;
import com.portgo.util.camera.CameraController;
import com.portgo.util.drawer.CameraDrawer;
import com.portgo.util.gpufilter.SlideGpuFilterGroup;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

/**
 * Created by cj on 2017/8/1.
 * desc
 */

public class CameraView extends GLSurfaceView implements GLSurfaceView.Renderer, SurfaceTexture.OnFrameAvailableListener, CameraDrawer.OnStopRecordingListener {
    private Context mContext;

    private CameraDrawer mCameraDrawer;
    private CameraController mCamera;

    private int dataWidth = 0, dataHeight = 0;

    private boolean isSetParm = false;

    private int cameraId = 1;//默认使用前置
    private int oriatation;
    public CameraView(Context context) {
        this(context, null);
    }

    public CameraView(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext = context;
        init(mContext);
    }

    private void init(Context context) {
        /**初始化OpenGL的相关信息*/
        setEGLContextClientVersion(2);//设置版本
        setRenderer(this);//设置Renderer
        setRenderMode(RENDERMODE_WHEN_DIRTY);//主动调用渲染
        setPreserveEGLContextOnPause(true);//保存Context当pause时
        setCameraDistance(100);//相机距离

        /**初始化Camera的绘制类*/
        mCameraDrawer = new CameraDrawer(context);
        mCameraDrawer.setOnStopRecordingListener(this);
        /**初始化相机的管理类*/
        mCamera = new CameraController();

    }

    private void open(int cameraId) {
        mCamera.close();
        mCamera.open(cameraId);
        mCameraDrawer.setCameraId(cameraId);
        final Point previewSize = mCamera.getPreviewSize();
        dataWidth = previewSize.x;
        dataHeight = previewSize.y;
        SurfaceTexture texture = mCameraDrawer.getTexture();
        texture.setOnFrameAvailableListener(this);
        mCamera.setPreviewTexture(texture);
        mCamera.preview();
    }
    public void startPreview(){
        if(mCamera!=null){
            mCamera.preview();
        }
    }
    /**
     * 切换前后置摄像头
     * */
    public void switchCamera() {
        cameraId = cameraId == 0 ? 1 : 0;
        mCameraDrawer.switchCamera();
        open(cameraId);
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        mCameraDrawer.onSurfaceCreated(gl, config);
        if (!isSetParm) {
            open(cameraId);
            stickerInit();
        }
        mCameraDrawer.setPreviewSize(dataWidth, dataHeight);
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        mCameraDrawer.onSurfaceChanged(gl, width, height);
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        if (isSetParm) {
            mCameraDrawer.onDrawFrame(gl);
        }
    }

    /**
     * 每次Activity onResume时被调用,第一次不会打开相机
     */
    @Override
    public void onResume() {
        super.onResume();
        if (isSetParm) {
            open(cameraId);
        }
    }

    public void onDestroy() {
        if (mCamera != null) {
            mCamera.close();
        }
    }

    /**
     * 摄像头聚焦
     */
    public void onFocus(Point point, Camera.AutoFocusCallback callback) {
        mCamera.onFocus(point, callback);
    }

    public int getCameraId() {
        return cameraId;
    }

    public int getBeautyLevel() {
        return mCameraDrawer.getBeautyLevel();
    }

    public void changeBeautyLevel(final int level) {
        queueEvent(new Runnable() {
            @Override
            public void run() {
                mCameraDrawer.changeBeautyLevel(level);
            }
        });
    }

    public void startRecord() {
        queueEvent(new Runnable() {
            @Override
            public void run() {
                mCamera.preview();
                mCameraDrawer.setOutputOrientation(oriatation);
                mCameraDrawer.startRecord();
            }
        });
    }

    public void stopRecord() {
        queueEvent(new Runnable() {
            @Override
            public void run() {
                mCameraDrawer.stopRecord();
            }
        });
    }

    public void setSavePath(String path) {
        mCameraDrawer.setSavePath(path);
    }
    public void setOriatation(int screenOriatation){
        oriatation = screenOriatation;
        if(mCameraDrawer !=null){
            mCameraDrawer.setCameraId(cameraId);
        }
    }

    public void resume(final boolean auto) {
        queueEvent(new Runnable() {
            @Override
            public void run() {
                mCameraDrawer.onResume(auto);
            }
        });
    }

    public void pause(final boolean auto) {
        queueEvent(new Runnable() {
            @Override
            public void run() {
                mCameraDrawer.onPause(auto);
            }
        });
    }

    public void onTouch(final MotionEvent event) {
        queueEvent(new Runnable() {
            @Override
            public void run() {
                mCameraDrawer.onTouch(event);
            }
        });
    }

    public void setOnFilterChangeListener(SlideGpuFilterGroup.OnFilterChangeListener listener) {
        mCameraDrawer.setOnFilterChangeListener(listener);
    }

    private void stickerInit() {
        if (!isSetParm && dataWidth > 0 && dataHeight > 0) {
            isSetParm = true;
        }
    }
    public Bitmap rotatePic(Bitmap bitmap){
        //oriatation==0==360 竖直//180 倒转 90右转 //270 左转
        int orientation = oriatation;
        android.hardware.Camera.CameraInfo info =
                new android.hardware.Camera.CameraInfo();
        android.hardware.Camera.getCameraInfo(cameraId, info);

        boolean scalex =false;
        boolean scaley =false;
        switch (orientation){
            case 180: //倒转
//                orientation = info.orientation;
                if(info.facing== Camera.CameraInfo.CAMERA_FACING_FRONT) {
                    orientation = 90;
                }else{
                    orientation = 90;
                }
                break;
            case 90://右转
                if(info.facing== Camera.CameraInfo.CAMERA_FACING_FRONT) {
                    orientation = 0;
                }else{
                    orientation = 0;
                    scaley =true;
                    scalex =true;
                }
                break;
            case 270:// 左转
                if(info.facing== Camera.CameraInfo.CAMERA_FACING_FRONT) {
                    scaley = true;
                    orientation = 0 ;
                }else{
                    orientation = 0;
                }

                break;
            case 0://竖直拿着
            case 360://
                if(info.facing== Camera.CameraInfo.CAMERA_FACING_FRONT) {
                    orientation = 90;
                }else{
                    orientation = 90;
                }
                break;
        }
        if(info.facing== Camera.CameraInfo.CAMERA_FACING_FRONT&&info.orientation!=90){
            scaley =!scaley;
        }

        return  VideoThumbnailUtils.rotaingImageView(orientation,bitmap,scalex,scaley);
    }

    @Override
    public void onFrameAvailable(SurfaceTexture surfaceTexture) {
        this.requestRender();
    }

    public void takePicture(Camera.PictureCallback pictureCallback){
        mCamera.takePicture( pictureCallback);
    }

    @Override
    public void onStopRecording(int recordingStatus) {
        if(mCamera!=null) {
            mCamera.stopPreview();
        }
    }
}
