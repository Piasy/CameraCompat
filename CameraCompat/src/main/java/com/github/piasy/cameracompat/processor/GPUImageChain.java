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

package com.github.piasy.cameracompat.processor;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.PixelFormat;
import android.media.Image;
import android.opengl.GLSurfaceView;
import android.os.Build;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import com.github.piasy.cameracompat.CameraCompat;
import com.github.piasy.cameracompat.gpuimage.GLFilterGroup;
import com.github.piasy.cameracompat.gpuimage.GLRender;
import com.github.piasy.cameracompat.gpuimage.SurfaceInitCallback;
import com.github.piasy.cameracompat.utils.GLUtil;
import com.github.piasy.cameracompat.utils.Profiler;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import jp.co.cyberagent.android.gpuimage.GPUImageFilter;
import jp.co.cyberagent.android.gpuimage.Rotation;

/**
 * Created by Piasy{github.com/Piasy} on 18/10/2016.
 */

public class GPUImageChain implements ProcessorChain, GLFilterGroup.ImageDumpedListener,
        GLRender.VideoSizeChangedListener {
    private final List<Processor> mProcessors;
    private final CameraCompat.VideoCaptureCallback mVideoCaptureCallback;
    private final boolean mDefaultFilterEnabled;
    private final Profiler mProfiler;

    private GLSurfaceView mGLSurfaceView;
    private GLRender mGLRender;
    private volatile boolean mIsFrontCamera;
    private volatile boolean mEnableMirror;

    private ByteBuffer mGLRgbaBuffer;
    private ByteBuffer mGLYuvBuffer;

    public GPUImageChain(List<Processor> processors, boolean defaultEnableFilter,
            boolean defaultFrontCamera, CameraCompat.VideoCaptureCallback videoCaptureCallback,
            Profiler profiler) {
        mProcessors = Collections.unmodifiableList(new ArrayList<>(processors));
        mVideoCaptureCallback = videoCaptureCallback;
        mDefaultFilterEnabled = defaultEnableFilter;
        mIsFrontCamera = defaultFrontCamera;
        mProfiler = profiler;
    }

    @Override
    public void setUp() {
        for (Processor processor : mProcessors) {
            processor.setUp();
        }
    }

    @Override
    public View createSurface(Context context) {
        mGLSurfaceView = new GLSurfaceView(context);
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        mGLSurfaceView.setLayoutParams(params);
        mGLSurfaceView.setKeepScreenOn(true);
        return mGLSurfaceView;
    }

    @Override
    public void initSurface(Context context) {
        List<GPUImageFilter> filters = new ArrayList<>();
        for (Processor processor : mProcessors) {
            filters.addAll(processor.getFilters());
        }
        filters.add(new GPUImageFilter());
        GLFilterGroup filterGroup = new GLFilterGroup(filters);
        filterGroup.setImageDumpedListener(this);
        mGLRender = new GLRender(filterGroup, mDefaultFilterEnabled, this, mProfiler);

        if (GLUtil.isSupportOpenGLES2(context)) {
            mGLSurfaceView.setEGLContextClientVersion(2);
        }
        mGLSurfaceView.setEGLConfigChooser(8, 8, 8, 8, 16, 0);
        mGLSurfaceView.getHolder().setFormat(PixelFormat.RGBA_8888);
        mGLSurfaceView.setRenderer(mGLRender);
        mGLSurfaceView.setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
        mGLSurfaceView.requestRender();
    }

    @Override
    public void onCameraOpened(Rotation rotation, boolean flipHorizontal, boolean flipVertical,
            SurfaceInitCallback callback) {
        mGLSurfaceView.setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);
        mGLRender.setRotationCamera(rotation, flipHorizontal, flipVertical);
        mGLRender.setUpSurfaceTexture(callback);
    }

    @Override
    public void pause() {
        mGLRender.pauseDrawing();
    }

    @Override
    public void resume() {
        mGLRender.resumeDrawing();
    }

    @Override
    public synchronized void cameraSwitched() {
        mIsFrontCamera = !mIsFrontCamera;
    }

    @Override
    public synchronized void switchMirror() {
        mEnableMirror = !mEnableMirror;
    }

    @Override
    public void tearDown() {
        for (Processor processor : mProcessors) {
            processor.tearDown();
        }
    }

    public synchronized void switchBeautify() {
        mGLRender.switchFilter();
    }

    @Override
    public void onFrameData(byte[] data, int width, int height,
            Runnable postProcessedTask) {
        if (mGLRgbaBuffer == null) {
            mGLRgbaBuffer = ByteBuffer.allocateDirect(width * height * 4);
        }
        if (mGLYuvBuffer == null) {
            // 16 bytes alignment
            int bufHeight = (width * mGLRender.getFrameWidth() / mGLRender.getFrameHeight())
                            & 0xfffffff0;
            mGLYuvBuffer = ByteBuffer.allocateDirect(width * bufHeight * 3 / 2);
        }
        if (!mGLRender.isBusyDrawing()) {
            RgbYuvConverter.yuv2rgba(width, height, data, mGLRgbaBuffer.array());
            mGLRender.scheduleDrawFrame(mGLRgbaBuffer, width, height, () -> {
                if (!mGLRender.isEnableFilter() && !mGLRender.isPaused()) {
                    sendNormalImage(width, height, data);
                }
                postProcessedTask.run();
            });
        }
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    @Override
    public void onFrameData(final Image image, final Runnable postProcessedTask) {
        final int width = image.getWidth();
        final int height = image.getHeight();
        if (mGLRgbaBuffer == null) {
            mGLRgbaBuffer = ByteBuffer.allocateDirect(width * height * 4);
        }
        if (mGLYuvBuffer == null) {
            // 16 bytes alignment
            int bufHeight = (width * mGLRender.getFrameWidth() / mGLRender.getFrameHeight())
                            & 0xfffffff0;
            mGLYuvBuffer = ByteBuffer.allocateDirect(width * bufHeight * 3 / 2);
        }
        if (!mGLRender.isBusyDrawing()) {
            RgbYuvConverter.image2rgba(image, mGLRgbaBuffer.array());
            mGLRender.scheduleDrawFrame(mGLRgbaBuffer, width, height, () -> {
                if (!mGLRender.isEnableFilter() && !mGLRender.isPaused()) {
                    sendNormalImage(image);
                }
                postProcessedTask.run();
            });
        } else {
            postProcessedTask.run();
        }
    }

    @Override
    public void imageDumped(ByteBuffer rgba, int width, int height) {
        sendBeautifyImage(rgba, width, height);
    }

    private void sendBeautifyImage(ByteBuffer rgba, int width, int height) {
        if (mIsFrontCamera) {
            if (mEnableMirror) {
                RgbYuvConverter.rgba2yuvRotateC90(width, height, rgba.array(),
                        mGLYuvBuffer.array());
            } else {
                RgbYuvConverter.rgba2yuvRotateC90Flip(width, height, rgba.array(),
                        mGLYuvBuffer.array());
            }
        } else {
            RgbYuvConverter.rgba2yuvRotateC90(width, height, rgba.array(), mGLYuvBuffer.array());
        }
        mVideoCaptureCallback.onFrameData(mGLYuvBuffer.array(), height, width);
    }

    private void sendNormalImage(int width, int height, byte[] data) {
        if (mIsFrontCamera && mEnableMirror) {
            if (mGLRender.getRotation() == Rotation.ROTATION_90) {
                RgbYuvConverter.yuvCropFlip(width, height, data, mGLRender.getVideoHeight(),
                        mGLYuvBuffer.array());
            } else {
                RgbYuvConverter.yuvCropRotateC180Flip(width, height, data,
                        mGLRender.getVideoHeight(), mGLYuvBuffer.array());
            }
        } else {
            if (mGLRender.getRotation() == Rotation.ROTATION_90) {
                RgbYuvConverter.yuvCropRotateC180(width, height, data, mGLRender.getVideoHeight(),
                        mGLYuvBuffer.array());
            } else {
                RgbYuvConverter.yuvCrop(width, height, data, mGLRender.getVideoHeight(),
                        mGLYuvBuffer.array());
            }
        }
        mVideoCaptureCallback.onFrameData(mGLYuvBuffer.array(), width, mGLRender.getVideoHeight());
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private void sendNormalImage(Image image) {
        if (mIsFrontCamera && mEnableMirror) {
            if (mGLRender.getRotation() == Rotation.ROTATION_90) {
                RgbYuvConverter.image2yuvCropFlip(image, mGLRender.getVideoHeight(),
                        mGLYuvBuffer.array());
            } else {
                RgbYuvConverter.image2yuvCropRotateC180Flip(image, mGLRender.getVideoHeight(),
                        mGLYuvBuffer.array());
            }
        } else {
            if (mGLRender.getRotation() == Rotation.ROTATION_90) {
                RgbYuvConverter.image2yuvCropRotateC180(image, mGLRender.getVideoHeight(),
                        mGLYuvBuffer.array());
            } else {
                RgbYuvConverter.image2yuvCrop(image, mGLRender.getVideoHeight(),
                        mGLYuvBuffer.array());
            }
        }
        mVideoCaptureCallback.onFrameData(mGLYuvBuffer.array(), image.getWidth(),
                mGLRender.getVideoHeight());
    }

    @Override
    public void onVideoSizeChanged(int width, int height) {
        mVideoCaptureCallback.onVideoSizeChanged(width, height);
    }
}
