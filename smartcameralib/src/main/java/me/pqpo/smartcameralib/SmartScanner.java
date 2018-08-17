package me.pqpo.smartcameralib;

import android.graphics.Bitmap;

/**
 * Created by pqpo on 2018/8/16.
 */
public class SmartScanner {

    public static native void cropRect(byte[] yuvData, int width, int height, int rotation, int x, int y, int maskWidth, int maskHeight, Bitmap result, float scaleRatio);

    public static native int scan(byte[] yuvData, int width, int height, int rotation, int x, int y, int maskWidth, int maskHeight, int threshold);

    static {
        System.loadLibrary("smart_camera");
    }

}
