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

import android.annotation.TargetApi;
import android.graphics.Bitmap;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.media.Image;
import android.os.Build;
import android.os.Environment;
import android.util.Log;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.Buffer;
import java.nio.ByteBuffer;

/**
 * Created by Piasy{github.com/Piasy} on 5/27/16.
 */
@TargetApi(Build.VERSION_CODES.LOLLIPOP)
public class ImageUtils {

    public static byte[] getDataFromImage(Image image) {
        Rect crop = image.getCropRect();
        int format = image.getFormat();
        int width = crop.width();
        int height = crop.height();
        Image.Plane[] planes = image.getPlanes();
        byte[] data = new byte[width * height * ImageFormat.getBitsPerPixel(format) / 8];
        byte[] rowData = new byte[planes[0].getRowStride()];
        int channelOffset = 0;
        int outputStride = 1;
        for (int i = 0; i < planes.length; i++) {
            switch (i) {
                case 0:
                    channelOffset = 0;
                    outputStride = 1;
                    break;
                case 1:
                    channelOffset = width * height;
                    outputStride = 1;
                    break;
                case 2:
                    channelOffset = (int) (width * height * 1.25);
                    outputStride = 1;
                    break;
            }
            ByteBuffer buffer = planes[i].getBuffer();
            int rowStride = planes[i].getRowStride();
            int pixelStride = planes[i].getPixelStride();
            int shift = (i == 0) ? 0 : 1;
            int w = width >> shift;
            int h = height >> shift;
            buffer.position(rowStride * (crop.top >> shift) + pixelStride * (crop.left >> shift));
            for (int row = 0; row < h; row++) {
                int length;
                if (pixelStride == 1) {
                    length = w;
                    buffer.get(data, channelOffset, length);
                    channelOffset += length;
                } else {
                    length = (w - 1) * pixelStride + 1;
                    buffer.get(rowData, 0, length);
                    for (int col = 0; col < w; col++) {
                        data[channelOffset] = rowData[col * pixelStride];
                        channelOffset += outputStride;
                    }
                }
                if (row < h - 1) {
                    buffer.position(buffer.position() + rowStride - length);
                }
            }
        }
        return data;
    }

    public static void image2yuv(Image imageIn, byte[] yuvOut) {
        Image.Plane[] planes = imageIn.getPlanes();
        ByteBuffer Y = planes[0].getBuffer();
        ByteBuffer Cr = planes[2].getBuffer();
        int CrPixelStride = planes[2].getPixelStride();
        ByteBuffer Cb = planes[1].getBuffer();
        int CbPixelStride = planes[1].getPixelStride();
        for (int i = 0, size = imageIn.getWidth() * imageIn.getHeight(); i < size; i++) {
            yuvOut[i] = Y.get(i);
        }
        for (int i = 0, size = imageIn.getWidth() * imageIn.getHeight(); i < size / 4; i++) {
            yuvOut[size + i * 2] = Cr.get(i * CrPixelStride);
            yuvOut[size + i * 2 + 1] = Cb.get(i * CbPixelStride);
        }
        try {
            File file = new File(
                    Environment.getExternalStorageDirectory().getAbsolutePath() + "/dump.y");
            FileOutputStream outputStream = new FileOutputStream(file);
            byte[] bytes = new byte[Y.remaining()];
            Y.get(bytes);
            outputStream.write(bytes);
            outputStream.close();

            file = new File(
                    Environment.getExternalStorageDirectory().getAbsolutePath() + "/dump.cb");
            outputStream = new FileOutputStream(file);
            bytes = new byte[Cb.remaining()];
            Cb.get(bytes);
            outputStream.write(bytes);
            outputStream.close();

            file = new File(
                    Environment.getExternalStorageDirectory().getAbsolutePath() + "/dump.cr");
            outputStream = new FileOutputStream(file);
            bytes = new byte[Cr.remaining()];
            Cr.get(bytes);
            outputStream.write(bytes);
            outputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void saveRgb2Bitmap(Buffer buf, String filename, int width, int height) {
        // Save the generated bitmap to a PNG so we can see what it looks like.
        Log.d("CameraCompat", "Creating "
                + Environment.getExternalStorageDirectory().getAbsolutePath()
                + "/"
                + filename);
        BufferedOutputStream bos = null;
        try {
            bos = new BufferedOutputStream(new FileOutputStream(
                    Environment.getExternalStorageDirectory().getAbsolutePath() + "/" + filename));
            Bitmap bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            bmp.copyPixelsFromBuffer(buf);
            bmp.compress(Bitmap.CompressFormat.PNG, 90, bos);
            bmp.recycle();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (bos != null) {
                try {
                    bos.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public static void saveRawRgbData(ByteBuffer buf, int width, int height, String prefix) {
        // Save the generated bitmap to a PNG so we can see what it looks like.
        String filename = Environment.getExternalStorageDirectory().getAbsolutePath()
                + "/"
                + prefix
                + "_"
                + width
                + "_"
                + height
                + ".rgb";
        Log.d("CameraCompat", "Creating " + filename);
        FileOutputStream outputStream;
        try {
            outputStream = new FileOutputStream(filename);
            outputStream.write(buf.array());
            outputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void saveRawRgbData(byte[] buf, int width, int height) {
        // Save the generated bitmap to a PNG so we can see what it looks like.
        String filename = Environment.getExternalStorageDirectory().getAbsolutePath()
                + "/dump_r_"
                + width
                + "_"
                + height
                + ".rgb";
        Log.d("CameraCompat", "Creating " + filename);
        FileOutputStream outputStream;
        try {
            outputStream = new FileOutputStream(filename);
            outputStream.write(buf);
            outputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void saveRawYuvData(byte[] buf, int width, int height, String prefix) {
        // Save the generated bitmap to a PNG so we can see what it looks like.
        String filename = Environment.getExternalStorageDirectory().getAbsolutePath()
                + "/"
                + prefix
                + "_"
                + width
                + "_"
                + height
                + ".yuv";
        Log.d("CameraCompat", "Creating " + filename);
        FileOutputStream outputStream;
        try {
            outputStream = new FileOutputStream(filename);
            outputStream.write(buf);
            outputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
