package com.brt.ibridge;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.brt.bluetooth.ibridge.BluetoothIBridgeAdapter;
import com.brt.bluetooth.ibridge.BluetoothIBridgeDevice;

/**
 * Created by qiuwenqing on 15/8/29.
 */
public class DescriptionView extends Screen implements BluetoothIBridgeAdapter.EventReceiver {
    private final static String TAG = "DescriptionView";
    private final static String BRT_GATT_SERVICE_UUID = "0000ff00-0000-1000-8000-00805f9b34fb";
    private final static String BRT_GATT_NOTIFY_CHARC_UUID = "0000ff01-0000-1000-8000-00805f9b34fb";
    private final static String BRT_GATT_WRITE_CHARC_UUID = "0000ff02-0000-1000-8000-00805f9b34fb";

    private final static String IVT_BRACELET_NOTIFY_CHARC_UUID = "0000ff01-0000-1000-8000-00805f9b34fb";

    private final static String ISSC_GATT_SERVICE_UUID = "49535343-FE7D-4AE5-8FA9-9FAFD205E455";
    private final static String ISSC_GATT_NOTIFY_CHARC_UUID = "49535343-1E4D-4BD9-BA61-23C647249616";
    private final static String ISSC_GATT_WRITE_CHARC_UUID = "49535343-8841-43F4-A8D4-ECBE34729BB3";

    private final static String BRT_I81_WIFI_SERVICE_UUID = "0000ff20-0000-1000-8000-00805f9b34fb";
    private final static String BRT_I81_WIFI_NOTIFY_CHARC_UUID = "0000ff21-0000-1000-8000-00805f9b34fb";
    private final static String BRT_I81_WIFI_WRITE_CHARC_UUID = "0000ff22-0000-1000-8000-00805f9b34fb";

    private final static String BRT_GATT_NOTIFY_CHARC_UUID_BRACELET = "0000ff01-0000-1000-8000-00805f9b34fb";

    private BluetoothIBridgeAdapter mAdapter;
    private BluetoothIBridgeDevice mSelectedDevice = null;
    private Button mNextButton;
    private Button mAncsButton;
    private Button mWifiConfigButton;
    private TextView mDescriptionText;
    private EditText mServiceUUIDEdit;
    private EditText mWriteUUIDEdit;
    private EditText mNotifyUUIDEdit;
    private EditText mMtuEdit;
    private TextView mMtuSetSupportedText;

    @SuppressLint("SetTextI18n")
    public DescriptionView(final Context context, View view) {
        super(context, view);

        mDescriptionText = (TextView) findViewById(R.id.description);
        mServiceUUIDEdit = (EditText) findViewById(R.id.serviceUUID);
        mWriteUUIDEdit = (EditText) findViewById(R.id.wrtieUUID);
        mNotifyUUIDEdit = (EditText) findViewById(R.id.notifyUUID);
        mMtuEdit = (EditText) findViewById(R.id.mtu);
        mMtuSetSupportedText = (TextView) findViewById(R.id.mtuSetSupported);

        if (Build.VERSION.SDK_INT >= 21) {
            mMtuSetSupportedText.setText("(增大MTU可以提高速度)");
        } else {
            mMtuSetSupportedText.setText("(不支持)");
        }

        mNextButton = (Button) findViewById(R.id.next);
        mNextButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mSelectedDevice != null) {
                    if (mSelectedDevice.getDeviceType() == BluetoothIBridgeDevice.DEVICE_TYPE_BLE) {
                        if (isServiceExist(mSelectedDevice, mServiceUUIDEdit.getText().toString())) {
                            Log.i(TAG, "Service is exist!");
                            mAdapter.bleSetMtu(mSelectedDevice, Integer.parseInt(mMtuEdit.getText().toString().trim()));
                            String newService = mServiceUUIDEdit.getText().toString().trim();
                            if (!newService.equalsIgnoreCase(BRT_GATT_SERVICE_UUID)) {
                                mAdapter.bleSetTargetUUIDs(mSelectedDevice, mServiceUUIDEdit.getText().toString()
                                        , mNotifyUUIDEdit.getText().toString(), mWriteUUIDEdit.getText().toString());
                            }
                            toggleView(SwitcherView.INDEX_OF_PRESENT_VIEW, mSelectedDevice);
                        } else {
                            Log.i(TAG, "Service is not exist!");
                        }
                    } else {
                        toggleView(SwitcherView.INDEX_OF_PRESENT_VIEW, mSelectedDevice);
                    }
                }
            }
        });

        mWifiConfigButton = (Button) findViewById(R.id.wifiConfig);
        mWifiConfigButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if ((mSelectedDevice != null) && (mSelectedDevice.getDeviceType() == BluetoothIBridgeDevice.DEVICE_TYPE_BLE)) {
                    toggleView(SwitcherView.INDEX_OF_WIFI_CONFIG_VIEW, mSelectedDevice);
                }
            }
        });

        mAncsButton = (Button) findViewById(R.id.ancs);
        mAncsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(context, SettingsActivity.class);
                MainActivity mainActivity = (MainActivity) context;
                mainActivity.startActivity(intent);
            }
        });
    }

    public void setDevice(BluetoothIBridgeDevice dev) {
        mSelectedDevice = dev;
        if (mSelectedDevice.getDeviceType() == BluetoothIBridgeDevice.DEVICE_TYPE_BLE) {
            mAdapter.connectDevice(mSelectedDevice);
        }
        refreshScreen();
    }

    public void setBluetoothAdapter(BluetoothIBridgeAdapter adapter) {
        if (adapter != null) {
            mAdapter = adapter;
            mAdapter.registerEventReceiver(this);
        } else if (mAdapter != null) {
            mAdapter.unregisterEventReceiver(this);
            mAdapter = null;
        }
    }

    public void onDestroy() {
        if (mAdapter != null) {
            mAdapter.unregisterEventReceiver(this);
        }
    }

    private String getPropertiesString(int properties) {
        String propertiesString = "";
        if ((properties & BluetoothGattCharacteristic.PROPERTY_BROADCAST) != 0) {
            propertiesString += "/broadcastable";
        }
        if ((properties & BluetoothGattCharacteristic.PROPERTY_EXTENDED_PROPS) != 0) {
            propertiesString += "/has extended properties";
        }
        if ((properties & BluetoothGattCharacteristic.PROPERTY_INDICATE) != 0) {
            propertiesString += "/supports indication";
        }
        if ((properties & BluetoothGattCharacteristic.PROPERTY_NOTIFY) != 0) {
            propertiesString += "/supports notification";
        }
        if ((properties & BluetoothGattCharacteristic.PROPERTY_READ) != 0) {
            propertiesString += "/readable";
        }
        if ((properties & BluetoothGattCharacteristic.PROPERTY_SIGNED_WRITE) != 0) {
            propertiesString += "/supports write with signature";
        }
        if ((properties & BluetoothGattCharacteristic.PROPERTY_WRITE) != 0) {
            propertiesString += "/can be written";
        }
        if ((properties & BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE) != 0) {
            propertiesString += "/can be written without response";
        }
        return propertiesString;
    }

    private boolean isServiceExist(BluetoothIBridgeDevice bluetoothDevice, String uuid) {
        boolean exist = false;

        if (bluetoothDevice.getGattServices() != null) {
            for (BluetoothGattService gattService : bluetoothDevice.getGattServices()) {
                if (gattService.getUuid().toString().toUpperCase().equals(uuid.toUpperCase())) {
                    exist = true;
                    break;
                }
            }
        }
        return exist;
    }

    private boolean isCharacteristicExist(BluetoothIBridgeDevice bluetoothDevice, String serviceUuid, String characteristicUuid) {
        boolean found = false;
        BluetoothGattService foundGattService = null;

        if (bluetoothDevice.getGattServices() != null) {
            for (BluetoothGattService gattService : bluetoothDevice.getGattServices()) {
                if (gattService.getUuid().toString().equals(serviceUuid)) {
                    foundGattService = gattService;
                    break;
                }
            }
        }
        if (foundGattService != null) {
            if (foundGattService.getCharacteristics() != null) {
                for (BluetoothGattCharacteristic bluetoothGattCharacteristic : foundGattService.getCharacteristics()) {
                    if (bluetoothGattCharacteristic.getUuid().toString().equals(characteristicUuid)) {
                        found = true;
                        break;
                    }
                }
            }
        }
        return found;
    }

    @SuppressLint("SetTextI18n")
    @Override
    public void refreshScreen() {
        String description;

        mAncsButton.setEnabled(mAdapter.ancsIsConnected());

        if (mSelectedDevice != null) {
            description = mContext.getString(R.string.deviceName) + mSelectedDevice.getDeviceName() + "\r\n";
            description += mContext.getString(R.string.deviceType) + mSelectedDevice.getDeviceType() + "\r\n";
            description += mContext.getString(R.string.deviceAddr) + mSelectedDevice.getDeviceAddress() + "\r\n";
            description += "\r\n";
            if (mSelectedDevice.getGattServices() != null) {
                description += mSelectedDevice.getGattServices().size() + "services found\r\n";
                for (BluetoothGattService gattService : mSelectedDevice.getGattServices()) {
                    description += gattService.getUuid().toString() + "\r\n";
                }
            }
            mDescriptionText.setText(description);

            if (mSelectedDevice.getDeviceType() == BluetoothIBridgeDevice.DEVICE_TYPE_BLE) {
                if ((mSelectedDevice.isConnected())) {
                    if (isServiceExist(mSelectedDevice, BRT_GATT_SERVICE_UUID)) {
                        mServiceUUIDEdit.setText(BRT_GATT_SERVICE_UUID);
                        mNotifyUUIDEdit.setText(BRT_GATT_NOTIFY_CHARC_UUID);
                        mWriteUUIDEdit.setText(BRT_GATT_WRITE_CHARC_UUID);
                    } else if (isServiceExist(mSelectedDevice, ISSC_GATT_SERVICE_UUID)) {
                        mServiceUUIDEdit.setText(ISSC_GATT_SERVICE_UUID);
                        mNotifyUUIDEdit.setText(ISSC_GATT_NOTIFY_CHARC_UUID);
                        mWriteUUIDEdit.setText(ISSC_GATT_WRITE_CHARC_UUID);
                    } else if (isServiceExist(mSelectedDevice, BRT_I81_WIFI_SERVICE_UUID)) {
                        mServiceUUIDEdit.setText(BRT_I81_WIFI_SERVICE_UUID);
                        mNotifyUUIDEdit.setText(BRT_I81_WIFI_NOTIFY_CHARC_UUID);
                        mWriteUUIDEdit.setText(BRT_I81_WIFI_WRITE_CHARC_UUID);
                    } else {
                        if ((mSelectedDevice.getGattServices() != null) &&
                                mSelectedDevice.getGattServices().isEmpty()) {
                            mServiceUUIDEdit.setText("NO Service");
                        } else {
                            mServiceUUIDEdit.setText("UNKNOWN");
                        }
                        mNotifyUUIDEdit.setText("");
                        mWriteUUIDEdit.setText("");
                    }
                    mMtuEdit.setText("" + mAdapter.bleGetMtu(mSelectedDevice));

                    mServiceUUIDEdit.setEnabled(true);
                    mWriteUUIDEdit.setEnabled(true);
                    mNotifyUUIDEdit.setEnabled(true);
                    if (Build.VERSION.SDK_INT >= 21) {
                        mMtuEdit.setEnabled(true);
                    } else {
                        mMtuEdit.setEnabled(false);
                    }
                    mWifiConfigButton.setEnabled(isServiceExist(mSelectedDevice, BRT_I81_WIFI_SERVICE_UUID));
                    mNextButton.setEnabled(true);
                } else {
                    mServiceUUIDEdit.setEnabled(false);
                    mWriteUUIDEdit.setEnabled(false);
                    mNotifyUUIDEdit.setEnabled(false);
                    mMtuEdit.setEnabled(false);
                    mWifiConfigButton.setEnabled(false);
                    mNextButton.setEnabled(false);
                }
            } else {
                mServiceUUIDEdit.setEnabled(false);
                mWriteUUIDEdit.setEnabled(false);
                mNotifyUUIDEdit.setEnabled(false);
                mMtuEdit.setEnabled(false);
                mWifiConfigButton.setEnabled(false);
                mNextButton.setEnabled(true);
            }
        }
    }

    @Override
    public void onBluetoothOn() {
    }

    @Override
    public void onBluetoothOff() {
    }

    @Override
    public void onDeviceBonding(BluetoothIBridgeDevice device) {
    }

    @Override
    public void onDeviceBonded(BluetoothIBridgeDevice device) {
    }

    @Override
    public void onDeviceBondNone(BluetoothIBridgeDevice device) {
    }

    @Override
    public void onDeviceFound(BluetoothIBridgeDevice device) {
    }

    @Override
    public void onDiscoveryFinished() {
    }

    @Override
    public void onDeviceConnected(BluetoothIBridgeDevice device) {
        refreshScreen();
    }

    @Override
    public void onDeviceDisconnected(BluetoothIBridgeDevice device, String exceptionMsg) {
        refreshScreen();
    }

    @Override
    public void onDeviceConnectFailed(BluetoothIBridgeDevice device, String exceptionMsg) {

    }

    @Override
    public void onWriteFailed(BluetoothIBridgeDevice device, String exceptionMsg) {

    }

    @Override
    public void onLeServiceDiscovered(BluetoothIBridgeDevice device,
                                      String exceptionMsg) {
        if (device.equals(mSelectedDevice)) {
            refreshScreen();
        }
    }
}
