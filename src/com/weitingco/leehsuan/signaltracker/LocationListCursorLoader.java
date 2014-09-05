package com.weitingco.leehsuan.signaltracker;

import android.content.Context;
import android.database.Cursor;

public class LocationListCursorLoader extends SQLiteCursorLoader {
	
	private String mProvider;
			
	public LocationListCursorLoader(Context c, String provider){
		super(c);
		mProvider = provider;
	}

	
	@Override
	protected Cursor loadCursor(){
		return ProviderManager.get(getContext()).queryLocationForProvider(mProvider);
	}

}
