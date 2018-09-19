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

static const char* const LOG_TAG = "smart_camera_lib";

#define LOG_D(...) __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, __VA_ARGS__)

static struct {
    int gaussianBlurRadius = 3;
    int cannyThreshold1 = 5;
    int cannyThreshold2 = 80;
    int thresholdThresh = 0;
    int thresholdMaxVal = 255;
    float checkMinLengthRatio = 0.5;
    int houghLinesThreshold = 110;
    int houghLinesMinLineLength = 80;
    int houghLinesMaxLineGap = 10;
    float detectionRatio = 0.1;
    float angleThreshold = 5;
} gScannerParams;

Mat cropByMask(Mat &imgMat, int rotation, int maskX, int maskY, int maskWidth, int maskHeight) {
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
    return croppedMat;
}

void processMat(void* yuvData, Mat& outMat, int width, int height, int rotation, int maskX, int maskY, int maskWidth, int maskHeight, float scaleRatio) {
    Mat mYuv(height+height/2, width, CV_8UC1, (uchar *)yuvData);
    Mat imgMat(height, width, CV_8UC1);
    cvtColor(mYuv, imgMat, CV_YUV420sp2GRAY);

    Mat croppedMat = cropByMask(imgMat, rotation, maskX, maskY, maskWidth, maskHeight);

    Mat resizeMat;
    resize(croppedMat, resizeMat, Size(static_cast<int>(maskWidth * scaleRatio),
                                       static_cast<int>(maskHeight * scaleRatio)));

    Mat blurMat;
    GaussianBlur(resizeMat, blurMat, Size(gScannerParams.gaussianBlurRadius,gScannerParams.gaussianBlurRadius), 0);

    Mat cannyMat;
    Canny(blurMat, cannyMat, gScannerParams.cannyThreshold1, gScannerParams.cannyThreshold2);
    Mat dilateMat;
    dilate(cannyMat, dilateMat, getStructuringElement(MORPH_RECT, Size(2, 2)));
    Mat thresholdMat;
    threshold(dilateMat, thresholdMat, gScannerParams.thresholdThresh, gScannerParams.thresholdMaxVal, CV_THRESH_OTSU);
    outMat = thresholdMat;
}

vector<Vec4i> houghLines(Mat &scr) {
    vector<Vec4i>lines;
    HoughLinesP(scr, lines, 1, CV_PI / 180, gScannerParams.houghLinesThreshold, gScannerParams.houghLinesMinLineLength, gScannerParams.houghLinesMaxLineGap);
    return lines;
}

bool checkLines(vector<Vec4i> &lines, int checkMinLength, bool vertical) {
    for( size_t i = 0; i < lines.size(); i++ ) {
        Vec4i l = lines[i];
        int x1 = l[0];
        int y1 = l[1];
        int x2 = l[2];
        int y2 = l[3];

        float distance;
        distance = powf((x1 - x2),2) + powf((y1 - y2),2);
        distance = sqrtf(distance);

        if (distance < checkMinLength) {
            continue;
        }
        if (x2 == x1) {
            return true;
        }

        float angle = cvFastArctan(fast_abs(y2 - y1), fast_abs(x2 - x1));
        if (DEBUG) {
            std::ostringstream logStr;
            logStr << "Detection angle: [ vertical = " << vertical
                   << ", angle = " << angle << ", threshold = " << gScannerParams.angleThreshold << " ]" << std::endl;
            string log = logStr.str();
            LOG_D("%s", log.c_str());
        }
        if (vertical) {
            if(fast_abs(90 - angle) < gScannerParams.angleThreshold) {
                return true;
            }
        }
        if (!vertical) {
            if(fast_abs(angle) < gScannerParams.angleThreshold) {
                return true;
            }
        }
    }
    return false;
}

extern "C"
JNIEXPORT jint JNICALL
Java_me_pqpo_smartcameralib_SmartScanner_previewScan(JNIEnv *env, jclass type, jbyteArray yuvData_,
                                                  jint width, jint height, jint rotation, jint x,
                                                  jint y, jint maskWidth, jint maskHeight,
                                                  jobject previewBitmap, jfloat ratio) {
    jbyte *yuvData = env->GetByteArrayElements(yuvData_, NULL);
    Mat outMat;
    processMat(yuvData, outMat, width, height, rotation, x, y, maskWidth, maskHeight, ratio);
    env->ReleaseByteArrayElements(yuvData_, yuvData, 0);

    int matH = outMat.rows;
    int matW = outMat.cols;
    int thresholdW = cvRound( gScannerParams.detectionRatio * matW);
    int thresholdH = cvRound( gScannerParams.detectionRatio * matH);
    //1. crop left
    Rect rect(0, 0, thresholdW, matH);
    Mat croppedMatL = outMat(rect);
    //2. crop top
    rect.x = 0;
    rect.y = 0;
    rect.width = matW;
    rect.height = thresholdH;
    Mat croppedMatT = outMat(rect);
    //3. crop right
    rect.x = matW - thresholdW;
    rect.y = 0;
    rect.width = thresholdW;
    rect.height = matH;
    Mat croppedMatR = outMat(rect);
    //4. crop bottom
    rect.x = 0;
    rect.y = matH - thresholdH;
    rect.width = matW;
    rect.height = thresholdH;
    Mat croppedMatB = outMat(rect);

    vector<Vec4i> linesLeft = houghLines(croppedMatL);
    vector<Vec4i> linesTop = houghLines(croppedMatT);
    vector<Vec4i> linesRight = houghLines(croppedMatR);
    vector<Vec4i> linesBottom = houghLines(croppedMatB);

    if (previewBitmap != NULL) {
        drawLines(outMat, linesLeft, 0, 0);
        drawLines(outMat, linesTop, 0, 0);
        drawLines(outMat, linesRight, matW - thresholdW, 0);
        drawLines(outMat, linesBottom, 0, matH - thresholdH);
        mat_to_bitmap(env, outMat, previewBitmap);
    }

    if(DEBUG) {
        std::ostringstream logStr;
        logStr << "Number of lines in the area: [ " << linesLeft.size()
               << " , " << linesTop.size()
               << " , " << linesRight.size()
               << " , " << linesBottom.size() << " ]" << std::endl;
        string log = logStr.str();
        LOG_D("%s", log.c_str());
    }

    int checkMinLengthH = static_cast<int>(matH * gScannerParams.checkMinLengthRatio);
    int checkMinLengthW = static_cast<int>(matW * gScannerParams.checkMinLengthRatio);
    if (checkLines(linesLeft, checkMinLengthH, true) && checkLines(linesRight, checkMinLengthH, true)
        && checkLines(linesTop, checkMinLengthW, false) && checkLines(linesBottom, checkMinLengthW, false)) {
        if (DEBUG) {
            LOG_D("Detect passed!");
        }
        return 1;
    }
    return 0;
}

extern "C"
JNIEXPORT void JNICALL
Java_me_pqpo_smartcameralib_SmartScanner_crop(JNIEnv *env, jclass type, jbyteArray yuvData_,
                                              jint width, jint height, jint rotation, jint maskX,
                                              jint maskY, jint maskWidth, jint maskHeight,
                                              jobject resultBitmap) {
    jbyte *yuvData = env->GetByteArrayElements(yuvData_, NULL);
    Mat mYuv(height+height/2, width, CV_8UC1, (uchar *)yuvData);
    Mat imgMat(height, width, CV_8UC4);
    cvtColor(mYuv, imgMat, CV_YUV420sp2RGBA);
    Mat croppedMat = cropByMask(imgMat, rotation, maskX, maskY, maskWidth, maskHeight);
    mat_to_bitmap(env, croppedMat, resultBitmap);
    env->ReleaseByteArrayElements(yuvData_, yuvData, 0);
}

static void initScannerParams(JNIEnv *env) {
    jclass classDocScanner = env->FindClass(kClassScanner);
    DEBUG = env->GetStaticBooleanField(classDocScanner, env -> GetStaticFieldID(classDocScanner, "DEBUG", "Z"));
    gScannerParams.gaussianBlurRadius = env->GetStaticIntField(classDocScanner, env -> GetStaticFieldID(classDocScanner, "gaussianBlurRadius", "I"));
    gScannerParams.cannyThreshold1 = env->GetStaticIntField(classDocScanner, env -> GetStaticFieldID(classDocScanner, "cannyThreshold1", "I"));
    gScannerParams.cannyThreshold2 = env->GetStaticIntField(classDocScanner, env -> GetStaticFieldID(classDocScanner, "cannyThreshold2", "I"));
    gScannerParams.checkMinLengthRatio = env->GetStaticFloatField(classDocScanner, env -> GetStaticFieldID(classDocScanner, "checkMinLengthRatio", "F"));
    gScannerParams.houghLinesThreshold = env->GetStaticIntField(classDocScanner, env -> GetStaticFieldID(classDocScanner, "houghLinesThreshold", "I"));
    gScannerParams.houghLinesMinLineLength = env->GetStaticIntField(classDocScanner, env -> GetStaticFieldID(classDocScanner, "houghLinesMinLineLength", "I"));
    gScannerParams.houghLinesMaxLineGap = env->GetStaticIntField(classDocScanner, env -> GetStaticFieldID(classDocScanner, "houghLinesMaxLineGap", "I"));
    gScannerParams.detectionRatio = env->GetStaticFloatField(classDocScanner, env -> GetStaticFieldID(classDocScanner, "detectionRatio", "F"));
    gScannerParams.angleThreshold = env->GetStaticFloatField(classDocScanner, env -> GetStaticFieldID(classDocScanner, "angleThreshold", "F"));
    if (DEBUG) {
        LOG_D("load params done!");
    }
}

extern "C"
JNIEXPORT void JNICALL
Java_me_pqpo_smartcameralib_SmartScanner_reloadParams(JNIEnv *env, jclass type) {
    initScannerParams(env);
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