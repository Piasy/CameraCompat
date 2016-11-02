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
import android.media.Image;
import android.media.ImageReader;
import android.os.Build;
import com.github.piasy.cameracompat.CameraCompat;
import com.github.piasy.cameracompat.gpuimage.CameraFrameCallback;

/**
 * Created by Piasy{github.com/Piasy} on 5/24/16.
 */
@TargetApi(Build.VERSION_CODES.LOLLIPOP)
class Camera2PreviewCallback implements ImageReader.OnImageAvailableListener {
    private final CameraFrameCallback mCameraFrameCallback;

    Camera2PreviewCallback(CameraFrameCallback cameraFrameCallback) {
        mCameraFrameCallback = cameraFrameCallback;
    }

    @Override
    public void onImageAvailable(ImageReader reader) {
        try {
            final Image image = reader.acquireLatestImage();
            if (image != null) {
                mCameraFrameCallback.onFrameData(image, new Runnable() {
                    @Override
                    public void run() {
                        image.close();
                    }
                });
            }
        } catch (OutOfMemoryError | IllegalStateException e) {
            CameraCompat.onError(CameraCompat.ERR_UNKNOWN);
        }
    }
}
