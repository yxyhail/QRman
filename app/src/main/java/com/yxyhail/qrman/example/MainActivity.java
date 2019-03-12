package com.yxyhail.qrman.example;

import android.graphics.Bitmap;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import com.yxyhail.qrman.qrcode.QRResult;
import com.yxyhail.qrman.qrcode.QRman;
import com.yxyhail.qrman.utils.Utils;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        QRman qRman = new QRman(this);
        qRman.toggleQrView(true, new QRman.QRCallback() {
            @Override
            public void onSuccess(QRResult qrResult, Bitmap barcode) {
                Utils.toast(MainActivity.this,qrResult.getText());
            }
        });
    }
}
