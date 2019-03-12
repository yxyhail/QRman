/*
 * Copyright (C) 2014 - 2019  Beijing Spruce World Information Technology Co., Ltd. All rights reserved.
 *
 * Project: colonel_tool
 * Module: app
 * File: PermissionUtil.java
 * Class:PermissionUtil
 *
 * LastModified: 2019.01.08 20:23
 * Author: yangxinyu
 */

package com.yxyhail.qrman.utils;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;

import com.yanzhenjie.permission.Action;
import com.yanzhenjie.permission.AndPermission;

import java.util.ArrayList;
import java.util.List;


/**
 * 权限申请工具类
 *
 * @author yangxinyu
 * @date: 2018.12.10 14:02:46
 * @version: V1.0
 */
public class PermissionUtil {

    public static final int TYPE_RESPONSE_ALL_GRANTED = 0;
    public static final int TYPE_STORAGE = 1;
    public static final int TYPE_CAMERA = 2;

    private Activity activity;
    private List<Integer> typeList = new ArrayList<>();

    public PermissionUtil(Activity activity) {
        this.activity = activity;
    }


    /**
     * 设置请求类型
     *
     * @param types 请求权限的类型  {@link #TYPE_CAMERA} {@link #TYPE_CAMERA}
     * @return {@link #PermissionUtil}
     * @author yangxinyu
     * @date: 2018.12.10 17:30:32
     */
    public PermissionUtil addPermission(int... types) {
        for (int type : types) {
            if (!hasPermission(type)) typeList.add(type);
        }
        return this;
    }


    /**
     * 请求权限
     *
     * @param granted 用户同意权限时调用 {@link PermissionGranted}
     * @return {@link #PermissionUtil}
     * @author yangxinyu
     * @date: 2018.12.10 17:31:34
     */
    public PermissionUtil request(@NonNull PermissionGranted granted) {
        return request(granted, null);
    }

    /**
     * 请求权限
     *
     * @param granted 用户同意权限时调用 {@link PermissionGranted}
     * @param denied  用户拒绝权限时调用 {@link PermissionDenied}
     *                此参数可为null 默认不处理任何用户拒绝后的逻辑
     * @return {@link #PermissionUtil}
     * @author yangxinyu
     * @date: 2018.12.10 17:31:34
     */
    public PermissionUtil request(@NonNull final PermissionGranted granted, final PermissionDenied denied) {
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

    /**
     * 是否有权限判读
     *
     * @param type 请求权限的类型  {@link #TYPE_CAMERA} {@link #TYPE_CAMERA}
     * @return boolean
     * @author yangxinyu
     * @date: 2018.12.10 14:28:51
     */
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

    /**
     * 获取真正权限类型
     *
     * @param type 请求权限的类型  {@link #TYPE_CAMERA} {@link #TYPE_CAMERA}
     * @return String[]
     * @author yangxinyu
     * @date: 2018.12.11 20:13:25
     */
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

    /**
     * 检查权限
     *
     * @param permission 权限内容
     * @return boolean
     * @author yangxinyu
     * @date: 2018.12.12 14:28:58
     */
    private boolean checkPermission(String permission) {
        return ContextCompat.checkSelfPermission(activity, permission)
                == PackageManager.PERMISSION_GRANTED;
    }


    /**
     * 用户同意权限
     *
     * @author yangxinyu
     * @date: 2018.12.11 19:51:18
     * @version: V1.0
     */
    public interface PermissionGranted {
        void onGranted(int requestType);
    }

    /**
     * 用户拒绝权限
     *
     * @author yangxinyu
     * @date: 2018.12.11 19:51:36
     * @version: V1.0
     */
    public interface PermissionDenied {
        void onDenied(int requestType);
    }
}
