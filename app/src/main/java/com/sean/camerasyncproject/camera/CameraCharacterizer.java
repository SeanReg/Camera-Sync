package com.sean.camerasyncproject.camera;

import android.graphics.Camera;
import android.graphics.ImageFormat;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.util.Size;

import java.util.HashMap;

public class CameraCharacterizer {
    private static final int  mFRONT_CAMERA = 1;
    private static final int  mBACK_CAMERA = 2;

    private CameraManager mCameraManager = null;
    private CameraType    mCurCamera     = null;
    private String        mCurCameraId   = null;

    private final HashMap<Integer, Size[]> supportedRes = new HashMap<>();

    /**
     * The enum Format.
     */
    public enum Format {
        /**
         * Jpeg format.
         */
        JPEG        (ImageFormat.JPEG),
        /**
         * Rgb 565 format.
         */
        RGB_565     (ImageFormat.RGB_565),
        /**
         * Yuv 420 888 format.
         */
        YUV_420_888 (ImageFormat.YUV_420_888),
        /**
         * Yuv 422 888 format.
         */
        YUV_422_888 (ImageFormat.YUV_422_888),
        /**
         * Yuv 444 888 format.
         */
        YUV_444_888 (ImageFormat.YUV_444_888);

        private int mFormatInt = 0;

        private Format(int n) {
            mFormatInt = n;
        }

        /**
         * Gets format.
         *
         * @return the format
         */
        public int getFormat() {
            return mFormatInt;
        }
    }

    /**
     * The enum Camera type.
     */
    public enum CameraType {
        /**
         * Front camera camera type.
         */
        FRONT_CAMERA(mFRONT_CAMERA),
        /**
         * Back camera camera type.
         */
        BACK_CAMERA(mBACK_CAMERA);

        private int mCamType = 0;

        CameraType(int type) {
            mCamType = type;
        }

        /**
         * Gets camera type.
         *
         * @return the camera type
         */
        public int getCameraType() {
            return mCamType;
        }
    }

    /**
     * Constructs a new CameraCharacterizer object that helps with getting the characteristics
     * for the camera
     *
     * @param cameraManager the system's CameraManager
     * @param cameraType    the desired type of camera - front facing camera or back camera
     * @throws IllegalArgumentException thrown if the provided cameraType is an invalid type
     */
    public CameraCharacterizer(CameraManager cameraManager, CameraType cameraType) throws IllegalArgumentException {
        mCameraManager = cameraManager;
        setCurrentCamera(cameraType);
    }

    /**
     * Finds a cameraID with the specified camera characteristic
     * @param camCharacteristicsKey the type of characteristic to search for
     * @param camCharacteristics the desired setting for the camCharacteristicKey
     * @return a string containing the Camera ID that has the desired characteristic. Null if none was found
     */
    private String getCameraId(CameraCharacteristics.Key<Integer> camCharacteristicsKey, int camCharacteristics) {
        try {
            //Loop through camera devices
            for (String cameraId : mCameraManager.getCameraIdList()) {
                //Get characteristics of device
                CameraCharacteristics characteristics = mCameraManager.getCameraCharacteristics(cameraId);
                int checkChar = characteristics.get(camCharacteristicsKey);

                //See Characteristic id matches the desired Characteristic
                if (camCharacteristics == checkChar)
                    return cameraId;
            }
        } catch(CameraAccessException e) {
            e.printStackTrace();
        }

        return null;
    }

    /**
     * Finds the Camera ID for the front facing camera
     * @return a string containing the Camera ID of the front facing camera. Null if none was found
     */
    private String getFrontFacingCameraId() {
        //Get the front facing camera ID
        return getCameraId(CameraCharacteristics.LENS_FACING, CameraCharacteristics.LENS_FACING_FRONT);
    }

    /**
     * Finds the Camera ID for the back facing camera
     * @return a string containing the Camera ID of the back facing camera. Null if none was found
     */
    private String getBackFacingCameraId() {
        //Get the back facing camera ID
        return getCameraId(CameraCharacteristics.LENS_FACING, CameraCharacteristics.LENS_FACING_BACK);
    }

    /**
     * Sets the current camera type that's being used by the CameraCharacterizer
     * @param cameraType the desired type of camera - front facing camera or back camera
     * @throws IllegalArgumentException thrown if the provided cameraType is an invalid type
     */
    private void setCurrentCamera(CameraType cameraType) throws IllegalArgumentException {
        String id = null;
        switch (cameraType.getCameraType()) {
            case mFRONT_CAMERA:
                id = getFrontFacingCameraId();
                break;
            case mBACK_CAMERA:
                id = getBackFacingCameraId();
                break;
            default:
                throw new IllegalArgumentException("Argument provided is not a type of camera!");
        }

        mCurCameraId = id;
        mCurCamera   = cameraType;
    }

    /**
     * Gets the maximum resolution for the specified image format
     *
     * @param imageFormat the ImageFormat constant that the resolution will be used for
     * @return a Size containing the width and height of the maximum resolution supported by the camera for the specified ImageFormat
     */
    public Size getMaxResolution(Format imageFormat) {
        //if (mCurCameraId == null) throw new IllegalStateException("Current camera has not been set!");

        //Look at output sizes for specified image format
        Size[] sizes      = getResolutionSizes(imageFormat);

        //Largest seems to always be the first index - so no need to waste time searching
        Size   largestRes = sizes[0];
/*        if (sizes.length > 0) {
            //Find the largest resolution size
            largestRes = sizes[0];
            for (Size size : sizes) {
                //Compare by total area
                if (size.getHeight() * size.getWidth() > largestRes.getHeight() * largestRes.getWidth()) {
                    //This one is larger, so set it
                    largestRes = size;
                }
            }
        }*/

        return largestRes;
    }

    /**
     * Gets the minimum resolution that will fit within a resolution contraint
     *
     * @param resConstraint the minimum required resolution to fit
     * @return the minimum resolution that is greater than resContraint and fits within the same aspect ratio
     */
    public Size getMinFitResolution(Size resConstraint) {
        //Get biggest resolution
        Size resolution = getMaxResolution(Format.JPEG);

        //List of resolution sizes
        Size[] sizes = getResolutionSizes(Format.JPEG);

        //Calculate aspect ratio for largest possible resolution (best aspect)
        float aspectRatio = resolution.getHeight() / (float)resolution.getWidth();

        //Find the minimum that satisfies the constraint
        Size bestFit = sizes[0];
        for (Size size : sizes) {
            //Check if the aspect ratio matches and if it is greater than the constraint
            if (size.getWidth() >= resConstraint.getWidth() && size.getHeight() >= resConstraint.getHeight() && size.getHeight() == Math.floor(size.getWidth() * aspectRatio)) {
                //Check if this is smaller than the current bestFit
                if (size.getHeight() * size.getWidth() < bestFit.getHeight() * bestFit.getWidth())
                    bestFit = size;
            }
        }

        return bestFit;
    }

    public boolean getFacingFront() {
        CameraCharacteristics characteristics = null;
        try {
            characteristics = mCameraManager.getCameraCharacteristics(mCurCameraId);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
        return (characteristics.get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_FRONT);
    }

    /**
     * Gets a list of resolutions supported for the specified imageFormat
     *
     * @param imageFormat the imageFormat to get resolutions for
     * @return an array of Size[] that contains the resolutions supported
     */
    public Size[] getResolutionSizes(Format imageFormat) {
        try {
            //Store results in Hashmap so we can reference them quickly
            if (!supportedRes.containsKey(imageFormat)){
                //Get the characteristic for resolution size
                CameraCharacteristics characteristics = mCameraManager.getCameraCharacteristics(mCurCameraId);
                StreamConfigurationMap streamMap = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);

                Size[] sizes = streamMap.getOutputSizes(imageFormat.getFormat());
                supportedRes.put(imageFormat.getFormat(), sizes);
            }

            return supportedRes.get(imageFormat.getFormat());
        } catch (CameraAccessException e) {

        }

        return null;
    }

    /**
     * Gets the Id of the camera for the camera device that the CameraCharacterizer uses
     *
     * @return a String containing the camera Id for the camera device type specified in the CameraCharacterizer constructor
     */
    public String getCameraId() {
        return mCurCameraId;
    }

    /**
     * Gets camera orientation.
     *
     * @return the camera orientation
     */
    public int getCameraOrientation() {
        CameraCharacteristics characteristics = null;
        try {
            characteristics = mCameraManager.getCameraCharacteristics(mCurCameraId);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
        return characteristics.get(CameraCharacteristics.SENSOR_ORIENTATION);
    }
}
