package com.brt.ibridge;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.FrameLayout;

import com.brt.bluetooth.ibridge.BluetoothIBridgeAdapter;
import com.brt.bluetooth.ibridge.BluetoothIBridgeDevice;

public class SwitcherView extends FrameLayout implements Switcher {
	private final static String TAG = "SwitcherView";
	private View deviceView;
	private View presentView;
	private View descriptionView;
	private View wifiConfigView;

	private DeviceView mDeviceView;
	private PresentView mPresentView;
	private DescriptionView mDescriptionView;
	private WifiConfigView mWifiConfigView;

	private int mIndexOfPreviousView = INDEX_OF_NONE;
	private int mIndexOfCurrentView = INDEX_OF_DEVICE_VIEW;

	public static int INDEX_OF_NONE = 0;
	public static int INDEX_OF_DEVICE_VIEW = 1;
	public static int INDEX_OF_DESCRIPTION_VIEW = 2;
	public static int INDEX_OF_PRESENT_VIEW = 3;
	public static int INDEX_OF_WIFI_CONFIG_VIEW = 4;

	public static int INDEX_OF_BACK = 10;

	public SwitcherView(Context context) {
		super(context);
	}

	public SwitcherView(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public SwitcherView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
	}

	public void setup(Context context) {
		deviceView = findViewById(R.id.deviceScreen);
		presentView = findViewById(R.id.presentScreen);
		descriptionView = findViewById(R.id.descriptionScreen);
		wifiConfigView = findViewById(R.id.wifiConfigScreen);

		deviceView.setVisibility(View.VISIBLE);
		presentView.setVisibility(View.GONE);
		descriptionView.setVisibility(View.GONE);
		wifiConfigView.setVisibility(View.GONE);

		mDeviceView = new DeviceView(context, deviceView);
		mDeviceView.setSwitcher(this);
		mPresentView = new PresentView(context, presentView);
		mPresentView.setSwitcher(this);
		mDescriptionView = new DescriptionView(context, descriptionView);
		mDescriptionView.setSwitcher(this);
		mWifiConfigView = new WifiConfigView(context, wifiConfigView);
		mWifiConfigView.setSwitcher(this);
	}

	@Override
	public boolean toggleView(int to, Object param) {
		boolean result = true;

		if (to == INDEX_OF_BACK) {
			if (mIndexOfCurrentView == INDEX_OF_DEVICE_VIEW) {
				to = INDEX_OF_DEVICE_VIEW;
				result = false;
			} else if (mIndexOfCurrentView == INDEX_OF_DESCRIPTION_VIEW) {
				to = INDEX_OF_DEVICE_VIEW;
			} else if (mIndexOfCurrentView == INDEX_OF_PRESENT_VIEW) {
				to = INDEX_OF_DEVICE_VIEW;
			} else if (mIndexOfCurrentView == INDEX_OF_WIFI_CONFIG_VIEW) {
				to = INDEX_OF_DEVICE_VIEW;
			}
		}
		if (to != mIndexOfCurrentView) {
			BluetoothIBridgeDevice dev = (BluetoothIBridgeDevice) param;
			mIndexOfPreviousView = mIndexOfCurrentView;
			if (to == INDEX_OF_DEVICE_VIEW) {
				deviceView.setVisibility(View.VISIBLE);
				presentView.setVisibility(View.GONE);
				descriptionView.setVisibility(View.GONE);
				wifiConfigView.setVisibility(View.GONE);
			} else if (to == INDEX_OF_DESCRIPTION_VIEW) {
				descriptionView.setVisibility(View.VISIBLE);
				deviceView.setVisibility(View.GONE);
				presentView.setVisibility(View.GONE);
				wifiConfigView.setVisibility(View.GONE);
				if (dev != null) {
					mDescriptionView.setDevice(dev);
				}
			} else if (to == INDEX_OF_PRESENT_VIEW) {
				presentView.setVisibility(View.VISIBLE);
				deviceView.setVisibility(View.GONE);
				descriptionView.setVisibility(View.GONE);
				wifiConfigView.setVisibility(View.GONE);
				if (dev != null) {
					mPresentView.setDevice(dev);
				}
			} else if (to == INDEX_OF_WIFI_CONFIG_VIEW) {
				deviceView.setVisibility(View.GONE);
				presentView.setVisibility(View.GONE);
				descriptionView.setVisibility(View.GONE);
				wifiConfigView.setVisibility(View.VISIBLE);
			}
			mIndexOfCurrentView = to;
			refreshScreen();
		}
		return result;
	}

	public void setBluetoothAdapter(BluetoothIBridgeAdapter adapter) {
		mDeviceView.setBluetoothAdapter(adapter);
		mPresentView.setBluetoothAdapter(adapter);
		mDescriptionView.setBluetoothAdapter(adapter);
		mWifiConfigView.setBluetoothAdapter(adapter);
	}

	public void refreshScreen() {
		mDeviceView.refreshScreen();
		mPresentView.refreshScreen();
	}

	public void onDestroy() {
		mDeviceView.onDestroy();
		mPresentView.onDestroy();
		mDescriptionView.onDestroy();
		mWifiConfigView.onDestroy();
	}

	public void onCreateAsDeviceConnected(BluetoothIBridgeDevice dev) {
		if (dev != null) {
			String addr = dev.getDeviceAddress();
			BluetoothIBridgeDevice device = BluetoothIBridgeDevice
					.createBluetoothIBridgeDevice(addr, BluetoothIBridgeDevice.DEVICE_TYPE_CLASSIC);
			mDeviceView.onDeviceConnected(device);
			mPresentView.onDeviceConnected(device);
			mDescriptionView.onDeviceConnected(device);
		}
	}
}
