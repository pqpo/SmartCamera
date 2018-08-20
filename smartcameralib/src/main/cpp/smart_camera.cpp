//
// Created by pqpo on 2018/8/16.
//
#include <opencv2/opencv.hpp>
#include <android/bitmap.h>
#include <android_utils.h>
#include "jni.h"
#include <android/log.h>
#include <sstream>
#include <opencv_utils.h>

using namespace cv;

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
    Canny(blurMat2, cannyMat, 5, 80);
    Mat thresholdMat;
    threshold(cannyMat, thresholdMat, 0, 255, CV_THRESH_OTSU);
    outMat = thresholdMat;
}

extern "C"
JNIEXPORT jint JNICALL
Java_me_pqpo_smartcameralib_SmartScanner_previewScan(JNIEnv *env, jclass type, jbyteArray yuvData_,
                                                  jint width, jint height, jint rotation, jint x,
                                                  jint y, jint maskWidth, jint maskHeight,
                                                  jobject previewBitmap, jfloat ratio, jfloat checkRatio) {
    jbyte *yuvData = env->GetByteArrayElements(yuvData_, NULL);
    Mat outMat;
    processMat(yuvData, outMat, width, height, rotation, x, y, maskWidth, maskHeight, ratio);
    env->ReleaseByteArrayElements(yuvData_, yuvData, 0);

    vector<Point> outDP = findMaxContours(outMat);
    if (previewBitmap != NULL) {
        if(outDP.size() == 4) {
            line(outMat, outDP[0], outDP[1], cv::Scalar(255), 1);
            line(outMat, outDP[1], outDP[2], cv::Scalar(255), 1);
            line(outMat, outDP[2], outDP[3], cv::Scalar(255), 1);
            line(outMat, outDP[3], outDP[0], cv::Scalar(255), 1);
        }
        mat_to_bitmap(env, outMat, previewBitmap);
    }

    if(outDP.size() == 4) {
        double maskArea = outMat.rows * outMat.cols;
        double realArea = contourArea(outDP);
//        std::ostringstream areaLog;
//        areaLog << "maskArea:" << maskArea << " realArea: " << realArea << std::endl;
//        __android_log_write(ANDROID_LOG_DEBUG, "smart_camera.cpp", areaLog.str().c_str());
        if (maskArea != 0 && (realArea / maskArea) >= checkRatio)  {
            return 1;
        }
    }
    return 0;
}

//extern "C"
//JNIEXPORT void JNICALL
//Java_me_pqpo_smartcameralib_SmartScanner_cropMask(JNIEnv *env, jclass type, jbyteArray data_, jint dataSize,
//                                                  jint width, jint height, jint maskX, jint maskY,
//                                                  jint maskW, jint maskH, jobject outBitmap) {
//    jbyte *data = env->GetByteArrayElements(data_, NULL);
//
//    vector<uchar> jpgbytes(data, data+dataSize);
//    Mat image = imdecode(jpgbytes, CV_LOAD_IMAGE_UNCHANGED);
//    cvtColor(image, image, CV_BGR2RGB);
//
//    Rect rect(maskX, maskY, maskW, maskH);
//    Mat croppedMat = image(rect);
//    mat_to_bitmap(env, croppedMat, outBitmap);
//}