package com.weitingco.leehsuan.signaltracker;

import android.support.v4.app.Fragment;

public class ProviderListActivity extends SingleFragmentActivity {

	@Override
	protected Fragment createFragment() {
		return new ProviderListFragment();
	}

}
