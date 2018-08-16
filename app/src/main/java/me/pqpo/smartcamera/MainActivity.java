package me.pqpo.smartcamera;

import android.graphics.Bitmap;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.cameraview.CameraView;
import com.google.android.cameraview.Size;

import me.pqpo.smartcameralib.SmartCameraView;
import me.pqpo.smartcameralib.SmartScanner;


public class MainActivity extends AppCompatActivity {

    SmartCameraView cameraView;
    ImageView imageView;


    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        cameraView = findViewById(R.id.sample_text);
        imageView = findViewById(R.id.image);
        cameraView.addCallback(new CameraView.Callback() {

            @Override
            public void onPicturePreview(CameraView cameraView, byte[] data) {
                super.onPicturePreview(cameraView, data);
                if (data == null) {
                    return;
                }
                Size size = cameraView.getPreviewSize();
                if (size != null) {
                    Bitmap bitmap = Bitmap.createBitmap(size.getWidth(), size.getHeight(), Bitmap.Config.ARGB_8888);

                    SmartScanner.cropRect(data, size.getWidth(), size.getHeight(), 0,0,0,0, bitmap);
                    imageView.setImageBitmap(bitmap);
                }
            }
        });
    }


    protected void onResume() {
        super.onResume();
        cameraView.start();
    }


    protected void onPause() {
        cameraView.stop();
        super.onPause();
    }
}
