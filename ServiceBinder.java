package com.brt.ibridge;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;

import com.brt.bluetooth.ibridge.BluetoothIBridgeAdapter;

import java.util.ArrayList;
import java.util.List;

public class ServiceBinder {

	private boolean mIsBound;
	private Context mContext;
	private List<BluetoothAdapterListener> mListeners;
	private BluetoothIBridgeAdapter iBridgeAdapter;

	public ServiceBinder(Context context) {
		mContext = context;
		mListeners = new ArrayList<BluetoothAdapterListener>();
	}
	public BluetoothIBridgeAdapter getiBridgeAdapter(){
		return iBridgeAdapter;
	}

	void doStartService() {
		Intent startIntent = new Intent(mContext, TestService.class);
		mContext.startService(startIntent);
	}

	void doStopService() {
		Intent stopIntent = new Intent(mContext, TestService.class);
		mContext.stopService(stopIntent);
	}

	void doBindService() {
		mContext.bindService(new Intent(mContext, TestService.class),
				mConnection, Context.BIND_AUTO_CREATE);
		mIsBound = true;
	}

	void doUnbindService() {
		if (mIsBound) {
			mContext.unbindService(mConnection);
			mIsBound = false;
		}
	}

	public void registerBluetoothAdapterListener(
			BluetoothAdapterListener listener) {
		synchronized (mListeners) {
			mListeners.add(listener);
		}
	}

	public void unregisterBluetoothAdapterListener(
			BluetoothAdapterListener listener) {
		synchronized (mListeners) {
			mListeners.remove(listener);
		}
	}

	public interface BluetoothAdapterListener {
		void onBluetoothAdapterCreated(BluetoothIBridgeAdapter adapter);

		void onBluetoothAdapterDestroyed();
	}

	private ServiceConnection mConnection = new ServiceConnection() {

		@Override
		public void onServiceConnected(ComponentName className, IBinder service) {
			BluetoothIBridgeAdapter adapter = ((TestService.LocalBinder) service)
					.getBluetoothAdapter();
			for (BluetoothAdapterListener l : mListeners) {
				if (l != null) {
					l.onBluetoothAdapterCreated(adapter);
					iBridgeAdapter = adapter;
				}
			}
		}

		@Override
		public void onServiceDisconnected(ComponentName name) {
			for (BluetoothAdapterListener l : mListeners) {
				if (l != null) {
					l.onBluetoothAdapterDestroyed();
				}
			}
		}
	};

}
