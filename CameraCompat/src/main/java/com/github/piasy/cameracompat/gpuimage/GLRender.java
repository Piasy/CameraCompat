/*
 * Copyright (C) 2012 CyberAgent
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Modified by Piasy
 */

package com.github.piasy.cameracompat.gpuimage;

import android.annotation.TargetApi;
import android.graphics.Rect;
import android.graphics.SurfaceTexture;
import android.media.Image;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.os.Build;
import com.github.piasy.cameracompat.CameraCompat;
import com.github.piasy.cameracompat.utils.Profiler;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.Collections;
import java.util.LinkedList;
import java.util.Queue;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;
import jp.co.cyberagent.android.gpuimage.GPUImage;
import jp.co.cyberagent.android.gpuimage.GPUImageFilter;
import jp.co.cyberagent.android.gpuimage.Rotation;
import jp.co.cyberagent.android.gpuimage.util.TextureRotationUtil;

import static jp.co.cyberagent.android.gpuimage.util.TextureRotationUtil.TEXTURE_NO_ROTATION;

/**
 * Created by Piasy{github.com/Piasy} on 5/24/16.
 *
 * {@link GLSurfaceView.Renderer} implementation, work both for Camera
 * and Camera2 framework.
 */
@SuppressWarnings("unused")
public class GLRender implements GLSurfaceView.Renderer, CameraFrameCallback {
    private static final int NO_IMAGE = -1;
    static final float CUBE[] = {
            -1.0f, -1.0f, 1.0f, -1.0f, -1.0f, 1.0f, 1.0f, 1.0f,
    };

    private GLFilterGroup mFilter;

    private final Object mSurfaceChangedWaiter = new Object();

    private int mGLTextureId = NO_IMAGE;
    private SurfaceTexture mSurfaceTexture = null;
    private final FloatBuffer mGLCubeBuffer;
    private final FloatBuffer mGLTextureBuffer;
    private ByteBuffer mGLRgbaBuffer;
    private ByteBuffer mGLYuvBuffer;

    /**
     * After surface created/changed, {@link #mOutputWidth} and {@link #mOutputHeight} will be
     * updated, and it's the size of the surface (view), it's also set to
     * {@link GPUImageFilter#mOutputWidth} and {@link GPUImageFilter#mOutputHeight}.
     */
    private int mOutputWidth;
    private int mOutputHeight;

    /**
     * After preview started (get data at {@link CameraFrameCallback#onFrameData(byte[], int, int,
     * Runnable)}), {@link #mImageWidth} and {@link #mImageHeight} will be updated, it's the size
     * of image data.
     */
    private int mImageWidth;
    private int mImageHeight;

    private int mVideoWidth;
    private int mVideoHeight;

    private final Queue<Runnable> mRunOnDraw;
    private final Queue<Runnable> mRunOnDrawEnd;

    /**
     * Used to adjust preview image size into the surface view size. so we only need to choose the
     * best match size from camera supported size, the render will handle the adjust for us.
     */
    private Rotation mRotation;
    private boolean mFlipHorizontal;
    private boolean mFlipVertical;
    private GPUImage.ScaleType mScaleType = GPUImage.ScaleType.CENTER_CROP;

    private float mBackgroundRed = 0;
    private float mBackgroundGreen = 0;
    private float mBackgroundBlue = 0;

    private final GLFilterGroup mDesiredFilter;
    private final GLFilterGroup mIdleFilterGroup;
    private volatile boolean mEnableFilter;
    private final CameraCompat.VideoCaptureCallback mVideoCaptureCallback;

    private boolean mIsPaused = false;
    private boolean mIsDrawing = true;

    public GLRender(final GLFilterGroup filter, boolean enableFilter,
            CameraCompat.VideoCaptureCallback videoCaptureCallback, boolean isFrontCamera) {
        mProfiler = CameraCompat.getInstance().getProfiler();
        mIdleFilterGroup = new GLFilterGroup(Collections.singletonList(new GPUImageFilter()),
                videoCaptureCallback, isFrontCamera);
        mVideoCaptureCallback = videoCaptureCallback;
        mDesiredFilter = filter;
        mEnableFilter = enableFilter;
        mFilter = enableFilter ? mDesiredFilter : mIdleFilterGroup;
        mRunOnDraw = new LinkedList<>();
        mRunOnDrawEnd = new LinkedList<>();

        mGLCubeBuffer = ByteBuffer.allocateDirect(CUBE.length * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer();
        mGLCubeBuffer.put(CUBE).position(0);

        mGLTextureBuffer = ByteBuffer.allocateDirect(TEXTURE_NO_ROTATION.length * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer();
        setRotation(Rotation.NORMAL, false, false);
    }

    @Override
    public void onSurfaceCreated(final GL10 unused, final EGLConfig config) {
        GLES20.glClearColor(mBackgroundRed, mBackgroundGreen, mBackgroundBlue, 1);
        GLES20.glDisable(GLES20.GL_DEPTH_TEST);
        mFilter.init();
    }

    @Override
    public void onSurfaceChanged(final GL10 gl, final int width, final int height) {
        mOutputWidth = width;
        mOutputHeight = height;
        GLES20.glUseProgram(mFilter.getProgram());
        mFilter.onOutputSizeChanged(width, height);
        adjustImageScaling();
        synchronized (mSurfaceChangedWaiter) {
            mSurfaceChangedWaiter.notifyAll();
        }
    }

    // profiling field
    private Profiler mProfiler;
    static long frameStart;
    static long yuv2rgba;
    static long preDraw;
    static long readPixels;
    static long rgba2yuv;
    static long draw;

    @Override
    public void onDrawFrame(final GL10 gl) {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);

        frameStart = System.nanoTime();
        runAll(mRunOnDraw);
        preDraw = System.nanoTime();
        if (!isResumed()) {
            return;
        }
        mFilter.onDraw(mGLTextureId, mGLCubeBuffer, mGLTextureBuffer);
        draw = System.nanoTime();
        runAll(mRunOnDrawEnd);
        if (mSurfaceTexture != null) {
            mSurfaceTexture.updateTexImage();
        }
        if (mProfiler != null) {
            mProfiler.metric((preDraw - frameStart) / 1_000_000,
                    (yuv2rgba - frameStart) / 1_000_000, (draw - preDraw) / 1_000_000,
                    (readPixels - preDraw) / 1_000_000, (rgba2yuv - readPixels) / 1_000_000);
        }
    }

    /**
     * Sets the background color
     *
     * @param red red color value
     * @param green green color value
     * @param blue red color value
     */
    public void setBackgroundColor(float red, float green, float blue) {
        mBackgroundRed = red;
        mBackgroundGreen = green;
        mBackgroundBlue = blue;
    }

    private void runAll(Queue<Runnable> queue) {
        synchronized (queue) {
            while (!queue.isEmpty()) {
                queue.poll().run();
            }
        }
    }

    public void switchFilter() {
        runOnDraw(new Runnable() {
            @Override
            public void run() {
                mEnableFilter = !mEnableFilter;
                GLFilterGroup oldFilter = mFilter;
                mFilter = mEnableFilter ? mDesiredFilter : mIdleFilterGroup;
                if (oldFilter != null) {
                    oldFilter.destroy();
                }
                mFilter.updateMergedFilters();
                mFilter.init();
                GLES20.glUseProgram(mFilter.getProgram());
                mFilter.onOutputSizeChanged(mOutputWidth, mOutputHeight);
                mFilter.onImageSizeChanged(mVideoWidth, mVideoHeight);
            }
        });
    }

    public synchronized void pauseDrawing() {
        mIsPaused = true;
        mIsDrawing = false;
    }

    public synchronized void resumeDrawing() {
        mIsPaused = false;
    }

    private synchronized void drawingResumed() {
        mIsDrawing = true;
    }

    private synchronized boolean isPaused() {
        return mIsPaused;
    }

    private synchronized boolean isResumed() {
        return mIsDrawing;
    }

    public void cameraSwitched() {
        mFilter.cameraSwitched();
    }

    @Override
    public void onFrameData(final byte[] data, final int width, final int height,
            final Runnable postProcessedTask) {
        if (mGLRgbaBuffer == null) {
            mGLRgbaBuffer = ByteBuffer.allocateDirect(width * height * 4);
        }
        if (mGLYuvBuffer == null) {
            // 16 bytes alignment
            int bufHeight = (width * mOutputWidth / mOutputHeight) & 0xfffffff0;
            mGLYuvBuffer = ByteBuffer.allocateDirect(width * bufHeight * 3 / 2);
        }
        if (mRunOnDraw.isEmpty()) {
            runOnDraw(new Runnable() {
                @Override
                public void run() {
                    if (isPaused()) {
                        postProcessedTask.run();
                        return;
                    }
                    if (mImageWidth == 0) {
                        mImageWidth = width;
                        mImageHeight = height;
                        mVideoWidth = mImageWidth;
                        // 16 bytes alignment
                        mVideoHeight = (mImageWidth * mOutputWidth / mOutputHeight) & 0xfffffff0;
                        mFilter.onImageSizeChanged(mVideoWidth, mVideoHeight);
                        mVideoCaptureCallback.onVideoSizeChanged(mVideoWidth, mVideoHeight);
                        adjustImageScaling();
                    }
                    if (!mEnableFilter) {
                        if (mRotation == Rotation.ROTATION_90) {
                            RgbYuvConverter.yuvCropRotateC180(width, height, data, mVideoHeight,
                                    mGLYuvBuffer.array());
                        } else {
                            RgbYuvConverter.yuvCrop(width, height, data, mVideoHeight,
                                    mGLYuvBuffer.array());
                        }
                        mVideoCaptureCallback.onFrameData(mGLYuvBuffer.array(), width,
                                mVideoHeight);
                    }
                    RgbYuvConverter.yuv2rgba(width, height, data, mGLRgbaBuffer.array());
                    yuv2rgba = System.nanoTime();

                    mGLTextureId = GLUtils.loadTexture(mGLRgbaBuffer, width, height, mGLTextureId);
                    postProcessedTask.run();

                    if (!isPaused()) {
                        drawingResumed();
                    }
                }
            });
        }
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    @Override
    public void onFrameData(final Image image, final Runnable postProcessedTask) {
        Rect crop = image.getCropRect();
        final int width = crop.width();
        final int height = crop.height();
        if (mGLRgbaBuffer == null) {
            mGLRgbaBuffer = ByteBuffer.allocateDirect(width * height * 4);
        }
        if (mGLYuvBuffer == null) {
            // 16 bytes alignment
            int bufHeight = (width * mOutputWidth / mOutputHeight) & 0xfffffff0;
            mGLYuvBuffer = ByteBuffer.allocateDirect(width * bufHeight * 3 / 2);
        }
        if (mRunOnDraw.isEmpty()) {
            runOnDraw(new Runnable() {
                @Override
                public void run() {
                    if (isPaused()) {
                        postProcessedTask.run();
                        return;
                    }
                    if (mImageWidth == 0) {
                        mImageWidth = width;
                        mImageHeight = height;
                        mVideoWidth = mImageWidth;
                        // 16 bytes alignment
                        mVideoHeight = (mImageWidth * mOutputWidth / mOutputHeight) & 0xfffffff0;
                        mFilter.onImageSizeChanged(mVideoWidth, mVideoHeight);
                        mVideoCaptureCallback.onVideoSizeChanged(mVideoWidth, mVideoHeight);
                        adjustImageScaling();
                    }
                    if (!mEnableFilter) {
                        if (mRotation == Rotation.ROTATION_90) {
                            RgbYuvConverter.image2yuvCropRotateC180(image, mVideoHeight,
                                    mGLYuvBuffer.array());
                        } else {
                            RgbYuvConverter.image2yuvCrop(image, mVideoHeight,
                                    mGLYuvBuffer.array());
                        }
                        mVideoCaptureCallback.onFrameData(mGLYuvBuffer.array(), width,
                                mVideoHeight);
                    }
                    RgbYuvConverter.image2rgba(image, mGLRgbaBuffer.array());
                    yuv2rgba = System.nanoTime();

                    mGLTextureId = GLUtils.loadTexture(mGLRgbaBuffer, width, height, mGLTextureId);
                    postProcessedTask.run();

                    if (!isPaused()) {
                        drawingResumed();
                    }
                }
            });
        } else {
            postProcessedTask.run();
        }
    }

    public void setUpSurfaceTexture(final SurfaceTextureInitCallback callback) {
        runOnDraw(new Runnable() {
            @Override
            public void run() {
                int[] textures = new int[1];
                GLES20.glGenTextures(1, textures, 0);
                mSurfaceTexture = new SurfaceTexture(textures[0]);
                callback.onSurfaceTextureInitiated(mSurfaceTexture);
            }
        });
    }

    public void setScaleType(GPUImage.ScaleType scaleType) {
        mScaleType = scaleType;
    }

    protected int getFrameWidth() {
        return mOutputWidth;
    }

    protected int getFrameHeight() {
        return mOutputHeight;
    }

    private void adjustImageScaling() {
        float outputWidth = mOutputWidth;
        float outputHeight = mOutputHeight;
        if (mRotation == Rotation.ROTATION_270 || mRotation == Rotation.ROTATION_90) {
            outputWidth = mOutputHeight;
            outputHeight = mOutputWidth;
        }

        float ratio1 = outputWidth / mImageWidth;
        float ratio2 = outputHeight / mImageHeight;
        float ratioMax = Math.max(ratio1, ratio2);
        int imageWidthNew = Math.round(mImageWidth * ratioMax);
        int imageHeightNew = Math.round(mImageHeight * ratioMax);

        float ratioWidth = imageWidthNew / outputWidth;
        float ratioHeight = imageHeightNew / outputHeight;

        float[] cube = CUBE;
        float[] textureCords =
                TextureRotationUtil.getRotation(mRotation, mFlipHorizontal, mFlipVertical);
        if (mScaleType == GPUImage.ScaleType.CENTER_CROP) {
            float distHorizontal = (1 - 1 / ratioWidth) / 2;
            float distVertical = (1 - 1 / ratioHeight) / 2;
            textureCords = new float[] {
                    addDistance(textureCords[0], distHorizontal),
                    addDistance(textureCords[1], distVertical),
                    addDistance(textureCords[2], distHorizontal),
                    addDistance(textureCords[3], distVertical),
                    addDistance(textureCords[4], distHorizontal),
                    addDistance(textureCords[5], distVertical),
                    addDistance(textureCords[6], distHorizontal),
                    addDistance(textureCords[7], distVertical),
            };
        } else {
            cube = new float[] {
                    CUBE[0] / ratioHeight, CUBE[1] / ratioWidth, CUBE[2] / ratioHeight,
                    CUBE[3] / ratioWidth, CUBE[4] / ratioHeight, CUBE[5] / ratioWidth,
                    CUBE[6] / ratioHeight, CUBE[7] / ratioWidth,
            };
        }

        mGLCubeBuffer.clear();
        mGLCubeBuffer.put(cube).position(0);
        mGLTextureBuffer.clear();
        mGLTextureBuffer.put(textureCords).position(0);
    }

    private float addDistance(float coordinate, float distance) {
        return coordinate == 0.0f ? distance : 1 - distance;
    }

    public void setRotationCamera(final Rotation rotation, final boolean flipHorizontal,
            final boolean flipVertical) {
        setRotation(rotation, flipVertical, flipHorizontal);
    }

    public void setRotation(final Rotation rotation) {
        mRotation = rotation;
        adjustImageScaling();
    }

    public void setRotation(final Rotation rotation, final boolean flipHorizontal,
            final boolean flipVertical) {
        mFlipHorizontal = flipHorizontal;
        mFlipVertical = flipVertical;
        setRotation(rotation);
    }

    public Rotation getRotation() {
        return mRotation;
    }

    public boolean isFlippedHorizontally() {
        return mFlipHorizontal;
    }

    public boolean isFlippedVertically() {
        return mFlipVertical;
    }

    protected void runOnDraw(final Runnable runnable) {
        synchronized (mRunOnDraw) {
            mRunOnDraw.add(runnable);
        }
    }

    private void clearOnDrawRunnable() {
        synchronized (mRunOnDraw) {
            mRunOnDraw.clear();
        }
    }

    protected void runOnDrawEnd(final Runnable runnable) {
        synchronized (mRunOnDrawEnd) {
            mRunOnDrawEnd.add(runnable);
        }
    }
}
