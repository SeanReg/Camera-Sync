package com.sean.camerasyncproject.encoding;

import android.media.MediaCodec;
import android.media.MediaFormat;

import java.io.IOException;

/**
 * Created by Sean on 3/14/2019.
 */

public abstract class Transcoder<T> {
    public interface Sync {
        public void onBufferAvailable(byte[] data, MediaCodec.BufferInfo info);
    }

    protected static final String VIDEO_TRANSCODER_TYPE = "video/avc";

    protected final Sync mSync;

    public Transcoder() {
        mSync = new Sync() {
            @Override
            public void onBufferAvailable(byte[] data, MediaCodec.BufferInfo info) {

            }
        };
    }

    public Transcoder(Sync sync) {
        mSync = sync;
    }

    public abstract boolean isRunning();
    public abstract T start() throws IOException;
    public abstract void putData(byte[] data, MediaCodec.BufferInfo info);
    public abstract void stop();
}
