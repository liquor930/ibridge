package com.brt.ibridge;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.provider.Settings;
import android.content.Context;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;

import com.brt.bluetooth.ibridge.BluetoothIBridgeAdapter;
import com.brt.ibridge.service.ServiceBinder;
import com.brt.ibridge.ui.SwitcherView;
import com.brt.ibridge.util.ViewFinder;
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
            // 楂樼増鏈?Android 12+)锛氫慨澶嶆牳蹇冮€昏緫
            newPermissions.clear(); // 鍏堟竻绌猴紝閬垮厤閲嶅娣诲姞
            // 1. 鍏堟坊鍔犺摑鐗?瀹氫綅鏍稿績鏉冮檺锛屽厛鍒ゆ柇鏄惁缂哄け鍐嶆坊鍔?
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
            // 2. 鍐嶅垽鏂瓨鍌ㄦ潈闄愶紝缂哄け鍒欐坊鍔?
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                newPermissions.add(Manifest.permission.READ_EXTERNAL_STORAGE);
                newPermissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
            }
            // 3. 鍙鏈変换鎰忔潈闄愮己澶憋紝灏辩粺涓€璇锋眰锛堣В闄ゅ祵濂楋紝鏍稿績淇锛?
            if (!newPermissions.isEmpty()) {
                String[] requestPerms = newPermissions.toArray(new String[0]); // 绠€鍖栨暟缁勮浆鎹?
                Log.d("test", "鏉冮檺璇锋眰鏁扮粍len 锛? + requestPerms.length);
                // 缁熶竴浣跨敤SET_REQUEST_CODE锛屾柟渚垮悗缁鐞嗙粨鏋?
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
//                            Log.i(TAG, "缂哄皯鏉冮檺");
//                        }
//                    }
//                }
                if (grantResults.length > 0) {
                    List<String> deniedPerms = new ArrayList<>();
                    for (int i = 0; i < grantResults.length; i++) {
                        if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                            deniedPerms.add(permissions[i]);
                            Log.e(TAG, "缂哄皯鏉冮檺锛? + permissions[i]);
                        }
                    }
                    // 鏈夋潈闄愯鎷掔粷锛屽紩瀵肩敤鎴峰幓绯荤粺璁剧疆
                    if (!deniedPerms.isEmpty()) {
                        showPermissionSettingsDialog();
                    }
                }
                break;
            default:
                break;
        }
    }

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
                                BluetoothIBridgeAdapter.sharedInstance(null).stopDiscovery();
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

    /**
     * 鏉冮檺琚嫆缁濆悗寮曞鐢ㄦ埛鍘荤郴缁熻缃〉
     */
    private void showPermissionSettingsDialog() {
        new AlertDialog.Builder(this)
                .setTitle("闇€瑕佽摑鐗欐潈闄?)
                .setMessage("iBridge 闇€瑕佽摑鐗欏拰浣嶇疆鏉冮檺鎵嶈兘姝ｅ父宸ヤ綔銆傝鍦ㄧ郴缁熻缃腑鍏佽鐩稿叧鏉冮檺銆?)
                .setPositiveButton("鍘昏缃?, (dialog, which) -> {
                    Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                    intent.setData(Uri.parse("package:" + getPackageName()));
                    startActivity(intent);
                })
                .setNegativeButton("鍙栨秷", null)
                .show();
    }

}

