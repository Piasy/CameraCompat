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
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import com.github.piasy.cameracompat.rxqrcode.R;

/**
 * Created by Piasy{github.com/Piasy} on 19/10/2016.
 */

public class HoleContainer extends FrameLayout {
    private HoleView mHoleView;

    private boolean mInitialized = false;
    private int mOutsideColor;

    public HoleContainer(Context context) {
        this(context, null);
    }

    public HoleContainer(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public HoleContainer(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        TypedArray a = context.getTheme()
                .obtainStyledAttributes(attrs, R.styleable.HoleContainer, 0, 0);
        mOutsideColor = a.getColor(R.styleable.HoleContainer_outside_color, 0x80000000);
        a.recycle();
    }

    public void setOutsideColor(int outsideColor) {
        mOutsideColor = outsideColor;
        mHoleView.setColor(mOutsideColor);
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);

        if (!mInitialized) {
            if (getChildCount() != 1) {
                throw new IllegalStateException("HoleContainer must have exactly one child!");
            }
            mHoleView = new HoleView(getContext());
            LayoutParams params = new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT);
            mHoleView.setLayoutParams(params);
            addView(mHoleView);
            View child = getChildAt(0);
            mHoleView.setHole(mOutsideColor, child.getX(), child.getY(), child.getMeasuredWidth(),
                    child.getMeasuredHeight());

            mInitialized = true;
        }
    }
}
