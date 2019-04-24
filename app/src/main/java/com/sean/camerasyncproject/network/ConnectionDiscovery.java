package com.sean.camerasyncproject.network;

import android.content.Context;
import android.support.annotation.NonNull;
import android.util.Log;

import com.google.android.gms.nearby.Nearby;
import com.google.android.gms.nearby.connection.ConnectionInfo;
import com.google.android.gms.nearby.connection.ConnectionLifecycleCallback;
import com.google.android.gms.nearby.connection.ConnectionResolution;
import com.google.android.gms.nearby.connection.DiscoveredEndpointInfo;
import com.google.android.gms.nearby.connection.DiscoveryOptions;
import com.google.android.gms.nearby.connection.EndpointDiscoveryCallback;
import com.google.android.gms.nearby.connection.Strategy;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;

/**
 * Created by Sean on 4/20/2019.
 */

public class ConnectionDiscovery extends DiscoveryService {
    private static final String TAG = "ConnectionDiscovery";

    private String mConnName = "";

    public ConnectionDiscovery(Context context, StatusListener listener) {
        super(context, listener);
    }

    @Override
    public void start(String name) {
        mConnName = name;

        DiscoveryOptions discoveryOptions =
                new DiscoveryOptions.Builder().setStrategy(Strategy.P2P_POINT_TO_POINT).build();
        Nearby.getConnectionsClient(mContext)
                .startDiscovery(mContext.getPackageName(), mDiscoveryCallback, discoveryOptions)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        mListener.onDiscoveryStart(ConnectionDiscovery.this);
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        mListener.onDiscoveryFailed(ConnectionDiscovery.this, e);
                    }
                });
    }

    @Override
    public void stop() {
        Nearby.getConnectionsClient(mContext).stopDiscovery();
    }

    @Override
    public boolean isActive() {
        return false;
    }

    private final EndpointDiscoveryCallback mDiscoveryCallback = new EndpointDiscoveryCallback() {
        @Override
        public void onEndpointFound(@NonNull String endPoint, @NonNull DiscoveredEndpointInfo discoveredEndpointInfo) {
            Nearby.getConnectionsClient(mContext).requestConnection(mConnName, endPoint, ConnectionDiscovery.this);
        }

        @Override
        public void onEndpointLost(@NonNull String s) {

        }
    };
}
