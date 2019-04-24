package com.sean.camerasyncproject.camera;

import android.graphics.SurfaceTexture;
import android.media.MediaCodec;
import android.media.MediaFormat;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Pair;
import android.util.Size;
import android.view.Surface;
import android.view.TextureView;

import com.google.android.gms.nearby.connection.Payload;
import com.sean.camerasyncproject.R;
import com.sean.camerasyncproject.encoding.H264Decoder;
import com.sean.camerasyncproject.network.PayloadUtil;

import java.io.IOException;
import java.util.concurrent.LinkedBlockingDeque;

/**
 * Created by Sean on 3/14/2019.
 */

public class RemoteCameraActivity extends CameraActivity {

    private H264Decoder mDecoder = null;

    public static RemoteCameraActivity mInstance;

    private MediaFormat mFormat = null;
    private boolean mSurfaceReady = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mInstance = this;
    }

    @Override
    protected void onTextureReady() {
        mSurfaceReady = true;
        startVideoDecoder();
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

            //fixViewAspect(mCameraView, new Size(mFormat.getInteger(MediaFormat.KEY_WIDTH), mFormat.getInteger(MediaFormat.KEY_HEIGHT)));
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
}
