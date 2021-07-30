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


    /*
    * 经过以上打开相机和设置预览两步，相机就可以正常工作了，相机会源源不断地把摄像头帧数据更新到SurfaceTexture上，即更新到对应的OpenGL纹理上。
    * 但是此时我们并不知道相机数据帧何时会更新到SurfaceTexture，也没有在GLSurfaceView的OnDrawFrame方法中将更新后的纹理渲染到屏幕.
    * 所以并不能在屏幕上看到预览画面。
    * */
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
