package com.yxyhail.qrman.example;

import android.graphics.Bitmap;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.yxyhail.qrman.qrcode.QRResult;
import com.yxyhail.qrman.qrcode.QRman;
import com.yxyhail.qrman.utils.Utils;

public class MainActivity extends AppCompatActivity implements QRman.QRCallback {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        new QRman(this);
    }

    @Override
    public void onScanSuccess(QRman qrman, QRResult rawResult, Bitmap barcode) {
        Utils.toast(MainActivity.this,rawResult.getText());
        qrman.restart();
    }
}
