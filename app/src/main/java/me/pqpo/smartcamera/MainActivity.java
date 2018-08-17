package me.pqpo.smartcamera;

import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Rect;
import android.graphics.RectF;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;

import com.google.android.cameraview.CameraView;
import com.google.android.cameraview.Size;

import me.pqpo.smartcameralib.SmartCameraView;
import me.pqpo.smartcameralib.SmartScanner;


public class MainActivity extends AppCompatActivity {

    SmartCameraView mCameraView;
    ImageView imageView;
    boolean preview = false;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mCameraView = findViewById(R.id.sample_text);
        imageView = findViewById(R.id.image);
        if (preview) {
            imageView.setVisibility(View.VISIBLE);
            mCameraView.stopScan();
        }
        mCameraView.addCallback(new CameraView.Callback() {

            @Override
            public void onPictureTaken(CameraView cameraView, byte[] data) {
                super.onPictureTaken(cameraView, data);
                BitmapFactory.Options options = new BitmapFactory.Options();
                options.inSampleSize = 3;
                Bitmap bitmap = BitmapFactory.decodeByteArray(data, 0, data.length, options);
                showPicture(bitmap);
            }

            @Override
            public void onPicturePreview(CameraView cameraView, byte[] data) {
                super.onPicturePreview(cameraView, data);
                if (data == null || !preview) {
                    return;
                }
                int previewRotation = cameraView.getPreviewRotation();
                Size size = cameraView.getPreviewSize();
                Rect revisedMaskRect = mCameraView.getRevisedMaskRect();
                if (revisedMaskRect != null) {
                    float scaleRatio = 0.3f;
                    Bitmap bitmap = Bitmap.createBitmap(Math.round(scaleRatio * revisedMaskRect.width()),
                            Math.round(scaleRatio * revisedMaskRect.height()), Bitmap.Config.ARGB_8888);
                    int result = SmartScanner.cropRect(data, size.getWidth(), size.getHeight(), previewRotation,
                            revisedMaskRect.left, revisedMaskRect.top, revisedMaskRect.width(), revisedMaskRect.height(),
                            bitmap, scaleRatio);
                    imageView.setImageBitmap(bitmap);
                    if (result == 1) {
                        mCameraView.takePicture();
                        mCameraView.stopScan();
                    }
                }
            }
        });
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
