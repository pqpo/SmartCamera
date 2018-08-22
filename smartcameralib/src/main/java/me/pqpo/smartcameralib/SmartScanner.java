package me.pqpo.smartcameralib;

import android.graphics.Bitmap;
import android.graphics.Rect;

/**
 * Created by pqpo on 2018/8/16.
 */
public class SmartScanner {

    public static boolean DEBUG = false;

    public static float checkMinLengthRatio = 0.5f;

    // 高斯模糊半径，消除噪点，必须为正奇数。
    // 第一次为原图模糊，第二次为灰度图模糊
    public static int firstGaussianBlurRadius = 3;
    public static int secondGaussianBlurRadius = 3;
    public static int cannyThreshold1 = 25;
    public static int cannyThreshold2 = 50;

    public static int houghLinesThreshold = 110;
    public static int houghLinesMinLineLength = 80;
    public static double houghLinesMaxLineGap = 10.0;


    private int maxSize = 600;
    private float checkRatio = 0.15f;
    private boolean preview = false;

    protected Bitmap mPreviewBitmap;

    public SmartScanner setMaxSize(int maxSize) {
        this.maxSize = maxSize;
        return this;
    }

    public int getMaxSize() {
        return maxSize;
    }

    public SmartScanner setCheckRatio(float checkRatio) {
        this.checkRatio = checkRatio;
        return this;
    }

    public float getCheckRatio() {
        return checkRatio;
    }

    public SmartScanner setPreview(boolean preview) {
        this.preview = preview;
        return this;
    }

    public boolean isPreview() {
        return preview;
    }

    public Bitmap getPreviewBitmap() {
        return mPreviewBitmap;
    }

    public int previewScan(byte[] yuvData, int width, int height, int rotation, Rect maskRect) {
        float scaleRatio = calculateScaleRatio(width, height);
        Bitmap previewBitmap = null;
        if (preview) {
            previewBitmap = preparePreviewBitmap((int)(scaleRatio * maskRect.width()),
                    (int)(scaleRatio * maskRect.height()));
        }
        return previewScan(yuvData, width, height, rotation, maskRect.left, maskRect.top, maskRect.width(), maskRect.height(), previewBitmap, scaleRatio, this.checkRatio);
    }

    private Bitmap preparePreviewBitmap(int bitmapW, int bitmapH) {
        if (mPreviewBitmap != null
                && (mPreviewBitmap.getWidth() != bitmapW || mPreviewBitmap.getHeight() != bitmapH)) {
            mPreviewBitmap.recycle();
            mPreviewBitmap = null;
        }
        if (mPreviewBitmap == null) {
            mPreviewBitmap = Bitmap.createBitmap(bitmapW, bitmapH, Bitmap.Config.ARGB_8888);
        }
        return mPreviewBitmap;
    }

    private float calculateScaleRatio(int width, int height) {
        float maxSize = this.maxSize * 1.0f;
        float ratio = Math.min(maxSize / width, maxSize / height);
        return Math.max(0, Math.min(ratio, 1));
    }

    private static native int previewScan(byte[] yuvData, int width, int height, int rotation, int x, int y, int maskWidth, int maskHeight, Bitmap previewBitmap, float scaleRatio, float checkRatio);

    static {
        System.loadLibrary("smart_camera");
    }

}
