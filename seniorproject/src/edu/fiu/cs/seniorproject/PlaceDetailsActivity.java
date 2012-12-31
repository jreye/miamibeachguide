package edu.fiu.cs.seniorproject;

import java.lang.ref.WeakReference;
import java.text.DecimalFormat;
import java.util.List;

import android.app.Dialog;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.location.LocationManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup.LayoutParams;
import android.webkit.WebView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.facebook.PlacePickerFragment;
import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapActivity;
import com.google.android.maps.MapController;
import com.google.android.maps.MapView;
import com.google.android.maps.Overlay;
import com.google.android.maps.OverlayItem;

import edu.fiu.cs.seniorproject.data.Event;
import edu.fiu.cs.seniorproject.data.Location;
import edu.fiu.cs.seniorproject.data.MbGuideDB;
import edu.fiu.cs.seniorproject.data.Place;
import edu.fiu.cs.seniorproject.data.SourceType;
import edu.fiu.cs.seniorproject.manager.AppLocationManager;
import edu.fiu.cs.seniorproject.manager.DataManager;
import edu.fiu.cs.seniorproject.utils.Logger;

public class PlaceDetailsActivity extends MapActivity {
	private PlaceDownloader mLoader = null;
	private Place currentPlace = null;

	MbGuideDB placeDB = new MbGuideDB(this);
	private String placeIdToDB = null;
	private String placeSourceToDB = null;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_place_details);
		getActionBar().setDisplayHomeAsUpEnabled(true);

		AppLocationManager.init(this);

		Intent intent = getIntent();

		if (intent != null && intent.hasExtra("id")
				&& intent.hasExtra("source")) {
			String placeId = intent.getStringExtra("id");
			placeIdToDB = placeId;
			SourceType source = (SourceType) intent
					.getSerializableExtra("source");
			placeSourceToDB = source.toString();

			this.mLoader = new PlaceDownloader(this);
			this.mLoader.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, new PlaceSearchData(placeId,	source));
		}
	}

	@Override
	public void onDestroy() {
		if ( this.mLoader != null && !this.mLoader.isCancelled() ) {
			this.mLoader.cancel(true);			
		}
		this.mLoader = null;
		super.onDestroy();
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.activity_place_details, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
			NavUtils.navigateUpFromSameTask(this);
			return true;
		case R.id.menu_settings:
			this.onSettingsClick(item);
			
		case R.id.add_place_to_db:
			this.onAddToDBClick(item);
			
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	protected boolean isRouteDisplayed() {
		return false;
	}

	// settings click
	public void onSettingsClick(MenuItem view) {
		Intent intent = new Intent(this, SettingsActivity.class);
		this.startActivity(intent);
	}
	
	public void onAddToDBClick(MenuItem view) {	
		addPlaceToDB();
	}

	public void onDirectionsClick(View view) {
		Logger.Debug("On direction click");
		if (this.currentPlace != null
				&& this.currentPlace.getLocation() != null) {
			android.location.Location currentLocation = AppLocationManager
					.getCurrentLocation();

			if (currentLocation != null) {
				String uri = "http://maps.google.com/maps?saddr="
						+ currentLocation.getLatitude() + ","
						+ currentLocation.getLongitude() + "&daddr="
						+ this.currentPlace.getLocation().getLatitude() + ","
						+ this.currentPlace.getLocation().getLongitude();
				Logger.Debug("Uri = " + uri);

				Intent intent = new Intent(android.content.Intent.ACTION_VIEW,
						Uri.parse(uri));
				this.startActivity(intent);
			}
		}
	}

	public void onNavigationClick(View view) {
		Logger.Debug("On navigation click");
		if (this.currentPlace != null
				&& this.currentPlace.getLocation() != null) {
			android.location.Location currentLocation = AppLocationManager
					.getCurrentLocation();

			if (currentLocation != null) {
				String uri = "google.navigation:q="
						+ this.currentPlace.getLocation().getLatitude() + ","
						+ this.currentPlace.getLocation().getLongitude();
				Logger.Debug("Uri = " + uri);

				Intent intent = new Intent(android.content.Intent.ACTION_VIEW,
						Uri.parse(uri));
				this.startActivity(intent);
			}
		}
	}

	public void onCheckIn(View view) {
		Logger.Debug("On check in click");
		if (this.currentPlace != null && this.currentPlace.getLocation() != null) {
			Intent intent = new Intent(this, FbPlacePicker.class);
			
			Location currentLocation = this.currentPlace.getLocation();
	    	intent.putExtra("latitude", currentLocation.getLatitude());
	    	intent.putExtra("longitude", currentLocation.getLongitude());
	    	intent.putExtra(PlacePickerFragment.RADIUS_IN_METERS_BUNDLE_KEY, 50);
	    	intent.putExtra(PlacePickerFragment.SHOW_SEARCH_BOX_BUNDLE_KEY, false);
	    	intent.putExtra(PlacePickerFragment.SEARCH_TEXT_BUNDLE_KEY, this.currentPlace.getName() );
	    	
	    	android.location.Location location = new android.location.Location(LocationManager.GPS_PROVIDER);
	    	location.setLatitude(Double.valueOf(currentLocation.getLatitude()));
			location.setLongitude(Double.valueOf(currentLocation.getLongitude()));
			
			intent.putExtra(PlacePickerFragment.LOCATION_BUNDLE_KEY, location);
			
	    	this.startActivity(intent);
		}
	}
		
	public void showPlace(Place place) {

		if (place != null) {
			findViewById(android.R.id.progress).setVisibility(View.GONE);
			findViewById(R.id.scrollView).setVisibility(View.VISIBLE);
			
			this.currentPlace = place;
			// create place name.
			TextView name = (TextView) findViewById(R.id.place_name);
			if (name != null) {
				name.setText(place.getName());
			}

			WebView description = (WebView)findViewById(R.id.place_description);
			if ( description != null ) {
				String descriptionStr = place.getDescription();
				
				if ( descriptionStr != null && !descriptionStr.isEmpty() && !descriptionStr.equals("null") && !descriptionStr.trim().isEmpty() ) {
					findViewById(R.id.description_frame).setVisibility(View.VISIBLE);
					description.loadData( "<html><body>" + descriptionStr + "</body></html>", "text/html", null);
				}
				else {
					findViewById(R.id.description_frame).setVisibility(View.GONE);
				}
			}			

			// place location
			Location location = place.getLocation();
			
			ImageView image = (ImageView) findViewById(R.id.place_image);
			if (image != null) {
				if (place.getImage() != null && !place.getImage().isEmpty()) {
					DataManager.getSingleton().downloadBitmap(place.getImage(),image);
				} else {
					String streetViewImage = String.format("http://maps.googleapis.com/maps/api/streetview?size=100x100&location=%1$s,%2$s&sensor=true", location.getLatitude(), location.getLongitude());
					DataManager.getSingleton().downloadBitmap(streetViewImage, image);
				}				
			}
			
			if (location != null) {
				TextView placeLocation = (TextView) findViewById(R.id.place_location);
				if (placeLocation != null) {
					placeLocation.setText(location.getAddress());
				}

				// place distance.
				TextView distance = (TextView) findViewById(R.id.place_distance);
				if (distance != null) {
					float[] distanceResults = new float[1];
					android.location.Location currentLocation = AppLocationManager
							.getCurrentLocation();
					DecimalFormat df = new DecimalFormat("#.#");

					if (currentLocation != null) {
						android.location.Location.distanceBetween(
								currentLocation.getLatitude(),
								currentLocation.getLongitude(),
								Double.valueOf(location.getLatitude()),
								Double.valueOf(location.getLongitude()),
								distanceResults);
						double miles = distanceResults[0] / 1609.34; // i mile =
																		// 1.60934km
						distance.setText(df.format(miles) + "mi");
					} else {
						distance.setText("--");
					}
				}

				MapView map = (MapView) findViewById(R.id.mapview);
				if (map != null) {
					MapController mc = map.getController();
					if (mc != null) {

						List<Overlay> mapOverlaysList = map.getOverlays();
						Drawable drawable = this.getResources().getDrawable(
								R.drawable.red_pointer_icon);

						// creating an ItemizedOverlayActivity object so we can
						// have multiple overlays
						// added to a list to show them in a map
						ItemizedOverlayActivity itemizedoverlay = new ItemizedOverlayActivity(
								drawable, this);

						GeoPoint geoPoint = new GeoPoint(
								(int) (Double.valueOf(location.getLatitude()) * 1E6),
								(int) (Double.valueOf(location.getLongitude()) * 1E6));

						// Creates an overlay item with a geopoint to show in
						// the map
						OverlayItem overlayitem = new OverlayItem(geoPoint,
								"Place", place.getName());

						itemizedoverlay.addOverlay(overlayitem);
						mapOverlaysList.add(itemizedoverlay);

						mc.setCenter(geoPoint);
						mc.setZoom(17);
					}
				}

				showEventList(place.getEventsAtPlace());
			}
		}

	}// end showPlace

	private void showEventList(List<Event> eventList) {

		TextView tv = (TextView) findViewById(android.R.id.empty);
		if (eventList != null && eventList.size() > 0) {

			tv.setVisibility(View.VISIBLE);
			tv.setText(R.string.events);
			//tv.setVisibility(View.GONE);
			LinearLayout ll = (LinearLayout) findViewById(android.R.id.list);

			if (ll != null) {
				ll.setVisibility(View.VISIBLE);
				for (int i = 0; i < eventList.size(); i++) {
					Event event = eventList.get(i);
					tv = new TextView(this);
					
					String eventName = event.getName().trim();
					Logger.Debug("Add event with name = " + event.toString() );
					tv.setText(eventName);
					
					final String eventId = event.getId();
					final SourceType source = event.getSource();
					tv.setOnClickListener(new OnClickListener() {
						@Override
						public void onClick(View v) {
							// TODO Auto-generated method stub
							Intent intent = new Intent(PlaceDetailsActivity.this, EventDetailsActivity.class);
							intent.putExtra("event_id", eventId);
							intent.putExtra("source", source);
							PlaceDetailsActivity.this.startActivity(intent);
						}
					});
					ll.addView(tv, LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT );
				}
			}
		} else {
			findViewById(android.R.id.list).setVisibility(View.GONE);
		}
	}// end showEventList

	private class PlaceSearchData {

		public String id;
		public SourceType source;

		public PlaceSearchData(String id, SourceType sourceType) {

			this.id = id;
			this.source = sourceType;
		}
	}

	public void addPlaceToDB()
	{    	
		//int duration = Toast.LENGTH_LONG;
	    if(this.currentPlace!=null)
	    {
	    	try{   	
	        	placeDB.openDatabase();   
	        	boolean exists = placeDB.existsPlace(placeIdToDB); 
	        	 if(exists)
	             {         		  
			        	Dialog d2 = new Dialog(this);
			        	d2.setTitle("This Place is already in your Place List!");
			        	d2.show();
	             }
	        	 
	        	 else{
	            
	        		 // long timeOpensInMilliseconds = Long.valueOf(eventToCalendar.getTime())* 1000;//startDate.getTimeInMillis();
	        		 // long timeClosesInMilliseconds = Long.valueOf(eventToCalendar.getTime())* 1000;//endDate.getTimeInMillis();	    		        
				         placeDB.createPlaceRecord(placeIdToDB, currentPlace.getName(), currentPlace.getLocation().getAddress(),
				        		 	currentPlace.getLocation().getLatitude(), currentPlace.getLocation().getLongitude(), currentPlace.getDescription(), 
				        		 	currentPlace.getWebsite(), placeSourceToDB);	    
	     			        	//eventDB.createEventRecord(eventNameOnly, createdCalendarEventID, eLocation);		  
	     			     Dialog d = new Dialog(this);
	     			     d.setTitle("New Place Added to your Place List!");
	     			     d.show();   			        	
	     		
	             }       	 
	        	
	  	        placeDB.closeDatabase();  	        
	    	}catch(Exception e)
	    	{   
	    		e.printStackTrace();    		
	    	}
    	}
	}

	private class PlaceDownloader extends
			AsyncTask<PlaceSearchData, Void, Place> {
		private WeakReference<PlaceDetailsActivity> mActivityReference;

		public PlaceDownloader(PlaceDetailsActivity activity) {
			mActivityReference = new WeakReference<PlaceDetailsActivity>(activity);
		}

		@Override
		protected Place doInBackground(PlaceSearchData... params) {
			return DataManager.getSingleton().getPlaceDetails(params[0].id, params[0].source);
		}

		@Override
		protected void onPostExecute(Place result) {
			if (mActivityReference != null && mActivityReference.get() != null) {
				PlaceDetailsActivity activity = this.mActivityReference.get();
				if ( activity != null ) {
					activity.showPlace(result);
					activity.mLoader = null;
				}
			}
		}
	}
}// end PlaceDetailActivity
