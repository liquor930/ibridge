package com.brt.ibridge;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.os.Looper;
import android.os.PowerManager;
import android.util.Log;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

import com.brt.bluetooth.ibridge.Ancs.AncsUtils;
import com.brt.bluetooth.ibridge.BluetoothIBridgeAdapter;
import com.brt.bluetooth.ibridge.BluetoothIBridgeAdapter.EventReceiver;
import com.brt.bluetooth.ibridge.BluetoothIBridgeDevice;

import java.util.ArrayList;
import java.util.Set;

public class TestService extends Service implements EventReceiver {
	private BluetoothIBridgeAdapter mAdapter;
	private IBinder mBinder = new LocalBinder();
	private PowerManager.WakeLock mWakeLock;

	private SensorManager sensorManager;
	private Sensor sensor;
	private int count = 0;
	private double mag_value = 0;

	static double magThreshold = 0;

	private static final String TAG = "TestService";
	// 与透明Activity一致的广播Action
	private static final String PERMISSION_RESULT_ACTION = "com.brt.ibridge.PERMISSION_RESULT_ACTION";
	// 权限结果广播接收器
	private PermissionResultReceiver permissionResultReceiver;


	@Override
	public void onCreate() {
		super.onCreate();
		// 1. 注册广播接收器，接收权限请求结果
		registerPermissionReceiver();
		// 2. 检测权限，按需请求
		checkBlePermissionAndInit();


	}

	/**
	 * 核心方法：检测蓝牙权限，未授予则启动透明Activity请求，授予则初始化蓝牙业务
	 */
	private void checkBlePermissionAndInit() {
		if (isBlePermissionGranted()) {
			Log.d(TAG, "蓝牙权限已授予，初始化蓝牙业务");
            Toast.makeText(this, "蓝牙权限已授予，初始化蓝牙业务", Toast.LENGTH_LONG).show();
			// 权限已授予，执行你的蓝牙核心业务初始化（原代码）
			initBleBusiness();
		} else {
			Log.d(TAG, "蓝牙权限未授予，启动透明Activity请求权限");
			// 权限缺失，启动透明PermissionRequestActivity触发弹窗
			Intent intent = new Intent(this, PermissionRequestActivity.class);
			// 关键：添加FLAG_ACTIVITY_NEW_TASK，Service启动Activity必须加
			intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			startActivity(intent);
		}
	}

	/**
	 * 检测蓝牙/定位权限是否授予（适配Android 12+）
	 */
	private boolean isBlePermissionGranted() {
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
			// Android <12：仅检测定位权限
			return ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
					&& ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;
		} else {
			// Android 12+：检测蓝牙+定位权限
			return ContextCompat.checkSelfPermission(this, android.Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED
					&& ContextCompat.checkSelfPermission(this, android.Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED
					&& ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
		}
	}

	/**
	 * 初始化蓝牙核心业务（替换为你的原有代码）
	 */
	private void initBleBusiness() {
		// *************************
		// 这里写你的蓝牙业务原代码：如初始化适配器、扫描、连接等
		// *************************
		mAdapter = BluetoothIBridgeAdapter.sharedInstance(null);
		if (mAdapter != null) {
			mAdapter.destroy();
		}
		mAdapter = BluetoothIBridgeAdapter.sharedInstance(this);

		if(!mAdapter.isEnabled()){
			mAdapter.setEnabled(true);
		}
		if(Build.VERSION.SDK_INT >= 10){
			mAdapter.setLinkKeyNeedAuthenticated(false);
		}else{
			mAdapter.setLinkKeyNeedAuthenticated(true);
		}
		mAdapter.registerEventReceiver(this);
		acquireWakeLock();
		android.util.Log.i("TestService", "onCreate");

		mAdapter.ancsRegisterReceiver(new BluetoothIBridgeAdapter.AncsReceiver() {
			@Override
			public void onPerformNotificationAction(String appIdentifier, byte actionID) {
				Log.i("onPerformNotificationAction", appIdentifier + ":" + actionID);
				if (appIdentifier.equals(AncsUtils.APP_PACKAGE_NAME_INCOMING_CALL)) {
					if (actionID == AncsUtils.ACTION_ID_POSITIVE) {
						showToast("accept incoming call");
						android.util.Log.i("TestService", "accept incoming call here");
					} else if (actionID == AncsUtils.ACTION_ID_NEGATICE) {
						showToast("refuse incoming call");
						android.util.Log.i("TestService", "refuse incoming call here");
					}
				} if (appIdentifier.equals(AncsUtils.APP_PACKAGE_NAME_MISS_CALL)) {
					if (actionID == AncsUtils.ACTION_ID_POSITIVE) {
						showToast("dial");
						android.util.Log.i("TestService", "dial");
					} else if (actionID == AncsUtils.ACTION_ID_NEGATICE) {
						showToast("clear");
						android.util.Log.i("TestService", "clear");
					}
				} else if (appIdentifier.equals(AncsUtils.APP_PACKAGE_NAME_SMS)) {
					if (actionID == AncsUtils.ACTION_ID_NEGATICE) {
						showToast("clear");
						android.util.Log.i("TestService", "clear");
					}
				} else {
					if (actionID == AncsUtils.ACTION_ID_NEGATICE) {
						showToast("clear");
						android.util.Log.i("TestService", "clear");
					} else if (actionID == AncsUtils.ACTION_ID_POSITIVE) {
						PackageManager packageManager = getPackageManager();
						Intent intent = new Intent();
						intent = packageManager.getLaunchIntentForPackage(appIdentifier);
						if (intent != null) {
							startActivity(intent);
						} else {
							android.util.Log.i("TestService", "APP not found!");
						}
					}
				}
			}
		});

		sensorManager = (SensorManager)getSystemService(SENSOR_SERVICE);
		sensor = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
		sensorManager.registerListener(new SensorEventListener() {
			@Override
			public void onSensorChanged(SensorEvent event) {
				if(count++ == 20) {
					double value = Math.sqrt(event.values[0]*event.values[0] + event.values[1]*event.values[1]
							+event.values[2]*event.values[2]);
					String str = String.format("X:%8.4f , Y:%8.4f , Z:%8.4f \n value:%8.4f\n",
							event.values[0],event.values[1],event.values[2],value);
					count = 1;
					if ((mag_value != 0) && (TestService.magThreshold > 0) && Math.abs(value - mag_value) > TestService.magThreshold) {
						Log.i("Mag", str);
						Log.i("Mag", "startMainActivity" + "before " + mag_value + ",after " + value);
						startMainActivity();
					}
					mag_value = value;
				}
			}

			@Override
			public void onAccuracyChanged(Sensor sensor, int i) {

			}
		}, sensor, sensorManager.SENSOR_DELAY_NORMAL);

		if (TestService.isNotificationListenerServiceEnabled(this)) {
			toggleNotificationListenerService();
		} else {
			Log.i("TestService", "app is not open notification");
		}
		Toast.makeText(this, "蓝牙服务初始化成功", Toast.LENGTH_SHORT).show();
	}

	/**
	 * 注册权限结果广播接收器
	 */
	@SuppressLint("UnspecifiedRegisterReceiverFlag")
    private void registerPermissionReceiver() {
		permissionResultReceiver = new PermissionResultReceiver();
		IntentFilter filter = new IntentFilter();
		filter.addAction(PERMISSION_RESULT_ACTION); // 匹配广播Action
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) { // API 34
			// 仅接收自身APP广播，设置为NOT_EXPORTED（推荐，最安全）
			registerReceiver(permissionResultReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
		} else {
			// 低版本系统，使用旧的注册方法
			registerReceiver(permissionResultReceiver, filter);
		}
		Log.d(TAG, "权限结果广播接收器已注册");
	}

	/**
	 * 权限结果广播接收器：接收透明Activity的权限请求结果
	 */
	private class PermissionResultReceiver extends BroadcastReceiver {
		@Override
		public void onReceive(Context context, Intent intent) {
			if (PERMISSION_RESULT_ACTION.equals(intent.getAction())) {
				// 获取权限请求结果
				boolean isAllGranted = intent.getBooleanExtra("is_permission_granted", false);
				Log.d(TAG, "收到权限请求结果：" + isAllGranted);
				if (isAllGranted) {
					// 权限授予，初始化蓝牙业务
                    Toast.makeText(TestService.this, "蓝牙权限已授予，初始化蓝牙业务", Toast.LENGTH_SHORT).show();
					initBleBusiness();
				} else {
					// 权限拒绝，停止服务并提示
					Toast.makeText(TestService.this, "蓝牙权限被拒绝，服务无法运行", Toast.LENGTH_SHORT).show();
					stopSelf(); // 停止服务，避免无权限运行崩溃
				}
			}
		}
	}

	private void toggleNotificationListenerService() {
		Log.i("TestService", "toggleNotificationListenerService");
		PackageManager pm = getPackageManager();
		pm.setComponentEnabledSetting(new ComponentName(this, com.brt.bluetooth.ibridge.Ancs.GattNotificationListener.class),
				PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP);

		pm.setComponentEnabledSetting(new ComponentName(this, com.brt.bluetooth.ibridge.Ancs.GattNotificationListener.class),
				PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP);
		Log.i("TestService", "toggleNotificationListenerService...");

	}

	public static boolean isNotificationListenerServiceEnabled(Context context) {
		Log.i("TestService", "isNotificationListenerServiceEnabled");
		boolean ret = false;
		Set<String> packageNames = NotificationManagerCompat.getEnabledListenerPackages(context);
		if (packageNames.contains(context.getPackageName())) {
			ret = true;
		}
		Log.i("TestService", "isNotificationListenerServiceEnabled" + ret);
		return ret;
	}

	@Override
	public void onDestroy() {
		android.util.Log.i("TestService", "onDestroy");
		if (permissionResultReceiver != null) {
			unregisterReceiver(permissionResultReceiver);
		}
		releaseWakeLock();
		mAdapter.unregisterEventReceiver(this);
		mAdapter.destroy();
		super.onDestroy();
	}

	@Override
	public IBinder onBind(Intent intent) {
		return mBinder ;
	}

	public class LocalBinder extends Binder {
		BluetoothIBridgeAdapter getBluetoothAdapter() {
			return mAdapter;
		}
	}

	private void showToast(String msg){
		ToastThread toastThread = new ToastThread(msg);
		toastThread.start();
	}

	private class ToastThread extends Thread {
		private String msg;

		public ToastThread(String msg) {
			this.msg = msg;
		}

		public void run() {
			Looper.prepare();
			Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_SHORT).show();
			Looper.loop();
		}
	}

	private void startForeground() {
		Intent it = new Intent(this, MainActivity.class);
		it.setAction(Intent.ACTION_MAIN);
		it.addCategory(Intent.CATEGORY_LAUNCHER);
		it.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		PendingIntent pit = PendingIntent.getActivity(this, 0, it,
				PendingIntent.FLAG_IMMUTABLE);

		Notification ntfc = new Notification(R.drawable.ic_launcher, getString(R.string.app_name),
				System.currentTimeMillis());
		//ntfc.setLatestEventInfo(this, getString(R.string.connected), null,pit);
		ntfc.flags |= Notification.FLAG_ONGOING_EVENT;
	}

	private void startMainActivity() {
		Intent it = new Intent(this, MainActivity.class);
		it.setAction(Intent.ACTION_MAIN);
		it.addCategory(Intent.CATEGORY_LAUNCHER);
		it.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		getApplication().startActivity(it);
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		android.util.Log.i("TestService", "Received start id " + startId + ": " + intent);
		// We want this service to continue running until it is explicitly
		// stopped, so return sticky.
		checkBlePermissionAndInit();
		return START_STICKY;
	}

	@Override
	public void onDeviceConnectFailed(BluetoothIBridgeDevice device,String exceptionMsg) {
	}

	@SuppressLint("ForegroundServiceType")
    @Override
	public void onDeviceConnected(BluetoothIBridgeDevice device) {
		ComponentName comp = new ComponentName(getPackageName(),
				MainActivity.class.getName());
		Intent intent = new Intent(Intent.ACTION_MAIN);
		intent.addCategory(Intent.CATEGORY_LAUNCHER);
		intent.setComponent(comp);
		intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		intent.putExtra("com.brt.bleSpp.device", device);
		startActivity(intent);
		startForeground();
	}

	@Override
	public void onDeviceDisconnected(BluetoothIBridgeDevice device,String exceptionMsg) {
		stopForeground(true);
		NotificationManager nm =(NotificationManager)getSystemService(NOTIFICATION_SERVICE);
		nm.cancel(0x37512433);
	}

	@Override
	public void onBluetoothOn() {}

	@Override
	public void onBluetoothOff() {}

	@Override
	public void onDeviceBonding(BluetoothIBridgeDevice device) {}

	@Override
	public void onDeviceBonded(BluetoothIBridgeDevice device) {}

	@Override
	public void onDeviceBondNone(BluetoothIBridgeDevice device) {}

	@Override
	public void onDeviceFound(BluetoothIBridgeDevice device) {
        Log.i("TestService", "OnDeviceFound");
	}

	@Override
	public void onDiscoveryFinished() {
        Log.i("TestService", "onDiscoveryFinished");
	}

	@Override
	public void onWriteFailed(BluetoothIBridgeDevice deivce, String exceptionMsg) {
		// TODO Auto-generated method stub
	}

	@Override
	public void onLeServiceDiscovered(BluetoothIBridgeDevice device, String exceptionMsg) {

	}

	private void acquireWakeLock() {
		PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
		mWakeLock = pm.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK, this
				.getClass().getCanonicalName());
		mWakeLock.acquire();
	}

	private void releaseWakeLock() {
		if (mWakeLock != null && mWakeLock.isHeld()) {
			mWakeLock.release();
			mWakeLock = null;
		}
	}

}
