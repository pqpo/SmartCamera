package me.pqpo.smartcameralib;

import android.graphics.Bitmap;

/**
 * Created by pqpo on 2018/8/16.
 */
public class SmartScanner {

    public static native int previewScan(byte[] yuvData, int width, int height, int rotation, int x, int y, int maskWidth, int maskHeight, Bitmap previewBitmap, float scaleRatio, float checkRatio);

    public static native void cropMask(byte[] data, int width, int height, int maskX, int maskY, int maskW, int maskH, Bitmap outBitmap);

    static {
        System.loadLibrary("smart_camera");
    }

}
