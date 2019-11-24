package com.yxyhail.qrman.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.Log;
import android.widget.Toast;

import com.yxyhail.qrman.BuildConfig;

import java.io.ByteArrayOutputStream;

public class Utils {

    public static void toast(Context context, String content) {
        Toast.makeText(context, content, Toast.LENGTH_SHORT).show();
    }

    public static void log(String content) {
        if (BuildConfig.DEBUG) {
            Log.i("QRman", content);
        }
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
