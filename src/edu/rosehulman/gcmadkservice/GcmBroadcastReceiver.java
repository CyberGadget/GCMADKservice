/*
 * Copyright 2013 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package edu.rosehulman.gcmadkservice;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import com.google.android.gms.gcm.GoogleCloudMessaging;

/**
 * This {@code WakefulBroadcastReceiver} takes care of creating and managing a
 * partial wake lock for your app. It passes off the work of processing the GCM
 * message to an {@code IntentService}, while ensuring that the device does not
 * go back to sleep in the transition. The {@code IntentService} calls
 * {@code GcmBroadcastReceiver.completeWakefulIntent()} when it is ready to
 * release the wake lock.
 */

public class GcmBroadcastReceiver extends BroadcastReceiver {

	@Override
	public void onReceive(Context context, Intent intent) {
		Bundle extras = intent.getExtras();
		GoogleCloudMessaging gcm = GoogleCloudMessaging.getInstance(context);
		String messageType = gcm.getMessageType(intent);
		Log.d("GCM", "Message type:" + messageType);
		
		if (!extras.isEmpty()) { // has effect of unparcelling Bundle
			if (GoogleCloudMessaging.MESSAGE_TYPE_SEND_ERROR.equals(messageType)) {
				//Toast.makeText(MainActivity.mMainActivity, "Error: " + extras.toString(), Toast.LENGTH_SHORT).show();
			} else if (GoogleCloudMessaging.MESSAGE_TYPE_DELETED.equals(messageType)) {
				//Toast.makeText(MainActivity.mMainActivity, "Deleted message: " + extras.toString(), Toast.LENGTH_SHORT).show();
			} else if (GoogleCloudMessaging.MESSAGE_TYPE_MESSAGE.equals(messageType)) {
				// If it's a regular GCM message, do some work.
				try {
					JSONObject jsonObj = new JSONObject(extras.getString("data"));
					GCMADKservice.mService.receivedGcmJson(jsonObj);
//					Log.d("GCM", "simple_string: " + jsonObj.getString("simple_string"));
//					Log.d("GCM", "int_value: " + jsonObj.getInt("int_value"));
//					Log.d("GCM", "float_value: " + jsonObj.getDouble("float_value"));
				} catch (JSONException e) {
					e.printStackTrace();
				} catch (NullPointerException e) {
					Log.d("GCM", "GCMADKservice tried to receive GCM, but was null.");
				}
			}
		}
	}
}
