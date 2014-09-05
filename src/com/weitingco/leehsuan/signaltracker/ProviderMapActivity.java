package com.weitingco.leehsuan.signaltracker;

import android.support.v4.app.Fragment;

public class ProviderMapActivity extends SingleFragmentActivity {
	/** A key for passing a run ID as a long */
	public static final String EXTRA_PROVIDER_ID = 
			"com.weitingco.leehsuan.signaltracker.provider_id";
	public static final String EXTRA_PROVIDER_NAME = 
			"com.weitingco.leehsuan.signaltracker.provider_name";
	
	@Override
	protected Fragment createFragment() {
		long providerId = getIntent().getLongExtra(EXTRA_PROVIDER_ID, -1);
		String providerName = getIntent().getStringExtra(EXTRA_PROVIDER_NAME);
		if(providerId != -1){
			return ProviderMapFragment.newInstance(providerId, providerName);
		}else{
			return new ProviderMapFragment();
		}
	}

}
