package com.weitingco.leehsuan.signaltracker;

import java.io.File;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.Loader;
import android.support.v4.widget.CursorAdapter;
import android.telephony.TelephonyManager;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ListView;
import android.widget.TextView;

import com.weitingco.leehsuan.signaltracker.ProviderDatabaseHelper.ProviderCursor;

public class ProviderListFragment extends ListFragment implements LoaderCallbacks<Cursor> {
	
	private static final int REQUEST_NEW_PROVIDER = 0;
	static private TelephonyManager mTM;
	static private boolean sIsProviderInDatabase = false;
	static private long sCurrentProviderId;
	
	private static class ProviderListCursorLoader extends SQLiteCursorLoader{
		
		public ProviderListCursorLoader(Context context){
			super(context);
		}
		
		@Override
		protected Cursor loadCursor(){
			//Query the list of runs
			Cursor cursor = ProviderManager.get(getContext()).queryProviders();
			return cursor;
		}
	}
    
	
	@Override
	public void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		setHasOptionsMenu(true);
		mTM = (TelephonyManager)getActivity()
				.getSystemService(Context.TELEPHONY_SERVICE);
		//initialize the loader to load the list of item
		getLoaderManager().initLoader(0, //id 
				null,  //Bundle
				this); //LoaderCallbacks<Cursor>
	}
	
	
	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater){
		super.onCreateOptionsMenu(menu, inflater);
		inflater.inflate(R.menu.provider_list_options, menu);
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item){
		switch(item.getItemId()){
			case R.id.menu_item_new_provider:
				Intent i = new Intent(getActivity(),ProviderActivity.class);
				if(sIsProviderInDatabase){
					i.putExtra(ProviderActivity.EXTRA_PROVIDER_ID, sCurrentProviderId);
					startActivity(i);
				} else{
					startActivityForResult(i,REQUEST_NEW_PROVIDER);
				}
				return true;
			case R.id.menu_item_delete_database:
				File fileDB = getActivity().getDatabasePath("providers.sqlite");
				fileDB.delete();
				getLoaderManager().destroyLoader(0);
				sIsProviderInDatabase = false;
				ProviderManager.get(getActivity()).recreateDatabase(getActivity());
			default:
				return super.onOptionsItemSelected(item);
		}
	}
	
	@Override
	public void onListItemClick(ListView l, View v, int position, long id){
		//the id argument will be the PROVIDER ID; CursorAdapter gives us this for free
		Intent i = new Intent(getActivity(), ProviderActivity.class);
		i.putExtra(ProviderActivity.EXTRA_PROVIDER_ID, id);
		startActivity(i);
	}
	
	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data){
		if(requestCode == REQUEST_NEW_PROVIDER){
			//Restart the loader to get any new provider available
			getLoaderManager().restartLoader(0, null, this);
		}
	}
	
	// use the loader instead
	public void onResume(){
		super.onResume();
		//if(getLoaderManager() != null) getLoaderManager().restartLoader(0, null, this);
	}
	
	private static class ProviderCursorAdapter extends CursorAdapter{
		private ProviderCursor mProviderCursor;
		
		public ProviderCursorAdapter(Context context, ProviderCursor cursor){
			super(context, cursor, 0);
			mProviderCursor = cursor;
		}

		@Override
		public void bindView(View view, Context context, Cursor cursor) {
			//Get the provider for the current row
			Provider provider = mProviderCursor.getProvider();
			String modifiedName = "";
			if(mTM.getNetworkOperatorName() != null){
				modifiedName = mTM.getNetworkOperatorName().replaceAll(" ", "_");
				if(modifiedName.equals(provider.getName())){
					sIsProviderInDatabase = true;
					sCurrentProviderId = provider.getId();
				}
			}
			/*
			//Set up the start date text view
			TextView startDateTextView = (TextView)view;
			String cellText =
					context.getString(R.string.cell_text, run.getStartDate());
			startDateTextView.setText(cellText);
			*/
			boolean trackingThisProvider = ProviderManager.get(context)
					.isTrackingProvider(provider);
			TextView title = (TextView)view.findViewById(R.id.list_item_provider_title);
			title.setText(provider.getName());
					//+" ID: "+String.valueOf(provider.getId()));
			TextView date = (TextView)view.findViewById(R.id.list_item_run_date);
			String dateFormat = "EEE, MMM 'at' dd k:m:s";
			date.setText("Run starts from:"+
					DateFormat.format(dateFormat, provider.getStartDate()));
			CheckBox check = (CheckBox)view.findViewById(R.id.list_item_current_tracking);
			check.setChecked(trackingThisProvider);
			CheckBox isProvider = (CheckBox)view.findViewById(R.id.list_item_current_provider);
			boolean isEqualtoCurrentOperator = false;
			if(mTM.getNetworkOperatorName() != null){
				isEqualtoCurrentOperator = modifiedName.equals(provider.getName());
			}
			isProvider.setChecked(isEqualtoCurrentOperator);
			
		}

		@Override
		public View newView(Context context, Cursor cursor, ViewGroup parent) {
			//using a layout inflater to get a row view
			LayoutInflater inflater = (LayoutInflater)context
					.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			//return inflater.inflate(android.R.layout.simple_list_item_1, parent, false);
			return inflater.inflate(R.layout.list_item_provider, parent, false);
		}
	}
	
	
	@Override
	public Loader<Cursor> onCreateLoader(int id, Bundle args) {
		//you only ever load the runs, so assume this is the case
		Loader<Cursor> loader = new ProviderListCursorLoader(getActivity());
		return loader;
	}

	//called on the main thread once the data has been loaded in the background
	@Override
	public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
		// create an adapter to point at this cursor
		// when load finished, update views
		ProviderCursorAdapter adapter = 
				new ProviderCursorAdapter(getActivity(),(ProviderCursor)cursor);
		setListAdapter(adapter);
	}

	//called when the data is no longer available; for safety, stop using the cursor
	@Override
	public void onLoaderReset(Loader<Cursor> loader) {
		// Stop using the cursor (via the adapter)
		setListAdapter(null);
	}
	
}
