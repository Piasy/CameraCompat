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
import android.hardware.Camera;
import android.os.Build;
import com.github.piasy.cameracompat.CameraCompat;
import jp.co.cyberagent.android.gpuimage.Rotation;

@TargetApi(Build.VERSION_CODES.JELLY_BEAN)
class Camera1Helper extends CameraHelper {

    private final CameraController mCameraController;
    private final Camera1PreviewCallback mPreviewCallback;
    private Camera mCamera;
    Camera1Helper(int previewWidth, int previewHeight, int activityRotation, boolean isFront,
            CameraController cameraController, Camera1PreviewCallback previewCallback) {
        super(previewWidth, previewHeight, activityRotation, isFront);
        mCameraController = cameraController;
        mPreviewCallback = previewCallback;
    }

    @Override
    protected boolean startPreview() {
        try {
            mCamera = openCamera();
            Camera.Parameters parameters = mCamera.getParameters();
            // TODO adjust by getting supportedPreviewSizes and then choosing
            // the best one for screen size (best fill screen)
            if (parameters.getSupportedFocusModes()
                    .contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO)) {
                parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO);
            }
            parameters.setPreviewSize(mPreviewWidth, mPreviewHeight);
            mCamera.setParameters(parameters);
            mCamera.setPreviewCallback(mPreviewCallback);
            mCameraController.onOpened(mCamera, getRotation(), mIsFront, false);
        } catch (RuntimeException e) {
            CameraCompat.onError(CameraCompat.ERR_UNKNOWN);
            return false;
        }
        return true;
    }

    @Override
    protected boolean stopPreview() {
        if (mCamera == null) {
            return true;
        }
        try {
            mCamera.stopPreview();
            mCamera.setPreviewCallback(null);
            mCamera.release();
            mCamera = null;
        } catch (RuntimeException e) {
            CameraCompat.onError(CameraCompat.ERR_UNKNOWN);
            return false;
        }
        return true;
    }

    @Override
    protected int getSensorDegree() {
        return getCameraInfo(getCurrentCameraId()).orientation;
    }

    @Override
    protected boolean canOperateFlash() {
        return mCamera != null;
    }

    @Override
    protected void doOpenFlash() throws RuntimeException {
        Camera.Parameters parameters = mCamera.getParameters();
        parameters.setFlashMode(Camera.Parameters.FLASH_MODE_TORCH);
        mCamera.setParameters(parameters);
    }

    @Override
    protected void doCloseFlash() throws RuntimeException {
        Camera.Parameters parameters = mCamera.getParameters();
        parameters.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
        mCamera.setParameters(parameters);
    }

    private Camera openCamera() {
        return Camera.open(mIsFront ? Camera.CameraInfo.CAMERA_FACING_FRONT
                : Camera.CameraInfo.CAMERA_FACING_BACK);
    }

    private int getCurrentCameraId() {
        return mIsFront ? Camera.CameraInfo.CAMERA_FACING_FRONT
                : Camera.CameraInfo.CAMERA_FACING_BACK;
    }

    private Camera.CameraInfo getCameraInfo(final int cameraId) {
        Camera.CameraInfo info = new Camera.CameraInfo();
        Camera.getCameraInfo(cameraId, info);
        return info;
    }

    interface CameraController {
        void onOpened(final Camera camera, final Rotation rotation, final boolean flipHorizontal,
                final boolean flipVertical);
    }
}
