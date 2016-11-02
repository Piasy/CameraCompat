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
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.media.ImageReader;
import android.opengl.GLSurfaceView;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.Surface;
import com.github.piasy.cameracompat.gpuimage.SurfaceTextureInitCallback;
import com.hannesdorfmann.fragmentargs.annotation.FragmentWithArgs;
import java.util.Arrays;
import java.util.List;
import jp.co.cyberagent.android.gpuimage.Rotation;

@TargetApi(Build.VERSION_CODES.LOLLIPOP)
@FragmentWithArgs
public class Camera2PreviewFragment extends PreviewFragment
        implements Camera2Helper.CameraController {

    private static final String TAG = "Camera2PreviewFragment";

    private Camera2Helper mCameraHelper;
    private Camera2PreviewCallback mCameraFrameCallback;

    public Camera2PreviewFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Camera2PreviewFragmentBuilder.injectArguments(this);
    }

    @Override
    protected void startPreview() {
        mCameraHelper.startPreview(mCameraFrameCallback);
    }

    @Override
    protected void stopPreview() {
        mCameraHelper.stopPreview();
    }

    @Override
    protected void switchCamera() {
        mRenderer.pauseDrawing();
        mCameraHelper.switchCamera(mCameraFrameCallback);
        mRenderer.cameraSwitched();
    }

    @Override
    protected void switchFlash() {
        mCameraHelper.switchFlash();
    }

    @Override
    protected void initFields() {
        super.initFields();
        try {
            mCameraHelper = new Camera2Helper(getActivity(), this, mPreviewWidth, mPreviewHeight,
                    mIsDefaultFrontCamera);
            mCameraFrameCallback = new Camera2PreviewCallback(mRenderer);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    /**
     * Creates a new {@link CameraCaptureSession} for camera preview.
     */
    private void startPreviewDirectly(final CameraDevice cameraDevice, List<Surface> targets,
            final boolean isFlashOn, final Handler cameraHandler) {
        try {

            final CaptureRequest.Builder captureRequestBuilder =
                    cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            for (int i = 0, size = targets.size(); i < size; i++) {
                captureRequestBuilder.addTarget(targets.get(i));
            }

            cameraDevice.createCaptureSession(targets, new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(@NonNull CameraCaptureSession cameraCaptureSession) {
                    try {
                        captureRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE,
                                CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_VIDEO);
                        captureRequestBuilder.set(CaptureRequest.FLASH_MODE,
                                isFlashOn ? CameraMetadata.FLASH_MODE_TORCH
                                        : CameraMetadata.FLASH_MODE_OFF);

                        cameraCaptureSession.setRepeatingRequest(captureRequestBuilder.build(),
                                null, cameraHandler);
                        mCameraHelper.previewSessionStarted(cameraCaptureSession);
                        mRenderer.resumeDrawing();
                    } catch (CameraAccessException e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onConfigureFailed(@NonNull CameraCaptureSession cameraCaptureSession) {
                    Log.e(TAG, "createCaptureSession::onConfigureFailed");
                }
            }, cameraHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onOpened(final CameraDevice cameraDevice, final ImageReader imageReader,
            final Handler cameraHandler, final Rotation rotation, final boolean flipHorizontal,
            final boolean flipVertical) {
        mGLSurfaceView.setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);
        mRenderer.setRotationCamera(rotation, flipHorizontal, flipVertical);
        mRenderer.setUpSurfaceTexture(new SurfaceTextureInitCallback() {
            @Override
            public void onSurfaceTextureInitiated(SurfaceTexture surfaceTexture) {
                // fix MX5 preview not show bug: http://stackoverflow.com/a/34337226/3077508
                surfaceTexture.setDefaultBufferSize(mPreviewWidth, mPreviewHeight);
                Surface surface = new Surface(surfaceTexture);
                List<Surface> targets = Arrays.asList(surface, imageReader.getSurface());
                mCameraHelper.outputTargetChanged(targets);
                startPreviewDirectly(cameraDevice, targets, mIsDefaultFlashOpen, cameraHandler);
            }
        });
    }

    @Override
    public void onSettingsChanged(final CameraDevice cameraDevice, final List<Surface> targets,
            final boolean isFlashOn, final Handler cameraHandler) {
        startPreviewDirectly(cameraDevice, targets, isFlashOn, cameraHandler);
    }
}
