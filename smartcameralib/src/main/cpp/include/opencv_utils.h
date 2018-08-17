//
// Created by admin on 2018/8/17.
//

#ifndef SMARTCAMERA_OPENCV_UTILS_H
#define SMARTCAMERA_OPENCV_UTILS_H

#include <opencv2/opencv.hpp>
using namespace cv;

void drawLines(Mat &src, vector<Vec2f> &lines);
vector<Vec2f> checkLines(Mat &scr, int houghThreshold);
vector<Point> findMaxContours(Mat &src);
void matRotateClockWise90(Mat &src);
void matRotateClockWise180(Mat &src);
void matRotateClockWise270(Mat &src);


#endif //SMARTCAMERA_OPENCV_UTILS_H
