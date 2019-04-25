package com.sean.camerasyncproject.network;

import android.content.Context;
import android.support.annotation.NonNull;

import com.google.android.gms.nearby.Nearby;
import com.google.android.gms.nearby.connection.ConnectionsClient;
import com.google.android.gms.nearby.connection.Payload;
import com.google.android.gms.nearby.connection.PayloadCallback;
import com.google.android.gms.nearby.connection.PayloadTransferUpdate;
import com.sean.camerasyncproject.camera.RemoteCameraActivity;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

/**
 * Created by Sean on 3/14/2019.
 */

public class Session implements MessageListener {
    private static Session mActiveSession = null;

    private final ArrayList<Client> mClients;
    private final HashMap<MessageListener, PayloadUtil.Desc> mMessageListeners = new HashMap<>();

    public Session(Client... clients) {
        this(Arrays.asList(clients));
    }

    public Session(List<Client> clients) {
        mClients = new ArrayList<>(clients);

        for (Client client : mClients) {
            client.setMessageListener(this);
        }
    }

    @Override
    public void onMessageReceived(Client sender, Payload msg) {
        PayloadUtil.Desc payloadType = PayloadUtil.getPayloadType(msg);
        for (MessageListener listener : mMessageListeners.keySet()) {
            if (mMessageListeners.get(listener) == payloadType) {
                listener.onMessageReceived(sender, msg);
            }
        }
    }

    public void addMessageListener(PayloadUtil.Desc type, MessageListener listener) {
        if (!mMessageListeners.containsKey(listener))
            mMessageListeners.put(listener, type);
    }

    public void removeMessageListener(MessageListener listener) {
        mMessageListeners.remove(listener);
    }

    public static void setActiveSession(Session session) {
        mActiveSession = session;
    }

    public static Session getActiveSession() {
        return mActiveSession;
    }

    public List<Client> getClients() {
        return new ArrayList<>(mClients);
    }

    public void broadcastToClients(Context context, Payload payload) {
        for (Client c : mClients) {
            sendToClient(context, c, payload);
        }
    }

    public void sendToClient(Context context, Client client, Payload payload) {
        Nearby.getConnectionsClient(context).sendPayload(client.getId(), payload);
    }

    public static class Client extends PayloadCallback {
        private final String mClientId;

        private MessageListener mMessageListener = null;

        public Client(String clientId) {
            mClientId = clientId;
        }

        public String getId() {
            return mClientId;
        }

        private void setMessageListener(MessageListener messageListener) {
            mMessageListener = messageListener;
        }

        @Override
        public void onPayloadReceived(@NonNull String s, @NonNull Payload payload) {
            if (mMessageListener != null) {
                mMessageListener.onMessageReceived(this, payload);
            }
        }

        @Override
        public void onPayloadTransferUpdate(@NonNull String s, @NonNull PayloadTransferUpdate payloadTransferUpdate) {

        }
    }
}
