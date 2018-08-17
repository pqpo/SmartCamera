package me.pqpo.smartcamera;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.RectF;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.cameraview.CameraView;
import com.google.android.cameraview.Size;

import me.pqpo.smartcameralib.SmartCameraView;
import me.pqpo.smartcameralib.SmartScanner;


public class MainActivity extends AppCompatActivity {

    SmartCameraView mCameraView;
    ImageView imageView;
    boolean preivew = true;


    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mCameraView = findViewById(R.id.sample_text);
        imageView = findViewById(R.id.image);
        mCameraView.addCallback(new CameraView.Callback() {

            @Override
            public void onPictureTaken(CameraView cameraView, byte[] data) {
                super.onPictureTaken(cameraView, data);
                Bitmap bitmap = BitmapFactory.decodeByteArray(data, 0, data.length);
                imageView.setImageBitmap(bitmap);
            }

            @Override
            public void onPicturePreview(CameraView cameraView, byte[] data) {
                super.onPicturePreview(cameraView, data);
                if (data == null || !preivew) {
                    return;
                }
                Size size = cameraView.getPreviewSize();
                if (size != null) {
                    int previewRotation = cameraView.getPreviewRotation();
                    RectF maskRect = mCameraView.getMaskRect();
                    float radio;
                    if (previewRotation == 90 || previewRotation == 270) {
                        radio = 1.0f * size.getHeight() / mCameraView.getWidth();
                    } else {
                        radio = 1.0f * size.getWidth() / mCameraView.getWidth();
                    }
                    int maskX = (int) ((int) maskRect.left * radio);
                    int maskY = (int) ((int) maskRect.top * radio);
                    int maskW = (int) ((int) maskRect.width() * radio);
                    int maskH = (int) ((int) maskRect.height() * radio);
                    float scaleRatio = 0.5f;
                    Bitmap bitmap = Bitmap.createBitmap(Math.round(scaleRatio * maskW), Math.round(scaleRatio * maskH), Bitmap.Config.ARGB_8888);
                    SmartScanner.cropRect(data, size.getWidth(), size.getHeight(), previewRotation, maskX,maskY,maskW,maskH, bitmap, scaleRatio);
                    imageView.setImageBitmap(bitmap);
                }
            }
        });
        imageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mCameraView.takePicture();
                preivew = false;
            }
        });
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
