package com.sean.camerasyncproject.network;

import android.media.MediaCodec;
import android.media.MediaFormat;
import android.util.Pair;

import com.google.android.gms.nearby.connection.Payload;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.nio.ByteBuffer;
import java.util.Arrays;

/**
 * Created by Sean on 4/21/2019.
 */

public class PayloadUtil {
    public enum Desc {
        VIDEO,
        MEDIA_INFO;
    }

    private PayloadUtil() {

    }

    public static Payload encodeFormatPayload(MediaFormat format) {
        byte[] mimeType = format.getString(MediaFormat.KEY_MIME).getBytes();

        ByteBuffer buffer = ByteBuffer.allocate(mimeType.length + 32);

        buffer.putInt(Desc.MEDIA_INFO.ordinal());
        buffer.putInt(mimeType.length);
        buffer.put(mimeType);
        buffer.putInt(format.getInteger(MediaFormat.KEY_FRAME_RATE));
        buffer.putInt(format.getInteger(MediaFormat.KEY_BIT_RATE));
        buffer.putInt(format.getInteger(MediaFormat.KEY_ROTATION));
        buffer.putInt(format.getInteger(MediaFormat.KEY_I_FRAME_INTERVAL));
        buffer.putInt(format.getInteger(MediaFormat.KEY_WIDTH));
        buffer.putInt(format.getInteger(MediaFormat.KEY_HEIGHT));

        return Payload.fromBytes(buffer.array());
    }

    public static MediaFormat decodeFormatPayload(Payload payload) {
        ByteBuffer buffer = ByteBuffer.wrap(payload.asBytes());

        checkCorrectDesc(buffer, Desc.MEDIA_INFO);

        byte[] mimeType = new byte[buffer.getInt()];
        buffer.get(mimeType);

        MediaFormat mediaFormat = new MediaFormat();
        mediaFormat.setString(MediaFormat.KEY_MIME, new String(mimeType));
        mediaFormat.setInteger(MediaFormat.KEY_FRAME_RATE, buffer.getInt());
        mediaFormat.setInteger(MediaFormat.KEY_BIT_RATE, buffer.getInt());
        mediaFormat.setInteger(MediaFormat.KEY_ROTATION, buffer.getInt());
        mediaFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, buffer.getInt());
        mediaFormat.setInteger(MediaFormat.KEY_WIDTH, buffer.getInt());
        mediaFormat.setInteger(MediaFormat.KEY_HEIGHT, buffer.getInt());

        return mediaFormat;
    }

    public static Payload encodeVideoPayload(byte[] videoBuffer, MediaCodec.BufferInfo info) {
        ByteBuffer buffer = ByteBuffer.allocate(videoBuffer.length + 24);

        buffer.putInt(Desc.VIDEO.ordinal());
        buffer.putInt(info.offset);
        buffer.putInt(info.size);
        buffer.putLong(info.presentationTimeUs);
        buffer.putInt(info.flags);
        buffer.put(videoBuffer);

        return Payload.fromBytes(buffer.array());
    }

    public static Pair<byte[], MediaCodec.BufferInfo> decodeVideoPayload(Payload payload) {
        ByteBuffer buffer = ByteBuffer.wrap(payload.asBytes());

        checkCorrectDesc(buffer, Desc.VIDEO);

        MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();
        info.set(buffer.getInt(), buffer.getInt(), buffer.getLong(), buffer.getInt());

        byte[] videoBuffer = new byte[buffer.remaining()];
        buffer.get(videoBuffer);


        return new Pair<>(videoBuffer, info);
    }

    public static Desc getPayloadType(Payload payload) {
        ByteBuffer buffer = ByteBuffer.wrap(payload.asBytes());

        return Desc.values()[buffer.getInt()];
    }

    private static void checkCorrectDesc(ByteBuffer buffer, Desc expected) {
        try {
            if (Desc.values()[buffer.getInt()] != expected)
                throw new IllegalArgumentException("Payload is not a video payload");
        } catch (ArrayIndexOutOfBoundsException e) {
            throw new IllegalArgumentException("Payload is not a valid payload");
        }
    }
}
