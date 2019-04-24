package com.sean.camerasyncproject;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import com.sean.camerasyncproject.camera.HostCameraActivity;
import com.sean.camerasyncproject.camera.RemoteCameraActivity;
import com.sean.camerasyncproject.network.Client;
import com.sean.camerasyncproject.network.ConnectionBroadcaster;
import com.sean.camerasyncproject.network.ConnectionDiscovery;
import com.sean.camerasyncproject.network.DiscoveryService;
import com.sean.camerasyncproject.network.Session;

import java.nio.charset.Charset;
import java.util.Random;

public class MainActivity extends AppCompatActivity {

    private DiscoveryService mDiscovery = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        byte[] array = new byte[7]; // length is bounded by 7
        new Random().nextBytes(array);
        final String generatedString = new String(array, Charset.forName("UTF-8"));

        findViewById(R.id.broadcast).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mDiscovery != null)
                    mDiscovery.stop();

                mDiscovery = new ConnectionBroadcaster(getApplicationContext(), mListener);
                mDiscovery.start(generatedString);
            }
        });

        findViewById(R.id.search).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mDiscovery != null)
                    mDiscovery.stop();

                mDiscovery = new ConnectionDiscovery(getApplicationContext(), mListener);
                mDiscovery.start(generatedString);
            }
        });

        //startActivity(new Intent(this, CameraActivity.class));
    }

    private final DiscoveryService.StatusListener mListener = new DiscoveryService.StatusListener() {
        @Override
        public void onDiscoveryStart(DiscoveryService service) {
            Log.d(MainActivity.class.getSimpleName(), "Started Broadcast");
        }

        @Override
        public void onDiscoveryFailed(DiscoveryService service, Exception e) {
            Log.d(MainActivity.class.getSimpleName(), "Failed to Broadcast " + e.getMessage());
        }

        @Override
        public boolean onConnectionDiscovered(DiscoveryService service, Client client) {
            Log.d(MainActivity.class.getSimpleName(), "Found client " + client.getId());
            return true;
        }

        @Override
        public void onConnectionAccepted(DiscoveryService service, Client client) {
            service.stop();
            Session session = service.createActiveSession();

            if (service instanceof ConnectionBroadcaster) {
                HostCameraActivity.setSession(session);
                startActivity(new Intent(getApplicationContext(), HostCameraActivity.class));
            } else {
                startActivity(new Intent(getApplicationContext(), RemoteCameraActivity.class));
            }
        }
    };
}
