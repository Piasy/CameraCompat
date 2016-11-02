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

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import com.afollestad.materialdialogs.MaterialDialog;
import com.github.piasy.cameracompat.CameraCompat;
import com.github.piasy.cameracompat.rxqrcode.RxQrCode;
import com.google.zxing.Result;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;
import rx.subjects.PublishSubject;
import rx.subjects.Subject;

/**
 * A simple {@link Fragment} subclass.
 */
public class QrScanFragment extends Fragment implements CameraCompat.ErrorHandler {
    private static final int PICK_IMAGE_RESULT = 100;

    @BindView(R2.id.mScrollView)
    ScrollView mScrollView;
    @BindView(R2.id.mTvResult)
    TextView mTvResult;

    private Subject<Result, Result> mScanResult = PublishSubject.<Result>create().toSerialized();
    private Subscription mScanResultSubscription;
    private Subscription mPreviewSubscription;

    public QrScanFragment() {
        // Required empty public constructor
    }

    @OnClick(R2.id.mScanFromPicture)
    public void scanFromPicture() {
        Intent galleryIntent = new Intent(Intent.ACTION_PICK,
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(galleryIntent, PICK_IMAGE_RESULT);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == Activity.RESULT_OK) {
            switch (requestCode) {
                case PICK_IMAGE_RESULT:
                    if (data != null && data.getData() != null) {
                        Uri imageFileUri = data.getData();
                        String realPath = UriUtil.getPath(getContext(), imageFileUri);
                        if (!TextUtils.isEmpty(realPath)) {
                            RxQrCode.scanFromPicture(realPath)
                                    .subscribeOn(Schedulers.io())
                                    .observeOn(AndroidSchedulers.mainThread())
                                    .subscribe(result -> {
                                        if (mTvResult != null) {
                                            mTvResult.setText(mTvResult.getText()
                                                              + "\nresult "
                                                              + result.getText());
                                            mScrollView.fullScroll(View.FOCUS_DOWN);
                                        }
                                    }, e -> {
                                        Toast.makeText(getContext(), "code not found",
                                                Toast.LENGTH_SHORT).show();
                                    });
                        } else {
                            Toast.makeText(getContext(), "file not found", Toast.LENGTH_SHORT)
                                    .show();
                        }
                    } else {
                        Toast.makeText(getContext(), "no data", Toast.LENGTH_SHORT).show();
                    }
                    break;
                default:
                    break;
            }
        }

        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mPreviewSubscription = RxQrCode.scanFromCamera(savedInstanceState,
                getActivity().getSupportFragmentManager(), R.id.mPreviewContainer, this)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(mScanResult);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_qr_scan, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        ButterKnife.bind(this, view);

        observeScanResult();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mPreviewSubscription != null && !mPreviewSubscription.isUnsubscribed()) {
            mPreviewSubscription.unsubscribe();
            mPreviewSubscription = null;
        }
    }

    private void observeScanResult() {
        mScanResultSubscription = mScanResult.subscribe(result -> {
            stopObserveScanResult();
            new MaterialDialog.Builder(getContext())
                    .title("found qr code")
                    .content(result.getText())
                    .positiveText("ok")
                    .dismissListener(dialog -> {
                        Log.d("QrScan", "request 1");
                        observeScanResult();
                    })
                    .show();
        });
    }

    private void stopObserveScanResult() {
        if (mScanResultSubscription != null && !mScanResultSubscription.isUnsubscribed()) {
            mScanResultSubscription.unsubscribe();
            mScanResultSubscription = null;
        }
    }

    @Override
    public void onError(@CameraCompat.ErrorCode int code) {

    }
}
