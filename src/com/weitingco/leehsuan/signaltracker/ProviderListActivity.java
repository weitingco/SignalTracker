package com.weitingco.leehsuan.signaltracker;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;

public class ProviderListActivity extends SingleFragmentActivity {

	@Override
	protected Fragment createFragment() {
		return new ProviderListFragment();
	}
	
	@Override
    protected int getLayoutResId() {
        return R.layout.activity_twopane;
    }
	
	@Override
	public void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		
		FragmentManager fm = getSupportFragmentManager();
        Fragment fragment = fm.findFragmentById(R.id.detailFragmentContainer);
        
        if(fragment == null){
        	fragment = new CurrentProviderFragment();
        	fm.beginTransaction()
        		.add(R.id.detailFragmentContainer, fragment)
        		.commit();
        	
        }
		
	}
	
}
