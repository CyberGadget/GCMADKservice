package edu.rosehulman.gcmadkservice;

import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import org.json.JSONException;
import org.json.JSONObject;

import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.hardware.usb.UsbAccessory;
import android.hardware.usb.UsbManager;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.IBinder;
import android.os.ParcelFileDescriptor;
import android.preference.PreferenceManager;
import android.util.Log;

import com.google.android.gms.gcm.GoogleCloudMessaging;

public class GCMADKservice extends Service {

	private static final String TAG = "GCMADKservice";
	private PendingIntent mPermissionIntent;
	private static final String ACTION_USB_PERMISSION = "edu.rosehulman.gcmadkservice.action.USB_PERMISSION";
	private boolean mPermissionRequestPending;
	private UsbManager mUsbManager;
	private UsbAccessory mAccessory;
	private ParcelFileDescriptor mFileDescriptor;
	private FileInputStream mInputStream;
	private FileOutputStream mOutputStream;

	public static GCMADKservice mService = null;

	/** GAE Project Number */
	String SENDER_ID = "458090634212"; // for davelldf-robot-controll
	// String SENDER_ID = "25263590245"; // for kahlda-gcm-tutorial

	GoogleCloudMessaging mGcmHelper;

	Handler mHandler = new Handler();
	Runnable mRxRunnable = new Runnable() {

		public void run() {
			int ret = 0;
			byte[] buffer = new byte[255];

			// Loop that runs forever (or until a -1 error state).
			while (ret >= 0) {
				try {
					ret = mInputStream.read(buffer);
				} catch (IOException e) {
					break;
				}

				if (ret > 0) {
					// Convert the bytes into a string.
					String received = new String(buffer, 0, ret);
					final String receivedCommand = received.trim();
					// onCommandReceived(receivedCommand);
					mHandler.post(new Runnable() {
						public void run() {
							onCommandReceived(receivedCommand);
						}
					});
				}
			}
		}
	};

	/**
	 * Override this method with your activity if you'd like to receive
	 * messages.
	 * 
	 * @param receivedCommand
	 */
	protected void onCommandReceived(final String receivedCommand) {
		// Toast.makeText(this, "Received command = " + receivedCommand,
		// Toast.LENGTH_SHORT).show();
		Log.d(TAG, "Received command = " + receivedCommand);
		sendCommand("ECHO: " + receivedCommand + '\0');
	}

	@Override
	public void onCreate() {
		super.onCreate();
		mService = this;
		mUsbManager = (UsbManager) getSystemService(Context.USB_SERVICE);
		mPermissionIntent = PendingIntent.getBroadcast(this, 0, new Intent(
				ACTION_USB_PERMISSION), 0);

		IntentFilter filter = new IntentFilter(ACTION_USB_PERMISSION);
		filter.addAction(UsbManager.ACTION_USB_ACCESSORY_DETACHED);
		registerReceiver(mUsbReceiver, filter);

		// -----GCM-----
		if (getRegistrationId().isEmpty()) {
			mGcmHelper = GoogleCloudMessaging.getInstance(this);
			new GetRegistrationIdTask().execute();
		} else {
			Log.d(TAG, "Alread have a reg id = " + getRegistrationId());
		}
	}

	private final BroadcastReceiver mUsbReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
			if (ACTION_USB_PERMISSION.equals(action)) {
				synchronized (this) {
					UsbAccessory accessory = (UsbAccessory) intent
							.getParcelableExtra(UsbManager.EXTRA_ACCESSORY);
					if (intent.getBooleanExtra(
							UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
						openAccessory(accessory);
					} else {
						Log.d(TAG, "permission denied for accessory "
								+ accessory);
					}
					mPermissionRequestPending = false;
				}
			} else if (UsbManager.ACTION_USB_ACCESSORY_DETACHED.equals(action)) {
				UsbAccessory accessory = (UsbAccessory) intent
						.getParcelableExtra(UsbManager.EXTRA_ACCESSORY);
				if (accessory != null && accessory.equals(mAccessory)) {
					closeAccessory();
				}
			}
		}
	};

	protected void sendCommand(String commandString) {
		new AsyncTask<String, Void, Void>() {
			@Override
			protected Void doInBackground(String... params) {
				String command = params[0];
				char[] buffer = new char[command.length() + 1];
				byte[] byteBuffer = new byte[command.length() + 1];
				command.getChars(0, command.length(), buffer, 0);
				buffer[command.length()] = '\n';
				for (int i = 0; i < command.length() + 1; i++) {
					byteBuffer[i] = (byte) buffer[i];
				}
				if (mOutputStream != null) {
					try {
						Log.d(TAG, "Sending command.");
						mOutputStream.write(byteBuffer);
					} catch (IOException e) {
						Log.e(TAG, "write failed", e);
					}
				}
				return null;
			}
		}.execute(commandString);
	}

	private void openAccessory(UsbAccessory accessory) {
		Log.d(TAG, "Open accessory called.");
		mFileDescriptor = mUsbManager.openAccessory(accessory);
		if (mFileDescriptor != null) {
			mAccessory = accessory;
			FileDescriptor fd = mFileDescriptor.getFileDescriptor();
			mInputStream = new FileInputStream(fd);
			mOutputStream = new FileOutputStream(fd);
			Thread thread = new Thread(null, mRxRunnable, TAG);
			thread.start();
			Log.d(TAG, "accessory opened");
		} else {
			Log.d(TAG, "accessory open fail");
		}
	}

	private void closeAccessory() {
		Log.d(TAG, "Close accessory called.");
		try {
			if (mFileDescriptor != null) {
				mFileDescriptor.close();
			}
		} catch (IOException e) {
		} finally {
			mFileDescriptor = null;
			mAccessory = null;
			mInputStream = null;
			mOutputStream = null;
		}
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		mService = null;
		Log.d(TAG, "Service is being destroyed. Oh no.");
		closeAccessory();
		unregisterReceiver(mUsbReceiver);
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		if (mInputStream != null && mOutputStream != null) {
			return -1;
		}

		UsbAccessory[] accessories = mUsbManager.getAccessoryList();
		UsbAccessory accessory = (accessories == null ? null : accessories[0]);
		if (accessory != null) {
			if (mUsbManager.hasPermission(accessory)) {
				Log.d(TAG, "Permission ready.");
				openAccessory(accessory);
			} else {
				Log.d(TAG, "Requesting permission.");
				synchronized (mUsbReceiver) {
					if (!mPermissionRequestPending) {
						mUsbManager.requestPermission(accessory,
								mPermissionIntent);
						mPermissionRequestPending = true;
					}
				}
			}
		} else {
			Log.d(TAG, "mAccessory is null.");
		}
		return START_STICKY;
	}

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

	// ======================GCM STUFF=========================
	private class GetRegistrationIdTask extends AsyncTask<Void, Void, String> {
		@Override
		protected String doInBackground(Void... params) {
			String msg = "";
			try {
				String registrationId = mGcmHelper.register(SENDER_ID);
				msg = "Device registered, registration ID = " + registrationId;
				storeRegistrationId(registrationId);
			} catch (IOException ex) {
				msg = "Error :" + ex.getMessage();
			}
			return msg;
		}

		@Override
		protected void onPostExecute(String msg) {
			Log.d(TAG, msg); // For this simple demo just copy paste this reg id
								// to your backend. (yes:)
		}
	}

	// --------------------- Passing data from BroadcastReceive this Activity
	// ----------------------
	public void receivedGcmJson(JSONObject jsonObj) {
		String simpleString = "No simple string found";
		int intValue = 0;
		double floatValue = 0.0;
		try {
			simpleString = jsonObj.getString("simple_string");
			if (jsonObj.has("int_value")) {
				intValue = jsonObj.getInt("int_value");
			}
			if (jsonObj.has("float_value")) {
				floatValue = jsonObj.getDouble("float_value");
			}
		} catch (JSONException e) {
			e.printStackTrace();
		}
		Log.d(TAG, "simple_string: " + simpleString);
		Log.d(TAG, "int_value: " + intValue);
		Log.d(TAG, "float_value: " + floatValue);

		sendCommand(simpleString + '\0');
		Log.d(TAG, "Should have sent command.");
	}

	// ---------------------- SharedPreferences to store the registration id
	// -----------------------
	public static final String PROPERTY_REG_ID = "registration_id";

	private String getRegistrationId() {
		return getSharedPreferences().getString(PROPERTY_REG_ID, "");
	}

	private void storeRegistrationId(String regId) {
		final SharedPreferences prefs = getSharedPreferences();
		SharedPreferences.Editor editor = prefs.edit();
		editor.putString(PROPERTY_REG_ID, regId);
		editor.commit();
	}

	private SharedPreferences getSharedPreferences() {
		return PreferenceManager.getDefaultSharedPreferences(this);
		// return getSharedPreferences(MainActivity.class.getSimpleName(),
		// Context.MODE_PRIVATE);
	}
}
