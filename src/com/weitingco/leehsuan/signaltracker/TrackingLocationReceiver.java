package com.weitingco.leehsuan.signaltracker;

import android.content.Context;
import android.location.Location;
import android.net.ConnectivityManager;
import android.telephony.TelephonyManager;
import android.util.Log;

public class TrackingLocationReceiver extends LocationReceiver {
	private static final String TAG = "TrackingLocationReceiver";
	
	@Override
	protected void onLocationReceived(Context c, Location loc){
		
		Log.d(TAG, this + " Got location from "+ loc.getProvider() + ": "
				+ loc.getLatitude()+", "+loc.getLongitude());
	}
}
