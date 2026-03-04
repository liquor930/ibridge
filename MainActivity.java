package com.brt.ibridge;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.KeyEvent;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;

import com.brt.bluetooth.ibridge.BluetoothIBridgeAdapter;
import com.permissionx.guolindev.PermissionX;
import com.permissionx.guolindev.callback.ExplainReasonCallback;
import com.permissionx.guolindev.callback.RequestCallback;
import com.permissionx.guolindev.request.ExplainScope;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends Activity {
    private static final String TAG = MainActivity.class.getSimpleName();
    private ServiceBinder mBinder;
    private SwitcherView mSwitcherView;
    private final String[] permissions = new String[]{Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_LOCATION_EXTRA_COMMANDS};
    private final ArrayList<String> newPermissions = new ArrayList<>();

    private static final int SET_REQUEST_CODE = 100;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mSwitcherView = findViewById(R.id.switcher);
        mSwitcherView.setup(this);
        mBinder = new ServiceBinder(this);
        mBinder.registerBluetoothAdapterListener(serviceListener);

        //checkConnectionTriggered();
        //mBinder.doStartService(); //If user wants TestService always run in background, add this function here.
        mBinder.doBindService();
        initPermissions(this);
    }

    private void initPermissions(Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            if (lacksPermission(permissions)) {
                ActivityCompat.requestPermissions(this, permissions, SET_REQUEST_CODE);
            }
        } else {
            // 高版本(Android 12+)：修复核心逻辑
            newPermissions.clear(); // 先清空，避免重复添加
            // 1. 先添加蓝牙+定位核心权限，先判断是否缺失再添加
            String[] corePermissions = new String[]{
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.ACCESS_LOCATION_EXTRA_COMMANDS,
                    Manifest.permission.BLUETOOTH_SCAN,
                    Manifest.permission.BLUETOOTH_CONNECT,
                    Manifest.permission.BLUETOOTH_ADVERTISE
            };
            for (String p : corePermissions) {
                if (ContextCompat.checkSelfPermission(this, p) != PackageManager.PERMISSION_GRANTED) {
                    newPermissions.add(p);
                }
            }
            // 2. 再判断存储权限，缺失则添加
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                newPermissions.add(Manifest.permission.READ_EXTERNAL_STORAGE);
                newPermissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
            }
            // 3. 只要有任意权限缺失，就统一请求（解除嵌套，核心修复）
            if (!newPermissions.isEmpty()) {
                String[] requestPerms = newPermissions.toArray(new String[0]); // 简化数组转换
                Log.d("test", "权限请求数组len ：" + requestPerms.length);
                // 统一使用SET_REQUEST_CODE，方便后续处理结果
                ActivityCompat.requestPermissions(this, requestPerms, SET_REQUEST_CODE);
            }
        }
    }

    private boolean lacksPermission(String[] permissions) {
        for (String permission : permissions) {
            if (ContextCompat.checkSelfPermission(this.getApplicationContext(), permission) != PackageManager.PERMISSION_GRANTED) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case SET_REQUEST_CODE:
//                if (grantResults.length > 0) {
//                    for (int i = 0; i < grantResults.length; i++) {
//                        if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
//                            Log.i(TAG, "缺少权限");
//                        }
//                    }
//                }
                if (grantResults.length > 0) {
                    List<String> deniedPerms = new ArrayList<>();
                    // 收集所有被拒绝的权限
                    for (int i = 0; i < grantResults.length; i++) {
                        if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                            deniedPerms.add(permissions[i]);
                            Log.i(TAG, "缺少权限：" + permissions[i]);
                        }
                    }
                }
                break;
            default:
                break;
        }
    }

    /*
        private void checkConnectionTriggered() {
            Bundle b = getIntent().getExtras();
            if (b != null) {
                BluetoothIBridgeDevice dev = b
                        .getParcelable("com.brt.bleSpp.device");
                if (dev != null) {
                    if (mSwitcherView != null) {
                        mSwitcherView.onCreateAsDeviceConnected(dev);
                    }
                }
            }
        }
        */

    @Override
    protected void onDestroy() {
        mBinder.doUnbindService();
        mBinder.unregisterBluetoothAdapterListener(serviceListener);
        mSwitcherView.onDestroy();
        super.onDestroy();
    }


    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mSwitcherView.refreshScreen();
    }

    private final ServiceBinder.BluetoothAdapterListener serviceListener = new ServiceBinder.BluetoothAdapterListener() {

        @Override
        public void  onBluetoothAdapterDestroyed() {
            mSwitcherView.setBluetoothAdapter(null);
        }

        @Override
        public void onBluetoothAdapterCreated(BluetoothIBridgeAdapter adapter) {
            mSwitcherView.setBluetoothAdapter(adapter);
            //adapter.setScanMode(23,10000);
        }
    };

    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            if (!mSwitcherView.toggleView(SwitcherView.INDEX_OF_BACK, null)) {
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle(R.string.hint);
                builder.setMessage(R.string.quit_hint);
                builder.setPositiveButton(android.R.string.yes,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int arg0) {
//                                mBinder.getiBridgeAdapter().stopDiscovery();
                                BluetoothIBridgeAdapter.sharedInstance(null).stopDiscovery();;
                                finishAndRemoveTask();
                            }
                        }).setNegativeButton(android.R.string.no,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int arg0) {
                            }
                        });
                builder.show();
            }
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

}
