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
import android.hardware.Camera;
import android.os.Build;
import android.view.Surface;
import jp.co.cyberagent.android.gpuimage.Rotation;

@TargetApi(Build.VERSION_CODES.JELLY_BEAN)
class Camera1Helper {

    private final int mPreviewWidth;
    private final int mPreviewHeight;

    interface CameraController {
        void onOpened(final Camera camera, final Rotation rotation, final boolean flipHorizontal,
                final boolean flipVertical);
    }

    private boolean mIsFront;
    private boolean mFlashLightOn;
    private final CameraController mCameraController;
    private Camera mCamera;

    Camera1Helper(int previewWidth, int previewHeight, CameraController cameraController,
            boolean isFront) {
        mIsFront = isFront;
        mPreviewWidth = previewWidth;
        mPreviewHeight = previewHeight;
        mCameraController = cameraController;
    }

    boolean startPreview(final Activity activity) {
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
            mCameraController.onOpened(mCamera, getRotation(activity, getCurrentCameraId()),
                    mIsFront, false);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    boolean stopPreview() {
        if (mCamera != null) {
            mCamera.stopPreview();
            mCamera.setPreviewCallback(null);
            mCamera.release();
            mCamera = null;
        }
        return true;
    }

    boolean switchCamera(final Activity activity) {
        if (!stopPreview()) {
            return false;
        }
        mIsFront = !mIsFront;
        return startPreview(activity);
    }

    boolean switchFlash() {
        if (mIsFront) {
            return false;
        }
        mFlashLightOn = !mFlashLightOn;
        if (mFlashLightOn) {
            if (mCamera != null) {
                Camera.Parameters parameters = mCamera.getParameters();
                parameters.setFlashMode(Camera.Parameters.FLASH_MODE_TORCH);
                mCamera.setParameters(parameters);
            }
        } else {
            if (mCamera != null) {
                Camera.Parameters parameters = mCamera.getParameters();
                parameters.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
                mCamera.setParameters(parameters);
            }
        }
        return true;
    }

    private Camera openCamera() {
        return Camera.open(mIsFront ? Camera.CameraInfo.CAMERA_FACING_FRONT
                : Camera.CameraInfo.CAMERA_FACING_BACK);
    }

    private int getCurrentCameraId() {
        return mIsFront ? Camera.CameraInfo.CAMERA_FACING_FRONT
                : Camera.CameraInfo.CAMERA_FACING_BACK;
    }

    private Rotation getRotation(final Activity activity, final int cameraId) {
        int rotation = activity.getWindowManager().getDefaultDisplay().getRotation();
        int activityDegree = 0;
        switch (rotation) {
            case Surface.ROTATION_0:
                activityDegree = 0;
                break;
            case Surface.ROTATION_90:
                activityDegree = 90;
                break;
            case Surface.ROTATION_180:
                activityDegree = 180;
                break;
            case Surface.ROTATION_270:
                activityDegree = 270;
                break;
        }

        int degree;
        Camera.CameraInfo info = getCameraInfo(cameraId);
        if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            degree = (info.orientation + activityDegree) % 360;
        } else {
            degree = (info.orientation - activityDegree + 360) % 360;
        }
        Rotation result;
        switch (degree) {
            case 90:
                result = Rotation.ROTATION_90;
                break;
            case 180:
                result = Rotation.ROTATION_180;
                break;
            case 270:
                result = Rotation.ROTATION_270;
                break;
            case 0:
            default:
                result = Rotation.NORMAL;
                break;
        }
        return result;
    }

    private Camera.CameraInfo getCameraInfo(final int cameraId) {
        Camera.CameraInfo info = new Camera.CameraInfo();
        Camera.getCameraInfo(cameraId, info);
        return info;
    }
}
