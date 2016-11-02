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

import android.annotation.TargetApi;
import android.media.Image;
import android.os.Build;
import java.nio.ByteBuffer;

/**
 * Created by Piasy{github.com/Piasy} on 6/2/16.
 *
 * from http://www.equasys.de/colorconversion.html
 * https://en.wikipedia.org/wiki/YCbCr#ITU-R_BT.601_conversion
 *
 * packed mode, and Cr comes first, ref: http://stackoverflow.com/a/12610396/3077508
 *
 * {@code
 * float operation:
 * R = 1.164 * (Y-16)                    + 1.596 * (Cr-128)
 * G = 1.164 * (Y-16) - 0.392 * (Cb-128) - 0.813 * (Cr-128)
 * B = 1.164 * (Y-16) + 2.017 * (Cb-128)
 *
 * transform to bit operation:
 * 1.164 = 1 + (1 >> 3) + (1 >> 5) + (1 >> 7) ~= 1 + (1 >> 3) + (1 >> 5) = 1.15625 , 0.0078125 , 2
 * 1.596 = 1 + (1 >> 1) + (1 >> 4) + (1 >> 5)
 * 0.392 =     (1 >> 2) + (1 >> 3) + (1 >> 6) ~=     (1 >> 1) - (1 >> 3) = 0.375   , 0.017     , 4.352
 * 0.813 =     (1 >> 1) + (1 >> 2) + (1 >> 4) ~= 1 - (1 >> 3) - (1 >> 4) = 0.8125  , 0.0005    , 0.128
 * 2.017 = 2 + (1 >> 6)                       ~= 2                                 , 0.017     , 4
 *
 * Y = Y - 16, Cb = Cb - 128, Cr = Cr - 128
 * Y = Y + (Y >> 3) + (Y >> 5) + (Y >> 7)
 *
 * R = Y                                + Cr + (Cr >> 1) + (Cr >> 4) + (Cr >> 5)
 * G = Y - (Cb >> 2) - (Cb >> 3) - (Cb >> 6) - (Cr >> 1) - (Cr >> 2) - (Cr >> 4)
 * B = Y + (Cb << 1) + (Cb >> 6)
 * }
 *
 * Perf:
 * float           640*480: 458060886 ns
 * bit             640*480: 406342604 ns,   12.7 % faster than float
 * jni (bit)       640*480:  15664062 ns, 2494.1 % faster than bit
 * jni (GpuImage)  640*480:  13116771 ns,   19.4 % faster than jni, but why??
 *
 *
 * {@code
 * float operation:
 * Y  =  0.257 * R + 0.504 * G + 0.098 * B + 16
 * Cb = -0.148 * R - 0.291 * G + 0.439 * B + 128
 * Cr =  0.439 * R - 0.368 * G - 0.071 * B + 128
 *
 * transform to bit operation:
 * 0.257 = (1 >> 2) + (1 >> 7)
 * 0.504 = (1 >> 1) + (1 >> 8)
 * 0.098 = (1 >> 4) + (1 >> 5) + (1 >> 8)
 * 0.148 = (1 >> 3) + (1 >> 6) + (1 >> 7)
 * 0.291 = (1 >> 2) + (1 >> 5) + (1 >> 7)
 * 0.439 = (1 >> 2) + (1 >> 3) + (1 >> 4) = (1 >> 1) - (1 >> 4)
 * 0.368 = (1 >> 2) + (1 >> 3) - (1 >> 7)
 * 0.071 = (1 >> 4) + (1 >> 7)
 *
 * Y  =  (R >> 2) + (R >> 7)            + (G >> 1) + (G >> 8)            + (B >> 4) + (B >> 5) + (B >> 8) + 16
 * Cb = -(R >> 3) - (R >> 6) - (R >> 7) - (G >> 2) - (G >> 5) - (G >> 7) + (B >> 1) - (B >> 4) + 128
 * Cr =  (R >> 1) - (R >> 4)            - (G >> 2) - (G >> 3) + (G >> 7) - (B >> 4) - (B >> 7) + 128
 * }
 *
 * Perf:
 * float       640*480: 227084323 ns
 * bit         640*480: 181100573 ns, 25.4 % faster than float
 * jni (bit)   640*480:  11113646 ns, 1529.5 % faster than bit
 */
public class RgbYuvConverter {

    static {
        System.loadLibrary("rgb-yuv-converter-library");
    }

    public static native int yuv2rgba(int width, int height, byte[] yuvIn, byte[] rgbaOut);

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public static int image2rgba(Image image, byte[] rgbaOut) {
        Image.Plane[] planes = image.getPlanes();
        ByteBuffer Y = planes[0].getBuffer();
        ByteBuffer Cr = planes[2].getBuffer();
        int CrPixelStride = planes[2].getPixelStride();
        ByteBuffer Cb = planes[1].getBuffer();
        int CbPixelStride = planes[1].getPixelStride();
        return image2rgba(image.getWidth(), image.getHeight(), Y, Cr, Cb, CrPixelStride,
                CbPixelStride, rgbaOut);
    }

    private static native int image2rgba(int width, int height, ByteBuffer Y, ByteBuffer Cr,
            ByteBuffer Cb, int CrPixelStride, int CbPixelStride, byte[] rgbaOut);

    /**
     * rotate 90 degree in counter clockwise and change to yuv
     */
    public static native int rgba2yuvRotateC90(int width, int height, byte[] rgbaIn, byte[] yuvOut);

    /**
     * rotate 90 degree in counter clockwise and change to yuv
     */
    public static native int rgba2yuvRotateC90Flip(int width, int height, byte[] rgbaIn,
            byte[] yuvOut);

    /**
     * rotate 180 degree in counter clockwise
     */
    public static native int yuvCropRotateC180(int width, int height, byte[] yuvIn,
            int outputHeight, byte[] yuvOut);

    public static native int yuvCrop(int width, int height, byte[] yuvIn,
            int outputHeight, byte[] yuvOut);

    /**
     * rotate 180 degree in counter clockwise and change to yuv
     */
    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public static int image2yuvCropRotateC180(Image imageIn, int outputHeight, byte[] yuvOut) {
        Image.Plane[] planes = imageIn.getPlanes();
        ByteBuffer Y = planes[0].getBuffer();
        ByteBuffer Cr = planes[2].getBuffer();
        int CrPixelStride = planes[2].getPixelStride();
        ByteBuffer Cb = planes[1].getBuffer();
        int CbPixelStride = planes[1].getPixelStride();
        return image2yuvCropRotateC180(imageIn.getWidth(), imageIn.getHeight(), Y, Cr, Cb,
                CrPixelStride, CbPixelStride, outputHeight, yuvOut);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public static int image2yuvCrop(Image imageIn, int outputHeight, byte[] yuvOut) {
        Image.Plane[] planes = imageIn.getPlanes();
        ByteBuffer Y = planes[0].getBuffer();
        ByteBuffer Cr = planes[2].getBuffer();
        int CrPixelStride = planes[2].getPixelStride();
        ByteBuffer Cb = planes[1].getBuffer();
        int CbPixelStride = planes[1].getPixelStride();
        return image2yuvCrop(imageIn.getWidth(), imageIn.getHeight(), Y, Cr, Cb, CrPixelStride,
                CbPixelStride, outputHeight, yuvOut);
    }

    private static native int image2yuvCropRotateC180(int width, int height, ByteBuffer Y,
            ByteBuffer Cr, ByteBuffer Cb, int CrPixelStride, int CbPixelStride, int outputWidth,
            byte[] yuvOut);

    private static native int image2yuvCrop(int width, int height, ByteBuffer Y, ByteBuffer Cr,
            ByteBuffer Cb, int CrPixelStride, int CbPixelStride, int outputWidth, byte[] yuvOut);
}
