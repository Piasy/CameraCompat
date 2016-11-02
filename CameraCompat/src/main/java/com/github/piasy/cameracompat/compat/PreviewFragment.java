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

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import com.github.piasy.cameracompat.CameraCompat;
import com.github.piasy.cameracompat.R;
import com.github.piasy.cameracompat.compat.events.SwitchBeautifyEvent;
import com.github.piasy.cameracompat.compat.events.SwitchCameraEvent;
import com.github.piasy.cameracompat.compat.events.SwitchFlashEvent;
import com.github.piasy.cameracompat.compat.events.SwitchMirrorEvent;
import com.github.piasy.cameracompat.processor.GPUImageChain;
import com.github.piasy.cameracompat.processor.ProcessorChain;
import com.hannesdorfmann.fragmentargs.annotation.Arg;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

/**
 * Created by Piasy{github.com/Piasy} on 6/29/16.
 */

abstract class PreviewFragment extends Fragment {

    protected ProcessorChain mProcessorChain;

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
    @Arg
    boolean mIsDefaultMirrorEnabled;

    private ViewGroup mPreviewContainer;

    private CameraHelper mCameraHelper;

    public PreviewFragment() {
        // Required empty public constructor
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        mProcessorChain = CameraCompat.getInstance().getProcessorChain();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        CameraCompat.getInstance().getEventBus().register(this);
        mProcessorChain.setUp();
        return inflater.inflate(R.layout.preview_fragment, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mPreviewContainer = (ViewGroup) view.findViewById(R.id.mPreviewContainer);
        mPreviewContainer.addView(mProcessorChain.createSurface(getContext()));
        mCameraHelper = createCameraHelper();
        mProcessorChain.initSurface(getContext());
    }

    @Override
    public void onResume() {
        super.onResume();

        if (mCameraHelper != null) {
            mCameraHelper.startPreview();
        }
    }

    @Override
    public void onPause() {
        super.onPause();

        if (mCameraHelper != null) {
            mCameraHelper.stopPreview();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        CameraCompat.getInstance().getEventBus().unregister(this);
        mProcessorChain.tearDown();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mProcessorChain = null;
    }

    protected abstract CameraHelper createCameraHelper();

    @Subscribe(threadMode = ThreadMode.ASYNC)
    public void onSwitchBeautify(SwitchBeautifyEvent event) {
        if (mProcessorChain instanceof GPUImageChain) {
            ((GPUImageChain) mProcessorChain).switchBeautify();
        }
    }

    @Subscribe(threadMode = ThreadMode.ASYNC)
    public void onSwitchCamera(SwitchCameraEvent event) {
        mProcessorChain.pause();
        mCameraHelper.switchCamera();
        mProcessorChain.cameraSwitched();
    }

    @Subscribe(threadMode = ThreadMode.ASYNC)
    public void onSwitchFlash(SwitchFlashEvent event) {
        mCameraHelper.switchFlash();
    }

    @Subscribe(threadMode = ThreadMode.ASYNC)
    public void onSwitchMirror(SwitchMirrorEvent event) {
        mProcessorChain.switchMirror();
    }
}
