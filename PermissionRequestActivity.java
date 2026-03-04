package com.brt.ibridge;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.List;

/**
 * 透明权限请求Activity：Service通过启动该Activity触发权限请求
 * 无布局，请求完成后立即销毁
 */
public class PermissionRequestActivity extends Activity {
    private static final String TAG = "PermissionRequestAct";
    public static final int PERMISSION_REQUEST_CODE = 1001;
    // 蓝牙+定位核心权限（适配Android 12+）
    private final String[] PERMISSIONS_BLE_CORE = {
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_LOCATION_EXTRA_COMMANDS,
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.BLUETOOTH_ADVERTISE
    };
    // 低版本Android（<12）仅需定位权限
    private final String[] PERMISSIONS_LOW_VERSION = {
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_LOCATION_EXTRA_COMMANDS
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // 无setContentView，透明Activity
        checkAndRequestPermissions();
    }

    /**
     * 检测并请求权限
     */
    private void checkAndRequestPermissions() {
        List<String> needPerms = new ArrayList<>();
        // 区分Android版本，收集缺失的权限
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            for (String perm : PERMISSIONS_LOW_VERSION) {
                if (ContextCompat.checkSelfPermission(this, perm) != PackageManager.PERMISSION_GRANTED) {
                    needPerms.add(perm);
                }
            }
        } else {
            for (String perm : PERMISSIONS_BLE_CORE) {
                if (ContextCompat.checkSelfPermission(this, perm) != PackageManager.PERMISSION_GRANTED) {
                    needPerms.add(perm);
                }
            }
        }

        if (needPerms.isEmpty()) {
            // 权限已授予，发送广播通知Service
            sendPermissionResultBroadcast(true);
            finish(); // 销毁Activity
            return;
        }
        // 触发权限请求弹窗
        ActivityCompat.requestPermissions(this, needPerms.toArray(new String[0]), PERMISSION_REQUEST_CODE);
    }

    /**
     * 权限请求结果回调
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            boolean allGranted = true;
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    allGranted = false;
                    break;
                }
            }
            // 发送广播，将权限结果通知给TestService
            sendPermissionResultBroadcast(allGranted);
        }
        finish(); // 无论是否授予，都销毁Activity
    }

    /**
     * 发送广播：通知Service权限请求结果
     * @param isAllGranted 是否全部授予
     */
    private void sendPermissionResultBroadcast(boolean isAllGranted) {
        Intent intent = new Intent();
        intent.setAction("com.brt.ibridge.PERMISSION_RESULT_ACTION"); // 广播Action，需与Service一致
        intent.putExtra("is_permission_granted", isAllGranted);
        sendBroadcast(intent);
        Log.d(TAG, "权限请求结果已广播：" + (isAllGranted ? "全部授予" : "部分/全部拒绝"));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "透明权限Activity已销毁");
    }
}
