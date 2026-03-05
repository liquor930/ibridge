package com.brt.ibridge;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.text.InputFilter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.brt.bluetooth.ibridge.BluetoothIBridgeAdapter;
import com.brt.bluetooth.ibridge.BluetoothIBridgeAdapter.EventReceiver;
import com.brt.bluetooth.ibridge.BluetoothIBridgeDevice;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;


public class DeviceView extends Screen implements EventReceiver {
    private BluetoothIBridgeAdapter mAdapter;
    private ArrayList<BluetoothIBridgeDevice> mDevices;
    private Button mSearchBtn;
    private Button mSettingsBtn;
    private CheckBox mClassicSelected;
    private CheckBox mBLESelected;
    private ListView mListView;
    private TextView mHintVeiw;
    private EditText Search_RSSI;
    private EditText Search_ADDR;

    public DeviceView(final Context context, View view) {
        super(context, view);

        mDevices = new ArrayList<BluetoothIBridgeDevice>();
        mClassicSelected = (CheckBox) findViewById(R.id.classicSelected);
        mClassicSelected.setChecked(true);
        mBLESelected = (CheckBox) findViewById(R.id.bleSelected);
        if (!BluetoothIBridgeAdapter.bleIsSupported()) {
            mBLESelected.setEnabled(false);
        }
        Search_RSSI = ((EditText) findViewById(R.id.et_serch_rssi));
        Search_ADDR = ((EditText) findViewById(R.id.et_serch_addr));
        mSearchBtn = ((Button) findViewById(R.id.discovery));
        mSearchBtn.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                clearDevices();
                mListAdapter.notifyDataSetChanged();
                String addr = Search_ADDR.getText().toString();
                String rssi = Search_RSSI.getText().toString();
                int convertedNumber = 0;

                if (mAdapter == null) {
                    // 替换为你App中创建BluetoothIBridgeAdapter实例的真实逻辑
                    // 示例：从上下文/全局单例中获取或重新创建Adapter
                    mAdapter = BluetoothIBridgeAdapter.sharedInstance(context);
                    // 重新注册事件接收器（和setBluetoothAdapter逻辑对齐）
                    mAdapter.registerEventReceiver(DeviceView.this);
                    // 提示用户稍等（可选）
                    //Toast.makeText(context, "正在初始化蓝牙适配器...", Toast.LENGTH_SHORT).show();

                    // 若创建失败，直接提示并返回
                    if (mAdapter == null) {
                        Toast.makeText(context, "蓝牙适配器初始化失败，请检查权限后重试", Toast.LENGTH_SHORT).show();
                        return;
                    }
                }

                if (mAdapter != null) {
                    if (mClassicSelected.isChecked()) {
                        try {
                            convertedNumber = Integer.parseInt(rssi);
                        } catch (NumberFormatException e) {
                            convertedNumber = 0;

                        }
                        mAdapter.setSearchValue(addr,convertedNumber);
                        mAdapter.startDiscovery();
                        mSearchBtn.setEnabled(false);
                    }
                    if (mBLESelected.isChecked()) {
                        try {
                            convertedNumber = Integer.parseInt(rssi);
                        } catch (NumberFormatException e) {
                            convertedNumber = 0;
                        }
                        mAdapter.setSearchValue(addr,convertedNumber);
                        if (mAdapter.bleStartScan(10)) {
                            mSearchBtn.setEnabled(false);
                        }
                    }
                    if (!mClassicSelected.isChecked() && !mBLESelected.isChecked()) {
                        mSearchBtn.setEnabled(true);
                    }
                }else {
                    Toast.makeText(context, "mAdapter is null", Toast.LENGTH_SHORT).show();
                }
            }

        });

        mSettingsBtn = (Button) findViewById(R.id.setting);
        mSettingsBtn.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(context, SettingsActivity.class);
                MainActivity mainActivity = (MainActivity) context;
                mainActivity.startActivity(intent);
            }
        });

        mListView = (ListView) findViewById(R.id.deviceList);
        mListView.setAdapter(mListAdapter);
        mListView.setOnItemClickListener(new OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> adapterView, View view,
                                    int position, long id) {
                if (mAdapter != null) {
                    if (mClassicSelected.isChecked()) {
                        mAdapter.stopDiscovery();
                    }
                    if (mBLESelected.isChecked()) {
                        mAdapter.bleStopScan();
                    }
                }
                BluetoothIBridgeDevice dev = (BluetoothIBridgeDevice) mListAdapter
                        .getItem(position);
                if (dev.isConnected()) {
                    toggleView(SwitcherView.INDEX_OF_PRESENT_VIEW, dev);
                } else {
                    if (dev.getDeviceType() == BluetoothIBridgeDevice.DEVICE_TYPE_BLE) {
                        toggleView(SwitcherView.INDEX_OF_DESCRIPTION_VIEW, dev);
                    } else {
                        toggleView(SwitcherView.INDEX_OF_PRESENT_VIEW, dev);
                    }
                }
            }
        });

        mListView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                BluetoothIBridgeDevice dev = (BluetoothIBridgeDevice) mListAdapter
                        .getItem(position);
                if (mAdapter != null) {
                    if (mClassicSelected.isChecked()) {
                        mAdapter.stopDiscovery();
                    }
                    if (mBLESelected.isChecked()) {
                        mAdapter.bleStopScan();
                    }
                }
                toggleView(SwitcherView.INDEX_OF_DESCRIPTION_VIEW, dev);
                return true;
            }
        });

        mHintVeiw = (TextView) findViewById(R.id.hint);
        mHintVeiw.setText("使用说明:\n" +
                "[Discovery]搜索周围的蓝牙设备\n" +
                "[BLE]是低功耗蓝牙\n" +
                "[CALSSIC]是传统蓝牙\n" +
                "在搜索到的设备条目点击,可以进入到连接界面\n" +
                "在搜索到的设备条目长按,可以进入到配置界面");
        mHintVeiw.setTextColor(Color.GRAY);
        mHintVeiw.setTextSize(16);
    }

    /**
     * bytes转换成十六进制字符串
     *
     * @param b byte数组
     * @return String 每个Byte值之间空格分隔
     */
    public static String byte2HexStr(byte[] b, boolean space) {
        String stmp = "";
        StringBuilder sb = new StringBuilder("");
        if (b != null) {
            for (int n = 0; n < b.length; n++) {
                stmp = Integer.toHexString(b[n] & 0xFF);
                sb.append((stmp.length() == 1) ? "0" + stmp : stmp);
                if (space) {
                    sb.append(" ");
                }
            }
        }
        return sb.toString().toUpperCase().trim();
    }

    private BaseAdapter mListAdapter = new BaseAdapter() {

        @Override
        public int getCount() {
            return mDevices.size();
        }

        @Override
        public Object getItem(int position) {
            return mDevices.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @SuppressLint("SetTextI18n")
		@Override
        public View getView(int position, View convertView, ViewGroup parent) {
            BluetoothIBridgeDevice device = mDevices.get(position);
            String name = device.getDeviceName();
            String addr = device.getDeviceAddress();
            LayoutInflater inflater = (LayoutInflater) mContext
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);

            if (null == mDevices || position < 0 || position >= mDevices.size()) {
                return null;
            }

            View view = null;
            if (null != convertView) {
                view = convertView;
            }
            if (null == view) {
                view = inflater.inflate(R.layout.device_list, parent, false);
            }

            if (null != view && (view instanceof LinearLayout)) {
                TextView nameView = view.findViewById(R.id.name);
                TextView addressView = view.findViewById(R.id.address);
                TextView rssiView = view.findViewById(R.id.rssi);
                if ((name != null) && (!name.isEmpty())) {
                    nameView.setText(name);
                } else {
                    nameView.setText("unknown");
                }
                if (device.getDeviceType() == BluetoothIBridgeDevice.DEVICE_TYPE_CLASSIC) {
                    nameView.setTextColor(Color.BLACK);
                    addressView.setText(addr);
                    rssiView.setText(String.format("%d", device.getRssi()));

                } else {
                    nameView.setTextColor(Color.BLUE);
					/* for adv data and scan response data
					 byte[] advData = new byte[31];
					System.arraycopy(device.getScanRecord(), 0, advData, 0, 31);
					byte[] srData = new byte[31];
					System.arraycopy(device.getScanRecord(), 31, srData, 0, 31);
					addr += "\r\n" + "rssi=" + device.getRssi() + "\r\nadv data=[" +byte2HexStr(advData, true) + "]"
							+ "\r\n" + "scan Response data=[" + byte2HexStr(srData, true) + "]";
					 */
                    //+ "\r,adv data=[" + byte2HexStr(device.getScanRecord(), true) + "]"
                    addr += "\r\n" + "rssi=" + device.getRssi() ;
                    if (BeaconModel.isBeacon(device.getScanRecord())) {
                        BeaconModel beaconModel = new BeaconModel();
                        beaconModel.updateFrom(device.mDevice, device.getRssi(), device.getScanRecord());
                        addr += "\r\niBeacon:\r"
                                + "uuid=" + beaconModel.uuid + "\r\n"
                                + beaconModel.arguments + "\r\n"
                                + "txpower=" + beaconModel.txPower + "\r";
                        addressView.setTextColor(Color.BLACK);
                    }
                    addressView.setText(addr);
                }
                if (device.isConnected()) {
                    //view.setBackgroundColor(Color.argb(0xFF, 0x33, 0xB5, 0xE5));
                } else {
                    view.setBackgroundColor(Color.argb(0x0A, 0x0A, 0x0A, 0x0A));
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

    public class RssiComparator implements Comparator<BluetoothIBridgeDevice> {
        @Override
        public int compare(BluetoothIBridgeDevice device1, BluetoothIBridgeDevice device2) {
            // 根据RSSI值比较大小，这里按照RSSI值从大到小排序
            return Integer.compare(device2.getRssi(), device1.getRssi());
        }
    }

    @Override
    public void onDeviceFound(BluetoothIBridgeDevice device) {
        if (!deviceExisted(device)) {
            synchronized (mDevices) {
                mDevices.add(device);
            }
            mListAdapter.notifyDataSetChanged();
        }
    }

    @Override
    public void onDiscoveryFinished() {
        RssiComparator rssiComparator = new RssiComparator();
        Collections.sort(mDevices, rssiComparator);
        mListAdapter.notifyDataSetChanged();
        mSearchBtn.setEnabled(true);
    }

    @Override
    public void onDeviceConnected(BluetoothIBridgeDevice device) {
        if (!deviceExisted(device)) {
            synchronized (mDevices) {
                mDevices.add(device);
            }
        }
        mListAdapter.notifyDataSetChanged();
    }

    @Override
    public void onDeviceDisconnected(BluetoothIBridgeDevice device, String exceptionMsg) {
        mListAdapter.notifyDataSetChanged();
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

    private boolean deviceExisted(BluetoothIBridgeDevice device) {
        if (device == null) {
            return false;
        }

        for (BluetoothIBridgeDevice d : mDevices) {
            if (d != null && d.equals(device)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void refreshScreen() {
        mListAdapter.notifyDataSetChanged();
    }

    public void onDestroy() {
        for (BluetoothIBridgeDevice d : mDevices) {
            if (d != null && d.isConnected() && mAdapter != null) {
                mAdapter.disconnectDevice(d);
            }
        }
        if (mAdapter != null) {
            mAdapter.unregisterEventReceiver(this);
        }
    }

    private void clearDevices() {
        if (mDevices != null) {
            ArrayList<BluetoothIBridgeDevice> newList = new ArrayList<BluetoothIBridgeDevice>();
            for (BluetoothIBridgeDevice d : mDevices) {
                if (d != null && d.isConnected()) {
                    newList.add(d);
                }
            }
            if (newList != null) {
                synchronized (mDevices) {
                    mDevices = newList;
                }
            }
        }
    }

    public void setBluetoothAdapter(BluetoothIBridgeAdapter adapter) {
        // 第一步：先注销旧适配器的接收器（防止内存泄漏+空指针）
        if (mAdapter != null) {
            try {
                mAdapter.unregisterEventReceiver(this);
            } catch (Exception e) {
                // 捕获注销异常，避免崩溃
                e.printStackTrace();
            }
            mAdapter = null;
        }

        // 第二步：仅当新adapter非空时，赋值并注册接收器
        if (adapter != null) {
            mAdapter = adapter;
            // 增加二次空值校验，兜底防止adapter实际为null
            if (mAdapter != null) {
                try {
                    mAdapter.registerEventReceiver(this);
                } catch (Exception e) {
                    e.printStackTrace();
                    Toast.makeText(mContext, "蓝牙适配器注册失败", Toast.LENGTH_SHORT).show();
                    mAdapter = null; // 注册失败则置空，避免后续误用
                }
            }
        }
    }
}
