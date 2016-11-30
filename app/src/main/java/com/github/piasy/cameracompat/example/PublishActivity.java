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

package com.github.piasy.cameracompat.example;

import android.os.Bundle;
import android.support.annotation.WorkerThread;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.Toast;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import com.github.piasy.cameracompat.CameraCompat;
import com.github.piasy.cameracompat.processor.basic.BasicBeautifyProcessor;
import com.yatatsu.autobundle.AutoBundle;
import com.yatatsu.autobundle.AutoBundleField;

public class PublishActivity extends AppCompatActivity implements CameraCompat.VideoCaptureCallback,
        CameraCompat.ErrorHandler {

    @AutoBundleField
    boolean mBeautifyCapable;

    @BindView(R2.id.mPreviewContainer)
    View mContainer;
    @BindView(R2.id.mBtnSwitchBeautify)
    View mBtnSwitchBeautify;

    private CameraCompat mCameraCompat;

    private boolean mHide = false;
    private boolean mIsBig = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        AutoBundle.bind(this);
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED);

        setContentView(R.layout.activity_publish);
        ButterKnife.bind(this);

        if (!mBeautifyCapable) {
            mBtnSwitchBeautify.setVisibility(View.GONE);
        }

        start();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        CameraCompat.reset();
    }

    @OnClick(R2.id.mBtnSwitchBeautify)
    public void switchBeautify() {
        mCameraCompat.switchBeautify();
    }

    @OnClick(R2.id.mBtnSwitchCamera)
    public void switchCamera() {
        mCameraCompat.switchCamera();
    }

    @OnClick(R2.id.mBtnSwitchFlash)
    public void switchFlash() {
        mCameraCompat.switchFlash();
    }

    @OnClick(R2.id.mBtnSwitchMirror)
    public void switchMirror() {
        mCameraCompat.switchMirror();
    }

    @OnClick(R2.id.mBtnSwitchVisibility)
    public void switchVisibility() {
        if (mHide) {
            mHide = false;
            mContainer.setVisibility(View.VISIBLE);
        } else {
            mHide = true;
            mContainer.setVisibility(View.INVISIBLE);
        }
    }

    @OnClick(R2.id.mBtnResize)
    public void resize() {
        if (mIsBig) {
            mIsBig = false;
            FrameLayout.LayoutParams params
                    = (FrameLayout.LayoutParams) mContainer.getLayoutParams();
            params.height = 300;
            params.width = 160;
            mContainer.setLayoutParams(params);

            getSupportFragmentManager().beginTransaction()
                    .remove(getSupportFragmentManager().findFragmentByTag(
                            CameraCompat.CAMERA_PREVIEW_FRAGMENT))
                    .commit();

            start();
        } else {
            mIsBig = true;
            FrameLayout.LayoutParams params
                    = (FrameLayout.LayoutParams) mContainer.getLayoutParams();
            params.height = ViewGroup.LayoutParams.MATCH_PARENT;
            params.width = ViewGroup.LayoutParams.MATCH_PARENT;
            mContainer.setLayoutParams(params);

            getSupportFragmentManager().beginTransaction()
                    .remove(getSupportFragmentManager().findFragmentByTag(
                            CameraCompat.CAMERA_PREVIEW_FRAGMENT))
                    .commit();

            start();
        }
    }

    private void start() {
        CameraCompat.Builder builder = new CameraCompat.Builder(this, this)
                .beautifyOn(false)      // default beautify option
                .flashOpen(false)       // default flash option
                .previewWidth(639)      // preview width
                .previewHeight(479)     // preview height
                .enableMirror(false)    // default mirror option
                .frontCamera(false);    // default camera option
        if (mBeautifyCapable) {
            // add beautify processor, sorry that BasicBeautifyProcessor doesn't work now,
            // but in our production app, our real beautify processor works well,
            // I'm not an expert about open-gl, pr is welcome!
            builder.addProcessor(new BasicBeautifyProcessor());
        }
        mCameraCompat = builder.build();
        mCameraCompat.startPreview(null,
                getSupportFragmentManager(),
                R.id.mPreviewContainer);    // add this ViewGroup in your layout
    }

    @WorkerThread
    @Override
    public void onVideoSizeChanged(int width, int height) {
        Log.d("PublishActivity", "onVideoSizeChanged width = " + width
                                 + ", height = " + height);
    }

    @WorkerThread
    @Override
    public void onFrameData(final byte[] data, final int width, final int height) {
        Log.d("PublishActivity", "onFrameData width = " + width
                                 + ", height = " + height
                                 + ", data length = " + data.length);
    }

    @WorkerThread
    @Override
    public void onError(@CameraCompat.ErrorCode int code) {
        runOnUiThread(() ->
                Toast.makeText(this, "@CameraCompat.ErrorCode " + code, Toast.LENGTH_SHORT).show());
    }
}
