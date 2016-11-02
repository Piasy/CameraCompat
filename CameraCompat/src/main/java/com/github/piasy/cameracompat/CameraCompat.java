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

package com.github.piasy.cameracompat;

import android.os.Build;
import android.os.Bundle;
import android.support.annotation.IdRes;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import com.github.piasy.cameracompat.internal.Camera1PreviewFragment;
import com.github.piasy.cameracompat.internal.Camera1PreviewFragmentBuilder;
import com.github.piasy.cameracompat.internal.Camera2PreviewFragment;
import com.github.piasy.cameracompat.internal.Camera2PreviewFragmentBuilder;
import com.github.piasy.cameracompat.internal.events.SwitchBeautifyEvent;
import com.github.piasy.cameracompat.internal.events.SwitchCameraEvent;
import com.github.piasy.cameracompat.internal.events.SwitchFlashEvent;
import com.github.piasy.cameracompat.utils.Profiler;
import org.greenrobot.eventbus.EventBus;

/**
 * Created by Piasy{github.com/Piasy} on 5/29/16.
 */
public final class CameraCompat {

    private static final int DEFAULT_WIDTH = 640;
    private static final int DEFAULT_HEIGHT = 480;
    public static final String CAMERA_PREVIEW_FRAGMENT = "CameraPreviewFragment";
    private static final int REQUEST_CODE = 1234;

    public static final class Builder {
        private int mPreviewWidth = DEFAULT_WIDTH;
        private int mPreviewHeight = DEFAULT_HEIGHT;
        private boolean mIsFrontCamera;
        private boolean mIsFlashOpen;
        private boolean mIsBeautifyOn;
        private EventBus mEventBus;
        private Profiler.MetricListener mMetricListener;

        public Builder previewWidth(int previewWidth) {
            mPreviewWidth = previewWidth;
            return this;
        }

        public Builder previewHeight(int previewHeight) {
            mPreviewHeight = previewHeight;
            return this;
        }

        public Builder frontCamera(boolean frontCamera) {
            mIsFrontCamera = frontCamera;
            return this;
        }

        public Builder flashOpen(boolean flashOpen) {
            mIsFlashOpen = flashOpen;
            return this;
        }

        public Builder beautifyOn(boolean beautifyOn) {
            mIsBeautifyOn = beautifyOn;
            return this;
        }

        public Builder eventBus(EventBus eventBus) {
            mEventBus = eventBus;
            return this;
        }

        public Builder metricListener(Profiler.MetricListener metricListener) {
            mMetricListener = metricListener;
            return this;
        }

        public CameraCompat build() {
            if (mEventBus == null) {
                mEventBus = EventBus.builder().build();
            }
            CameraCompat instance = new CameraCompat(this);
            sCameraCompat = instance;
            return instance;
        }
    }

    /**
     * Video will have the specified width, but the height will be trimmed.
     *
     * width > height
     * */
    public interface VideoCaptureCallback {
        void onVideoSizeChanged(int width, int height);

        void onFrameData(final byte[] data, final int width, final int height);
    }

    private static volatile CameraCompat sCameraCompat;

    private final int mPreviewWidth;
    private final int mPreviewHeight;
    private final boolean mIsFrontCamera;
    private final boolean mIsFlashOpen;
    private final boolean mIsBeautifyOn;
    private final EventBus mEventBus;
    private final Profiler mProfiler;

    private CameraCompat(CameraCompat.Builder builder) {
        mPreviewWidth = builder.mPreviewWidth;
        mPreviewHeight = builder.mPreviewHeight;
        mIsFrontCamera = builder.mIsFrontCamera;
        mIsFlashOpen = builder.mIsFlashOpen;
        mIsBeautifyOn = builder.mIsBeautifyOn;
        mEventBus = builder.mEventBus;
        if (builder.mMetricListener != null) {
            mProfiler = new Profiler(builder.mMetricListener);
        } else {
            mProfiler = null;
        }
    }

    public static CameraCompat getInstance() {
        if (sCameraCompat == null) {
            throw new IllegalStateException("CameraCompat is not initialized!");
        }
        return sCameraCompat;
    }

    /**
     * add {@link Camera1PreviewFragment} or {@link Camera2PreviewFragment} to start preview
     *
     * @param savedInstance to determine whether should add a new fragment
     * @param fragmentManager the fragment manager
     * @param container the container id to hold this fragment
     * @return {@code true} if the fragment is newly added
     */
    public boolean startPreviewFromFragment(Bundle savedInstance, FragmentManager fragmentManager,
            @IdRes int container, Fragment frameCallback) {
        // API 21 has bug on Camera2, the YUV_420_888 data has all zero in UV planes,
        // ref: comments of `convertYUV_420_888ToRGB` function in this blog post:
        // https://attiapp.wordpress.com/2015/06/28/convert-yuv_420_888-to-rgb-with-camera2-api-2/
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
            if (savedInstance == null
                    || fragmentManager.findFragmentByTag(CAMERA_PREVIEW_FRAGMENT) == null) {
                Camera2PreviewFragment fragment =
                        Camera2PreviewFragmentBuilder.newCamera2PreviewFragment(mIsBeautifyOn,
                                mIsFlashOpen, mIsFrontCamera, mPreviewHeight, mPreviewWidth);
                if (frameCallback != null) {
                    fragment.setTargetFragment(frameCallback, REQUEST_CODE);
                }
                fragmentManager.beginTransaction()
                        .add(container, fragment, CAMERA_PREVIEW_FRAGMENT)
                        .commit();
                return true;
            }
        } else {
            if (savedInstance == null
                    || fragmentManager.findFragmentByTag(CAMERA_PREVIEW_FRAGMENT) == null) {
                Camera1PreviewFragment fragment =
                        Camera1PreviewFragmentBuilder.newCamera1PreviewFragment(mIsBeautifyOn,
                                mIsFlashOpen, mIsFrontCamera, mPreviewHeight, mPreviewWidth);
                if (frameCallback != null) {
                    fragment.setTargetFragment(frameCallback, REQUEST_CODE);
                }
                fragmentManager.beginTransaction()
                        .add(container, fragment, CAMERA_PREVIEW_FRAGMENT)
                        .commit();
                return true;
            }
        }
        return false;
    }

    public boolean startPreview(Bundle savedInstance, FragmentManager fragmentManager,
            @IdRes int container) {
        return startPreviewFromFragment(savedInstance, fragmentManager, container, null);
    }

    public EventBus getEventBus() {
        return mEventBus;
    }

    public void switchBeautify() {
        mEventBus.post(new SwitchBeautifyEvent());
    }

    public void switchCamera() {
        mEventBus.post(new SwitchCameraEvent());
    }

    public void switchFlash() {
        mEventBus.post(new SwitchFlashEvent());
    }

    public Profiler getProfiler() {
        return mProfiler;
    }
}
