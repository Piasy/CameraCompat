

## Camera1

1. `PreviewFragment#onViewCreated`：

~~~ java
@Override
public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);

    mPreviewContainer.addView(mProcessorChain.createSurface(getContext()));
    mCameraHelper = createCameraHelper();
    mProcessorChain.initSurface(getContext());
}
~~~

2. `PreviewFragment#onResume` -> 
3. `mCameraHelper.startPreview()` -> 
4. `Camera1PreviewFragment#onOpened()` ->
5. `Camera1PreviewFragment#startPreviewDirectly()` ->
6. `camera.startPreview()`，`mRenderer.resumeDrawing()`

数据流：Camera -> Camera1PreviewCallback -> ProcessorChain

`ProcessorChain.resume()` 之后，`GLRender` 才会对相机数据进行处理。

如果开启美颜，则走：

1. `ProcessorChain#onFrameData` -> 
2. filter draw -> 
3. `GLFilterGroup#dumpImage` ->
4. `ProcessorChain#imageDumped` ->
5. `CameraCompat.VideoCaptureCallback#onFrameData` ->
6. 外部使用方实现的回调

否则直接走（当然也会渲染到 GLSurfaceView 上）：

1. `ProcessorChain#onFrameData` -> 
2. `CameraCompat.VideoCaptureCallback#onFrameData` ->
3. 外部使用方实现的回调

`PreviewFragment#onPause` -> `mCameraHelper.stopPreview()`

## Camera2

和 Camera1 基本一致。
