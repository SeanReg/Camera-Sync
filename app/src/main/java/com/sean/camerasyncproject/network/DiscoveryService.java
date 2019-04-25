package com.sean.camerasyncproject.network;

import android.content.Context;
import android.support.annotation.NonNull;
import android.util.Log;

import com.google.android.gms.common.api.Status;
import com.google.android.gms.nearby.Nearby;
import com.google.android.gms.nearby.connection.ConnectionInfo;
import com.google.android.gms.nearby.connection.ConnectionLifecycleCallback;
import com.google.android.gms.nearby.connection.ConnectionResolution;

import java.util.HashMap;
import java.util.List;

/**
 * Created by Sean on 4/20/2019.
 */

public abstract class DiscoveryService extends ConnectionLifecycleCallback {
    private static final String TAG = "DiscoveryService";

    public interface StatusListener {
        public void onDiscoveryStart(DiscoveryService service);
        public void onDiscoveryFailed(DiscoveryService service, Exception e);
        public boolean onConnectionDiscovered(DiscoveryService service, Session.Client pendingClient);
        public void onConnectionAccepted(DiscoveryService service, Session.Client client);
    }

    private Session mPendingSession = new Session();
    private final HashMap<String, Session.Client> mPendingClients = new HashMap<>();

    protected final Context mContext;
    protected final StatusListener mListener;

    public DiscoveryService(Context context, StatusListener listener) {
        mContext = context;
        mListener = listener;
    }

    public abstract void start(String name);
    public abstract void stop();
    public abstract boolean isActive();

    @Override
    public void onConnectionInitiated(@NonNull String name, @NonNull ConnectionInfo connectionInfo) {
        Log.d(TAG, "Connection Initiated by " + name);

        Session.Client pendingClient = new Session.Client(name);
        if (mListener.onConnectionDiscovered(this, pendingClient)) {
            mPendingClients.put(name, pendingClient);
            Nearby.getConnectionsClient(mContext).acceptConnection(pendingClient.getId(), pendingClient);
        } else {
            Nearby.getConnectionsClient(mContext).rejectConnection(pendingClient.getId());
        }
    }

    @Override
    public void onConnectionResult(@NonNull String clientId, @NonNull ConnectionResolution connectionResolution) {
        Log.d(TAG, "Connection result " + connectionResolution.getStatus().getStatusMessage());

        if (connectionResolution.getStatus().isSuccess()) {
            List<Session.Client> curSession = mPendingSession.getClients();
            curSession.add(mPendingClients.get(clientId));
            mPendingSession = new Session(curSession);

            mListener.onConnectionAccepted(this, mPendingClients.get(clientId));
        } else {
            mPendingClients.remove(clientId);
        }
    }

    @Override
    public void onDisconnected(@NonNull String clientId) {
        mPendingClients.remove(clientId);
        mPendingSession = new Session(mPendingClients.values().toArray(new Session.Client[mPendingClients.values().size()]));
    }

    public Session createActiveSession() {
        Session sess = mPendingSession;
        Session.setActiveSession(mPendingSession);
        return sess;
    }
}
