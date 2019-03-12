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

/**
 * The set of constants to use when sending Barcode Scanner an Intent which requests a barcode
 * to be encoded.
 *
 * @author dswitkin@google.com (Daniel Switkin)
 */
public final class Contents {
    private Contents() {
    }

    /**
     * Contains type constants used when sending Intents.
     */
    public static final class Type {
        /**
         * Plain text. Use Intent.putExtra(DATA, string). This can be used for URLs too, but string
         * must include "http://" or "https://".
         */
        public static final String TEXT = "TEXT_TYPE";
    }

    public static final class Status {
        public static final int  DECODE = 1000;
        public static final int  DECODE_SUCCEEDED = 1001;
        public static final int  DECODE_FAILED = 1002;
        public static final int  QUIT = 1004;
        public static final int  RESTART_PREVIEW = 1005;
    }
}
