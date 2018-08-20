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
import android.util.Log;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import com.google.android.cameraview.CameraView;
import com.google.android.cameraview.Size;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Created by pqpo on 2018/8/15.
 */
public class SmartCameraView extends CameraView {

    private static final String TAG = "SmartCameraView";

    protected MaskViewImpl maskView;
    protected boolean scanning = true;
    private Thread previewThread;
    private ConcurrentLinkedQueue<byte[]> previewDataQueue;
    private Handler uiHandler;
    private OnScanResultListener onScanResultListener;

    private boolean preview = false;
    private Bitmap previewBitmap = null;

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
        uiHandler = new ScanResultHandler(this);
        previewDataQueue = new ConcurrentLinkedQueue<>();
        previewThread = new ScanThread();
        previewThread.start();

        addCallback(new Callback() {
            @Override
            public void onPicturePreview(CameraView cameraView, byte[] data) {
                super.onPicturePreview(cameraView, data);
                if (data == null || !scanning) {
                    return;
                }
                if (previewDataQueue.size() <= 10) {
                    previewDataQueue.offer(data);
                }
            }
        });
    }

    public MaskViewImpl getMaskView() {
        return maskView;
    }

    public Bitmap getPreviewBitmap() {
        return previewBitmap;
    }

    public void setOnScanResultListener(OnScanResultListener onScanResultListener) {
        this.onScanResultListener = onScanResultListener;
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        previewThread.interrupt();
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

    public void setPreview(boolean preview) {
        this.preview = preview;
        previewBitmap = null;
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
        boolean onScanResult(SmartCameraView smartCameraView, int result);
    }

    private class ScanThread extends Thread {

        public ScanThread() {
            super("ScanThread");
        }

        @Override
        public void run() {
            super.run();
            while (!previewThread.isInterrupted()) {
                byte[] data = previewDataQueue.poll();
                if (data != null && scanning) {
                    int previewRotation = getPreviewRotation();
                    Size size = getPreviewSize();
                    Rect revisedMaskRect = getAdjustPreviewMaskRect();
                    float scaleRatio = 0.3f;
                    if (revisedMaskRect != null && size != null) {
                        if (preview && previewBitmap == null) {
                            previewBitmap = Bitmap.createBitmap(Math.round(scaleRatio * revisedMaskRect.width()),
                                    Math.round(scaleRatio * revisedMaskRect.height()), Bitmap.Config.ARGB_8888);
                        }
                        int result = SmartScanner.previewScan(data, size.getWidth(), size.getHeight(), previewRotation,
                                revisedMaskRect.left, revisedMaskRect.top, revisedMaskRect.width(), revisedMaskRect.height(),
                                previewBitmap, scaleRatio, 0.85f);
                        uiHandler.sendEmptyMessage(result);
                    }
                }
            }
        }
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
            if (smartCameraView.onScanResultListener == null
                    || !smartCameraView.onScanResultListener.onScanResult(smartCameraView, result)) {
                if (result == 1) {
                    smartCameraView.takePicture();
                    smartCameraView.stopScan();
                }
            }
        }
    }

}
