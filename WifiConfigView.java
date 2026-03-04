package com.brt.ibridge;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Handler;
import android.os.Message;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import com.brt.bluetooth.ibridge.BluetoothIBridgeAdapter;
import com.brt.bluetooth.ibridge.BluetoothIBridgeDevice;

import java.io.UnsupportedEncodingException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;

/**
 * Created by qiuwenqing on 15/10/29.
 */
public class WifiConfigView extends Screen implements BluetoothIBridgeAdapter.DataReceiver {
    private BluetoothIBridgeAdapter mAdapter = null;
    private MyHandler myHandler;
    private ListView mListView;
    private ArrayList<String> mSSIDs;

    public WifiConfigView(final Context context, View view) {
        super(context, view);
        myHandler = new MyHandler(this);
        mSSIDs = new ArrayList<>();

        mListView = (ListView) findViewById(R.id.ssidList);
        mListView.setAdapter(mListAdapter);

        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view,
                                    int position, long id) {
                String ssid = (String) mListAdapter.getItem(position);
                final EditText inputServer = new EditText(context);
                AlertDialog.Builder builder = new AlertDialog.Builder(context);

                builder.setTitle("Password of " + ssid).setIcon(android.R.drawable.ic_dialog_info).setView(inputServer)
                        .setNegativeButton("Cancel", null);
                builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        inputServer.getText().toString();
                    }
                });
                builder.show();
            }
        });
    }

    public void onDestroy() {
        if (mAdapter != null) {
            mAdapter.unregisterDataReceiver(this);
        }
    }

    @Override
    public void refreshScreen() {

    }

    private BaseAdapter mListAdapter = new BaseAdapter() {

        @Override
        public int getCount() {
            return mSSIDs.size();
        }

        @Override
        public Object getItem(int position) {
            return mSSIDs.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            String ssid = mSSIDs.get(position);
            LayoutInflater inflater = (LayoutInflater) mContext
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);

            if (null == mSSIDs || position < 0 || position >= mSSIDs.size()) {
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
                TextView nameView = (TextView) view.findViewById(R.id.name);
                TextView addressView = (TextView) view
                        .findViewById(R.id.address);
                if ((ssid != null) && (ssid.length() > 0)) {
                    nameView.setText(ssid);
                } else {
                    nameView.setText("unknown");
                }
                /*
                if (device.getDeviceType() == BluetoothIBridgeDevice.DEVICE_TYPE_CLASSIC) {
                    nameView.setTextColor(Color.BLACK);
                } else {
                    nameView.setTextColor(Color.BLUE);
                }
                addressView.setText(addr);
                if (device.isConnected()) {
                    view.setBackgroundColor(Color.argb(0xFF, 0x33, 0xB5, 0xE5));
                } else {
                    view.setBackgroundColor(Color.argb(0x0A, 0x0A, 0x0A, 0x0A));
                }
                */
            }
            return view;

        }

    };

    @Override
    public void onDataReceived(BluetoothIBridgeDevice device, byte[] buffer, int len) {
        String result = "";
        if (len < 10) {
            try {
                result = new String(buffer, 0, len, "utf-8");
                myHandler.obtainMessage(MyHandler.MSG_RECEIVED_STRING, result)
                        .sendToTarget();
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
        }
    }

    public void setBluetoothAdapter(BluetoothIBridgeAdapter adapter) {
        if (adapter != null) {
            mAdapter = adapter;
            mAdapter.registerDataReceiver(this);
        } else if (mAdapter != null) {
            mAdapter.unregisterDataReceiver(this);
            mAdapter = null;
        }
    }

    private static class MyHandler extends Handler {
        public static final int MSG_RECEIVED_STRING = 0x01;
        public static final int MESSAGE_CLEAR = 0x02;

        private final WeakReference<WifiConfigView> host;

        public MyHandler(WifiConfigView view) {
            host = new WeakReference<WifiConfigView>(view);
        }

        @Override
        public void handleMessage(Message msg) {
            final WifiConfigView view = host.get();
            switch (msg.what) {
                case MSG_RECEIVED_STRING:
                    break;
                case MESSAGE_CLEAR:
                    break;
            }
            super.handleMessage(msg);
        }
    }
}
