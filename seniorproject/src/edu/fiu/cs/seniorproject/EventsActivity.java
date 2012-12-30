package edu.fiu.cs.seniorproject;

import java.lang.ref.WeakReference;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

import android.app.Dialog;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.AsyncTask.Status;
import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.text.format.DateFormat;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Button;
import android.widget.ListView;
import android.widget.SearchView;

import android.widget.SearchView.OnQueryTextListener;
import android.widget.SimpleAdapter;
import android.widget.Spinner;
import edu.fiu.cs.seniorproject.data.DateFilter;
import edu.fiu.cs.seniorproject.data.Event;
import edu.fiu.cs.seniorproject.data.EventCategoryFilter;
import edu.fiu.cs.seniorproject.data.Location;
import edu.fiu.cs.seniorproject.data.SourceType;
import edu.fiu.cs.seniorproject.manager.AppLocationManager;
import edu.fiu.cs.seniorproject.manager.DataManager;
import edu.fiu.cs.seniorproject.manager.DataManager.ConcurrentEventListLoader;
import edu.fiu.cs.seniorproject.utils.BitmapSimpleAdapter;
import edu.fiu.cs.seniorproject.utils.Logger;

public class EventsActivity extends FilterActivity {

	private EventLoader mEventLoader = null;
	private List<Hashtable<String, String>> mEventList = null;
	private boolean isInForeground = false;	
	private List<Event> mPendingEventList = null;	
	private Button loadMoreButton = null;
	private SimpleAdapter listAdapter = null;
	
	private final OnItemClickListener mClickListener = new OnItemClickListener() {
		@Override
		public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
			if ( mEventList != null && mEventList.size() > position ) {
				EventsActivity.this.hideFilters();
				Hashtable<String, String> map = mEventList.get(position);
				
				if ( map != null ) {
					Intent intent = new Intent(EventsActivity.this, EventDetailsActivity.class);
					intent.putExtra("event_id", map.get("event_id"));
					intent.putExtra("source", SourceType.valueOf(map.get("source")));
					EventsActivity.this.startActivity(intent);
				}
			}
		}
	};
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        overridePendingTransition(R.anim.pull_in_from_bottom, R.anim.hold);
        setContentView(R.layout.activity_events);
        getActionBar().setDisplayHomeAsUpEnabled(true);
        
        ListView lv = (ListView)findViewById(android.R.id.list);
    	
    	if ( lv != null ) {
    		Button button = new Button(this);
    		button.setText("Load More");
    		this.loadMoreButton = button; 
    		button.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					getMoreEvents();
				}
			});    		
    		lv.addFooterView(button);
    	}
        
        
        AppLocationManager.init(this);
        
        this.setupFilters();
        
        //mEventLoader = new EventLoader(this);
        //this.loadEvents();
        this.startNewSearch(null);
    }

    @Override
    protected void onDestroy() {
    	this.cancelEventLoader();
    	super.onDestroy();
    }
    
    @Override
    protected void onResume() {
    	super.onResume();
    	this.isInForeground = true;
    	
    	if ( this.mPendingEventList != null ) {
    		this.showEventList(mPendingEventList);
    		this.onDoneLoadingEvents(1);
    		mPendingEventList = null;
    	}
    }
    
    private void getMoreEvents() {
    	this.cancelEventLoader();
    	mEventLoader = new EventLoader(this);
    	mEventLoader.useNextPage = true;
    	mEventLoader.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }
    
    @Override
    protected void onPause() {
    	mPendingEventList = null;
    	this.isInForeground = false;
    	overridePendingTransition(R.anim.hold, R.anim.pull_out_to_bottom);
    	super.onPause();
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_events, menu);
        SearchView searchView = (SearchView) menu.findItem(R.id.menu_search).getActionView();
        
        if ( searchView != null ) {
        	searchView.setOnQueryTextListener(new OnQueryTextListener() {
				
				@Override
				public boolean onQueryTextSubmit(String query) {
					Logger.Debug("process query = " + query);
					EventsActivity.this.startNewSearch(query);
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
    
    public void onEventsMapClick( MenuItem menuItem)
    {
    	if( mEventList != null)
    		this.showEventsInMapView();
    	else
    	{
    		Dialog d= new Dialog(this);
        	d.setTitle(" No events to show in the map!");       
        	d.show();
    	}
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
    	Logger.Debug("click on menu item = " + item.getItemId());
        switch (item.getItemId()) {
            case android.R.id.home:
                NavUtils.navigateUpFromSameTask(this);
                return true;  
            case R.id.map_menuitem:
            	this.onEventsMapClick(item);
            	return true;
            case R.id.menu_settings:
                this.onSettingsClick(item);
                return true;    
        }
        return super.onOptionsItemSelected(item);
    }  
    
    private void cancelEventLoader() {
    	if ( mEventLoader != null ) {
	    	if (  mEventLoader.getStatus() != Status.FINISHED ) {
	    		mEventLoader.cancelLoader();
	    		mEventLoader.cancel(true);
	    	}
	    	mEventLoader = null;
    	}
    }
    
    private void loadEvents() {
    	if ( mEventLoader != null ) {
    		
    		ListView lv = (ListView)findViewById(android.R.id.list);
    		if ( lv != null ) {
    			lv.setVisibility(View.GONE);
    			lv.setAdapter(null);
    		}
    		
    		findViewById(android.R.id.empty).setVisibility(View.GONE);
    		//findViewById(R.id.filter_form).setVisibility(View.GONE);
    		findViewById(android.R.id.progress).setVisibility(View.VISIBLE);
    		
    		mEventList = null;
    		mEventLoader.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    	}
    }
    
    private void onDoneLoadingEvents(int total) {
    	findViewById(android.R.id.list).setVisibility( mEventList != null ? View.VISIBLE : View.GONE);
		findViewById(android.R.id.empty).setVisibility(mEventList == null ? View.VISIBLE : View.GONE);
		findViewById(android.R.id.progress).setVisibility(View.GONE);
		
		if(total == 0)
		{
			this.loadMoreButton.setText("no more events!!!");
			this.loadMoreButton.setEnabled(false);
		}
    }
    
    private List<Hashtable<String, String>> buildEventMap(List<Event> eventList ) {
    	List<Hashtable<String, String>> fillMaps = new ArrayList<Hashtable<String, String>>(eventList.size());
		float[] distanceResults = new float[1];
		android.location.Location currentLocation = AppLocationManager.getCurrentLocation();
		DecimalFormat df = new DecimalFormat("#.#");
		
		for(int i = 0; i<eventList.size(); i++)
		{
			Event event = eventList.get(i);
			Hashtable<String, String> entry = new Hashtable<String, String>();
			entry.put("id", event.getId());
			entry.put("event_id", event.getId());
			entry.put("source", event.getSource().toString() );
			entry.put("name", event.getName() );			
			entry.put("time", DateFormat.format("EEEE, MMMM dd, h:mmaa", Long.valueOf( event.getTime() ) * 1000 ).toString() );
			
			String image = event.getImage();
			if ( image != null && !image.isEmpty()) {
				entry.put("image", event.getImage());
			}
			
			Location location = event.getLocation();
			
			if ( location != null ) {
				entry.put("place", location.getAddress() );
				entry.put("latitude", location.getLatitude());
				entry.put("longitude", location.getLongitude());
				
				if ( currentLocation != null ) {
					android.location.Location.distanceBetween(currentLocation.getLatitude(), currentLocation.getLongitude(), Double.valueOf(location.getLatitude()), Double.valueOf(location.getLongitude()), distanceResults);
					double miles = distanceResults[0] / 1609.34;	// i mile = 1.60934km								
					entry.put("distance", df.format(miles) + "mi" );
				} else {
					entry.put("distance", "0mi");
				}
			}
			fillMaps.add(entry);
		}
		return fillMaps;
    }
    
    private void showEventList( List<Event> eventList ) {
    	if ( this.isInForeground ) {	// only update ui when in foreground
	    	if (this.mEventList == null ) {
	    		
	    		if (  eventList != null && eventList.size() > 0) {
	    			
		    		ListView lv = (ListView)findViewById(android.R.id.list);
		    		if ( lv != null ) {
		    			
		    			// create the grid item mapping
		    			String[] from = new String[] {"name", "place", "time", "distance" };
						int[] to = new int[] { R.id.event_name, R.id.event_place, R.id.event_time, R.id.event_distance };
	
						this.mEventList = this.buildEventMap(eventList);						
						this.listAdapter = new BitmapSimpleAdapter(this, this.mEventList, R.layout.event_row, from, to);
						lv.setAdapter(this.listAdapter);
		    			lv.setVisibility(View.VISIBLE);
		    			lv.setOnItemClickListener(mClickListener);
		    		}
		    		
		    		// Hide progress bar
			    	findViewById(android.R.id.progress).setVisibility(View.GONE);
	    		}
	    	} else {
	    		ListView lv = (ListView)findViewById(android.R.id.list);
	    		if ( lv != null && lv.getAdapter() != null ) {
	    			List<Hashtable<String, String>> eventMap = this.buildEventMap(eventList);
	        		if ( eventMap != null ) {
	        			this.mEventList.addAll(eventMap);
	        			this.listAdapter.notifyDataSetChanged();
	        		}
	    		}    		
	    	}
    	} else {
    		if ( this.mPendingEventList == null ) {
    			mPendingEventList = eventList;
    		} else if ( eventList != null ) {
    			mPendingEventList.addAll(eventList);
    		}
    	}
    }
    
    private void getSearchFilters() {
    	if ( mEventLoader != null ) {
	    	Spinner spinner = (Spinner)findViewById(R.id.category_spinner);
	    	if ( spinner != null ) {
	    		mEventLoader.mCategoryFilter = EventCategoryFilter.getValueAtIndex( spinner.getSelectedItemPosition() );
	    	}
	    	
	    	spinner = (Spinner)findViewById(R.id.dates_spinner);
	    	if ( spinner != null ) {
	    		mEventLoader.mDataFilter = DateFilter.getValueAtIndex( spinner.getSelectedItemPosition() );
	    	}
	    	
	    	mEventLoader.mSearchRadius = this.getSearchRadiusStr();	    	
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
    		int selectedIndex = EventCategoryFilter.valueOf( SettingsActivity.getDefaultEventsCategory(this) ).ordinal();
    		if ( selectedIndex >= 0 ) {
    			spinner.setSelection(selectedIndex);
    		}
    	}
    }

    private void startNewSearch(String query) {
    	this.cancelEventLoader();
    	this.hideFilters();
    	mEventLoader = new EventLoader(this);
    	this.getSearchFilters();
    	this.mEventLoader.mQuery = query;
    	this.loadEvents();
    }
    
    // Method to show all events in a MapView 
    // It populates the static field locationsList
    public void showEventsInMapView(){
    	EventsMapViewActivity.locationsList = mEventList;
    	EventsMapViewActivity.actvtitle = "Events";
    	Intent intent = new Intent(this, EventsMapViewActivity.class);
		EventsActivity.this.startActivity(intent);
    }
    
    private class EventLoader extends AsyncTask<Void, List<Event>, Integer> {

    	private final WeakReference<EventsActivity> mActivityReference;
    	private ConcurrentEventListLoader mLoader = null;
    	
    	protected EventCategoryFilter mCategoryFilter = EventCategoryFilter.NONE;
    	protected DateFilter mDataFilter = DateFilter.TODAY;
    	protected String mSearchRadius = "1";	// one mile by default
    	protected String mQuery = null;
    	protected boolean useNextPage = false;
    	
    	public EventLoader(EventsActivity activity) {
    		mActivityReference = new WeakReference<EventsActivity>(activity);
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
			
			if(!this.useNextPage)
				this.mLoader = DataManager.getSingleton().getConcurrentEventList(location, mCategoryFilter, mSearchRadius, mQuery, mDataFilter );
			else
				this.mLoader = DataManager.getSingleton().getConcurrentNextEventList();
			
			if ( this.mLoader != null ) {
				List<Event> iter = null;
				while ( (iter = this.mLoader.getNext()) != null ) {
					total += iter.size();
					
					String source = iter.size() > 0 ? iter.get(0).getSource().toString() : "Unknow";
					Logger.Debug("Add new set of data from " + source + " size = " + iter.size());
					
					if ( iter.size() > 0 ) {
						this.publishProgress(iter);
					}
				}
			}
			return total;
		}
    	
    	
		@Override
		protected void onProgressUpdate(List<Event>... eventList) {
			if ( !this.isCancelled() && mActivityReference != null ) {
				EventsActivity activity = this.mActivityReference.get();
				if ( activity != null ) {
					for ( int i = 0; i < eventList.length; i++ ) {
						activity.showEventList(eventList[i]);
					}
				}
			}
		}
		
		@Override
		protected void onPostExecute(Integer total) {
			if ( !this.isCancelled() && mActivityReference != null ) {
				EventsActivity activity = this.mActivityReference.get();
				if ( activity != null ) {
					activity.onDoneLoadingEvents(total);
				}
			}
			Logger.Debug("Total events = " + total );
		}		
    }// end EventLoader
}
