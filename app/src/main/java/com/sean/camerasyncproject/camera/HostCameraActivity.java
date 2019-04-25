package com.sean.camerasyncproject.camera;

import android.Manifest;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Camera;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraManager;
import android.media.CamcorderProfile;
import android.media.MediaCodec;
import android.media.MediaFormat;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.util.Size;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ToggleButton;

import com.google.android.gms.nearby.connection.Payload;
import com.sean.camerasyncproject.R;
import com.sean.camerasyncproject.encoding.H264SurfaceEncoder;
import com.sean.camerasyncproject.encoding.Transcoder;
import com.sean.camerasyncproject.network.MessageListener;
import com.sean.camerasyncproject.network.PayloadUtil;
import com.sean.camerasyncproject.network.Session;
import com.sean.camerasyncproject.permissions.PermissionRequester;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * The type Camera activity.
 */
public class HostCameraActivity extends CameraActivity {

    private PermissionRequester mPermission    = null;

    private CameraCharacterizer.CameraType mCameraType = CameraCharacterizer.CameraType.BACK_CAMERA;

    private H264SurfaceEncoder mEncoder = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mCaptureButton.setOnClickListener(mCaptureListener);
        mSwitchCamera.setOnClickListener(mSwitchCameraListener);

        CamcorderProfile profile = CamcorderProfile.get(CamcorderProfile.QUALITY_480P);
        mEncoder = new H264SurfaceEncoder(mEncoderSync, profile.videoFrameWidth, profile.videoFrameHeight, profile.videoFrameRate, 1000000, 90);
        Session.getActiveSession().broadcastToClients(HostCameraActivity.this, PayloadUtil.encodeFormatPayload(mEncoder.getFormat()));

        Session.getActiveSession().addMessageListener(PayloadUtil.Desc.TAKE_PICTURE, new MessageListener() {
            @Override
            public void onMessageReceived(Session.Client sender, Payload msg) {
                mFlashButton.setChecked(PayloadUtil.decodeTakePicture(msg));
                mCaptureButton.callOnClick();
            }
        });

        Session.getActiveSession().addMessageListener(PayloadUtil.Desc.SWITCH_CAMERA, new MessageListener() {
            @Override
            public void onMessageReceived(Session.Client sender, Payload msg) {
                mSwitchCamera.callOnClick();
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onTextureReady() {
        startCamera(mCameraType);
    }

    private final Transcoder.Sync mEncoderSync = new Transcoder.Sync() {
        @Override
        public void onBufferAvailable(byte[] data, MediaCodec.BufferInfo info) {
            Session.getActiveSession().broadcastToClients(HostCameraActivity.this, PayloadUtil.encodeVideoPayload(data, info));
        }
    };

    @Override
    protected void onPause() {
        super.onPause();

        mEncoder.stop();
        CameraHandle.getInstance().stop();
    }

    private void startCamera(CameraCharacterizer.CameraType cameraType) {
        CameraHandle camHandler = CameraHandle.getInstance();

        //if (camHandler.getCameraConnected()) return;

        mCameraType = cameraType;
        CameraManager manager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        camHandler.setCameraStatusListener(mCameraStatus);

        try {
            camHandler.openCamera(manager, cameraType);
        } catch (SecurityException e) {
            //Need to request permissions again
            mPermission = new PermissionRequester(this);
            mPermission.setResultListener(mCameraPermissionListener);
            mPermission.requestPermission(Manifest.permission.CAMERA);
        } catch (CameraAccessException e2) {

        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        mPermission.onPermissionResult(requestCode, permissions, grantResults);
    }

    private PermissionRequester.ResultListener mCameraPermissionListener = new PermissionRequester.ResultListener() {
        @Override
        public void onAccessGranted(String permission) {
            //Camera granted - redo connection
            startCamera(mCameraType);
        }

        @Override
        public void onAccessDenied(String permission) {
            //User doesn't want to use their camera - leave activity
            finish();
        }
    };

    private CameraHandle.CameraStatusCallback mCameraStatus = new CameraHandle.CameraStatusCallback() {
        @Override
        public void onConnected() {
            CameraHandle camHandler = CameraHandle.getInstance();

            //Start the camera preview feed
            try {
                Surface previewSurface = new Surface(mCameraView.getSurfaceTexture());

                if (!mEncoder.isRunning())
                    mEncoder.start();
                camHandler.startFeed(previewSurface,  mEncoder.getSurface());

                Size previewSize = new Size(mCameraView.getWidth(), mCameraView.getHeight());
                if (!screenIsLandscape()) {
                    previewSize = new Size(previewSize.getHeight(), previewSize.getWidth());
                }
                Size supportedSize = camHandler.getSupportedResolution(previewSize);
                fixViewAspect(mCameraView, supportedSize);
            }catch (CameraAccessException e) {
                finish();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onEnded() {

        }

        @Override
        public void onImageCaptured(Bitmap capturedImage) {
            Log.d("CameraActivity", "Got image from ImageReader");

            MediaStore.Images.Media.insertImage(getContentResolver(), capturedImage, "", "");  // Saves the image.

//            FileOutputStream fStream = null;
//            File filePath = null;
//            try {
//                filePath = File.createTempFile("bitmap", ".png", directory);
//                fStream = new FileOutputStream(filePath);
//                capturedImage.compress(Bitmap.CompressFormat.JPEG, 100, fStream);
//                fStream.close();
//            } catch (IOException e) {
//
//            }
        }
    };

    private ImageButton.OnClickListener mCaptureListener = new ImageButton.OnClickListener() {
        @Override
        public void onClick(View v) {
            try {
                CameraHandle.getInstance().captureImage(getWindowManager(), mFlashButton.isChecked());
            }catch (CameraAccessException e) {
                finish();
            }
        }
    };

    private ImageButton.OnClickListener mSwitchCameraListener = new ImageButton.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (mCameraType.getCameraType() == CameraCharacterizer.CameraType.BACK_CAMERA.getCameraType()) {
                startCamera(CameraCharacterizer.CameraType.FRONT_CAMERA);
            } else {
                startCamera(CameraCharacterizer.CameraType.BACK_CAMERA);
            }
        }
    };
}
