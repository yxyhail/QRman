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

import android.graphics.Bitmap;
import android.os.Bundle;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * This class does the work of decoding the user's request and extracting all the data
 * to be encoded in a barcode.
 *
 * @author dswitkin@google.com (Daniel Switkin)
 */
final class QRCodeEncoder {

    private static final String TAG = QRCodeEncoder.class.getSimpleName();

    private static final int WHITE = 0xFFFFFFFF;
    private static final int BLACK = 0xFF000000;

    private String contents;
    private String displayContents;
    private String title;
    private BarcodeFormat format;
    private final int dimension;
    private final boolean useVCard;

    QRCodeEncoder(String content, int dimension, boolean useVCard) {
        this.dimension = dimension;
        this.useVCard = useVCard;
        encodeContentsFromZXingIntent(content);
    }

    String getContents() {
        return contents;
    }

    String getDisplayContents() {
        return displayContents;
    }

    String getTitle() {
        return title;
    }

    boolean isUseVCard() {
        return useVCard;
    }

    // It would be nice if the string encoding lived in the core ZXing library,
    // but we use platform specific code like PhoneNumberUtils, so it can't.
    private void encodeContentsFromZXingIntent(String content) {
        // Default to QR_CODE if no format given.

        String formatString = BarcodeFormat.QR_CODE.toString();
        format = null;
        try {
            format = BarcodeFormat.valueOf(formatString);
        } catch (IllegalArgumentException iae) {
            // Ignore it then
        }
        if (format == null || format == BarcodeFormat.QR_CODE) {
            String type = Contents.Type.TEXT;
            this.format = BarcodeFormat.QR_CODE;
            encodeQRCodeContents(content, type);
        } else {
            if (content != null && !content.isEmpty()) {
                contents = content;
                displayContents = content;
                title = "纯文本";
            }
        }
    }

    // Handles send intents from multitude of Android application
    private void encodeQRCodeContents(String content, String type) {
        switch (type) {
            case Contents.Type.TEXT:
                if (content != null && !content.isEmpty()) {
                    contents = content;
                    displayContents = content;
                    title = "纯文本";
                }
                break;
        }
    }

    private static List<String> getAllBundleValues(Bundle bundle, String[] keys) {
        List<String> values = new ArrayList<>(keys.length);
        for (String key : keys) {
            Object value = bundle.get(key);
            values.add(value == null ? null : value.toString());
        }
        return values;
    }

    private static List<String> toList(String[] values) {
        return values == null ? null : Arrays.asList(values);
    }

    public Bitmap encodeAsBitmap() {
        String contentsToEncode = contents;
        if (contentsToEncode == null) {
            return null;
        }
        Map<EncodeHintType, Object> hints = null;
        String encoding = guessAppropriateEncoding(contentsToEncode);
        if (encoding != null) {
            hints = new EnumMap<>(EncodeHintType.class);
            hints.put(EncodeHintType.CHARACTER_SET, encoding);
        }
        BitMatrix result;
        try {
            result = new MultiFormatWriter().encode(contentsToEncode, format, dimension, dimension, hints);
        } catch (IllegalArgumentException iae) {
            // Unsupported format
            return null;
        } catch (WriterException e) {
            e.printStackTrace();
            return null;
        }
        int width = result.getWidth();
        int height = result.getHeight();
        int[] pixels = new int[width * height];
        for (int y = 0; y < height; y++) {
            int offset = y * width;
            for (int x = 0; x < width; x++) {
                pixels[offset + x] = result.get(x, y) ? BLACK : WHITE;
            }
        }

        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        bitmap.setPixels(pixels, 0, width, 0, 0, width, height);
        return bitmap;
    }

    private static String guessAppropriateEncoding(CharSequence contents) {
        // Very crude at the moment
        for (int i = 0; i < contents.length(); i++) {
            if (contents.charAt(i) > 0xFF) {
                return "UTF-8";
            }
        }
        return null;
    }

}
