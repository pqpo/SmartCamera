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

static const char* const kClassScanner = "me/pqpo/smartcameralib/SmartScanner";

static bool DEBUG = false;

static struct {
    int firstGaussianBlurRadius = 3;
    int secondGaussianBlurRadius = 3;
    int cannyThreshold1 = 5;
    int cannyThreshold2 = 80;
    int thresholdThresh = 0;
    int thresholdMaxVal = 255;
} gScannerParams;

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
    resize(croppedMat, resizeMat, Size(static_cast<int>(maskWidth * scaleRatio),
                                       static_cast<int>(maskHeight * scaleRatio)));

    Mat blurMat;
    GaussianBlur(resizeMat, blurMat, Size(gScannerParams.firstGaussianBlurRadius,gScannerParams.firstGaussianBlurRadius), 0);
    Mat grayMat;
    cvtColor(blurMat, grayMat, CV_RGB2GRAY);
    Mat blurMat2;
    GaussianBlur(grayMat, blurMat2, Size(gScannerParams.secondGaussianBlurRadius, gScannerParams.secondGaussianBlurRadius), 0);

    Mat cannyMat;
    Canny(blurMat2, cannyMat, gScannerParams.cannyThreshold1, gScannerParams.cannyThreshold2);
    Mat thresholdMat;
    threshold(cannyMat, thresholdMat, gScannerParams.thresholdThresh, gScannerParams.thresholdMaxVal, CV_THRESH_OTSU);
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
        if (DEBUG) {
            std::ostringstream areaLog;
            areaLog << "previewScan: " << "maskArea=" << maskArea << "; realArea= " << realArea << "; checkRatio= " << ratio << std::endl;
            __android_log_write(ANDROID_LOG_DEBUG, "smart_camera.cpp", areaLog.str().c_str());
        }
        if (maskArea != 0 && (realArea / maskArea) >= checkRatio)  {
            return 1;
        }
    }
    return 0;
}

static void initScannerParams(JNIEnv *env) {
    jclass classDocScanner = env->FindClass(kClassScanner);
    DEBUG = env->GetStaticBooleanField(classDocScanner, env -> GetStaticFieldID(classDocScanner, "DEBUG", "Z"));
    gScannerParams.firstGaussianBlurRadius = env->GetStaticIntField(classDocScanner, env -> GetStaticFieldID(classDocScanner, "firstGaussianBlurRadius", "I"));
    gScannerParams.secondGaussianBlurRadius = env->GetStaticIntField(classDocScanner, env -> GetStaticFieldID(classDocScanner, "secondGaussianBlurRadius", "I"));
    gScannerParams.cannyThreshold1 = env->GetStaticIntField(classDocScanner, env -> GetStaticFieldID(classDocScanner, "cannyThreshold1", "I"));
    gScannerParams.cannyThreshold2 = env->GetStaticIntField(classDocScanner, env -> GetStaticFieldID(classDocScanner, "cannyThreshold2", "I"));
//    gScannerParams.thresholdThresh = env->GetStaticIntField(classDocScanner, env -> GetStaticFieldID(classDocScanner, "thresholdThresh", "I"));
//    gScannerParams.thresholdMaxVal = env->GetStaticIntField(classDocScanner, env -> GetStaticFieldID(classDocScanner, "thresholdMaxVal", "I"));
}

extern "C"
JNIEXPORT jint JNICALL
JNI_OnLoad(JavaVM* vm, void* reserved) {
    JNIEnv *env = NULL;
    if (vm->GetEnv((void **) &env, JNI_VERSION_1_4) != JNI_OK) {
        return JNI_FALSE;
    }
    initScannerParams(env);
    return JNI_VERSION_1_4;
}