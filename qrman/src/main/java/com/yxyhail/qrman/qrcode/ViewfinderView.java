/*
 * Copyright (C) 2008 ZXing authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.yxyhail.qrman.qrcode;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.util.AttributeSet;
import android.view.View;
import android.widget.Toast;

import com.google.zxing.ResultPoint;
import com.yxyhail.qrman.R;
import com.yxyhail.qrman.camera.CameraManager;

import java.util.ArrayList;
import java.util.List;

/**
 * This view is overlaid on top of the camera preview. It adds the viewfinder rectangle and partial
 * transparency outside it, as well as the laser scanner animation and result points.
 *
 * @author dswitkin@google.com (Daniel Switkin)
 */
public final class ViewfinderView extends View {

    private static final long ANIMATION_DELAY = 10L;
    private static final int CURRENT_POINT_OPACITY = 0xA0;
    private static final int MAX_RESULT_POINTS = 30;
    private static final int POINT_SIZE = 6;

    private CameraManager cameraManager;
    private final Paint paint;

    private Paint linePaint;
    private Paint traAnglePaint;
    private Paint textPaint;

    //    private final int maskColor = Color.parseColor("#60000000");                          //蒙在摄像头上面区域的半透明颜色
    private final int triAngleColor = Color.parseColor("#76EE00");                        //边角的颜色
    private final int lineColor = Color.parseColor("#FF0000");                            //中间线的颜色
    private final int textColor = Color.parseColor("#CCCCCC");                            //文字的颜色
    private final int triAngleLength = dp2px(20);                                         //每个角的点距离
    private final int triAngleWidth = dp2px(4);                                           //每个角的点宽度
    private final int textMarinTop = dp2px(30);                                           //文字距离识别框的距离
    private int lineOffsetCount = 0;

    private Bitmap resultBitmap;
    private final int maskColor;
    private final int resultColor;
    private final int resultPointColor;
    private List<ResultPoint> possibleResultPoints;
    private List<ResultPoint> lastPossibleResultPoints;

    // This constructor is used when the class is built from an XML resource.
    public ViewfinderView(Context context, AttributeSet attrs) {
        super(context, attrs);
        // Initialize these once for performance rather than calling them every time in onDraw().
        paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        Resources resources = getResources();
        maskColor = resources.getColor(R.color.viewfinder_mask);
        resultColor = resources.getColor(R.color.result_view);
        resultPointColor = resources.getColor(R.color.possible_result_points);
        possibleResultPoints = new ArrayList<>(10);
        lastPossibleResultPoints = null;

        traAnglePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        traAnglePaint.setColor(triAngleColor);
        traAnglePaint.setStrokeWidth(triAngleWidth);
        traAnglePaint.setStyle(Paint.Style.STROKE);

        linePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        linePaint.setColor(lineColor);

        textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setColor(textColor);
        textPaint.setTextSize(dp2px(14));
    }

    public void setCameraManager(CameraManager cameraManager) {
        this.cameraManager = cameraManager;
    }

    @SuppressLint("DrawAllocation")
    @Override
    public void onDraw(Canvas canvas) {
        if (cameraManager == null) {
            return; // not ready yet, early draw before done configuring
        }
        Rect frame = cameraManager.getFramingRect();
        Rect previewFrame = cameraManager.getFramingRectInPreview();
        if (frame == null || previewFrame == null) {
            return;
        }
        int width = canvas.getWidth();
        int height = canvas.getHeight();

        // Draw the exterior (i.e. outside the framing rect) darkened
        paint.setColor(resultBitmap != null ? resultColor : maskColor);
        canvas.drawRect(0, 0, width, frame.top, paint);
        canvas.drawRect(0, frame.top, frame.left, frame.bottom + 1, paint);
        canvas.drawRect(frame.right + 1, frame.top, width, frame.bottom + 1, paint);
        canvas.drawRect(0, frame.bottom + 1, width, height, paint);

        if (resultBitmap != null) {
            toast("resultBitmap");
            // Draw the opaque result bitmap over the scanning rectangle
            paint.setAlpha(CURRENT_POINT_OPACITY);
            canvas.drawBitmap(resultBitmap, null, frame, paint);
        } else {


            // 除了中间的识别区域，其他区域都将蒙上一层半透明的图层
//            canvas.drawRect(0, 0, width, frame.top, maskPaint);
//            canvas.drawRect(0, frame.top, frame.left, frame.bottom + 1, maskPaint);
//            canvas.drawRect(frame.right + 1, frame.top, width, frame.bottom + 1, maskPaint);
//            canvas.drawRect(0, frame.bottom + 1, width, height, maskPaint);

            String text = "将二维码放入框内，即可自动扫描";
            canvas.drawText(text, (width - textPaint.measureText(text)) / 2, frame.bottom + textMarinTop, textPaint);

            // 四个角落的三角
            Path leftTopPath = new Path();
            leftTopPath.moveTo(frame.left + triAngleLength, frame.top + triAngleWidth / 2.0f);
            leftTopPath.lineTo(frame.left + triAngleWidth / 2.0f, frame.top + triAngleWidth / 2.0f);
            leftTopPath.lineTo(frame.left + triAngleWidth / 2.0f, frame.top + triAngleLength);
            canvas.drawPath(leftTopPath, traAnglePaint);

            Path rightTopPath = new Path();
            rightTopPath.moveTo(frame.right - triAngleLength, frame.top + triAngleWidth / 2.0f);
            rightTopPath.lineTo(frame.right - triAngleWidth / 2.0f, frame.top + triAngleWidth / 2.0f);
            rightTopPath.lineTo(frame.right - triAngleWidth / 2.0f, frame.top + triAngleLength);
            canvas.drawPath(rightTopPath, traAnglePaint);

            Path leftBottomPath = new Path();
            leftBottomPath.moveTo(frame.left + triAngleWidth / 2.0f, frame.bottom - triAngleLength);
            leftBottomPath.lineTo(frame.left + triAngleWidth / 2.0f, frame.bottom - triAngleWidth / 2.0f);
            leftBottomPath.lineTo(frame.left + triAngleLength, frame.bottom - triAngleWidth / 2.0f);
            canvas.drawPath(leftBottomPath, traAnglePaint);

            Path rightBottomPath = new Path();
            rightBottomPath.moveTo(frame.right - triAngleLength, frame.bottom - triAngleWidth / 2.0f);
            rightBottomPath.lineTo(frame.right - triAngleWidth / 2.0f, frame.bottom - triAngleWidth / 2.0f);
            rightBottomPath.lineTo(frame.right - triAngleWidth / 2.0f, frame.bottom - triAngleLength);
            canvas.drawPath(rightBottomPath, traAnglePaint);

            //循环划线，从上到下
            if (lineOffsetCount > frame.bottom - frame.top - dp2px(10)) {
                lineOffsetCount = 0;
            } else {
                lineOffsetCount = lineOffsetCount + 6;
//            canvas.drawLine(frame.left, frame.top + lineOffsetCount, frame.right, frame.top + lineOffsetCount, linePaint);    //画一条红色的线
                Rect lineRect = new Rect();
                lineRect.left = frame.left;
                lineRect.top = frame.top + lineOffsetCount;
                lineRect.right = frame.right;
                lineRect.bottom = frame.top + dp2px(10) + lineOffsetCount;
                canvas.drawBitmap(((BitmapDrawable) (getResources().getDrawable(R.drawable.scanline))).getBitmap(), null, lineRect, linePaint);
            }

            float scaleX = frame.width() / (float) previewFrame.width();
            float scaleY = frame.height() / (float) previewFrame.height();

            List<ResultPoint> currentPossible = possibleResultPoints;
            List<ResultPoint> currentLast = lastPossibleResultPoints;
            int frameLeft = frame.left;
            int frameTop = frame.top;
            if (currentPossible.isEmpty()) {
                lastPossibleResultPoints = null;
            } else {
                possibleResultPoints = new ArrayList<>(10);
                lastPossibleResultPoints = currentPossible;
                paint.setAlpha(CURRENT_POINT_OPACITY);
                paint.setColor(resultPointColor);
                synchronized (currentPossible) {
                    for (ResultPoint point : currentPossible) {
                        canvas.drawCircle(frameLeft + (int) (point.getX() * scaleX),
                                frameTop + (int) (point.getY() * scaleY),
                                POINT_SIZE, paint);
                    }
                }
            }
            if (currentLast != null) {
                paint.setAlpha(CURRENT_POINT_OPACITY / 2);
                paint.setColor(resultPointColor);
                synchronized (currentLast) {
                    float radius = POINT_SIZE / 2.0f;
                    for (ResultPoint point : currentLast) {
                        canvas.drawCircle(frameLeft + (int) (point.getX() * scaleX),
                                frameTop + (int) (point.getY() * scaleY),
                                radius, paint);
                    }
                }
            }

            // Request another update at the animation interval, but only repaint the laser line,
            // not the entire viewfinder mask.
            postInvalidateDelayed(ANIMATION_DELAY,
                    frame.left - POINT_SIZE,
                    frame.top - POINT_SIZE,
                    frame.right + POINT_SIZE,
                    frame.bottom + POINT_SIZE);
        }
    }

    public void drawViewfinder() {
        Bitmap resultBitmap = this.resultBitmap;
        this.resultBitmap = null;
        if (resultBitmap != null) {
            resultBitmap.recycle();
        }
        invalidate();
    }

    /**
     * Draw a bitmap with the result points highlighted instead of the live scanning display.
     *
     * @param barcode An image of the decoded barcode.
     */
    public void drawResultBitmap(Bitmap barcode) {
        resultBitmap = barcode;
        invalidate();
    }

    public void addPossibleResultPoint(ResultPoint point) {
        List<ResultPoint> points = possibleResultPoints;
        synchronized (points) {
            points.add(point);
            int size = points.size();
            if (size > MAX_RESULT_POINTS) {
                // trim it
                points.subList(0, size - MAX_RESULT_POINTS / 2).clear();
            }
        }
    }

    private int dp2px(int dp) {
        float density = getContext().getResources().getDisplayMetrics().density;
        return (int) (dp * density + 0.5f);
    }

    public void toast(String content) {
        Toast.makeText(getContext(), content, Toast.LENGTH_SHORT).show();
    }

}
