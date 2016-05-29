package com.github.piasy.cameracompat.gpuimage;

import com.github.piasy.cameracompat.utils.Size;

/**
 * Created by Piasy{github.com/Piasy} on 5/24/16.
 */
public interface CameraFrameCallback {
    void onFrameData(final byte[] data, final Size size, final Runnable postProcessedTask);
}
