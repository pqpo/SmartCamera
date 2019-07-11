//
// Created by admin on 2018/8/17.
//

#ifndef SMARTCAMERA_OPENCV_UTILS_H
#define SMARTCAMERA_OPENCV_UTILS_H

#include <opencv2/opencv.hpp>
using namespace cv;
using namespace std;

void drawLines(Mat &src, vector<Vec4i> &lines, int offsetX, int offsetY);
void matRotateClockWise90(Mat &src);
void matRotateClockWise180(Mat &src);
void matRotateClockWise270(Mat &src);


#endif //SMARTCAMERA_OPENCV_UTILS_H
