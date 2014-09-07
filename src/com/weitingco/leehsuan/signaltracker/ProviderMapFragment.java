package com.weitingco.leehsuan.signaltracker;

import java.util.Date;

import android.annotation.TargetApi;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Color;
import android.location.Location;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.support.v4.app.LoaderManager;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.Loader;
import android.telephony.CellInfo;
import android.telephony.CellInfoCdma;
import android.telephony.CellInfoGsm;
import android.telephony.CellInfoLte;
import android.telephony.CellInfoWcdma;
import android.telephony.NeighboringCellInfo;
import android.telephony.PhoneStateListener;
import android.telephony.SignalStrength;
import android.telephony.TelephonyManager;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;
import com.weitingco.leehsuan.signaltracker.ProviderDatabaseHelper.LocationCursor;


public class ProviderMapFragment extends SupportMapFragment 
	implements LoaderCallbacks<Cursor>{
	private static final String TAG = "ProviderMapFragment";
	private static final String ARG_PROVIDER_ID = "PROVIDER_ID";
	private static final String ARG_PROVIDER = "PROVIDER_NAME";
	private static final int LOAD_LOCATIONS = 0;
	
	private static final int CENTER_RED = 0xFFFF0000;
	private static final int FILL_RED = 0x50FF0000;
	private static final int CENTER_BLUE = 0xFF00FF00;
	private static final int FILL_BLUE = 0x5000FF00;
	private static final int CENTER_GREEN = 0xFF0000FF;
	private static final int FILL_GREEN = 0x500000FF;
	
	private static final int mStrong = -73;
	private static final int mMedian = -84;
	
	private static WakeLock sWL;
	
	private TelephonyManager mTM;
	private myPhoneStateListener mListener;
	private ConnectivityManager mCM;
	
	private long mProviderId = -1;
	
	//Signal types
	private int mGSM = -1, mCDMA = -1, mWCDMA = -1, mLTE = -1;
	//Operator related Text
	private String mSimOperator, mNetOperator, mMobileNetName;
	private String mCellInfo, mNeighborCellInfo, mMobielNetConnectInfo;
	private ProviderManager mProviderManager;
	private int mCircleRadius = 1;
	private int mStrokeWidth = 1;
	
	private GoogleMap mGoogleMap;
	private LocationCursor mLocationCursor;
	
	
    private BroadcastReceiver mLocationReceiver = new LocationReceiver() {

        @Override
        protected void onLocationReceived(Context context, Location loc) {
            
        	if (!mProviderManager.isTrackingProvider(mProviderId))
                return;
            
        	SignalLocation newSignalLocation = new SignalLocation();
            //Update the Signal Location;
        	newSignalLocation.setLocation(loc);
            //SimOperator or NetOperator
        	newSignalLocation.setProvider(mNetOperator);
            if(mGSM != -1)
            	newSignalLocation.setSignalStrength(mGSM);
            newSignalLocation.setSignalType(mMobileNetName);
            //insert to the data base
            ProviderManager.get(getActivity()).insertLocation(newSignalLocation);
        	
            if (isVisible()) {
            	LatLng latLng = new LatLng(loc.getLatitude(), loc.getLongitude());
            	addCircle(latLng);
            	/*
            	MarkerOptions MidPointMarkerOptions = new MarkerOptions()
				.position(latLng)
				.icon(BitmapDescriptorFactory
						.defaultMarker(BitmapDescriptorFactory.HUE_CYAN));
            	mGoogleMap.addMarker(MidPointMarkerOptions);
            	*/
            	
            	
            }
                
        }
        
        @Override
        protected void onProviderEnabledChanged(boolean enabled) {
            int toastText = enabled ? R.string.gps_enabled : R.string.gps_disabled;
            Toast.makeText(getActivity(), toastText, Toast.LENGTH_LONG).show();
        }
        
    };
    //Copied from Provider fragment
    private class myPhoneStateListener extends PhoneStateListener{
		
		@TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
		@Override
		public void onSignalStrengthsChanged(SignalStrength signalStrength){
			super.onSignalStrengthsChanged(signalStrength);
			//Get provider names
			mSimOperator = mTM.getSimOperatorName();// return empty string in some cases
			mNetOperator = mTM.getNetworkOperatorName();
			mNetOperator = mNetOperator.replaceAll(" ", "_");
			//Try to get signal strength from API 18
			boolean isGetInfo = false;
			if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2){
				isGetInfo = getSignalStrengthFromCellInfo();
			} else{
				mCellInfo = "Your API Version is below 18"; 
			}
				
			if(!isGetInfo || 
					android.os.Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR2){
				mGSM = signalStrength.getGsmSignalStrength();
				if (mGSM != 99){
					mGSM = 2*mGSM - 113; //convert to dBm
				} else{
					mGSM = -1;
				}
				mCDMA = signalStrength.getCdmaDbm();
			}
			
			try{
				NeighboringCellInfo ncInfo = mTM.getNeighboringCellInfo().get(0);
				mCDMA = ncInfo.getRssi();
				mNeighborCellInfo = "not empty";
				//Log.d(TAG,"getNeighboringCellInfo not empty!!!");
			} catch(Exception e){
				if(mTM.getNeighboringCellInfo() == null){
					mNeighborCellInfo = "empty";
				}
				else{
					if (mTM.getNeighboringCellInfo().size() == 0){
						mNeighborCellInfo = "empty";
					} else{
						mNeighborCellInfo = "Error, Check Log!";
						//Log.e(TAG,mNeighborCellInfo,e);
					}
				}
			}
			
			NetworkInfo[] nfs = mCM.getAllNetworkInfo();
			
			for(NetworkInfo nf: nfs){
				boolean isConnected = nf.isConnected();
				mMobielNetConnectInfo = mMobielNetConnectInfo + 
						nf.getTypeName()+" "+nf.getSubtypeName()+" #"+
						String.valueOf(isConnected)+"\n";
				if(nf.getType() == ConnectivityManager.TYPE_MOBILE){
					mMobileNetName = nf.getSubtypeName();
				}
			}
			//Log.d(TAG, String.valueOf(signalStrength));
		}
	}
	
	public static ProviderMapFragment newInstance(long providerId, String providerName){
		Bundle args = new Bundle();
		args.putLong(ARG_PROVIDER_ID, providerId);
		args.putString(ARG_PROVIDER, providerName);
		ProviderMapFragment rf = new ProviderMapFragment();
		rf.setArguments(args);
		return rf;
	}
	
	@Override 
	public void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		
		mProviderManager = ProviderManager.get(getActivity());
		
		mTM = (TelephonyManager)getActivity()
				.getSystemService(Context.TELEPHONY_SERVICE);
		//get data network information
		mCM = (ConnectivityManager)getActivity()
				.getSystemService(Context.CONNECTIVITY_SERVICE);
		mListener = new myPhoneStateListener();
		mTM.listen(mListener ,PhoneStateListener.LISTEN_SIGNAL_STRENGTHS);
		//Get provider names
		mSimOperator = mTM.getSimOperatorName();// return empty string in some cases
		mNetOperator = mTM.getNetworkOperatorName();
		mNetOperator = mNetOperator.replaceAll(" ", "_");
		//Check for the Run ID as an Argument, and find the provider
		Bundle args = getArguments();
		if(args != null){
			mProviderId = args.getLong(ARG_PROVIDER_ID, -1);
			if(mProviderId != -1){
				LoaderManager lm = getLoaderManager();
				lm.initLoader(LOAD_LOCATIONS, args, this);
			}
		}
		PowerManager pm = (PowerManager) getActivity().getSystemService(Context.POWER_SERVICE);
        sWL = pm.newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK, this.getClass().getName());
        sWL.acquire();
	}
	
	@Override
	public void onDestroy(){
		super.onDestroy();
		if(sWL != null){
			sWL.release();
			sWL = null;
		}
	}
	
	@Override
    public void onStart() {
        super.onStart();
        getActivity().registerReceiver(mLocationReceiver, 
                new IntentFilter(ProviderManager.ACTION_LOCATION));
    }
	
	@Override
    public void onStop() {
        getActivity().unregisterReceiver(mLocationReceiver);
        super.onStop();
    }
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup parent,
			Bundle savedInstanceState){
		View v = super.onCreateView(inflater, parent, savedInstanceState);
		//CAN NOT DO THIS WHY?????
		//View v = inflater.inflate(R.layout.fragment_map, parent, false);
		//Stash a reference  to the GoogleMap
		mGoogleMap = getMap();
		//Show the user's location
		mGoogleMap.setMyLocationEnabled(true);
		
		return v;
	}
	
	@Override
	public Loader<Cursor> onCreateLoader(int id, Bundle args) {
		//long providerId = args.getLong(ARG_PROVIDER_ID,-1);
		String providerName = args.getString(ARG_PROVIDER);
		return new LocationListCursorLoader(getActivity(), providerName);
	}

	@Override
	public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
		mLocationCursor = (LocationCursor)cursor;
		updateUI();
	}

	@Override
	public void onLoaderReset(Loader<Cursor> arg0) {
		// Stop using the data
		mLocationCursor.close();
		mLocationCursor = null;
	}
	
	private void updateUI(){
		if (mGoogleMap == null || mLocationCursor == null)
			return;
		
		PolylineOptions line =  new PolylineOptions();
		LatLngBounds.Builder latLngBuilder = new LatLngBounds.Builder();
		
		mLocationCursor.moveToFirst();
		
		boolean firstFlag = false;
		while(!mLocationCursor.isAfterLast()){
			Location loc = mLocationCursor.getSignalLocation().getLocation();
			int signalStrength = mLocationCursor.getSignalLocation().getSignalStrength();
			LatLng latLng = new LatLng(loc.getLatitude(), loc.getLongitude());
			line.add(latLng);
			latLngBuilder.include(latLng);
			//move to the next location
			mLocationCursor.moveToNext();
			
			Resources r = getResources();
			
			//If this is the firstLocation, add a marker for it
			//if(mLocationCursor.isFirst()){
			//mLocationCursor First is very weird
			if (loc != null && !firstFlag){
				String startDate = new Date(loc.getTime()).toString();
				firstFlag = true;
				/*
				MarkerOptions startMarkerOptions = new MarkerOptions()
					.position(latLng)
					.title(r.getString(R.string.location_start))
					.snippet(r.getString(R.string.location_started_at_format, startDate));
					//.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED));
				mGoogleMap.addMarker(startMarkerOptions);*/
				addCircleFromDatabase(latLng,signalStrength);
				
			} else if (mLocationCursor.isLast()){
				//if this is the last location, and not also the first, add a marker
				/*String endDate =  new Date(loc.getTime()).toString();
				MarkerOptions finishMarkerOptions = new MarkerOptions()
					.position(latLng)
					.title(r.getString(R.string.location_finish))
					.snippet(endDate)
					.icon(BitmapDescriptorFactory
							.defaultMarker(BitmapDescriptorFactory.HUE_AZURE));
				mGoogleMap.addMarker(finishMarkerOptions);*/
				addCircleFromDatabase(latLng,signalStrength);
			} else {
				/*
				MarkerOptions MidPointMarkerOptions = new MarkerOptions()
					.position(latLng)
					.icon(BitmapDescriptorFactory
							.defaultMarker(BitmapDescriptorFactory.HUE_CYAN));
				mGoogleMap.addMarker(MidPointMarkerOptions);
				*/
				addCircleFromDatabase(latLng,signalStrength);
			}
			
			line.color(CENTER_RED);
			//Add the polyline to the Map
			//mGoogleMap.addPolyline(line);
			//Make the map zoom to show the track, with some padding
			//Use the size of the current display in pixels as a bounding box
			Display display = getActivity().getWindowManager().getDefaultDisplay();
			//Construct a movement instruction for the map camera
			LatLngBounds latLngBounds = latLngBuilder.build();
			CameraUpdate movement = CameraUpdateFactory.newLatLngBounds(latLngBounds, 
					display.getWidth(), display.getHeight(), 15);
			mGoogleMap.moveCamera(movement);
			
		}
	}
	
	//Copied from Provider fragment
	@TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
	private boolean getSignalStrengthFromCellInfo(){
		try {
		    for (final CellInfo info : mTM.getAllCellInfo()) {
		        if (info instanceof CellInfoGsm) {
		            mGSM = ((CellInfoGsm) info).getCellSignalStrength().getDbm();
		            // do what you need
		        } else if (info instanceof CellInfoCdma) {
		            mCDMA = ((CellInfoCdma) info).getCellSignalStrength().getDbm();
		            // do what you need
		        } else if (info instanceof CellInfoLte) {
		            mLTE = ((CellInfoLte) info).getCellSignalStrength().getDbm();
		            // do what you need
		        } else if (info instanceof CellInfoWcdma){
		        	mWCDMA = ((CellInfoWcdma) info).getCellSignalStrength().getDbm();
		        } else {
		            throw new Exception("Unknown type of cell signal!");
		        }
		    }
		    mCellInfo = "not empty";
		    return true;
		} catch (Exception e) {
		    //Log.e(TAG, "Unable to obtain cell signal information", e);
		    if (mTM.getAllCellInfo() == null){
		    	mCellInfo = "empty";
		    }else{
		    	if(mTM.getAllCellInfo().size() == 0)
		    		mCellInfo = "empty";
		    	else
		    		mCellInfo = "Error! Check Log!";
		    }
			return false;
		}
	}
	
	private void addCircle(LatLng latLng){
		int signalStrength = mGSM;
		int centerColor;
		int fillColor;
		if(signalStrength >= mStrong){
			centerColor = CENTER_GREEN;
			fillColor = FILL_GREEN;
		}else if(signalStrength >= mMedian){
			centerColor = CENTER_BLUE;
			fillColor = CENTER_BLUE;
		}else{
			centerColor = CENTER_RED;
			fillColor = FILL_RED;
		}
		
		mGoogleMap.addCircle(new CircleOptions()
			.center(latLng)
			.radius(mCircleRadius)
			.strokeColor(centerColor)
			.strokeWidth(mStrokeWidth)
			.fillColor(fillColor));
	}
	
	private void addCircleFromDatabase(LatLng latLng, int signalStrength){
		int centerColor;
		int fillColor;
		if(signalStrength >= mStrong){
			centerColor = CENTER_GREEN;
			fillColor = FILL_GREEN;
		}else if(signalStrength >= mMedian){
			centerColor = CENTER_BLUE;
			fillColor = CENTER_BLUE;
		}else{
			centerColor = CENTER_RED;
			fillColor = FILL_RED;
		}
		mGoogleMap.addCircle(new CircleOptions()
			.center(latLng)
			.radius(mCircleRadius)
			.strokeColor(centerColor)
			.strokeWidth(mStrokeWidth)
			.fillColor(fillColor));
	}
	
}