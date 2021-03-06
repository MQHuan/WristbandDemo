package com.realsil.android.wristbanddemo.utility;

/*
 * Copyright (C) 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Handler;
import android.os.Message;
import android.os.Vibrator;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;
import android.view.WindowManager;

import com.realsil.android.wristbanddemo.R;
import com.realsil.android.wristbanddemo.applicationlayer.ApplicationLayer;
import com.realsil.android.wristbanddemo.applicationlayer.ApplicationLayerAlarmPacket;
import com.realsil.android.wristbanddemo.applicationlayer.ApplicationLayerAlarmsPacket;
import com.realsil.android.wristbanddemo.applicationlayer.ApplicationLayerCallback;
import com.realsil.android.wristbanddemo.applicationlayer.ApplicationLayerFacSensorPacket;
import com.realsil.android.wristbanddemo.applicationlayer.ApplicationLayerRecentlySportPacket;
import com.realsil.android.wristbanddemo.applicationlayer.ApplicationLayerSitPacket;
import com.realsil.android.wristbanddemo.applicationlayer.ApplicationLayerSleepItemPacket;
import com.realsil.android.wristbanddemo.applicationlayer.ApplicationLayerSleepPacket;
import com.realsil.android.wristbanddemo.applicationlayer.ApplicationLayerSportItemPacket;
import com.realsil.android.wristbanddemo.applicationlayer.ApplicationLayerSportPacket;
import com.realsil.android.wristbanddemo.applicationlayer.ApplicationLayerTodaySportPacket;
import com.realsil.android.wristbanddemo.applicationlayer.ApplicationLayerTodaySumSportPacket;
import com.realsil.android.wristbanddemo.applicationlayer.ApplicationLayerUserPacket;
import com.realsil.android.wristbanddemo.backgroundscan.BackgroundScanAutoConnected;
import com.realsil.android.wristbanddemo.battery.BatteryService;
import com.realsil.android.wristbanddemo.bmob.BmobControlManager;
import com.realsil.android.wristbanddemo.bmob.BmobDataSyncManager;
import com.realsil.android.wristbanddemo.dfu.DfuService;
import com.realsil.android.wristbanddemo.greendao.SleepData;
import com.realsil.android.wristbanddemo.greendao.SportData;
import com.realsil.android.wristbanddemo.immediatealert.ImmediateAlertService;
import com.realsil.android.wristbanddemo.linkloss.LinkLossService;
import com.realsil.android.wristbanddemo.notifybroadcast.NotificationReceive;
import com.realsil.android.wristbanddemo.notifybroadcast.NotifyBroadcastReceive;
import com.realsil.android.wristbanddemo.sport.SportSubData;

import cn.bmob.v3.exception.BmobException;

public class WristbandManager implements BatteryService.OnServiceListener,NotifyBroadcastReceive.OnBroadcastListener,LinkLossService.OnServiceListener,DfuService.OnServiceListener{
    // Log
    private final static String TAG = "WristbandManager";
    private final static boolean D = true;

	public static final String ACTION_SYNC_DATA_OK = "ACTION_SYNC_DATA_OK";

    private String mDeviceName;

	private String mDeviceAddress;

    // Application Layer Object
    ApplicationLayer mApplicationLayer;

	private boolean isConnected = false;

	private static final String ENABLED_NOTIFICATION_LISTENERS = "enabled_notification_listeners";

    // Message
    public static final int MSG_STATE_CONNECTED = 0;
    public static final int MSG_STATE_DISCONNECTED = 1;
    public static final int MSG_WRIST_STATE_CHANGED = 2;
    public static final int MSG_RECEIVE_SPORT_INFO = 3;//characteristic read
	public static final int MSG_RECEIVE_SLEEP_INFO = 4;
	public static final int MSG_RECEIVE_HISTORY_SYNC_BEGIN = 5;
	public static final int MSG_RECEIVE_HISTORY_SYNC_END = 6;
	public static final int MSG_RECEIVE_ALARMS_INFO = 7;
	public static final int MSG_RECEIVE_NOTIFY_MODE_SETTING = 8;
	public static final int MSG_RECEIVE_LONG_SIT_SETTING = 9;
	public static final int MSG_RECEIVE_FAC_SENSOR_INFO = 10;
	public static final int MSG_RECEIVE_DFU_VERSION_INFO = 11;
	public static final int MSG_RECEIVE_DEVICE_NAME_INFO = 12;
	public static final int MSG_RECEIVE_BATTERY_INFO = 13;


    public static final int MSG_ERROR = 20;

    // Wristband state manager
    public int mWristState;
	public static final int STATE_WRIST_INITIAL                = 0;
    public static final int STATE_WRIST_LOGING                 = 1;
    public static final int STATE_WRIST_BONDING                = 2;
    public static final int STATE_WRIST_LOGIN                  = 3;
    public static final int STATE_WRIST_SYNC_DATA              = 4;
    public static final int STATE_WRIST_SYNC_HISTORY_DATA      = 5;
	public static final int STATE_WRIST_ENTER_TEST_MODE        = 6;
    
    private boolean mErrorStatus;
    public final int ERROR_CODE_NO_LOGIN_RESPONSE_COME = 1;
    public final int ERROR_CODE_BOND_ERROR = 2;
    public final int ERROR_CODE_COMMAND_SEND_ERROR = 3;
    

	// Token Key
	private final byte[] TEST_TOKEN_KEY = {(byte)0x01, (byte)0x01, (byte)0x01, (byte)0x01, (byte)0x01, (byte)0x01,
			(byte)0x01, (byte)0x01, (byte)0x01, (byte)0x01, (byte)0x01, (byte)0x01,
			(byte)0x01, (byte)0x01, (byte)0x01, (byte)0x01, (byte)0x01, (byte)0x01,
			(byte)0x01, (byte)0x01, (byte)0x01, (byte)0x01, (byte)0x01, (byte)0x01,
			(byte)0x01, (byte)0x01, (byte)0x01, (byte)0x01, (byte)0x01, (byte)0x01,
			(byte)0x01, (byte)0x01, (byte)0x01, (byte)0x01, (byte)0x01, (byte)0x01};
    
    // green dao
    private GlobalGreenDAO mGlobalGreenDAO;

    
    // Use to manager request and response transaction
  	private boolean isResponseCome;
  	private final Object mRequestResponseLock = new Object();
  	private final int MAX_REQUEST_RESPONSE_TRANSACTION_WAIT_TIME = 30000;
	private boolean isNeedWaitForResponse;
  	
  	// Use to manager command send transaction
	private volatile boolean isInSendCommand;
   	private volatile boolean isCommandSend;
   	private volatile boolean isCommandSendOk;
   	private final Object mCommandSendLock = new Object();
   	private final int MAX_COMMAND_SEND_WAIT_TIME = 15000;
    
  	// object
    private static WristbandManager mInstance;
    private static Context mContext;

	ArrayList<WristbandManagerCallback> mCallbacks;

	private NotifyBroadcastReceive mNotifyBroadcastReceive;

	// MyHandler
	//MyHandler mHandler;

	public static void initial(Context context) {
        if(D) Log.d(TAG, "initial()");
        mInstance = new WristbandManager();
        mContext = context;
        
        // green dao
        mInstance.mGlobalGreenDAO = GlobalGreenDAO.getInstance();
		mInstance.isConnected = false;
        
        // initial Wristband Application Layer and register the callback
        mInstance.mApplicationLayer = new ApplicationLayer(context, mInstance.mApplicationCallback);

		// Initial Callback list
		mInstance.mCallbacks = new ArrayList<>();

		// Initial State
		mInstance.mWristState = STATE_WRIST_INITIAL;

    }
    public static WristbandManager getInstance() { 
        return mInstance;  
    }
	public void close() {
		if(D) Log.d(TAG, "close()");
		// be careful here!!!!
		isConnected = false;

		mCallbacks.clear();
		mApplicationLayer.close();
		// close all wait lock
		synchronized(mRequestResponseLock) {
			isResponseCome = false;
			isNeedWaitForResponse = false;
			mRequestResponseLock.notifyAll();
		}


		synchronized(mCommandSendLock) {
			isCommandSend = false;
			isCommandSendOk = false;
			isInSendCommand = false;
			mCommandSendLock.notifyAll();
		}
		// unregister call back.
		unregisterNotifyBroadcast();
	}

	public boolean isConnect() {
		if(D) Log.d(TAG, "isConnected: " + isConnected);
		return isConnected;
	}

	public String getBluetoothAddress() {
		return mDeviceAddress;
	}


	public void setBluetoothAddress(String mDeviceAddress) {
		this.mDeviceAddress = mDeviceAddress;
	}
	public void registerCallback(WristbandManagerCallback callback) {
		if(!mCallbacks.contains(callback)) {
			mCallbacks.add(callback);
		}
	}
	public boolean isCallbackRegisted(WristbandManagerCallback callback) {
		return mCallbacks.contains(callback);
	}
	public void unRegisterCallback(WristbandManagerCallback callback) {
		if(mCallbacks.contains(callback)) {
			mCallbacks.remove(callback);
		}
	}

    /**
     * Connect to the wristband.
     * 
     * */
    public void Connect(String address, WristbandManagerCallback callback) {
        if(D) Log.d(TAG, "Connect to: " + address);
		// register callback
		//mCallbacks.add(callback);
		registerCallback(callback);
		//mCallback = callback;

		mDeviceAddress = address;
		//HandlerThread handlerThread = new HandlerThread("handler_thread");
		//handlerThread.start();
		//mHandler = new MyHandler(handlerThread.getLooper());
		// connect to the device
        mApplicationLayer.connect(address);

		// Add first initial flag
		SPWristbandConfigInfo.setFirstInitialFlag(mContext, true);
		// close all.
		if(mBatteryService != null) {
			mBatteryService.close();
			mImmediateAlertService.close();
			mLinkLossService.close();
			mDfuService.close();
			if(mNotifyBroadcastReceive != null) {
				mNotifyBroadcastReceive.close();
			}
		}
		// Extend service
		mBatteryService = new BatteryService(mDeviceAddress, this);
		mImmediateAlertService = new ImmediateAlertService(mDeviceAddress);
		mLinkLossService = new LinkLossService(mDeviceAddress, this);
		mDfuService = new DfuService(mDeviceAddress, this);

		// Register Broadcast
		registerNotifyBroadcast();

		if(isInAlarm) {
			stopAlarm();
		}
		// Alert
		mVibrator = (Vibrator) mContext.getSystemService(mContext.VIBRATOR_SERVICE);
		mMediaPlayer = new MediaPlayer();
		/*
		mMediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
			@Override
			public void onCompletion(MediaPlayer mp) {

			}
		});*/
    }

    public void StartLoginProcess(final String id) {
		new Thread(new Runnable() {
			@Override
			public void run() {
				// update state
				UpdateWristState(STATE_WRIST_LOGING);
				// Request to login
				if(RequestLogin(id) != true) {
					if(mErrorStatus != true) {
						// update state
						UpdateWristState(STATE_WRIST_BONDING);
						Log.d("onBondCmdRequestLogin","请求绑定命令");
						// Request to bond
						if(RequestBond(id) == true) {
							// do all the setting work.
							if(RequestSetNeedInfo()) {
								// update state
								UpdateWristState(STATE_WRIST_LOGIN);
							}
						} else {
							// some thing error
							SendErrorMessage(ERROR_CODE_BOND_ERROR);
							if(D) Log.e(TAG, "long time no login response, do disconnect");
							return;
						}
					} else {
						// some thing error
						SendErrorMessage(ERROR_CODE_NO_LOGIN_RESPONSE_COME);
						if(D) Log.e(TAG, "long time no login response, do disconnect");
						return;
					}
				} else {
					//自动重连会触发
					//if(syncNotifySetting()) {
						// time sync
						if (SetTimeSync()) {
							if(SetPhoneOS()) {
								// update state
								UpdateWristState(STATE_WRIST_LOGIN);
							}
						}
					//}
				}
			}
		}).start();

    }
	private boolean syncNotifySetting() {
		// initial error status
		mErrorStatus = false;
		isResponseCome = false;
		if(SendNotifyModeRequest()) {
			// wait for a while the remote response
			synchronized(mRequestResponseLock) {
				if(isResponseCome != true) {
					try {
						// wait a while
						if(D) Log.d(TAG, "wait the notify setting response come, wait for: " + MAX_REQUEST_RESPONSE_TRANSACTION_WAIT_TIME + "ms");
						mRequestResponseLock.wait(MAX_REQUEST_RESPONSE_TRANSACTION_WAIT_TIME);
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}

			if(isResponseCome != true) {
				if(D) Log.e(TAG, "wait the notify setting response come failed");

				mErrorStatus = true;
				return false;
			}

			// initial error status
			mErrorStatus = false;
			isResponseCome = false;
			if(SendLongSitRequest()) {
				// wait for a while the remote response
				synchronized(mRequestResponseLock) {
					if(isResponseCome != true) {
						try {
							// wait a while
							if(D) Log.d(TAG, "wait the long sit setting response come, wait for: " + MAX_REQUEST_RESPONSE_TRANSACTION_WAIT_TIME + "ms");
							mRequestResponseLock.wait(MAX_REQUEST_RESPONSE_TRANSACTION_WAIT_TIME);
						} catch (InterruptedException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}
				}
				if(isResponseCome != true) {
					if(D) Log.e(TAG, "wait the long sit setting response come failed");
					mErrorStatus = true;
					return false;
				}
				return true;
			}
		}
		return false;
	}

	public boolean RequestSendLongsitRequestSync() {
		// initial error status
		mErrorStatus = false;
		isResponseCome = false;
		if(SendLongSitRequest()) {
			// wait for a while the remote response
			synchronized(mRequestResponseLock) {
				if(isResponseCome != true) {
					try {
						// wait a while
						if(D) Log.d(TAG, "wait the long sit setting response come, wait for: " + MAX_REQUEST_RESPONSE_TRANSACTION_WAIT_TIME + "ms");
						mRequestResponseLock.wait(MAX_REQUEST_RESPONSE_TRANSACTION_WAIT_TIME);
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}
			if(isResponseCome != true) {
				if(D) Log.e(TAG, "wait the long sit setting response come failed");
				mErrorStatus = true;
				return false;
			}
			return true;
		}
		return false;
	}

	private boolean RequestSetNeedInfo() {
		boolean enableCall = SPWristbandConfigInfo.getNotifyCallFlag(mContext);
		boolean enableMes = SPWristbandConfigInfo.getNotifyMessageFlag(mContext);
		boolean enableQQ = SPWristbandConfigInfo.getNotifyQQFlag(mContext);
		boolean enableWechat = SPWristbandConfigInfo.getNotifyWechatFlag(mContext);

		//ApplicationLayerSitPacket sit = new ApplicationLayerSitPacket((byte) 0x01, 6123, 57, 16, 17, (byte) (ApplicationLayer.REPETITION_SUN | ApplicationLayer.REPETITION_FRI));
		// set time sync
		if(SetUserProfile()) {
			if(SetTargetStep()) {
				if (SetTimeSync()) {
					if (SetPhoneOS()) {
						if(SendSyncTodayStepCommand()) {
							if(SendSyncTodayNearlyOffsetStepCommand()) {
								//if (SetNotifyMode(enableCall ? ApplicationLayer.CALL_NOTIFY_MODE_ON : ApplicationLayer.CALL_NOTIFY_MODE_OFF)) {
								//if (SetNotifyMode(enableMes ? ApplicationLayer.CALL_NOTIFY_MODE_ENABLE_MESSAGE : ApplicationLayer.CALL_NOTIFY_MODE_DISABLE_MESSAGE)) {
								//if (SetNotifyMode(enableQQ ? ApplicationLayer.CALL_NOTIFY_MODE_ENABLE_QQ : ApplicationLayer.CALL_NOTIFY_MODE_DISABLE_QQ)) {
								//if (SetNotifyMode(enableWechat ? ApplicationLayer.CALL_NOTIFY_MODE_ENABLE_WECHAT : ApplicationLayer.CALL_NOTIFY_MODE_DISABLE_WECHAT)) {
								if (D) Log.e(TAG, "all set is ok!");
								return true;
								//}
								//}
								//}
								//}
							}
						}
					}
				}
			}
		}
		return false;
	}

	/**
	 * Set the name
	 *
	 * @param name 		the name
	 */
	public void setDeviceName(String name) {
		if(D) Log.d(TAG, "set name, name: " + name);
		initialCommandSend();

		mApplicationLayer.setDeviceName(name);

		isInSendCommand = false;

		SPWristbandConfigInfo.setInfoKeyValue(mContext, getBluetoothAddress(), name);
	}

	/**
	 * Get the name
	 *
	 */
	public boolean getDeviceName() {
		if(D) Log.d(TAG, "getDeviceName");
		initialCommandSend();

		mApplicationLayer.getDeviceName();

		return waitCommandSend();
	}

	/**
	 * Use to sync the notify mode
	 * @return	the operate result
	 * */
	public boolean SetNotifyMode(byte mode) {
		if(D) Log.d(TAG, "SetNotifyMode, mode: " + mode);
		initialCommandSend();

		// Try to set notify mode
		mApplicationLayer.SettingCmdCallNotifySetting(mode);

		return waitCommandSend();
	}

	/**
	 * Use to request current notify mode
	 * @return	the operate result
	 * */
	public boolean SendNotifyModeRequest() {
		if(D) Log.d(TAG, "SendNotifyModeRequest");
		initialCommandSend();
		// Need add this
		isNeedWaitForResponse = true;
		// Try to request notify mode
		mApplicationLayer.SettingCmdRequestNotifySwitchSetting();

		return waitCommandSend();
	}

    /**
     * Use to start/stop data sync.
     *
     * @param enable    start or stop data sync.
     * @return	the operate result
     * */
    public boolean SetDataSync(boolean enable) {
        if(D) Log.d(TAG, "SetDataSync()");
		Log.d("SetDataSync()","发送同步数据的指令");
        if(mWristState != STATE_WRIST_LOGIN && mWristState != STATE_WRIST_SYNC_DATA) {
            if(D) Log.e(TAG, "SetDataSync failed, with error state: " + mWristState);
            return false;
        }

        initialCommandSend();

        // Try to start/stop data sync
        if(enable) {
            //数据实时同步设置
            mApplicationLayer.SportDataCmdSyncSetting(ApplicationLayer.SPORT_DATA_SYNC_MODE_ENABLE);
			if(!waitCommandSend()) {
				return false;
			}
			initialCommandSend();
			mApplicationLayer.SportDataCmdRequestData();

            UpdateWristState(STATE_WRIST_SYNC_DATA);
        } else {
            mApplicationLayer.SportDataCmdSyncSetting(ApplicationLayer.SPORT_DATA_SYNC_MODE_DISABLE);

            UpdateWristState(STATE_WRIST_LOGIN);
        }

		return waitCommandSend();
    }

	/**
	 * Use to send data request.
	 *
	 * @return	the operate result
	 * */
	public boolean SendDataRequest() {
		if(D) Log.d(TAG, "SendDataRequest()");
		if(mWristState != STATE_WRIST_LOGIN && mWristState != STATE_WRIST_SYNC_DATA) {
			if(D) Log.e(TAG, "StartDataSync failed, with error state: " + mWristState);
			return false;
		}

		initialCommandSend();

		isNeedWaitForResponse = true;
		mApplicationLayer.SportDataCmdRequestData();

		return waitCommandSend();
	}
	/**
	 * Use to enable/disable the long sit set
	 * @return	the operate result
	 * */
	public boolean SetLongSit(boolean enable) {
		if(D) Log.d(TAG, "SetLongSit(), enable: " + enable);
		initialCommandSend();
		ApplicationLayerSitPacket sit;
		if(enable) {
			sit = new ApplicationLayerSitPacket(ApplicationLayer.LONG_SIT_CONTROL_ENABLE
					, 0, SPWristbandConfigInfo.getLongSitAlarmTime(mContext), 0, 0, (byte) 0);
		} else {
			sit = new ApplicationLayerSitPacket(ApplicationLayer.LONG_SIT_CONTROL_DISABLE
					, 0, 0, 0, 0, (byte) 0);
		}
		// Try to set long sit
		mApplicationLayer.SettingCmdLongSitSetting(sit);

		return waitCommandSend();
	}

    /**
     * Use to sync the long sit set
     * @return	the operate result
     * */
    public boolean SetLongSit(ApplicationLayerSitPacket sit) {
    	if(D) Log.d(TAG, "SetLongSit()");
    	initialCommandSend();
    	
    	// Try to set long sit
        mApplicationLayer.SettingCmdLongSitSetting(sit);

		return waitCommandSend();
    }


	/**
	 * Use to request current long sit set
	 * @return	the operate result
	 * */
	public boolean SendLongSitRequest() {
		if(D) Log.d(TAG, "SendLongSitRequest()");
		initialCommandSend();
		// Need add this
		isNeedWaitForResponse = true;

		// Try to set long sit
		mApplicationLayer.SettingCmdRequestLongSitSetting();

		return waitCommandSend();
	}
	/**
	 * Use to sync the user profile, use local info
	 * @return	the operate result
	 * */
	public boolean SetUserProfile() {
		if(D) Log.d(TAG, "SetUserProfile()");
		initialCommandSend();
		boolean sex = SPWristbandConfigInfo.getGendar(mContext);
		int age = SPWristbandConfigInfo.getAge(mContext);
		int height = SPWristbandConfigInfo.getHeight(mContext);
		int weight = SPWristbandConfigInfo.getWeight(mContext);

		ApplicationLayerUserPacket user = new ApplicationLayerUserPacket(sex, age, height, weight);
		// Try to set user profile
		mApplicationLayer.SettingCmdUserSetting(user);

		return waitCommandSend();
	}
    
    /**
     * Use to sync the user profile
     * @return	the operate result
     * */
    public boolean SetUserProfile(ApplicationLayerUserPacket user) {
    	if(D) Log.d(TAG, "SetUserProfile()");
    	initialCommandSend();
    	
    	// Try to set user profile
        mApplicationLayer.SettingCmdUserSetting(user);

		return waitCommandSend();
    }

	/**
	 * Use to sync the target step, user local info
	 * @return	the operate result
	 * */
	public boolean SetTargetStep() {
		initialCommandSend();

		int step = SPWristbandConfigInfo.getTotalStep(mContext);
		if(D) Log.d(TAG, "SetTargetStep, step: " + step);

		// Try to set step
		mApplicationLayer.SettingCmdStepTargetSetting(step);

		return waitCommandSend();
	}

    /**
     * Use to sync the target step
     * @return	the operate result
     * */
    public boolean SetTargetStep(long step) {
    	if(D) Log.d(TAG, "SetTargetStep, step: " + step);
    	initialCommandSend();
    	
    	// Try to set step
        mApplicationLayer.SettingCmdStepTargetSetting(step);

		return waitCommandSend();
    }

	/**
	 * Use to set the phone os
	 * @return	the operate result
	 * */
	public boolean SetPhoneOS() {
		if(D) Log.d(TAG, "SetPhoneOS");
		initialCommandSend();

		// Try to set step
		mApplicationLayer.SettingCmdPhoneOSSetting(ApplicationLayer.PHONE_OS_ANDROID);

		return waitCommandSend();
	}
    /**
     * Use to set the clocks
     * @return	the operate result
     * */
    public boolean SetClocks(ApplicationLayerAlarmsPacket alarms) {
    	if(D) Log.d(TAG, "SetClocks()");
		initialCommandSend();
    	
    	// Try to set alarms
        mApplicationLayer.SettingCmdAlarmsSetting(alarms);

		return waitCommandSend();
    }
	/**
	 * Use to set the clock
	 * @return	the operate result
	 * */
	public boolean SetClock(ApplicationLayerAlarmPacket alarm) {
		if(D) Log.d(TAG, "SetClocks()");
		initialCommandSend();
		ApplicationLayerAlarmsPacket alarms = new ApplicationLayerAlarmsPacket();
		alarms.add(alarm);
		// Try to set alarms
		mApplicationLayer.SettingCmdAlarmsSetting(alarms);

		return waitCommandSend();
	}
	/**
	 * Use to sync the clock
	 * @return	the operate result
	 * */
	public boolean SetClocksSyncRequest() {
		if(D) Log.d(TAG, "SetClocksSyncRequest()");
		initialCommandSend();

		// Try to set alarms
		mApplicationLayer.SettingCmdRequestAlarmList();

		return waitCommandSend();
	}
    
    /**
     * Use to sync the time
     * @return	the operate result
     * */
    public boolean SetTimeSync() {
    	if(D) Log.d(TAG, "SetTimeSync()");
		initialCommandSend();
    	
    	// Try to set time
		//in nexus9, Calendar's month is not right
        Calendar c1  = Calendar.getInstance();
		if(D) Log.d(TAG, "SetTimeSync: " + c1.toString());
		mApplicationLayer.SettingCmdTimeSetting(c1.get(Calendar.YEAR),
				c1.get(Calendar.MONTH) + 1, // here need add 1, because it origin range is 0 - 11;
				c1.get(Calendar.DATE),
				c1.get(Calendar.HOUR_OF_DAY),
				c1.get(Calendar.MINUTE),
				c1.get(Calendar.SECOND));
		/*
		Date d = new Date();
		if(D) Log.d(TAG, "SetTimeSync: " + d.toString());
		mApplicationLayer.SettingCmdTimeSetting(d.getYear(),
				d.getMonth(),
				d.getDate(),
				d.getHours(),
				d.getMinutes(),
				d.getSeconds());*/

        return waitCommandSend();
    }
	/**
	 * Use to send the call notify info
	 * @return	the operate result
	 * */
	public boolean SendCallNotifyInfo() {
		if(D) Log.d(TAG, "SendCallNotifyInfo");
		initialCommandSend();

		// Try to set alarms
		mApplicationLayer.NotifyCmdCallNotifyInfoSetting();

		return waitCommandSend();
	}
	/**
	 * Use to send the call accept notify info
	 * @return	the operate result
	 * */
	public boolean SendCallAcceptNotifyInfo() {
		if(D) Log.d(TAG, "SendCallNotifyInfo");
		initialCommandSend();

		// Try to set alarms
		mApplicationLayer.NotifyCmdCallAcceptNotifyInfoSetting();

		return waitCommandSend();
	}
	/**
	 * Use to send the call reject notify info
	 * @return	the operate result
	 * */
	public boolean SendCallRejectNotifyInfo() {
		if(D) Log.d(TAG, "SendCallNotifyInfo");
		initialCommandSend();

		// Try to set alarms
		mApplicationLayer.NotifyCmdCallRejectNotifyInfoSetting();

		return waitCommandSend();
	}
	/**
	 * Use to send the other notify info
	 * @return	the operate result
	 * */
	public boolean SendOtherNotifyInfo(byte info) {
		if(D) Log.d(TAG, "SendOtherNotifyInfo, info: " + info);
		initialCommandSend();

		// Try to set alarms
		mApplicationLayer.NotifyCmdOtherNotifyInfoSetting(info);

		return waitCommandSend();
	}

	/**
	 * Use to send enable fac test mode
	 * @return	the operate result
	 * */
	public boolean SendEnableFacTest() {
		if(D) Log.d(TAG, "SendEnableFacTest");
		if (mWristState != STATE_WRIST_INITIAL) {
			return false;
		}
		initialCommandSend();

		// Try to set alarms
		mApplicationLayer.FACCmdEnterTestMode(null);

		return waitCommandSend();
	}

	/**
	 * Use to send disable fac test mode
	 * @return	the operate result
	 * */
	public boolean SendDisableFacTest() {
		if(D) Log.d(TAG, "SendDisableFacTest");
		if (mWristState != STATE_WRIST_ENTER_TEST_MODE) {
			return false;
		}
		initialCommandSend();

		// Try to set alarms
		mApplicationLayer.FACCmdExitTestMode(null);

		return waitCommandSend();
	}

	/**
	 * Use to send enable led
	 *
	 * @param led the led want to enable
	 * @return	the operate result
	 * */
	public boolean SendEnableFacLed(byte led) {
		if(D) Log.d(TAG, "SendEnableFacLed");
		initialCommandSend();

		// Try to set alarms
		mApplicationLayer.FACCmdEnableLed(led);

		return waitCommandSend();
	}

	/**
	 * Use to send enable vibrate
	 * @return	the operate result
	 * */
	public boolean SendEnableFacVibrate() {
		if(D) Log.d(TAG, "SendEnableFacVibrate");
		initialCommandSend();

		// Try to set alarms
		mApplicationLayer.FACCmdEnableVibrate();

		return waitCommandSend();
	}

	/**
	 * Use to send request sensor data
	 * @return	the operate result
	 * */
	public boolean SendEnableFacSensorDataRequest() {
		if(D) Log.d(TAG, "SendEnableFacSensorData");
		initialCommandSend();

		// Try to set alarms
		mApplicationLayer.FACCmdRequestSensorData();

		return waitCommandSend();
	}
	/**
	 * Use to send remove bond command
	 * @return	the operate result
	 * */
	public boolean SendRemoveBondCommand() {
		if(D) Log.d(TAG, "SendRemoveBondCommand");
		if(D) Log.d("SendRemoveBondCommand","SendRemoveBondCommand");
		initialCommandSend();

		// Try to set alarms
		mApplicationLayer.BondCmdRequestRemoveBond();

		// when send remove bond command, we just think link is lost.
		isConnected = false;
		return waitCommandSend();
	}

	/**
	 * Use to send sync today step command
	 * @return	the operate result
	 * */
	public boolean SendSyncTodayNearlyOffsetStepCommand() {
		if(D) Log.d(TAG, "SendSyncTodayNearlyOffsetStepCommand");
		initialCommandSend();
		Calendar c1  = Calendar.getInstance();
		List<SportData> sports = mGlobalGreenDAO.loadSportDataByDate(c1.get(Calendar.YEAR),
				c1.get(Calendar.MONTH) + 1,// here need add 1, because it origin range is 0 - 11;
				c1.get(Calendar.DATE));

		SportData subData = WristbandCalculator.getNearlyOffsetStepData(sports);

		ApplicationLayerRecentlySportPacket packet;
		if(subData != null) {
			packet = new ApplicationLayerRecentlySportPacket((byte)(subData.getMode() & 0xff)
					, subData.getActiveTime()
					, subData.getCalory()
					, subData.getStepCount()
					, subData.getDistance());
		} else {
			packet = new ApplicationLayerRecentlySportPacket((byte)0x00
					, 0
					, 0
					, 0
					, 0);
		}
		// Try to sync total step data
		mApplicationLayer.SportDataCmdSyncRecently(packet);

		return waitCommandSend();
	}

	/**
	 * Use to send sync today step command
	 * @return	the operate result
	 * */
	public boolean SendSyncTodayStepCommand() {
		if(D) Log.d(TAG, "SendSyncTodayStepCommand");
		initialCommandSend();
		Calendar c1  = Calendar.getInstance();
		List<SportData> sports = mGlobalGreenDAO.loadSportDataByDate(c1.get(Calendar.YEAR),
				c1.get(Calendar.MONTH) + 1,// here need add 1, because it origin range is 0 - 11;
				c1.get(Calendar.DATE));

		SportSubData subData = WristbandCalculator.sumOfSportDataByDate(c1.get(Calendar.YEAR),
				c1.get(Calendar.MONTH) + 1,// here need add 1, because it origin range is 0 - 11;
				c1.get(Calendar.DATE),
				sports);
		ApplicationLayerTodaySportPacket packet;
		if(subData != null) {
			packet = new ApplicationLayerTodaySportPacket((long)subData.getStepCount()
					, (long)subData.getDistance()
					, (long)subData.getCalory());
		} else {
			packet = new ApplicationLayerTodaySportPacket(0
					, 0
					, 0);
		}
		// Try to sync total step data
		mApplicationLayer.SportDataCmdSyncToday(packet);

		return waitCommandSend();
	}

	public boolean isInSendCommand() {
		return this.isInSendCommand;
	}

	private boolean initialCommandSend() {
		if(D) Log.d(TAG, "initialCommandSend()");
		// Here we need do more thing for queue send command, current version didn't fix it.


		while(isInSendCommand || isNeedWaitForResponse) {
			if(!isConnect()) {
				return false;
			}
			if(D) Log.d(TAG, "Wait for last command send ok. isInSendCommand: " +isInSendCommand + ", isNeedWaitForResponse: " + isNeedWaitForResponse);
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}

		synchronized(mCommandSendLock) {
			// initial status
			mErrorStatus = false;
			isCommandSend = false;
			isCommandSendOk = false;

			isInSendCommand = true;
		}

		return true;
	}

	private boolean waitCommandSend() {
		if(D) Log.d(TAG, "waitCommandSend()");
		boolean commendSendReady = false;
		synchronized(mCommandSendLock) {
			if(isCommandSend != true) {
				try {
					// wait a while
					if(D) Log.d(TAG, "wait the time set callback, wait for: " + MAX_COMMAND_SEND_WAIT_TIME + "ms");
					mCommandSendLock.wait(MAX_COMMAND_SEND_WAIT_TIME);

					if(D) Log.d(TAG, "waitCommandSend, isCommandSendOk: " + isCommandSendOk);
					isInSendCommand = false;

					commendSendReady = isCommandSendOk;
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
		return commendSendReady;
	}
    
    
    
    // Login response
    private boolean mLoginResponse;
    /**
     * Request Login
     * 
     * @id	the user id
     * @return the login result, fail or success
     * 
     * */
    private boolean RequestLogin(String id) {
    	if(D) Log.d(TAG, "RequestLogin, id: " + id);
		if(D) Log.d("requestLoginBond", "RequestLogin, id: " + id);
    	// initial error status
    	mErrorStatus = false;
    	isResponseCome = false;
    	mLoginResponse = false;
    	

    	// Try to login
        mApplicationLayer.BondCmdRequestLogin(id);// it will wait the onBondCmdRequestLogin callback invoke.
        
    	synchronized(mRequestResponseLock) {
			if(isResponseCome != true) {
				try {
					// wait a while
					if(D) Log.d(TAG, "wait the login response come, wait for: " + MAX_REQUEST_RESPONSE_TRANSACTION_WAIT_TIME + "ms");
					mRequestResponseLock.wait(MAX_REQUEST_RESPONSE_TRANSACTION_WAIT_TIME);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
    	
    	if(isResponseCome != true) {
    		mErrorStatus = true;
    		return false;
    	}
		if(mLoginResponse) {
			if (D) Log.d("onBondCmdRequestLogin", "mLoginResponse true");
		}else {
			if (D) Log.d("onBondCmdRequestLogin", "mLoginResponse false");
		}
        return mLoginResponse;

    }
    
    // Login response
    private boolean mBondResponse;
    /**
     * Request Bond
     * 
     * @id	the user id
     * @return the bond result, fail or success
     * 
     * */
    private boolean RequestBond(String id) {
    	if(D) Log.d(TAG, "RequestBond, id: " + id);
		if(D) Log.d("requestBondLogin","RequestBond id"+id);
    	// initial error status
    	mErrorStatus = false;
    	isResponseCome = false;
    	mBondResponse = false;
    	
    	// Try to login
        mApplicationLayer.BondCmdRequestBond(id);// it will wait the onBondCmdRequestLogin callback invoke.
        
    	synchronized(mRequestResponseLock) {
			if(isResponseCome != true) {
				try {
					// wait a while
					if(D) Log.d(TAG, "wait the bond response come, wait for: " + MAX_REQUEST_RESPONSE_TRANSACTION_WAIT_TIME + "ms");
					mRequestResponseLock.wait(MAX_REQUEST_RESPONSE_TRANSACTION_WAIT_TIME);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
    	
    	if(isResponseCome != true) {
    		mErrorStatus = true;
    		return false;
    	}
        return mBondResponse;
    }

	public boolean isReady() {
		if(D) Log.d(TAG, "isReady, mWristState: " + mWristState);
		return mWristState == STATE_WRIST_SYNC_DATA;
	}

	// Extend Service
	BatteryService mBatteryService;
	ImmediateAlertService mImmediateAlertService;
	LinkLossService mLinkLossService;
	DfuService mDfuService;
	/**
	 * Use to set the remote Immediate Alert Level
	 *
	 * @param enable enable/disable Immediate Alert
	 * */
	public boolean enableImmediateAlert(boolean enable) {

		return mImmediateAlertService.enableAlert(enable);
	}

	/**
	 * Use to set the remote Link Loss Alert Level
	 *
	 * @param enable enable/disable Link Loss Alert
	 * */
	public boolean enableLinkLossAlert(boolean enable) {

		return mLinkLossService.enableAlert(enable);
	}

	/**
	 * Use to read the remote Battery Level
	 *
	 * */
	public boolean readBatteryLevel() {
		if(D) Log.d(TAG, "readBatteryLevel");
		//isInSendCommand = true;
		initialCommandSend();

		boolean result = mBatteryService.readInfo();

		isInSendCommand = false;
		return result;
	}
	/**
	 * Use to read the remote Link loss Level
	 *
	 * */
	public boolean readLinkLossLevel() {
		if(D) Log.d(TAG, "readLinkLossLevel");
		initialCommandSend();

		if(!mLinkLossService.readInfo()) {
			if(D) Log.e(TAG, "readLinkLossLevel, failed");
			return false;
		}

		return waitCommandSend();
	}
	/**
	 * Use to read the remote version info
	 *
	 * */
	public boolean readDfuVersion() {
		if(D) Log.d(TAG, "readDfuVersion");
		initialCommandSend();

		if(!mDfuService.readInfo()) {
			if(D) Log.e(TAG, "readDfuVersion, failed");
			return false;
		}

		return waitCommandSend();
	}

	/**
	 * Use to get the remote Battery Level
	 *
	 * */
	public int getBatteryLevel() {
		if(mBatteryService != null) {
			return mBatteryService.getBatteryValue();
		} else {
			return -1;
		}
	}



	/**
	 * Use to enable battery power notification
	 *
	 * */
	public boolean enableBatteryNotification(boolean enable) {
		// enable notification
		return mBatteryService.enableNotification(enable);
	}

	// notify control
	public void registerNotifyBroadcast() {
		if(D) Log.i(TAG, "registerNotifyBroadcast");
		mNotifyBroadcastReceive = new NotifyBroadcastReceive(this);
		IntentFilter intentFilter = new IntentFilter();
		intentFilter.addAction(NotifyBroadcastReceive.ACTION_BROADCAST_CALL);
		intentFilter.addAction(NotifyBroadcastReceive.ACTION_BROADCAST_SMS);
		intentFilter.addAction(NotificationReceive.BROADCAST_TYPE);
		intentFilter.setPriority(Integer.MAX_VALUE);
		mContext.registerReceiver(mNotifyBroadcastReceive, intentFilter);
	}

	public void unregisterNotifyBroadcast() {
		if(D) Log.i(TAG, "unregisterNotifyBroadcast");
		if(mNotifyBroadcastReceive != null) {
			mContext.unregisterReceiver(mNotifyBroadcastReceive);
			mNotifyBroadcastReceive = null;
		}
	}



	// The Handler that gets information back from test thread
    //private class MyHandler extends Handler {
	private Handler mHandler = new Handler() {
		@Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_STATE_CONNECTED:
					isConnected = true;
					for(WristbandManagerCallback callback: mCallbacks) {
						callback.onConnectionStateChange(true);
					}
                    //mCallback.onConnectionStateChange(true);
                    break;
                case MSG_STATE_DISCONNECTED:
					if(isConnect()
							&& mWristState >= STATE_WRIST_LOGIN) {// Only login may call lost alert
						if(!SPWristbandConfigInfo.getControlSwitchLost(mContext)) {
							if (D) Log.i(TAG, "Lost alarm didn't enable.");
						} else {
							mAlertDialog = new AlertDialog.Builder(mContext, AlertDialog.THEME_HOLO_LIGHT).create();
							mAlertDialog.setMessage(mContext.getString(R.string.connect_disconnect));
							mAlertDialog.setTitle(R.string.app_name);
							mAlertDialog.setButton(AlertDialog.BUTTON_POSITIVE, mContext.getString(R.string.cancel), new DialogInterface.OnClickListener() {
								@Override
								public void onClick(DialogInterface dialog, int which) {
									stopAlarm();
									dialog.dismiss();
								}
							});
							mAlertDialog.setCancelable(false);
							mAlertDialog.getWindow().setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT);
							mAlertDialog.show();

							playAlarm();
						}
					}
					isConnected = false;
                    // do something
					for(WristbandManagerCallback callback: mCallbacks) {
						callback.onConnectionStateChange(false);
					}
                    //mCallback.onConnectionStateChange(false);

					// close all
					close();
                    break;
                case MSG_ERROR:
					// do something
					for(WristbandManagerCallback callback: mCallbacks) {
						callback.onError(msg.arg1);
					}
                    //mCallback.onError(msg.arg1);
                    break;
                case MSG_WRIST_STATE_CHANGED:
                    if(D) Log.d(TAG, "MSG_WRIST_STATE_CHANGED, current state: " + msg.arg1);
                    // show state
					// do something
					for(WristbandManagerCallback callback: mCallbacks) {
						callback.onLoginStateChange(msg.arg1);
					}
                    //mCallback.onLoginStateChange(msg.arg1);
                    break;
                case MSG_RECEIVE_SPORT_INFO:
                    //
                    ApplicationLayerSportPacket sportPacket = (ApplicationLayerSportPacket) msg.obj;
                    if(D) Log.d(TAG, "Receive a sport packet, Year: " + (sportPacket.getYear() + 2000) +
                                                        ", Month: " + sportPacket.getMonth() +
                                                        ", Day: " + sportPacket.getDay() +
                                                        ", Item count: " + sportPacket.getItemCount());
                    for(ApplicationLayerSportItemPacket p: sportPacket.getSportItems()) {
                        if(D) Log.d(TAG, "Find a sport item, Offset: " + p.getOffset() +
                                                        ", Mode: " + p.getMode() +
                                                        ", Step count: " + p.getStepCount() +
                                                        ", Active time: " + p.getActiveTime() +
                                                        ", Calory: " + p.getCalory() +
                                                        ", Distance: " + p.getDistance());
                        
                        SportData sportData = new SportData(null, sportPacket.getYear() + 2000, sportPacket.getMonth(), sportPacket.getDay(),
                        		p.getOffset(), p.getMode(), p.getStepCount(), p.getActiveTime(), p.getCalory(), p.getDistance(), new Date());
						//请求数据一次，将数据保存到本地
						mGlobalGreenDAO.saveSportData(sportData);
						for(WristbandManagerCallback callback: mCallbacks) {
							callback.onSportDataReceive(sportData);
						}
                        //mCallback.onSportDataReceive(sportData);
                    }
                    
                    break;
				case MSG_RECEIVE_SLEEP_INFO:
					//
					ApplicationLayerSleepPacket sleepPacket = (ApplicationLayerSleepPacket) msg.obj;
					if(D) Log.d(TAG, "Receive a sleep packet, Year: " + (sleepPacket.getYear() + 2000) +
							", Month: " + sleepPacket.getMonth() +
							", Day: " + sleepPacket.getDay() +
							", Item count: " + sleepPacket.getItemCount());
					for(ApplicationLayerSleepItemPacket p: sleepPacket.getSleepItems()) {
						if(D) Log.d(TAG, "Find a sleep item, Minutes: " + p.getMinutes() +
								", Mode: " + p.getMode());

						SleepData sleepData = new SleepData(null, sleepPacket.getYear() + 2000, sleepPacket.getMonth(), sleepPacket.getDay(),
								p.getMinutes(), p.getMode(), new Date());
						//请求数据一次，保存数据到本地
						mGlobalGreenDAO.saveSleepData(sleepData);
						for(WristbandManagerCallback callback: mCallbacks) {
							callback.onSleepDataReceive(sleepData);
						}
						//mCallback.onSleepDataReceive(sleepData);
					}

					break;
				case MSG_RECEIVE_NOTIFY_MODE_SETTING:
					byte mode = (byte) msg.obj;
					if(D) Log.w(TAG, "Current notify setting is: " + mode);
					SPWristbandConfigInfo.setNotifyCallFlag(mContext, (mode & ApplicationLayer.NOTIFY_SWITCH_SETTING_CALL) != 0);
					SPWristbandConfigInfo.setNotifyMessageFlag(mContext, (mode & ApplicationLayer.NOTIFY_SWITCH_SETTING_MESSAGE) != 0);
					if(isNotifyManageEnabled()) {
						SPWristbandConfigInfo.setNotifyQQFlag(mContext, (mode & ApplicationLayer.NOTIFY_SWITCH_SETTING_QQ) != 0);
						SPWristbandConfigInfo.setNotifyWechatFlag(mContext, (mode & ApplicationLayer.NOTIFY_SWITCH_SETTING_WECHAT) != 0);
					} else {
						if(D) Log.w(TAG, "Notify not enable, should not enable these setting.");
					}

					synchronized(mRequestResponseLock) {
						isResponseCome = true;
						isNeedWaitForResponse = false;
						mRequestResponseLock.notifyAll();
					}

					for(WristbandManagerCallback callback: mCallbacks) {
						callback.onNotifyModeSettingReceive(mode);
					}
					break;
				case MSG_RECEIVE_LONG_SIT_SETTING:
					byte longsitmode = (byte) msg.obj;
					if(D) Log.w(TAG, "Current long sit setting is: " + longsitmode);
					SPWristbandConfigInfo.setControlSwitchLongSit(mContext, longsitmode == ApplicationLayer.LONG_SIT_CONTROL_ENABLE);
					synchronized(mRequestResponseLock) {
						isResponseCome = true;
						isNeedWaitForResponse = false;
						mRequestResponseLock.notifyAll();
					}

					for(WristbandManagerCallback callback: mCallbacks) {
						callback.onLongSitSettingReceive(longsitmode);
					}
					break;
				case MSG_RECEIVE_ALARMS_INFO:
					ApplicationLayerAlarmsPacket alarmPacket = (ApplicationLayerAlarmsPacket) msg.obj;
					if(alarmPacket.getAlarms().size() == 0) {
						if(D) Log.w(TAG, "No alarm list.");
						for(WristbandManagerCallback callback: mCallbacks) {
							callback.onAlarmDataReceive(null);
						}
						return;
					}
					for(ApplicationLayerAlarmPacket p: alarmPacket.getAlarms()) {
						if(D) Log.d(TAG, "Find a alarm item, Year: " + p.getYear() +
								", Month: " + p.getMonth() +
								", Day: " + p.getDay() +
								", Hour: " + p.getHour() +
								", Minute: " + p.getMinute() +
								", Id: " + p.getId() +
								", Day Flags: " + p.getDayFlags());

						for(WristbandManagerCallback callback: mCallbacks) {
							callback.onAlarmDataReceive(p);
						}
						//mCallback.onSleepDataReceive(sleepData);
					}
					break;
				case MSG_RECEIVE_FAC_SENSOR_INFO:
					if(D) Log.d(TAG, "MSG_RECEIVE_FAC_SENSOR_INFO");
					ApplicationLayerFacSensorPacket sensorPacket = (ApplicationLayerFacSensorPacket) msg.obj;
					if(D) Log.d(TAG, "Receive Fac Sensor info, X: " + sensorPacket.getX() +
							", Y: " + sensorPacket.getY() +
							", Z: " + sensorPacket.getZ());
					// show state
					// do something
					for(WristbandManagerCallback callback: mCallbacks) {
						callback.onFacSensorDataReceive(sensorPacket);
					}
					break;
				//收到版本号的信息
				case MSG_RECEIVE_DFU_VERSION_INFO:
					if(D) Log.d(TAG, "MSG_RECEIVE_DFU_VERSION_INFO");
					int appVersion = msg.arg1;
					int patchVersion = msg.arg2;
					if(D) Log.d(TAG, "Receive dfu version info, appVersion: " + appVersion +
							", patchVersion: " + patchVersion);
					// show state
					// do something
					for(WristbandManagerCallback callback: mCallbacks) {
						callback.onVersionRead(appVersion, patchVersion);
					}
					break;
				case MSG_RECEIVE_DEVICE_NAME_INFO:
					if(D) Log.d(TAG, "MSG_RECEIVE_DEVICE_NAME_INFO");
					String name = (String)msg.obj;
					if(D) Log.d(TAG, "Receive device name info, name: " + name);
					// show state
					// do something
					for(WristbandManagerCallback callback: mCallbacks) {
						callback.onNameRead(name);
					}
					break;
				case MSG_RECEIVE_BATTERY_INFO:
					if(D) Log.d(TAG, "MSG_RECEIVE_BATTERY_INFO");
					int battery = msg.arg1;
					for(WristbandManagerCallback callback: mCallbacks) {
						callback.onBatteryRead(battery);
					}
				default:
					break;
            }
        }
    };

    // Application Layer callback
    ApplicationLayerCallback mApplicationCallback = new ApplicationLayerCallback() {
        @Override
        public void onConnectionStateChange(final boolean status, final boolean newState) {
            if(D) Log.d(TAG, "onConnectionStateChange, status: " + status + "newState: " + newState);
			if(D) Log.d("onConnectionStateChange","onConnectionStateChange, status: " + status + "newState: " + newState);
            // if already connect to the remote device, we can do more things here.
            if(status && newState) {
                SendMessage(MSG_STATE_CONNECTED, null, -1, -1);
            } else {
                SendMessage(MSG_STATE_DISCONNECTED, null, -1, -1);
            }
        }
        @Override
        public void onSettingCmdRequestAlarmList(final ApplicationLayerAlarmsPacket alarms) {
            if(D) Log.d(TAG, "ApplicationLayerAlarmsPacket");
			SendMessage(MSG_RECEIVE_ALARMS_INFO, alarms, -1, -1);
        }
		@Override
		public void onSettingCmdRequestNotifySwitch(final byte mode) {
			if(D) Log.d(TAG, "onSettingCmdRequestNotifySwitch");
			SendMessage(MSG_RECEIVE_NOTIFY_MODE_SETTING, mode, -1, -1);
		}
		@Override
		public void onSettingCmdRequestLongSit(final byte mode) {
			if(D) Log.d(TAG, "onSettingCmdRequestLongSit");
			SendMessage(MSG_RECEIVE_LONG_SIT_SETTING, mode, -1, -1);
		}
		//运动数据返回
		@Override
        public void onSportDataCmdSportData(final ApplicationLayerSportPacket sport) {
			if(D) Log.d(TAG, "onSportDataCmdSportData");
			SendMessage(MSG_RECEIVE_SPORT_INFO, sport, -1, -1);
		}
		//睡眠数据返回
		@Override
		public void onSportDataCmdSleepData(final ApplicationLayerSleepPacket sleep) {
			if(D) Log.d(TAG, "onSportDataCmdSleepData");
			SendMessage(MSG_RECEIVE_SLEEP_INFO, sleep, -1, -1);
		}
		//历史数据同步开始
		@Override
		public void onSportDataCmdHistorySyncBegin() {
			if(D) Log.d(TAG, "onSportDataCmdHistorySyncBegin");
			UpdateWristState(STATE_WRIST_SYNC_HISTORY_DATA);
		}
		//历史数据同步结束
		@Override
		public void onSportDataCmdHistorySyncEnd(final ApplicationLayerTodaySumSportPacket packet) {
			if(D) Log.d(TAG, "onSportDataCmdHistorySyncEnd");

			isNeedWaitForResponse = false;
			// Adjust data
			if(packet != null) {
				if(D) Log.d(TAG, "onSportDataCmdHistorySyncEnd, pakect.getOffset()" + packet.getOffset()
						+ ", pakect.getTotalStep()" + packet.getTotalStep()
						+ ", pakect.getTotalCalory()" + packet.getTotalCalory()
						+ ", pakect.getTotalDistance()" + packet.getTotalDistance());
				WristbandCalculator.adjustTodayTotalStepData(packet);
			}

			if(BmobControlManager.getInstance().checkAPKWorkType()
					&& BmobControlManager.getInstance().isNetworkConnected()) {

				// Need to sync data to the server, while sync end  //把运动数据同步到云端
				BmobDataSyncManager.syncDatatoServer(mContext, new BmobDataSyncManager.SyncListen() {
					@Override
					public void onSyncDone(BmobException e) {
						if (D)
							Log.d(TAG, "onSyncDone, e: " + (e == null ? "none" : e.getMessage()));

						sendSyncDataBroadcast();
						UpdateWristState(STATE_WRIST_SYNC_DATA);
					}
				});
			} else {
				sendSyncDataBroadcast();
				UpdateWristState(STATE_WRIST_SYNC_DATA);
			}

		}

		@Override
		public void onFACCmdSensorData(final ApplicationLayerFacSensorPacket sensor) {
			if (D) Log.d(TAG, "onFACCmdSensorData");
			SendMessage(MSG_RECEIVE_FAC_SENSOR_INFO, sensor, -1, -1);
		}
		//绑定结果
        @Override
        public void onBondCmdRequestBond(final byte status) {
            if(D) Log.d(TAG, "onBondCmdRequestBond, status: " + status);
            // bond right
            if(status == ApplicationLayer.BOND_RSP_SUCCESS) {
            	mBondResponse = true;
            }
            // bond error
            else {
            	mBondResponse = false;
            }
            synchronized(mRequestResponseLock) {
            	isResponseCome = true;
				isNeedWaitForResponse = false;
            	mRequestResponseLock.notifyAll();
			}
        }

		//登陆
        @Override
        public void onBondCmdRequestLogin(final byte status) {
            if(D) Log.d(TAG, "onBondCmdRequestLogin, status: " + status);
			if(D) Log.d("onBondCmdRequestLogin", "onBondCmdRequestLogin, status: " + status);

            // Login right
            if(status == ApplicationLayer.LOGIN_RSP_SUCCESS) {
            	mLoginResponse = true;
            }
            // Login error
            else {
            	mLoginResponse = false;
            }
            synchronized(mRequestResponseLock) {
            	isResponseCome = true;
				isNeedWaitForResponse = false;
            	mRequestResponseLock.notifyAll();
			}
        }


        @Override
        public void onCommandSend(final boolean status, byte command, byte key) {
            if(D) Log.d(TAG, "onCommandSend, status: " + status + ", command: " + command + ", key: " + key);
            // if command send not right(no ACK). we just close it, and think connection is wrong.
            // Or, we can try to reconnect, or do other things.
            if(status != true) {
            	isCommandSendOk = false;
            	SendErrorMessage(ERROR_CODE_COMMAND_SEND_ERROR);
                //mApplicationLayer.close(); // error
            } else {
            	isCommandSendOk = true;
				if(command == ApplicationLayer.CMD_FACTORY_TEST) {
					if(key == ApplicationLayer.KEY_FAC_TEST_ENTER_SPUER_KEY) {
						UpdateWristState(STATE_WRIST_ENTER_TEST_MODE);
					} else if (key == ApplicationLayer.KEY_FAC_TEST_LEAVE_SPUER_KEY) {
						UpdateWristState(STATE_WRIST_INITIAL);
					}
				}
            }
            synchronized(mCommandSendLock) {
            	isCommandSend = true;
            	mCommandSendLock.notifyAll();
			}
        }


		@Override
		public void onNameReceive(final String data) {
			SendMessage(MSG_RECEIVE_DEVICE_NAME_INFO, data, -1, -1);
			synchronized(mCommandSendLock) {
				isCommandSend = true;
				mCommandSendLock.notifyAll();
			}
		}
    };
    private void SendErrorMessage(int error) {
    	SendMessage(MSG_ERROR, null, error, -1);
    }
    
    
    private void UpdateWristState(int state) {
        // update the wrist state
        mWristState = state;
        SendMessage(MSG_WRIST_STATE_CHANGED, null, mWristState, -1);
    }

    /**
     * send message
     * @param msgType Type message type
     * @param obj object sent with the message set to null if not used
     * @param arg1 parameter sent with the message, set to -1 if not used
     * @param arg2 parameter sent with the message, set to -1 if not used
     **/
    private void SendMessage(int msgType, Object obj, int arg1, int arg2) {
        if(mHandler != null) {
            //	Message msg = new Message();
            Message msg = Message.obtain();
            msg.what = msgType;
            if(arg1 != -1) {
                msg.arg1 = arg1;
            }
            if(arg2 != -1) {
                msg.arg2 = arg2;
            }
            if(null != obj) {
                msg.obj = obj;
            }
            mHandler.sendMessage(msg);
        }
        else {
            if(D) Log.e(TAG,"handler is null, can't send message");
        }
    }

	@Override
	public void onBatteryValueReceive(int value) {
		if(D) Log.d(TAG,"onBatteryValueReceive, value: " + value);
		SendMessage(MSG_RECEIVE_BATTERY_INFO, null, value, -1);
	}

	private long mLastOtherNotifySendTime = 0;
	private void setLastOtherNotifySendTime() {
		mLastOtherNotifySendTime = System.currentTimeMillis();
	}
	private boolean checkLastOtherNotifySendTime() {
		// Diff must big then 300ms, to make sure not too much notify
		if(Math.abs(mLastOtherNotifySendTime - System.currentTimeMillis()) > 300) {
			return true;
		}
		return false;
	}
	@Override
	public void onBroadcastCome(int type) {
		if(D) Log.d(TAG,"onBroadcastCome, type: " + type);
		if(isConnect()
				&& isReady()
				&& !BackgroundScanAutoConnected.getInstance().isInLogin()) {
			switch (type) {
				case NotifyBroadcastReceive.BROADCAST_CALL_WAIT:
					if(SPWristbandConfigInfo.getNotifyCallFlag(mContext)) {
						new Thread(new Runnable() {
							@Override
							public void run() {
								// Send Call notify
								SendCallNotifyInfo();
							}
						}).start();
					}
					break;
				case NotifyBroadcastReceive.BROADCAST_CALL_ACC:
					if(SPWristbandConfigInfo.getNotifyCallFlag(mContext)) {
						new Thread(new Runnable() {
							@Override
							public void run() {
								// Send Call notify
								SendCallAcceptNotifyInfo();
							}
						}).start();
					}
					break;
				case NotifyBroadcastReceive.BROADCAST_CALL_REJ:
					if(SPWristbandConfigInfo.getNotifyCallFlag(mContext)) {
						new Thread(new Runnable() {
							@Override
							public void run() {
								// Send Call notify
								SendCallRejectNotifyInfo();
							}
						}).start();

					}
					break;
				case NotifyBroadcastReceive.BROADCAST_SMS:
					if(SPWristbandConfigInfo.getNotifyMessageFlag(mContext)) {
						new Thread(new Runnable() {
							@Override
							public void run() {
								// Send Message notify
								SendOtherNotifyInfo(ApplicationLayer.OTHER_NOTIFY_INFO_MESSAGE);
							}
						}).start();
					}
					break;
				case NotifyBroadcastReceive.BROADCAST_QQ:
					if(SPWristbandConfigInfo.getNotifyQQFlag(mContext)) {
						if(!checkLastOtherNotifySendTime()) {
							if(D) Log.w(TAG, "Other notify receive too fast, didn't need to send again.");
							return;
						}
						new Thread(new Runnable() {
							@Override
							public void run() {
								setLastOtherNotifySendTime();
								// Send Message notify
								SendOtherNotifyInfo(ApplicationLayer.OTHER_NOTIFY_INFO_QQ);
							}
						}).start();
					}
					break;
				case NotifyBroadcastReceive.BROADCAST_WECHAT:
					if(SPWristbandConfigInfo.getNotifyWechatFlag(mContext)) {
						if(!checkLastOtherNotifySendTime()) {
							if(D) Log.w(TAG, "Other notify receive too fast, didn't need to send again.");
							return;
						}
						new Thread(new Runnable() {
							@Override
							public void run() {
								setLastOtherNotifySendTime();
								// Send Message notify
								SendOtherNotifyInfo(ApplicationLayer.OTHER_NOTIFY_INFO_WECHAT);
							}
						}).start();
					}
					break;
			}
		} else {
			if(D) Log.e(TAG, "Receive broadcast with state error, do nothing!");
		}
	}



	private MediaPlayer mMediaPlayer;
	// for Vibrator
	private Vibrator mVibrator;
	private AlertDialog mAlertDialog;
	private boolean isInAlarm = false;
	// Alarm timer
	Handler mAlarmSuperHandler = new Handler();
	Runnable mAlarmSuperTask = new Runnable(){
		@Override
		public void run() {
			// TODO Auto-generated method stub
			if(D) Log.w(TAG, "Wait Alarm Timeout");
			// stop timer
			stopAlarm();
		}
	};
	private void playAlarm() {
		if (D) Log.e(TAG, "playAlarm");
		int alarmTime;
		String musicPath;
		isInAlarm = true;

		alarmTime = SPWristbandConfigInfo.getLostAlarmTime(mContext);
		musicPath = SPWristbandConfigInfo.getLostAlarmMusic(mContext);
		try {
			if(musicPath != null) {
				mMediaPlayer.setDataSource(musicPath);
				if (D) Log.e(TAG, "load music, path: " + musicPath);
			} else {
				Uri uri = Uri.parse("android.resource://com.realsil.android.wristbanddemo/" + R.raw.alarm);
				//AssetFileDescriptor fileDescriptor = mContext.getAssets().openFd("alarm.wav");
				if(D) Log.d(TAG, "load music, uri: " + uri.toString());
				mMediaPlayer.setDataSource(mContext, uri);
			}
			mMediaPlayer.prepare();
			mMediaPlayer.setLooping(true);
			mMediaPlayer.start();
		} catch (IOException e) {
			e.printStackTrace();
		}
		long[] pattern = new long[]{200, 200, 200, 200, 200};
		mVibrator.vibrate(pattern, 0);
		//mVibrator.vibrate(alarmTime * 1000);
		mAlarmSuperHandler.postDelayed(mAlarmSuperTask, alarmTime * 1000);
	}

	private void stopAlarm() {
		isInAlarm = false;
		mMediaPlayer.stop();
		//mSoundPool.autoPause();
		mVibrator.cancel();
		if(mAlertDialog != null) {
			mAlertDialog.dismiss();
		}
		mAlarmSuperHandler.removeCallbacks(mAlarmSuperTask);
	}
    //读取到版本号
	@Override
	public void onVersionRead(int appVersion, int patchVersion) {
		if(D) Log.d(TAG, "onVersionRead, appVersion: " + appVersion + ", patchVersion: " + patchVersion);
		SendMessage(MSG_RECEIVE_DFU_VERSION_INFO, null, appVersion, patchVersion);

		synchronized(mCommandSendLock) {
			isCommandSendOk = true;
			isCommandSend = true;
			mCommandSendLock.notifyAll();
		}
	}

	@Override
	public void onLinkLossValueReceive(boolean value) {
		if(D) Log.d(TAG, "onLinkLossValueReceive, value: " + value);
		SPWristbandConfigInfo.setControlSwitchLost(mContext, value);
		synchronized(mCommandSendLock) {
			isCommandSendOk = true;
			isCommandSend = true;
			mCommandSendLock.notifyAll();
		}
	}

	private boolean isNotifyManageEnabled() {
		if(D) Log.d(TAG, "isNotifyManageEnabled");
		String pkgName = mContext.getPackageName();
		final String flat = Settings.Secure.getString(mContext.getContentResolver(),
				ENABLED_NOTIFICATION_LISTENERS);
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

	private void sendSyncDataBroadcast() {
		Intent intent = new Intent();
		intent.setAction(ACTION_SYNC_DATA_OK);
		mContext.sendBroadcast(intent);
	}
}
