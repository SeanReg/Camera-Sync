package com.sean.camerasyncproject.encoding;

import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaCodecList;
import android.media.MediaFormat;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.Surface;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;

/**
 * Created by Sean on 3/14/2019.
 */

public class H264SurfaceEncoder extends Transcoder<Surface> {
    private static final String TAG = "H264SurfaceEncoder";

    private final MediaFormat mMediaFormat;

    private MediaCodec mMediaCodec = null;

    private Surface mEncoderService = null;

    public H264SurfaceEncoder(Sync sync, int width, int height, int fps, int bitrate, int rotation) {
        super(sync);

        mMediaFormat = MediaFormat.createVideoFormat(VIDEO_TRANSCODER_TYPE, width, height);
        mMediaFormat.setInteger(MediaFormat.KEY_BIT_RATE, bitrate);
        mMediaFormat.setInteger(MediaFormat.KEY_FRAME_RATE, fps);
        mMediaFormat.setInteger(MediaFormat.KEY_ROTATION, rotation);
        mMediaFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1);
        mMediaFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface);
    }

    public MediaFormat getFormat() {
        return mMediaFormat;
    }

    @Override
    public boolean isRunning() {
        return (mMediaCodec != null);
    }

    @Override
    public Surface start() throws IOException {
        if (mMediaCodec != null)
            throw new IllegalStateException("Encoder is already running!");

        mMediaCodec = MediaCodec.createEncoderByType(VIDEO_TRANSCODER_TYPE);
        mMediaCodec.configure(mMediaFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);


        mEncoderService = mMediaCodec.createInputSurface();
        mMediaCodec.setCallback(mCallback);
        mMediaCodec.start();

        return mEncoderService;
    }

    public Surface getSurface() {
        return mEncoderService;
    }

    @Override
    public void putData(byte[] data, MediaCodec.BufferInfo info) {

    }

    @Override
    public void stop() {
        if (mMediaCodec != null) {
            mMediaCodec.stop();
            mMediaCodec.release();
            mMediaCodec = null;
        }
    }

    private final MediaCodec.Callback mCallback = new MediaCodec.Callback() {
        @Override
        public void onInputBufferAvailable(@NonNull MediaCodec mediaCodec, int i) {

        }

        @Override
        public void onOutputBufferAvailable(@NonNull MediaCodec mediaCodec, int i, @NonNull MediaCodec.BufferInfo bufferInfo) {
            ByteBuffer buffer = mediaCodec.getOutputBuffer(i);

            ByteBuffer data = ByteBuffer.allocate(buffer.limit());
            data.put(buffer);
            mSync.onBufferAvailable(data.array(), bufferInfo);

            mediaCodec.releaseOutputBuffer(i, false);

            Log.d(TAG, "Got buffer");
        }

        @Override
        public void onError(@NonNull MediaCodec mediaCodec, @NonNull MediaCodec.CodecException e) {

        }

        @Override
        public void onOutputFormatChanged(@NonNull MediaCodec mediaCodec, @NonNull MediaFormat mediaFormat) {

        }
    };
}
