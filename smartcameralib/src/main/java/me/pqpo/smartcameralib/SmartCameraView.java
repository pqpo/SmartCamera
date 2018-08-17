package me.pqpo.smartcameralib;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.graphics.RectF;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import com.google.android.cameraview.CameraView;
import com.google.android.cameraview.Size;

/**
 * Created by pqpo on 2018/8/15.
 */
public class SmartCameraView extends CameraView {

    MaskView maskView;
    private boolean scanPreview = true;

    public SmartCameraView(@NonNull Context context) {
        this(context, null);
    }

    public SmartCameraView(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public SmartCameraView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        addMaskView();
        addCallback(new Callback() {
            @Override
            public void onPicturePreview(CameraView cameraView, byte[] data) {
                super.onPicturePreview(cameraView, data);
                if (data == null || !scanPreview) {
                    return;
                }
                int previewRotation = cameraView.getPreviewRotation();
                Size size = cameraView.getPreviewSize();
                Rect revisedMaskRect = getRevisedMaskRect();
                if (revisedMaskRect != null) {
                    int result = SmartScanner.scan(data, size.getWidth(), size.getHeight(), previewRotation,
                            revisedMaskRect.left, revisedMaskRect.top, revisedMaskRect.width(), revisedMaskRect.height(),
                            0.8f);
                    if (result == 1) {
                        takePicture();
                        stopScan();
                    }
                }
            }
        });
    }

    public Rect getRevisedMaskRect() {
        Size size = getPreviewSize();
        if (size != null) {
            int previewRotation = getPreviewRotation();
            RectF maskRect = getMaskRect();
            int cameraViewWidth = getWidth();
            int mCameraViewHeight = getHeight();
            int previewWidth;
            int previewHeight;
            if (previewRotation == 90 || previewRotation == 270) {
                previewWidth = size.getHeight();
                previewHeight = size.getWidth();
            } else {
                previewWidth = size.getWidth();
                previewHeight = size.getHeight();
            }
            float radio = Math.min(1.0f * previewWidth / cameraViewWidth, 1.0f * previewHeight / mCameraViewHeight);
            int maskX = (int) ((int) maskRect.left * radio);
            int maskY = (int) ((int) maskRect.top * radio);
            int maskW = (int) ((int) maskRect.width() * radio);
            int maskH = (int) ((int) maskRect.height() * radio);
            return new Rect(maskX, maskY, maskX + maskW, maskY + maskH);
        }
        return null;
    }

    public void startScan() {
        scanPreview = true;
    }

    public void stopScan() {
        scanPreview = false;
    }

    private void addMaskView() {
        maskView = new MaskView(getContext());
        FrameLayout.LayoutParams lp = new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        maskView.setLayoutParams(lp);
        addView(maskView);
    }

    public RectF getMaskRect() {
        if (maskView == null) {
            return null;
        }
        return maskView.getMaskRect();
    }

}
