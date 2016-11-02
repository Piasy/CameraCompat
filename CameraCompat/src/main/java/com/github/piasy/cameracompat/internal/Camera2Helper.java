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
import android.text.TextUtils;
import android.util.Size;
import android.view.Surface;
import com.github.piasy.cameracompat.CameraCompat;
import com.github.piasy.cameracompat.internal.events.CameraAccessError;
import java.util.List;
import jp.co.cyberagent.android.gpuimage.Rotation;

@TargetApi(Build.VERSION_CODES.LOLLIPOP)
class Camera2Helper extends CameraHelper {

    private final CameraController mCameraController;
    private final Camera2PreviewCallback mPreviewCallback;
    private final CameraManager mCameraManager;
    private String mFrontCameraId;
    private String mBackCameraId;
    private CameraDevice mCameraDevice;
    private HandlerThread mBackgroundThread;
    private Handler mCamera2Handler;
    private CameraCaptureSession mCaptureSession;
    private ImageReader mImageReader;
    private final CameraDevice.StateCallback mCameraCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(@NonNull CameraDevice camera) {
            try {
                mCameraDevice = camera;
                mCameraController.onOpened(mCameraDevice, mImageReader, mCamera2Handler,
                        getRotation(), mIsFront, false);
            } catch (IllegalStateException e) {
                CameraCompat.onError(CameraCompat.ERR_UNKNOWN);
            }
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
    };
    private List<Surface> mOutputTargets;

    Camera2Helper(int previewWidth, int previewHeight, int activityRotation, boolean isFront,
            CameraController cameraController, CameraManager cameraManager,
            Camera2PreviewCallback previewCallback) throws CameraAccessException {
        super(previewWidth, previewHeight, activityRotation, isFront);
        mCameraController = cameraController;
        mCameraManager = cameraManager;
        mPreviewCallback = previewCallback;
        initCameraIds();
    }

    void previewSessionStarted(CameraCaptureSession captureSession) {
        mCaptureSession = captureSession;
    }

    void outputTargetChanged(List<Surface> targets) {
        mOutputTargets = targets;
    }

    @Override
    protected boolean startPreview() {
        try {
            mBackgroundThread = new HandlerThread("PreviewFragmentV21Thread");
            mBackgroundThread.start();
            mCamera2Handler = new Handler(mBackgroundThread.getLooper());
            Size size = findOptSize(getCurrentCameraId());
            mImageReader = ImageReader.newInstance(size.getWidth(), size.getHeight(),
                    ImageFormat.YUV_420_888, 2);
            mImageReader.setOnImageAvailableListener(mPreviewCallback, mCamera2Handler);
            mCameraManager.openCamera(getCurrentCameraId(), mCameraCallback, mCamera2Handler);
        } catch (SecurityException | CameraAccessException | IllegalStateException e) {
            CameraCompat.onError(CameraCompat.ERR_PERMISSION);
            return false;
        } catch (RuntimeException e) {
            // http://crashes.to/s/3a4227c2262
            // http://crashes.to/s/17d9761180d
            CameraCompat.onError(CameraCompat.ERR_UNKNOWN);
            return false;
        }
        return true;
    }

    @Override
    protected boolean stopPreview() {
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
            if (mImageReader != null) {
                // http://crashes.to/s/099a8255d2b
                mImageReader.setOnImageAvailableListener(null, null);
            }
        } catch (InterruptedException e) {
            CameraCompat.onError(CameraCompat.ERR_UNKNOWN);
            return false;
        }
        return true;
    }

    @Override
    protected int getSensorDegree() {
        CameraCharacteristics characteristics;
        Integer orientation = null;
        try {
            characteristics = mCameraManager.getCameraCharacteristics(getCurrentCameraId());
            orientation = characteristics.get(CameraCharacteristics.SENSOR_ORIENTATION);
        } catch (CameraAccessException | SecurityException e) {
            CameraCompat.onError(CameraCompat.ERR_PERMISSION);
        }
        return orientation == null ? 0 : orientation;
    }

    @Override
    protected boolean canOperateFlash() {
        return mCameraDevice != null;
    }

    @Override
    protected void doOpenFlash() throws RuntimeException {
        try {
            operateFlash(true);
        } catch (CameraAccessException e) {
            throw new CameraAccessError();
        }
    }

    @Override
    protected void doCloseFlash() throws RuntimeException {
        try {
            operateFlash(false);
        } catch (CameraAccessException e) {
            throw new CameraAccessError();
        }
    }

    private void operateFlash(boolean isOpen) throws CameraAccessException, SecurityException {
        CameraCharacteristics characteristics =
                mCameraManager.getCameraCharacteristics(mBackCameraId);
        Boolean available = characteristics.get(CameraCharacteristics.FLASH_INFO_AVAILABLE);
        boolean flashSupported = available == null ? false : available;
        if (!flashSupported) {
            return;
        }
        mCameraController.onSettingsChanged(mCameraDevice, mOutputTargets, isOpen, mCamera2Handler);
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
            return new Size(mPreviewWidth, mPreviewHeight);
        }
        // TODO: 5/30/16 adjust size
        return new Size(mPreviewWidth, mPreviewHeight);
    }

    private String getCurrentCameraId() throws IllegalStateException {
        String id = mIsFront ? mFrontCameraId : mBackCameraId;
        if (TextUtils.isEmpty(id)) {
            throw new IllegalStateException("Get a null camera id: " + mIsFront);
        }
        return id;
    }

    interface CameraController {
        void onOpened(final CameraDevice cameraDevice, final ImageReader imageReader,
                final Handler cameraHandler, final Rotation rotation, final boolean flipHorizontal,
                final boolean flipVertical);

        void onSettingsChanged(final CameraDevice cameraDevice, final List<Surface> targets,
                final boolean isFlashOn, final Handler cameraHandler);
    }
}
