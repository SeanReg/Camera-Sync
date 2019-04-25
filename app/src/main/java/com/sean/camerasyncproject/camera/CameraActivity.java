package com.sean.camerasyncproject.camera;

import android.content.res.Configuration;
import android.graphics.SurfaceTexture;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Size;
import android.view.TextureView;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ToggleButton;

import com.sean.camerasyncproject.R;
import com.sean.camerasyncproject.encoding.H264SurfaceEncoder;
import com.sean.camerasyncproject.network.PayloadUtil;

/**
 * Created by Sean on 4/23/2019.
 */

public abstract class CameraActivity extends AppCompatActivity {
    protected TextureView mCameraView = null;
    protected ToggleButton mFlashButton = null;
    protected ImageButton mCaptureButton = null;
    protected ImageButton mSwitchCamera = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);

        mCameraView = (TextureView)findViewById(R.id.cameraView);
        mCameraView.setSurfaceTextureListener(mCameraSurfaceListener);
        mCaptureButton = (ImageButton)findViewById(R.id.captureButton);
        mSwitchCamera = (ImageButton)findViewById(R.id.switchCamera);
        mFlashButton = (ToggleButton)findViewById(R.id.flashButton);
    }

    @Override
    protected void onStart() {
        super.onStart();

        if (mCameraView.getSurfaceTexture() != null)
            onTextureReady();
    }

    protected boolean screenIsLandscape() {
        return (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE);
    }

    protected void fixViewAspect(View fixView, Size resolution) {
        if (!screenIsLandscape()) {
            resolution = new Size(resolution.getHeight(), resolution.getWidth());
        }

        if (fixView.getWidth() > resolution.getWidth() || fixView.getHeight() > resolution.getHeight()) {
            //Get the aspect ratio of the camera resolution
            double aspectRatio = resolution.getHeight() / (double)resolution.getWidth();

            int scaledWidth = 0;
            int scaledHeight = 0;
            //Find the width or height constraint
            if (fixView.getWidth() > fixView.getHeight()) {
                scaledWidth  = fixView.getWidth();
                //Scale the height proportionately with the width
                scaledHeight = (int)(fixView.getWidth() * aspectRatio);
            } else {
                //Scale the width proportionately with the height
                scaledWidth  = (int)(fixView.getHeight() * aspectRatio);
                scaledHeight = fixView.getHeight();
            }

            fixView.setScaleX((float)scaledWidth / fixView.getWidth());
            fixView.setScaleX((float)scaledHeight / fixView.getHeight());
        } else {
            //Set the new scales of the TextureView
            fixView.setScaleX((float)resolution.getWidth() / fixView.getWidth());
            fixView.setScaleY((float)resolution.getHeight() / fixView.getHeight());
        }
    }

    protected abstract void onTextureReady();

    private TextureView.SurfaceTextureListener mCameraSurfaceListener = new TextureView.SurfaceTextureListener() {
        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
            onTextureReady();
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
    };
}
