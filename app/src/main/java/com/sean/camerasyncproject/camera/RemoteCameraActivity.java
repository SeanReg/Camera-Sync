package com.sean.camerasyncproject.camera;

import android.graphics.SurfaceTexture;
import android.media.MediaCodec;
import android.media.MediaFormat;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Pair;
import android.view.Surface;
import android.view.TextureView;

import com.google.android.gms.nearby.connection.Payload;
import com.sean.camerasyncproject.R;
import com.sean.camerasyncproject.encoding.H264Decoder;
import com.sean.camerasyncproject.network.PayloadUtil;

import java.io.IOException;

/**
 * Created by Sean on 3/14/2019.
 */

public class RemoteCameraActivity extends AppCompatActivity {

    private TextureView mCameraView = null;

    private H264Decoder mDecoder = null;

    public static RemoteCameraActivity mInstance;

    private MediaFormat mFormat = null;
    private boolean mSurfaceReady = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);

        mInstance = this;

        mCameraView = (TextureView)findViewById(R.id.cameraView);
        mCameraView.setSurfaceTextureListener(mCameraSurfaceListener);
    }

    @Override
    protected void onStop() {
        super.onStop();

        if (mDecoder != null) {
            mDecoder.stop();
            mDecoder = null;
        }

        mSurfaceReady = false;
        mFormat = null;
    }

    public void test(Payload videoData) {
        if (PayloadUtil.getPayloadType(videoData) == PayloadUtil.Desc.MEDIA_INFO) {
            mFormat = PayloadUtil.decodeFormatPayload(videoData);
            startVideoDecoder();
            return;
        }

        if (mDecoder != null) {
            Pair<byte[], MediaCodec.BufferInfo> decoded = PayloadUtil.decodeVideoPayload(videoData);

            mDecoder.putData(decoded.first, decoded.second);
        }
    }

    private void startVideoDecoder() {
        if (mFormat == null || !mSurfaceReady) {
            return;
        }

        mDecoder = new H264Decoder(new Surface(mCameraView.getSurfaceTexture()), mFormat);
        try {
            mDecoder.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private TextureView.SurfaceTextureListener mCameraSurfaceListener = new TextureView.SurfaceTextureListener() {
        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
            mSurfaceReady = true;
            startVideoDecoder();
        }

        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {

        }

        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
            mSurfaceReady = false;
            return false;
        }

        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture surface) {

        }
    };
}
