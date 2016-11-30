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
import android.graphics.SurfaceTexture;
import android.media.Image;
import android.os.Build;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import com.github.piasy.cameracompat.CameraCompat;
import com.github.piasy.cameracompat.gpuimage.SurfaceInitCallback;
import java.nio.ByteBuffer;
import jp.co.cyberagent.android.gpuimage.Rotation;

/**
 * Created by Piasy{github.com/Piasy} on 18/10/2016.
 */

public class DirectChain implements ProcessorChain, TextureView.SurfaceTextureListener {
    private final CameraCompat.VideoCaptureCallback mVideoCaptureCallback;

    private TextureView mTextureView;
    private volatile SurfaceInitCallback mPendingNotify = null;

    private Rotation mRotation;

    private int mOutputWidth;
    private int mOutputHeight;
    private volatile int mVideoWidth;
    private volatile int mVideoHeight;

    private volatile boolean mIsFrontCamera;
    private volatile boolean mEnableMirror;

    private ByteBuffer mGLYuvBuffer;

    public DirectChain(boolean defaultFrontCamera,
            CameraCompat.VideoCaptureCallback videoCaptureCallback) {
        mVideoCaptureCallback = videoCaptureCallback;
        mIsFrontCamera = defaultFrontCamera;
    }

    @Override
    public void setUp() {
    }

    @Override
    public View createSurface(Context context) {
        mTextureView = new TextureView(context);
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        mTextureView.setLayoutParams(params);
        mTextureView.setKeepScreenOn(true);
        return mTextureView;
    }

    @Override
    public void initSurface(Context context) {
        mTextureView.setSurfaceTextureListener(this);
    }

    @Override
    public void onCameraOpened(Rotation rotation, boolean flipHorizontal, boolean flipVertical,
            SurfaceInitCallback callback) {
        mRotation = rotation;
        if (mTextureView.getSurfaceTexture() != null) {
            callback.onSurfaceTextureInitiated(mTextureView.getSurfaceTexture());
            mTextureView.post(this::adjustImageScaling);
        } else {
            mPendingNotify = callback;
        }
    }

    @Override
    public void pause() {
    }

    @Override
    public void resume() {
    }

    @Override
    public void cameraSwitched() {
        mIsFrontCamera = !mIsFrontCamera;
    }

    @Override
    public void switchMirror() {
        mEnableMirror = !mEnableMirror;
    }

    @Override
    public void tearDown() {
    }

    @Override
    public void onFrameData(byte[] data, int width, int height,
            Runnable postProcessedTask) {
        notifyVideoSizeChanged(width, height);
        sendNormalImage(width, height, data);
        postProcessedTask.run();
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    @Override
    public void onFrameData(Image image, Runnable postProcessedTask) {
        notifyVideoSizeChanged(image.getWidth(), image.getHeight());
        sendNormalImage(image);
        postProcessedTask.run();
    }

    /**
     * direct chain won't change frame size, so the size is ignored, but actually they are equal.
     */
    private void notifyVideoSizeChanged(int width, int height) {
        if (mVideoWidth != 0) {
            return;
        }
        mVideoWidth = width;
        mVideoHeight = height;
        if (mGLYuvBuffer == null) {
            mGLYuvBuffer = ByteBuffer.allocateDirect(mVideoWidth * mVideoHeight * 3 / 2);
        }
        mVideoCaptureCallback.onVideoSizeChanged(mVideoWidth, mVideoHeight);
        mTextureView.post(this::adjustImageScaling);
    }

    private void adjustImageScaling() {
        if (mRotation == null || mVideoWidth == 0 || mVideoHeight == 0) {
            return;
        }
        float videoWidth = mVideoWidth;
        float videoHeight = mVideoHeight;
        if (mRotation == Rotation.ROTATION_270 || mRotation == Rotation.ROTATION_90) {
            videoWidth = mVideoHeight;
            videoHeight = mVideoWidth;
        }
        float ratioW = mOutputWidth / videoWidth;
        float ratioH = mOutputHeight / videoHeight;
        float ratioMax = Math.max(ratioW, ratioH);
        int outputWidthNew = Math.round(videoWidth * ratioMax);
        int outputHeightNew = Math.round(videoHeight * ratioMax);
        FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) mTextureView.getLayoutParams();
        params.width = outputWidthNew;
        params.height = outputHeightNew;
        if (ratioW > ratioH) {
            params.topMargin = -((int) Math.ceil((outputHeightNew - mOutputHeight) / 2));
        } else {
            params.leftMargin = -((int) Math.ceil((outputWidthNew - mOutputWidth) / 2));
        }
        mTextureView.setLayoutParams(params);
    }

    private void sendNormalImage(int width, int height, byte[] data) {
        if (mIsFrontCamera && mEnableMirror) {
            if (mRotation == Rotation.ROTATION_90) {
                RgbYuvConverter.yuvCropFlip(width, height, data, mVideoHeight,
                        mGLYuvBuffer.array());
            } else {
                RgbYuvConverter.yuvCropRotateC180Flip(width, height, data, mVideoHeight,
                        mGLYuvBuffer.array());
            }
        } else {
            if (mRotation == Rotation.ROTATION_90) {
                RgbYuvConverter.yuvCropRotateC180(width, height, data, mVideoHeight,
                        mGLYuvBuffer.array());
            } else {
                RgbYuvConverter.yuvCrop(width, height, data, mVideoHeight, mGLYuvBuffer.array());
            }
        }
        mVideoCaptureCallback.onFrameData(mGLYuvBuffer.array(), width, mVideoHeight);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private void sendNormalImage(Image image) {
        if (mIsFrontCamera && mEnableMirror) {
            if (mRotation == Rotation.ROTATION_90) {
                RgbYuvConverter.image2yuvCropFlip(image, mVideoHeight, mGLYuvBuffer.array());
            } else {
                RgbYuvConverter.image2yuvCropRotateC180Flip(image, mVideoHeight,
                        mGLYuvBuffer.array());
            }
        } else {
            if (mRotation == Rotation.ROTATION_90) {
                RgbYuvConverter.image2yuvCropRotateC180(image, mVideoHeight, mGLYuvBuffer.array());
            } else {
                RgbYuvConverter.image2yuvCrop(image, mVideoHeight, mGLYuvBuffer.array());
            }
        }
        mVideoCaptureCallback.onFrameData(mGLYuvBuffer.array(), image.getWidth(), mVideoHeight);
    }

    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
        mOutputWidth = width;
        mOutputHeight = height;
        adjustImageScaling();
        if (mPendingNotify != null) {
            mPendingNotify.onSurfaceTextureInitiated(surface);
            mPendingNotify = null;
        }
    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
        return false;
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surface) {
    }
}
