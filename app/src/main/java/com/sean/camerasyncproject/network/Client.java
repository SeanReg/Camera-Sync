package com.sean.camerasyncproject.network;

import android.support.annotation.NonNull;
import android.util.Log;

import com.google.android.gms.nearby.connection.Payload;
import com.google.android.gms.nearby.connection.PayloadCallback;
import com.google.android.gms.nearby.connection.PayloadTransferUpdate;
import com.sean.camerasyncproject.camera.RemoteCameraActivity;

/**
 * Created by Sean on 3/14/2019.
 */

public class Client extends PayloadCallback {
    private final String mClientId;

    public Client(String clientId) {
        mClientId = clientId;
    }

    public String getId() {
        return mClientId;
    }

    @Override
    public void onPayloadReceived(@NonNull String s, @NonNull Payload payload) {
        RemoteCameraActivity.mInstance.test(payload);
    }

    @Override
    public void onPayloadTransferUpdate(@NonNull String s, @NonNull PayloadTransferUpdate payloadTransferUpdate) {

    }
}
