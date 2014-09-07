package com.weitingco.leehsuan.signaltracker;

import java.io.File;

import android.annotation.TargetApi;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.Location;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.support.v4.app.Fragment;
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
import android.telephony.gsm.GsmCellLocation;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

public class ProviderFragment extends Fragment {
	private static final String TAG = "ProviderFragment";
	private static final String ARG_PROVIDER_ID = "PROVIDER_ID";
	private static final String ARG_PROVIDER = "PROVIDER_NAME";
	//LOADER FUNCTION
	private static final int LOAD_PROVIDER = 0;
    private static final int LOAD_LOCATION = 1;
    private static WakeLock sWL; 
	
	private TelephonyManager mTM;
	private myPhoneStateListener mListener;
	private ConnectivityManager mCM;
	
	private TextView netText, simText, cdmaText, gsmText, wcdmaText, mSimStateText,
		lteText, mCellInfoText, mNeighborCellInfoText, mCellLocationText,
		mMobileNetText, mAltitudeText, mLatitudeText, mLongtitudeText;
	private Button mStopButton, mStartButton, mMapButton;
	//Signal types
	private int mGSM = -1, mCDMA = -1, mWCDMA = -1, mLTE = -1;
	//Operator related Text
	private String mSimOperator, mNetOperator, mMobileNetName;
	//Cell phone support function
	private String mCellInfo, mNeighborCellInfo, mMobielNetConnectInfo;
	private ProviderManager mProviderManager;
	private Provider mProvider;
	private Location mLastLocation;
	private SignalLocation mLastSignalLocation = new SignalLocation();
	
	//anonymous class
    private BroadcastReceiver mLocationReceiver = new LocationReceiver() {

        @Override
        protected void onLocationReceived(Context context, Location loc) {
        	
            if (!mProviderManager.isTrackingProvider(mProvider))
                return;
            
            mLastLocation = loc;
            //Update the Signal Location;
            mLastSignalLocation.setLocation(mLastLocation);
            //SimOperator or NetOperator
            mLastSignalLocation.setProvider(mNetOperator);
            if(mGSM != -1)
            	mLastSignalLocation.setSignalStrength(mGSM);
            mLastSignalLocation.setSignalType(mMobileNetName);
            //insert to the data base
            ProviderManager.get(getActivity()).insertLocation(mLastSignalLocation);
            
            Log.d(TAG, this + " Got location from "+ loc.getProvider() + ": "
    				+ loc.getLatitude()+", "+loc.getLongitude());
            
            if (isVisible()) 
                updateUI();
        }
        
        @Override
        protected void onProviderEnabledChanged(boolean enabled) {
            int toastText = enabled ? R.string.gps_enabled : R.string.gps_disabled;
            Toast.makeText(getActivity(), toastText, Toast.LENGTH_LONG).show();
        }
        
    };

	private class myPhoneStateListener extends PhoneStateListener{
		
		@TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
		@Override
		public void onSignalStrengthsChanged(SignalStrength signalStrength){
			super.onSignalStrengthsChanged(signalStrength);
			
			if(!mProviderManager.isTrackingProvider(mProvider)) return;
			
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
			
			if(isVisible()){
				updateUI();
			}
			//Log.d(TAG, String.valueOf(signalStrength));
		}
	}
	
	public static ProviderFragment newInstance(long providerId) {
        Bundle args = new Bundle();
        args.putLong(ARG_PROVIDER_ID, providerId);
        ProviderFragment pf = new ProviderFragment();
        pf.setArguments(args);
        return pf;
    }
	
	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater){
		super.onCreateOptionsMenu(menu, inflater);
		inflater.inflate(R.menu.provider_fragment_options, menu);
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item){
		switch(item.getItemId()){
			case R.id.menu_item_delete_table:
				if(mProvider != null){
					mProviderManager.deleteProviderLocationTable(mProvider.getName());
				}
				return true;
			default:
				return super.onOptionsItemSelected(item);
		}
	}
	
	public void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		
		setHasOptionsMenu(true);
		
		mProviderManager = ProviderManager.get(getActivity());
		
		setRetainInstance(true);
		
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
		Bundle args = getArguments();
        if (args != null) {
            long providerId = args.getLong(ARG_PROVIDER_ID, -1);
            if (providerId != -1) {
                LoaderManager lm = getLoaderManager();
                lm.initLoader(LOAD_PROVIDER, args, new ProviderLoaderCallbacks());
                //Implement loader for retrieving provider's data
                //mLastLocation = mRunManager.getLastLocationForRun(runId);
                //lm.initLoader(LOAD_LOCATION, args, new LocationLoaderCallbacks());
            }
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
	public void onDestroy(){
		if(sWL != null){
        	sWL.release();
        	sWL = null;
        }
		super.onDestroy();
	}
	
	public View onCreateView(LayoutInflater inflater, ViewGroup parent, 
			Bundle savedInstanceState){
		View v = inflater.inflate(R.layout.fragment_provider_info, parent, false);
		//Bind all TextViews
		simText = (TextView)v.findViewById(R.id.sim_operator_name);
		netText = (TextView)v.findViewById(R.id.net_operator_name);
		//activeNetworkText = (TextView)v.findViewById(R.id.active_network);
		//ssText = (TextView)v.findViewById(R.id.ss_toString);
		mCellInfoText = (TextView)v.findViewById(R.id.cell_info);
		mNeighborCellInfoText = (TextView)v.findViewById(R.id.neighbor_cell_info);
		mMobileNetText = (TextView)v.findViewById(R.id.mobile_active_network);
		mSimStateText = (TextView)v.findViewById(R.id.sim_operator_state);
		//Signal Text
		gsmText = (TextView)v.findViewById(R.id.signal_strength_gsm);
		cdmaText = (TextView)v.findViewById(R.id.signal_strength_cdma);
		wcdmaText = (TextView)v.findViewById(R.id.signal_strength_wcdma);
		lteText = (TextView)v.findViewById(R.id.signal_strength_lte);
		//Location Text
		
		mAltitudeText = (TextView)v.findViewById(R.id.fragment_provider_altitude);
		mLatitudeText = (TextView)v.findViewById(R.id.fragment_provider_latitude);
		mLongtitudeText = (TextView)v.findViewById(R.id.fragment_provider_longtitude);
		
		//Cell location text
		mCellLocationText = (TextView)v.findViewById(R.id.cell_location);
		mCellLocationText.setText("Cell location is not provided...");

		//Buttons
		mStartButton =(Button)v.findViewById(R.id.fragment_provider_start_button);
		mStartButton.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
		        //mProviderManager.startTrackingProvider();
				PowerManager pm = (PowerManager) getActivity().getSystemService(Context.POWER_SERVICE);
		        sWL = pm.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK, TAG);
		        sWL.acquire();
				if (mProvider == null) {
					Log.d(TAG,mNetOperator);
                    mProvider = mProviderManager.startNewProvider(mNetOperator);
                } else {
                	mProviderManager.startTrackingProvider(mProvider);
                }
				updateUI();
			}
		});
		mStopButton = (Button)v.findViewById(R.id.fragment_provider_stop_button);
		mStopButton.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
		        mProviderManager.stopTracking();
		        if(sWL != null){
		        	sWL.release();
		        	sWL = null;
		        }
		        updateUI();
			}
		});
		
		mMapButton = (Button)v.findViewById(R.id.fragment_provider_map_button);
		mMapButton.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				if(sWL != null){
		        	sWL.release();
		        	sWL = null;
		        }
				Intent i = new Intent(getActivity(), ProviderMapActivity.class);
				i.putExtra(ProviderMapActivity.EXTRA_PROVIDER_ID, mProvider.getId());
				i.putExtra(ProviderMapActivity.EXTRA_PROVIDER_NAME, mProvider.getName());
				startActivity(i);
			}
		});
		
		
		updateUI();
		return v;
		
	}
	
	private void updateUI(){
		//Update operator name and state
		if(!mSimOperator.isEmpty()){
			simText.setText(mSimOperator);
		} else {
			simText.setText("null");
		}
		
		mSimStateText.setText(String.valueOf(mTM.getSimState()));
		if(mProvider != null){
			netText.setText(mProvider.getName());
		}
		//Update Signal 
		if(mGSM  != -1)
			gsmText.setText(String.valueOf(mGSM));
		if(mCDMA != -1)
			cdmaText.setText(String.valueOf(mCDMA));
		if(mWCDMA != -1)
			wcdmaText.setText(String.valueOf(mWCDMA));
		if(mLTE != -1)
			lteText.setText(String.valueOf(mLTE));
		
		mCellInfoText.setText(mCellInfo);
		mNeighborCellInfoText.setText(mNeighborCellInfo);
		mMobileNetText.setText(mMobileNetName);
		
		//Get cell location
		if(mTM.getNetworkType() != TelephonyManager.NETWORK_TYPE_CDMA){	
			GsmCellLocation cl = (GsmCellLocation)mTM.getCellLocation();
			try{
				mCellLocationText.setText(cl.toString());
			} catch(Exception e){
				mCellLocationText.setText("Error! Check Log!");
				//Log.d(TAG, "GsmCellLocation");
			}
		}
		
		
		//Update Location
		if(mLastLocation != null){
			mAltitudeText.setText(String.valueOf(mLastLocation.getAltitude()));
			mLongtitudeText.setText(String.valueOf(mLastLocation.getLongitude()));
			mLatitudeText.setText(String.valueOf(mLastLocation.getLatitude()));
		}
		
		//Button
		boolean started = mProviderManager.isTrackingProvider();
        boolean trackingThisProvider = mProviderManager.isTrackingProvider(mProvider);
        
        mStartButton.setEnabled(!started);
		mStopButton.setEnabled(started && trackingThisProvider);
	}
	
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
	
	private class ProviderLoaderCallbacks implements LoaderCallbacks<Provider> {
        
        @Override
        public Loader<Provider> onCreateLoader(int id, Bundle args) {
            return new ProviderLoader(getActivity(), args.getLong(ARG_PROVIDER_ID));
        }

        @Override
        public void onLoadFinished(Loader<Provider> loader, Provider provider) {
            mProvider = provider;
            updateUI();
        }

        @Override
        public void onLoaderReset(Loader<Provider> loader) {
            // do nothing
        }
    }
}
