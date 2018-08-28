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
import android.support.annotation.ColorInt;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;

/**
 * Created by pqpo on 2018/8/15.
 */
public class MaskView extends View implements MaskViewImpl{

    private Paint mMaskPaint;
    private PorterDuffXfermode porterDuffXfermode;
    private int radius = 20;
    private RectF maskRect = new RectF();
    private int maskAlpha = 0x99;
    private int maskOffsetX = 0;
    private int maskOffsetY = 0;
    private int maskWidth = -1;
    private int maskHeight = -1;
    private int maskLineColor = Color.WHITE;
    private int maskLineWidth = 1;

    private boolean showScanLine = true;

    private float currentScanningY = -1;
    private Bitmap scanGradientBitmap;
    private int scanGradientSpread = 100;
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
        ViewGroup.MarginLayoutParams lp = new ViewGroup.MarginLayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        setLayoutParams(lp);
        initMaskView();
    }

    public void setShowScanLine(boolean showScanLine) {
        this.showScanLine = showScanLine;
        currentScanningY = -1;
        scanGradientBitmap = null;
        postInvalidateOnAnimation();
    }

    public void setMaskLineWidth(int maskLineWidth) {
        this.maskLineWidth = maskLineWidth;
        postInvalidateOnAnimation();
    }

    public void setMaskRadius(int radius) {
        this.radius = radius;
        postInvalidateOnAnimation();
    }

    public void setMaskSize(int mWidth, int mHeight) {
        if (mWidth == maskWidth && mHeight == maskHeight) {
            return;
        }
        scanGradientBitmap = null;
        this.maskWidth = mWidth;
        this.maskHeight = mHeight;
        postInvalidateOnAnimation();
    }

    public void setMaskOffset(int maskOffsetX, int maskOffsetY) {
        if (this.maskOffsetX == maskOffsetX && this.maskOffsetY == maskOffsetY) {
            return;
        }
        scanGradientBitmap = null;
        this.maskOffsetX = maskOffsetX;
        this.maskOffsetY = maskOffsetY;
        postInvalidateOnAnimation();
    }

    public void setMaskAlpha(int maskAlpha) {
        this.maskAlpha = maskAlpha;
        postInvalidateOnAnimation();
    }

    public void setMaskLineColor(int maskLineColor) {
        this.maskLineColor = maskLineColor;
        postInvalidateOnAnimation();
    }

    public void setScanLineGradient(@ColorInt int startColor, @ColorInt int endColor) {
        if (this.scanStartColor == startColor && this.scanEndColor == endColor) {
            return;
        }
        this.scanStartColor = startColor;
        this.scanEndColor = endColor;
        scanGradientBitmap = null;
        postInvalidateOnAnimation();
    }

    public void setScanGradientSpread(int scanGradientSpread) {
        if (this.scanGradientSpread == scanGradientSpread) {
            return;
        }
        this.scanGradientSpread = scanGradientSpread;
        scanGradientBitmap = null;
        postInvalidateOnAnimation();
    }

    public void setScanSpeed(int scanSpeed) {
        this.scanSpeed = scanSpeed;
        postInvalidateOnAnimation();
    }

    @Override
    public View getMaskView() {
        return this;
    }

    @Override
    public RectF getMaskRect() {
        return maskRect;
    }

    private void initMaskView() {
        porterDuffXfermode = new PorterDuffXfermode(PorterDuff.Mode.CLEAR);
        mMaskPaint = new Paint();
        setBackgroundColor(Color.TRANSPARENT);
        mMaskPaint.setAntiAlias(true);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        drawMask(canvas);
        if (showScanLine) {
            drawScanningLine(canvas);
        }
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
        canvas.drawRoundRect(maskRect, radius, radius, mMaskPaint);
        mMaskPaint.setXfermode(null);
        canvas.restoreToCount(sc);
        mMaskPaint.setColor(maskLineColor);
        mMaskPaint.setStyle(Paint.Style.STROKE);
        mMaskPaint.setStrokeWidth(maskLineWidth);
        canvas.drawRoundRect(maskRect, radius, radius, mMaskPaint);
    }

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
        if (maskWidth <= 0 || maskHeight <= 0) {
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
        maskRect.left = leftRightOffset + maskOffsetX;
        maskRect.top = topBottomOffset + maskOffsetY;
        maskRect.right = maskRect.left + maskWidth;
        maskRect.bottom =  maskRect.top + maskHeight;
    }

}
