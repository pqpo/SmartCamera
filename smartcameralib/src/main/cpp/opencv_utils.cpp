//
// Created by admin on 2018/8/17.
//

#include <opencv_utils.h>

using namespace cv;
using namespace std;

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

void drawLines(Mat &src, vector<Vec4i> &lines, int offsetX, int offsetY) {
    for( size_t i = 0; i < lines.size(); i++ ) {
        Vec4i l = lines[i];
        line(src, Point(l[0] + offsetX, l[1] + offsetY), Point(l[2] + offsetX, l[3] + offsetY), Scalar(255), 4, LINE_AA);
    }
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