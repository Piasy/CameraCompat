package com.github.piasy.cameracompat.v16;

import android.annotation.TargetApi;
import android.graphics.PixelFormat;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.opengl.GLSurfaceView;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import com.github.piasy.cameracompat.R;
import com.github.piasy.cameracompat.gpuimage.GLRender;
import com.github.piasy.cameracompat.utils.Utils;
import java.io.IOException;
import jp.co.cyberagent.android.gpuimage.GPUImageColorInvertFilter;
import jp.co.cyberagent.android.gpuimage.Rotation;

/**
 * A simple {@link Fragment} subclass.
 */
@TargetApi(Build.VERSION_CODES.JELLY_BEAN)
public class PreviewFragmentV16 extends Fragment {

    private CameraHelper mCameraHelper;
    private CameraLoader mCameraLoader;
    private GLRender mRenderer;
    private Camera1PreviewCallback mCameraFrameCallback;

    private GLSurfaceView mGLSurfaceView;

    public PreviewFragmentV16() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        return inflater.inflate(R.layout.preview_fragment, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        bindView(view);

        initFields();

        initSurface();
    }

    @Override
    public void onResume() {
        super.onResume();

        mCameraLoader.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();

        mCameraLoader.onPause();
    }

    private void bindView(View rootView) {
        mGLSurfaceView = (GLSurfaceView) rootView.findViewById(R.id.mSurface);
    }

    private void initFields() {
        mCameraHelper = new CameraHelper();
        mCameraLoader = new CameraLoader();
        mRenderer = new GLRender(new GPUImageColorInvertFilter());
        mCameraFrameCallback = new Camera1PreviewCallback(mRenderer);
    }

    private void initSurface() {
        if (Utils.isSupportOpenGLES2(getContext())) {
            mGLSurfaceView.setEGLContextClientVersion(2);
        }
        mGLSurfaceView.setEGLConfigChooser(8, 8, 8, 8, 16, 0);
        mGLSurfaceView.getHolder().setFormat(PixelFormat.RGBA_8888);
        mGLSurfaceView.setRenderer(mRenderer);
        mGLSurfaceView.setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
        mGLSurfaceView.requestRender();
    }

    /**
     * Sets the up camera to be connected to GPUImage to get a filtered preview.
     *
     * @param camera the camera
     * @param degrees by how many degrees the image should be rotated
     * @param flipHorizontal if the image should be flipped horizontally
     * @param flipVertical if the image should be flipped vertically
     */
    void setUpCamera(final Camera camera, final int degrees, final boolean flipHorizontal,
            final boolean flipVertical) {
        mGLSurfaceView.setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);
        mRenderer.setUpSurfaceTexture(new SurfaceTextureInitCallback() {
            @Override
            public void onSurfaceTextureInitiated(SurfaceTexture surfaceTexture) {
                try {
                    camera.setPreviewCallback(mCameraFrameCallback);
                    camera.setPreviewTexture(surfaceTexture);
                    camera.startPreview();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
        Rotation rotation = Rotation.NORMAL;
        switch (degrees) {
            case 90:
                rotation = Rotation.ROTATION_90;
                break;
            case 180:
                rotation = Rotation.ROTATION_180;
                break;
            case 270:
                rotation = Rotation.ROTATION_270;
                break;
        }
        mRenderer.setRotationCamera(rotation, flipHorizontal, flipVertical);
    }

    private class CameraLoader {

        private boolean mIsFrontCamera;
        private Camera mCameraInstance;

        public void onResume() {
            openCamera();
        }

        public void onPause() {
            releaseCamera();
        }

        public void switchCamera() {
            releaseCamera();
            mIsFrontCamera = !mIsFrontCamera;
            openCamera();
        }

        private void openCamera() {
            mCameraInstance = getCameraInstance();
            if (mCameraInstance == null) {
                return;
            }
            Camera.Parameters parameters = mCameraInstance.getParameters();
            // TODO adjust by getting supportedPreviewSizes and then choosing
            // the best one for screen size (best fill screen)
            if (parameters.getSupportedFocusModes()
                    .contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE)) {
                parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
            }
            mCameraInstance.setParameters(parameters);

            int orientation =
                    mCameraHelper.getCameraDisplayOrientation(getActivity(), getCurrentCameraId());
            Camera.CameraInfo cameraInfo = mCameraHelper.getCameraInfo(getCurrentCameraId());
            boolean flipHorizontal = cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_FRONT;
            setUpCamera(mCameraInstance, orientation, flipHorizontal, false);
        }

        private int getCurrentCameraId() {
            return mIsFrontCamera ? Camera.CameraInfo.CAMERA_FACING_FRONT
                    : Camera.CameraInfo.CAMERA_FACING_BACK;
        }

        /** A safe way to get an instance of the Camera object. */
        private Camera getCameraInstance() {
            Camera c = null;
            try {
                c = mIsFrontCamera ? mCameraHelper.openFrontCamera()
                        : mCameraHelper.openBackCamera();
            } catch (Exception e) {
                e.printStackTrace();
            }
            return c;
        }

        private void releaseCamera() {
            mCameraInstance.setPreviewCallback(null);
            mCameraInstance.release();
            mCameraInstance = null;
        }
    }
}
