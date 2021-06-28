package com.zhaocius.previewcam1;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.pm.PackageManager;
import android.hardware.Camera;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    private SurfaceView mSurfaceView;
    private Camera mCamera;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mSurfaceView=findViewById(R.id.sv_camera);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
            if(ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)!= PackageManager.PERMISSION_GRANTED){
                ActivityCompat.requestPermissions(this,new String[]{Manifest.permission.CAMERA},0);
            }else {
                setSurface();
            }
        }else{
            setSurface();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        Log.d(TAG, "onRequestPermissionsResult: ");
        if(requestCode==0){
            boolean isAllGranted=true;
            for(int result: grantResults){
                if(result==-1)
                    isAllGranted=false;
            }
            if(!isAllGranted){
                finish();
            }else{
                setSurface();
            }
        }
    }


    private void setSurface(){
        mSurfaceView.getHolder().addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(SurfaceHolder surfaceHolder) {
                initCamera();

            }

            @Override
            public void surfaceChanged(SurfaceHolder surfaceHolder, int i, int i1, int i2) {
            }

            @Override
            public void surfaceDestroyed(SurfaceHolder surfaceHolder) {
                deinitCamera();
            }
        });
    }

    private void initCamera(){
        Log.d(TAG, "initCamera: ");
        try{
            mCamera=Camera.open(1);
            mCamera.setDisplayOrientation(90);
            mCamera.setPreviewDisplay(mSurfaceView.getHolder());
            mCamera.startPreview();
        }catch (Exception e){

        }
    }

    private void deinitCamera(){
        Log.d(TAG, "deinitCamera: ");
        try{
            mCamera.stopPreview();
            mCamera.release();
            mCamera = null;
        }catch (Exception e){

        }
    }
}
