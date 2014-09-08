package com.weitingco.leehsuan.signaltracker;

import android.content.Context;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.telephony.PhoneStateListener;
import android.telephony.SignalStrength;
import android.telephony.TelephonyManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

public class CurrentProviderFragment extends Fragment {
	private TelephonyManager mTM;
	private ConnectivityManager mCM;
	private myPhoneStateListener mListener;
	private TextView mNetOperator, mSimState, mSimOperator;
	
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
		
		updateUI();
		
		return v;
	}
	
	private void updateUI(){
		mSimOperator.setText(mTM.getSimOperatorName());
		mNetOperator.setText(mTM.getNetworkOperatorName());
		mSimState.setText(String.valueOf(mTM.getSimState()));
	}
}
