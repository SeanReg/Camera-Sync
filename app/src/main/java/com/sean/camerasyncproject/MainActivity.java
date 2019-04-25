package com.sean.camerasyncproject;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import com.sean.camerasyncproject.camera.HostCameraActivity;
import com.sean.camerasyncproject.camera.RemoteCameraActivity;
import com.sean.camerasyncproject.network.ConnectionBroadcaster;
import com.sean.camerasyncproject.network.ConnectionDiscovery;
import com.sean.camerasyncproject.network.DiscoveryService;
import com.sean.camerasyncproject.network.Session;
import com.sean.camerasyncproject.permissions.PermissionRequester;

import java.nio.charset.Charset;
import java.util.Random;

public class MainActivity extends AppCompatActivity {


    private PermissionRequester mPermission    = null;

    private DiscoveryService mDiscovery = null;

    private ProgressDialog mProgressDialog = null;

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

                startProgressDialog();
            }
        });

        findViewById(R.id.search).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mDiscovery != null)
                    mDiscovery.stop();

                mDiscovery = new ConnectionDiscovery(getApplicationContext(), mListener);
                mDiscovery.start(generatedString);

                startProgressDialog();
            }
        });

        //startActivity(new Intent(this, CameraActivity.class));
    }

    private void startProgressDialog() {
        mProgressDialog = new ProgressDialog(this);
        mProgressDialog.setMessage("Searching for connection");
        mProgressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        mProgressDialog.show();

        mProgressDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialogInterface) {
                if (mDiscovery != null && mDiscovery.isActive())
                    mDiscovery.stop();

                mDiscovery = null;
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();

        //Need to request permissions again
        mPermission = new PermissionRequester(this);
        mPermission.setResultListener(mCameraPermissionListener);
        mPermission.requestPermission(Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.ACCESS_COARSE_LOCATION);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        mPermission.onPermissionResult(requestCode, permissions, grantResults);
    }

    private PermissionRequester.ResultListener mCameraPermissionListener = new PermissionRequester.ResultListener() {
        @Override
        public void onAccessGranted(String permission) {
        }

        @Override
        public void onAccessDenied(String permission) {
            //User doesn't want to use their camera - leave activity
            finish();
        }
    };

    @Override
    protected void onStop() {
        super.onStop();

        if (mDiscovery != null && mDiscovery.isActive()) {
            mDiscovery.stop();
        }

        mDiscovery = null;
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
        public boolean onConnectionDiscovered(DiscoveryService service, Session.Client client) {
            Log.d(MainActivity.class.getSimpleName(), "Found client " + client.getId());
            return true;
        }

        @Override
        public void onConnectionAccepted(DiscoveryService service, Session.Client client) {
            mProgressDialog.hide();
            mDiscovery.stop();

            Session session = service.createActiveSession();

            if (service instanceof ConnectionBroadcaster) {
                startActivity(new Intent(getApplicationContext(), HostCameraActivity.class));
            } else {
                startActivity(new Intent(getApplicationContext(), RemoteCameraActivity.class));
            }
        }
    };
}
