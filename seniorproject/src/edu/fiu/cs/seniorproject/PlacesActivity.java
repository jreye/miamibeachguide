package edu.fiu.cs.seniorproject;

import android.os.Bundle;

import java.lang.ref.WeakReference;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

import edu.fiu.cs.seniorproject.data.Location;
import edu.fiu.cs.seniorproject.data.Place;
import edu.fiu.cs.seniorproject.data.PlaceCategoryFilter;
import edu.fiu.cs.seniorproject.data.SourceType;
import edu.fiu.cs.seniorproject.manager.AppLocationManager;
import edu.fiu.cs.seniorproject.manager.DataManager;
import edu.fiu.cs.seniorproject.manager.DataManager.ConcurrentPlaceListLoader;
import edu.fiu.cs.seniorproject.utils.BitmapSimpleAdapter;
import edu.fiu.cs.seniorproject.utils.Logger;

import android.os.AsyncTask;
import android.os.AsyncTask.Status;
import android.content.Intent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.SearchView;
import android.widget.SimpleAdapter;
import android.widget.Spinner;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.SearchView.OnQueryTextListener;

public class PlacesActivity extends FilterActivity {

	private PlacesLoader mPlacesLoader = null;
	private List<Hashtable<String, String>> mPlaceList = null;
	private SimpleAdapter listAdapter = null;
	private Button loadMoreButton = null;
	
	private final OnItemClickListener mClickListener = new OnItemClickListener() 
	{
		@Override
		public void onItemClick(AdapterView<?> parent, View view, int position, long id) 
		{
			if ( mPlaceList != null && mPlaceList.size() > position ) 
			{
				Hashtable<String, String> map = mPlaceList.get(position);
				if ( map != null && map.containsKey("id") && map.containsKey("source")) 
				{
					Intent intent = new Intent(PlacesActivity.this, PlaceDetailsActivity.class);
					
					intent.putExtra("id", map.get("id"));
					intent.putExtra("source", SourceType.valueOf(map.get("source")));
					
					PlacesActivity.this.startActivity(intent);
				}
			}
		}
	};
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_places);
        
        ListView lv = (ListView)findViewById(android.R.id.list);
    	
    	if ( lv != null ) {
    		Button button = new Button(this);
    		button.setText("Load More");
    		this.loadMoreButton = button; 
    		button.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					getMorePlaces();
				}
			});    		
    		lv.addFooterView(button);
    	}
    	
        mPlaceList = null;
        AppLocationManager.init(this);
        
        this.setupFilters();
        
        this.startNewSearch(null);
    }

    @Override
    protected void onDestroy() {
    	this.cancelLoader();
    	mPlaceList = null;	// release memory
    	super.onDestroy();
    }
    
    // This creates an Action bar with options in EventsActivity
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
    	getMenuInflater().inflate(R.menu.activity_places, menu);
    	
    	SearchView searchView = (SearchView) menu.findItem(R.id.menu_search).getActionView();
        
        if ( searchView != null ) {
        	searchView.setOnQueryTextListener(new OnQueryTextListener() {
				
				@Override
				public boolean onQueryTextSubmit(String query) {
					Logger.Debug("process query = " + query);
					PlacesActivity.this.startNewSearch(query);
					return true;
				}
				
				@Override
				public boolean onQueryTextChange(String newText) {
					Logger.Debug("query changed " + newText);
					return false;
				}
			});
        }
        
        return true;
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_settings:
                this.onSettingsClick(item);
                return true;
            case R.id.map_menuitem:
            	this.onPlacesMapClick(item);
            	return true;
        }
        return super.onOptionsItemSelected(item);
    }  
    
    public void onPlacesMapClick( MenuItem menuItem)
    {
    	this.showPlacesInMapView();
    }
    
    private void cancelLoader() {
    	if ( mPlacesLoader != null && mPlacesLoader.getStatus() != Status.FINISHED ) {
    		mPlacesLoader.cancelLoader();
    		mPlacesLoader.cancel(true);
    	}
    	mPlacesLoader = null;
    }
    
    private void startNewSearch( String query ) {
    	this.cancelLoader();    	
    	
    	ListView lv = (ListView)findViewById(android.R.id.list);
    	
    	if ( lv != null ) {
    		lv.setVisibility( View.GONE);
    		lv.setAdapter(null);
    	}
    	
		findViewById(android.R.id.empty).setVisibility(View.GONE);
		findViewById(android.R.id.progress).setVisibility(View.VISIBLE);
		
		mPlaceList = null;
    	mPlacesLoader = new PlacesLoader(this);
    	
    	this.getSearchFilters();
    	
    	mPlacesLoader.mQuery = query;
    	mPlacesLoader.useNextPage = false;
    	
        mPlacesLoader.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }
    
    private void getMorePlaces() {
    	this.cancelLoader();
    	mPlacesLoader = new PlacesLoader(this);
    	mPlacesLoader.useNextPage = true;
    	mPlacesLoader.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }
    
    public void showPlaceList( List<Place> places ) {
    	
    	if ( this.mPlaceList == null ) {
	    	if ( places != null && places.size() > 0 ) {
	    		ListView lv = (ListView)findViewById(android.R.id.list);
	    		if ( lv != null ) {
	    			
	    			// create the grid item mapping
					String[] from = new String[] {"name", "address", "distance" };
					int[] to = new int[] { R.id.place_name, R.id.place_address, R.id.distance };
	
					this.mPlaceList = this.buildPlaceList(places);
					
					this.listAdapter = new BitmapSimpleAdapter(this, this.mPlaceList, R.layout.place_row, from, to);
					lv.setAdapter(this.listAdapter);
	    			lv.setVisibility(View.VISIBLE);
	    			lv.setOnItemClickListener(mClickListener);
	    		}
	    		
	    		// Hide progress bar
		    	ProgressBar pb = (ProgressBar)findViewById(android.R.id.progress);
		    	if ( pb!= null ) {
		    		pb.setVisibility(View.GONE);
		    	}
	    	}
    	} else {
    		ListView lv = (ListView)findViewById(android.R.id.list);
    		if ( lv != null && lv.getAdapter() != null ) {
    			List<Hashtable<String, String>> placeList = this.buildPlaceList(places);
        		if ( placeList != null ) {
        			this.mPlaceList.addAll(placeList);
        			if ( this.listAdapter != null ) {
        				this.listAdapter.notifyDataSetChanged();
        			}
        		}
    		}    		
    	}
    }
    
    private List<Hashtable<String, String>> buildPlaceList( List<Place> places ) {
    	List<Hashtable<String, String>> placeList = new ArrayList<Hashtable<String,String>>(places.size());
		
		float[] distanceResults = new float[1];
		android.location.Location currentLocation = AppLocationManager.getCurrentLocation();
		DecimalFormat df = new DecimalFormat("#.#");
		
		for( int i = 0; i < places.size(); i++ ) {
			Hashtable<String, String> map = new Hashtable<String, String>();
			
			Place place = places.get(i);
			map.put("id", place.getId());
			map.put("name", place.getName());
			map.put("source", place.getSource().toString());
			
			if ( place.getImage() != null ) {
				map.put("image", place.getImage());
			}
			
			Location location = place.getLocation();
			if ( location != null && currentLocation != null ) {		

				// Adding the location to the Hashtable List map so it can be used to show all
				// places in PlacesMapView Activity
				map.put("latitude", location.getLatitude());
				map.put("longitude",location.getLongitude());
				
				map.put("address", location.getAddress() != null ? location.getAddress() : "No Address");
				
				android.location.Location.distanceBetween(currentLocation.getLatitude(), currentLocation.getLongitude(), Double.valueOf(location.getLatitude()), Double.valueOf(location.getLongitude()), distanceResults);
				double miles = distanceResults[0] / 1609.34;	// i mile = 1.60934km								
				map.put("distance", df.format(miles) + "mi" );
				
				placeList.add(map);
			}
		}
		return placeList;
    }
    
    private void onDoneLoadingPlaces(int total) {
    	findViewById(android.R.id.list).setVisibility( mPlaceList != null ? View.VISIBLE : View.GONE);
		findViewById(android.R.id.empty).setVisibility(mPlaceList == null ? View.VISIBLE : View.GONE);
		findViewById(android.R.id.progress).setVisibility(View.GONE);
		
		if(total == 0)
		{
			this.loadMoreButton.setText("no more places!!!");
			this.loadMoreButton.setEnabled(false);
		}
    }
    
    private void getSearchFilters() {
    	if ( mPlacesLoader != null ) {
	    	Spinner spinner = (Spinner)findViewById(R.id.category_spinner);
	    	if ( spinner != null ) {
	    		mPlacesLoader.mCategory = PlaceCategoryFilter.getValueAtIndex( spinner.getSelectedItemPosition() );
	    	}

	    	mPlacesLoader.mSearchRadius = this.getSearchRadiusStr();
	    	mPlacesLoader.mQuery = null;
    	}
    }
    
    @Override
    protected void onFilterClicked() {
    	this.startNewSearch(null);
	}   
    
    @Override
    protected void setupFilters() {
    	super.setupFilters();
    	
    	Spinner spinner = (Spinner)findViewById(R.id.category_spinner);
    	if ( spinner != null ) {
    		int selectedIndex = PlaceCategoryFilter.valueOf( SettingsActivity.getDefaultPlaceCategory(this) ).ordinal();
    		if ( selectedIndex >= 0 ) {
    			spinner.setSelection(selectedIndex);
    		}
    	}
    }

    private class PlacesLoader extends AsyncTask<Void, List<Place>, Integer>
    {
    	protected PlaceCategoryFilter mCategory = PlaceCategoryFilter.RESTAURANT_BARS;
    	protected String mSearchRadius = "1";
    	protected String mQuery = null;
    	protected boolean useNextPage = false;
    	
    	private final WeakReference<PlacesActivity> mActivityReference;
    	private ConcurrentPlaceListLoader mLoader = null;
    	
    	public PlacesLoader( PlacesActivity activity) {
    		mActivityReference = new WeakReference<PlacesActivity>(activity);
    	}
    	
    	public void cancelLoader() {
    		if ( mLoader != null ) {
    			mLoader.cancel();
    		}
    	}
		@SuppressWarnings("unchecked")
		@Override
		protected Integer doInBackground(Void... params) {
			android.location.Location currentLocation = AppLocationManager.getCurrentLocation();
			Location location = new Location( String.valueOf( currentLocation.getLatitude() ), String.valueOf(currentLocation.getLongitude()) );
			
			Integer total = 0;
			
			if ( useNextPage ) {
				mLoader = DataManager.getSingleton().getConcurrentNextPlaceList();
			} else {
				mLoader = DataManager.getSingleton().getConcurrentPlaceList(location, mCategory, mSearchRadius, mQuery);
			}
			
			if ( mLoader != null ) {
				List<Place> iter = null;
				while ( (iter = mLoader.getNext()) != null ) {
					int iterSize = iter.size();
					total += iterSize;
					
					String source = iter.size() > 0 ? iter.get(0).getSource().toString() : "Unknow";
					Logger.Debug("Add new set of data from " + source + " size = " + iterSize);
					
					if ( iterSize > 0 ) {
						this.publishProgress(iter);
					}
				}
			}
			return total;
			//return DataManager.getSingleton().getPlaceList(location, null, "500", null);
		}
		
		@Override
		protected void onProgressUpdate(List<Place>... placeList) {
			if ( !this.isCancelled() && placeList != null && mActivityReference != null && mActivityReference.get() != null ) {
				for( int i = 0; i < placeList.length; i++ ) {
					mActivityReference.get().showPlaceList(placeList[i]);
				}
			}
		}
    	
		@Override
		protected void onPostExecute(Integer total) {
			Logger.Debug("Total records = " + total );	
			if ( !this.isCancelled() && mActivityReference != null && mActivityReference.get() != null ) {
				mActivityReference.get().onDoneLoadingPlaces(total);
			}
		}
    }
    
    public void showPlacesInMapView()
    {
    	PlacesMapViewActivity.placesLocationsList = mPlaceList;
    	Intent intent = new Intent(this, PlacesMapViewActivity.class);
		PlacesActivity.this.startActivity(intent);
    }
}
