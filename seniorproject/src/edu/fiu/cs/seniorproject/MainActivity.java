package edu.fiu.cs.seniorproject;



import java.lang.ref.WeakReference;
import java.util.List;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;

import android.os.AsyncTask;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ImageView.ScaleType;
import edu.fiu.cs.seniorproject.data.DateFilter;
import edu.fiu.cs.seniorproject.data.Event;
import edu.fiu.cs.seniorproject.data.Location;
import edu.fiu.cs.seniorproject.data.MbGuideDB;
import edu.fiu.cs.seniorproject.manager.AppLocationManager;
import edu.fiu.cs.seniorproject.manager.DataManager;
import edu.fiu.cs.seniorproject.manager.DataManager.ConcurrentEventListLoader;
import edu.fiu.cs.seniorproject.manager.DataManager.IBitmapDownloaderListener;

import edu.fiu.cs.seniorproject.utils.Logger;

public class MainActivity extends Activity {

	MbGuideDB db;
//	private ArrayList<Integer> hotdList =  new ArrayList<Integer>();
//	private ArrayList<Integer> restdList =  new ArrayList<Integer>();	
//	private ArrayList<Bitmap> hotBmList =  new ArrayList<Bitmap>();
//	private ArrayList<Bitmap> restBmList =  new ArrayList<Bitmap>();
//	EventsActivity mAct;
//	List<Event> mEventListm;
	
	private EventLoader mEventLoader = null;
	//private PlacesLoader mPlacesLoader = null;
//	private List<Hashtable<String, String>> mEventList = null;
	//private List<Hashtable<String, String>> mPlaceList = null;
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main); 
//        addBitmapss();
        AppLocationManager.init(this);     
  
        mEventLoader = new EventLoader(this);
        if(mEventLoader != null)
        	mEventLoader.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        
//        mPlacesLoader = new PlacesLoader(this);
//        if(mPlacesLoader != null)
//        	mPlacesLoader.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);   

        // Disable personalization activity for now
//        db = new MbGuideDB(this);
//        try
//        {
//        	db.openDatabase();
//        	
//        	if(!db.isUserPrefSet())
//        	{
//        		  Intent intent = new Intent(this, PersonalizationActivity.class);
//        	      startActivity(intent);
//        	}
//        	db.closeDatabase();
//        }
//        catch(SQLException s)
//        {
//        	s.printStackTrace();
//        	
//        }      
        
    }

    @Override
    public void onDestroy() {
    	if ( this.mEventLoader != null && !this.mEventLoader.isCancelled() ) {
    		this.mEventLoader.cancelLoader();
    		this.mEventLoader.cancel(true);
    		this.mEventLoader = null;
    	}
    	
//    	if ( this.mPlacesLoader != null && !this.mPlacesLoader.isCancelled() ) {
//    		this.mPlacesLoader.cancel(true);
//    		this.mPlacesLoader = null;
//    	}
    	super.onDestroy();
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_main, menu);
        return true;
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_settings:
                this.onSettingsClick(item);
                return true;            
        }
        return super.onOptionsItemSelected(item);
    }
    
    public void onFacebookLoginButtonClick(View view) {
    	Intent intent = new Intent(this, FacebookLoginActivity.class);
    	this.startActivity(intent);
    }
    
    public void onShowGeoLocationClick(View view) {
    	Intent intent = new Intent(this, GeoLocationActivity.class);
    	this.startActivity(intent);
    }
    
    public void  onShowEventsClick(View view) {
    	Intent intent = new Intent(this, EventsActivity.class);
    	this.startActivity(intent);
    }
    
    public void  onPlacesButtonClick(View view) {
    	Intent intent = new Intent(this, PlacesActivity.class);
    	this.startActivity(intent);
    }
    
    public void onShowPlacesClick(View view) {
    	Intent intent = new Intent(this, PlacesActivity.class);
    	this.startActivity(intent);
    }
    
    public void onShowToursClick(View view) {
    	Intent intent = new Intent(this, TourActivity.class);
    	this.startActivity(intent);
    }
    
    public void onSettingsClick(MenuItem view) {
    	Intent intent = new Intent(this, SettingsActivity.class);
    	this.startActivity(intent);
    }   
    
    public void onShowMyPlacesClick(View view) {
    	Intent intent = new Intent(this, MyPlacesActivity.class);
    	this.startActivity(intent);
    }
    
    public void onShowMyEventsClick (View view) {

    	Intent intent = new Intent(this,MyEventsActivity.class); 
    	//Intent intent = new Intent(this,PersonalizationActivity.class); 
    	this.startActivity(intent);
    }
    
    public void onItemClicked (View view) {
    	//Intent intent = new Intent(this,MyEventsActivity.class); 
    	Intent intent = new Intent(this,PersonalizationActivity.class); 
    	this.startActivity(intent);	
    }
    
    /*
    private List<Hashtable<String, String>> buildPlaceList( List<Place> places ) {
    	List<Hashtable<String, String>> placeList = new ArrayList<Hashtable<String,String>>(places.size());
		
		float[] distanceResults = new float[1];
		android.location.Location currentLocation = AppLocationManager.getCurrentLocation();
		DecimalFormat df = new DecimalFormat("#.#");
		
		for( int i = 0; i < places.size(); i++ ) {
			
			Place place = places.get(i);
			
			Location location = place.getLocation();
			if ( location != null && currentLocation != null ) {		

				Hashtable<String, String> map = new Hashtable<String, String>();
				map.put("id", place.getId());
				map.put("name", place.getName());
				map.put("source", place.getSource().toString());
				
				if ( place.getImage() != null ) {
					map.put("image", place.getImage());
				}
				
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
    */
/*    private ArrayList<Place> convertPlaceListToArrayList(List <Place> list)
    {
    	ArrayList<Place> plList = new ArrayList<Place>();
    	
    	if(list != null)
    	{
    		int size =list.size();
    		
    		if(size>0)
    		{
    			for(int i=0; i<size; i++)
    			{
    				plList.add(list.get(i));
    			}
    		}
    	}
    	return plList;
    }*/
    
    /*
    private void showPlaceList(List<Place> placeList)
    {
    	//pList = this.convertPlaceListToArrayList(placeList);
    	 LayoutInflater inflater = (LayoutInflater)this.getLayoutInflater();
    	 LinearLayout ly = (LinearLayout)findViewById(R.id.child_linear_layout);
    	 int size  = getHotelBitmapList().size();
    	 int pSize = placeList != null ? placeList.size() : 0;
    	 int mSize = 0;
		if (placeList != null &&  pSize> 0) {
			this.mPlaceList = this.buildPlaceList(placeList);			
			if(pSize < size)
				mSize = pSize;
			else
				mSize = 8;//pSize;				
			for (int i = 0; i < mSize; i++) {	
				
				//TextView mtv = (TextView) v.findViewById(R.id.text);				
				if (placeList.get(i).getImage() != null) {	
					View v = (View) inflater.inflate(R.layout.image_box_linear,	null);
					ImageView iv = (ImageView) v.findViewById(R.id.image);
					
					DataManager.getSingleton().downloadBitmap(placeList.get(i).getImage(),iv);
			    	final Hashtable<String, String> map = mPlaceList.get(i);
			    	iv.setOnClickListener(new OnClickListener() {
			    		
		                public void onClick(View v) {       
		                		if ( map != null && map.containsKey("id") && map.containsKey("source"))		        				{
		        					Intent intent = new Intent(MainActivity.this, PlaceDetailsActivity.class);
		        					intent.putExtra("id", map.get("id"));
		        					intent.putExtra("source", SourceType.valueOf(map.get("source")));
		        					startActivity(intent);
		        				} 			                         
		                }
		            });    				  
			    	ly.addView(v, LayoutParams.WRAP_CONTENT, LayoutParams.MATCH_PARENT);
					
				}
				
				}
		}
    }
    */
    
    private void showEventList(List<Event> eventList)
    {    	
		if (eventList != null && eventList.size() > 0) {

			//this.mEventList = this.buildEventMap(eventList);
	
			for (int i = 0; i < eventList.size(); i++) {
				final Event event = eventList.get(i);
				
				if ( event != null && event.getImage() != null && !event.getImage().isEmpty() ) {
					
					DataManager.getSingleton().downloadBitmap(event.getImage(), new IBitmapDownloaderListener() {
						
						@Override
						public void OnFail() {
							Logger.Warning("Bitmap download failed. Event = " + event.toString());
						}
						
						@Override
						public void OnCompleted(Bitmap bm) {
							if ( bm != null ) {
								//LayoutInflater inflater = (LayoutInflater) MainActivity.this.getLayoutInflater();
								LinearLayout ly = (LinearLayout) findViewById(R.id.today_events_container );
								
								//View v = (View) inflater.inflate(R.layout.image_box_linear,	null);
								ImageView iv = new ImageView(MainActivity.this);// (ImageView) v.findViewById(R.id.image);
				
								if (iv != null) {						
									
									iv.setOnClickListener(new OnClickListener() {
										public void onClick(View v) {
											
											Intent intent = new Intent(MainActivity.this,EventDetailsActivity.class);
											intent.putExtra("event_id", event.getId() );
											intent.putExtra("source", event.getSource() );
											MainActivity.this.startActivity(intent);
										}
									});			
									iv.setImageBitmap(bm);
									iv.setPadding(10, 10, 10, 10);
									iv.setScaleType(ScaleType.FIT_XY);
									
									int targetHeight = ly.getMeasuredHeight();
									int targetWidth = (int)( (bm.getWidth() * targetHeight) / bm.getWidth() );
									
									ly.addView(iv, targetWidth, targetHeight);
									ly.invalidate();
								}								
							}
						}
					});					
					
				}
			}
		}
    }
    
    /*
    public void addBitmapss()
	{
		 getHotelImages();
		getRestaurantImages();
		//getRestaurantImages();
		for(int i=0; i<9; i++)
		{
			Bitmap bm = (Bitmap)BitmapFactory.decodeResource(this.getResources(), hotdList.get(i));
			hotBmList.add(bm);	   
			
			Bitmap bm2 = (Bitmap)BitmapFactory.decodeResource(this.getResources(), restdList.get(i));
			restBmList.add(bm2);	
		
		}
		
	}*/
    
    /*
	private void getHotelImages()
	{	
			hotdList.add(R.drawable.hot1);		
			hotdList.add(R.drawable.hot2);
			hotdList.add(R.drawable.hot3);
			hotdList.add(R.drawable.hot4);
			hotdList.add(R.drawable.hot5);
			hotdList.add(R.drawable.hot6);
			hotdList.add(R.drawable.hot7);
			hotdList.add(R.drawable.hot8);
			hotdList.add(R.drawable.hot9);
			hotdList.add(R.drawable.hot10);
			hotdList.add(R.drawable.hot11);
			hotdList.add(R.drawable.hot12);				
	}
	private void getRestaurantImages()
	{
		restdList.add(R.drawable.rest1);		
		restdList.add(R.drawable.rest2);
		restdList.add(R.drawable.rest3);
		restdList.add(R.drawable.rest4);
		restdList.add(R.drawable.rest5);
		restdList.add(R.drawable.rest6);
		restdList.add(R.drawable.rest7);
		restdList.add(R.drawable.rest8);
		restdList.add(R.drawable.rest9);
		//restdList.add(R.drawable.rest10);
		//restdList.add(R.drawable.rest11);
		//restdList.add(R.drawable.rest12);			
	}*/
	
//	public ArrayList<Bitmap> getHotelBitmapList()
//	{
//		return hotBmList;
//	}
//	
//	public ArrayList<Bitmap> getRestaurantBitmapList()
//	{
//		return restBmList;
//	}
    
    /*
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
*/
	 
    private class EventLoader extends AsyncTask<Void, List<Event>, Integer> {

    	private final WeakReference<MainActivity> mActivityReference;
    	private ConcurrentEventListLoader mLoader = null;
    	
    	public EventLoader(MainActivity activity) {
    		mActivityReference = new WeakReference<MainActivity>(activity);
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
			
			this.mLoader = DataManager.getSingleton().getConcurrentEventList(location, null, "25", null, DateFilter.TODAY );
			
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
				MainActivity activity = this.mActivityReference.get();
				if ( activity != null ) {
					for ( int i = 0; i < eventList.length; i++ ) {
						activity.showEventList(eventList[i]);
					}
				}
			}
		}
		
		@Override
		protected void onPostExecute(Integer total) {
//			if ( !this.isCancelled() && mActivityReference != null ) {
//				MainActivity activity = this.mActivityReference.get();
//				if ( activity != null ) {
//					activity.onDoneLoadingEvents(total);
//				}
//			}
			Logger.Debug("Total events = " + total );
		}		
    }
    
    /*
    private class PlacesLoader extends AsyncTask<Void, List<Place>, Integer>
    {
    	protected PlaceCategoryFilter mCategory = PlaceCategoryFilter.RESTAURANT_BARS;
    	protected String mSearchRadius = "10";
    	protected String mQuery = null;  	
    	private final WeakReference<MainActivity> mActivityReference;    	
    	public PlacesLoader( MainActivity activity) {
    		mActivityReference = new WeakReference<MainActivity>(activity);
    	}
		@SuppressWarnings("unchecked")
		@Override
		protected Integer doInBackground(Void... params) {
			android.location.Location currentLocation = AppLocationManager.getCurrentLocation();
			Location location = new Location( String.valueOf( currentLocation.getLatitude() ), String.valueOf(currentLocation.getLongitude()) );			
			Integer total = 0;			
			List<Place> pList;
				pList = DataManager.getSingleton().getPlaceList(location, mCategory, mSearchRadius, mQuery);
						this.publishProgress(pList);
			return total;		
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
		}
    }    
    public void showPlacesInMapView()
    {
    	PlacesMapViewActivity.placesLocationsList = mPlaceList;
    	Intent intent = new Intent(this, PlacesMapViewActivity.class);
		MainActivity.this.startActivity(intent);
    }
	*/
	
}

	
    
    

