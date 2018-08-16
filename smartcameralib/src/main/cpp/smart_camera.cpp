//
// Created by pqpo on 2018/8/16.
//
#include <opencv2/opencv.hpp>
#include <android/bitmap.h>
#include <android_utils.h>
#include "jni.h"
#include <math.h>

using namespace cv;

void matRotateClockWise90(Mat &src);
void matRotateClockWise180(Mat &src);
void matRotateClockWise270(Mat &src);


extern "C"
JNIEXPORT void JNICALL
Java_me_pqpo_smartcameralib_SmartScanner_cropRect(JNIEnv *env, jclass type, jbyteArray yuvData_,
                                                  jint width, jint height, jint rotation, jint x,
                                                  jint y, jint maskWidth, jint maskHeight,
                                                  jobject result) {
    jbyte *yuvData = env->GetByteArrayElements(yuvData_, NULL);
    Mat mYuv(height+height/2, width, CV_8UC1, (uchar *)yuvData);
    Mat mBgr(height, width, CV_8UC3);
    cvtColor(mYuv, mBgr, CV_YUV420sp2RGB);
    if (rotation == 90) {
        matRotateClockWise90(mBgr);
    } else if (rotation == 180) {
        matRotateClockWise180(mBgr);
    } else if (rotation == 270) {
        matRotateClockWise270(mBgr);
    }
    int newHeight = mBgr.rows;
    int newWidth = mBgr.cols;
    x = max(0, min(x, newWidth));
    y = max(0, min(y, newHeight));
    maskWidth = max(0, min(maskWidth, newWidth - x));
    maskHeight = max(0, min(maskHeight, newHeight - y));

    Rect rect(x, y, maskWidth, maskHeight);
    Mat croppedMat = mBgr(rect);
    mat_to_bitmap(env, croppedMat, result);
    env->ReleaseByteArrayElements(yuvData_, yuvData, 0);
}

//顺时针90
void matRotateClockWise90(Mat &src)
{
    transpose(src, src);
    flip(src, src, 1);
}

//顺时针180
void matRotateClockWise180(Mat &src)
{
    flip(src, src, -1);
}

//顺时针270
void matRotateClockWise270(Mat &src)
{
    transpose(src, src);
    flip(src, src, 0);
}