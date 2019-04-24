package com.sean.camerasyncproject.network;

import com.google.android.gms.nearby.connection.ConnectionsClient;
import com.google.android.gms.nearby.connection.Payload;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by Sean on 3/14/2019.
 */

public class Session {
    public interface ResponseListener {
        public void onClientDisconnected(Client client);
        public void onMessageReceived(Client sender, Payload msg);
    }

    private final ArrayList<Client> mClients;
    private final ConnectionsClient mConnections;

    public Session(ConnectionsClient connections, Client... clients) {
        mClients = new ArrayList<>(Arrays.asList(clients));
        mConnections = connections;
    }

    public Session(ConnectionsClient connections, List<Client> clients) {
        mClients = new ArrayList<>(clients);
        mConnections = connections;
    }

    public List<Client> getClients() {
        return new ArrayList<>(mClients);
    }

    public void broadcastToClients(Payload payload) {
        for (Client c : mClients) {
            sendToClient(c, payload);
        }
    }

    public void sendToClient(Client client, Payload payload) {
        mConnections.sendPayload(client.getId(), payload);
    }
}
