package com.github.piasy.cameracompat.v21;

import android.annotation.TargetApi;
import android.media.Image;
import android.media.ImageReader;
import android.os.Build;
import com.github.piasy.cameracompat.gpuimage.CameraFrameCallback;
import com.github.piasy.cameracompat.utils.Size;

/**
 * Created by Piasy{github.com/Piasy} on 5/24/16.
 */
@TargetApi(Build.VERSION_CODES.LOLLIPOP)
public class Camera2PreviewCallback implements ImageReader.OnImageAvailableListener {
    private final CameraFrameCallback mCameraFrameCallback;
    private Size mSize;

    public Camera2PreviewCallback(CameraFrameCallback cameraFrameCallback) {
        mCameraFrameCallback = cameraFrameCallback;
    }

    @Override
    public void onImageAvailable(ImageReader reader) {
        final Image image = reader.acquireLatestImage();
        if (image != null) {
            final byte[] data = ImageUtils.getDataFromImage(image, ImageUtils.COLOR_FormatI420);
            if (mSize == null
                    || mSize.getWidth() != image.getWidth()
                    || mSize.getHeight() != image.getHeight()) {
                mSize = new Size(image.getWidth(), image.getHeight());
            }
            mCameraFrameCallback.onFrameData(data, mSize, new Runnable() {
                @Override
                public void run() {
                    image.close();
                }
            });
        }
    }
}
