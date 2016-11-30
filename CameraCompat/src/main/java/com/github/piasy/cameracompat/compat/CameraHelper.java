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

package com.github.piasy.cameracompat.compat;

import android.view.Surface;
import com.github.piasy.cameracompat.CameraCompat;
import com.github.piasy.cameracompat.compat.events.CameraAccessError;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import jp.co.cyberagent.android.gpuimage.Rotation;

/**
 * Created by Piasy{github.com/Piasy} on 7/21/16.
 */

abstract class CameraHelper {
    protected final int mPreviewWidth;
    protected final int mPreviewHeight;
    protected boolean mIsFront;

    private final int mActivityRotation;
    private boolean mFlashLightOn;

    CameraHelper(int previewWidth, int previewHeight, int activityRotation, boolean isFront) {
        mPreviewWidth = previewWidth;
        mPreviewHeight = previewHeight;
        mActivityRotation = activityRotation;
        mIsFront = isFront;
    }

    final Rotation getRotation() {
        int activityDegree = 0;
        switch (mActivityRotation) {
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

        int sensorDegree = getSensorDegree();
        int degree;
        if (mIsFront) {
            degree = (sensorDegree + activityDegree) % 360;
        } else {
            degree = (sensorDegree - activityDegree + 360) % 360;
        }
        Rotation ret = Rotation.NORMAL;
        switch (degree) {
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

    boolean switchCamera() {
        if (!stopPreview()) {
            return false;
        }
        mIsFront = !mIsFront;
        if (mIsFront) {
            mFlashLightOn = false;
        }
        return startPreview();
    }

    boolean switchFlash() {
        if (mIsFront || !canOperateFlash()) {
            return false;
        }
        try {
            mFlashLightOn = !mFlashLightOn;
            if (mFlashLightOn) {
                doOpenFlash();
            } else {
                doCloseFlash();
            }
        } catch (CameraAccessError | SecurityException e) {
            CameraCompat.onError(CameraCompat.ERR_PERMISSION);
            return false;
        } catch (RuntimeException e) {
            CameraCompat.onError(CameraCompat.ERR_UNKNOWN);
            return false;
        }

        return true;
    }

    protected PreviewSize findOptSize(int desiredWidth, int desiredHeight) {
        List<PreviewSize> supportedSize = getSupportedSize();
        List<PreviewSize> qualifiedSize = new ArrayList<>();
        for (int i = 0, size = supportedSize.size(); i < size; i++) {
            PreviewSize option = supportedSize.get(i);
            if (desiredWidth > desiredHeight) {
                if (option.getWidth() >= desiredWidth && option.getHeight() >= desiredHeight) {
                    qualifiedSize.add(option);
                }
            } else {
                if (option.getWidth() >= desiredHeight && option.getHeight() >= desiredWidth) {
                    qualifiedSize.add(option);
                }
            }
        }
        if (qualifiedSize.size() > 0) {
            return Collections.min(qualifiedSize, (lhs, rhs) -> {
                int delta = lhs.getWidth() * lhs.getHeight() - rhs.getWidth() * rhs.getHeight();
                return Integer.signum(delta);
            });
        } else {
            return new PreviewSize(desiredWidth, desiredHeight);
        }
    }

    protected abstract boolean startPreview();

    protected abstract boolean stopPreview();

    protected abstract int getSensorDegree();

    protected abstract boolean canOperateFlash();

    protected abstract void doOpenFlash() throws RuntimeException;

    protected abstract void doCloseFlash() throws RuntimeException;

    protected abstract List<PreviewSize> getSupportedSize() throws RuntimeException;
}
