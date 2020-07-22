package com.portgo.util.camera;

import android.app.Activity;
import android.graphics.Point;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;

/**
 * Created by cj on 2017/8/2.
 * desc 相机的接口控制类
 */

public interface ICamera {
    /**open the camera*/
    void open(int cameraId);

    void setPreviewTexture(SurfaceTexture texture);
    /**set the camera config*/
    void setConfig(Config config);

    void setOnPreviewFrameCallback(PreviewFrameCallback callback);

    void preview();
    void stopPreview();
    void takePicture(Camera.PictureCallback callback);
    Point getPreviewSize();

    Point getPictureSize();
    /**close the camera*/
    boolean close();

    class Config{
        public float rate=1.778f; //宽高比
        public int minPreviewWidth;
        public int minPictureWidth;
    }

    interface PreviewFrameCallback{
        void onPreviewFrame(byte[] bytes, int width, int height);
    }
}
