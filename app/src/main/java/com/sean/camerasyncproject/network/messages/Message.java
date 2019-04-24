package com.sean.camerasyncproject.network.messages;

import com.google.android.gms.nearby.connection.Payload;

/**
 * Created by Sean on 4/23/2019.
 */

public abstract class Message<T> {
    public abstract T decode(Payload payload);
    public abstract Payload encode(T obj);
}
