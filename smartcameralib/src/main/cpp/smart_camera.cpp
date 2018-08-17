//
// Created by pqpo on 2018/8/16.
//
#include <opencv2/opencv.hpp>
#include <android/bitmap.h>
#include <android_utils.h>
#include "jni.h"
#include <android/log.h>
#include <sstream>

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
    Canny(blurMat2, cannyMat, 5, 80);
    Mat thresholdMat;
    threshold(cannyMat, thresholdMat, 0, 255, CV_THRESH_OTSU);
    outMat = thresholdMat;
}

void drawLines(Mat &src, vector<Vec2f> &lines) {
    // 以下遍历图像绘制每一条线
    std::vector<cv::Vec2f>::const_iterator it= lines.begin();
    while (it!=lines.end())
    {
        // 以下两个参数用来检测直线属于垂直线还是水平线
        float rho= (*it)[0];   // 表示距离
        float theta= (*it)[1]; // 表示角度

        if (theta < CV_PI/4. || theta > 3.* CV_PI/4.) // 若检测为垂直线
        {
            // 得到线与第一行的交点
            cv::Point pt1(cvRound(rho/cos(theta)),0);
            // 得到线与最后一行的交点
            cv::Point pt2(cvRound((rho - src.rows*sin(theta))/cos(theta)),src.rows);
            // 调用line函数绘制直线
            cv::line(src, pt1, pt2, cv::Scalar(255), 1);

        }
        else // 若检测为水平线
        {
            // 得到线与第一列的交点
            cv::Point pt1(0,cvRound(rho/sin(theta)));
            // 得到线与最后一列的交点
            cv::Point pt2(src.cols,cvRound((rho-src.cols*cos(theta))/sin(theta)));
            // 调用line函数绘制直线
            cv::line(src, pt1, pt2, cv::Scalar(255), 1);
        }
        ++it;
    }
}

vector<Vec2f> checkLines(Mat &scr, int houghThreshold) {
    vector<Vec2f>lines;//矢量结构lines用于存放得到的线段矢量集合
    HoughLines(scr, lines, 2, CV_PI / 180, houghThreshold);
    int lineSize = lines.size();
    return lines;
}

vector<Point> findMaxContours(Mat &src) {
    vector<vector<Point>> contours;
    findContours(src, contours, RETR_EXTERNAL, CHAIN_APPROX_NONE);
    vector<Point> maxAreaPoints;
    double maxArea = 0;
    vector<vector<Point>>::const_iterator it= contours.begin();
    while (it!=contours.end()) {
        vector<Point> item = *it;
        double area = contourArea(Mat(item));
        if(area > maxArea) {
            maxArea = area;
            maxAreaPoints = item;
        }
        ++it;
    }
    vector<Point> outDP;
    if(maxAreaPoints.size() > 0) {
        double arc = arcLength(maxAreaPoints, true);
        //多变形逼近
        approxPolyDP(Mat(maxAreaPoints), outDP, 0.02*arc, true);
    }
    return outDP;
}

extern "C"
JNIEXPORT jint JNICALL
Java_me_pqpo_smartcameralib_SmartScanner_cropRect(JNIEnv *env, jclass type, jbyteArray yuvData_,
                                                  jint width, jint height, jint rotation, jint x,
                                                  jint y, jint maskWidth, jint maskHeight,
                                                  jobject result, jfloat ratio) {
    jbyte *yuvData = env->GetByteArrayElements(yuvData_, NULL);
    Mat outMat;
    processMat(yuvData, outMat, width, height, rotation, x, y, maskWidth, maskHeight, ratio);

    vector<Point> outDP = findMaxContours(outMat);
    if (result != NULL) {
        if(outDP.size() == 4) {
            line(outMat, outDP[0], outDP[1], cv::Scalar(255), 1);
            line(outMat, outDP[1], outDP[2], cv::Scalar(255), 1);
            line(outMat, outDP[2], outDP[3], cv::Scalar(255), 1);
            line(outMat, outDP[3], outDP[0], cv::Scalar(255), 1);
        }
        mat_to_bitmap(env, outMat, result);
    }
    env->ReleaseByteArrayElements(yuvData_, yuvData, 0);
    if(outDP.size() == 4) {
        double maskArea = outMat.rows * outMat.cols;
        double realArea = contourArea(outDP);
        std::ostringstream logstr;
        logstr << "maskArea:" << maskArea << " realArea: " << realArea << std::endl;
        __android_log_write(ANDROID_LOG_DEBUG, "smart_camera.cpp", logstr.str().c_str());
        if (maskArea != 0 && (realArea / maskArea) >= 0.8)  {
            return 1;
        }
    }
    return 0;
}

extern "C"
JNIEXPORT jint JNICALL
Java_me_pqpo_smartcameralib_SmartScanner_scan(JNIEnv *env, jclass type, jbyteArray yuvData_,
                                              jint width, jint height, jint rotation, jint x,
                                              jint y, jint maskWidth, jint maskHeight,
                                              jfloat maskThreshold) {
    jbyte *yuvData = env->GetByteArrayElements(yuvData_, NULL);
    Mat outMat;
    processMat(yuvData, outMat, width, height, rotation, x, y, maskWidth, maskHeight, 0.3f);
    env->ReleaseByteArrayElements(yuvData_, yuvData, 0);

    vector<Point> outDP = findMaxContours(outMat);
    if(outDP.size() == 4) {
        double maskArea = outMat.rows * outMat.cols;
        double realArea = contourArea(outDP);
        std::ostringstream logstr;
        logstr << "maskArea:" << maskArea << " realArea: " << realArea << std::endl;
        __android_log_write(ANDROID_LOG_DEBUG, "smart_camera.cpp", logstr.str().c_str());
        if (maskArea != 0 && (realArea / maskArea) >= maskThreshold)  {
            return 1;
        }
    }
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