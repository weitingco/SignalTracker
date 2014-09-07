package com.weitingco.leehsuan.signaltracker;

import java.io.File;
import java.util.Date;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.CursorWrapper;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.location.Location;

public class ProviderDatabaseHelper extends SQLiteOpenHelper {
	//Database 
	private static final String DB_NAME = "providers.sqlite";
	private static final int VERSION = 1;
	//Table provider
	private static final String TABLE_PROVIDER = "provider";
	private static final String COLUMN_PROVIDER_ID = "_id";
	private static final String COLUMN_PROVIDER_START_DATE = "start_date";
	//private static final String COLUMN_PROVIDER_END_DATE = "end_date";
	private static final String COLUMN_PROVIDER_NAME = "name";
	//Table location
	//TABLE_LOCATION = location_providerName_Hash(location)
	private static final String TABLE_PROVIDER_LOCATION = "location_";
	private static final String CREATE_LOCATION_TABLE_IF_NOT_EXIST 
											= "CREATE TABLE IF NOT EXISTS ";
	private static final String CREATE_LOCATION_TABLE_POST = " ( timestamp integer, " +
			"signal_strength integer, latitude real, longtitude real, altitude real, "+
			"provider varchar(100), signal_type varchar(100), loc_provider varchar(100), " +
			"provider_id integer references provider(_id))";
	private static final String COLUMN_LOCATION_LATITUDE = "latitude";
	private static final String COLUMN_LOCATION_LONGTITUDE = "longtitude";
	private static final String COLUMN_LOCATION_ALTITUDE = "altitude";
	private static final String COLUMN_LOCATION_TIMESTAMP = "timestamp";
	private static final String COLUMN_LOCATION_PROVIDER = "provider";
	private static final String COLUMN_LOCATION_LOCPROVIDER = "loc_provider";
	private static final String COLUMN_LOCATION_PROVIDER_ID = "provider_id";
	private static final String COLUMN_LOCATION_SIGNAL_STRENGTH = "signal_strength";
	private static final String COLUMN_LOCATION_SIGNAL_TYPE = "signal_type";
	//private static final String COLUMN_LOCATION_BUCKET_ID = "bucket_id";
	
	public static long Hash(Location loc){
		int scaleInt = 100;
		long latFloor = ((Double)((loc.getLatitude()+90)*scaleInt)).longValue();
		long lngFloor = ((Double)((loc.getLongitude()+180)*scaleInt)).longValue();
		return lngFloor*180*scaleInt+latFloor;
	}
	//Create different signal location table 
	public static String getLocationTableName(Location loc, String provider){
		/*
		long locationHash = Hash(loc);
		return "location_"+provider+"_"+String.valueOf(locationHash);
		*/
		return TABLE_PROVIDER_LOCATION+provider;
	}
	
	public static String getLocationTableName(SignalLocation sLoc){
		/*
		long locationHash = Hash(sLoc.getLocation());
		return "location_"+sLoc.getProvider()+"_"+String.valueOf(locationHash);
		*/
		return TABLE_PROVIDER_LOCATION+sLoc.getProvider();
	}
	
	/** A convenient class to wrap a cursor that returns rows from the "provider" table.
	 * The {@link getRun()} method will give you a Run instance representing
	 * the current row.
	 */
	public static class ProviderCursor extends CursorWrapper{
		public ProviderCursor(Cursor c){
			super(c);
		}
		/**
		 * Returns a Provider object configured for the current row,
		 * or null if the current row is invalid.
		 */
		public Provider getProvider(){
			if(isBeforeFirst() || isAfterLast()) return null;
			Provider provider = new Provider();
			long providerId = getLong(getColumnIndex(COLUMN_PROVIDER_ID));
			provider.setId(providerId);
			long startDate = getLong(getColumnIndex(COLUMN_PROVIDER_START_DATE));
			provider.setStartDate(new Date(startDate));
			//long endDate = getLong(getColumnIndex(COLUMN_PROVIDER_END_DATE));
			//provider.setEndDate(new Date(endDate));
			String name = getString(getColumnIndex(COLUMN_PROVIDER_NAME));
			provider.setName(name);
			return provider;
		}
	}
	
	public static class LocationCursor extends CursorWrapper{
		public LocationCursor(Cursor c){
			super(c);
		}
		
		public SignalLocation getSignalLocation(){
			if(isBeforeFirst()||isAfterLast())
				return null;
			//First get the location provider out so that you can use the constructor
			String locProvider = getString(getColumnIndex(COLUMN_LOCATION_LOCPROVIDER));
			Location loc = new Location(locProvider);
			//Populate the remaining properties
			loc.setAltitude(getDouble(getColumnIndex(COLUMN_LOCATION_ALTITUDE)));
			loc.setLatitude(getDouble(getColumnIndex(COLUMN_LOCATION_LATITUDE)));
			loc.setLongitude(getDouble(getColumnIndex(COLUMN_LOCATION_LONGTITUDE)));
			loc.setTime(getLong(getColumnIndex(COLUMN_LOCATION_TIMESTAMP)));
			//Get the service provider name out
			String provider = getString(getColumnIndex(COLUMN_LOCATION_PROVIDER));
			SignalLocation sloc = new SignalLocation();
			sloc.setLocation(loc);
			sloc.setProvider(provider);
			sloc.setSignalStrength(getInt(getColumnIndex(COLUMN_LOCATION_SIGNAL_STRENGTH)));
			sloc.setSignalType(getString(getColumnIndex(COLUMN_LOCATION_SIGNAL_TYPE)));
			return sloc;
		}
	}
	
	
	public ProviderDatabaseHelper(Context context){
		//super(context, DB NAME, CursorFactory, version)
		super(context,DB_NAME, null, VERSION);
	}
	
	@Override
	public void onCreate(SQLiteDatabase db) {
		//Create the "provider" table
		db.execSQL("create table provider (" + 
				"_id integer primary key autoincrement, " +
				"name varchar(100), start_date integer)");

	}
	
	public void createProviderLocationTable(String provider){
		String tableName = TABLE_PROVIDER_LOCATION+provider;
		getWritableDatabase().execSQL("create table "+ tableName+
				CREATE_LOCATION_TABLE_POST);
	}
	
	public void deleteProviderLocationTable(String providerName){
		String tableName = TABLE_PROVIDER_LOCATION + providerName;
		getWritableDatabase().execSQL("DROP TABLE IF EXISTS " + tableName);
		createProviderLocationTable(providerName);
	}
	
	public ProviderCursor queryProviders(){
		//Equivalent to "select * from run order by start_date asc"
		Cursor wrapped = getReadableDatabase().query(TABLE_PROVIDER, 
				null, null, null, null, null, COLUMN_PROVIDER_ID+" asc");
		return new ProviderCursor(wrapped);
	}
	
	public ProviderCursor queryProvider(long id){
		Cursor wrapped = getReadableDatabase().query(TABLE_PROVIDER, 
				null, //All columns
				COLUMN_PROVIDER_ID + " = ?", //Look for a provider ID
				new String[] {String.valueOf(id)},  // with this value
				null, //group by
				null, //having
				null, //order by
				"1"); //limit the return row;
		return new ProviderCursor(wrapped);
	}
	
	public ProviderCursor queryProvider(String provider){
		Cursor wrapped = getReadableDatabase().query(TABLE_PROVIDER, 
				null, //All columns
				COLUMN_PROVIDER_NAME + " = ?", //Look for a provider ID
				new String[] {provider},  // with this value
				null, //group by
				null, //having
				null, //order by
				"1"); //limit the return row;
		return new ProviderCursor(wrapped);
	} 
	
	public LocationCursor queryLocation(Location loc, String provider){
		String tableName = getLocationTableName(loc,provider);
		Cursor wrapped = getReadableDatabase().query(tableName,
				null,
				COLUMN_LOCATION_PROVIDER + "= ?",
				new String[] {provider},
				null,
				null,
				COLUMN_LOCATION_LATITUDE+" asc"); //order by
		return new LocationCursor(wrapped);
	}
	
	
	public long insertProvider(Provider provider){
		ContentValues cv = new ContentValues();
		cv.put(COLUMN_PROVIDER_START_DATE, provider.getStartDate().getTime());
		//if(provider.getEndDate() != null)
		//	cv.put(COLUMN_PROVIDER_END_DATE, provider.getEndDate().getTime());
		cv.put(COLUMN_PROVIDER_NAME, provider.getName());
		return getWritableDatabase().insert(TABLE_PROVIDER, null, cv);
	}
	
	public long insertLocation(long providerId, SignalLocation sigLoc){
		ContentValues cv = new ContentValues();
		cv.put(COLUMN_LOCATION_ALTITUDE, sigLoc.getLocation().getAltitude());
		cv.put(COLUMN_LOCATION_LATITUDE, sigLoc.getLocation().getLatitude());
		cv.put(COLUMN_LOCATION_LOCPROVIDER, sigLoc.getLocation().getProvider());
		cv.put(COLUMN_LOCATION_LONGTITUDE, sigLoc.getLocation().getLongitude());
		cv.put(COLUMN_LOCATION_PROVIDER, sigLoc.getProvider());
		cv.put(COLUMN_LOCATION_PROVIDER_ID, providerId);
		cv.put(COLUMN_LOCATION_TIMESTAMP, sigLoc.getLocation().getTime());
		cv.put(COLUMN_LOCATION_SIGNAL_STRENGTH, sigLoc.getSignalStrength());
		cv.put(COLUMN_LOCATION_SIGNAL_TYPE, sigLoc.getSignalType());
		return getWritableDatabase().insert(
				getLocationTableName(sigLoc), null, cv);
	}
	
	
	
	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		// TODO Auto-generated method stub
	}
	public LocationCursor queryLocationForProvider(String provider){
		String tableName = TABLE_PROVIDER_LOCATION+provider;
		Cursor wrapped = getReadableDatabase().query(tableName, 
				null, // all column
				null, //COLUMN_LOCATION_RUN_ID + " = ?", //limit to the given run
				null, //new String[]{String.valueOf(runId)}, 
				null, //group by
				null, 
				COLUMN_LOCATION_TIMESTAMP+" asc");
		
		return new LocationCursor(wrapped);
	}

}
