package com.sean.camerasyncproject.network;

import com.google.android.gms.nearby.connection.Payload;

/**
 * Created by Sean on 4/24/2019.
 */

public interface MessageListener {
    public void onMessageReceived(Session.Client sender, Payload msg);
}