
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
- [ ] use libyuv rather than self writen converter
