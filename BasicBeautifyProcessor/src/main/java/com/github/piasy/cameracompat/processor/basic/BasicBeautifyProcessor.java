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

package com.github.piasy.cameracompat.processor.basic;

import android.media.Image;
import com.github.piasy.cameracompat.processor.Processor;
import java.util.ArrayList;
import java.util.List;
import jp.co.cyberagent.android.gpuimage.BasicBeautyFilter;
import jp.co.cyberagent.android.gpuimage.GPUImageFilter;

/**
 * Created by Piasy{github.com/Piasy} on 17/10/2016.
 */

public class BasicBeautifyProcessor implements Processor {

    @Override
    public void setUp() {
    }

    @Override
    public List<GPUImageFilter> getFilters() {
        List<GPUImageFilter> filters = new ArrayList<>();
        filters.add(new GPUImageFilter());
        filters.add(new BasicBeautyFilter(4));
        return filters;
    }

    @Override
    public void tearDown() {
    }

    @Override
    public void onFrameData(byte[] data, int width, int height,
            Runnable postProcessedTask) {

    }

    @Override
    public void onFrameData(Image image, Runnable postProcessedTask) {

    }
}
