package com.weitingco.leehsuan.signaltracker;

import android.location.Location;

public class SignalLocation {
	private Location mLocation;
	private int mSignalStrength;
	private String provider;
	private String mSignalType;
	//private Date mDate;

	public SignalLocation(){
		//mDate = new Date();
	}
	
	public Location getLocation() {
		return mLocation;
	}
	public void setLocation(Location location) {
		mLocation = location;
	}
	public int getSignalStrength() {
		return mSignalStrength;
	}
	public void setSignalStrength(int signalStrength) {
		mSignalStrength = signalStrength;
	}

	public String getProvider() {
		return provider;
	}

	public void setProvider(String provider) {
		this.provider = provider;
	}
	/*
	public Date getDate() {
		return mDate;
	}

	public void setDate(Date date) {
		mDate = date;
	}
	*/
	public String getSignalType() {
		return mSignalType;
	}

	public void setSignalType(String signalType) {
		mSignalType = signalType;
	}
	
}
