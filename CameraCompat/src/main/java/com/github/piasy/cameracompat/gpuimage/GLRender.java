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

package com.github.piasy.cameracompat.gpuimage;

import android.graphics.SurfaceTexture;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import com.github.piasy.cameracompat.CameraCompat;
import com.github.piasy.cameracompat.compat.CameraFrameCallback;
import com.github.piasy.cameracompat.utils.GLUtil;
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
public class GLRender implements GLSurfaceView.Renderer {
    static final float CUBE[] = {
            -1.0f, -1.0f, 1.0f, -1.0f, -1.0f, 1.0f, 1.0f, 1.0f,
    };
    private static final int NO_IMAGE = -1;
    private final FloatBuffer mGLCubeBuffer;
    private final FloatBuffer mGLTextureBuffer;
    private final Queue<Runnable> mRunOnDraw;
    private final Queue<Runnable> mRunOnDrawEnd;
    private final GLFilterGroup mDesiredFilter;
    private final GLFilterGroup mIdleFilterGroup;
    private final VideoSizeChangedListener mVideoSizeChangedListener;

    // profiling field
    private final Profiler mProfiler;

    private GLFilterGroup mFilter;
    private int mGLTextureId = NO_IMAGE;
    private SurfaceTexture mSurfaceTexture = null;

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

    // only accessed in draw thread
    private boolean mEnableFilter;
    private boolean mIsPaused = false;
    private boolean mIsDrawing = true;

    public GLRender(final GLFilterGroup filter, boolean enableFilter,
            VideoSizeChangedListener videoSizeChangedListener, Profiler profiler) {
        mVideoSizeChangedListener = videoSizeChangedListener;
        mProfiler = profiler;
        mIdleFilterGroup = new GLFilterGroup(Collections.singletonList(new GPUImageFilter()));
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
    public void onSurfaceChanged(final GL10 unused, final int width, final int height) {
        mOutputWidth = width;
        mOutputHeight = height;
        GLES20.glUseProgram(mFilter.getProgram());
        mFilter.onOutputSizeChanged(width, height);
        adjustImageScaling();
    }

    @Override
    public void onDrawFrame(final GL10 gl) {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);

        try {
            runAll(mRunOnDraw);
            if (!isResumed()) {
                return;
            }
            mFilter.onDraw(mGLTextureId, mGLCubeBuffer, mGLTextureBuffer);
            runAll(mRunOnDrawEnd);
            if (mSurfaceTexture != null) {
                mSurfaceTexture.updateTexImage();
            }
            //if (mProfiler != null) {
            //    mProfiler.metric((preDraw - frameStart) / 1_000_000,
            //            (yuv2rgba - frameStart) / 1_000_000, (draw - preDraw) / 1_000_000,
            //            (readPixels - preDraw) / 1_000_000, (rgba2yuv - readPixels) / 1_000_000);
            //}
        } catch (RuntimeException | OutOfMemoryError error) {
            // throw from: runAll(mRunOnDraw) -> mFilter.onImageSizeChanged -> ByteBuffer
            // .allocateDirect
            CameraCompat.onError(CameraCompat.ERR_UNKNOWN);
        }
    }

    public void scheduleDrawFrame(ByteBuffer frame, int width, int height,
            Runnable postProcessedTask) {
        runOnDraw(() -> {
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
                mVideoSizeChangedListener.onVideoSizeChanged(mVideoWidth, mVideoHeight);
                adjustImageScaling();
            }

            mGLTextureId = GLUtil.loadTexture(frame, width, height, mGLTextureId);
            postProcessedTask.run();

            if (!isPaused()) {
                drawingResumed();
            }
        });
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
        runOnDraw(() -> {
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
        });
    }

    public synchronized void pauseDrawing() {
        mIsPaused = true;
        mIsDrawing = false;
    }

    public synchronized void resumeDrawing() {
        mIsPaused = false;
    }

    public synchronized void drawingResumed() {
        mIsDrawing = true;
    }

    public synchronized boolean isPaused() {
        return mIsPaused;
    }

    public synchronized boolean isEnableFilter() {
        return mEnableFilter;
    }

    private synchronized boolean isResumed() {
        return mIsDrawing;
    }

    public void setUpSurfaceTexture(final SurfaceInitCallback callback) {
        runOnDraw(() -> {
            int[] textures = new int[1];
            GLES20.glGenTextures(1, textures, 0);
            mSurfaceTexture = new SurfaceTexture(textures[0]);
            callback.onSurfaceTextureInitiated(mSurfaceTexture);
        });
    }

    public void setScaleType(GPUImage.ScaleType scaleType) {
        mScaleType = scaleType;
    }

    public int getFrameWidth() {
        return mOutputWidth;
    }

    public int getFrameHeight() {
        return mOutputHeight;
    }

    public int getVideoWidth() {
        return mVideoWidth;
    }

    public int getVideoHeight() {
        return mVideoHeight;
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

    public void setRotation(final Rotation rotation, final boolean flipHorizontal,
            final boolean flipVertical) {
        mFlipHorizontal = flipHorizontal;
        mFlipVertical = flipVertical;
        setRotation(rotation);
    }

    public Rotation getRotation() {
        return mRotation;
    }

    public void setRotation(final Rotation rotation) {
        mRotation = rotation;
        adjustImageScaling();
    }

    public boolean isFlippedHorizontally() {
        return mFlipHorizontal;
    }

    public boolean isFlippedVertically() {
        return mFlipVertical;
    }

    public boolean isBusyDrawing() {
        return !mRunOnDraw.isEmpty();
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

    public interface VideoSizeChangedListener {
        void onVideoSizeChanged(int width, int height);
    }
}
