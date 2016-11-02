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

import android.media.AudioFormat;
import android.os.Bundle;
import android.support.annotation.WorkerThread;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Toast;
import com.github.piasy.cameracompat.CameraCompat;
import com.github.piasy.rxandroidaudio.StreamAudioRecorder;
import com.yolo.livesdk.YoloLiveNative;
import com.yolo.livesdk.YoloLiveObs;
import com.yolo.livesdk.audio.YoloLiveAudioRecorder;
import com.yolo.livesdk.rx.YoloLivePublishParam;

public class PublishActivity extends AppCompatActivity implements CameraCompat.VideoCaptureCallback,
        CameraCompat.ErrorHandler {

    private static final int DEFAULT_SAMPLE_RATE = 16000;
    private static final int DEFAULT_CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO;
    private static final int DEFAULT_BIT_DEPTH_CONFIG = AudioFormat.ENCODING_PCM_16BIT;

    private CameraCompat mCameraCompat;
    private YoloLiveAudioRecorder mYLLiveAudioRecorder;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_publish);

        findViewById(R.id.mBtnSwitchBeautify).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mCameraCompat.switchBeautify();
            }
        });

        findViewById(R.id.mBtnSwitchCamera).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mCameraCompat.switchCamera();
            }
        });

        findViewById(R.id.mBtnSwitchFlash).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mCameraCompat.switchFlash();
            }
        });

        findViewById(R.id.mBtnSwitchMirror).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mCameraCompat.switchMirror();
            }
        });

        start();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        mYLLiveAudioRecorder.stop();
        YoloLiveNative.closeSender();
    }

    private void start() {
        mYLLiveAudioRecorder = YoloLiveAudioRecorder.getInstance();
        mCameraCompat = new CameraCompat.Builder(this, this)
                .beautifyOn(false)
                .frontCamera(false)
                .build();
        mCameraCompat.startPreview(null, getSupportFragmentManager(), R.id.mPreviewContainer);
    }

    @WorkerThread
    @Override
    public void onVideoSizeChanged(int width, int height) {
        startPublish(width, height);
    }

    @WorkerThread
    @Override
    public void onFrameData(final byte[] data, final int width, final int height) {
        YoloLiveNative.pushVideoData(data, System.currentTimeMillis());
    }

    @WorkerThread
    @Override
    public void onError(@CameraCompat.ErrorCode int code) {
        Toast.makeText(this, "@CameraCompat.ErrorCode " + code, Toast.LENGTH_SHORT).show();
    }

    private void startPublish(int videoWidth, int videoHeight) {
        String url = "rtmp://video-center.alivecdn.com"
                     + "/yolo/4acdd42854a63366fac1339b10784d1e89418ed6"
                     + "?vhost=alive.yoloyolo.tv"
                     + "&auth_key=2524579200-0-0-b334ee106592e2c719e2d801291b2c7d";
        YoloLivePublishParam.Builder builder = new YoloLivePublishParam.Builder(url, videoWidth,
                videoHeight, YoloLivePublishParam.DEFAULT_SAMPLERATE,
                YoloLivePublishParam.DEFAULT_CHANNEL_NUM);
        builder.autoCrf(this);
        YoloLiveNative.initSender(builder.build());

        YoloLiveObs.setCallback(new YoloLiveObs.LiveCallback() {
            @Override
            public void notifyEvent(int resultCode, String content) {
                if (resultCode == YoloLiveObs.OK_PublishConnect) {
                    mYLLiveAudioRecorder.start(DEFAULT_SAMPLE_RATE, DEFAULT_CHANNEL_CONFIG,
                            DEFAULT_BIT_DEPTH_CONFIG,
                            new StreamAudioRecorder.AudioDataCallback() {
                                @Override
                                public void onAudioData(byte[] data, int size) {
                                    YoloLiveNative.pushAudioData(data, size,
                                            System.currentTimeMillis());
                                }

                                @Override
                                public void onError() {
                                    Log.e("PublishActivity",
                                            "StreamAudioRecorder.AudioDataCallback::onError");
                                }
                            });
                }
            }

            @Override
            public void notifyVideoData(int receiverId, byte[] data) {

            }

            @Override
            public int notifyAudioData(int receiverId, byte[] data, int size) {
                return 0;
            }

            @Override
            public void onH264Video(byte[] data, int size, int type) {

            }

            @Override
            public int setWidthHeight(int receiverId, int width, int height) {
                return 0;
            }

            @Override
            public int setAudioSpec(int receiverId, int bitDepth, int nChannels, int sampleRate) {
                return 0;
            }
        });
    }
}
