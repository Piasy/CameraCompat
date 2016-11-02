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

//
// Created by Piasy on 6/4/16.
//

#include <jni.h>
#include <android/log.h>

#define LOGI(...) \
  ((void)__android_log_print(ANDROID_LOG_INFO, "RgbYuvEncoder::", __VA_ARGS__))

JNIEXPORT int JNICALL Java_com_github_piasy_cameracompat_processor_RgbYuvConverter_yuv2rgba(
        JNIEnv *env, jobject obj, jint width, jint height, jbyteArray yuvIn, jbyteArray rgbaOut) {
    int R, G, B;
    int Y, Cb = 0, Cr = 0;
    int cOffset, pixelIndex, rOffset;

    jbyte *yuv = (jbyte *) (*env)->GetPrimitiveArrayCritical(env, yuvIn, 0);
    jbyte *rgba = (jbyte *) ((*env)->GetPrimitiveArrayCritical(env, rgbaOut, 0));

    int y = 0, size = width * height;
    for (; y < height; y++) {
        int x = 0;
        int y_div_2 = y >> 1;
        pixelIndex = y * width;
        for (; x < width; x++) {
            Y = yuv[pixelIndex];
            if (Y < 0) {
                Y += 255;
            }
            if ((x & 0x1) == 0) {
                cOffset = size + y_div_2 * width + x;
                Cr = yuv[cOffset];
                Cr = Cr < 0 ? Cr + 127 : Cr - 128;
                Cb = yuv[cOffset + 1];
                Cb = Cb < 0 ? Cb + 127 : Cb - 128;
            }
            Y = Y + (Y >> 3) + (Y >> 5);
            R = Y + Cr + (Cr >> 1) + (Cr >> 4) + (Cr >> 5);
            G = Y - (Cb >> 1) + (Cb >> 3) - Cr + (Cr >> 3) + (Cr >> 4);
            B = Y + (Cb << 1);
            rOffset = pixelIndex << 2;
            rgba[rOffset] = (jbyte) (R < 0 ? 0 : (R > 255 ? 255 : R));
            rgba[rOffset + 1] = (jbyte) (G < 0 ? 0 : (G > 255 ? 255 : G));
            rgba[rOffset + 2] = (jbyte) (B < 0 ? 0 : (B > 255 ? 255 : B));
            rgba[rOffset + 3] = (jbyte) 0xFF;
            pixelIndex++;
        }
    }

    (*env)->ReleasePrimitiveArrayCritical(env, yuvIn, yuv, 0);
    (*env)->ReleasePrimitiveArrayCritical(env, rgbaOut, rgba, 0);
    return 0;
}

JNIEXPORT int JNICALL Java_com_github_piasy_cameracompat_processor_RgbYuvConverter_image2rgba(
        JNIEnv *env, jobject obj, jint width, jint height, jobject YIn, jobject CrIn, jobject CbIn,
        int CrPixelStride, int CbPixelStride, jbyteArray rgbaOut) {
    int R, G, B;
    int Y, Cb = 0, Cr = 0;
    int cIndex, pixelIndex, rOffset;

    jbyte *YArray = (jbyte *) (*env)->GetDirectBufferAddress(env, YIn);
    jbyte *CrArray = (jbyte *) (*env)->GetDirectBufferAddress(env, CrIn);
    jbyte *CbArray = (jbyte *) (*env)->GetDirectBufferAddress(env, CbIn);
    if (YArray == 0 || CrArray == 0 || CbArray == 0) {
        return -1;
    }
    jbyte *rgba = (jbyte *) ((*env)->GetPrimitiveArrayCritical(env, rgbaOut, 0));

    int y = 0;
    for (; y < height; y++) {
        int x = 0;
        int y_div_2 = y >> 1;
        pixelIndex = y * width;
        for (; x < width; x++) {
            Y = YArray[pixelIndex];
            if (Y < 0) {
                Y += 255;
            }
            if ((x & 0x1) == 0) {
                cIndex = (y_div_2 * width + x) >> 1;
                Cr = CrArray[cIndex * CrPixelStride];
                Cr = Cr < 0 ? Cr + 127 : Cr - 128;
                Cb = CbArray[cIndex * CbPixelStride];
                Cb = Cb < 0 ? Cb + 127 : Cb - 128;
            }
            Y = Y + (Y >> 3) + (Y >> 5);
            R = Y + Cr + (Cr >> 1) + (Cr >> 4) + (Cr >> 5);
            G = Y - (Cb >> 1) + (Cb >> 3) - Cr + (Cr >> 3) + (Cr >> 4);
            B = Y + (Cb << 1);
            rOffset = pixelIndex << 2;
            rgba[rOffset] = (jbyte) (R < 0 ? 0 : (R > 255 ? 255 : R));
            rgba[rOffset + 1] = (jbyte) (G < 0 ? 0 : (G > 255 ? 255 : G));
            rgba[rOffset + 2] = (jbyte) (B < 0 ? 0 : (B > 255 ? 255 : B));
            rgba[rOffset + 3] = (jbyte) 0xFF;
            pixelIndex++;
        }
    }
    (*env)->ReleasePrimitiveArrayCritical(env, rgbaOut, rgba, 0);
    return 0;
}

JNIEXPORT int JNICALL Java_com_github_piasy_cameracompat_processor_RgbYuvConverter_rgba2yuv(
        JNIEnv *env, jobject obj, jint width, jint height, jintArray rgbaIn, jbyteArray yuvOut) {
    int R, G, B;
    int Y, Cb = 0, Cr = 0;
    int cOffset;
    int outputWidth = height, outputHeight = width;

    jint *rgba = (jint *) ((*env)->GetPrimitiveArrayCritical(env, rgbaIn, 0));
    jbyte *yuv = (jbyte *) (*env)->GetPrimitiveArrayCritical(env, yuvOut, 0);

    int y = 0, x, size = outputWidth * outputHeight;
    for (; y < outputHeight; y++) {
        x = 0;
        int y_M_width = y * outputWidth;
        int y_div2_M_width = (y >> 1) * outputWidth;
        for (; x < outputWidth; x++) {
            int rgbaIndex = outputHeight * (outputWidth - 1 - x) + y;
            int value = rgba[rgbaIndex];
            R = (value & 0x00FF0000) > 16;
            if (R < 0) {
                R += 256;
            }
            G = (value & 0x0000FF00) > 8;
            if (G < 0) {
                G += 256;
            }
            B = value & 0x000000FF;
            if (B < 0) {
                B += 256;
            }
            Y = (R >> 2) + (R >> 7) + (G >> 1) + (G >> 8) + (B >> 4) + (B >> 5) + (B >> 8) + 16;
            yuv[y_M_width + x] = (jbyte) Y;
            if ((y & 0x1) == 0 && (x & 0x1) == 0) {
                cOffset = size + y_div2_M_width + x;
                Cr = (R >> 1) - (R >> 4) - (G >> 2) - (G >> 3) + (G >> 7) - (B >> 4) - (B >> 7) +
                     128;
                yuv[cOffset] = (jbyte) Cr;
                Cb = -(R >> 3) - (R >> 6) - (R >> 7) - (G >> 2) - (G >> 5) - (G >> 7) + (B >> 1) -
                     (B >> 4) + 128;
                yuv[cOffset + 1] = (jbyte) Cb;
            }
        }
    }

    (*env)->ReleasePrimitiveArrayCritical(env, rgbaIn, rgba, 0);
    (*env)->ReleasePrimitiveArrayCritical(env, yuvOut, yuv, 0);
    return 0;
}

JNIEXPORT int JNICALL Java_com_github_piasy_cameracompat_processor_RgbYuvConverter_rgba2yuvRotateC90(
        JNIEnv *env, jobject obj, jint width, jint height, jbyteArray rgbaIn, jbyteArray yuvOut) {
    int R, G, B;
    int Y, Cb = 0, Cr = 0;
    int cOffset;
    int outputWidth = height, outputHeight = width;

    jbyte *rgba = (jbyte *) ((*env)->GetPrimitiveArrayCritical(env, rgbaIn, 0));
    jbyte *yuv = (jbyte *) (*env)->GetPrimitiveArrayCritical(env, yuvOut, 0);

    int y = 0, x, size = outputWidth * outputHeight;
    for (; y < outputHeight; y++) {
        x = 0;
        int y_M_width = y * outputWidth;
        int y_div2_M_width = (y >> 1) * outputWidth;
        for (; x < outputWidth; x++) {
            int rgbaIndex = (outputHeight * (outputWidth - 1 - x) + y) << 2;
            R = rgba[rgbaIndex];
            if (R < 0) {
                R += 256;
            }
            G = rgba[rgbaIndex + 1];
            if (G < 0) {
                G += 256;
            }
            B = rgba[rgbaIndex + 2];
            if (B < 0) {
                B += 256;
            }
            Y = (R >> 2) + (R >> 7) + (G >> 1) + (G >> 8) + (B >> 4) + (B >> 5) + (B >> 8) + 16;
            yuv[y_M_width + x] = (jbyte) Y;
            if ((y & 0x1) == 0 && (x & 0x1) == 0) {
                cOffset = size + y_div2_M_width + x;
                Cr = (R >> 1) - (R >> 4) - (G >> 2) - (G >> 3) + (G >> 7) - (B >> 4) - (B >> 7) +
                     128;
                yuv[cOffset] = (jbyte) Cr;
                Cb = -(R >> 3) - (R >> 6) - (R >> 7) - (G >> 2) - (G >> 5) - (G >> 7) + (B >> 1) -
                     (B >> 4) + 128;
                yuv[cOffset + 1] = (jbyte) Cb;
            }
        }
    }

    (*env)->ReleasePrimitiveArrayCritical(env, rgbaIn, rgba, 0);
    (*env)->ReleasePrimitiveArrayCritical(env, yuvOut, yuv, 0);
    return 0;
}

JNIEXPORT int JNICALL Java_com_github_piasy_cameracompat_processor_RgbYuvConverter_rgba2yuvRotateC90Flip(
        JNIEnv *env, jobject obj, jint width, jint height, jbyteArray rgbaIn, jbyteArray yuvOut) {
    int R, G, B;
    int Y, Cb = 0, Cr = 0;
    int cOffset;
    int outputWidth = height, outputHeight = width;

    jbyte *rgba = (jbyte *) ((*env)->GetPrimitiveArrayCritical(env, rgbaIn, 0));
    jbyte *yuv = (jbyte *) (*env)->GetPrimitiveArrayCritical(env, yuvOut, 0);

    int y = 0, x, size = outputWidth * outputHeight;
    for (; y < outputHeight; y++) {
        x = 0;
        int y_M_width = y * outputWidth;
        int y_div2_M_width = (y >> 1) * outputWidth;
        for (; x < outputWidth; x++) {
            int rgbaIndex = (outputHeight * (outputWidth - x) - y) << 2;
            R = rgba[rgbaIndex];
            if (R < 0) {
                R += 256;
            }
            G = rgba[rgbaIndex + 1];
            if (G < 0) {
                G += 256;
            }
            B = rgba[rgbaIndex + 2];
            if (B < 0) {
                B += 256;
            }
            Y = (R >> 2) + (R >> 7) + (G >> 1) + (G >> 8) + (B >> 4) + (B >> 5) + (B >> 8) + 16;
            yuv[y_M_width + x] = (jbyte) Y;
            if ((y & 0x1) == 0 && (x & 0x1) == 0) {
                cOffset = size + y_div2_M_width + x;
                Cr = (R >> 1) - (R >> 4) - (G >> 2) - (G >> 3) + (G >> 7) - (B >> 4) - (B >> 7) +
                     128;
                yuv[cOffset] = (jbyte) Cr;
                Cb = -(R >> 3) - (R >> 6) - (R >> 7) - (G >> 2) - (G >> 5) - (G >> 7) + (B >> 1) -
                     (B >> 4) + 128;
                yuv[cOffset + 1] = (jbyte) Cb;
            }
        }
    }

    (*env)->ReleasePrimitiveArrayCritical(env, rgbaIn, rgba, 0);
    (*env)->ReleasePrimitiveArrayCritical(env, yuvOut, yuv, 0);
    return 0;
}

JNIEXPORT int JNICALL Java_com_github_piasy_cameracompat_processor_RgbYuvConverter_yuvCropRotateC180(
        JNIEnv *env, jobject obj, jint width, jint height, jbyteArray yuvIn_,
        jint outputHeight_, jbyteArray yuvOut_) {
    jbyte *yuvIn = (*env)->GetPrimitiveArrayCritical(env, yuvIn_, 0);
    jbyte *yuvOut = (*env)->GetPrimitiveArrayCritical(env, yuvOut_, 0);

    int delta = (height - outputHeight_) >> 1;
    int org_size = width * height, crop_size_minus_1 = width * outputHeight_ - 1;
    int crop_len_minus_1 = (width * outputHeight_ * 3) >> 1;
    int y_M_width, y_div_2_M_width, y_minus_delta_M_width, y_minus_delta_div_2_M_width;
    int x, y = delta;
    for (; y < height - delta; y++) {
        y_M_width = y * width;
        y_div_2_M_width = y_M_width >> 1;
        y_minus_delta_M_width = (y - delta) * width;
        y_minus_delta_div_2_M_width = y_minus_delta_M_width >> 1;
        x = 0;
        for (; x < width; x++) {
            yuvOut[crop_size_minus_1 - y_minus_delta_M_width - x] = yuvIn[y_M_width + x];
            if ((y & 0x1) == 0 && (x & 0x1) == 0) {
                yuvOut[crop_len_minus_1 - y_minus_delta_div_2_M_width - x] = yuvIn[org_size + y_div_2_M_width + x];
                yuvOut[crop_len_minus_1 - y_minus_delta_div_2_M_width - x - 1] = yuvIn[org_size + y_div_2_M_width + x + 1];
            }
        }
    }
    (*env)->ReleasePrimitiveArrayCritical(env, yuvIn_, yuvIn, 0);
    (*env)->ReleasePrimitiveArrayCritical(env, yuvOut_, yuvOut, 0);

    return 0;
}

JNIEXPORT int JNICALL Java_com_github_piasy_cameracompat_processor_RgbYuvConverter_yuvCrop(
        JNIEnv *env, jobject obj, jint width, jint height, jbyteArray yuvIn_,
        jint outputHeight_, jbyteArray yuvOut_) {
    jbyte *yuvIn = (*env)->GetPrimitiveArrayCritical(env, yuvIn_, 0);
    jbyte *yuvOut = (*env)->GetPrimitiveArrayCritical(env, yuvOut_, 0);

    int delta = (height - outputHeight_) / 2;
    int org_size = width * height, crop_size = width * outputHeight_;
    int y_M_width, y_div_2_M_width, y_minus_delta_M_width, y_minus_delta_div_2_M_width;
    int x, y = delta;
    for (; y < height - delta; y++) {
        y_M_width = y * width;
        y_div_2_M_width = y_M_width >> 1;
        y_minus_delta_M_width = (y - delta) * width;
        y_minus_delta_div_2_M_width = y_minus_delta_M_width >> 1;
        x = 0;
        for (; x < width; x++) {
            yuvOut[y_minus_delta_M_width + x] = yuvIn[y_M_width + x];
            if ((y & 0x1) == 0 && (x & 0x1) == 0) {
                yuvOut[crop_size + y_minus_delta_div_2_M_width + x] = yuvIn[org_size + y_div_2_M_width + x];
                yuvOut[crop_size + y_minus_delta_div_2_M_width + x + 1] = yuvIn[org_size + y_div_2_M_width + x + 1];
            }
        }
    }
    (*env)->ReleasePrimitiveArrayCritical(env, yuvIn_, yuvIn, 0);
    (*env)->ReleasePrimitiveArrayCritical(env, yuvOut_, yuvOut, 0);

    return 0;
}

JNIEXPORT int JNICALL Java_com_github_piasy_cameracompat_processor_RgbYuvConverter_yuvCropRotateC180Flip(
        JNIEnv *env, jobject obj, jint width, jint height, jbyteArray yuvIn_,
        jint outputHeight_, jbyteArray yuvOut_) {
    jbyte *yuvIn = (*env)->GetPrimitiveArrayCritical(env, yuvIn_, 0);
    jbyte *yuvOut = (*env)->GetPrimitiveArrayCritical(env, yuvOut_, 0);

    int delta = (height - outputHeight_) >> 1;
    int org_size = width * height, crop_size_minus = width * outputHeight_;
    int y_M_width, y_div_2_M_width, h_minus_1_M_w, h_minus_1_div2_M_w;
    int x, y = delta, y_end = height - delta;
    for (; y < y_end; y++) {
        y_M_width = y * width;
        y_div_2_M_width = y_M_width >> 1;
        h_minus_1_M_w = (height - 1 - y - delta) * width;
        h_minus_1_div2_M_w = ((height - 1 - y - delta) >> 1) * width;
        x = 0;
        for (; x < width; x++) {
            yuvOut[h_minus_1_M_w + x] = yuvIn[y_M_width + x];
            if ((y & 0x1) == 0 && (x & 0x1) == 0) {
                yuvOut[crop_size_minus + h_minus_1_div2_M_w + x] = yuvIn[org_size + y_div_2_M_width + x];
                yuvOut[crop_size_minus + h_minus_1_div2_M_w + x + 1] = yuvIn[org_size + y_div_2_M_width + x + 1];
            }
        }
    }
    (*env)->ReleasePrimitiveArrayCritical(env, yuvIn_, yuvIn, 0);
    (*env)->ReleasePrimitiveArrayCritical(env, yuvOut_, yuvOut, 0);

    return 0;
}

JNIEXPORT int JNICALL Java_com_github_piasy_cameracompat_processor_RgbYuvConverter_yuvCropFlip(
        JNIEnv *env, jobject obj, jint width, jint height, jbyteArray yuvIn_,
        jint outputHeight_, jbyteArray yuvOut_) {
    jbyte *yuvIn = (*env)->GetPrimitiveArrayCritical(env, yuvIn_, 0);
    jbyte *yuvOut = (*env)->GetPrimitiveArrayCritical(env, yuvOut_, 0);

    int delta = (height - outputHeight_) >> 1;
    int org_size = width * height, crop_size = width * outputHeight_;
    int y_M_width, y_div_2_M_width, y_minus_delta_M_width, y_minus_delta_div_2_M_width;
    int x, y = delta;
    for (; y < height - delta; y++) {
        y_M_width = y * width;
        y_div_2_M_width = y_M_width >> 1;
        y_minus_delta_M_width = (y - delta) * width;
        y_minus_delta_div_2_M_width = y_minus_delta_M_width >> 1;
        x = 0;
        for (; x < width; x++) {
            yuvOut[y_minus_delta_M_width + width - 1 - x] = yuvIn[y_M_width + x];
            if ((y & 0x1) == 0 && (x & 0x1) == 0) {
                yuvOut[crop_size + y_minus_delta_div_2_M_width + width - 1 - x] = yuvIn[org_size + y_div_2_M_width + x];
                yuvOut[crop_size + y_minus_delta_div_2_M_width + width - 1 - x + 1] = yuvIn[org_size + y_div_2_M_width + x + 1];
            }
        }
    }
    (*env)->ReleasePrimitiveArrayCritical(env, yuvIn_, yuvIn, 0);
    (*env)->ReleasePrimitiveArrayCritical(env, yuvOut_, yuvOut, 0);

    return 0;
}

JNIEXPORT int JNICALL Java_com_github_piasy_cameracompat_processor_RgbYuvConverter_image2yuvCropRotateC180(
        JNIEnv *env, jobject obj, jint width, jint height, jobject YIn, jobject CrIn, jobject CbIn,
        jint CrPixelStride, jint CbPixelStride, jint outputHeight_, jbyteArray yuvOut_) {
    jbyte *Y = (jbyte *) (*env)->GetDirectBufferAddress(env, YIn);
    jbyte *Cr = (jbyte *) (*env)->GetDirectBufferAddress(env, CrIn);
    jbyte *Cb = (jbyte *) (*env)->GetDirectBufferAddress(env, CbIn);
    if (Y == 0 || Cr == 0 || Cb == 0) {
        return -1;
    }
    jbyte *yuvOut = (jbyte *) ((*env)->GetPrimitiveArrayCritical(env, yuvOut_, 0));

    int delta = (height - outputHeight_) >> 1;
    int crop_size_minus_1 = outputHeight_ * width - 1;
    int i_index_div_2, i_div_4;
    int crop_len_minus_1 = ((width * outputHeight_ * 3) >> 1) - 1;
    int i, i_start = delta * width, i_end = (height - delta) * width;
    for (i = i_start; i < i_end; i++) {
        yuvOut[crop_size_minus_1 - i + i_start] = Y[i];
        if ((i & 0x3) == 0) {
            i_index_div_2 = (i - i_start) >> 1;
            i_div_4 = i >> 2;
            yuvOut[crop_len_minus_1 - i_index_div_2 - 1] = Cr[i_div_4 * CrPixelStride];
            yuvOut[crop_len_minus_1 - i_index_div_2] = Cb[i_div_4 * CbPixelStride];
        }
    }

    (*env)->ReleasePrimitiveArrayCritical(env, yuvOut_, yuvOut, 0);

    return 0;
}

JNIEXPORT jint JNICALL Java_com_github_piasy_cameracompat_processor_RgbYuvConverter_image2yuvCrop(
        JNIEnv *env, jobject obj, jint width, jint height, jobject YIn, jobject CrIn, jobject CbIn,
        jint CrPixelStride, jint CbPixelStride, jint outputHeight_, jbyteArray yuvOut_) {
    jbyte *Y = (jbyte *) (*env)->GetDirectBufferAddress(env, YIn);
    jbyte *Cr = (jbyte *) (*env)->GetDirectBufferAddress(env, CrIn);
    jbyte *Cb = (jbyte *) (*env)->GetDirectBufferAddress(env, CbIn);
    if (Y == 0 || Cr == 0 || Cb == 0) {
        return -1;
    }
    jbyte *yuvOut = (jbyte *) ((*env)->GetPrimitiveArrayCritical(env, yuvOut_, 0));

    int delta = (height - outputHeight_) >> 1;
    int crop_size = outputHeight_ * width;
    int Cr_position, i_div_4;
    int i, i_start = delta * width, i_end = (height - delta) * width;
    for (i = i_start; i < i_end; i++) {
        yuvOut[i - i_start] = Y[i];
        if ((i & 0x3) == 0) {
            Cr_position = (i - i_start) >> 1;
            i_div_4 = i >> 2;
            yuvOut[crop_size + Cr_position] = Cr[i_div_4 * CrPixelStride];
            yuvOut[crop_size + Cr_position + 1] = Cb[i_div_4 * CbPixelStride];
        }
    }

    (*env)->ReleasePrimitiveArrayCritical(env, yuvOut_, yuvOut, 0);

    return 0;
}

JNIEXPORT int JNICALL Java_com_github_piasy_cameracompat_processor_RgbYuvConverter_image2yuvCropRotateC180Flip(
        JNIEnv *env, jobject obj, jint width, jint height, jobject YIn, jobject CrIn, jobject CbIn,
        jint CrPixelStride, jint CbPixelStride, jint outputHeight_, jbyteArray yuvOut_) {
    jbyte *Y = (jbyte *) (*env)->GetDirectBufferAddress(env, YIn);
    jbyte *Cr = (jbyte *) (*env)->GetDirectBufferAddress(env, CrIn);
    jbyte *Cb = (jbyte *) (*env)->GetDirectBufferAddress(env, CbIn);
    if (Y == 0 || Cr == 0 || Cb == 0) {
        return -1;
    }
    jbyte *yuvOut = (jbyte *) ((*env)->GetPrimitiveArrayCritical(env, yuvOut_, 0));

    int delta = (height - outputHeight_) >> 1;
    int crop_size = outputHeight_ * width;
    int Cr_out, C_in;
    int x, y = delta, y_end = height - delta;
    int index_in, index_out, y_M_w, y_div_2_M_w, h_minus_1_M_w, h_minus_1_div2_M_w;
    for (; y < y_end; y++) {
        y_M_w = y * width;
        y_div_2_M_w = (y >> 1) * width;
        h_minus_1_M_w = (height - 1 - y - delta) * width;
        h_minus_1_div2_M_w = ((height - 1 - y - delta) >> 1) * width;
        for (x = 0; x < width; x++) {
            index_in = y_M_w + x;
            index_out = h_minus_1_M_w + x;
            yuvOut[index_out] = Y[index_in];
            if ((y & 0x1) == 0 && (x & 0x1) == 0) {
                Cr_out = h_minus_1_div2_M_w + x;
                C_in = (y_div_2_M_w + x) >> 1;
                yuvOut[crop_size + Cr_out] = Cr[C_in * CrPixelStride];
                yuvOut[crop_size + Cr_out + 1] = Cb[C_in * CbPixelStride];
            }
        }
    }

    (*env)->ReleasePrimitiveArrayCritical(env, yuvOut_, yuvOut, 0);

    return 0;
}

JNIEXPORT jint JNICALL Java_com_github_piasy_cameracompat_processor_RgbYuvConverter_image2yuvCropFlip(
        JNIEnv *env, jobject obj, jint width, jint height, jobject YIn, jobject CrIn, jobject CbIn,
        jint CrPixelStride, jint CbPixelStride, jint outputHeight_, jbyteArray yuvOut_) {
    jbyte *Y = (jbyte *) (*env)->GetDirectBufferAddress(env, YIn);
    jbyte *Cr = (jbyte *) (*env)->GetDirectBufferAddress(env, CrIn);
    jbyte *Cb = (jbyte *) (*env)->GetDirectBufferAddress(env, CbIn);
    if (Y == 0 || Cr == 0 || Cb == 0) {
        return -1;
    }
    jbyte *yuvOut = (jbyte *) ((*env)->GetPrimitiveArrayCritical(env, yuvOut_, 0));

    int delta = (height - outputHeight_) >> 1;
    int crop_size = outputHeight_ * width;
    int Cr_out, C_in;
    int x, y = delta, y_end = height - delta;
    int index_in, index_out, y_M_w, y_div_2_M_w, y_minus_delta_M_w, y_minus_delta_div_2_M_w;
    for (; y < y_end; y++) {
        y_M_w = y * width;
        y_div_2_M_w = (y >> 1) * width;
        y_minus_delta_M_w = (y - delta) * width;
        y_minus_delta_div_2_M_w = ((y - delta) >> 1) * width;
        for (x = 0; x < width; x++) {
            index_in = y_M_w + x;
            index_out = y_minus_delta_M_w + width - 1 - x;
            yuvOut[index_out] = Y[index_in];
            if ((y & 0x1) == 0 && (x & 0x1) == 0) {
                Cr_out = y_minus_delta_div_2_M_w + width - 1 - x;
                C_in = (y_div_2_M_w + x) >> 1;
                yuvOut[crop_size + Cr_out + 1] = Cr[C_in * CrPixelStride];
                yuvOut[crop_size + Cr_out] = Cb[C_in * CbPixelStride];
            }
        }
    }

    (*env)->ReleasePrimitiveArrayCritical(env, yuvOut_, yuvOut, 0);

    return 0;
}
