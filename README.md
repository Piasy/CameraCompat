
## RxQrCode

Call `RxQrCode.scanFromCamera` from your `onCreate` of Activity/Fragment.

**Note that you must add** `getWindow().addFlags(WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED)` in your Activity's `onCreate`, otherwise the camera preview may not show.

~~~ java
@Override
public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    RxQrCode.scanFromCamera(savedInstanceState, getActivity().getSupportFragmentManager(),
            R.id.mScannerPreview, this)
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(result -> {
                // you got the scan result
            }, e -> {
                // other error happened, **code not found won't get there**
            });
}
~~~

## Todo

- [ ] focus
- [ ] 华为P8存在色调偏蓝
- [ ] N6 不美颜时锐化非常严重
- [ ] 滤镜开关切换时仍稍有瑕疵，疑为手动裁剪和GPU裁剪方式不同导致
- [ ] 开启滤镜时，图像边缘异常
