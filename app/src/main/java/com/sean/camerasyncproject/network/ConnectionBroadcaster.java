package com.sean.camerasyncproject.network;

import android.content.Context;
import android.support.annotation.NonNull;
import android.util.Log;

import com.google.android.gms.common.api.Status;
import com.google.android.gms.nearby.Nearby;
import com.google.android.gms.nearby.connection.AdvertisingOptions;
import com.google.android.gms.nearby.connection.ConnectionInfo;
import com.google.android.gms.nearby.connection.ConnectionLifecycleCallback;
import com.google.android.gms.nearby.connection.ConnectionResolution;
import com.google.android.gms.nearby.connection.Strategy;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;

import java.util.HashMap;
import java.util.List;

/**
 * Created by Sean on 3/14/2019.
 */

public class ConnectionBroadcaster extends DiscoveryService {
    private static final String TAG = "ConnectionBroadcaster";

    private String mConnName = "";
    private boolean mIsActive = false;

    public ConnectionBroadcaster(Context context, StatusListener listener) {
        super(context, listener);
    }

    public void start(String name) {
        if (mIsActive)
            throw new IllegalStateException("Attempting to start when already currently active!");

        mConnName = name;

        AdvertisingOptions advertisingOptions = new AdvertisingOptions.Builder()
                .setStrategy(Strategy.P2P_POINT_TO_POINT).build();

        mIsActive = true;
        Nearby.getConnectionsClient(mContext)
                .startAdvertising(mConnName, mContext.getPackageName(), this, advertisingOptions)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        mListener.onDiscoveryStart(ConnectionBroadcaster.this);
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        mIsActive = false;
                        mListener.onDiscoveryFailed(ConnectionBroadcaster.this, e);
                    }
                });
    }

    public boolean isActive() {
        return mIsActive;
    }

    public void stop() {
        if (!mIsActive)
            throw new IllegalStateException("Attempting to stop when not currently active!");

        Nearby.getConnectionsClient(mContext).stopAdvertising();
        mIsActive = false;
    }
}
