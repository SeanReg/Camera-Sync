package com.sean.camerasyncproject.camera;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.TotalCaptureResult;
import android.media.Image;
import android.media.ImageReader;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Size;
import android.util.SparseIntArray;
import android.view.Surface;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;

public class CameraHandle {
    /**
     * Static reference to the single instance of the CameraHandle class - Singleton
     */
    private static CameraHandle mCameraHandle = null;

    private static final Size MAX_CAPTURE_SIZE = new Size(1920, 1080);

    //Device and session references
    private CameraCaptureSession  mCaptureSession     = null;
    private CameraDevice          mCameraDevice       = null;
    private CameraCharacterizer   mCharacterizer      = null;

    /**
     * The M record surfaces.
     */
    Surface[] mRecordSurfaces = null;

    private ImageReader mImageReader = null;

    //Threads
    private Handler       mRequestHandler = null;
    private HandlerThread mRequestThread = null;

    //Callback listners
    private CameraStatusCallback  mCameraStatsListener = null;

    private static final SparseIntArray ORIENTATIONS = new SparseIntArray();
    static {
        ORIENTATIONS.append(Surface.ROTATION_0, 0);
        ORIENTATIONS.append(Surface.ROTATION_90, 90);
        ORIENTATIONS.append(Surface.ROTATION_180, 180);
        ORIENTATIONS.append(Surface.ROTATION_270, 270);
    }

    /**
     * Interface to get the status of the camera
     */
    public interface CameraStatusCallback {
        /**
         * On connected.
         */
        public void onConnected();

        /**
         * On ended.
         */
        public void onEnded();

        /**
         * On image captured.
         *
         * @param capturedImage the captured image
         */
        public void onImageCaptured(Bitmap capturedImage);
    }

    //Private - only one instance - Singleton design
    private CameraHandle() {
    }

    /**
     * The default instance of the CameraHandle
     *
     * @return the default instance of the CameraHandle
     */
    public static CameraHandle getInstance() {
        if (mCameraHandle == null) mCameraHandle = new CameraHandle();
        return mCameraHandle;
    }

    /**
     * Attempts to open and connect to the system's camera device
     *
     * @param manager    the system's CameraManager
     * @param cameraType a CameraCharacterizer constant denoting the desired type of camera (Front or Back)
     * @throws CameraAccessException thrown if there was an error with opening the camera device
     * @throws SecurityException     user has not granted permissions for the camera
     */
    public void openCamera(CameraManager manager, CameraCharacterizer.CameraType cameraType) throws CameraAccessException, SecurityException {
        if (mCameraDevice != null) {
            stop();
        }

        try {
            //Ask for access to the camera
            mCharacterizer = new CameraCharacterizer(manager, cameraType);
            manager.openCamera(mCharacterizer.getCameraId(), mCameraListener, null);

            //Start the thread - used later by the capture session
            createCameraThread();
        } catch(IllegalArgumentException e) {
            e.printStackTrace();
        }
    }

    /**
     * Check if the camera has been opened and is connected
     *
     * @return true if the camera device is opened otherwise false
     */
    public boolean getCameraConnected() {
        return (mCameraDevice != null);
    }

    /**
     * Begins an active camera feed that draws frames to the specified surfaces
     *
     * @param recordSurfaces a list of surfaces to draw the camera frames to
     * @throws IllegalStateException    if the camera has not yet been opened
     * @throws IllegalArgumentException if there was an error when accessing the camera device
     * @throws CameraAccessException    if there was an error when attempting to communicate with the camera device
     */
    public void startFeed(final Surface... recordSurfaces) throws IllegalStateException, IllegalArgumentException, CameraAccessException {
        if (mCameraDevice == null) throw new IllegalStateException("Camera device not opened!");

        if (recordSurfaces.length == 0) throw new IllegalArgumentException("Must have at least one surface!");

        try {
            //Create an image reader for still capturing
            Size resolution = mCharacterizer.getMinFitResolution(MAX_CAPTURE_SIZE); //mCharacterizer.getMaxResolution(ImageFormat.JPEG);
            mImageReader = ImageReader.newInstance(resolution.getWidth(), resolution.getHeight(), ImageFormat.JPEG, 2);
            mImageReader.setOnImageAvailableListener(mImageReaderListener, null);

            //Add the image reader to the list of draw surfaces
            mRecordSurfaces = recordSurfaces;
            ArrayList<Surface> surfaceTargets = new ArrayList<Surface>(Arrays.asList(recordSurfaces));
            surfaceTargets.add(mImageReader.getSurface());

            //Start a capture session with the specified output surfaces
            mCameraDevice.createCaptureSession(surfaceTargets, mCaptureSessionListener, null);
        } catch(CameraAccessException e) {
            //Stop the camera and notify the caller
            stop();
            throw e;
        }
    }

    /**
     * Gets the minmum resolution supported by the camera that is greater
     * than a specified minimum
     *
     * @param minimumRes the minimum resolution size to be considered
     * @return the optimal resolution supported by the camera that is >= the minimumRes
     */
    public Size getSupportedResolution(Size minimumRes) {
        if (mCharacterizer == null) return null;

        return mCharacterizer.getMinFitResolution(minimumRes);
    }

    /**
     * Begins an  image capture from the current camera feed. The resulting image is returned by the CameraStatusListener
     *
     * @param useFlash if true then enables flash for the image capture. Otherwise flash is disabled
     * @throws IllegalStateException thrown when the camera feed has not been started. Also thrown if a CameraStatusListener has not been registered for the CameraHandle
     * @throws CameraAccessException thrown if there was an error with opening the camera device
     */
    public void captureImage(boolean useFlash) throws IllegalStateException, CameraAccessException {
        if (mCaptureSession == null) throw new IllegalStateException("Camera feed has not been started!");
        if (mCameraStatsListener == null) throw new IllegalStateException("CameraStatusListener has not been registered");

        try {
            //Build a capture request
            CaptureRequest.Builder request = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
            request.addTarget(mImageReader.getSurface());

            //Set auto focus mode
            request.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
            if (useFlash) {
                request.set(CaptureRequest.FLASH_MODE, CaptureRequest.FLASH_MODE_SINGLE);
            } else  {
                request.set(CaptureRequest.FLASH_MODE, CaptureRequest.FLASH_MODE_OFF);
            }
            request.set(CaptureRequest.JPEG_ORIENTATION, mCharacterizer.getCameraOrientation());

            //Stop old preview request and do capture request
            mCaptureSession.stopRepeating();
            mCaptureSession.capture(request.build(), mCapturedListener, null);
        } catch(CameraAccessException e) {
            //Stop the camera and notify the caller
            stop();
            throw e;
        }
    }

    /**
     * Stops any current camera feeds and releases the camera device and capture session
     */
    public void stop() {

        //Cleanup session
        if (mCaptureSession != null) {
            mCaptureSession.close();
        }

        //Release device
        if (mCameraDevice != null) {
            mCameraDevice.close();
        }

        //Tell the CameraStatusListener that the camera has been stopped
        if (mCameraStatsListener != null) {
            mCameraStatsListener.onEnded();
        }

        stopCameraThread();

        mCameraDevice        = null;
        mCaptureSession      = null;
        //mCameraStatsListener = null;
    }

    /**
     * Create a thread and a message handler to control the capture session
     */
    private void createCameraThread() {
        if (mRequestThread != null) stopCameraThread();

        //Create a thread for the capture session to be handled on
        mRequestThread = new HandlerThread("CameraRequests");
        mRequestThread.start();
        mRequestHandler = new Handler(mRequestThread.getLooper());
    }

    /**
     * End the camera thread if it is currently running
     */
    private void stopCameraThread() {
        if (mRequestThread == null) return;

        //Ask looper to stop
        mRequestThread.quitSafely();
        try {
            //Block until stopped
            mRequestThread.join();
        }catch (InterruptedException e) {
            //Interrupt triggered
        }

        mRequestThread       = null;
        mRequestHandler      = null;
    }

    /**
     * Registers a callback listner to get the status of the camera
     *
     * @param cameraStatus a callback listner to get the status of the camera
     */
    public void setCameraStatusListener(CameraStatusCallback cameraStatus) {
        mCameraStatsListener = cameraStatus;
    }

    /**
     * Callback when the camera device's status has been changed - Opened or Closed
     */
    private CameraDevice.StateCallback mCameraListener = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(CameraDevice camera) {
            //Camera device has been opened
            mCameraDevice = camera;

            if (mCameraStatsListener != null) {
                //Alert the callback listener that the camera has been connected
                mCameraStatsListener.onConnected();
            }
        }

        @Override
        public void onDisconnected(CameraDevice camera) {

        }

        @Override
        public void onError(CameraDevice camera, int error) {

        }
    };

    /**
     * Callback to handle the completion of the image capture request
     */
    private CameraCaptureSession.CaptureCallback mCapturedListener = new CameraCaptureSession.CaptureCallback() {
        @Override
        public void onCaptureCompleted(CameraCaptureSession session, CaptureRequest request, TotalCaptureResult result) {
            super.onCaptureCompleted(session, request, result);

            //End the camera session since an image has been captured
            //stop();
        }
    };

    /**
     * Callback when the ImageReader has received the captured image from the CaptureSession
     */
    private ImageReader.OnImageAvailableListener mImageReaderListener = new ImageReader.OnImageAvailableListener() {
        @Override
        public void onImageAvailable(ImageReader reader) {
            //Reader has newly captured image

            Image capture = reader.acquireLatestImage();
            //Alert caller that their image has been captured
            if (mCameraStatsListener != null) {
                Bitmap bmp = createBitmap(capture);
                mCameraStatsListener.onImageCaptured(bmp);
            }

            capture.close();

            stop();
        }
    };

    private Bitmap createBitmap(Image image) {
        Bitmap bmp = null;
        if(image.getFormat() == ImageFormat.JPEG)
        {
            //JPEGs - Simple just need to copy buffer
            ByteBuffer buffer = image.getPlanes()[0].getBuffer();
            byte[] jpegByteData = new byte[buffer.remaining()];
            buffer.get(jpegByteData);
            bmp = BitmapFactory.decodeByteArray(jpegByteData, 0, jpegByteData.length, null);
        } else {
            //Multi-planar image formats are mor complicated
            final Image.Plane[] planes = image.getPlanes();
            final ByteBuffer buffer = planes[0].getBuffer();

            //Get the distance between pixels and the total row length
            int pixelStride = planes[0].getPixelStride();
            int rowStride = planes[0].getRowStride();
            //Calculate amount of padding that's been added
            int rowPadding = rowStride - pixelStride * image.getWidth();

            // create bitmap
            bmp = Bitmap.createBitmap(image.getWidth() + rowPadding / Math.max(1, pixelStride), image.getHeight(), Bitmap.Config.RGB_565);
            bmp.copyPixelsFromBuffer(buffer);
        }

        return bmp;
    }

    /**
     * Callback after the CaptureSession has been setup
     */
    private CameraCaptureSession.StateCallback mCaptureSessionListener = new CameraCaptureSession.StateCallback() {
        @Override
        public void onConfigured(CameraCaptureSession session) {
            mCaptureSession = session;

            try {
                //Start a continuous capture request to draw frames to the surfaces
                CaptureRequest.Builder request = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
                for(Surface recSurface : mRecordSurfaces) {
                    request.addTarget(recSurface);
                }
                request.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);

                //Request continuous frames from the camera
                mCaptureSession.setRepeatingRequest(request.build(), null, mRequestHandler);
            } catch (CameraAccessException e) {
                //Stop the camera
                stop();
            }
        }

        @Override
        public void onConfigureFailed(CameraCaptureSession session) {
            //Capture session failed to open
        }
    };
}
