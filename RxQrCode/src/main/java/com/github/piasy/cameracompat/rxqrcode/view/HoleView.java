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

package com.github.piasy.cameracompat.rxqrcode.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.View;

/**
 * Created by Piasy{github.com/Piasy} on 19/10/2016.
 */

public class HoleView extends View {

    private final Paint mPaint = new Paint();
    private final Path mPath = new Path();
    private boolean mInitialized = false;

    private float mHoleX;
    private float mHoleY;
    private int mHoleWidth;
    private int mHoleHeight;

    public HoleView(Context context) {
        this(context, null);
    }

    public HoleView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public HoleView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        mPaint.setStyle(Paint.Style.FILL);
        mPaint.setAntiAlias(true);
        setLayerType(LAYER_TYPE_HARDWARE, mPaint);
    }

    public void setHole(int color, float holeX, float holeY, int holeWidth, int holeHeight) {
        mInitialized = true;

        mPaint.setColor(color);
        mHoleX = holeX;
        mHoleY = holeY;
        mHoleWidth = holeWidth;
        mHoleHeight = holeHeight;
        invalidate();
    }

    public void setColor(int color) {
        mPaint.setColor(color);
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (!mInitialized) {
            return;
        }

        int width = getWidth();
        int height = getHeight();

        mPath.reset();
        mPath.moveTo(0, 0);
        mPath.lineTo(width, 0);
        mPath.lineTo(width, mHoleY);
        mPath.lineTo(0, mHoleY);
        mPath.lineTo(0, 0);
        canvas.drawPath(mPath, mPaint);

        mPath.reset();
        mPath.moveTo(0, mHoleY);
        mPath.lineTo(mHoleX, mHoleY);
        mPath.lineTo(mHoleX, mHoleY + mHoleHeight);
        mPath.lineTo(0, mHoleY + mHoleHeight);
        mPath.lineTo(0, mHoleY);
        canvas.drawPath(mPath, mPaint);

        mPath.reset();
        mPath.moveTo(mHoleX + mHoleWidth, mHoleY);
        mPath.lineTo(width, mHoleY);
        mPath.lineTo(width, mHoleY + mHoleHeight);
        mPath.lineTo(mHoleX + mHoleWidth, mHoleY + mHoleHeight);
        mPath.lineTo(mHoleX + mHoleWidth, mHoleY);
        canvas.drawPath(mPath, mPaint);

        mPath.reset();
        mPath.moveTo(0, mHoleY + mHoleHeight);
        mPath.lineTo(width, mHoleY + mHoleHeight);
        mPath.lineTo(width, height);
        mPath.lineTo(0, height);
        mPath.lineTo(0, mHoleY + mHoleHeight);
        canvas.drawPath(mPath, mPaint);
    }
}
