package me.pqpo.smartcamera;

import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Rect;
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

    private SmartCameraView mCameraView;
    private ImageView imageView;
    private boolean preview = true;
    private Bitmap previewBitmap;

    protected void onCreate(Bundle savedInstanceState) {
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

        if (preview) {
            imageView.setVisibility(View.VISIBLE);
            mCameraView.stopScan();
        }
        mCameraView.addCallback(new CameraView.Callback() {

            @Override
            public void onPictureTaken(CameraView cameraView, byte[] data) {
                super.onPictureTaken(cameraView, data);
                Bitmap bitmap = BitmapFactory.decodeByteArray(data, 0, data.length);
                Rect revisedMaskRect = mCameraView.getAdjustPictureMaskRect();
                if (revisedMaskRect != null) {
                    bitmap = Bitmap.createBitmap(bitmap, revisedMaskRect.left, revisedMaskRect.top, revisedMaskRect.width(), revisedMaskRect.height());
                }
                showPicture(bitmap);

//                Size pictureSize = cameraView.getPictureSize();
//                int previewRotation = cameraView.getPreviewRotation();
//                if (pictureSize != null && (previewRotation == 90 || previewRotation == 270)) {
//                    pictureSize = new Size(pictureSize.getHeight(), pictureSize.getWidth());
//                }
//                if (pictureSize != null) {
//                    Bitmap outBitmap = Bitmap.createBitmap(revisedMaskRect.width(), revisedMaskRect.height(), Bitmap.Config.ARGB_8888);
//                    SmartScanner.cropMask(data, pictureSize.getWidth(), pictureSize.getHeight(),
//                            revisedMaskRect.left, revisedMaskRect.top, revisedMaskRect.width(), revisedMaskRect.height(), outBitmap);
//                    showPicture(outBitmap);
//                }
            }

            @Override
            public void onPicturePreview(CameraView cameraView, byte[] data) {
                super.onPicturePreview(cameraView, data);
                if (data == null || !preview) {
                    return;
                }
                int previewRotation = cameraView.getPreviewRotation();
                Size size = cameraView.getPreviewSize();
                Rect revisedMaskRect = mCameraView.getAdjustPreviewMaskRect();
                if (revisedMaskRect != null && size != null) {
                    float scaleRatio = 0.2f;
                    if (previewBitmap == null) {
                        previewBitmap = Bitmap.createBitmap(Math.round(scaleRatio * revisedMaskRect.width()),
                                Math.round(scaleRatio * revisedMaskRect.height()), Bitmap.Config.ARGB_8888);
                    }
                    int result = SmartScanner.previewScan(data, size.getWidth(), size.getHeight(), previewRotation,
                            revisedMaskRect.left, revisedMaskRect.top, revisedMaskRect.width(), revisedMaskRect.height(),
                            previewBitmap, scaleRatio, 0.7f);
                    imageView.setImageBitmap(previewBitmap);
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
