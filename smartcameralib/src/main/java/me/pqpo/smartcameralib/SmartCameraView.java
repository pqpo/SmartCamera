package me.pqpo.smartcameralib;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Rect;
import android.graphics.RectF;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.AttributeSet;

import com.google.android.cameraview.CameraView;
import com.google.android.cameraview.base.Size;

/**
 * Created by pqpo on 2018/8/15.
 */
public class SmartCameraView extends CameraView {

    private static final String TAG = "SmartCameraView";

    protected SmartScanner smartScanner;

    protected MaskViewImpl maskView;
    protected boolean scanning = true;
    private Handler uiHandler;
    private OnScanResultListener onScanResultListener;

    public SmartCameraView(@NonNull Context context) {
        this(context, null);
    }

    public SmartCameraView(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public SmartCameraView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
        setMaskView(new MaskView(context));
    }

    private void init() {
        smartScanner = new SmartScanner();
        uiHandler = new ScanResultHandler(this);

        addCallback(new Callback() {
            @Override
            public void onPicturePreview(CameraView cameraView, byte[] data) {
                super.onPicturePreview(cameraView, data);
                if (data == null || !scanning) {
                    return;
                }
                int previewRotation = getPreviewRotation();
                Size size = getPreviewSize();
                Rect revisedMaskRect = getAdjustPreviewMaskRect();
                if (revisedMaskRect != null && size != null) {
                    int result = smartScanner.previewScan(data, size.getWidth(), size.getHeight(), previewRotation, revisedMaskRect);
                    uiHandler.obtainMessage(result, data).sendToTarget();
                }
            }
        });
    }

    public SmartScanner getSmartScanner() {
        return smartScanner;
    }

    public MaskViewImpl getMaskView() {
        return maskView;
    }

    public Bitmap getPreviewBitmap() {
        return smartScanner.getPreviewBitmap();
    }

    public void setOnScanResultListener(OnScanResultListener onScanResultListener) {
        this.onScanResultListener = onScanResultListener;
    }

    public Rect getAdjustPictureMaskRect() {
        Size size = getPictureSize();
        return getAdjustMaskRect(size);
    }

    public Rect getAdjustPreviewMaskRect() {
        Size size = getPreviewSize();
        return getAdjustMaskRect(size);
    }

    public Rect getAdjustMaskRect(Size size) {
        if (size != null) {
            int previewRotation = getPreviewRotation();
            RectF maskRect = getMaskRect();
            int cameraViewWidth = getWidth();
            int cameraViewHeight = getHeight();
            int picW;
            int picH;
            if (previewRotation == 90 || previewRotation == 270) {
                picW = size.getHeight();
                picH = size.getWidth();
            } else {
                picW = size.getWidth();
                picH = size.getHeight();
            }
            float radio = Math.min(1.0f * picW / cameraViewWidth, 1.0f * picH / cameraViewHeight);
            int maskX = (int) ((int) maskRect.left * radio);
            int maskY = (int) ((int) maskRect.top * radio);
            int maskW = (int) ((int) maskRect.width() * radio);
            int maskH = (int) ((int) maskRect.height() * radio);
            return new Rect(maskX, maskY, maskX + maskW, maskY + maskH);
        }
        return null;
    }

    public void startScan() {
        scanning = true;
    }

    public void stopScan() {
        scanning = false;
    }

    public void setMaskView(MaskViewImpl maskView) {
        if (this.maskView == maskView) {
            return;
        }
        if (this.maskView != null) {
            removeView(this.maskView.getMaskView());
        }
        this.maskView = maskView;
        addView(maskView.getMaskView());
    }

    public RectF getMaskRect() {
        if (maskView == null) {
            return null;
        }
        return maskView.getMaskRect();
    }

    public void cropImage(final byte[] data, final CropCallback cropCallback) {
        new Thread() {
            @Override
            public void run() {
                super.run();
                Rect revisedMaskRect = getAdjustPictureMaskRect();
                Bitmap bitmapSrc = BitmapFactory.decodeByteArray(data, 0, data.length);
                if (revisedMaskRect != null) {
                    final Bitmap bitmap = Bitmap.createBitmap(bitmapSrc, revisedMaskRect.left, revisedMaskRect.top, revisedMaskRect.width(), revisedMaskRect.height());
                    bitmapSrc.recycle();
                    post(new Runnable() {
                        @Override
                        public void run() {
                            cropCallback.onCropped(bitmap);
                        }
                    });
                    return;
                }
                post(new Runnable() {
                    @Override
                    public void run() {
                        cropCallback.onCropped(null);
                    }
                });
            }
        }.start();
    }

    public interface CropCallback {
        void onCropped(Bitmap cropBitmap);
    }

    public interface OnScanResultListener {
        boolean onScanResult(SmartCameraView smartCameraView, int result, byte[] yuvData);
    }

    private static class ScanResultHandler extends Handler {

        SmartCameraView smartCameraView;

        public ScanResultHandler(SmartCameraView smartCameraView) {
            this.smartCameraView = smartCameraView;
        }

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            if(!smartCameraView.scanning) {
                return;
            }
            int result = msg.what;
            byte[] data = (byte[]) msg.obj;
            if (smartCameraView.onScanResultListener == null
                    || !smartCameraView.onScanResultListener.onScanResult(smartCameraView, result, data)) {
                if (result == 1) {
                    smartCameraView.takePicture();
                    smartCameraView.stopScan();
                }
            }
        }
    }

}
