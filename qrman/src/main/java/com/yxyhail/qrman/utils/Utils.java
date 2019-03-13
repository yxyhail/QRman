package com.yxyhail.qrman.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.widget.Toast;

import com.yxyhail.logger.BuildConfig;
import com.yxyhail.logger.LogFormatter;
import com.yxyhail.logger.Logger;

import java.io.ByteArrayOutputStream;

public class Utils {

    public static void toast(Context context, String content) {
        Toast.makeText(context, content, Toast.LENGTH_SHORT).show();
    }

    public static void log(String content) {
        LogFormatter logFormatter = LogFormatter.onBuilder()
                .isLogEnable(BuildConfig.DEBUG)
                .setGlobalTag("QRman")
                .extraMethodOffset(1).build();
        Logger.initFormatter(logFormatter);
        Logger.e(content);
    }


    public static byte[] bitmapToByteArr(Bitmap bitmap) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            int options = 100;
            bitmap.compress(Bitmap.CompressFormat.JPEG, options, baos);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return baos.toByteArray();
    }
}
