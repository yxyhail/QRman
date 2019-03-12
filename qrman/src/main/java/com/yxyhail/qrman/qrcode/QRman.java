package com.yxyhail.qrman.qrcode;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Point;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.Result;
import com.google.zxing.ResultPoint;
import com.yxyhail.qrman.R;
import com.yxyhail.qrman.camera.CameraManager;
import com.yxyhail.qrman.utils.PermissionUtil;

import java.io.IOException;
import java.util.Collection;

public class QRman implements SurfaceHolder.Callback {
    private static final String TAG = QRman.class.getSimpleName();

    private static final long BULK_MODE_SCAN_DELAY_MS = 800L;

    private CameraManager cameraManager;
    private QRHandler handler;
    private Result savedResultToShow;
    private ViewfinderView viewfinderView;
    private Result lastResult;
    private boolean hasSurface;
    private Collection<BarcodeFormat> decodeFormats;
    private String characterSet;
    private InactivityTimer inactivityTimer;
    private BeepManager beepManager;
    private AmbientLightManager ambientLightManager;
    private Activity activity;
    private QRCallback qrCallback;
    private View captureView;
    private ViewGroup viewGroup;
    private boolean addedView = false;
    private boolean onResumed = false;
    private boolean inited = false;
    private boolean isTorchOn = false;
    private boolean isAutoRestartAfterSuccess = false;
    private int ScanStatus = 0;//0 扫描二维码 1 生成二维码 2 解析Bitmap二维码

    public interface QRCallback {
        void onSuccess(QRResult rawResult, Bitmap barcode);
    }

    ViewfinderView getViewfinderView() {
        return viewfinderView;
    }

    Handler getHandler() {
        return handler;
    }

    CameraManager getCameraManager() {
        return cameraManager;
    }

    Activity getActivity() {
        return activity;
    }

    public QRman(Activity activity) {
        this.activity = activity;
    }

    private void init() {
        PermissionUtil permissionUtil = new PermissionUtil(activity);
        boolean hasPermission = permissionUtil.hasPermission(PermissionUtil.TYPE_CAMERA);
        if (hasPermission) {
            addCaptureView();
        } else {
            permissionUtil
                    .addPermission(PermissionUtil.TYPE_CAMERA)
                    .request(new PermissionUtil.PermissionGranted() {
                        @Override
                        public void onGranted(int requestType) {
                            addCaptureView();
                        }
                    });
        }
    }

    private void addCaptureView() {
        hasSurface = false;
        inited = true;
        activity.setTheme(R.style.QRmanTheme);
        inactivityTimer = new InactivityTimer(activity);
        beepManager = new BeepManager(activity);
        ambientLightManager = new AmbientLightManager(activity);
        captureView = LayoutInflater.from(activity).inflate(R.layout.layout_capture_view, null);
        viewGroup = activity.findViewById(android.R.id.content);
        viewGroup.addView(captureView);
        viewfinderView = captureView.findViewById(R.id.viewfinder_view);
        Window window = activity.getWindow();
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        addedView = true;
        addListener();
        onResume();
    }


    private void addListener() {
        OnTorchClick onTorchClick = new OnTorchClick();
        OnBackClick onBackClick = new OnBackClick();
        captureView.findViewById(R.id.capture_flash).setOnClickListener(onTorchClick);
        captureView.findViewById(R.id.capture_flash_hint).setOnClickListener(onTorchClick);
        captureView.findViewById(R.id.capture_back).setOnClickListener(onBackClick);

        activity.getApplication().registerActivityLifecycleCallbacks(new Application.ActivityLifecycleCallbacks() {
            @Override
            public void onActivityCreated(Activity activity, Bundle savedInstanceState) {
            }

            @Override
            public void onActivityStarted(Activity activity) {
            }

            @Override
            public void onActivityResumed(Activity activity) {
                if (activity == QRman.this.activity) {
                    onResume();
                }
            }

            @Override
            public void onActivityPaused(Activity activity) {
                if (activity == QRman.this.activity) {
                    onPause();
                }
            }

            @Override
            public void onActivityStopped(Activity activity) {
            }

            @Override
            public void onActivitySaveInstanceState(Activity activity, Bundle outState) {
            }

            @Override
            public void onActivityDestroyed(Activity activity) {
                if (activity == QRman.this.activity) {
                    onDestroy();
                }
            }
        });
    }


    private class OnBackClick implements View.OnClickListener{

        @Override
        public void onClick(View v) {
            activity.finish();
        }
    }


    private class OnTorchClick implements View.OnClickListener{

        @Override
        public void onClick(View v) {
            isTorchOn = !isTorchOn;
            cameraManager.setTorch(isTorchOn);
            ImageView captureFlash = captureView.findViewById(R.id.capture_flash);
            captureFlash.setImageDrawable(
                    activity.getResources().getDrawable(
                            isTorchOn ? R.drawable.flash_on : R.drawable.flash_off));
        }
    }


    private void onResume() {
        if (onResumed) return;
        if (cameraManager == null) {
            cameraManager = new CameraManager(activity.getApplication());
        }
        viewfinderView.setCameraManager(cameraManager);
        handler = null;
        lastResult = null;

        beepManager.updatePrefs();
        ambientLightManager.start(cameraManager);

        inactivityTimer.onResume();

        decodeFormats = null;
        characterSet = null;
        SurfaceView surfaceView = captureView.findViewById(R.id.preview_view);
        SurfaceHolder surfaceHolder = surfaceView.getHolder();
        if (hasSurface) {
            // The activity was paused but not stopped, so the surface still exists. Therefore
            // surfaceCreated() won't be called, so init the camera here.
            initCamera(surfaceHolder);
        } else {
            // Install the callback and wait for surfaceCreated() to init the camera.
            surfaceHolder.addCallback(this);
        }
        onResumed = true;
    }

    private void onPause() {
        onResumed = false;
        if (cameraManager == null) return;
        if (handler != null) {
            handler.quitSynchronously();
            handler = null;
        }
        inactivityTimer.onPause();
        ambientLightManager.stop();
        beepManager.close();
        cameraManager.closeDriver();
        //historyManager = null; // Keep for onActivityResult
        if (!hasSurface) {
            SurfaceView surfaceView = captureView.findViewById(R.id.preview_view);
            SurfaceHolder surfaceHolder = surfaceView.getHolder();
            surfaceHolder.removeCallback(this);
        }
    }

    private void onDestroy() {
        inactivityTimer.shutdown();
    }

    private void decodeOrStoreSavedBitmap(Bitmap bitmap, Result result) {
        // Bitmap isn't used yet -- will be used soon
        if (handler == null) {
            savedResultToShow = result;
        } else {
            if (result != null) {
                savedResultToShow = result;
            }
            if (savedResultToShow != null) {
                Message message = Message.obtain(handler, Contents.Status.DECODE_SUCCEEDED, savedResultToShow);
                handler.sendMessage(message);
            }
            savedResultToShow = null;
        }
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        if (!hasSurface) {
            hasSurface = true;
            initCamera(holder);
        }
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        hasSurface = false;
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        // do nothing
    }

    /**
     * A valid barcode has been found, so give an indication of success and show the results.
     *
     * @param rawResult   The contents of the barcode.
     * @param scaleFactor amount by which thumbnail was scaled
     * @param barcode     A greyscale bitmap of the camera data which was decoded.
     */
    void handleDecode(Result rawResult, Bitmap barcode, float scaleFactor) {
        inactivityTimer.onActivity();
        lastResult = rawResult;
        boolean fromLiveScan = barcode != null;

        if (fromLiveScan) {
            // Then not from history, so beep/vibrate and we have an image to draw on
            if (ScanStatus == 0) {
                beepManager.playBeepSoundAndVibrate();
            }
            drawResultPoints(barcode, scaleFactor, rawResult);
        }

        //QrConfig 是否批量扫描
        boolean isBulk = false;
        if (fromLiveScan && isBulk) {
//          maybeSetClipboard(resultHandler);
            // Wait a moment or else it will scan the same barcode continuously about 3 times
            restartPreview(BULK_MODE_SCAN_DELAY_MS);
        } else {
            if (isAutoRestartAfterSuccess) {
                handleDecodeInternally(rawResult, barcode);
            }
        }
        if (qrCallback != null) {
            QRResult qrResult = new QRResult(
                    rawResult.getText(),
                    rawResult.getRawBytes(),
                    rawResult.getNumBits(),
                    rawResult.getResultPoints(),
                    rawResult.getBarcodeFormat(),
                    rawResult.getTimestamp());
            qrCallback.onSuccess(qrResult, barcode);
        }
        ScanStatus = 0;
    }

    /**
     * Superimpose a line for 1D or dots for 2D to highlight the key features of the barcode.
     *
     * @param barcode     A bitmap of the captured image.
     * @param scaleFactor amount by which thumbnail was scaled
     * @param rawResult   The decoded results which contains the points to draw.
     */
    private void drawResultPoints(Bitmap barcode, float scaleFactor, Result rawResult) {
        ResultPoint[] points = rawResult.getResultPoints();
        if (points != null && points.length > 0) {
            Canvas canvas = new Canvas(barcode);
            Paint paint = new Paint();
            paint.setColor(activity.getResources().getColor(R.color.result_points));
            if (points.length == 2) {
                paint.setStrokeWidth(4.0f);
                drawLine(canvas, paint, points[0], points[1], scaleFactor);
            } else if (points.length == 4 &&
                    (rawResult.getBarcodeFormat() == BarcodeFormat.UPC_A ||
                            rawResult.getBarcodeFormat() == BarcodeFormat.EAN_13)) {
                // Hacky special case -- draw two lines, for the barcode and metadata
                drawLine(canvas, paint, points[0], points[1], scaleFactor);
                drawLine(canvas, paint, points[2], points[3], scaleFactor);
            } else {
                paint.setStrokeWidth(10.0f);
                for (ResultPoint point : points) {
                    if (point != null) {
                        canvas.drawPoint(scaleFactor * point.getX(), scaleFactor * point.getY(), paint);
                    }
                }
            }
        }
    }

    /**
     * 生成二维码
     *
     * @param content
     * @return
     */
    public Bitmap encodeQRCode(String content) {
        ScanStatus = 1;
        WindowManager manager = (WindowManager) activity.getSystemService(Context.WINDOW_SERVICE);
        Display display = manager.getDefaultDisplay();
        Point displaySize = new Point();
        display.getSize(displaySize);
        int width = displaySize.x;
        int height = displaySize.y;
        int smallerDimension = width < height ? width : height;
        smallerDimension = smallerDimension * 7 / 8;

        QRCodeEncoder qrCodeEncoder = new QRCodeEncoder(content, smallerDimension, true);
        return qrCodeEncoder.encodeAsBitmap();
    }


    /**
     * 解析本地二维码
     *
     * @param bitmap
     */
    public void decodeQrBitmap(Bitmap bitmap) {
        ScanStatus = 2;
        handler.getDecodeHandler().decodeBitmap(bitmap);
    }

    /**
     * 添加 扫码View
     *
     * @param isAutoRestartAfterSuccess 在扫描一次成功后是否自动重新开始
     * @param qrCallback                扫描成功回调
     */
    public void toggleQrView(boolean isAutoRestartAfterSuccess, QRCallback qrCallback) {
        this.isAutoRestartAfterSuccess = isAutoRestartAfterSuccess;
        this.qrCallback = qrCallback;
        if (addedView) {
            addedView = false;
            onPause();
            viewGroup.removeView(captureView);
        } else {
            addedView = true;
            if (inited) {
                viewGroup.addView(captureView);
                onResume();
            } else {
                init();
            }
        }
    }

    private static void drawLine(Canvas canvas, Paint paint, ResultPoint a, ResultPoint b, float scaleFactor) {
        if (a != null && b != null) {
            canvas.drawLine(scaleFactor * a.getX(),
                    scaleFactor * a.getY(),
                    scaleFactor * b.getX(),
                    scaleFactor * b.getY(),
                    paint);
        }
    }

    // Put up our own UI for how to handle the decoded contents.
    private void handleDecodeInternally(Result rawResult, Bitmap barcode) {
//        Toast.makeText(activity, rawResult.toString(), Toast.LENGTH_SHORT).show();
        restartPreview(1000L);
    }


    private void initCamera(final SurfaceHolder surfaceHolder) {
        if (surfaceHolder == null) {
            throw new IllegalStateException("No SurfaceHolder provided");
        }
        if (cameraManager.isOpen()) {
            return;
        }

        try {
            cameraManager.openDriver(surfaceHolder);
            // Creating the handler starts the preview, which can also throw a RuntimeException.
            if (handler == null) {
                handler = new QRHandler(QRman.this, decodeFormats, null, characterSet, cameraManager);
            }
            decodeOrStoreSavedBitmap(null, null);
        } catch (IOException ioe) {
//            displayFrameworkBugMessageAndExit();
        } catch (RuntimeException e) {
            // Barcode Scanner has seen crashes in the wild of this variety:
            // java.?lang.?RuntimeException: Fail to connect to camera service
//            displayFrameworkBugMessageAndExit();
        }
    }

//    private void displayFrameworkBugMessageAndExit() {
//        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
//        builder.setTitle(activity.getString(R.string.app_name));
//        builder.setMessage(activity.getString(R.string.msg_camera_framework_bug));
//        builder.setPositiveButton(R.string.button_ok, new FinishListener(activity));
//        builder.setOnCancelListener(new FinishListener(activity));
//        builder.show();
//    }


    public void restartPreview(long delayMS) {
        if (handler != null) {
            handler.sendEmptyMessageDelayed(Contents.Status.RESTART_PREVIEW, delayMS);
        }
        lastResult = null;
    }

    public void drawViewfinder() {
        viewfinderView.drawViewfinder();
    }

}
