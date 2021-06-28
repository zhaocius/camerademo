package com.zhaocius.previewcamx;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.AspectRatio;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraControl;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.FocusMeteringAction;
import androidx.camera.core.FocusMeteringResult;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.MeteringPoint;
import androidx.camera.core.MeteringPointFactory;
import androidx.camera.core.Preview;
import androidx.camera.core.SurfaceOrientedMeteringPointFactory;
import androidx.camera.core.VideoCapture;
import androidx.camera.core.impl.VideoCaptureConfig;
import androidx.camera.extensions.HdrImageCaptureExtender;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.Size;
import android.view.MotionEvent;
import android.view.OrientationEventListener;
import android.view.Surface;
import android.view.View;
import android.widget.Button;

import com.google.common.util.concurrent.ListenableFuture;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    private PreviewView mPreviewView;
    private Button mCaptureButton;
    private Button mZoomButton;
    private Button mRecordButton;
    private ExecutorService cameraExecutor;
    private Camera camera;
    private ImageCapture imageCapture;
    private VideoCapture videoCapture;
    private CameraControl cameraControl;
    private Preview preview;
    private ImageAnalysis imageAnalysis;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mPreviewView=findViewById(R.id.viewFinder);
        mZoomButton=findViewById(R.id.zoom_button);
        mZoomButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                float max =camera.getCameraInfo().getZoomState().getValue().getMaxZoomRatio();
                float min = camera.getCameraInfo().getZoomState().getValue().getMinZoomRatio();
                Log.d(TAG, "mZoomButton: zoom max == "+max +", min == "+min);
                ListenableFuture<Void> future = cameraControl.setZoomRatio(max);
                future.addListener(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            Log.d(TAG, "run: zoom down");
                            future.get();
                        } catch (ExecutionException e) {
                            e.printStackTrace();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                },cameraExecutor);

            }
        });
        mPreviewView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                Log.d(TAG, "onTouch: x == "+motionEvent.getX()+", y == "+motionEvent.getY());
                MeteringPointFactory factory = new SurfaceOrientedMeteringPointFactory(mPreviewView.getWidth(), mPreviewView.getHeight());
                MeteringPoint point = factory.createPoint(motionEvent.getX(),  motionEvent.getY());
                FocusMeteringAction action = new FocusMeteringAction.Builder(point, FocusMeteringAction.FLAG_AF)
                        // auto calling cancelFocusAndMetering in 5 seconds
                        .setAutoCancelDuration(1, TimeUnit.SECONDS)
                        .build();
                ListenableFuture<FocusMeteringResult> future = cameraControl.startFocusAndMetering(action);
                return false;
            }
        });
        mCaptureButton = findViewById(R.id.camera_capture_button);
        mCaptureButton.setOnClickListener((view) -> takePhoto());

        mRecordButton = findViewById(R.id.record_button);
        mRecordButton.setOnTouchListener(new View.OnTouchListener() {
            @SuppressLint("RestrictedApi")
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                if (motionEvent.getAction() == MotionEvent.ACTION_DOWN) {
                    mRecordButton.setBackgroundColor(Color.GREEN);
                    File video = new File(getExternalCacheDir() + "/" + System.currentTimeMillis() + ".mp4");
                    videoCapture.startRecording(video, cameraExecutor, new VideoCapture.OnVideoSavedCallback() {
                        @Override
                        public void onVideoSaved(@NonNull File file) {
                            Log.d(TAG, "onVideoSaved: ");
                        }

                        @Override
                        public void onError(int videoCaptureError, @NonNull String message, @Nullable Throwable cause) {

                        }
                    });
                } else if (motionEvent.getAction() == MotionEvent.ACTION_UP) {
                    mRecordButton.setBackgroundColor(Color.RED);

                    Log.d(TAG, "Video File stopped");
                }
                return false;

            }
        });
        cameraExecutor = Executors.newSingleThreadExecutor();


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED
            ||ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA,Manifest.permission.RECORD_AUDIO}, 0);
            } else {
                startCamera();
            }
        } else {
            startCamera();
        }


    }

    private int aspectRatio(int width, int height){
        int previewRatio = (int)((double)Math.max(width, height) / Math.min(width, height));
        if (Math.abs(previewRatio - 4.0f / 3.0f) <= Math.abs(previewRatio - 16.0f / 9.0f)) {
            return AspectRatio.RATIO_4_3;
        }
        return AspectRatio.RATIO_16_9;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        cameraExecutor.shutdown();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        Log.d(TAG, "onRequestPermissionsResult: ");
        if (requestCode == 0) {
            boolean isAllGranted = true;
            for (int result : grantResults) {
                if (result == -1)
                    isAllGranted = false;
            }
            if (!isAllGranted) {
                finish();
            } else {
                startCamera();
            }
        }
    }

    @SuppressLint("RestrictedApi")
    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(this);
        cameraProviderFuture.addListener(() -> {

            Log.d(TAG, "startCamera: Thread == "+Thread.currentThread().getName());
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();

                preview = new Preview.Builder().build();
                preview.setSurfaceProvider(mPreviewView.createSurfaceProvider());
                DisplayMetrics metrics= new DisplayMetrics();

                getWindowManager().getDefaultDisplay().getMetrics(metrics);
                int width = metrics.widthPixels;  // 表示屏幕的像素宽度，单位是px（像素）
                int height = metrics.heightPixels;

                int screenAspectRatio = aspectRatio(width, height);
                Log.d(TAG, "Preview aspect ratio: $screenAspectRatio");

                //        int rotation = mPreviewView.getDisplay().getRotation();
                ImageCapture.Builder builder = new ImageCapture.Builder();
                //Vendor-Extensions (The CameraX extensions dependency in build.gradle)
                HdrImageCaptureExtender hdrImageCaptureExtender = HdrImageCaptureExtender.create(builder);
                // Query if extension is available (optional).
                if (hdrImageCaptureExtender.isExtensionAvailable(CameraSelector.DEFAULT_BACK_CAMERA)) {
                    // Enable the extension if available.
                    Log.d(TAG, "hdrImageCaptureExtender: ");
                    hdrImageCaptureExtender.enableExtension(CameraSelector.DEFAULT_BACK_CAMERA);
                }

                imageCapture = builder.setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                        // We request aspect ratio but no resolution to match preview config, but letting
                        // CameraX optimize for whatever specific resolution best fits our use cases
                        .setTargetAspectRatio(screenAspectRatio)
                        // Set initial target rotation, we will have to call this again if rotation changes
                        // during the lifecycle of this use case
                        .setTargetRotation(Surface.ROTATION_90)
                        .build();

                VideoCaptureConfig videoCaptureConfig= new VideoCaptureConfig.Builder().setTargetRotation(Surface.ROTATION_90).getUseCaseConfig();
                videoCapture=new VideoCapture(videoCaptureConfig);
                Log.d(TAG, "onCreate: videoCapture == "+videoCapture);

                OrientationEventListener orientationEventListener = new OrientationEventListener((Context)this) {
                    @Override
                    public void onOrientationChanged(int orientation) {
                        int rotation;
                        // Monitors orientation values to determine the target rotation value
                        if (orientation >= 45 && orientation < 135) {
                            rotation = Surface.ROTATION_270;
                        } else if (orientation >= 135 && orientation < 225) {
                            rotation = Surface.ROTATION_180;
                        } else if (orientation >= 225 && orientation < 315) {
                            rotation = Surface.ROTATION_90;
                        } else {
                            rotation = Surface.ROTATION_0;
                        }
//                Log.d(TAG, "onOrientationChanged: "+rotation);
                        //将要保存的图片旋转到和拍摄时一样的角度。
                        imageCapture.setTargetRotation(rotation);
                    }
                };

                if (orientationEventListener.canDetectOrientation()) {
                    orientationEventListener.enable();
                } else {
                    orientationEventListener.disable();//注销
                    Log.d(TAG, "当前设备不支持手机旋转");
                }


                imageAnalysis =
                        new ImageAnalysis.Builder()
                                .setTargetResolution(new Size(480, 640))
                                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                                .build();

                imageAnalysis.setAnalyzer(cameraExecutor, new ImageAnalysis.Analyzer() {
                    @Override
                    public void analyze(@NonNull ImageProxy image) {
                        int rotationDegrees = image.getImageInfo().getRotationDegrees();
                        // insert your code here.
                        /*
                        Log.d(TAG, "Thread == "+Thread.currentThread().getName()+", analyze: rotatoin == "+rotationDegrees +", width == "+image.getWidth()+", height == "+image.getHeight());
                        Log.d(TAG, "analyze: "+image.getFormat());
                        ByteBuffer buffer=((image.getPlanes())[0]).getBuffer();
                        byte[] bytes = new byte[buffer.remaining()];
                        buffer.get(bytes);

                        ByteBuffer buffer1=((image.getPlanes())[1]).getBuffer();
                        byte[] bytes1 = new byte[buffer1.remaining()];
                        buffer1.get(bytes1);

                        ByteBuffer buffer2=((image.getPlanes())[2]).getBuffer();
                        byte[] bytes2 = new byte[buffer2.remaining()];
                        buffer2.get(bytes2);

                        Log.d(TAG, "analyze: bytes =="+bytes.length+", bytes1 == "+bytes1.length+", bytes2 == "+bytes2.length);
                        byte[] ee = new byte[bytes.length + bytes2.length ];
                        System.arraycopy(bytes, 0, ee, 0, bytes.length);
                        System.arraycopy(bytes2, 0, ee, bytes.length, bytes2.length);

                        File jpgfile = new File(getExternalCacheDir() + "/" + System.currentTimeMillis() + ".jpg");
                        FileOutputStream fos = null;
                        try {
                                fos = new FileOutputStream(jpgfile);

                                YuvImage yuvImage = new YuvImage(ee, ImageFormat.NV21, 640, 480, null);
                                yuvImage.compressToJpeg(new Rect(0, 0, yuvImage.getWidth(), yuvImage.getHeight()), 100, fos);

                                fos.flush();
                                Log.d(TAG, "save jpg file success " + jpgfile.getAbsolutePath());
                        }catch (Exception e){
                            e.printStackTrace();
                        }finally {
                            if (fos != null ){
                                try {
                                    fos.close();
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                            }
                        }


                         */
                        image.close();
                    }
                });

                cameraProvider.unbindAll();
                camera=cameraProvider.bindToLifecycle(MainActivity.this, CameraSelector.DEFAULT_BACK_CAMERA, preview,imageCapture,videoCapture);
                cameraControl = camera.getCameraControl();

            } catch (Exception e) {
                e.printStackTrace();
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void takePhoto() {
        File photo = new File(getExternalCacheDir() + "/" + System.currentTimeMillis() + ".jpg");

        ImageCapture.OutputFileOptions outputOptions = new ImageCapture.OutputFileOptions.Builder(photo)
                .setMetadata(new ImageCapture.Metadata())
                .build();

        imageCapture.takePicture(outputOptions,cameraExecutor, new ImageCapture.OnImageSavedCallback(){

            @Override
            public void onImageSaved(@NonNull ImageCapture.OutputFileResults outputFileResults) {
                Log.d(TAG, "onImageSaved: Thread == "+Thread.currentThread().getName());
            }

            @Override
            public void onError(@NonNull ImageCaptureException exception) {

            }
        });

    }

    public static String getDate() {
        Date now = new Date();
        return simpleDateFormat.format(now);
    }
    public static final String SIMPLE_DATE_TIME_FORMAT = "yyyy-MM-dd_HH_mm_ss";
    private static final SimpleDateFormat simpleDateFormat = new SimpleDateFormat(SIMPLE_DATE_TIME_FORMAT, Locale.CHINA);
}
