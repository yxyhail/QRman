/*
 * Copyright (C) 2010 ZXing authors
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

import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;

import com.google.zxing.BinaryBitmap;
import com.google.zxing.DecodeHintType;
import com.google.zxing.LuminanceSource;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.PlanarYUVLuminanceSource;
import com.google.zxing.RGBLuminanceSource;
import com.google.zxing.ReaderException;
import com.google.zxing.Result;
import com.google.zxing.common.HybridBinarizer;

import java.io.ByteArrayOutputStream;
import java.util.Map;

final class DecodeHandler extends Handler {

    private final QRman qrman;
    private final MultiFormatReader multiFormatReader;
    private boolean running = true;

    DecodeHandler(QRman qrman, Map<DecodeHintType, Object> hints) {
        multiFormatReader = new MultiFormatReader();
        multiFormatReader.setHints(hints);
        this.qrman = qrman;
    }

    @Override
    public void handleMessage(Message message) {
        if (message == null || !running) {
            return;
        }
        switch (message.what) {
            case Contents.Status.DECODE:
                decode((byte[]) message.obj, message.arg1, message.arg2);
                break;
            case Contents.Status.QUIT:
                running = false;
                Looper.myLooper().quit();
                break;
        }
    }

    /**
     * Decode the data within the viewfinder rectangle, and time how long it took. For efficiency,
     * reuse the same reader objects from one decode to the next.
     *
     * @param data   The YUV preview frame.
     * @param width  The width of the preview frame.
     * @param height The height of the preview frame.
     */
    public void decode(byte[] data, int width, int height) {
        try {
            Result rawResult = null;
            PlanarYUVLuminanceSource source = qrman.getCameraManager().buildLuminanceSource(data, width, height);
            if (source != null) {
                BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(source));
                try {
                    rawResult = multiFormatReader.decodeWithState(bitmap);
                } catch (ReaderException re) {
                    // continue
                } finally {
                    multiFormatReader.reset();
                }
            }
            sendScanResult(rawResult, bundleThumbnail(source, null));
        } catch (Exception e) {
            sendScanResult(null, null);
            e.printStackTrace();
        }

    }

    /**
     * 解析Bitmap二维码
     *
     * @param bitmap
     */
    public void decodeBitmap(Bitmap bitmap) {
        Result rawResult = null;
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        final int[] pixels = new int[width * height];
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height);
        RGBLuminanceSource luminanceSource = new RGBLuminanceSource(width, height, pixels);
        BinaryBitmap binaryBitmap = new BinaryBitmap(new HybridBinarizer(luminanceSource));
        try {
            rawResult = multiFormatReader.decodeWithState(binaryBitmap);
        } catch (ReaderException re) {
            // continue
        } finally {
            multiFormatReader.reset();
        }
        sendScanResult(rawResult, bundleThumbnail(luminanceSource, pixels));
    }

    private static Bundle bundleThumbnail(LuminanceSource source, int[] pixels) {
        if (source == null) {
            return null;
        }
        Bundle bundle = new Bundle();
        int width;
        int height;
        if (source instanceof PlanarYUVLuminanceSource) {
            PlanarYUVLuminanceSource pyuvSource = (PlanarYUVLuminanceSource) source;
            pixels = pyuvSource.renderThumbnail();
            width = pyuvSource.getThumbnailWidth();
            height = pyuvSource.getThumbnailHeight();
        } else {
            width = source.getWidth();
            height = source.getHeight();
        }

        Bitmap bitmap = Bitmap.createBitmap(pixels, 0, width, width, height, Bitmap.Config.ARGB_8888);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 50, out);
        bundle.putByteArray(DecodeThread.BARCODE_BITMAP, out.toByteArray());
        bundle.putFloat(DecodeThread.BARCODE_SCALED_FACTOR, (float) width / source.getWidth());
        return bundle;
    }

    /**
     * 发送扫描后的结果
     *
     * @param rawResult
     */
    private void sendScanResult(Result rawResult, Bundle bundle) {
        Handler handler = qrman.getHandler();
        if (rawResult != null) {
            // Don't log the barcode contents for security.
            if (handler != null) {
                Message message = Message.obtain(handler, Contents.Status.DECODE_SUCCEEDED, rawResult);
                message.setData(bundle);
                message.sendToTarget();
            }
        } else {
            if (handler != null) {
                Message message = Message.obtain(handler, Contents.Status.DECODE_FAILED);
                message.sendToTarget();
            }
        }
    }

}
