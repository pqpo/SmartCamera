package me.pqpo.smartcameralib;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.RectF;
import android.graphics.Shader;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.View;

/**
 * Created by pqpo on 2018/8/15.
 */
public class MaskView extends View {

    private Paint mMaskPaint;
    private Paint mScanLinePaint;
    private PorterDuffXfermode porterDuffXfermode;
    private int radius = 20;
    private RectF maskRect = new RectF();
    private int maskAlpha = 0xaa;
    private int maskWidth = -1;
    private int maskHeight = -1;
    private int maskLineColor = Color.WHITE;

    private Bitmap scanGradientBitmap;
    private int scanGradientSpread = 80;
    private int scanSpeed = 10;
    private int scanStartColor = 0xFFFFFFFF;
    private int scanEndColor = 0x00FFFFFF;

    public MaskView(@NonNull Context context) {
        this(context, null);
    }

    public MaskView(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public MaskView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initMaskView();
    }

    public RectF getMaskRect() {
        return maskRect;
    }

    private void initMaskView() {
        porterDuffXfermode = new PorterDuffXfermode(PorterDuff.Mode.CLEAR);
        mMaskPaint = new Paint();
        setBackgroundColor(Color.TRANSPARENT);
        mMaskPaint.setAntiAlias(true);
        mScanLinePaint = new Paint();
        mScanLinePaint.setColor(Color.WHITE);
        mScanLinePaint.setAntiAlias(true);
        mScanLinePaint.setStyle(Paint.Style.FILL);
        mScanLinePaint.setStrokeWidth(2);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        drawMask(canvas);
        drawScanningLine(canvas);
    }

    private void drawMask(Canvas canvas) {
        int canvasHeight = canvas.getHeight();
        int canvasWidth = canvas.getWidth();
        initMaskSize(canvasWidth, canvasHeight);
        int sc = canvas.saveLayerAlpha(0, 0, canvasWidth, canvasHeight, maskAlpha, Canvas.ALL_SAVE_FLAG);
        mMaskPaint.setStyle(Paint.Style.FILL);
        mMaskPaint.setColor(Color.BLACK);
        canvas.drawRect(0,0, canvasWidth, canvasHeight, mMaskPaint);
        mMaskPaint.setXfermode(porterDuffXfermode);
        mMaskPaint.setColor(Color.BLACK);
        canvas.drawRoundRect(maskRect, radius, radius, mMaskPaint);
        mMaskPaint.setXfermode(null);
        canvas.restoreToCount(sc);
        mMaskPaint.setColor(maskLineColor);
        mMaskPaint.setStyle(Paint.Style.STROKE);
        mMaskPaint.setStrokeWidth(2);
        canvas.drawRoundRect(maskRect, radius, radius, mMaskPaint);
    }

    private float currentScanningY = -1;

    private void drawScanningLine(Canvas canvas) {
        if (currentScanningY == -1) {
            currentScanningY = maskRect.top;
        } else {
            currentScanningY += scanSpeed;
        }
        currentScanningY = Math.max(maskRect.top, Math.min(currentScanningY, maskRect.bottom));
        if (scanGradientBitmap == null) {
            scanGradientBitmap = createGradientBitmap();
        }
        canvas.drawBitmap(scanGradientBitmap, maskRect.left, currentScanningY - scanGradientSpread, null);
        if (currentScanningY >= maskRect.bottom) {
            currentScanningY = maskRect.top;
        }
        postInvalidateOnAnimation();
    }

    private @NonNull Bitmap createGradientBitmap() {
        Bitmap bitmap = Bitmap.createBitmap(maskWidth, scanGradientSpread, Bitmap.Config.ARGB_4444);
        Canvas canvas = new Canvas(bitmap);
        LinearGradient shader = new LinearGradient(0, scanGradientSpread,0,0,new int[] {scanStartColor,scanEndColor},null,Shader.TileMode.CLAMP);
        Paint paint = new Paint();
        paint.setShader(shader);
        canvas.drawRoundRect(new RectF(0, 0, maskWidth, scanGradientSpread), radius, radius, paint);
        return bitmap;
    }

    private void initMaskSize(int canvasWidth, int canvasHeight) {
        if (maskWidth == -1 || maskHeight == -1) {
            if (canvasWidth < canvasHeight) {
                maskWidth = (int) (1.0f * canvasWidth * 2 / 3);
                maskHeight = (int) (1.0f * maskWidth / 0.65);
            } else {
                maskHeight = (int) (1.0f * canvasHeight * 2 / 3);
                maskWidth = (int) (1.0f * maskHeight / 0.65);
            }
        }
        maskWidth = Math.min(canvasWidth, maskWidth);
        maskHeight = Math.min(canvasHeight, maskHeight);
        float leftRightOffset = 0.5f * (canvasWidth - maskWidth);
        float topBottomOffset = 0.5f * (canvasHeight - maskHeight);
        leftRightOffset = Math.max(0, leftRightOffset);
        topBottomOffset = Math.max(0, topBottomOffset);
        maskRect.left = leftRightOffset;
        maskRect.top = topBottomOffset;
        maskRect.right = leftRightOffset + maskWidth;
        maskRect.bottom = topBottomOffset + maskHeight;
    }

}
