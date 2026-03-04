package com.brt.ibridge;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.TextView;

import com.brt.bluetooth.ibridge.BluetoothIBridgeAdapter;
import com.brt.bluetooth.ibridge.BluetoothIBridgeDevice;


import androidx.annotation.Nullable;

/**
 * Created by dell on 2017/7/18.
 */

public class ReceiveSpeedTestActivity extends Activity implements BluetoothIBridgeAdapter.EventReceiver,
        BluetoothIBridgeAdapter.DataReceiver, View.OnClickListener {
    private static final String TAG = "RcvSpeedTestActivity";
    private BluetoothIBridgeAdapter mAdapter;
    private BluetoothIBridgeDevice mBluetoothDevice;
    public static final String DEVICE_OBJ = "ReceiveSpeedTestActivity.DeviceObj";
    private EditText mEditInterval;
    private Button mBtnMonitor;
    private MyThread monitorThread;
    private Button mBtnRcvClear;
    private TextView resultText;
    private TextView mTextTotal;
    private ScrollView scrollView;
    private static int MAX_LENGTH = 1024 * 10;
    private String text = "";
    private int logIndex = 0;
    private MyHandle myHandle;
    private long totalBytes;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        Log.i(TAG, "onCreate");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_rcv);
        mAdapter = BluetoothIBridgeAdapter.sharedInstance(null);
        if (mAdapter == null) {
            finish();
        }
        mBluetoothDevice = getIntent().getParcelableExtra(DEVICE_OBJ);
        mBtnMonitor = findViewById(R.id.monitor);
        mBtnRcvClear = findViewById(R.id.btnRcvClear);
        mEditInterval = findViewById(R.id.rcvInterval);
        mBtnMonitor.setOnClickListener(this);
        mBtnRcvClear.setOnClickListener(this);
        scrollView = findViewById(R.id.scorllView);
        resultText = findViewById(R.id.receiveData);
        mTextTotal = findViewById(R.id.totalBytes);
        monitorThread = null;
        mEditInterval.clearFocus();
        mAdapter.registerEventReceiver(this);
        mAdapter.registerDataReceiver(this);
        myHandle = new MyHandle();
    }

    @Override
    protected void onDestroy() {
        if (mAdapter != null) {
            mAdapter.unregisterEventReceiver(this);
            mAdapter.unregisterDataReceiver(this);
        }
        super.onDestroy();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            if (monitorThread != null) {
                monitorThread.closeThread();
            }
            finish();
        }
        return true;
    }

    private class MyHandle extends Handler {
        public static final int MESSAGE_REFRESH_RESULT = 1;
        public static final int MESSAGE_REFRESH_TOTAL = 2;

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MESSAGE_REFRESH_RESULT:
                    String result = (String) msg.obj;
                    setResultText(result, true);
                    mTextTotal.setText(Long.toString(totalBytes));
                    break;
                case MESSAGE_REFRESH_TOTAL:
                default:
                    break;
            }
        }
    }

    private class MyThread extends Thread {
        private long dataReceive = 0;
        private long intervalTime = 0;
        private boolean isClose = false;

        public MyThread() {
            intervalTime = Long.parseLong(mEditInterval.getText().toString());
        }

        @Override
        public void run() {
            while (!isClose) {
                startCountDataReceived();
                try {
                    sleep(intervalTime);
                } catch (Exception e) {

                }
                if (mBluetoothDevice.isConnected()) {
                    stopCountDataReceived();
                }
            }
            monitorThread = null;
        }

        public synchronized void closeThread() {
            isClose = true;
        }

        public synchronized void startCountDataReceived() {
            dataReceive = 0;
        }

        public synchronized void stopCountDataReceived() {
            double speed = 0.0;
            String result = "";
            try {
                if (dataReceive > 0) {
                    logIndex++;
                    speed = ((double) dataReceive) / ((double) intervalTime);
                    result = "[" + Integer.toString(logIndex) + "]" + "Rcv: " + Long.toString(dataReceive) + "bytes," + "Speed ";
                    result += String.format("%.2f", speed) + "kBps\r\n";
                    Message msg = myHandle.obtainMessage(MyHandle.MESSAGE_REFRESH_RESULT);
                    msg.obj = result;
                    myHandle.sendMessage(msg);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        public synchronized void countDataReceived(int dataLength) {
            dataReceive += dataLength;
        }
    }

    private void setResultText(String result, boolean append) {
        if (append) {
            if (text.length() < MAX_LENGTH) {
                text += result;
            } else {
                text = result;
            }
            resultText.setText(text);
            scrollView.scrollTo(0, resultText.getHeight());
        } else {
            resultText.setText(result);
            text = result;
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.monitor: {
                if (monitorThread == null) {
                    totalBytes = 0;
                    logIndex = 0;
                    monitorThread = new MyThread();
                    if (monitorThread != null) {
                        mEditInterval.setEnabled(false);
                        monitorThread.start();
                        mBtnMonitor.setText(getString(R.string.stop_monitor));
                    }
                } else {
                    if (monitorThread != null) {
                        monitorThread.closeThread();
                        monitorThread = null;
                    }
                    mEditInterval.setEnabled(true);
                    mBtnMonitor.setText(getString(R.string.start_monitor));
                }
                break;
            }
            case R.id.btnRcvClear: {
                setResultText("", false);
                break;
            }
            default:
                break;
        }
    }

    @Override
    public void onBluetoothOn() {
        Log.i(TAG, "onBluetoothOn");
    }

    @Override
    public void onBluetoothOff() {
        Log.i(TAG, "onBluetoothOff");

    }

    @Override
    public void onDiscoveryFinished() {
        Log.i(TAG, "onDiscoveryFinished");
    }

    @Override
    public void onDeviceBonding(BluetoothIBridgeDevice bluetoothIBridgeDevice) {
        Log.i(TAG, "onDeviceBonding");

    }

    @Override
    public void onDeviceBonded(BluetoothIBridgeDevice bluetoothIBridgeDevice) {
        Log.i(TAG, "onDeviceBonded");
    }

    @Override
    public void onDeviceBondNone(BluetoothIBridgeDevice bluetoothIBridgeDevice) {
        Log.i(TAG, "onDeviceBondNone");
    }

    @Override
    public void onDeviceFound(BluetoothIBridgeDevice bluetoothIBridgeDevice) {
        Log.i(TAG, "onDeviceFound");
    }

    @Override
    public void onDeviceConnected(BluetoothIBridgeDevice bluetoothIBridgeDevice) {
        Log.i(TAG, "onDeviceConnected");
    }

    @Override
    public void onDeviceDisconnected(BluetoothIBridgeDevice bluetoothIBridgeDevice, String s) {
        Log.i(TAG, "onDeviceDisconnected");
        if (mBluetoothDevice != null && mBluetoothDevice.equals(bluetoothIBridgeDevice)) {
            if (monitorThread != null) {
                monitorThread.closeThread();
                monitorThread = null;
            }
            this.finish();
        }
    }

    @Override
    public void onDeviceConnectFailed(BluetoothIBridgeDevice bluetoothIBridgeDevice, String s) {
        Log.i(TAG, "onDeviceConnectFailed");
    }

    @Override
    public void onWriteFailed(BluetoothIBridgeDevice bluetoothIBridgeDevice, String s) {
        Log.i(TAG, "onWriteFailed");
    }

    @Override
    public void onLeServiceDiscovered(BluetoothIBridgeDevice bluetoothIBridgeDevice, String s) {
        Log.i(TAG, "onLeServiceDiscovered");
    }

    @Override
    public void onDataReceived(BluetoothIBridgeDevice bluetoothIBridgeDevice, byte[] bytes, int i) {
        if (bluetoothIBridgeDevice != null && bluetoothIBridgeDevice.equals(mBluetoothDevice)) {
            if (monitorThread != null) {
                monitorThread.countDataReceived(i);
            }
            totalBytes += i;
        }
        Log.i(TAG, "onDataReceived");
    }
}
