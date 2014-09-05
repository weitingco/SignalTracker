package com.weitingco.leehsuan.signaltracker;

import java.util.Date;

import android.content.res.Resources;
import android.database.Cursor;
import android.location.Location;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.Loader;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;
import com.weitingco.leehsuan.signaltracker.ProviderDatabaseHelper.LocationCursor;


public class ProviderMapFragment extends SupportMapFragment 
	implements LoaderCallbacks<Cursor>{
	private static final String ARG_PROVIDER_ID = "PROVIDER_ID";
	private static final String ARG_PROVIDER = "PROVIDER_NAME";
	private static final int LOAD_LOCATIONS = 0;
	private static final int RED = 0xFFFF0000;
	
	private GoogleMap mGoogleMap;
	private LocationCursor mLocationCursor;
	
	public static ProviderMapFragment newInstance(long providerId, String providerName){
		Bundle args = new Bundle();
		args.putLong(ARG_PROVIDER_ID, providerId);
		args.putString(ARG_PROVIDER, providerName);
		ProviderMapFragment rf = new ProviderMapFragment();
		rf.setArguments(args);
		return rf;
	}
	
	@Override 
	public void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		
		//Check for the Run ID as an Argument, and find the run
		Bundle args = getArguments();
		if(args != null){
			long providerId = args.getLong(ARG_PROVIDER_ID, -1);
			if(providerId != -1){
				LoaderManager lm = getLoaderManager();
				lm.initLoader(LOAD_LOCATIONS, args, this);
			}
		}
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup parent,
			Bundle savedInstanceState){
		View v = super.onCreateView(inflater, parent, savedInstanceState);
		//CAN NOT DO THIS WHY?????
		//View v = inflater.inflate(R.layout.fragment_map, parent, false);
		//Stash a reference  to the GoogleMap
		mGoogleMap = getMap();
		//Show the user's location
		mGoogleMap.setMyLocationEnabled(true);
		
		return v;
	}
	
	@Override
	public Loader<Cursor> onCreateLoader(int id, Bundle args) {
		//long providerId = args.getLong(ARG_PROVIDER_ID,-1);
		String providerName = args.getString(ARG_PROVIDER);
		return new LocationListCursorLoader(getActivity(), providerName);
	}

	@Override
	public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
		mLocationCursor = (LocationCursor)cursor;
		updateUI();
	}

	@Override
	public void onLoaderReset(Loader<Cursor> arg0) {
		// Stop using the data
		mLocationCursor.close();
		mLocationCursor = null;
	}
	
	private void updateUI(){
		if (mGoogleMap == null || mLocationCursor == null)
			return;
		
		PolylineOptions line =  new PolylineOptions();
		LatLngBounds.Builder latLngBuilder = new LatLngBounds.Builder();
		
		mLocationCursor.moveToFirst();
		
		boolean firstFlag = false;
		while(!mLocationCursor.isAfterLast()){
			Location loc = mLocationCursor.getSignalLocation().getLocation();
			LatLng latLng = new LatLng(loc.getLatitude(), loc.getLongitude());
			line.add(latLng);
			latLngBuilder.include(latLng);
			//move to the next location
			mLocationCursor.moveToNext();
			
			Resources r = getResources();
			
			//If this is the firstLocation, add a marker for it
			//if(mLocationCursor.isFirst()){
			//mLocationCursor First is very weird
			if (loc != null && !firstFlag){
				String startDate = new Date(loc.getTime()).toString();
				firstFlag = true;
				MarkerOptions startMarkerOptions = new MarkerOptions()
					.position(latLng)
					.title(r.getString(R.string.location_start))
					.snippet(r.getString(R.string.location_started_at_format, startDate));
					//.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED));
				mGoogleMap.addMarker(startMarkerOptions);
				
			} else if (mLocationCursor.isLast()){
				//if this is the last location, and not also the first, add a marker
				String endDate =  new Date(loc.getTime()).toString();
				MarkerOptions finishMarkerOptions = new MarkerOptions()
					.position(latLng)
					.title(r.getString(R.string.location_finish))
					.snippet(endDate)
					.icon(BitmapDescriptorFactory
							.defaultMarker(BitmapDescriptorFactory.HUE_AZURE));
				mGoogleMap.addMarker(finishMarkerOptions);
			} else {
				MarkerOptions MidPointMarkerOptions = new MarkerOptions()
					.position(latLng)
					.icon(BitmapDescriptorFactory
							.defaultMarker(BitmapDescriptorFactory.HUE_CYAN));
				mGoogleMap.addMarker(MidPointMarkerOptions);
			}
			
			line.color(RED);
			//Add the polyline to the Map
			//mGoogleMap.addPolyline(line);
			//Make the map zoom to show the track, with some padding
			//Use the size of the current display in pixels as a bounding box
			Display display = getActivity().getWindowManager().getDefaultDisplay();
			//Construct a movement instruction for the map camera
			LatLngBounds latLngBounds = latLngBuilder.build();
			CameraUpdate movement = CameraUpdateFactory.newLatLngBounds(latLngBounds, 
					display.getWidth(), display.getHeight(), 15);
			mGoogleMap.moveCamera(movement);
			
		}
	}
	
}