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

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.PixelFormat;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.support.annotation.CallSuper;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import com.github.piasy.cameracompat.CameraCompat;
import com.github.piasy.cameracompat.R;
import com.github.piasy.cameracompat.gpuimage.GLFilterGroup;
import com.github.piasy.cameracompat.gpuimage.GLRender;
import com.github.piasy.cameracompat.internal.events.SwitchBeautifyEvent;
import com.github.piasy.cameracompat.internal.events.SwitchCameraEvent;
import com.github.piasy.cameracompat.internal.events.SwitchFlashEvent;
import com.github.piasy.cameracompat.utils.Utils;
import com.hannesdorfmann.fragmentargs.annotation.Arg;
import java.util.Arrays;
import jp.co.cyberagent.android.gpuimage.GPUImageFilter;
import jp.co.cyberagent.android.gpuimage.GPUImageTwoInputFilter;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import tv.yoloyolo.ios.beautify.BeautifyShaders;

/**
 * Created by Piasy{github.com/Piasy} on 6/29/16.
 */

abstract class PreviewFragment extends Fragment {

    @Arg
    int mPreviewWidth;
    @Arg
    int mPreviewHeight;
    @Arg
    boolean mIsDefaultFrontCamera;
    @Arg
    boolean mIsDefaultFlashOpen;
    @Arg
    boolean mIsDefaultBeautifyOn;

    GLSurfaceView mGLSurfaceView;

    GLRender mRenderer;
    private CameraCompat.VideoCaptureCallback mVideoCaptureCallback;
    private GPUImageTwoInputFilter mTwoInputFilter;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof CameraCompat.VideoCaptureCallback) {
            mVideoCaptureCallback = (CameraCompat.VideoCaptureCallback) context;
        } else if (getTargetFragment() instanceof CameraCompat.VideoCaptureCallback) {
            mVideoCaptureCallback = (CameraCompat.VideoCaptureCallback) getTargetFragment();
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        CameraCompat.getInstance().getEventBus().register(this);
        return inflater.inflate(R.layout.preview_fragment, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        bindView(view);

        initFields();

        initSurface();
    }

    @Override
    public void onResume() {
        super.onResume();

        startPreview();
    }

    @Override
    public void onPause() {
        super.onPause();

        stopPreview();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        CameraCompat.getInstance().getEventBus().unregister(this);
        mTwoInputFilter.recycleBitmap();
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mVideoCaptureCallback = null;
    }

    protected abstract void startPreview();

    protected abstract void stopPreview();

    protected abstract void switchCamera();

    protected abstract void switchFlash();

    public PreviewFragment() {
        // Required empty public constructor
    }

    private void bindView(View rootView) {
        mGLSurfaceView = (GLSurfaceView) rootView.findViewById(R.id.mSurface);
    }

    @CallSuper
    protected void initFields() {
        GPUImageFilter smoothFilter = new GPUImageFilter(GPUImageFilter.NO_FILTER_VERTEX_SHADER,
                BeautifyShaders.SMOOTH_FRAGMENT_SHADER);
        mTwoInputFilter = new GPUImageTwoInputFilter(BeautifyShaders.RGB_MAP_FRAGMENT_SHADER);
        Bitmap bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.map);
        mTwoInputFilter.setBitmap(bitmap);
        GLFilterGroup filterGroup = new GLFilterGroup(
                Arrays.asList(smoothFilter, mTwoInputFilter, new GPUImageFilter()),
                mVideoCaptureCallback, mIsDefaultFrontCamera);
        mRenderer = new GLRender(filterGroup, mIsDefaultBeautifyOn, mVideoCaptureCallback,
                mIsDefaultFrontCamera);
    }

    private void initSurface() {
        if (Utils.isSupportOpenGLES2(getContext())) {
            mGLSurfaceView.setEGLContextClientVersion(2);
        }
        mGLSurfaceView.setEGLConfigChooser(8, 8, 8, 8, 16, 0);
        mGLSurfaceView.getHolder().setFormat(PixelFormat.RGBA_8888);
        mGLSurfaceView.setRenderer(mRenderer);
        mGLSurfaceView.setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
        mGLSurfaceView.requestRender();
    }

    @Subscribe(threadMode = ThreadMode.ASYNC)
    public void onSwitchBeautify(SwitchBeautifyEvent event) {
        mRenderer.switchFilter();
    }

    @Subscribe(threadMode = ThreadMode.ASYNC)
    public void onSwitchCamera(SwitchCameraEvent event) {
        switchCamera();
    }

    @Subscribe(threadMode = ThreadMode.ASYNC)
    public void onSwitchFlash(SwitchFlashEvent event) {
        switchFlash();
    }
}
