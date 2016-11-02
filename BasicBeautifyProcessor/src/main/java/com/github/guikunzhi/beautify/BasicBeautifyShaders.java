/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2016 Guikz
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

package com.github.guikunzhi.beautify;

/**
 * Created by Piasy{github.com/Piasy} on 7/6/16.
 */

public final class BasicBeautifyShaders {
    public static final String VERTEX_SHADER =
              "attribute vec4 position;\n"
            + "attribute vec4 inputTextureCoordinate;\n"
            + "const int GAUSSIAN_SAMPLES = 9;\n"
            + "uniform float texelWidthOffset;\n"
            + "uniform float texelHeightOffset;\n"
            + "varying vec2 textureCoordinate;\n"
            + "varying vec2 blurCoordinates[GAUSSIAN_SAMPLES];\n"
            + "\n"
            + "void main() {\n"
            + "  gl_Position = position;\n"
            + "  textureCoordinate = inputTextureCoordinate.xy;\n"
            + "  // Calculate the positions for the blur\n"
            + "  int multiplier = 0;\n"
            + "  vec2 blurStep;\n"
            + "  vec2 singleStepOffset = vec2(texelWidthOffset, texelHeightOffset);\n"
            + "  for (int i = 0; i < GAUSSIAN_SAMPLES; i++) {\n"
            + "    multiplier = (i - ((GAUSSIAN_SAMPLES - 1) / 2));\n"
            + "    // Blur in x (horizontal)\n"
            + "    blurStep = float(multiplier) * singleStepOffset;\n"
            + "    blurCoordinates[i] = inputTextureCoordinate.xy + blurStep;\n"
            + "  }\n"
            + "}";

    public static final String FRAGMENT_SHADER =
              "uniform sampler2D inputImageTexture;\n"
            + "const lowp int GAUSSIAN_SAMPLES = 9;\n"
            + "varying mediump vec2 textureCoordinate;\n"
            + "varying mediump vec2 blurCoordinates[GAUSSIAN_SAMPLES];\n"
            + "\n"
            + "uniform mediump float distanceNormalizationFactor;\n"
            + "const mediump float smoothDegree = 0.6;\n"
            + "\n"
            + "void main() {\n"
            + "  lowp vec4 centralColor;\n"
            + "  lowp float gaussianWeightTotal;\n"
            + "  lowp vec4 sum;\n"
            + "  lowp vec4 sampleColor;\n"
            + "  lowp float distanceFromCentralColor;\n"
            + "  lowp float gaussianWeight;\n"
            + "  mediump vec4 origin = texture2D(inputImageTexture,textureCoordinate);\n"
            + "\n"
            + "  centralColor = texture2D(inputImageTexture, blurCoordinates[4]);\n"
            + "  gaussianWeightTotal = 0.18;\n"
            + "  sum = centralColor * 0.18;\n"
            + "\n"
            + "  sampleColor = texture2D(inputImageTexture, blurCoordinates[0]);\n"
            + "  distanceFromCentralColor = min(distance(centralColor, sampleColor) * "
            + "distanceNormalizationFactor, 1.0);\n"
            + "  gaussianWeight = 0.05 * (1.0 - distanceFromCentralColor);\n"
            + "  gaussianWeightTotal += gaussianWeight;\n"
            + "  sum += sampleColor * gaussianWeight;\n"
            + "\n"
            + "  sampleColor = texture2D(inputImageTexture, blurCoordinates[1]);\n"
            + "  distanceFromCentralColor = min(distance(centralColor, sampleColor) * "
            + "distanceNormalizationFactor, 1.0);\n"
            + "  gaussianWeight = 0.09 * (1.0 - distanceFromCentralColor);\n"
            + "  gaussianWeightTotal += gaussianWeight;\n"
            + "  sum += sampleColor * gaussianWeight;\n"
            + "\n"
            + "  sampleColor = texture2D(inputImageTexture, blurCoordinates[2]);\n"
            + "  distanceFromCentralColor = min(distance(centralColor, sampleColor) * "
            + "distanceNormalizationFactor, 1.0);\n"
            + "  gaussianWeight = 0.12 * (1.0 - distanceFromCentralColor);\n"
            + "  gaussianWeightTotal += gaussianWeight;\n"
            + "  sum += sampleColor * gaussianWeight;\n"
            + "\n"
            + "  sampleColor = texture2D(inputImageTexture, blurCoordinates[3]);\n"
            + "  distanceFromCentralColor = min(distance(centralColor, sampleColor) * "
            + "distanceNormalizationFactor, 1.0);\n"
            + "  gaussianWeight = 0.15 * (1.0 - distanceFromCentralColor);\n"
            + "  gaussianWeightTotal += gaussianWeight;\n"
            + "  sum += sampleColor * gaussianWeight;\n"
            + "\n"
            + "  sampleColor = texture2D(inputImageTexture, blurCoordinates[5]);\n"
            + "  distanceFromCentralColor = min(distance(centralColor, sampleColor) * "
            + "distanceNormalizationFactor, 1.0);\n"
            + "  gaussianWeight = 0.15 * (1.0 - distanceFromCentralColor);\n"
            + "  gaussianWeightTotal += gaussianWeight;\n"
            + "  sum += sampleColor * gaussianWeight;\n"
            + "\n"
            + "  sampleColor = texture2D(inputImageTexture, blurCoordinates[6]);\n"
            + "  distanceFromCentralColor = min(distance(centralColor, sampleColor) * "
            + "distanceNormalizationFactor, 1.0);\n"
            + "  gaussianWeight = 0.12 * (1.0 - distanceFromCentralColor);\n"
            + "  gaussianWeightTotal += gaussianWeight;\n"
            + "  sum += sampleColor * gaussianWeight;\n"
            + "\n"
            + "  sampleColor = texture2D(inputImageTexture, blurCoordinates[7]);\n"
            + "  distanceFromCentralColor = min(distance(centralColor, sampleColor) * "
            + "distanceNormalizationFactor, 1.0);\n"
            + "  gaussianWeight = 0.09 * (1.0 - distanceFromCentralColor);\n"
            + "  gaussianWeightTotal += gaussianWeight;\n"
            + "  sum += sampleColor * gaussianWeight;\n"
            + "\n"
            + "  sampleColor = texture2D(inputImageTexture, blurCoordinates[8]);\n"
            + "  distanceFromCentralColor = min(distance(centralColor, sampleColor) * "
            + "distanceNormalizationFactor, 1.0);\n"
            + "  gaussianWeight = 0.05 * (1.0 - distanceFromCentralColor);\n"
            + "  gaussianWeightTotal += gaussianWeight;\n"
            + "  sum += sampleColor * gaussianWeight;\n"
            + "\n"
            + "  mediump vec4 bilateral = sum / gaussianWeightTotal;\n"
            + "  mediump vec4 smoothOut;\n"
            + "  lowp float r = origin.r;\n"
            + "  lowp float g = origin.g;\n"
            + "  lowp float b = origin.b;\n"
            + "  if (r > 0.3725 && g > 0.1568 && b > 0.0784 && r > b && (max(max(r, g), b) - min"
            + "(min(r, g), b)) > 0.0588 && abs(r-g) > 0.0588) {\n"
            + "    smoothOut = (1.0 - smoothDegree) * (origin - bilateral) + bilateral;\n"
            + "  }\n"
            + "  else {\n"
            + "    smoothOut = origin;\n"
            + "  }\n"
            + "  smoothOut.r = log(1.0 + 0.2 * smoothOut.r)/log(1.2);\n"
            + "  smoothOut.g = log(1.0 + 0.2 * smoothOut.g)/log(1.2);\n"
            + "  smoothOut.b = log(1.0 + 0.2 * smoothOut.b)/log(1.2);\n"
            + "  gl_FragColor = smoothOut;\n"
            + "}\n";
}
