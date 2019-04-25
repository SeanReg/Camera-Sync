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
import android.view.View;

import com.google.android.gms.nearby.connection.Payload;
import com.sean.camerasyncproject.R;
import com.sean.camerasyncproject.encoding.H264Decoder;
import com.sean.camerasyncproject.network.MessageListener;
import com.sean.camerasyncproject.network.PayloadUtil;
import com.sean.camerasyncproject.network.Session;

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

        mCaptureButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Session.getActiveSession().broadcastToClients(RemoteCameraActivity.this, PayloadUtil.encodeTakePicture(mFlashButton.isChecked()));
            }
        });

        mSwitchCamera.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Session.getActiveSession().broadcastToClients(RemoteCameraActivity.this, PayloadUtil.encodeSwitchCamera());
            }
        });

        mInstance = this;
    }

    @Override
    protected void onStart() {
        super.onStart();

        Session.getActiveSession().addMessageListener(PayloadUtil.Desc.MEDIA_INFO, new MessageListener() {
            @Override
            public void onMessageReceived(Session.Client sender, Payload msg) {
                mFormat = PayloadUtil.decodeFormatPayload(msg);

                fixViewAspect(mCameraView, new Size(mFormat.getInteger(MediaFormat.KEY_WIDTH), mFormat.getInteger(MediaFormat.KEY_HEIGHT)));
                startVideoDecoder();
            }
        });

        Session.getActiveSession().addMessageListener(PayloadUtil.Desc.VIDEO, new MessageListener() {
            @Override
            public void onMessageReceived(Session.Client sender, Payload msg) {
                Pair<byte[], MediaCodec.BufferInfo> decoded = PayloadUtil.decodeVideoPayload(msg);

                mDecoder.putData(decoded.first, decoded.second);
            }
        });
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
