package me.pqpo.smartcameralib;

import android.graphics.Bitmap;
import android.graphics.Rect;

/**
 * Created by pqpo on 2018/8/16.
 */
public class SmartScanner {

    private static final String TAG = "SmartScanner";

    /*** 以下配置参数会在 native 代码中读取 */

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
     */
    public static int gaussianBlurRadius = 3;
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
    public static int houghLinesMaxLineGap = 10;
    /**
     * 检测范围比例
     * 比例越小表示待检测物体要更靠近边框
     */
    public static float detectionRatio = 0.1f;
    /**
     * 为了提高性能，检测的图片会缩小到该尺寸之内
     * 设置太小的话会影响检测效果
     */
    public static float maxSize = 300;

    /**
     * 检测角度阈值
     * 例如:
     *
     * 实际检测时会将夹角收敛到第一象限
     *
     * 1. 检测出来的左边框线段与 x 轴夹角为 angleLeft
     *  if （abs(90 - angleLeft) < angleThreshold） {
     *      该线段符合检测条件,几乎垂直
     *  }
     * 2. 检测出来的上边框线段与 x 轴夹角为 angleTop
     *  if （abs(angleTop) < angleThreshold） {
     *      该线段符合检测条件,几乎水平
     *  }
     */
    public static float angleThreshold = 5;

    /** 预览 */
    private boolean preview = false;
    private Bitmap mPreviewBitmap;

    /**
     * 是否需要预览处理过程中的图片
     * 用于调用参数，开启后通过 getPreviewBitmap() 获取实时处理的图片
     * 出于性能考虑，其他情况下请关闭预览
     */
    public SmartScanner setPreview(boolean preview) {
        this.preview = preview;
        if (!preview) {
            mPreviewBitmap = null;
        }
        return this;
    }

    public boolean isPreview() {
        return preview;
    }

    public Bitmap getPreviewBitmap() {
        if (mPreviewBitmap != null && mPreviewBitmap.isRecycled()) {
            mPreviewBitmap = null;
        }
        return mPreviewBitmap;
    }

    public int previewScan(byte[] yuvData, int width, int height, int rotation, Rect maskRect) {
        float scaleRatio = calculateScaleRatio(maskRect.width(), maskRect.height());
        Bitmap previewBitmap = null;
        if (preview) {
            previewBitmap = preparePreviewBitmap((int)(scaleRatio * maskRect.width()),
                    (int)(scaleRatio * maskRect.height()));
        }
        return previewScan(yuvData, width, height, rotation, maskRect.left, maskRect.top, maskRect.width(), maskRect.height(), previewBitmap, scaleRatio);
    }

    private Bitmap preparePreviewBitmap(int bitmapW, int bitmapH) {
        if (bitmapH == 0 || bitmapW == 0) {
            return null;
        }
        if (mPreviewBitmap != null
                && (mPreviewBitmap.getWidth() != bitmapW || mPreviewBitmap.getHeight() != bitmapH)) {
            mPreviewBitmap = null;
        }
        if (mPreviewBitmap == null) {
            mPreviewBitmap = Bitmap.createBitmap(bitmapW, bitmapH, Bitmap.Config.ARGB_8888);
        }
        return mPreviewBitmap;
    }

    private static float calculateScaleRatio(int width, int height) {
        float ratio = Math.min(maxSize / width, maxSize / height);
        return Math.max(0, Math.min(ratio, 1));
    }

    private static native int previewScan(byte[] yuvData, int width, int height, int rotation, int x, int y, int maskWidth, int maskHeight, Bitmap previewBitmap, float scaleRatio);

    public static native void reloadParams();

    public static native void crop(byte[] yuvData, int width, int height, int rotation, int x, int y, int maskWidth, int maskHeight, Bitmap resultBitmap);

    static {
        System.loadLibrary("smart_camera");
    }

}
