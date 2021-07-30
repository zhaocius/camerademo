package com.zhaocius.previewopengl;

import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.util.Log;

class CameraInterface {
    private static final String TAG = "CameraInterface";
    private static volatile CameraInterface instance;
    private CameraInterface(){

    }
    public static CameraInterface getInstance(){
        if(instance == null){
            synchronized (CameraInterface.class){
                if(instance == null){
                    instance = new CameraInterface();

                }

            }
        }
        return instance;

    }

    Camera mCamera;

    public void doOpenCamera() {
        try{
            mCamera=Camera.open(1);
        }catch (Exception e){

        }
    }


    public void doStartPreview(SurfaceTexture mSurface, float v) {
        try{
            mCamera.setDisplayOrientation(90);
            mCamera.setPreviewTexture(mSurface);
            mCamera.startPreview();
        }catch (Exception e){

        }
    }

    public void doStopCamera() {
        Log.d(TAG, "deinitCamera: ");
        try{
            mCamera.stopPreview();
            mCamera.release();
            mCamera = null;
        }catch (Exception e){

        }
    }
}
