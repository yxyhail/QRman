package com.yxyhail.qrman.utils;

/**
 * @author yangxinyu
 * @version V1.0
 * @date 2019.3.4 19:42:01
 */
public class UpdateLog {

    /*

     扫码失败可能是预览区域 getFramingRectInPreview 最终显示的位置内的二维码Bitmap
     不完整，致使无法完成decode,
     需要修改getFramingRectInPreview 内 Rect的值 方法

     而Camera有几个支持的分辨率 最终可能与手机屏幕的分辨率不同，而手机的分辨率可以通过
     Camera.getSupportedPreviewSizes 方法获取。Zxing 内的 CameraConfigurationUtils 内有
     findBestPreviewSizeValue 方法根据不同的形式获取 最佳预览分辨率，而ZxingTest内的
     CameraConfigurationManager.initFromCameraParameters 内获取

     因为最终的图像是横着的 但需要竖着的，致使 getFramingRectInPreview 内修改最终图像大小时
     加减的数值是与正常的Left Right Top Bottom 反着的
-----------------------------
     最终的图片 接近方形 ，接近 真正的二维码大小 ，但摄像头方向还是横着的，接下来就要调整方向问题

     修改图片方向可以试着从 OpenCameraInterface 内的初始化Camera时修改
     图片横着的原因也可能是生成Bitmap时就是横着的 也可以从这方面修改

---------------------------------------
      修改了预览方向 getFramingRectInPreview 与方框的方向后边 同样需要修改 图片数据方向

      接下来需要将扫描二维码的代码移动到新的项目内




















     */


}
