package com.brt.ibridge;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattService;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.PopupWindow;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.brt.bluetooth.ibridge.BluetoothIBridgeAdapter;
import com.brt.bluetooth.ibridge.BluetoothIBridgeAdapter.DataReceiver;
import com.brt.bluetooth.ibridge.BluetoothIBridgeAdapter.EventReceiver;
import com.brt.bluetooth.ibridge.BluetoothIBridgeDevice;
import com.brt.ibridge.common.Throughput;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.ref.WeakReference;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Random;

public class PresentView extends Screen implements DataReceiver, EventReceiver {
    private static final String TAG = "PresentView";
    private static int MAX_BYTE = 1024;
    private static int MAX_LENGTH = 1024 * 10;
    private static String AUTO_TEXT = "auto test is runing, make sure enough long.++";
    private static final String iBridge_PATH = "/iBridge";
    private static final String LOG_FILE = "iBridge.log";
    private boolean BtnEnable = true;
    private boolean firstRev = false;
    private boolean autoTest = false;
    private boolean autoTestData = false;
    private long firstTime = 0;
    private long revLength = 0;
    private long timeInterval = 0;
    private double revSpeed = 0;
    private String text = "";
    private int times = 0;
    private long connect_sec = 0;

    private long circleTime = 0;
    private long circleStartTime = 0;

	private BluetoothIBridgeAdapter mAdapter;
	private Button connBtn;
	private CheckBox removeBondCheckBox;
	private Button autoSendBtn;
	private Button sendBtn;
	private Button clearBtn;
	private Button autoConnBtn;
	private Button settingBtn;
    private Button rcvBtn;

	private EditText dataText;
	private EditText timesText;
	private EditText intervalText;
	private EditText inputText;

    private ScrollView scrollView;
	private TextView speedText;
	private TextView resultText;
	private TextView writeText;

	private PopupWindow mPopupWindow;
	private CheckBox removeBondWhileDisconnectCheckBox;
	private CheckBox bondBeforeConnectCheckBox;
	private CheckBox autoPairReplyCheckBox;

	private CheckBox enterCheckBox;
	private CheckBox newLineCheckBox;
	private CheckBox hexSendCheckBox;
	private CheckBox hexRecvCheckBox;
	private CheckBox sendTestDataForAutoConnected;
	private EditText autoConnectTimes;
	private EditText autoConnectInterval;

	private CheckBox highPriorityConnectLE;
	private CheckBox balancedPriorityConnectLE;
	private CheckBox lowPowerPriorityConnectLE;

	private MyHandler myHandler;
	private BluetoothIBridgeDevice mSelectedDevice;

	private ConnectAndDisconnectThread connectAndDisconnectThread;
	private TransmissionThread transThread;

	public List<Throughput> mThroughputTest = new ArrayList<Throughput>();

	private final static String IVT_GATT_SERVICE_UUID = "0000ff00-0000-1000-8000-00805f9b34fb";
	private final static String IVT_GATT_NOTIFY_CHARC_UUID = "0000ff01-0000-1000-8000-00805f9b34fb";
	private final static String IVT_GATT_WRITE_CHARC_UUID = "0000ff02-0000-1000-8000-00805f9b34fb";

	private final static String IVT_BRACELET_NOTIFY_CHARC_UUID = "0000ff01-0000-1000-8000-00805f9b34fb";

	private final static String ISSC_GATT_SERVICE_UUID = "49535343-FE7D-4AE5-8FA9-9FAFD205E455";
	private final static String ISSC_GATT_NOTIFY_CHARC_UUID = "49535343-1E4D-4BD9-BA61-23C647249616";
	private final static String ISSC_GATT_WRITE_CHARC_UUID = "49535343-8841-43F4-A8D4-ECBE34729BB3";

	private final static String IVT_GATT_NOTIFY_CHARC_UUID_BRACELET = "0000ff01-0000-1000-8000-00805f9b34fb";

	public PresentView(final Context context, View view) {
		super(context, view);

		connBtn = (Button) findViewById(R.id.connect);
		connBtn.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				if (mSelectedDevice != null) {
					connBtn.setEnabled(false);
					if (mSelectedDevice.isConnected() && mAdapter != null) {
						mAdapter.disconnectDevice(mSelectedDevice);
					} else {
						mAdapter.connectDevice(mSelectedDevice);
						SystemClock.sleep(1000);
//						mAdapter.bleSetMtu(mSelectedDevice, 515);
						mAdapter.bleSetTargetUUIDs(mSelectedDevice, IVT_GATT_SERVICE_UUID
								, IVT_GATT_NOTIFY_CHARC_UUID, IVT_GATT_WRITE_CHARC_UUID);

					}
				}
			}
		});

		autoSendBtn = (Button) findViewById(R.id.sendTestSpeed);
		autoSendBtn.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				int data = Integer.parseInt(dataText.getText().toString().trim());
				int times = Integer.parseInt(timesText.getText().toString().trim());
				//int interval = Integer.parseInt(intervalText.getText().toString().trim());
				dataText.clearFocus();
				timesText.clearFocus();
				intervalText.clearFocus();
				if (data > 64) {
					dataText.setText("64");
					Toast.makeText(mContext,
							mContext.getString(R.string.over_max_value),
							Toast.LENGTH_SHORT).show();
				} else {
					if (transThread == null) {
						transThread = new TransmissionThread(data, times);
					}
					if (transThread.isAlive()) {
						autoSendBtn.setEnabled(false);
						transThread.cancel();
					} else {
						transThread = new TransmissionThread(data, times);
						transThread.start();
					}
				}
			}
		});

		sendBtn = (Button) findViewById(R.id.sendInput);
		sendBtn.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                // TODO Auto-generated method stub
                new Thread() {
                    @Override
                    public void run() {
                        super.run();
                        String input = inputText.getText().toString().trim();
                        byte[] data = null;
                        if (hexSendCheckBox.isChecked() && hexStrCheck(input)) {
                            data = hexStr2Bytes(input);
                        } else {
                            if (enterCheckBox.isChecked()) {
                                input = input + "\r";
                            }
                            if (newLineCheckBox.isChecked()) {
                                input = input + "\n";
                            }
                            data = input.getBytes();
                        }
                        if (null != mSelectedDevice && mAdapter != null
                                && mSelectedDevice.isConnected()) {
                            Date date = new Date();
                            circleStartTime = date.getTime();
                            Log.i(TAG, "# send data # data = " + data);
                            mAdapter.send(mSelectedDevice, data, data.length);
                        }
                    }
                }.start();
            }
        });

        clearBtn = (Button) findViewById(R.id.clear);
        clearBtn.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				if (transThread == null || !transThread.isAlive()) {
					mThroughputTest.clear();
				}
				clearScreen();
			}});
		
		autoConnBtn = (Button) findViewById(R.id.start);
		autoConnBtn.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				if (connectAndDisconnectThread == null) {
					String strTimes = autoConnectTimes.getText().toString().trim();
					int connectTimes = 0;
					if (strTimes == null || "".equals(strTimes)) {
						connectTimes = Integer.MAX_VALUE;
					} else {
						connectTimes = Integer.parseInt(autoConnectTimes.getText().toString().trim());
						if (connectTimes == 0) {
							connectTimes = Integer.MAX_VALUE;
						}
					}
					connectAndDisconnectThread = new ConnectAndDisconnectThread(connectTimes);
				}
				if (connectAndDisconnectThread.isAlive()) {
					autoConnBtn.setEnabled(false);
					connectAndDisconnectThread.cancel();
				} else {
					String strTimes = autoConnectTimes.getText().toString().trim();
					int connectTimes = 0;
					if (strTimes == null || "".equals(strTimes)) {
						connectTimes = Integer.MAX_VALUE;
					} else {
						connectTimes = Integer.parseInt(autoConnectTimes.getText().toString().trim());
						if (connectTimes == 0) {
							connectTimes = Integer.MAX_VALUE;
						}
					}
					connectAndDisconnectThread = new ConnectAndDisconnectThread(connectTimes);
					connectAndDisconnectThread.start();
				}
			}
		});

		settingBtn = (Button)findViewById(R.id.connectionsetting);
		settingBtn.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				setBackgroundAlpha(0.3f);
				mPopupWindow.showAtLocation(settingBtn, Gravity.BOTTOM, 0, 0);
			}
		});
        rcvBtn = (Button)findViewById(R.id.btnRcv);
        rcvBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
				if (!mSelectedDevice.isConnected()) {
					Toast.makeText(mContext, "Device is not connected", Toast.LENGTH_SHORT).show();
				} else {
					Intent intent = new Intent(context, ReceiveSpeedTestActivity.class);
					Bundle bundle = new Bundle();
					bundle.putParcelable(ReceiveSpeedTestActivity.DEVICE_OBJ, mSelectedDevice);
					intent.putExtras(bundle);
					context.startActivity(intent);
				}
            }
        });

		initPopupWindows();

		scrollView = (ScrollView) findViewById(R.id.scorllView);
		dataText = (EditText) findViewById(R.id.writeData);
		timesText = (EditText) findViewById(R.id.writeTime);
		intervalText = (EditText) findViewById(R.id.interval);
		inputText = (EditText) findViewById(R.id.input);
		speedText = (TextView) findViewById(R.id.speed);
		resultText = (TextView) findViewById(R.id.receiveData);
		myHandler = new MyHandler(this);

		resultText.setText("[Conn]:连接和断开当前设备\n" +
				"[Conn(n)]:连接断开，循环n次\n" +
				"[Send]:发送[to send]文本框中的数据\n" +
				"[Send(n)]:每次发送[data]文本框中指定大大小的数据(单位是KB),连续发送[times]次,两次发送之间间隔[/]ms");
		resultText.setTextColor(Color.GRAY);
		resultText.setTextSize(16);
		speedText.setTextColor(Color.GRAY);

		IntentFilter filter = new IntentFilter(
				BluetoothAdapter.ACTION_STATE_CHANGED);
		filter.addAction(BluetoothDevice.ACTION_ACL_CONNECTED);
		filter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED);
		mContext.registerReceiver(mReceiver, filter);
	}

	private void initPopupWindows()
	{
		LayoutInflater inflater = (LayoutInflater)mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		View popupView = inflater.inflate(R.layout.layout_setting_popup, null);
		mPopupWindow = new PopupWindow(popupView, ViewGroup.LayoutParams.MATCH_PARENT,
				ViewGroup.LayoutParams.WRAP_CONTENT, true);

		removeBondWhileDisconnectCheckBox = (CheckBox) popupView.findViewById(R.id.removeBondWhileDisconnect);

		bondBeforeConnectCheckBox = (CheckBox) popupView.findViewById(R.id.bondBeforeConnect);
		bondBeforeConnectCheckBox.setChecked(true);
		bondBeforeConnectCheckBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				mAdapter.setAutoBondBeforConnect(isChecked);
			}
		});

		autoPairReplyCheckBox = (CheckBox) popupView.findViewById(R.id.autoPairReply);
		autoPairReplyCheckBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				mAdapter.setAutoWritePincode(isChecked);
				mAdapter.setPincode("1234");
			}
		});

		enterCheckBox = (CheckBox) popupView.findViewById(R.id.checkBoxEnter);
		newLineCheckBox = (CheckBox) popupView.findViewById(R.id.checkBoxNewline);
		hexSendCheckBox = (CheckBox) popupView.findViewById(R.id.checkBoxHexSend);
		hexSendCheckBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
				if (b) {
					inputText.setText(str2HexStr(inputText.getText().toString().trim(), false));
				} else {
					inputText.setText(hexStr2Str(inputText.getText().toString().trim()));
				}
			}
		});
		hexRecvCheckBox = (CheckBox) popupView.findViewById(R.id.checkBoxRecvHex);
		hexRecvCheckBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
				if (b) {
					resultText.setText(str2HexStr(resultText.getText().toString().trim(), false));
				} else {
					resultText.setText(hexStr2Str(resultText.getText().toString().trim()));
				}
			}
		});

		sendTestDataForAutoConnected = (CheckBox)popupView.findViewById(R.id.sendTestDataForAutoConn);
		autoConnectTimes = (EditText)popupView.findViewById(R.id.autoConnTimes);
		autoConnectInterval = (EditText)popupView.findViewById(R.id.autoConnInterval);

		highPriorityConnectLE = (CheckBox)popupView.findViewById(R.id.connectLEWithHighPriority);
		highPriorityConnectLE.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				if (isChecked) {
					balancedPriorityConnectLE.setChecked(false);
					lowPowerPriorityConnectLE.setChecked(false);
				}
			}
		});

		balancedPriorityConnectLE = (CheckBox)popupView.findViewById(R.id.connectLEWithBalancedPriority);
		balancedPriorityConnectLE.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				if (isChecked) {
					highPriorityConnectLE.setChecked(false);
					lowPowerPriorityConnectLE.setChecked(false);
				}
			}
		});

		lowPowerPriorityConnectLE = (CheckBox)popupView.findViewById(R.id.connectLEWithLowPowerPriority);
		lowPowerPriorityConnectLE.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				if (isChecked) {
					highPriorityConnectLE.setChecked(false);
					balancedPriorityConnectLE.setChecked(false);
				}
			}
		});

		mPopupWindow.setFocusable(true);
		mPopupWindow.setTouchable(true);
		mPopupWindow.setOutsideTouchable(true);

		mPopupWindow.setAnimationStyle(R.style.PopupWindowAnimation);
		mPopupWindow.setBackgroundDrawable(new BitmapDrawable());
		//mPopupWindow.showAtLocation(settingBtn, Gravity.BOTTOM, 0, 0);
		mPopupWindow.setOnDismissListener(new PopupWindow.OnDismissListener() {
			@Override
			public void onDismiss() {
				setBackgroundAlpha(1.0f);
				Log.i("PresenetView", "PopupWind dismiss");
			}
		});
	}

	private void setBackgroundAlpha(float bgAlpha)
	{
		WindowManager.LayoutParams lp = ((MainActivity)mContext).getWindow().getAttributes();
		lp.alpha = bgAlpha;
		((MainActivity)mContext).getWindow().setAttributes(lp);
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
	@Override
	public void onDataReceived(BluetoothIBridgeDevice device, byte[] buffer,
			int len) {
		dealData(buffer,len);
	}

	@Override
	public void refreshScreen() {
		if (mSelectedDevice == null)
			return;
		if (mSelectedDevice.isConnected()) {
			connBtn.setText(R.string.disconnect);
			connBtn.setEnabled(true);
			autoSendBtn.setEnabled(true);
			sendBtn.setEnabled(true);
		} else {
			connBtn.setText(R.string.connect);
			connBtn.setEnabled(true);
			autoSendBtn.setEnabled(false);
			sendBtn.setEnabled(false);
		}
	}
	
	private  BroadcastReceiver mReceiver =  new BroadcastReceiver (){

		@Override
		public void onReceive(Context context, Intent intent) {
			// TODO Auto-generated method stub
			String action = intent.getAction();
			if (action.equals(BluetoothAdapter.ACTION_STATE_CHANGED)) {	
				if (intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, -1) == BluetoothAdapter.STATE_OFF) {
					mAdapter.setEnabled(true);
				}
				if (intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, -1) == BluetoothAdapter.STATE_ON) {
					//refreshScreen();
				}
			}
			if (action.equals(BluetoothDevice.ACTION_ACL_CONNECTED)){
				BluetoothDevice device = intent
				.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
			}
			if (action.equals(BluetoothDevice.ACTION_ACL_DISCONNECTED)){
				BluetoothDevice device = intent
				.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
			}
		}
		
	};

	private static class MyHandler extends Handler {
		public static final int MSG_RECEIVED_SPEED = 0x01;
		public static final int MSG_RECEIVED_STRING = 0x02;
		public static final int MESSAGE_CLEAR = 0x03;
		public static final int MSG_AUTO_WRITE_STARTED = 0x04;
		public static final int MSG_AUTO_WRITE_SPEED = 0x05;
		public static final int MSG_AUTO_WRITE_COMPLETED = 0x06;
		public static final int MSG_AUTO_CONNECT_STARTED = 0x07;
		public static final int MSG_AUTO_CONNECT = 0x08;
		public static final int MSG_AUTO_CONNECT_COMPLETED = 0x09;

		private final WeakReference<PresentView> host;

		public MyHandler(PresentView view) {
			host = new WeakReference<PresentView>(view);
		}

		@Override
		public void handleMessage(Message msg) {
			final PresentView view = host.get();
			switch (msg.what) {
				case MSG_RECEIVED_SPEED:
					String string = (String) msg.obj;
					view.setSpeedValue(string);
					break;
				case MSG_RECEIVED_STRING:
					String result = (String) msg.obj;
					view.setResultText(result, true);
					break;
				case MESSAGE_CLEAR:
					view.firstRev = false;
					view.revLength = 0;
					break;
				case MSG_AUTO_WRITE_STARTED:
					view.sendThreadStarted();
					view.setResultText("Data Send Test...\n", false);
					break;
				case MSG_AUTO_WRITE_SPEED:
					String writeSpeed = (String) msg.obj;
					if (view.mThroughputTest != null && view.mThroughputTest.size() > 0) {
						String resultText = "";
						for (int i=0; i< view.mThroughputTest.size(); i++) {
							Throughput throughput = (Throughput)view.mThroughputTest.get(i);
							if (throughput != null) {
								resultText += "[" + (i+1) + "] "  + throughput.resultText + "\n";
							}
						}
						view.setResultText(resultText,false);
					} else {
						view.setResultText(writeSpeed,false);
					}
					break;
				case MSG_AUTO_WRITE_COMPLETED:
					view.sendThreadCompleted();
					if (view.mThroughputTest != null && view.mThroughputTest.size() > 0) {
						String resultText = "";
						int totalDataLength = 0;
						long totalCostTime = 0;
						for (int i=0; i<view.mThroughputTest.size(); i++) {
							Throughput throughput = (Throughput)view.mThroughputTest.get(i);
							if (throughput != null) {
								totalDataLength += throughput.datalength;
								totalCostTime += (throughput.finishTime - throughput.startTime);
							}
						}
						if (totalDataLength > 0 && totalCostTime > 0) {
							DecimalFormat df = new DecimalFormat(".#");
							double speed = totalDataLength/((double)(totalCostTime/1000));
							resultText = "[sum]  Total:" + df.format((double)(totalCostTime/1000)) + "sec, average speed: " + df.format(speed/1000) + "KByte/s" + "\n";
							//resultText += "Data Send Test Completed.";
						}
						view.setResultText(resultText,true);
					} else {
						view.setResultText("Data Send Test Completed.\n", true);
					}
					break;
				case MSG_AUTO_CONNECT_STARTED:
					view.connectAndDisconnectThreadStarted();
					view.setResultText("Connect/Disconnect Test...\n", false);
					break;
				case MSG_AUTO_CONNECT:
					String connectResult = (String) msg.obj;
					view.setResultText(connectResult + "\n", true);
					break;
				case MSG_AUTO_CONNECT_COMPLETED:
					view.connectAndDisconnectThreadCompleted();
					view.setResultText("Connect/Disconnect Test Completed.\n", true);
					break;
			}
			super.handleMessage(msg);
		}
	}

	private void clearScreen(){
		text = "";
		myHandler.obtainMessage(MyHandler.MSG_RECEIVED_STRING, text)
		.sendToTarget();
		myHandler.obtainMessage(MyHandler.MESSAGE_CLEAR, text)
		.sendToTarget();
		myHandler.obtainMessage(MyHandler.MSG_RECEIVED_SPEED, mContext.getString(R.string.recvSpeed))
		.sendToTarget();
	}
	
	private void dealData(byte[] buffer , int len){
		String result = "";
		int length = 0;
		if(len > buffer.length){
			length = 1024;
		}else{
			length = len;
		}
		if (hexRecvCheckBox.isChecked()) {
			byte[] b = new byte[len];
			System.arraycopy(buffer, 0, b, 0, len);
			result = byte2HexStr(b, false);
		} else {
			try {
				result = new String(buffer, 0, length, "utf-8");
			} catch (UnsupportedEncodingException e) {
				e.printStackTrace();
			}
		}
		myHandler.obtainMessage(MyHandler.MSG_RECEIVED_STRING, result)
				.sendToTarget();
		if (!firstRev) {
			Date date = new Date();
			revLength = len;
			firstTime = date.getTime();
			firstRev = true;
			
			if(circleStartTime != 0){
				circleTime = date.getTime() - circleStartTime;	
			}
		} else {
			Date date = new Date();
			long currentTime = date.getTime();
			timeInterval = currentTime - firstTime;
			revLength += len;
			if(timeInterval!= 0){
				revSpeed = revLength / ((double)timeInterval / 1000 );
			}
			
			if(circleStartTime != 0){
				circleTime = date.getTime() - circleStartTime;	
			}
		}
		DecimalFormat df = new DecimalFormat(".#");
		String speedText = mContext.getString(R.string.recvSpeed)
				+ revLength + "," + df.format(revSpeed) + "Byte/s";
		myHandler.obtainMessage(MyHandler.MSG_RECEIVED_SPEED, speedText)
		.sendToTarget();
		myHandler.removeMessages(MyHandler.MESSAGE_CLEAR);
		myHandler.sendEmptyMessageDelayed(MyHandler.MESSAGE_CLEAR, 10000);
	}
	
	private void setSpeedValue(String value) {
		speedText.setText(value);
	}
	
	private void setWriteSpeedValue(String value) {
		writeText.setText(value);
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

	private void sendThreadStarted() {
		autoConnBtn.setEnabled(false);
		autoSendBtn.setText(R.string.stop);
		sendBtn.setEnabled(false);
		connBtn.setEnabled(false);
	}

	private void sendThreadCompleted() {
		autoConnBtn.setEnabled(true);
		autoSendBtn.setText(R.string.testSpeed);
		autoSendBtn.setEnabled(true);
		refreshScreen();
	}

	private void connectAndDisconnectThreadStarted() {
		autoConnBtn.setText(R.string.stop);
		sendBtn.setEnabled(false);
		connBtn.setEnabled(false);
		autoSendBtn.setEnabled(false);
	}

	private void connectAndDisconnectThreadCompleted() {
		autoSendBtn.setEnabled(true);
		autoConnBtn.setText(R.string.start);
		autoConnBtn.setEnabled(true);
		refreshScreen();
	}
	
	private class TransmissionThread extends Thread {
		private final int count;
		private final int time;
		private boolean mCancelFlag = false;
		
		public TransmissionThread(int Count, int Time){
			count = Count;
			time = Time;
		}
		
		public void run(){
			byte[] data = new byte[MAX_BYTE * count];
			Random random = new Random();
			for (int i = 0; i < data.length; i++) {
				int is = random.nextInt(5);
				data[i] = (byte)(0x35 + is);
			}
			long start = System.currentTimeMillis();
			long finish;
			float speed;
			String writeSpeed;
			Throughput throughput = new Throughput();
			if (throughput != null) {
				throughput.startTime = start;
				throughput.type = Throughput.THROUGHPUT_TYPE_TX;
				throughput.status = Throughput.THROUGHPUT_STATUS_ONGOING;
				throughput.datalength = 0;
				mThroughputTest.add(throughput);
			}
			if (mSelectedDevice.getDeviceType() == BluetoothIBridgeDevice.DEVICE_TYPE_BLE) {
				mAdapter.bleRequestConnectionPriority(mSelectedDevice, BluetoothGatt.CONNECTION_PRIORITY_HIGH);
			}
			myHandler.obtainMessage(MyHandler.MSG_AUTO_WRITE_STARTED, null).sendToTarget();
			for (int i = 0; (i < time) && (mCancelFlag == false); i++) {
				if (null != mSelectedDevice && mAdapter != null
						&& mSelectedDevice.isConnected()) {
					mAdapter.send(mSelectedDevice, data, data.length);
					finish = System.currentTimeMillis();
					throughput.datalength += data.length;
					speed = (data.length * i)/((float)(finish - start)/1000);
					DecimalFormat df=new DecimalFormat(".#");
					writeSpeed = (i+1) + "/" + time + "," + df.format(speed/1000) + "KByte/s";
					throughput.resultText = writeSpeed;
					myHandler.obtainMessage(MyHandler.MSG_AUTO_WRITE_SPEED, writeSpeed)
							.sendToTarget();

					if (!intervalText.getText().toString().isEmpty() && Integer.parseInt(intervalText.getText().toString().trim()) > 0) {
						try {
							Thread.sleep(Integer.parseInt(intervalText.getText().toString().trim()));
						} catch (InterruptedException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}
				} else {
					if (transThread != null && transThread.isAlive()) {
						transThread.cancel();
					}
					break;
				}
			}
			if (throughput != null) {
				throughput.finishTime = System.currentTimeMillis();
				throughput.status = Throughput.THROUGHPUT_STATUS_FINSIHED;
			}
			transThread = null;
			myHandler.obtainMessage(MyHandler.MSG_AUTO_WRITE_COMPLETED, null).sendToTarget();
		}

		public void cancel() {
			mCancelFlag = true;
		}
	}

	private class ConnectAndDisconnectThread extends Thread {
		private int mTimes = 0;
		private boolean mCancelFlag = false;

		public ConnectAndDisconnectThread(int times) {
			mTimes = times;
		}

		public void run() {
			String connectResult;
			long startTime = 0, endTime = 0;

			myHandler.obtainMessage(MyHandler.MSG_AUTO_CONNECT_STARTED, null).sendToTarget();
			for (int i = 0; (i < mTimes) && (mCancelFlag == false); i++) {
				//Disconnect
				if (mSelectedDevice.isConnected()) {
					mAdapter.disconnectDevice(mSelectedDevice);
					EventObjectUtils.waitEvent(EventObjectUtils.SAMPLE_EVENT_1, 10000);
				}
				try {
					Thread.sleep(Integer.parseInt(autoConnectInterval.getText().toString().trim()));
				} catch (Exception e) {
					e.printStackTrace();
				}
				//Connect
				startTime = System.currentTimeMillis();
				if(mAdapter.connectDevice(mSelectedDevice)) {
					EventObjectUtils.waitEvent(EventObjectUtils.SAMPLE_EVENT_0, 30000);
					if (mSelectedDevice.isConnected()) {
						endTime = System.currentTimeMillis();
						connectResult = (i+1) + ":success, cost=" + (endTime - startTime);
						if (sendTestDataForAutoConnected.isChecked()) {
							//Send Test data for auto connect
							int count = Integer.parseInt(dataText.getText().toString().trim());
							byte[] data = new byte[MAX_BYTE * count];
							Random random = new Random();
							for (int j = 0; j < data.length; j++) {
								int is = random.nextInt(5);
								data[j] = (byte)(0x35 + is);
							}
							if (null != mSelectedDevice && mAdapter != null
									&& mSelectedDevice.isConnected()) {
								mAdapter.send(mSelectedDevice, data, data.length);
							}
						}
					} else {
						connectResult = (i+1) + ":timeut";
					}
				} else {
					connectResult = (i+1) + ":fail";
				}
				myHandler.obtainMessage(MyHandler.MSG_AUTO_CONNECT, connectResult).sendToTarget();
			}
			myHandler.obtainMessage(MyHandler.MSG_AUTO_CONNECT_COMPLETED, null).sendToTarget();
		}

		public void cancel() {
			if (isAlive()) {
				mCancelFlag = true;
			}
		}

		public void connectCompleted() {
			if (isAlive()) {
				EventObjectUtils.notifyEvent(EventObjectUtils.SAMPLE_EVENT_0);
			}
		}

		public void disconnected() {
			if (isAlive()) {
				EventObjectUtils.notifyEvent(EventObjectUtils.SAMPLE_EVENT_1);
			}
		}
	}

	public void setDevice(BluetoothIBridgeDevice dev) {
		mSelectedDevice = dev;
		if (dev.getDeviceType() == BluetoothIBridgeDevice.DEVICE_TYPE_BLE) {
			intervalText.setText("100");
		}
		if (dev.getDeviceType() == BluetoothIBridgeDevice.DEVICE_TYPE_CLASSIC) {
			intervalText.setText("0");
			//intervalText.setEnabled(false);
		}
		refreshScreen();
	}

	public void setBluetoothAdapter(BluetoothIBridgeAdapter adapter) {
		if (adapter != null) {
			mAdapter = adapter;
			mAdapter.registerDataReceiver(this);
			mAdapter.registerEventReceiver(this);
		} else if (mAdapter != null) {
			mAdapter.unregisterDataReceiver(this);
			mAdapter.unregisterEventReceiver(this);
			mAdapter = null;
		}
	}

	public void onDestroy() {
		mContext.unregisterReceiver(mReceiver);
		if (mAdapter != null) {
			mAdapter.unregisterDataReceiver(this);
			mAdapter.unregisterEventReceiver(this);
		}
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
	public void onDeviceFound(BluetoothIBridgeDevice dev) {}

	@Override
	public void onDiscoveryFinished() {}

	@Override
	public void onDeviceConnectFailed(BluetoothIBridgeDevice device,String exceptionMsg) {
		if ((connectAndDisconnectThread != null) && (connectAndDisconnectThread.isAlive())) {
			connectAndDisconnectThread.connectCompleted();
		} else {
			refreshScreen();
		}
	}

	@Override
	public void onDeviceConnected(BluetoothIBridgeDevice device) {
		if ((connectAndDisconnectThread != null) && (connectAndDisconnectThread.isAlive())) {
			connectAndDisconnectThread.connectCompleted();
		} else {
			refreshScreen();
			mThroughputTest.clear();
			if (mSelectedDevice != null) {
				if (mSelectedDevice.getDeviceType() == BluetoothIBridgeDevice.DEVICE_TYPE_BLE){
					if (isServiceExist(mSelectedDevice, IVT_GATT_SERVICE_UUID.toString())) {
						//mAdapter.bleSetMtu(mSelectedDevice, 20);
						mAdapter.bleSetTargetUUIDs(mSelectedDevice, IVT_GATT_SERVICE_UUID.toString()
								, IVT_GATT_NOTIFY_CHARC_UUID.toString(), IVT_GATT_WRITE_CHARC_UUID.toString());
					} else if (isServiceExist(mSelectedDevice, ISSC_GATT_SERVICE_UUID.toString())){
							mAdapter.bleSetTargetUUIDs(mSelectedDevice, ISSC_GATT_SERVICE_UUID.toString()
								, ISSC_GATT_NOTIFY_CHARC_UUID.toString(), ISSC_GATT_WRITE_CHARC_UUID.toString());
					}

				}
			}
		}

		if (device.getDeviceType() == BluetoothIBridgeDevice.DEVICE_TYPE_BLE) {
			if (highPriorityConnectLE.isChecked()) {
				mAdapter.bleRequestConnectionPriority(device, BluetoothGatt.CONNECTION_PRIORITY_HIGH);
			} else if (balancedPriorityConnectLE.isChecked()) {
				mAdapter.bleRequestConnectionPriority(device, BluetoothGatt.CONNECTION_PRIORITY_BALANCED);
			} else if (lowPowerPriorityConnectLE.isChecked()) {
				mAdapter.bleRequestConnectionPriority(device, BluetoothGatt.CONNECTION_PRIORITY_LOW_POWER);
			}
		}

	}

	@Override
	public void onDeviceDisconnected(BluetoothIBridgeDevice device,String exceptionMsg) {
		firstRev = false;
		revLength = 0;
		if ((connectAndDisconnectThread != null) && (connectAndDisconnectThread.isAlive())) {
			connectAndDisconnectThread.disconnected();
		} else {
			refreshScreen();
		}
		if (removeBondWhileDisconnectCheckBox.isChecked()) {
			mSelectedDevice.removeBond();
		}
		mThroughputTest.clear();
	}
	
	@Override
	public void onWriteFailed(BluetoothIBridgeDevice deivce, String exceptionMsg) {
		// TODO Auto-generated method stub
	}

	@Override
	public void onLeServiceDiscovered(BluetoothIBridgeDevice device,
									  String exceptionMsg) {
	}
	
	public static void logf(String tag, String content) {
		String dir = Environment.getExternalStorageDirectory().toString()
				+ iBridge_PATH;
		File d = new File(dir);
		if (!d.exists()) {
			d.mkdir();
		}

		String path = Environment.getExternalStorageDirectory().toString()
				+ iBridge_PATH + "/" + LOG_FILE;
		File f = new File(path);
		if (!f.exists()) {
			try {
				f.createNewFile();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		FileOutputStream fs;
		try {
			fs = new FileOutputStream(f, true);
			fs.write(content.getBytes());
			fs.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	/**
	 * 字符串转换成十六进制字符串
	 * @param String str 待转换的ASCII字符串
	 * @return String 每个Byte之间空格分隔，如: [61 6C 6B]
	 */
	public static String str2HexStr(String str, boolean space)
	{

		char[] chars = "0123456789ABCDEF".toCharArray();
		StringBuilder sb = new StringBuilder("");
		byte[] bs = str.getBytes();
		int bit;

		for (int i = 0; i < bs.length; i++)
		{
			bit = (bs[i] & 0x0f0) >> 4;
			sb.append(chars[bit]);
			bit = bs[i] & 0x0f;
			sb.append(chars[bit]);
			if (space) {
				sb.append(' ');
			}
		}
		return sb.toString().trim();
	}

	public static boolean hexStrCheck(String hexStr)
	{
		String str = "0123456789ABCDEF";
		char[] hexs = hexStr.toCharArray();
		int i = 0;

		for (i = 0; i < hexs.length; i++) {
			if (str.indexOf(hexs[i]) == -1) {
				break;
			}
		}
		return (i == hexs.length);
	}

	/**
	 * 十六进制转换字符串
	 * @param String str Byte字符串(Byte之间无分隔符 如:[616C6B])
	 * @return String 对应的字符串
	 */
	public static String hexStr2Str(String hexStr)
	{
		String str = "0123456789ABCDEF";
		char[] hexs = hexStr.toCharArray();
		byte[] bytes = new byte[hexStr.length() / 2];
		int n;

		for (int i = 0; i < bytes.length; i++)
		{
			n = str.indexOf(hexs[2 * i]) * 16;
			n += str.indexOf(hexs[2 * i + 1]);
			bytes[i] = (byte) (n & 0xff);
		}
		return new String(bytes);
	}

	/**
	 * bytes转换成十六进制字符串
	 * @param byte[] b byte数组
	 * @return String 每个Byte值之间空格分隔
	 */
	public static String byte2HexStr(byte[] b, boolean space)
	{
		String stmp="";
		StringBuilder sb = new StringBuilder("");
		for (int n=0;n<b.length;n++)
		{
			stmp = Integer.toHexString(b[n] & 0xFF);
			sb.append((stmp.length()==1)? "0"+stmp : stmp);
			if (space) {
				sb.append(" ");
			}
		}
		return sb.toString().toUpperCase().trim();
	}

	/**
	 * bytes字符串转换为Byte值
	 * @param String src Byte字符串，每个Byte之间没有分隔符
	 * @return byte[]
	 */
	public static byte[] hexStr2Bytes(String src)
	{
		int m=0,n=0;
		int l=src.length()/2;
		System.out.println(l);
		byte[] ret = new byte[l];
		for (int i = 0; i < l; i++)
		{
			m=i*2+1;
			n=m+1;
			ret[i] = (byte)Integer.parseInt((src.substring(i*2, m) + src.substring(m,n)), 16);
		}
		return ret;
	}

	/**
	 * String的字符串转换成unicode的String
	 * @param String strText 全角字符串
	 * @return String 每个unicode之间无分隔符
	 * @throws Exception
	 */
	public static String strToUnicode(String strText)
			throws Exception
	{
		char c;
		StringBuilder str = new StringBuilder();
		int intAsc;
		String strHex;
		for (int i = 0; i < strText.length(); i++)
		{
			c = strText.charAt(i);
			intAsc = (int) c;
			strHex = Integer.toHexString(intAsc);
			if (intAsc > 128)
				str.append("\\u" + strHex);
			else // 低位在前面补00
				str.append("\\u00" + strHex);
		}
		return str.toString();
	}

	/**
	 * unicode的String转换成String的字符串
	 * @param String hex 16进制值字符串 （一个unicode为2byte）
	 * @return String 全角字符串
	 */
	public static String unicodeToString(String hex)
	{
		int t = hex.length() / 6;
		StringBuilder str = new StringBuilder();
		for (int i = 0; i < t; i++)
		{
			String s = hex.substring(i * 6, (i + 1) * 6);
			// 高位需要补上00再转
			String s1 = s.substring(2, 4) + "00";
			// 低位直接转
			String s2 = s.substring(4);
			// 将16进制的string转为int
			int n = Integer.valueOf(s1, 16) + Integer.valueOf(s2, 16);
			// 将int转换为字符
			char[] chars = Character.toChars(n);
			str.append(new String(chars));
		}
		return str.toString();
	}
}
