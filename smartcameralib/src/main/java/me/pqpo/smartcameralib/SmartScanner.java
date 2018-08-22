package me.pqpo.smartcameralib;

import android.graphics.Bitmap;
import android.graphics.Rect;

/**
 * Created by pqpo on 2018/8/16.
 */
public class SmartScanner {

    /*** 以下参数会在 native 代码中读取 */

    public static boolean DEBUG = false;

    /**
     * 线段最小长度检测比例
     * 例如：
     * 靠近上边框检测出一条线段长度为： checkLength
     * 上边框总宽度为：width
     * 那么：
     * if (checkLength > width * checkMinLengthRatio ) {
     *     该线段符合检测条件，认为该线段为被检测物体上边框
     * }
     */
    public static float checkMinLengthRatio = 0.8f;

    /**
     * 高斯模糊半径，用于消除噪点，必须为正奇数。
     * 第一次为原图模糊，第二次为灰度图模糊
     */
    public static int firstGaussianBlurRadius = 3;
    public static int secondGaussianBlurRadius = 3;
    /**
     * canny 算符阈值
     * 1. 低于阈值1的像素点会被认为不是边缘；
     * 2. 高于阈值2的像素点会被认为是边缘；
     * 3. 在阈值1和阈值2之间的像素点,若与第2步得到的边缘像素点相邻，则被认为是边缘，否则被认为不是边缘。
     *
     * 大小比例推荐2到3倍
     */
    public static int cannyThreshold1 = 20;
    public static int cannyThreshold2 = 50;
    /**
     * 霍夫变换检测线段参数
     * 1. threshold: 最小投票数，要检测一条直线所需最少的的曲线交点，增大该值会减少检测出的线段数量。
     * 2. minLinLength: 能组成一条直线的最少点的数量, 点数量不足的直线将被抛弃。
     * 3. maxLineGap: 能被认为在一条直线上的点的最大距离，若出现较多断断续续的线段可以适当增大该值。
     */
    public static int houghLinesThreshold = 130;
    public static int houghLinesMinLineLength = 80;
    public static double houghLinesMaxLineGap = 10.0;

    // 以上参数会在 native 代码中读取

    /**
     * 为了提高性能，检测的图片会缩小到该尺寸之内
     * 太小的话会影响检测效果
     */
    private int maxSize = 300;
    /**
     * 检测范围比例
     * 比例越小表示待检测物体要更靠近边框
     */
    private float checkRatio = 0.10f;

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
        float scaleRatio = calculateScaleRatio(maskRect.width(), maskRect.height());
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
