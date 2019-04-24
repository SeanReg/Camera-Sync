package com.sean.camerasyncproject.permissions;

import android.app.Activity;
import android.content.pm.PackageManager;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;

import java.util.ArrayList;

public class PermissionRequester {
    private Activity mContext       = null;
    private ResultListener  mResultListener = null;

    /**
     * Callback interface to notify of the user's choice to deny or allow the permission
     */
    public interface ResultListener {
        /**
         * On access granted.
         *
         * @param permission the permission
         */
        public void onAccessGranted(String permission);

        /**
         * On access denied.
         *
         * @param permission the permission
         */
        public void onAccessDenied(String permission);
    }

    /**
     * Constructs a PermissionRequester object that checks current permissions and
     * prompts the user to allow permissions
     *
     * @param context the Activity context that the user should be prompted in
     */
    public PermissionRequester(@NonNull Activity context) {
        mContext = context;
    }

    /**
     * Checks if the user has allowed the specified permission
     *
     * @param permission the Manifest.permission to check
     * @return true if the user has granted access to the permission. Otherwise false
     */
    public boolean hasPermission(String permission) {
        int permissionCheck = ContextCompat.checkSelfPermission(mContext, permission);

        return (permissionCheck == PackageManager.PERMISSION_GRANTED);
    }

    /**
     * Requests the specified list of permissions
     *
     * @param permissions a list of permission to ask the user for
     * @throws IllegalArgumentException the illegal argument exception
     */
    public void requestPermission(String... permissions) throws IllegalArgumentException {
        if (permissions.length == 0) return;

        //Check if user already has all of the permissions being requested
        ArrayList<String> requestPermissions = new ArrayList<>();

        for (String permission : permissions) {
            if (!hasPermission(permission)) {
                //Don't have this permission - so add it to the list
                requestPermissions.add(permission);
            }
        }
        //We have every permission so we can leave
        if (requestPermissions.size() == 0) return;

        String[] requestPermissionsArr = requestPermissions.toArray(new String[requestPermissions.size()]);
        ActivityCompat.requestPermissions(mContext, requestPermissionsArr, 1);
    }

    /**
     * Sets the callback listener that will recieve the permission prompt results
     *
     * @param resultListener the ResultListener to notify of the permission prompt result
     */
    public void setResultListener(ResultListener resultListener) {
        mResultListener = resultListener;
    }

    /**
     * The Activity's onPermissionResult should call PermissionRequester.onPermissionResult to handle
     * the permission request results
     * NOTE: NEEDS TO BE RE-THOUGHT
     *
     * @param requestCode  the result Id code
     * @param permissions  the permissions that were asked for
     * @param grantResults the results of the permission prompt
     */
    public void onPermissionResult(int requestCode, String permissions[], int[] grantResults) {
        for (int i = 0; i < permissions.length; ++i) {
            if (mResultListener != null) {
                if (grantResults[i] == PackageManager.PERMISSION_GRANTED) {
                    mResultListener.onAccessGranted(permissions[i]);
                } else {
                    mResultListener.onAccessDenied(permissions[i]);
                }
            }
        }
    }
}
