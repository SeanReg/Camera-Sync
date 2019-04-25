package com.sean.camerasyncproject.encoding;

import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.Surface;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.LinkedBlockingDeque;

/**
 * Created by Sean on 3/14/2019.
 */

public class H264Decoder extends Transcoder {
    private final Surface mRenderSurface;

    private final MediaFormat mMediaFormat;

    private MediaCodec mDecoder = null;

    private final LinkedBlockingDeque<Integer> mDecoderQueue = new LinkedBlockingDeque<>();

    public H264Decoder(Surface surface, MediaFormat format) {
        super();

        mRenderSurface = surface;
        mMediaFormat = format;
    }

    @Override
    public boolean isRunning() {
        return (mDecoder != null);
    }

    @Override
    public Void start() throws IOException {
        mDecoder = MediaCodec.createDecoderByType(VIDEO_TRANSCODER_TYPE);
        mDecoder.setCallback(new MediaCodec.Callback() {
            @Override
            public void onInputBufferAvailable(@NonNull MediaCodec mediaCodec, int i) {
                mDecoderQueue.add(i);
            }

            @Override
            public void onOutputBufferAvailable(@NonNull MediaCodec mediaCodec, int i, @NonNull MediaCodec.BufferInfo bufferInfo) {
                mediaCodec.getOutputBuffer(i);

                Log.d("Decoder", "Output buffer");

                mediaCodec.releaseOutputBuffer(i, true);
            }

            @Override
            public void onError(@NonNull MediaCodec mediaCodec, @NonNull MediaCodec.CodecException e) {

            }

            @Override
            public void onOutputFormatChanged(@NonNull MediaCodec mediaCodec, @NonNull MediaFormat mediaFormat) {

            }
        });
        mDecoder.configure(mMediaFormat, mRenderSurface, null, 0);

        mDecoder.start();

        return null;
    }

    @Override
    public void putData(byte[] data, MediaCodec.BufferInfo info) {
        if (!mDecoderQueue.isEmpty()) {
            ByteBuffer buffer = mDecoder.getInputBuffer(mDecoderQueue.peek());
            buffer.put(data);

            mDecoder.queueInputBuffer(mDecoderQueue.poll(), info.offset, info.size, info.presentationTimeUs, info.flags);
        }
    }

    @Override
    public void stop() {

    }
}
