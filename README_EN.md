![](art/smartcamera_banner.png)

**SmartCamera** is an Android camera extension library，Provides a highly customizable real-time scanning module that captures and recognizes whether the object's border inside the camera matches the specified area in real time. If you like, welcome star, fork。

The language is slightly simplistic, and the specific functions are as shown in the figure below. It is suitable for scanning, automatic shooting and cutting of ID cards, business cards, documents, etc.

[SmartCamera-Sample-debug.apk](art/SmartCamera-Sample-debug.apk)

![](art/smartcamera_demo.gif)

In the camera implementation, SmartCamera references Google's open source [CameraView](https://github.com/google/cameraview) in source code and modified to support the Camera.PreviewCallback callback to get the camera preview stream.

SmartCameraView extends from the modified CameraView, adding a mask view (MaskView) and a real-time scanning module (SmartScanner). The mask view is the layer of marquee on the camera as you can see, and it is equipped with a top-down scan effect. Of course, you can also implement the MaskViewImpl interface to customize the mask view.

**The real-time scanning module (SmartScanner) is the core function of the library. It can judge whether the content matches the box in real time with good performance.**。

You can also follow my other library [SmartCropper](https://github.com/pqpo/SmartCropper)： An easy-to-use smart picture cropping library for cropping of ID cards, business cards, documents, etc.

## Scanning algorithm tuning

In addition, SmartScanner provides a rich configuration, users can modify the scanning algorithm to obtain better adaptability.

![](art/smartscannerparams.jpg)

In order to more easily and efficiently optimize the algorithm, SmartScanner provides you with a scan preview mode.

After the preview function is enabled, you can use SmartScanner to get the result of each frame processing and output it to the ImageView to observe the result of the native layer scan in real time. The white line area is the result of edge detection, and the white line bold area is the recognized border.

![](art/smartcamera_frame1.jpg)

**Your goal is to make the content boundaries clearly visible and the identified borders (white bold segments) are accurate by adjusting the various parameters of the SmartScanner.**。

Note: SmartCamera performs performance and memory optimization in all aspects, but for unnecessary waste of performance resources, please close the preview mode after tuning the algorithm parameters.

## Import

1.Add it in your root build.gradle at the end of repositories:

```groovy
allprojects {
        repositories {
            ...
            maven { url 'https://jitpack.io' }
        }
}
```

2.Add the dependency

```groovy
dependencies {
      implementation 'com.github.pqpo:SmartCamera:v1.3.0'
}
```

Please pay attention ProGuard：

```
-keep class me.pqpo.smartcameralib.**{*;}
```

## Usage

### 1. Use the camera layout and launch the camera (start preview if necessary)

```xml
<me.pqpo.smartcameralib.SmartCameraView
        android:id="@+id/camera_view"
        android:layout_width="match_parent"
        android:layout_height="match_parent"/>
```

```java
@Override
protected void onResume() {
   super.onResume();
   mCameraView.start();
   mCameraView.startScan();
}


@Override
protected void onPause() {
   mCameraView.stop();
   super.onPause();
   if (alertDialog != null) {
       alertDialog.dismiss();
   }
   mCameraView.stopScan();
}
```

If the preview is turned on, don't forget to call the corresponding method to open and end the preview.

### 2. Modify the scan module parameters (optional, tuning algorithm, and press the preview mode in the fourth step)

	See Appendix 1 for the meaning of each parameter of the scanning module.

```java
private void initScannerParams() {
     SmartScanner.DEBUG = true;
     SmartScanner.detectionRatio = 0.1f;
     SmartScanner.checkMinLengthRatio = 0.8f;
     SmartScanner.cannyThreshold1 = 20;
     SmartScanner.cannyThreshold2 = 50;
     SmartScanner.houghLinesThreshold = 130;
     SmartScanner.houghLinesMinLineLength = 80;
     SmartScanner.houghLinesMaxLineGap = 10;
     SmartScanner.firstGaussianBlurRadius = 3;
     SmartScanner.secondGaussianBlurRadius = 3;
     SmartScanner.maxSize = 300;
     SmartScanner.angleThreshold = 5;
     // don't forget reload params
     SmartScanner.reloadParams();
}
```
Don't forget to notify the native layer to reload the parameters after modifying the parameters： SmartScanner.reloadParams();

### 3. Configure the mask view (optional, to modify the default view, or to modify the marquee area)

	The meaning of configuring each method of MaskView is shown in Appendix II.
	

```java
final MaskView maskView = (MaskView) mCameraView.getMaskView();;
maskView.setMaskLineColor(0xff00adb5);
maskView.setShowScanLine(true);
maskView.setScanLineGradient(0xff00adb5, 0x0000adb5);
maskView.setMaskLineWidth(2);
maskView.setMaskRadius(5);
maskView.setScanSpeed(6);
maskView.setScanGradientSpread(80);
mCameraView.post(new Runnable() {
      @Override
      public void run() {
          int width = mCameraView.getWidth();
          int height = mCameraView.getHeight();
          if (width < height) {
              maskView.setMaskSize((int) (width * 0.6f), (int) (width * 0.6f / 0.63));
              maskView.setMaskOffset(0, -(int)(width * 0.1));
           } else {
              maskView.setMaskSize((int) (width * 0.6f), (int) (width * 0.6f * 0.63));
           }
      }
});
mCameraView.setMaskView(maskView);
        
```

### 4. Configure SmartCameraView

#### 1. Start preview：

```java 
mCameraView.getSmartScanner().setPreview(true);
mCameraView.setOnScanResultListener(new SmartCameraView.OnScanResultListener() {
      @Override
      public boolean onScanResult(SmartCameraView smartCameraView, int result) {
          Bitmap previewBitmap = smartCameraView.getPreviewBitmap();
          if (previewBitmap != null) {
             ivPreview.setImageBitmap(previewBitmap);
          }
          return false;
      }
});
```
Preview mode is turned on by the first sentence code.
You can set the callback by setOnScanResultListener to get the scan result of each frame，**result == 1 means the recognition result matches the border**
If the preview mode is turned on, you can use the smartCameraView.getPreviewBitmap() method in the callback to get the result of each frame processing.
A return value of false means that the scan result is not intercepted. At this time, SmartCameraView will automatically trigger the photo when the result is 1, if you have processed the scan result, it returns true.


#### 2. Get the photo results and crop the marquee area：

```java
mCameraView.addCallback(new CameraView.Callback() {
     @Override
     public void onPictureTaken(CameraView cameraView, byte[] data) {
          super.onPictureTaken(cameraView, data);
          // crop the image
          mCameraView.cropImage(data, new SmartCameraView.CropCallback() {
              @Override
              public void onCropped(Bitmap cropBitmap) {
                  if (cropBitmap != null) {
                      showPicture(cropBitmap);
                  }
              }
       	);
   }
});
```

## Appendix

### 1. Scan module (SmartScanner) recognition algorithm parameter introduction：

|参数名|默认值|备注|
|:---:|:---:|:--:|
|maxSize|300|为了提高性能，检测的图片会缩小到该尺寸之内，设置太小的话会影响检测效果|
|detectionRatio|0.1|检测范围比例，比例越小表示待检测物体需要更靠近边框才能检测通过|
|checkMinLengthRatio|0.8|线段最小长度检测比例,例如: 靠近上边框检测出一条线段长度为： checkLength, 上边框总宽度为：width, 那么如果 checkLength > width * checkMinLengthRatio 则 该线段符合检测条件，认为该线段为被检测物体上边框|
|angleThreshold|5|检测角度阈值，实际检测时会将夹角收敛到第一象限，若检测出的线段与坐标轴夹角小于该值则认为边框水平或者垂直，检测通过。|
| firstGaussianBlurRadius |3| 第一次高斯模糊半径，用于消除噪点，必须为正奇数，针对的是原图|
| secondGaussianBlurRadius |3| 第二次高斯模糊半径，用于消除噪点，必须为正奇数，针对的是灰度图|
| cannyThreshold1 |20|canny 算符阈值1|
| cannyThreshold2 |50|canny 算符阈值2，低于阈值1的像素点会被认为不是边缘， 高于阈值2的像素点会被认为是边缘， 在阈值1和阈值2之间的像素点,若与第2步得到的边缘像素点相邻，则被认为是边缘，否则被认为不是边缘。大小比例推荐2到3倍。用于调节使得边框清晰可见，同时减少干扰。|
| houghLinesThreshold |130| 最小投票数，要检测一条直线所需最少的的曲线交点，增大该值会减少检测出的线段数量。|
| houghLinesMinLineLength |80|能组成一条直线的最少点的数量, 点数量不足的直线将被抛弃。|
| houghLinesMaxLineGap | 10 |能被认为在一条直线上的点的最大距离，若出现较多断断续续的线段可以适当增大该值。|

### 2. Mask view (MaskView) methods:
|方法名|备注|
|:---:|:---:|
|setShowScanLine|设置是否显示扫描动画|
|setMaskLineWidth|设置中间选框线的宽度|
|setMaskLineColor|设置中间选框的颜色|
|setMaskRadius|设置中间选框圆角弧度|
|setMaskSize|设置选框的大小，默认居中|
|setMaskOffset|用于调整选框的位置|
|setMaskAlpha|设置选框区域外蒙版的透明度|
|setScanLineGradient|设置扫描线的渐变颜色|
|setScanGradientSpread|设置扫描线渐变的高度|
|setScanSpeed|设置扫描线移动速度|


## Thanks

- [Google/CameraView](https://github.com/google/cameraview)

---

## About Me：

- Email：    pqponet@gmail.com
- GitHub：  [pqpo](https://github.com/pqpo)
- Blog：    [pqpo's notes](https://pqpo.me)
- Twitter: [Pqponet](https://twitter.com/Pqponet)
- WeChat: pqpo_me

<img src="art/qrcode_for_gh.jpg" width="200">

## License

    Copyright 2017 pqpo
    
    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at
    
       http://www.apache.org/licenses/LICENSE-2.0
    
    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.
