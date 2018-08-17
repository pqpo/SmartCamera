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

void drawDetectLines(Mat& image,const vector<Vec4i>& lines, Scalar& color) {
     // 将检测到的直线在图上画出来
     vector<Vec4i>::const_iterator it=lines.begin();
     while(it!=lines.end())
    {
        Point pt1((*it)[0],(*it)[1]);
        Point pt2((*it)[2],(*it)[3]);
        line(image,pt1,pt2,color,2); //  线条宽度设置为2
        ++it;
    }
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

    int houghThreshold = cvRound(min(outMat.rows, outMat.cols) * 0.45);
    vector<Vec2f>lines;//矢量结构lines用于存放得到的线段矢量集合
    HoughLines(outMat, lines, 2, CV_PI / 180, houghThreshold);
    drawLines(outMat, lines);

//    vector<Vec4i> lines;
//    int matH = outMat.rows;
//    // 检测直线，最小投票为90，线条不短于50，间隙不小于10
//    HoughLinesP(outMat, lines, 2, CV_PI/180, matH / 5);
//    Scalar color = Scalar(0,255,0);
//    drawDetectLines(outMat,lines, color);

    mat_to_bitmap(env, outMat, result);
    env->ReleaseByteArrayElements(yuvData_, yuvData, 0);
}

vector<Vec2f> checkLines(Mat &scr) {
    int houghThreshold = cvRound(max(scr.rows, scr.cols) * 0.45);
    vector<Vec2f>lines;//矢量结构lines用于存放得到的线段矢量集合
    HoughLines(scr, lines, 2, CV_PI / 180, houghThreshold);
    return lines;
}

extern "C"
JNIEXPORT jint JNICALL
Java_me_pqpo_smartcameralib_SmartScanner_scan(JNIEnv *env, jclass type, jbyteArray yuvData_,
                                              jint width, jint height, jint rotation, jint x,
                                              jint y, jint maskWidth, jint maskHeight,
                                              jint threshold) {
    jbyte *yuvData = env->GetByteArrayElements(yuvData_, NULL);
    Mat outMat;
    processMat(yuvData, outMat, width, height, rotation, x, y, maskWidth, maskHeight, 0.3);
    threshold = cvRound(threshold * 0.3f);
    int matH = outMat.rows;
    int matW = outMat.cols;
    Rect rect(0, 0, threshold, matH);
    Mat croppedMatL = outMat(rect);
    rect.x = 0;
    rect.y = 0;
    rect.width = matW;
    rect.height = threshold;
    Mat croppedMatT = outMat(rect);
    rect.x = matW - threshold;
    rect.y = 0;
    rect.width = threshold;
    rect.height = matH;
    Mat croppedMatR = outMat(rect);
    rect.x = 0;
    rect.y = matH - threshold;
    rect.width = matW;
    rect.height = threshold;
    Mat croppedMatB = outMat(rect);

    env->ReleaseByteArrayElements(yuvData_, yuvData, 0);
    if(checkLines(croppedMatL).size() > 0 && checkLines(croppedMatT).size() && checkLines(croppedMatR).size() && checkLines(croppedMatB).size()) {
        return 1;
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