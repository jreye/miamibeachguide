package edu.fiu.cs.seniorproject;

import java.lang.ref.WeakReference;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

import android.app.Activity;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.AsyncTask.Status;
import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.text.format.DateFormat;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import edu.fiu.cs.seniorproject.data.Event;
import edu.fiu.cs.seniorproject.data.Location;
import edu.fiu.cs.seniorproject.data.SourceType;
import edu.fiu.cs.seniorproject.manager.AppLocationManager;
import edu.fiu.cs.seniorproject.manager.DataManager;
import edu.fiu.cs.seniorproject.manager.DataManager.ConcurrentEventListLoader;
import edu.fiu.cs.seniorproject.utils.Logger;

import android.os.AsyncTask;
import android.os.AsyncTask.Status;
import android.os.Bundle;
import android.app.Activity;
import android.content.Intent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.NumberPicker;
import android.widget.ProgressBar;
import android.widget.SearchView;
import android.widget.SearchView.OnQueryTextListener;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.AdapterView.OnItemClickListener;
import android.support.v4.app.NavUtils;
import android.text.format.DateFormat;

public class EventsActivity extends Activity {

	private EventLoader mEventLoader = null;
	private List<Hashtable<String, String>> mEventList = null;
	
	private boolean filterExpanded = false;
	private Animation animation = null;
	
	private final OnItemClickListener mClickListener = new OnItemClickListener() {
		@Override
		public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
			if ( !filterExpanded && mEventList != null && mEventList.size() > position ) {
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
        setContentView(R.layout.activity_events);
        getActionBar().setDisplayHomeAsUpEnabled(true);
        AppLocationManager.init(this);
        
        NumberPicker picker = (NumberPicker)findViewById(R.id.radius_picker);
        if ( picker != null ) {
        	picker.setMinValue(1);
        	picker.setMaxValue(10);
        	picker.setWrapSelectorWheel(false);
        }
        
        mEventLoader = new EventLoader(this);
        mEventLoader.execute();
    }

    @Override
    protected void onDestroy() {
    	if ( mEventLoader != null && mEventLoader.getStatus() != Status.FINISHED ) {
    		mEventLoader.cancelLoader();
    		mEventLoader.cancel(true);
    	}
    	super.onDestroy();
    }
    
    //settings click
    public void onSettingsClick(MenuItem view) {
    	Intent intent = new Intent(this, SettingsActivity.class);
    	this.startActivity(intent);
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
    	this.showEventsInMapView();
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
    	Logger.Debug("click on menu item = " + item.getItemId());
        switch (item.getItemId()) {
            case android.R.id.home:
                NavUtils.navigateUpFromSameTask(this);
                return true;
            case R.id.expand_collapse:
            	this.switchFilterView(item);
            	break;
        }
        return super.onOptionsItemSelected(item);
    }
    
    

    private void switchFilterView(MenuItem item) {
    	findViewById(R.id.filter_form).setVisibility(View.VISIBLE);
    	filterExpanded = !filterExpanded;
    	if ( filterExpanded ) {
    		item.setIcon(R.drawable.navigation_collapse_dark);    	
    		
    		if ( animation != null ) {
    			animation.cancel();
    		}
    		
    		this.animation = AnimationUtils.loadAnimation(this, R.anim.slide_from_top);
    		findViewById(R.id.filter_form).startAnimation(animation);
    	} else {
    		item.setIcon(R.drawable.navigation_expand_dark);
    		
    		if ( animation != null ) {
    			animation.cancel();
    		}
    		
    		this.animation = AnimationUtils.loadAnimation(this, R.anim.slide_to_top);
    		findViewById(R.id.filter_form).startAnimation(animation);
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
			entry.put("event_id", event.getId());
			entry.put("source", event.getSource().toString() );
			entry.put("name", event.getName() );						
			entry.put("time", DateFormat.format("EEEE, MMMM dd, h:mmaa", Long.valueOf( event.getTime() ) * 1000 ).toString() );
			
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
    	if (this.mEventList == null ) {
    		
    		if ( eventList != null && eventList.size() > 0 ) {
	    		ListView lv = (ListView)findViewById(android.R.id.list);
	    		if ( lv != null ) {
	    			
	    			// create the grid item mapping
	    			String[] from = new String[] {"name", "place", "time", "distance" };
					int[] to = new int[] { R.id.event_name, R.id.event_place, R.id.event_time, R.id.event_distance };

					this.mEventList = this.buildEventMap(eventList);
					
					SimpleAdapter adapter = new SimpleAdapter(this, this.mEventList, R.layout.event_row, from, to);
					lv.setAdapter(adapter);
	    			lv.setVisibility(View.VISIBLE);
	    			lv.setOnItemClickListener(mClickListener);
	    		}
	    	} else {
	    		TextView tv = (TextView)findViewById(android.R.id.empty);
	    		if ( tv != null ) {
	    			tv.setVisibility(View.VISIBLE);
	    		}
	    	}
    		
    		// Hide progress bar
	    	ProgressBar pb = (ProgressBar)findViewById(android.R.id.progress);
	    	if ( pb!= null ) {
	    		pb.setVisibility(View.GONE);
	    	}
    	} else {
    		ListView lv = (ListView)findViewById(android.R.id.list);
    		if ( lv != null && lv.getAdapter() != null ) {
    			List<Hashtable<String, String>> eventMap = this.buildEventMap(eventList);
        		if ( eventMap != null ) {
        			this.mEventList.addAll(eventMap);
        			((SimpleAdapter)lv.getAdapter()).notifyDataSetChanged();
        		}
    		}    		
    	}
    }
    
    
    // Method to show all events in a MapView 
    // It populates the static field locationsList
    public void showEventsInMapView(){
    	EventsMapViewActivity.locationsList = mEventList;
    	Intent intent = new Intent(this, EventsMapViewActivity.class);
		EventsActivity.this.startActivity(intent);
    }
    
    private class EventLoader extends AsyncTask<Void, List<Event>, Integer> {

    	private final WeakReference<EventsActivity> mActivityReference;
    	private ConcurrentEventListLoader mLoader = null;
    	
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
			this.mLoader = DataManager.getSingleton().getConcurrentEventList(location, null, "2", null);
			
			if ( this.mLoader != null ) {
				List<Event> iter = null;
				while ( (iter = this.mLoader.getNext()) != null ) {
					total += iter.size();
					Logger.Debug("Add new set of data size = " + iter.size());
					this.publishProgress(iter);
				}
			}
			return total;
		}
    	
		@Override
		protected void onProgressUpdate(List<Event>... eventList) {
			if ( mActivityReference != null ) {
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
			Logger.Debug("Total events = " + total );
		}
		
		
    }
    
    
    
}
