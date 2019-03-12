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

import android.os.Looper;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.DecodeHintType;
import com.google.zxing.ResultPointCallback;

import java.util.Collection;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

/**
 * This thread does all the heavy lifting of decoding the images.
 *
 * @author dswitkin@google.com (Daniel Switkin)
 */
final class DecodeThread extends Thread {

    public static final String BARCODE_BITMAP = "barcode_bitmap";
    public static final String BARCODE_SCALED_FACTOR = "barcode_scaled_factor";

    private final QRman qrman;
    private final Map<DecodeHintType, Object> hints;
    private DecodeHandler handler;
    private final CountDownLatch handlerInitLatch;

    DecodeThread(QRman qrman,
                 Collection<BarcodeFormat> decodeFormats,
                 Map<DecodeHintType, ?> baseHints,
                 String characterSet,
                 ResultPointCallback resultPointCallback) {

        this.qrman = qrman;
        handlerInitLatch = new CountDownLatch(1);

        hints = new EnumMap<>(DecodeHintType.class);
        if (baseHints != null) {
            hints.putAll(baseHints);
        }

        // The prefs can't change while the thread is running, so pick them up once here.
        if (decodeFormats == null || decodeFormats.isEmpty()) {
            decodeFormats = EnumSet.noneOf(BarcodeFormat.class);
            //条形商品码
            decodeFormats.addAll(DecodeFormatManager.PRODUCT_FORMATS);
            //条形工业码
            decodeFormats.addAll(DecodeFormatManager.INDUSTRIAL_FORMATS);
            //二维码
            decodeFormats.addAll(DecodeFormatManager.QR_CODE_FORMATS);
            //DATA_MATRIX  二维条码
            decodeFormats.addAll(DecodeFormatManager.DATA_MATRIX_FORMATS);
            //二维条码  AZTEC    PDF417
            decodeFormats.addAll(DecodeFormatManager.AZTEC_FORMATS);
            decodeFormats.addAll(DecodeFormatManager.PDF417_FORMATS);
        }
        hints.put(DecodeHintType.POSSIBLE_FORMATS, decodeFormats);

        if (characterSet != null) {
            hints.put(DecodeHintType.CHARACTER_SET, characterSet);
        }
        hints.put(DecodeHintType.NEED_RESULT_POINT_CALLBACK, resultPointCallback);
    }

    DecodeHandler getHandler() {
        try {
            handlerInitLatch.await();
        } catch (InterruptedException ie) {
            // continue?
        }
        return handler;
    }

    @Override
    public void run() {
        Looper.prepare();
        handler = new DecodeHandler(qrman, hints);
        handlerInitLatch.countDown();
        Looper.loop();
    }

}
