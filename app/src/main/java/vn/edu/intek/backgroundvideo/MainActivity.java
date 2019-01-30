package vn.edu.intek.backgroundvideo;

import android.Manifest;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.graphics.Point;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.TotalCaptureResult;
import android.media.CamcorderProfile;
import android.media.MediaRecorder;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.LayoutDirection;
import android.util.SparseIntArray;
import android.view.Display;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.Toast;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {
    // Initialize and loading variables
    private SurfaceTexture mPreviewSurfaceTexture;
    private static final int MY_CAMERA_REQUEST_CODE = 100;
    private CameraDevice mCamera;
    private MediaRecorder mMediaRecorder;
    private Boolean cameraReady = false;
    private Boolean recording = false;
    private static final SparseIntArray DEFAULT_ORIENTATIONS = new SparseIntArray();
    private static final SparseIntArray INVERSE_ORIENTATIONS = new SparseIntArray();
    static {
        DEFAULT_ORIENTATIONS.append(Surface.ROTATION_0, 90);
        DEFAULT_ORIENTATIONS.append(Surface.ROTATION_90, 0);
        DEFAULT_ORIENTATIONS.append(Surface.ROTATION_180, 270);
        DEFAULT_ORIENTATIONS.append(Surface.ROTATION_270, 180);
    }
    static {
        INVERSE_ORIENTATIONS.append(Surface.ROTATION_0, 270);
        INVERSE_ORIENTATIONS.append(Surface.ROTATION_90, 180);
        INVERSE_ORIENTATIONS.append(Surface.ROTATION_180, 90);
        INVERSE_ORIENTATIONS.append(Surface.ROTATION_270, 0);
    }
    String[] PERMISSIONS = {

            android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
            android.Manifest.permission.RECORD_AUDIO,
            android.Manifest.permission.CAMERA
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        // Calculating screen width and height
        Display display = getWindowManager().getDefaultDisplay();
        DisplayMetrics metrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(metrics);
        Point size = new Point();
        display.getSize(size);
        int height = size.y;
        int width = size.x;

        // create texture for video capture
        TextureView previewView = findViewById(R.id.previewView);
        RelativeLayout.LayoutParams params_preview = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT,height/2);
        params_preview.setMargins(0, 0, 0, 0);
        previewView.setLayoutParams(params_preview);
        previewView.setAlpha(0);

        // UI component button
        final Button stop = findViewById(R.id.record);
        // Resizing Button Component
        double stop_db = height * 0.10;
        long stp_h = (long) stop_db;  //explicit type casting required
        int stop_height = (int) stp_h;
        //Applying
        RelativeLayout.LayoutParams params_stop = new RelativeLayout.LayoutParams(width/2,stop_height);
        params_stop.setMargins(0,10,0,0);
        params_stop.addRule(RelativeLayout.CENTER_HORIZONTAL);
        params_stop.addRule(RelativeLayout.BELOW, R.id.previewView);
        stop.setLayoutParams(params_stop);
        stop.setEnabled(false);
        stop.setText(getResources().getString(R.string.initialize));

        previewView.setSurfaceTextureListener(new TextureView.SurfaceTextureListener() {

            @Override
            public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
                if (isExternalStorageWritable()){
                    mPreviewSurfaceTexture = surface;
                    openCamera();
                }
                else {
                    makeToast(getResources().getString(R.string.no_external));
                    stop.setText(getResources().getString(R.string.error));
                }
            }

            @Override
            public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {

            }

            @Override
            public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
                return false;
            }

            @Override
            public void onSurfaceTextureUpdated(SurfaceTexture surface) {

            }
        });

    }
    public void makeToast(String textToast) {
        Toast.makeText(MainActivity.this, textToast, Toast.LENGTH_SHORT).show();
    }

    // Open Camera
    private void openCamera() {
        if (ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {
            CameraManager cameraManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
            try {
                String cameraId = cameraManager.getCameraIdList()[0];
                cameraManager.openCamera(cameraId, mStateCallback, null);
            }
            catch (CameraAccessException a){
                makeToast(a.getLocalizedMessage());
            }

        } else {
            makeToast(getResources().getString(R.string.permission_req));
            requestPermissions(PERMISSIONS,
                    MY_CAMERA_REQUEST_CODE);
        }

    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == MY_CAMERA_REQUEST_CODE) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                CameraManager manager =
                        (CameraManager) getSystemService(CAMERA_SERVICE);
                try {
                    for (String cameraId : manager.getCameraIdList()) {
                        CameraCharacteristics chars
                                = manager.getCameraCharacteristics(cameraId);
                        Integer facing = chars.get(CameraCharacteristics.LENS_FACING);
                        if (facing != null && facing !=
                                CameraCharacteristics.LENS_FACING_FRONT) {
                            if (ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.CAMERA)
                                    == PackageManager.PERMISSION_GRANTED) {
                                openCamera();
                            }
                        }
                    }
                } catch (CameraAccessException e) {
                    makeToast(e.getMessage());
                }

            } else {
                makeToast(getResources().getString(R.string.permission_den));
            }

        }
    }

    private final CameraDevice.StateCallback mStateCallback = new CameraDevice.StateCallback() {

        @Override
        public void onOpened(@NonNull CameraDevice cameraDevice) {
            // This method is called when the camera is opened.  We start camera preview here.
            mCamera = cameraDevice;
            createPreview();
        }

        @Override
        public void onDisconnected(@NonNull CameraDevice cameraDevice) {
        }

        @Override
        public void onError(@NonNull CameraDevice cameraDevice, int error) {
            makeToast(getResources().getString(R.string.camera_error));
        }

    };

    // Create preview for video capture
    private void createPreview(){
        final Surface surface = new Surface(mPreviewSurfaceTexture);
        final Button stop = findViewById(R.id.record);
        List<Surface> surfaces = Arrays.asList(surface);

        try {
            mCamera.createCaptureSession(surfaces, new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(@NonNull CameraCaptureSession session) {

                    try {
                        CaptureRequest.Builder request = mCamera.createCaptureRequest(CameraDevice.TEMPLATE_RECORD);
                        request.addTarget(surface);
                        setUpCaptureRequestBuilder(request);
                        makeToast(getResources().getString(R.string.camera_ready));
                        stop.setText(getResources().getString(R.string.record));

                        session.setRepeatingRequest(request.build(), new CameraCaptureSession.CaptureCallback() {
                            @Override
                            public void onCaptureCompleted(@NonNull  CameraCaptureSession session, @NonNull  CaptureRequest request, @NonNull  TotalCaptureResult result) {
                                cameraReady = true;
                                stop.setEnabled(true);
                                stop.setOnClickListener(new View.OnClickListener() {
                                    @Override
                                    public void onClick(View v) {
                                        if (cameraReady){
                                            SimpleDateFormat formatter = new SimpleDateFormat("yyyy_MM_dd_HH_mm_ss", Locale.US);
                                            Date now = new Date();
                                            String fileName = Environment.getExternalStorageDirectory() + "/" +formatter.format(now) + ".3gp";
                                            if (!recording) {
                                                stop.setText(getResources().getString(R.string.start_rec));
                                                mMediaRecorder = new MediaRecorder();
                                                mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.CAMCORDER);
                                                mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);
                                                mMediaRecorder.setProfile(CamcorderProfile.get(CamcorderProfile.QUALITY_HIGH));
                                                mMediaRecorder.setOutputFile(fileName);
                                                mMediaRecorder.setPreviewDisplay(surface);
                                                mMediaRecorder.setOrientationHint(getCorrectCameraOrientation());

                                                try {
                                                    mMediaRecorder.prepare();
                                                    mMediaRecorder.start();
                                                    recording = true;
                                                } catch (IOException a) {
                                                    makeToast(a.getLocalizedMessage());
                                                }
                                            }
                                            else {
                                                stop.setText(getResources().getString(R.string.record));
                                                if (mMediaRecorder != null){
                                                    try {
                                                        mMediaRecorder.stop();
                                                        mMediaRecorder.reset();
                                                        mMediaRecorder.release();
                                                        recording = false;
                                                        makeToast(getResources().getString(R.string.stop_rec));
                                                    }
                                                    catch (RuntimeException e){
                                                        makeToast(e.getLocalizedMessage());
                                                    }
                                                }
                                            }
                                        }
                                    }
                                });
                            }
                        }, null);
                    }
                    catch (CameraAccessException e){
                        makeToast(e.getLocalizedMessage());
                    }
                }

                @Override
                public void onConfigureFailed(@NonNull CameraCaptureSession session) {
                    makeToast(getResources().getString(R.string.config_fail));

                }
            }, null);
        }
        catch (CameraAccessException a){
            makeToast(a.getLocalizedMessage());
        }
    }

    private void setUpCaptureRequestBuilder(CaptureRequest.Builder builder) {
        builder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO);
    }

    public int getCorrectCameraOrientation() {
        int rotation = getWindowManager().getDefaultDisplay().getRotation();
        int degrees = 0;

        switch (rotation) {
            case Surface.ROTATION_0:
                degrees = 0;
                break;

            case Surface.ROTATION_90:
                degrees = 90;
                break;

            case Surface.ROTATION_180:
                degrees = 180;
                break;

            case Surface.ROTATION_270:
                degrees = 270;
                break;
        }
        int result = 0;
        CameraManager manager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);

        try {
            CameraCharacteristics chars
                    = manager.getCameraCharacteristics("0");
            Integer facing = chars.get(CameraCharacteristics.LENS_FACING);
            if (facing != null && facing !=
                    CameraCharacteristics.LENS_FACING_FRONT) {
                result = (90 - degrees + 360) % 360;
            }
        }
        catch (CameraAccessException e ){
            makeToast(e.getLocalizedMessage());
        }
            return result;
    }

    public boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state)) {
            return true;
        }
        return false;
    }
}
