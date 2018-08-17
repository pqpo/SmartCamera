//
// Created by pqpo on 2018/8/16.
//
#include <opencv2/opencv.hpp>
#include <android/bitmap.h>
#include <android_utils.h>
#include "jni.h"

using namespace cv;

void matRotateClockWise90(Mat &src);
void matRotateClockWise180(Mat &src);
void matRotateClockWise270(Mat &src);

void processMat(void* yuvData, Mat& outMat, int width, int height, int rotation, int maskX, int maskY, int maskWidth, int maskHeight, float scaleRatio) {
    Mat mYuv(height+height/2, width, CV_8UC1, (uchar *)yuvData);
    Mat imgMat(height, width, CV_8UC1);
    cvtColor(mYuv, imgMat, CV_YUV420sp2RGB);

    if (rotation == 90) {
        matRotateClockWise90(imgMat);
    } else if (rotation == 180) {
        matRotateClockWise180(imgMat);
    } else if (rotation == 270) {
        matRotateClockWise270(imgMat);
    }
    int newHeight = imgMat.rows;
    int newWidth = imgMat.cols;
    maskX = max(0, min(maskX, newWidth));
    maskY = max(0, min(maskY, newHeight));
    maskWidth = max(0, min(maskWidth, newWidth - maskX));
    maskHeight = max(0, min(maskHeight, newHeight - maskY));

    Rect rect(maskX, maskY, maskWidth, maskHeight);
    Mat croppedMat = imgMat(rect);

    Mat resizeMat;
    resize(croppedMat, resizeMat, Size(cvRound(maskWidth*scaleRatio), cvRound(maskHeight*scaleRatio)));

    Mat blurMat;
    GaussianBlur(resizeMat, blurMat, Size(3,3), 0);
    Mat grayMat;
    cvtColor(blurMat, grayMat, CV_RGB2GRAY);
    Mat blurMat2;
    GaussianBlur(grayMat, blurMat2, Size(3,3), 0);

    Mat cannyMat;
    Canny(blurMat2, cannyMat, 5, 100, 3);
    Mat thresholdMat;
    threshold(cannyMat, thresholdMat, 128, 255, CV_THRESH_OTSU);
    outMat = thresholdMat;
}

extern "C"
JNIEXPORT void JNICALL
Java_me_pqpo_smartcameralib_SmartScanner_cropRect(JNIEnv *env, jclass type, jbyteArray yuvData_,
                                                  jint width, jint height, jint rotation, jint x,
                                                  jint y, jint maskWidth, jint maskHeight,
                                                  jobject result, jfloat ratio) {
    jbyte *yuvData = env->GetByteArrayElements(yuvData_, NULL);
    Mat outMat;
    processMat(yuvData, outMat, width, height, rotation, x, y, maskWidth, maskHeight, ratio);
    mat_to_bitmap(env, outMat, result);
    env->ReleaseByteArrayElements(yuvData_, yuvData, 0);
}

extern "C"
JNIEXPORT jint JNICALL
Java_me_pqpo_smartcameralib_SmartScanner_scan(JNIEnv *env, jclass type, jbyteArray yuvData_,
                                              jint width, jint height, jint rotation, jint x,
                                              jint y, jint maskWidth, jint maskHeight,
                                              jint threshold) {
    jbyte *yuvData = env->GetByteArrayElements(yuvData_, NULL);
    Mat outMat;
    processMat(yuvData, outMat, width, height, rotation, x, y, maskWidth, maskHeight, 1);


    env->ReleaseByteArrayElements(yuvData_, yuvData, 0);
    return 0;
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