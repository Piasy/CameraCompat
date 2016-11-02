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
import android.hardware.Camera;
import android.opengl.GLSurfaceView;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import com.github.piasy.cameracompat.gpuimage.SurfaceTextureInitCallback;
import com.hannesdorfmann.fragmentargs.annotation.FragmentWithArgs;
import java.io.IOException;
import jp.co.cyberagent.android.gpuimage.Rotation;

/**
 * A simple {@link Fragment} subclass.
 */
@TargetApi(Build.VERSION_CODES.JELLY_BEAN)
@FragmentWithArgs
public class Camera1PreviewFragment extends PreviewFragment
        implements Camera1Helper.CameraController {

    private static final String TAG = "Camera1PreviewFragment";

    private Camera1Helper mCameraHelper;
    private Camera1PreviewCallback mCameraFrameCallback;

    public Camera1PreviewFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Camera1PreviewFragmentBuilder.injectArguments(this);
    }

    @Override
    protected void startPreview() {
        mCameraHelper.startPreview(getActivity());
    }

    @Override
    protected void stopPreview() {
        mCameraHelper.stopPreview();
    }

    @Override
    protected void switchCamera() {
        mRenderer.pauseDrawing();
        mCameraHelper.switchCamera(getActivity());
        mRenderer.cameraSwitched();
    }

    @Override
    protected void switchFlash() {
        mCameraHelper.switchFlash();
    }

    @Override
    protected void initFields() {
        super.initFields();
        mCameraHelper = new Camera1Helper(mPreviewWidth, mPreviewHeight, this,
                mIsDefaultFrontCamera);
        mCameraFrameCallback = new Camera1PreviewCallback(mRenderer);
    }

    @Override
    public void onOpened(final Camera camera, Rotation rotation, boolean flipHorizontal,
            boolean flipVertical) {
        mGLSurfaceView.setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);
        mRenderer.setRotationCamera(rotation, flipHorizontal, flipVertical);
        mRenderer.setUpSurfaceTexture(new SurfaceTextureInitCallback() {
            @Override
            public void onSurfaceTextureInitiated(SurfaceTexture surfaceTexture) {
                startPreviewDirectly(surfaceTexture, camera);
            }
        });
    }

    private void startPreviewDirectly(SurfaceTexture surfaceTexture, Camera camera) {
        try {
            camera.setPreviewCallback(mCameraFrameCallback);
            camera.setPreviewTexture(surfaceTexture);
            camera.startPreview();
            mRenderer.resumeDrawing();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
