package edu.fiu.cs.seniorproject;

import java.lang.ref.WeakReference;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

import android.app.Dialog;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.CalendarContract;
import android.support.v4.app.NavUtils;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.webkit.WebView;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapActivity;
import com.google.android.maps.MapController;
import com.google.android.maps.MapView;
import com.google.android.maps.Overlay;
import com.google.android.maps.OverlayItem;

import edu.fiu.cs.seniorproject.data.CalendarEvent;
import edu.fiu.cs.seniorproject.data.Event;
import edu.fiu.cs.seniorproject.data.Location;
import edu.fiu.cs.seniorproject.data.MbGuideDB;
import edu.fiu.cs.seniorproject.data.SourceType;
import edu.fiu.cs.seniorproject.manager.AppLocationManager;
import edu.fiu.cs.seniorproject.manager.DataManager;
import edu.fiu.cs.seniorproject.utils.Logger;

public class EventDetailsActivity extends MapActivity {
	
	private EventDownloader mLoader = null;

	final static String TAG = "Miami Beach Guide";
	CalendarEvent calendar = null;
	ArrayList<CalendarEvent> calendarList = new  ArrayList<CalendarEvent>();
	Event eventToCalendar = null;
	ArrayList<String> createdEventsList = new ArrayList<String>();
	//Map<String, Long> createdEvents= new HashMap<String, Long>();
	MbGuideDB eventDB  = new MbGuideDB(this);
    
	private Event currentEvent = null;
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        overridePendingTransition(R.anim.pull_in_from_bottom, R.anim.hold);
        setContentView(R.layout.activity_event_details);
        getActionBar().setDisplayHomeAsUpEnabled(true);
        
        AppLocationManager.init(this);
        
        Intent intent = getIntent();
        if ( intent.hasExtra("event_id") && intent.hasExtra("source")) {
        	String eventId = intent.getStringExtra("event_id");
        	SourceType source = (SourceType)intent.getSerializableExtra("source"); 
        	this.mLoader = new EventDownloader(this);
        	this.mLoader.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, new EventSearchData(eventId, source));
        }
    }

    @Override
    public void onDestroy() {
    	if ( this.mLoader != null && !this.mLoader.isCancelled() ) {
    		this.mLoader.cancel(true);
    		this.mLoader = null;
    	}
    	super.onDestroy();
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_event_details, menu);
        return true;
    }

    @Override
    protected void onPause() {
    	overridePendingTransition(R.anim.hold, R.anim.pull_out_to_bottom);
    	super.onPause();
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                NavUtils.navigateUpFromSameTask(this);
                return true;
            case R.id.menu_settings:
                this.onSettingsClick(item);
                return true;  
            case R.id.add_to_calendar:
                this.onAddEventToCalendarClick(item);
                return true;  
            case R.id.delete_from_calendar:
                this.onDeleteEventFromCalendarAndDBClick(item);
                return true;  
            case R.id.invite_friends:
                this.onInviteFriends(item);
                return true;  
            case R.id.share:
                this.onShareEvent(item);
                return true;  
            case R.id.post_tweet:
                this.onPostTweet(item);
                return true; 
        }
        return super.onOptionsItemSelected(item);
    }

	@Override
	protected boolean isRouteDisplayed() {
		return false;
	}
	
	//settings click
    public void onSettingsClick(MenuItem view) {
    	Intent intent = new Intent(this, SettingsActivity.class);
    	this.startActivity(intent);
    }
    
    public void onAddEventToCalendarClick(MenuItem view) {
    	addEventToCalendarAndDB();
    }
    
    public void onDeleteEventFromCalendarAndDBClick(MenuItem view) { 	  	
    	
    	deleteEventToCalendarAndDB();
    }
    
    public void onInviteFriends(MenuItem menu) {
    	if ( this.currentEvent != null ) {
	    	Intent intent = new Intent(this, FbRequestActivity.class);
	    	intent.putExtra("title", "Invite Dialog");
	    	intent.putExtra("message", "Would you like to join me at " + this.currentEvent.getName() );
	    	this.startActivity(intent);
    	}
    }
    
    public void onShareEvent(MenuItem menu) {
    	if ( this.currentEvent != null ) {
	    	Intent intent = new Intent(this, FbPublishFeedActivity.class);
	    	intent.putExtra("title", "Miami Beach Events");
	    	intent.putExtra("message", "I was at " + this.currentEvent.getName() );
	    	this.startActivity(intent);
    	}
    }
    
    public void onPostTweet(MenuItem menu) {
    	if ( this.currentEvent != null ) {
	    	Intent intent = new Intent(this, UpdateTwitterStatusActivity.class);
	    	intent.putExtra("message", "I was at " + this.currentEvent.getName() );
	    	this.startActivity(intent);
    	}
    }
    
    public void onDirectionsClick(View view) {
    	Logger.Debug("On direction click");
    	if ( this.currentEvent != null && this.currentEvent.getLocation() != null ) {
    		android.location.Location currentLocation = AppLocationManager.getCurrentLocation();
    	
    		if ( currentLocation != null ) {
    			String uri = "http://maps.google.com/maps?saddr=" + currentLocation.getLatitude() +
    					"," + currentLocation.getLongitude() + 
    					"&daddr=" + this.currentEvent.getLocation().getLatitude() +
    					"," + this.currentEvent.getLocation().getLongitude();
    			Logger.Debug("Uri = " + uri);
    			
    			Intent intent = new Intent(android.content.Intent.ACTION_VIEW, Uri.parse(uri));
    			this.startActivity(intent);
    		}
    	}
    }
    
    public void onNavigationClick(View view) {
    	Logger.Debug("On navigation click");
    	if ( this.currentEvent != null && this.currentEvent.getLocation() != null ) {
    		android.location.Location currentLocation = AppLocationManager.getCurrentLocation();
    	
    		if ( currentLocation != null ) {
    			String uri = "google.navigation:q=" + this.currentEvent.getLocation().getLatitude() +
    					"," + this.currentEvent.getLocation().getLongitude();
    			Logger.Debug("Uri = " + uri);
    			
    			Intent intent = new Intent(android.content.Intent.ACTION_VIEW, Uri.parse(uri));
    			this.startActivity(intent);
    		}
    	}
    }
 
	public void showEvent(Event event) {
		
		if ( event != null ) {
			findViewById(R.id.progress).setVisibility(View.GONE);
			findViewById(R.id.scrollView).setVisibility(View.VISIBLE);
			eventToCalendar = event;
			this.currentEvent = event;
			TextView name = (TextView)findViewById(R.id.event_name);
			if ( name != null ) {
				name.setText(event.getName());
			}
			
			
			WebView description = (WebView)findViewById(R.id.event_description);
			if ( description != null ) {
				String descriptionStr = event.getDescription();
				
				if ( descriptionStr != null && !descriptionStr.isEmpty() && !descriptionStr.equals("null") && !descriptionStr.trim().isEmpty() ) {
					findViewById(R.id.description_frame).setVisibility(View.VISIBLE);
					description.loadData( "<html><body>" + descriptionStr + "</body></html>", "text/html", null);
				}
				else {
					findViewById(R.id.description_frame).setVisibility(View.GONE);
				}
			}
			
			TextView time = (TextView)findViewById(R.id.event_time);
			if ( time != null ) {
				time.setText(DateFormat.format("EEEE, MMMM dd, h:mmaa", Long.valueOf( event.getTime() ) * 1000 ).toString());
                
			}
			 
			Location location = event.getLocation();
			
			ImageView image = (ImageView)findViewById(R.id.image);
			if ( image != null ) {
				if ( event.getImage() != null && !event.getImage().isEmpty() ) {
					DataManager.getSingleton().downloadBitmap(event.getImage(), image);
				} else if ( location != null ) {
					String streetViewImage = String.format("http://maps.googleapis.com/maps/api/streetview?size=100x100&location=%1$s,%2$s&sensor=true", location.getLatitude(), location.getLongitude());
					DataManager.getSingleton().downloadBitmap(streetViewImage, image);
				}			
			}
			
			if ( location != null ) {
				TextView place = (TextView)findViewById(R.id.event_place);
				if ( place != null ) {
					place.setText(location.getAddress());
				}
				
				TextView distance = (TextView)findViewById(R.id.event_distance);
				if ( distance != null ) {
					float[] distanceResults = new float[1];
					android.location.Location currentLocation = AppLocationManager.getCurrentLocation();
					DecimalFormat df = new DecimalFormat("#.#");
					
					if ( currentLocation != null ) {
						android.location.Location.distanceBetween(currentLocation.getLatitude(), currentLocation.getLongitude(), Double.valueOf(location.getLatitude()), Double.valueOf(location.getLongitude()), distanceResults);
						double miles = distanceResults[0] / 1609.34;	// i mile = 1.60934km								
						distance.setText( df.format(miles) + "mi" );
					} else {
						distance.setText("--");
					}
				}
				
				MapView map = (MapView)findViewById(R.id.mapview);
			    
				if ( map != null ) {
					MapController mc = map.getController();
		    		if ( mc != null ) {
		    			
		    			List<Overlay> mapOverlaysList = map.getOverlays();
						Drawable drawable = this.getResources().getDrawable(R.drawable.red_pointer_icon);
						
						//creating an ItemizedOverlayActivity object so we can have multiple overlays
						//added to a list to show them in a map
					    ItemizedOverlayActivity itemizedoverlay = new ItemizedOverlayActivity(drawable, this);
					    
		    			GeoPoint geoPoint =  new GeoPoint( (int)(Double.valueOf( location.getLatitude() ) * 1E6),
		    			  (int)(Double.valueOf( location.getLongitude() ) * 1E6 ));
		    			
		    			// Creates an overlay item with a geopoint to show in the map
		    			 OverlayItem overlayitem = new OverlayItem(geoPoint, "Event", event.getName());
		    			
	    			    itemizedoverlay.addOverlay(overlayitem);
	    			    mapOverlaysList.add(itemizedoverlay);	    			    
		    			
		    			mc.setCenter(geoPoint);
		    			mc.setZoom(17);
		    		}
				}
			}
		}
	}

	public void addEventToCalendarAndDB()
	{
		String eName = this.getEventNameOnly();	
//		int commaIndex = eName.indexOf(',');		
//    	//int atIndex = eName.indexOf("at");    	
//    	int atLastIndex = eName.lastIndexOf("at");    	
//    	String eLocation = eName.substring(0, commaIndex);    	
//    	eLocation = eName.substring(atLastIndex+3);    	
//    	String eventNameOnly = eName.substring(0,commaIndex);
//    	eventNameOnly = eventNameOnly.trim(); 

		//int duration = Toast.LENGTH_LONG;

    	try{   	
        	eventDB.openDatabase();   
        	boolean exists = eventDB.existsVersion3(eName); 
        	 if(exists)
             {         		  
		        	Dialog d2 = new Dialog(this);
		        	d2.setTitle("This Event is already in your Event List!");
		        	TextView tv2 = new TextView(this);
		        	tv2.setText("TRUE!");
		        	d2.show();
             }
        	 
        	 else{
            
     			listCalendars();       
     	        if(calendarList!=null && calendarList.size() > 0 && calendarList.get(0)!= null)
     	        {	    
     	        	long startTimeInMilliseconds = Long.valueOf(eventToCalendar.getTime())* 1000;//startDate.getTimeInMillis();
     		        long endTimeInMilliseconds = Long.valueOf(eventToCalendar.getTime())* 1000;//endDate.getTimeInMillis();	    		        
     		        long createdCalendarEventID = createEventToCalendar(calendarList.get(0).getCalendarId(),eName,TAG, eName, startTimeInMilliseconds,  endTimeInMilliseconds );           
     		     //this VERSION IS NOT YET USED
     		        
     		       

     		       eventDB.createEventRecord(eName, createdCalendarEventID, eventToCalendar.getId() , eventToCalendar.getDescription(), eventToCalendar.getLocation().getLatitude(),
     		    		   eventToCalendar.getLocation().getLongitude() , startTimeInMilliseconds,  endTimeInMilliseconds, eventToCalendar.getSource().toString());	
     		        
			       //  eventDB.createEventRecordVersion2(eName, createdCalendarEventID, eventToCalendar.getLocation().toString(), startTimeInMilliseconds,  endTimeInMilliseconds);	    
     			        	//eventDB.createEventRecord(eventNameOnly, createdCalendarEventID, eLocation);		  
     			     Dialog d = new Dialog(this);
     			     d.setTitle("New Event Added to your Event List!");
     		     	 TextView tv = new TextView(this);
     			     tv.setText("FALSE!");
     			     d.show();   			        	
     			        	
     	        }
             }       	 
        	
  	        eventDB.closeDatabase();  	        
    	}catch(Exception ex)
    	{   
    		ex.printStackTrace();    		
    	}
	}
	
	public void deleteEventToCalendarAndDB()
	{
		//probably return the event name 
		
		// Two things
		// 1. Delete the event from the calendar
		// 2. Set its event_is_deleted_flag in DB to 1
		//maybe find a way that by only deleting the even from calendar, you set the flag automatically
		String eNameClean = this.getEventNameOnly();
		try{   	
        	eventDB.openDatabase();   
        	
        	
        	long eID = eventDB.getEventCalendarID(eNameClean);
        	
        	if(eID >=0 )
        	{
        		deleteEventFromCalendar(eID);
        		int result = eventDB.deleteEvent(eID);
        		
        	//boolean exists = eventDB.existsVersion3(eventNameOnly); 
        		if(result>0)
        		{
        			Dialog d2 = new Dialog(this);
		        	d2.setTitle("Event deleted "+eNameClean+ " from your Event List!");
		        	TextView tv2 = new TextView(this);
		        	tv2.setText("TRUE!");
		        	d2.show();
		        }
        		else
        		{
        			 Dialog d = new Dialog(this);
     			     d.setTitle("Event NOT deleted "+eNameClean+ " from your Event List!");
     		     	 TextView tv = new TextView(this);
     			     tv.setText("FALSE!");
     			     d.show();
        		}
        	}
        	else
        	{
             
			     //    eventDB.createEventRecordVersion2(eventNameOnly, createdCalendarEventID, eLocation, startTimeInMilliseconds,  endTimeInMilliseconds);	    
     			        	//eventDB.createEventRecord(eventNameOnly, createdCalendarEventID, eLocation);		  
     			     Dialog d = new Dialog(this);
     			     d.setTitle("Event NOT deleted "+eNameClean+ " from your Event List!");
     		     	 TextView tv = new TextView(this);
     			     tv.setText("FALSE!");
     			     d.show();  
        	}  			        	
     			        	
     	        
                 	 
        	
  	        eventDB.closeDatabase();  	        
    	}catch(Exception ex)
    	{   
    		ex.printStackTrace();    		
    	}
		
		
		
	}
	
	private void listCalendars(){
	        String[] returnColumns = new String[] {
	            CalendarContract.Calendars._ID,                     // 0
	            CalendarContract.Calendars.ACCOUNT_NAME,            // 1
	            CalendarContract.Calendars.CALENDAR_DISPLAY_NAME,   // 2
	            CalendarContract.Calendars.ACCOUNT_TYPE             // 3
	        };

	        Cursor cursor = null;
	        ContentResolver cr = getContentResolver();

	        // Call query to get all rows from the Calendars table
	        cursor = cr.query(CalendarContract.Calendars.CONTENT_URI, returnColumns, null, null, null);

	        while (cursor.moveToNext()) {
	            long calID = 0;
	            String displayName = null;
	            String accountName = null;
	            String accountType = null;

	            // Get the field values
	            calID = cursor.getLong(0);
	            displayName = cursor.getString(1);
	            accountName = cursor.getString(2);
	            accountType = cursor.getString(3);
	            
	            //Create a MyCalendar Object and add to the Calendar List
	            
	            calendarList.add(new CalendarEvent(calID, displayName, accountName, accountType ));
	            
//	            Log.i(TAG, String.format("ID=%d  Display=%s  Account=%s  Type=%s",
//	                    calID, displayName, accountName, accountType));
	        }

	        cursor.close();
	    }
	
	
	
	
	// Method to add an event to a Calendar
	 
	 private long createEventToCalendar(long calendarID, String title, String description, String location, long startMilliseconds, long endMilliseconds) //, Calendar startDate, Calendar endDate
	 {
	        long eventID = -1;        
	        ContentResolver cr = getContentResolver();

	        // Populate content values	    
	        ContentValues values = new ContentValues();
	        values.put(CalendarContract.Events.CALENDAR_ID, calendarID);
	        values.put(CalendarContract.Events.TITLE, title);
	        values.put(CalendarContract.Events.DESCRIPTION, description);
	        values.put(CalendarContract.Events.EVENT_LOCATION, location);
	        values.put(CalendarContract.Events.DTSTART, startMilliseconds);
	        values.put(CalendarContract.Events.DTEND, endMilliseconds);
	        values.put(CalendarContract.Events.EVENT_TIMEZONE, "US/Eastern");	  

	        // Call insert and get the returned ID
	        Uri uri = cr.insert(CalendarContract.Events.CONTENT_URI, values);
	        eventID = ContentUris.parseId(uri);	        
	        return eventID;
	        
	  }
	 
	 private void deleteEventFromCalendar(long eCalendarID)
	 {		 
		 if(isCalendarEventDeleted(eCalendarID))
		 {
			 //do not deleted because the event is already deleted
			 Log.i(TAG, String.format("Event was already deleted"));
		 }
		 else
		 {
			 ContentResolver cr = getContentResolver();
			 // Set the event URI
		     Uri uri = ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, eCalendarID);
			 int rows = cr.delete(uri, null, null);
			 Log.i(TAG, String.format("Event ID:%d  |  Rows Deleted:%d", eCalendarID, rows));			 
		 }        

	   }
	 
	  private boolean isCalendarEventDeleted(long eCalID) {
	        String[] EVENT_PROJECTION = new String[] {
	            CalendarContract.Events._ID,                     // 0
	            CalendarContract.Events.TITLE,                   // 1
	            CalendarContract.Events.DELETED                  // 2
	        };
	        // Assure that deleted flag is not set
	        String queryFilter = CalendarContract.Events.DELETED + " = ?";
	        String[] queryFilterValues = {"0"};

	        Cursor cur = null;
	        ContentResolver cr = getContentResolver();
	        Uri uri = ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, eCalID);
	        cur = cr.query(uri, EVENT_PROJECTION, queryFilter, queryFilterValues, null);

	        // Use the cursor to step through the returned records
	        if (cur.moveToNext()) {
	        	
	        	cur.close();
	        	return false;
//	            long returnedEventID = cur.getLong(0);
//	            String title = cur.getString(1);
//	            int isDeleted = cur.getInt(2);

	           // Log.i(TAG, String.format("[NOT DELETED] Event ID=%d  Title=%s  Is Deleted:%d", returnedEventID, title, isDeleted));
	        }
	        else 
	        {
	        	cur.close();
	        	return true;
	            //Log.i(TAG, String.format("[DELETED]Event ID=%d", eventID));
	        	
	        }
	        
	    }
	  
	  private String getEventNameOnly()
	  {
		  String eName ="null name"; 
		  if(eventToCalendar != null)
		  {
			  String name = eventToCalendar.getName();	
			  if(name != null){	  
				  eName =name; 
			  }
			  else
			  {
				  eName = "Empty name";
			  }
		  }
		//	int commaIndex = eName.indexOf(',');		
	    //	int atIndex = eName.indexOf("at");    	
	    //	int atLastIndex = eName.lastIndexOf("at");    	
	    	//String eLocation = eName.substring(0, commaIndex);    	
	    //	eLocation = eName.substring(atLastIndex+3);    	
	    //	String eventNameOnly = eName.substring(0,commaIndex);
	    //	eventNameOnly = eventNameOnly.trim();
	    	
	    	return eName;
	  }
	 
	 /*
	 uncomment when needed
	  private void findEvent(long eventID) {
	        String[] EVENT_PROJECTION = new String[] {
	            CalendarContract.Events._ID,                     // 0
	            CalendarContract.Events.TITLE,                   // 1
	            CalendarContract.Events.DELETED                  // 2
	        };

	        Cursor cur = null;
	        ContentResolver cr = getContentResolver();
	        Uri uri = ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, eventID);
	        
	        
	        cur = cr.query(uri, EVENT_PROJECTION, null, null, null);

	        // Use the cursor to step through the returned records
	        if (cur.moveToNext()) {
	            long returnedEventID = cur.getLong(0);
	            String title = cur.getString(1);
	            int isDeleted = cur.getInt(2);

	            Log.i(TAG, String.format("Event ID=%d  Title=%s  Is Deleted:%d", returnedEventID, title, isDeleted));
	        }

	        cur.close();
	    }
	 */

	private class EventSearchData
	{
		public EventSearchData(String id, SourceType sourceType) {
			this.eventId = id;
			this.source = sourceType;
		}
		public String eventId;
		public SourceType source;
	}
	
	private class EventDownloader extends AsyncTask<EventSearchData, Void, Event>
	{
		private WeakReference<EventDetailsActivity> mActivityReference;
		
		public EventDownloader( EventDetailsActivity activity ) {
			mActivityReference = new WeakReference<EventDetailsActivity>(activity);
		}
		
		@Override
		protected Event doInBackground(EventSearchData... params) {
			return DataManager.getSingleton().getEventDetails(params[0].eventId, params[0].source);
		}
		
		@Override
		protected void onPostExecute(Event result) {
			if ( mActivityReference != null && mActivityReference.get() != null ) {
				EventDetailsActivity activity = mActivityReference.get();
				if ( activity != null ) {
					activity.showEvent(result);
					activity.mLoader = null;
				}
			}
	    }
	}
}
