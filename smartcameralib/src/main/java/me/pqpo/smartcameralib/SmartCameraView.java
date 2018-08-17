package me.pqpo.smartcameralib;

import android.content.Context;
import android.graphics.Bitmap;
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
                Size size = cameraView.getPreviewSize();
                if (size != null) {
                    int previewRotation = cameraView.getPreviewRotation();
                    RectF maskRect = getMaskRect();
                    float radio;
                    if (previewRotation == 90 || previewRotation == 270) {
                        radio = 1.0f * size.getHeight() / getWidth();
                    } else {
                        radio = 1.0f * size.getWidth() / getWidth();
                    }
                    int maskX = (int) ((int) maskRect.left * radio);
                    int maskY = (int) ((int) maskRect.top * radio);
                    int maskW = (int) ((int) maskRect.width() * radio);
                    int maskH = (int) ((int) maskRect.height() * radio);
                    int round = Math.round(Math.min(maskW, maskH) * 1.0f * 0.2f);
                    int scan = SmartScanner.scan(data, size.getWidth(), size.getHeight(), previewRotation, maskX, maskY, maskW, maskH, round);
                    if (scan == 1) {
                        takePicture();
                        stopScan();
                    }
                }
            }
        });
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
