package com.zhaocius.previewopengl;

import android.app.Activity;
import android.opengl.GLSurfaceView;
import android.os.Bundle;

public class MainActivity extends Activity {
    private static final String TAG = "MainActivity";
    private CameraGLSurfaceView mGLSurfaceView;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mGLSurfaceView = findViewById(R.id.glsurfaceview);
    }

    @Override
    protected void onResume() {
        // TODO Auto-generated method stub
        super.onResume();
        mGLSurfaceView.bringToFront();
    }

    @Override
    protected void onPause() {
        // TODO Auto-generated method stub
        super.onPause();
        mGLSurfaceView.onPause();
    }

}
