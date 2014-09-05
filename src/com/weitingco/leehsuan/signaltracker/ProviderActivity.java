package com.weitingco.leehsuan.signaltracker;

import android.support.v4.app.Fragment;

public class ProviderActivity extends SingleFragmentActivity {

	/** A key for passing a run Id as a long*/
	public static final String EXTRA_PROVIDER_ID = 
			"com.weitingco.leehsuan.signaltracker";
	@Override
	protected Fragment createFragment() {
		long runId = getIntent().getLongExtra(EXTRA_PROVIDER_ID, -1);
		if(runId != -1){
			return ProviderFragment.newInstance(runId);
		} else{
			return new ProviderFragment();
		}
	}

}
