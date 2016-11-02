/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2016 Piasy
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.github.piasy.cameracompat.internal;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.graphics.ImageFormat;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.ImageReader;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.annotation.NonNull;
import android.util.Size;
import android.view.Surface;
import android.view.WindowManager;
import java.util.List;
import jp.co.cyberagent.android.gpuimage.Rotation;

@TargetApi(Build.VERSION_CODES.LOLLIPOP)
class Camera2Helper extends CameraDevice.StateCallback {

    interface CameraController {
        void onOpened(final CameraDevice cameraDevice, final ImageReader imageReader,
                final Handler cameraHandler, final Rotation rotation, final boolean flipHorizontal,
                final boolean flipVertical);

        void onSettingsChanged(final CameraDevice cameraDevice, final List<Surface> targets,
                final boolean isFlashOn, final Handler cameraHandler);
    }

    private String mFrontCameraId;
    private String mBackCameraId;

    private boolean mIsFront;
    private boolean mFlashLightOn;
    private final CameraController mCameraController;
    private CameraDevice mCameraDevice;
    private HandlerThread mBackgroundThread;
    private Handler mCamera2Handler;
    private CameraCaptureSession mCaptureSession;
    private ImageReader mImageReader;
    private List<Surface> mOutputTargets;

    private final WindowManager mWindowManager;
    private final CameraManager mCameraManager;

    private final int mDesiredWidth;
    private final int mDesiredHeight;

    Camera2Helper(Activity activity, CameraController cameraController, int width, int height,
            boolean isFront) throws CameraAccessException {
        mIsFront = isFront;
        mCameraController = cameraController;
        mCameraManager = (CameraManager) activity.getSystemService(Context.CAMERA_SERVICE);
        mWindowManager = activity.getWindowManager();
        mDesiredWidth = width;
        mDesiredHeight = height;
        initCameraIds();
    }

    boolean startPreview(ImageReader.OnImageAvailableListener availableListener) {
        try {
            mBackgroundThread = new HandlerThread("PreviewFragmentV21Thread");
            mBackgroundThread.start();
            mCamera2Handler = new Handler(mBackgroundThread.getLooper());
            Size size = findOptSize(getCurrentCameraId());
            mImageReader = ImageReader.newInstance(size.getWidth(), size.getHeight(),
                    ImageFormat.YUV_420_888, 2);
            mImageReader.setOnImageAvailableListener(availableListener, mCamera2Handler);
            mCameraManager.openCamera(getCurrentCameraId(), this, mCamera2Handler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    void previewSessionStarted(CameraCaptureSession captureSession) {
        mCaptureSession = captureSession;
    }

    void outputTargetChanged(List<Surface> targets) {
        mOutputTargets = targets;
    }

    boolean stopPreview() {
        try {
            if (null != mCaptureSession) {
                mCaptureSession.close();
                mCaptureSession = null;
            }
            if (null != mCameraDevice) {
                mCameraDevice.close();
                mCameraDevice = null;
            }
            if (null != mBackgroundThread) {
                mBackgroundThread.quitSafely();
                mBackgroundThread.join();
                mBackgroundThread = null;
                mCamera2Handler = null;
            }
            mImageReader.setOnImageAvailableListener(null, null);
        } catch (InterruptedException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    boolean switchCamera(ImageReader.OnImageAvailableListener availableListener) {
        if (!stopPreview()) {
            return false;
        }
        mIsFront = !mIsFront;
        return startPreview(availableListener);
    }

    @TargetApi(Build.VERSION_CODES.M)
    boolean switchFlash() {
        if (mIsFront) {
            return false;
        }
        try {
            CameraCharacteristics characteristics =
                    mCameraManager.getCameraCharacteristics(mBackCameraId);
            Boolean available = characteristics.get(CameraCharacteristics.FLASH_INFO_AVAILABLE);
            boolean flashSupported = available == null ? false : available;
            if (!flashSupported) {
                return false;
            }
            mFlashLightOn = !mFlashLightOn;
            mCameraController.onSettingsChanged(mCameraDevice, mOutputTargets, mFlashLightOn,
                    mCamera2Handler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
        return false;
    }

    private void initCameraIds() throws CameraAccessException {
        for (String cameraId : mCameraManager.getCameraIdList()) {
            CameraCharacteristics characteristics =
                    mCameraManager.getCameraCharacteristics(cameraId);

            Integer facing = characteristics.get(CameraCharacteristics.LENS_FACING);
            if (facing != null) {
                if (facing == CameraCharacteristics.LENS_FACING_FRONT) {
                    mFrontCameraId = cameraId;
                } else if (facing == CameraCharacteristics.LENS_FACING_BACK) {
                    mBackCameraId = cameraId;
                }
            }
        }
    }

    private Size findOptSize(String cameraId) throws CameraAccessException {
        CameraCharacteristics characteristics = mCameraManager.getCameraCharacteristics(cameraId);
        StreamConfigurationMap map =
                characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
        if (map == null) {
            return new Size(mDesiredWidth, mDesiredHeight);
        }
        // TODO: 5/30/16 adjust size
        return new Size(mDesiredWidth, mDesiredHeight);
    }

    private Rotation getRotation(String cameraId) {
        int rotation = mWindowManager.getDefaultDisplay().getRotation();
        int degrees = 0;
        switch (rotation) {
            case Surface.ROTATION_0:
                degrees = 0;
                break;
            case Surface.ROTATION_90:
                degrees = 90;
                break;
            case Surface.ROTATION_180:
                degrees = 180;
                break;
            case Surface.ROTATION_270:
                degrees = 270;
                break;
        }

        int result;
        CameraCharacteristics characteristics;
        Integer orientation = null;
        try {
            characteristics = mCameraManager.getCameraCharacteristics(cameraId);
            orientation = characteristics.get(CameraCharacteristics.SENSOR_ORIENTATION);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
        int sensorOrientation = orientation == null ? 0 : orientation;
        if (mIsFront) {
            result = (sensorOrientation + degrees) % 360;
        } else {
            result = (sensorOrientation - degrees + 360) % 360;
        }
        Rotation ret = Rotation.NORMAL;
        switch (result) {
            case 90:
                ret = Rotation.ROTATION_90;
                break;
            case 180:
                ret = Rotation.ROTATION_180;
                break;
            case 270:
                ret = Rotation.ROTATION_270;
                break;
        }
        return ret;
    }

    @Override
    public void onOpened(@NonNull CameraDevice camera) {
        mCameraDevice = camera;
        mCameraController.onOpened(mCameraDevice, mImageReader, mCamera2Handler,
                getRotation(getCurrentCameraId()), mIsFront, false);
    }

    @Override
    public void onDisconnected(@NonNull CameraDevice camera) {
        camera.close();
        mCameraDevice = null;
    }

    @Override
    public void onError(@NonNull CameraDevice camera, int error) {
        camera.close();
        mCameraDevice = null;
    }

    private String getCurrentCameraId() {
        return mIsFront ? mFrontCameraId : mBackCameraId;
    }
}
