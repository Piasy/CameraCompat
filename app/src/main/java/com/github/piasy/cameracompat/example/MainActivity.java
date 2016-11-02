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

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Toast;
import com.github.piasy.cameracompat.CameraCompat;
import com.vinny.vinnylive.ConnectionChangeReceiver;
import com.vinny.vinnylive.LiveObs;
import com.vinny.vinnylive.LiveParam;
import com.vinny.vinnylive.NativeLive;
import com.vinny.vinnylive.audio.YLLiveAudioRecorder;

public class MainActivity extends AppCompatActivity implements CameraCompat.VideoCaptureCallback {

    private volatile boolean mStarted;
    private YLLiveAudioRecorder mYLLiveAudioRecorder;
    private CameraCompat mCameraCompat;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        findViewById(R.id.mBtnStart).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                start();
            }
        });

        findViewById(R.id.mBtnProfiling).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, ProfilingActivity.class));
            }
        });

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
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mStarted) {
            mYLLiveAudioRecorder.stop();
            NativeLive.StopPublish();
        }
    }

    private void start() {
        mYLLiveAudioRecorder = YLLiveAudioRecorder.getInstance();
        mCameraCompat = new CameraCompat.Builder().beautifyOn(false).frontCamera(false).build();
        mCameraCompat.startPreview(null, getSupportFragmentManager(), R.id.mPreviewContainer);
        NativeLive.init(this);
        NativeLive.CreateVinnyLive();
        NativeLive.EnableDebug(true);
        NativeLive.AddObs();
        findViewById(R.id.mOpContainer).setVisibility(View.VISIBLE);
    }

    private void startPublish(int videoWidth, int videoHeight) {
        final LiveParam param = getParam(videoWidth, videoHeight);
        param.setStream_name("55615151206af318d57996e8178cf05a");
        String paramStr = param.getParamStr();
        if (NativeLive.SetParam(paramStr) == 0) {
            String url = getLivePublishUrl("55615151206af318d57996e8178cf05a",
                    "958338d836dcc0786284089b5775e1888a020894");
            NativeLive.StartPublish(url);
        } else {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(MainActivity.this, "Fail", Toast.LENGTH_SHORT).show();
                }
            });
        }
        LiveObs.setCallback(new LiveObs.LiveCallback() {
            @Override
            public void notifyEvent(int resultCode, String content) {
                if (resultCode == LiveObs.OK_PublishConnect) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(MainActivity.this, "Start", Toast.LENGTH_SHORT).show();
                        }
                    });
                    mStarted = true;
                    mYLLiveAudioRecorder.start();
                }
            }

            @Override
            public void notifyVideoData(byte[] data) {

            }

            @Override
            public int notifyAudioData(byte[] data, int size) {
                return 0;
            }

            @Override
            public void onH264Video(byte[] data, int size, int type) {

            }
        });
    }

    private static String getLivePublishUrl(String vhallStreamName, String token) {
        return LiveParam.rtmpPublishBaseUrl + "?token=" + token + "/" + vhallStreamName;
    }

    private LiveParam getParam(int videoWidth, int videoHeight) {
        LiveParam param = LiveParam.getParam(LiveParam.TYPE_XHDPI);
        param.buffer_time = 2;
        param.publish_reconnect_times = 50;
        param.publish_timeout = 5000;
        param.video_width = videoWidth;
        param.video_height = videoHeight;
        param.setDevice_type(android.os.Build.MODEL);

        int netState = ConnectionChangeReceiver.ConnectionDetect(this);
        switch (netState) {
            case ConnectionChangeReceiver.NET_UNKNOWN:
                param.crf = LiveParam.CRF_WIFI;
                return param;
            case ConnectionChangeReceiver.NET_2G3G:
                param.crf = LiveParam.CRF_2G3G;
                return param;
            case ConnectionChangeReceiver.NET_WIFI:
                param.crf = LiveParam.CRF_WIFI;
                return param;
            case ConnectionChangeReceiver.NET_ERROR:
            default:
                return param;
        }
    }

    @Override
    public void onVideoSizeChanged(int width, int height) {
        startPublish(width, height);
    }

    @Override
    public void onFrameData(final byte[] data, final int width, final int height) {
        if (mStarted) {
            NativeLive.PushVideoData(data, 0, System.currentTimeMillis());
        }
    }
}
