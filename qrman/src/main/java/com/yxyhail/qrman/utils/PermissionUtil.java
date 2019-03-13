package com.yxyhail.qrman.utils;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.os.Build;
import android.support.v4.content.ContextCompat;

import com.yanzhenjie.permission.Action;
import com.yanzhenjie.permission.AndPermission;

import java.util.ArrayList;
import java.util.List;



public class PermissionUtil {

    public static final int TYPE_RESPONSE_ALL_GRANTED = 0;
    public static final int TYPE_STORAGE = 1;
    public static final int TYPE_CAMERA = 2;

    private Activity activity;
    private List<Integer> typeList = new ArrayList<>();

    public PermissionUtil(Activity activity) {
        this.activity = activity;
    }



    public PermissionUtil addPermission(int... types) {
        for (int type : types) {
            if (!hasPermission(type)) typeList.add(type);
        }
        return this;
    }


    public PermissionUtil request(PermissionGranted granted) {
        return request(granted, null);
    }


    public PermissionUtil request(final PermissionGranted granted, final PermissionDenied denied) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (typeList.isEmpty()) {
                granted.onGranted(TYPE_RESPONSE_ALL_GRANTED);
            } else {
                activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        for (final int type : typeList) {
                            AndPermission.with(activity)
                                    .runtime()
                                    .permission(getPermission(type))
                                    .onGranted(new Action<List<String>>() {
                                        @Override
                                        public void onAction(List<String> data) {
                                            granted.onGranted(type);
                                        }

                                    })
                                    .onDenied(new Action<List<String>>() {
                                        @Override
                                        public void onAction(List<String> data) {
                                            denied.onDenied(type);
                                        }
                                    })
                                    .start();
                        }
                    }
                });
            }
        } else {
            granted.onGranted(TYPE_RESPONSE_ALL_GRANTED);
        }

        return this;
    }


    public boolean hasPermission(int type) {
        boolean result = false;
        switch (type) {
            case TYPE_STORAGE:
                result = checkPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE);
                break;
            case TYPE_CAMERA:
                result = checkPermission(Manifest.permission.CAMERA);
                break;
            default:
                break;
        }
        return result;
    }

    private String[] getPermission(int type) {
        List<String> permissionList = new ArrayList<>();
        switch (type) {
            case TYPE_STORAGE:
                permissionList.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
                permissionList.add(Manifest.permission.READ_EXTERNAL_STORAGE);
                break;
            case TYPE_CAMERA:
                permissionList.add(Manifest.permission.CAMERA);
                break;
            default:
                break;
        }
        return permissionList.toArray(new String[0]);
    }

    private boolean checkPermission(String permission) {
        return ContextCompat.checkSelfPermission(activity, permission)
                == PackageManager.PERMISSION_GRANTED;
    }



    public interface PermissionGranted {
        void onGranted(int requestType);
    }


    public interface PermissionDenied {
        void onDenied(int requestType);
    }
}
