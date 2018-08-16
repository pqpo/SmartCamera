//
// Created by pqpo on 2018/8/16.
//
#include <opencv2/opencv.hpp>
#include <android/bitmap.h>
#include <android_utils.h>
#include "jni.h"

using namespace cv;

extern "C"
JNIEXPORT void JNICALL
Java_me_pqpo_smartcameralib_SmartScanner_cropRect(JNIEnv *env, jclass type, jbyteArray yuvData_,
                                                  jint width, jint height, jfloat left, jfloat top,
                                                  jfloat right, jfloat bottom, jobject result) {
    jbyte *yuvData = env->GetByteArrayElements(yuvData_, NULL);
    Mat mYuv(height+height/2, width, CV_8UC1, (uchar *)yuvData);
    Mat mBgr(height, width, CV_8UC3);
    cvtColor(mYuv, mBgr, CV_YUV420sp2RGB);
    mat_to_bitmap(env, mBgr, result);
    env->ReleaseByteArrayElements(yuvData_, yuvData, 0);
}