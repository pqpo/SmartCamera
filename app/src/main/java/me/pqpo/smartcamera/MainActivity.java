package me.pqpo.smartcamera;

import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Build;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;

import com.google.android.cameraview.CameraView;

import me.pqpo.smartcameralib.MaskView;
import me.pqpo.smartcameralib.SmartCameraView;
import me.pqpo.smartcameralib.SmartScanner;


public class MainActivity extends AppCompatActivity {

    private SmartCameraView mCameraView;
    private ImageView imageView;

    protected void onCreate(Bundle savedInstanceState) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = getWindow();
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            window.getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION);
            window.setStatusBarColor(Color.TRANSPARENT);
        }
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mCameraView = findViewById(R.id.sample_text);
        imageView = findViewById(R.id.image);

        imageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mCameraView.takePicture();
                mCameraView.stopScan();
            }
        });

        initMaskView();
        initCameraView();
        initScannerParams();
    }

    private void initScannerParams() {
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
        // don't forget reload params
        SmartScanner.reloadParams();
    }

    private void initCameraView() {
        mCameraView.getSmartScanner().setPreview(true);
        mCameraView.setOnScanResultListener(new SmartCameraView.OnScanResultListener() {
            @Override
            public boolean onScanResult(SmartCameraView smartCameraView, int result) {
                Bitmap previewBitmap = smartCameraView.getPreviewBitmap();
                if (previewBitmap != null) {
                    imageView.setImageBitmap(previewBitmap);
                }
                return false;
            }
        });

        mCameraView.addCallback(new CameraView.Callback() {

            @Override
            public void onPictureTaken(CameraView cameraView, byte[] data) {
                super.onPictureTaken(cameraView, data);
                mCameraView.cropImage(data, new SmartCameraView.CropCallback() {
                    @Override
                    public void onCropped(Bitmap cropBitmap) {
                        if (cropBitmap != null) {
                            showPicture(cropBitmap);
                        }
                    }
                });
            }

        });
    }

    private void initMaskView() {
        final MaskView maskView = new MaskView(this);
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
    }

    private void showPicture(Bitmap bitmap) {
        ImageView imageView = new ImageView(this);
        imageView.setImageBitmap(bitmap);
        AlertDialog alertDialog = new AlertDialog.Builder(this).setView(imageView).create();
        alertDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                mCameraView.startScan();
            }
        });
        Window window = alertDialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawableResource(R.color.colorTrans);
        }
        alertDialog.show();
    }

    protected void onResume() {
        super.onResume();
        mCameraView.start();
    }


    protected void onPause() {
        mCameraView.stop();
        super.onPause();
    }
}
