package com.weitingco.leehsuan.signaltracker;

import java.util.Date;

public class Provider {
	private long mId;
	private String mName;
	private Date mStartDate;
	//private Date mEndDate;
	
	public Provider(){
		mId = -1;
		mName = null;
		mStartDate = new Date();
	}
	
	public Provider(String name){
		mId = -1;
		mName = name;
		mStartDate = new Date();
	}
	
	
	public long getId() {
		return mId;
	}
	public void setId(long id) {
		mId = id;
	}


	public String getName() {
		return mName;
	}


	public void setName(String name) {
		mName = name;
	}


	public Date getStartDate() {
		return mStartDate;
	}


	public void setStartDate(Date startDate) {
		mStartDate = startDate;
	}
	
	
}
