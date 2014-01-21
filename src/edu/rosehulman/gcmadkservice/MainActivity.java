package edu.rosehulman.gcmadkservice;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

public class MainActivity extends Activity {
	/** Called when the activity is first created. */

	private String TAG = "M";

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		Log.d(TAG, "onCreate entered");

		Intent thisIntent = getIntent();
		Intent passIntent = new Intent(this, GCMADKservice.class);
		passIntent.putExtras(thisIntent);
		// intent.fillIn(getIntent(), 0); // TODO: Find better way to get extras
		// for `UsbManager.getAccessory()` use?
		startService(passIntent);

		Log.d(TAG, "onCreate exited");
	}
}