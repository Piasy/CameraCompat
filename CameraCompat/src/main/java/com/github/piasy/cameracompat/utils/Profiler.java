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

package com.github.piasy.cameracompat.utils;

/**
 * Created by Piasy{github.com/Piasy} on 7/6/16.
 */

public class Profiler {
    private final MetricListener mMetricListener;

    public Profiler(MetricListener metricListener) {
        mMetricListener = metricListener;
    }

    public void metric(long preDraw, long yuv2rgba, long draw, long readPixels, long rgba2yuv) {
        if (preDraw <= 0 || yuv2rgba <= 0 || draw <= 0 || readPixels <= 0 || rgba2yuv <= 0) {
            return;
        }
        mMetricListener.onMetric(new Metric(preDraw, yuv2rgba, draw, readPixels, rgba2yuv));
    }

    public interface MetricListener {
        void onMetric(Metric metric);
    }

    public static class Metric {
        public final long preDraw;
        public final long yuv2rgba;
        public final long draw;
        public final long readPixels;
        public final long rgba2yuv;

        public Metric(long preDraw, long yuv2rgba, long draw, long readPixels, long rgba2yuv) {
            this.preDraw = preDraw;
            this.yuv2rgba = yuv2rgba;
            this.draw = draw;
            this.readPixels = readPixels;
            this.rgba2yuv = rgba2yuv;
        }
    }
}
