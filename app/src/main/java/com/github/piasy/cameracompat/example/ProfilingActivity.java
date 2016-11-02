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

import android.os.Build;
import android.os.Bundle;
import android.support.annotation.WorkerThread;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.TextView;
import com.github.piasy.cameracompat.CameraCompat;
import com.github.piasy.cameracompat.utils.Profiler;

public class ProfilingActivity extends AppCompatActivity
        implements CameraCompat.VideoCaptureCallback, Profiler.MetricListener,
        CameraCompat.ErrorHandler {

    private volatile boolean mStarted;

    private int mFrameCount;
    private long preDraw;
    private long yuv2rgba;
    private long draw;
    private long readPixels;
    private long rgba2yuv;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profiling);

        start();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    private void start() {
        CameraCompat cameraCompat = new CameraCompat.Builder(this, this).beautifyOn(true)
                .frontCamera(true)
                .metricListener(this)
                .build();
        cameraCompat.startPreview(null, getSupportFragmentManager(), R.id.mPreviewContainer);

        final TextView tvResult = (TextView) findViewById(R.id.mTvResult);
        findViewById(R.id.mPreviewContainer).postDelayed(() -> {
            tvResult.setVisibility(View.VISIBLE);
            tvResult.setText(Build.BRAND + "_" + Build.MODEL + "_" + Build.DEVICE
                             + "\nper frame: " + ((double) (preDraw + draw) / mFrameCount)
                             + "\n\tyuv2rgba: " + ((double) yuv2rgba / mFrameCount)
                             + "\n\treadPixels: " + ((double) readPixels / mFrameCount)
                             + "\n\trgba2yuv: " + ((double) rgba2yuv / mFrameCount));
        }, 30_000);
    }

    private void startPublish(int videoWidth, int videoHeight) {

    }

    @WorkerThread
    @Override
    public void onVideoSizeChanged(int width, int height) {
        startPublish(width, height);
    }

    @WorkerThread
    @Override
    public void onFrameData(final byte[] data, final int width, final int height) {
        if (mStarted) {
        }
    }

    @Override
    public void onMetric(Profiler.Metric metric) {
        mFrameCount++;
        preDraw += metric.preDraw;
        yuv2rgba += metric.yuv2rgba;
        draw += metric.draw;
        readPixels += metric.readPixels;
        rgba2yuv += metric.rgba2yuv;
    }

    @WorkerThread
    @Override
    public void onError(@CameraCompat.ErrorCode int code) {
    }
}
