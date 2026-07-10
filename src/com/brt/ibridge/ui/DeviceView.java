package com.brt.ibridge.ui;

import com.brt.ibridge.Switcher;
import com.brt.ibridge.SettingsActivity;
import com.brt.ibridge.model.BeaconModel;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
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
import android.util.Log;
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
        mClassicSelected = find(R.id.classicSelected);
        mClassicSelected.setChecked(true);
        mBLESelected = find(R.id.bleSelected);
        if (!BluetoothIBridgeAdapter.bleIsSupported()) {
            mBLESelected.setEnabled(false);
        }
        Search_RSSI = (find(R.id.et_serch_rssi));
        Search_ADDR = (find(R.id.et_serch_addr));
        mSearchBtn = (find(R.id.discovery));
        mSearchBtn.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                clearDevices();
                mListAdapter.notifyDataSetChanged();
                String addr = Search_ADDR.getText().toString();
                String rssi = Search_RSSI.getText().toString();
                int convertedNumber = 0;

                if (mAdapter == null) {
                    // 鏇挎崲涓轰綘App涓垱寤築luetoothIBridgeAdapter瀹炰緥鐨勭湡瀹為€昏緫
                    // 绀轰緥锛氫粠涓婁笅鏂?鍏ㄥ眬鍗曚緥涓幏鍙栨垨閲嶆柊鍒涘缓Adapter
                    mAdapter = BluetoothIBridgeAdapter.sharedInstance(context);
                    // 閲嶆柊娉ㄥ唽浜嬩欢鎺ユ敹鍣紙鍜宻etBluetoothAdapter閫昏緫瀵归綈锛?
                    mAdapter.registerEventReceiver(DeviceView.this);
                    // 鎻愮ず鐢ㄦ埛绋嶇瓑锛堝彲閫夛級
                    //Toast.makeText(context, "姝ｅ湪鍒濆鍖栬摑鐗欓€傞厤鍣?..", Toast.LENGTH_SHORT).show();

                    // 鑻ュ垱寤哄け璐ワ紝鐩存帴鎻愮ず骞惰繑鍥?
                    if (mAdapter == null) {
                        Toast.makeText(context, "钃濈墮閫傞厤鍣ㄥ垵濮嬪寲澶辫触锛岃妫€鏌ユ潈闄愬悗閲嶈瘯", Toast.LENGTH_SHORT).show();
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

        mSettingsBtn = find(R.id.setting);
        mSettingsBtn.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(context, SettingsActivity.class);
                MainActivity mainActivity = (MainActivity) context;
                mainActivity.startActivity(intent);
            }
        });

        mListView = find(R.id.deviceList);
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

        mHintVeiw = find(R.id.hint);
        mHintVeiw.setText("浣跨敤璇存槑:\n" +
                "[Discovery]鎼滅储鍛ㄥ洿鐨勮摑鐗欒澶嘰n" +
                "[BLE]鏄綆鍔熻€楄摑鐗橽n" +
                "[CALSSIC]鏄紶缁熻摑鐗橽n" +
                "鍦ㄦ悳绱㈠埌鐨勮澶囨潯鐩偣鍑?鍙互杩涘叆鍒拌繛鎺ョ晫闈n" +
                "鍦ㄦ悳绱㈠埌鐨勮澶囨潯鐩暱鎸?鍙互杩涘叆鍒伴厤缃晫闈?);
        mHintVeiw.setTextColor(Color.GRAY);
        mHintVeiw.setTextSize(16);
    }

    /**
     * bytes杞崲鎴愬崄鍏繘鍒跺瓧绗︿覆
     *
     * @param b byte鏁扮粍
     * @return String 姣忎釜Byte鍊间箣闂寸┖鏍煎垎闅?
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
            // 鏍规嵁RSSI鍊兼瘮杈冨ぇ灏忥紝杩欓噷鎸夌収RSSI鍊间粠澶у埌灏忔帓搴?
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
        // 鍏堟敞閿€鏃ч€傞厤鍣ㄧ殑鎺ユ敹鍣?
        if (mAdapter != null) {
            try {
                mAdapter.unregisterEventReceiver(this);
            } catch (Exception e) {
                Log.e(TAG, "Exception", e);
            }
            mAdapter = null;
        }

        // 鏂癮dapter闈炵┖鏃惰祴鍊煎苟娉ㄥ唽
        if (adapter != null) {
            mAdapter = adapter;
            try {
                mAdapter.registerEventReceiver(this);
            } catch (Exception e) {
                Log.e(TAG, "Exception", e);
                Toast.makeText(mContext, "钃濈墮閫傞厤鍣ㄦ敞鍐屽け璐?, Toast.LENGTH_SHORT).show();
                mAdapter = null;
            }
        }
    }
}


