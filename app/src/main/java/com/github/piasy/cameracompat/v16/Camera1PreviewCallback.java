package com.github.piasy.cameracompat.v16;

import android.hardware.Camera;
import com.github.piasy.cameracompat.gpuimage.CameraFrameCallback;
import com.github.piasy.cameracompat.utils.Size;

/**
 * Created by Piasy{github.com/Piasy} on 5/24/16.
 */
public class Camera1PreviewCallback implements Camera.PreviewCallback {
    private final CameraFrameCallback mCameraFrameCallback;
    private Size mSize;

    public Camera1PreviewCallback(CameraFrameCallback cameraFrameCallback) {
        mCameraFrameCallback = cameraFrameCallback;
    }

    @Override
    public void onPreviewFrame(final byte[] data, final Camera camera) {
        Camera.Size size = camera.getParameters().getPreviewSize();
        if (mSize == null || mSize.getWidth() != size.width || mSize.getHeight() != size.height) {
            mSize = new Size(size.width, size.height);
        }
        mCameraFrameCallback.onFrameData(data, mSize, new Runnable() {
            @Override
            public void run() {
                camera.addCallbackBuffer(data);
            }
        });
    }
}
