package com.weitingco.leehsuan.signaltracker;

import java.util.List;

import android.annotation.TargetApi;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.telephony.CellInfo;
import android.telephony.NeighboringCellInfo;
import android.telephony.PhoneStateListener;
import android.telephony.SignalStrength;
import android.telephony.TelephonyManager;
import android.telephony.gsm.GsmCellLocation;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

public class CurrentProviderFragment extends Fragment {
	private TelephonyManager mTM;
	private ConnectivityManager mCM;
	private myPhoneStateListener mListener;
	private TextView mNetOperator, mSimState, mSimOperator, mMobileNetType, mIsInDB,
		mCellInfoText, mNeighborCellInfoText, mCellLocationText;
	private String mCellInfo, mNeighborCellInfo, mMobielNetConnectInfo;
	private class myPhoneStateListener extends PhoneStateListener{
		
		@Override 
		public void onSignalStrengthsChanged(SignalStrength signalStrength){
			updateUI();
		}
	}
	
	@Override
	public void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		
		mTM = (TelephonyManager)getActivity()
				.getSystemService(Context.TELEPHONY_SERVICE);
		//get data network information
		mCM = (ConnectivityManager)getActivity()
				.getSystemService(Context.CONNECTIVITY_SERVICE);
		
		mListener = new myPhoneStateListener();
		mTM.listen(mListener ,PhoneStateListener.LISTEN_SIGNAL_STRENGTHS);
		
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup parent, 
			Bundle savedInstanceState){
		View v = inflater.inflate(R.layout.fragment_current_provider, parent, false);
		mSimOperator = (TextView) v.findViewById(R.id.current_sim_operator_name);
		mNetOperator = (TextView) v.findViewById(R.id.current_net_operator_name);
		mSimState = (TextView)v.findViewById(R.id.current_sim_operator_state);
		mMobileNetType = (TextView)v.findViewById(R.id.current_mobile_net_type);
		
		mCellInfoText = (TextView)v.findViewById(R.id.cell_info);
		mNeighborCellInfoText = (TextView)v.findViewById(R.id.neighbor_cell_info);
		mCellLocationText = (TextView)v.findViewById(R.id.cell_location);
		
		mIsInDB = (TextView)v.findViewById(R.id.current_is_in_db);
		
		updateUI();
		
		return v;
	}
	
	@TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
	private void updateUI(){
		mSimOperator.setText(mTM.getSimOperatorName());
		mNetOperator.setText(mTM.getNetworkOperatorName());
		mSimState.setText(String.valueOf(mTM.getSimState()));
		mMobileNetType.setText(getMobileNetName());
		
		String name = mTM.getNetworkOperatorName().replaceAll(" ", "_");
		Provider provider = ProviderManager.get(getActivity())
				.getProvider(name);
		if(provider != null){
			mIsInDB.setText("YES");
		}else{
			mIsInDB.setText("NO");
		}
		
		try{
			NeighboringCellInfo ncInfo = mTM.getNeighboringCellInfo().get(0);
			mNeighborCellInfo = ncInfo.toString();
			//Log.d(TAG,"getNeighboringCellInfo not empty!!!");
		} catch(Exception e){
			if(mTM.getNeighboringCellInfo() == null){
				mNeighborCellInfo = "Not Provided";
			}
			else{
				if (mTM.getNeighboringCellInfo().size() == 0){
					mNeighborCellInfo = "0";
				} else{
					mNeighborCellInfo = "Error, Check Log!";
					//Log.e(TAG,mNeighborCellInfo,e);
				}
			}
		}
		
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
		//Cell info
		if (android.os.Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR2){
			mCellInfo = "Your API Version is below 18"; 
		} else {
			try{
				List<CellInfo> cellInfo = mTM.getAllCellInfo();
				if( cellInfo.size() != 0){
					mCellInfo = "Provided";
				} else{
					mCellInfo = "0";
				}
			} catch(Exception e){
				mCellInfo = "null";
			}
		}
		
		mCellInfoText.setText(mCellInfo);
		mNeighborCellInfoText.setText(mNeighborCellInfo);
		
	}
	
	private String getMobileNetName(){
		NetworkInfo[] nfs = mCM.getAllNetworkInfo();
		
		for(NetworkInfo nf: nfs){
			//boolean isConnected = nf.isConnected();
			
			if(nf.getType() == ConnectivityManager.TYPE_MOBILE){
				return nf.getSubtypeName();
			}
		}
		return null;
	}
}
