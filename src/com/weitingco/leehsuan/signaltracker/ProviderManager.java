package com.weitingco.leehsuan.signaltracker;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Location;
import android.location.LocationManager;
import android.telephony.TelephonyManager;
import android.util.Log;

import com.weitingco.leehsuan.signaltracker.ProviderDatabaseHelper.LocationCursor;
import com.weitingco.leehsuan.signaltracker.ProviderDatabaseHelper.ProviderCursor;



public class ProviderManager {
	public static final String TAG = "ProviderManager";
	public static final String ACTION_LOCATION = 
			"com.weitingco.leehsuan.signaltracker.ACTION_LOCATION";
	
	private static final String TEST_LOC_PROVIDER ="TEST_PROVIDER";
	private static final String PREFS_FILE = "providers";
	private static final String PREF_CURRENT_PROVIDER_ID = "ProviderManager.currentProviderId";
	private static final String PREF_CURRENT_PROVIDER_NAME = "ProviderManager.currentProviderName";
	
	private static ProviderManager sProviderManager;
	private Context mAppContext;
	private LocationManager mLocationManager;
	private ProviderDatabaseHelper mHelper;
	private SharedPreferences mPrefs;
	private TelephonyManager mTelephonyManager;
	private String mProviderName;
	private long mCurrentProviderId;
	
	
	private ProviderManager(Context appContext){
		mAppContext = appContext;
		mLocationManager = (LocationManager)mAppContext
				.getSystemService(Context.LOCATION_SERVICE);
		mHelper = new ProviderDatabaseHelper(mAppContext);
		mPrefs = mAppContext.getSharedPreferences(PREFS_FILE, Context.MODE_PRIVATE);
		mTelephonyManager = (TelephonyManager)mAppContext
				.getSystemService(Context.TELEPHONY_SERVICE);
		//mProviderName = mTelephonyManager.getNetworkOperatorName();
		mCurrentProviderId = mPrefs.getLong(PREF_CURRENT_PROVIDER_ID, -1);
	}
	
	public static ProviderManager get(Context c){
		if(sProviderManager == null ){
			sProviderManager = new ProviderManager(c.getApplicationContext());
		}
		return sProviderManager;
	}
	
	public void recreateDatabase(Context appContext){
		mHelper = new ProviderDatabaseHelper(mAppContext);
	}
	
	private PendingIntent getLocationPendingIntent(boolean shouldCreate){
		Intent broadcast = new Intent(ACTION_LOCATION);
		int flags = shouldCreate ? 0:PendingIntent.FLAG_NO_CREATE;
		return PendingIntent.getBroadcast(mAppContext, 0, broadcast, flags);
	}
	
	public void startLocationUpdates(){
		String locProvider = LocationManager.GPS_PROVIDER;
		//If you have the test provider and it's enable, use it
		if(mLocationManager.getProvider(TEST_LOC_PROVIDER) != null &&
				mLocationManager.isProviderEnabled(TEST_LOC_PROVIDER)){
			locProvider = TEST_LOC_PROVIDER;
		}
		Log.d(TAG,"Using provider "+locProvider);
		//get the last known location and broadcast it if you have one
		Location lastKnown = mLocationManager.getLastKnownLocation(locProvider);
		if(lastKnown != null){
			lastKnown.setTime(System.currentTimeMillis());
			broadcastLocation(lastKnown);
		}
		
		//start update from the location manager
		PendingIntent pi = getLocationPendingIntent(true);
		mLocationManager.requestLocationUpdates(locProvider, 0, 0, pi);
	}
	
	public boolean isTrackingProvider(){
		return getLocationPendingIntent(false) != null;
	}
	
	public boolean isTrackingProvider(Provider provider){
		return provider != null && provider.getId() == mCurrentProviderId;
	}
	
	public boolean isTrackingProvider(long providerId){
		return providerId != -1 && providerId == mCurrentProviderId;
	}
	
	public void stopLocationUpdates(){
		PendingIntent pi = getLocationPendingIntent(false);
		if(pi != null){
			mLocationManager.removeUpdates(pi);
			pi.cancel();
		}
	}
	
	private void broadcastLocation(Location location){
		Intent broadcast = new Intent(ACTION_LOCATION);
		broadcast.putExtra(LocationManager.KEY_LOCATION_CHANGED, location);
		mAppContext.sendBroadcast(broadcast);
	}
	
	public void stopTracking(){
		stopLocationUpdates();
		mCurrentProviderId = -1;
		mPrefs.edit().remove(PREF_CURRENT_PROVIDER_ID).commit();
	}
	
	public void startTrackingProvider(Provider provider){
		//keep the Id
		mCurrentProviderId = provider.getId();
		mProviderName = provider.getName();
		//Store it into shared preferences
		mPrefs.edit().putLong(PREF_CURRENT_PROVIDER_ID, mCurrentProviderId).commit();
		mPrefs.edit().putString(PREF_CURRENT_PROVIDER_NAME, mProviderName).commit();
		//start location updates
		startLocationUpdates();
	}
	//TESTING
	public void startTrackingProvider(){
		//keep the Id
		mCurrentProviderId = 0; //provider.getId();
		//Store it into shared preferences
		mPrefs.edit().putLong(PREF_CURRENT_PROVIDER_ID, mCurrentProviderId).commit();
		//start location updates
		startLocationUpdates();
	}
	
	
	private Provider insertProvider(){
		Provider provider = new Provider();
		provider.setId(mHelper.insertProvider(provider));
		return provider;
	}
	
	private Provider insertProvider(String name){
		Provider provider = new Provider(name);
		provider.setId(mHelper.insertProvider(provider));
		return provider;
	}
	
	public Provider startNewProvider(){
		Provider provider = insertProvider();
		startTrackingProvider(provider);
		return provider;
	}
	
	public Provider startNewProvider(String operatorName){
		Provider provider = insertProvider(operatorName);
		createProviderLocationTable(operatorName);
		startTrackingProvider(provider);
		return provider;
	}
	
	public void insertLocation(SignalLocation loc){
		if(mCurrentProviderId != -1){
			mHelper.insertLocation(mCurrentProviderId, loc);
		} else{
			Log.e(TAG,"Location received with no tracking provider; ignoring.");
		}
	}
	
	//Query
	public ProviderCursor queryProviders(){
		return mHelper.queryProviders();
	}
	
	public Provider getProvider(long id){
		Provider provider = null;
		ProviderCursor cursor = mHelper.queryProvider(id);
		cursor.moveToFirst();
		//If you get a row, get a run
		if(!cursor.isAfterLast()) provider = cursor.getProvider();
		cursor.close();
		return provider;
	}
	
	public void createProviderLocationTable(String provider){
		mHelper.createProviderLocationTable(provider);
	}
	
	public void deleteProviderLocationTable(String providerName){
		mHelper.deleteProviderLocationTable(providerName);
	}
	
	
	public LocationCursor queryLocationForProvider(String provider){
		return mHelper.queryLocationForProvider(provider);
	}
}
