package com.brt.ibridge;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import com.brt.bluetooth.ibridge.Ancs.AncsUtils;
import com.brt.bluetooth.ibridge.BluetoothIBridgeAdapter;
import com.brt.bluetooth.ibridge.BluetoothIBridgeDevice;

import java.util.ArrayList;
import java.util.List;

import android.text.TextUtils;
import android.content.ComponentName;

public class SettingsActivity extends Activity implements BluetoothIBridgeAdapter.EventReceiver {
    PackageManager pm;
    private List<PackageInfo> pakageinfos;
    private ListView mAppListView;
    private TextView versionTextView;
    private Button disconnectButton;
    private EditText magThresholdEditView;
    private Button setMagThresholdButton;
    private Button enableNotificationButton;
    private Button saveAncsDeviceButton;
    private Button whiteListButton;
    private CheckBox phoneCallCheckBox;
    private CheckBox smsCheckBox;
    BluetoothIBridgeAdapter bluetoothIBridgeAdapter = null;
    private static final String ENABLED_NOTIFICATION_LISTENERS = "enabled_notification_listeners";
    BluetoothIBridgeDevice mBluetoothDevice = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        pm = getPackageManager();

        pakageinfos = new ArrayList<>();
        for (PackageInfo packageInfo : getPackageManager().getInstalledPackages(0)) {
            if (!isSystemApp(packageInfo) && !isSystemUpdateApp(packageInfo)) {
                pakageinfos.add(packageInfo);
            }
        }

        setContentView(R.layout.activity_settings);

        versionTextView = (TextView) findViewById(R.id.version);
        String demoVersionName;
        try {
            demoVersionName = pm.getPackageInfo(getPackageName(), 0).versionName;
        } catch (PackageManager.NameNotFoundException e) {
            demoVersionName = "0.0";
        }
        bluetoothIBridgeAdapter = BluetoothIBridgeAdapter.sharedInstance(null);
        if (bluetoothIBridgeAdapter != null) {
            versionTextView.setText("Version:" + demoVersionName +
                    "(" + bluetoothIBridgeAdapter.getVersion() + ")");
        } else {
            versionTextView.setText("Version:" + demoVersionName);
        }

        disconnectButton = (Button) findViewById(R.id.disconnect);
        disconnectButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                disconnectButton.setEnabled(false);
                if (bluetoothIBridgeAdapter != null) {
                    List<BluetoothIBridgeDevice> connectedDevices = bluetoothIBridgeAdapter.getCurrentConnectedDevices();
                    if (connectedDevices != null && connectedDevices.size() > 0) {
                        for (BluetoothIBridgeDevice connectedDevice : connectedDevices) {
                            bluetoothIBridgeAdapter.disconnectDevice(connectedDevice);
                        }
                    } else {
                        if (mBluetoothDevice != null) {
                            bluetoothIBridgeAdapter.connectDevice(mBluetoothDevice);
                        }
                    }
                }

                //disconnectButton.setEnabled(false);
            }
        });

        saveAncsDeviceButton = (Button) findViewById(R.id.saveAncsDevice);
        saveAncsDeviceButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (bluetoothIBridgeAdapter != null) {
                    List<BluetoothIBridgeDevice> connectedDevices = bluetoothIBridgeAdapter.getCurrentConnectedDevices();
                    if (connectedDevices != null && connectedDevices.size() > 0) {
                        for (BluetoothIBridgeDevice connectedDevice : connectedDevices) {
                            bluetoothIBridgeAdapter.ancsSetLastConnectedDevice(connectedDevice);
                        }
                    } else {
                        bluetoothIBridgeAdapter.ancsClearLastConnectedDevice();
                    }
                }
            }
        });

        phoneCallCheckBox = (CheckBox) findViewById(R.id.checkBoxPhoneCall);
        phoneCallCheckBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                if (b) {
                    bluetoothIBridgeAdapter.ancsAddAppToWhiteList(AncsUtils.APP_PACKAGE_NAME_INCOMING_CALL);
                    bluetoothIBridgeAdapter.ancsAddAppToWhiteList(AncsUtils.APP_PACKAGE_NAME_MISS_CALL);
                } else {
                    bluetoothIBridgeAdapter.ancsRemoveAppFromWhiteList(AncsUtils.APP_PACKAGE_NAME_INCOMING_CALL);
                    bluetoothIBridgeAdapter.ancsRemoveAppFromWhiteList(AncsUtils.APP_PACKAGE_NAME_MISS_CALL);
                }
            }
        });

        smsCheckBox = (CheckBox) findViewById(R.id.checkBoxSms);
        smsCheckBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                if (b) {
                    bluetoothIBridgeAdapter.ancsAddAppToWhiteList(AncsUtils.APP_PACKAGE_NAME_SMS);
                } else {
                    bluetoothIBridgeAdapter.ancsRemoveAppFromWhiteList(AncsUtils.APP_PACKAGE_NAME_SMS);
                }
            }
        });

        whiteListButton = (Button) findViewById(R.id.whiteList);
        whiteListButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (bluetoothIBridgeAdapter.ancsIsWhiteListEnabled()) {
                    bluetoothIBridgeAdapter.ancsDisableWhiteList();
                } else {
                    bluetoothIBridgeAdapter.ancsEnableWhiteList();
                }
                if (bluetoothIBridgeAdapter.ancsIsWhiteListEnabled()) {
                    mAppListView.setEnabled(true);
                    phoneCallCheckBox.setEnabled(true);
                    smsCheckBox.setEnabled(true);
                } else {
                    mAppListView.setEnabled(false);
                    phoneCallCheckBox.setEnabled(false);
                    smsCheckBox.setEnabled(false);
                }
            }
        });

        mAppListView = (ListView) findViewById(R.id.app_list);
        mAppListView.setAdapter(mListAdapter);
        mAppListView.setOnItemClickListener(new ListView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if (bluetoothIBridgeAdapter != null) {
                    boolean found = false;
                    List<String> whiteList = bluetoothIBridgeAdapter.ancsGetAppWhiteList();
                    if (whiteList != null) {
                        for (String app : whiteList) {
                            if (app.equals(pakageinfos.get(position).packageName)) {
                                found = true;
                                break;
                            }
                        }
                    }
                    if (found) {
                        bluetoothIBridgeAdapter.ancsRemoveAppFromWhiteList(pakageinfos.get(position).packageName);
                    } else {
                        bluetoothIBridgeAdapter.ancsAddAppToWhiteList(pakageinfos.get(position).packageName);
                    }
                    mListAdapter.notifyDataSetChanged();
                }
            }
        });

        magThresholdEditView = (EditText) findViewById(R.id.magThreshold);
        setMagThresholdButton = (Button) findViewById(R.id.setMagThreshold);
        setMagThresholdButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                TestService.magThreshold = Double.valueOf(magThresholdEditView.getText().toString()).doubleValue();
            }
        });

        if (bluetoothIBridgeAdapter != null) {
            bluetoothIBridgeAdapter.registerEventReceiver(this);
        }


        enableNotificationButton = (Button) findViewById(R.id.enableNotification);
        enableNotificationButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS");
                startActivity(intent);
            }
        });

        if (bluetoothIBridgeAdapter != null) {
            List<BluetoothIBridgeDevice> connectedDevices = bluetoothIBridgeAdapter.getCurrentConnectedDevices();
            if (connectedDevices != null && connectedDevices.size() > 0) {
                disconnectButton.setVisibility(View.VISIBLE);
                saveAncsDeviceButton.setText("Save Device");
            } else {
                disconnectButton.setVisibility(View.INVISIBLE);
                saveAncsDeviceButton.setText("Clear Device");
            }
            if (bluetoothIBridgeAdapter.ancsGetAppWhiteList().indexOf(AncsUtils.APP_PACKAGE_NAME_INCOMING_CALL) >= 0) {
                phoneCallCheckBox.setChecked(true);
            } else {
                phoneCallCheckBox.setChecked(false);
            }
            if (bluetoothIBridgeAdapter.ancsGetAppWhiteList().indexOf(AncsUtils.APP_PACKAGE_NAME_SMS) >= 0) {
                smsCheckBox.setChecked(true);
            } else {
                smsCheckBox.setChecked(false);
            }
            if (bluetoothIBridgeAdapter.ancsIsWhiteListEnabled()) {
                mAppListView.setEnabled(true);
                phoneCallCheckBox.setEnabled(true);
                smsCheckBox.setEnabled(true);
            } else {
                mAppListView.setEnabled(false);
                phoneCallCheckBox.setEnabled(false);
                smsCheckBox.setEnabled(false);
            }
        }
        /*
        if (saveAncsDeviceButton.getText() == "Save Device" && !isAppNotificationEnabled()) {
            new AlertDialog.Builder(SettingsActivity.this).setTitle("Notification Access").setIcon(android.R.drawable.ic_dialog_alert)
                    .setMessage("Please click Notifi button to enable NotificationMonitor access").setPositiveButton("OK", null).show();
        }*/
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (bluetoothIBridgeAdapter != null) {
            bluetoothIBridgeAdapter.unregisterEventReceiver(this);
        }
    }

    private boolean isAppNotificationEnabled() {
        String pkgName = getPackageName();
        final String flat = android.provider.Settings.Secure.getString(getContentResolver(), ENABLED_NOTIFICATION_LISTENERS);
        if (!TextUtils.isEmpty(flat)) {
            final String[] names = flat.split(":");
            for (int i = 0; i < names.length; i++) {
                final ComponentName cn = ComponentName.unflattenFromString(names[i]);
                if (cn != null) {
                    if (TextUtils.equals(pkgName, cn.getPackageName())) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private boolean isSystemApp(PackageInfo pInfo) {
        return ((pInfo.applicationInfo.flags & ApplicationInfo.FLAG_SYSTEM) != 0);
    }

    private boolean isSystemUpdateApp(PackageInfo pInfo) {
        return ((pInfo.applicationInfo.flags & ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0);
    }

    private BaseAdapter mListAdapter = new BaseAdapter() {

        @Override
        public int getCount() {
            return pakageinfos.size();
        }

        @Override
        public Object getItem(int position) {
            return pakageinfos.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            PackageInfo packageInfo = pakageinfos.get(position);
            String name = packageInfo.applicationInfo.loadLabel(pm).toString();
            String packageName = packageInfo.packageName;

            LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);

            if (null == pakageinfos || position < 0 || position >= pakageinfos.size()) {
                return null;
            }

            View view = null;
            if (null != convertView) {
                view = convertView;
            }
            if (null == view) {
                view = inflater.inflate(R.layout.app_list, parent, false);
            }

            if (null != view && (view instanceof LinearLayout)) {
                TextView nameView = (TextView) view.findViewById(R.id.name);
                TextView packageNameView = (TextView) view
                        .findViewById(R.id.package_name);

                ImageView imageView = (ImageView) view.findViewById(R.id.imageView);
                imageView.setImageDrawable(packageInfo.applicationInfo.loadIcon(pm));

                if ((name != null) && (name.length() > 0)) {
                    nameView.setText(name);
                } else {
                    nameView.setText("unknown");
                }
                if ((packageName != null) && (packageName.length() > 0)) {
                    packageNameView.setText(packageName);
                } else {
                    packageNameView.setText("unknown");
                }

                if (bluetoothIBridgeAdapter != null) {
                    boolean found = false;
                    List<String> whiteList = bluetoothIBridgeAdapter.ancsGetAppWhiteList();
                    if (whiteList != null) {
                        for (String app : whiteList) {
                            if (app.equals(pakageinfos.get(position).packageName)) {
                                found = true;
                                break;
                            }
                        }
                    }
                    if (found) {
                        view.setBackgroundColor(Color.argb(0xFF, 0x33, 0xB5, 0xE5));
                    } else {
                        view.setBackgroundColor(Color.argb(0xFF, 0xFF, 0xFF, 0xFF));
                    }
                }
            }
            return view;
        }
    };


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
        disconnectButton.setEnabled(true);
        disconnectButton.setText("disconnect");
        if (mBluetoothDevice == null) {
            mBluetoothDevice = device;
        }

    }

    @Override
    public void onDeviceDisconnected(BluetoothIBridgeDevice device, String exceptionMsg) {
        disconnectButton.setText("connect");
        disconnectButton.setEnabled(true);
        if (mBluetoothDevice == null) {
            mBluetoothDevice = device;
        }
        //disconnectButton.setEnabled(false);
    }

    @Override
    public void onDeviceConnectFailed(BluetoothIBridgeDevice device, String exceptionMsg) {
        // TODO Auto-generated method stub

    }

    @Override
    public void onWriteFailed(BluetoothIBridgeDevice device, String exceptionMsg) {
        // TODO Auto-generated method stub
    }

    @Override
    public void onLeServiceDiscovered(BluetoothIBridgeDevice device,
                                      String exceptionMsg) {
    }

}
