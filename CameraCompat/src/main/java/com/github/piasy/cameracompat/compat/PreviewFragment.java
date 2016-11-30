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

import android.Manifest;
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
import com.github.piasy.safelyandroid.misc.CheckUtil;
import com.hannesdorfmann.fragmentargs.annotation.Arg;
import com.tbruyelle.rxpermissions2.RxPermissions;
import io.reactivex.disposables.Disposable;
import javax.annotation.concurrent.GuardedBy;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

/**
 * Created by Piasy{github.com/Piasy} on 6/29/16.
 */

abstract class PreviewFragment extends Fragment {

    @GuardedBy("this")
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
    @GuardedBy("this")
    private EventBus mEventBus;
    private Disposable mPermissionRequest;

    public PreviewFragment() {
        // Required empty public constructor
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        try {
            mProcessorChain = CameraCompat.getInstance().getProcessorChain();
            mEventBus = CameraCompat.getInstance().getEventBus();
        } catch (IllegalStateException e) {
            // this could happen when the host Activity is recreated, and before user init
            // CameraCompat instance, this preview fragment get attached.

            // currently we could only cover this problem to avoid crash, but
            // TODO: 27/10/2016 could we notify user that error happen?
            // although leave this Activity and enter again would solve this problem

            mProcessorChain = NoOpChain.INSTANCE;
            mEventBus = EventBus.getDefault();
        }
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        mEventBus.register(this);
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

        disposePermissionRequest();
        mPermissionRequest = RxPermissions.getInstance(getContext())
                .request(Manifest.permission.CAMERA)
                .subscribe(grant -> {
                    if (grant && CheckUtil.nonNull(mCameraHelper)) {
                        mCameraHelper.startPreview();
                    } else {
                        CameraCompat.onError(CameraCompat.ERR_PERMISSION);
                    }
                }, throwable -> CameraCompat.onError(CameraCompat.ERR_PERMISSION));
    }

    @Override
    public void onPause() {
        super.onPause();

        if (CheckUtil.nonNull(mCameraHelper)) {
            mCameraHelper.stopPreview();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mEventBus.unregister(this);
        mProcessorChain.tearDown();
        disposePermissionRequest();
    }

    @Override
    public void onDetach() {
        super.onDetach();

        synchronized (this) {
            mProcessorChain = null;
            mEventBus = null;
        }
    }

    private void disposePermissionRequest() {
        if (mPermissionRequest != null && !mPermissionRequest.isDisposed()) {
            mPermissionRequest.dispose();
            mPermissionRequest = null;
        }
    }

    protected abstract CameraHelper createCameraHelper();

    @Subscribe(threadMode = ThreadMode.ASYNC)
    public synchronized void onSwitchBeautify(SwitchBeautifyEvent event) {
        if (isResumed() && mProcessorChain instanceof GPUImageChain) {
            ((GPUImageChain) mProcessorChain).switchBeautify();
        }
    }

    @Subscribe(threadMode = ThreadMode.ASYNC)
    public synchronized void onSwitchCamera(SwitchCameraEvent event) {
        if (isResumed() && CheckUtil.nonNull(mProcessorChain, mCameraHelper)) {
            mProcessorChain.pause();
            mCameraHelper.switchCamera();
            mProcessorChain.cameraSwitched();
        }
    }

    @Subscribe(threadMode = ThreadMode.ASYNC)
    public synchronized void onSwitchFlash(SwitchFlashEvent event) {
        if (isResumed() && CheckUtil.nonNull(mCameraHelper)) {
            mCameraHelper.switchFlash();
        }
    }

    @Subscribe(threadMode = ThreadMode.ASYNC)
    public synchronized void onSwitchMirror(SwitchMirrorEvent event) {
        if (isResumed() && CheckUtil.nonNull(mProcessorChain)) {
            mProcessorChain.switchMirror();
        }
    }
}
