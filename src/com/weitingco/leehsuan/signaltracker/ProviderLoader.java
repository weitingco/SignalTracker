package com.weitingco.leehsuan.signaltracker;

import android.content.Context;

public class ProviderLoader extends DataLoader<Provider> {
	private long mProviderId;
	
	public ProviderLoader(Context context, long runId){
		super(context);
		mProviderId = runId;
	}
	
	@Override 
	public Provider loadInBackground(){
		return ProviderManager.get(getContext()).getProvider(mProviderId);
	}
}
