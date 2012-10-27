package edu.fiu.cs.seniorproject.data.provider;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.text.format.DateFormat;

import edu.fiu.cs.seniorproject.client.EventfulRestClient;
import edu.fiu.cs.seniorproject.config.AppConfig;
import edu.fiu.cs.seniorproject.data.DateFilter;
import edu.fiu.cs.seniorproject.data.Event;
import edu.fiu.cs.seniorproject.data.EventCategoryFilter;
import edu.fiu.cs.seniorproject.data.Location;
import edu.fiu.cs.seniorproject.data.Place;
import edu.fiu.cs.seniorproject.data.PlaceCategoryFilter;
import edu.fiu.cs.seniorproject.data.SourceType;
import edu.fiu.cs.seniorproject.utils.DateUtils;
import edu.fiu.cs.seniorproject.utils.Logger;

public class EventFullProvider extends DataProvider
{
	private EventfulRestClient myRestClient;
	
	private final static String IMAGE_BASE_URL = "http://www.eventful.com";	
	
		
	 public EventFullProvider()
	 {
		 this.myRestClient = new EventfulRestClient(AppConfig.EVENTFUL_APP_ID);
	 }// EventFullProvider
	 
	 public List<Event> getEventList(Location location, EventCategoryFilter category, String radius, String query, DateFilter date ) 
	 {
		 List<Event> myEventList = null;
		 String myListRequestClient = this.myRestClient.getEventList(query, location, getDatesFromFilter(date), getEventCategory(category), (int)Math.ceil(Double.valueOf(radius))); 
		 if ( myListRequestClient != null && !myListRequestClient.isEmpty() )
		 {
				try {
					JSONObject eventsObject = new JSONObject(myListRequestClient);
					if ( eventsObject != null && eventsObject.has("events") && !eventsObject.isNull("events") )
					{
						JSONObject events = eventsObject.getJSONObject("events");
						
						if ( events != null && events.has("event") && !events.isNull("event")) {
							JSONArray jsonEventList = events.getJSONArray("event");
							
							if ( jsonEventList != null && jsonEventList.length() > 0 )
							{
								
								myEventList = new LinkedList<Event>();
								
								for( int i = 0; i < jsonEventList.length(); i++ )
								{
									JSONObject iter = jsonEventList.getJSONObject(i);
									
									Event event = this.parseEvent(iter);
									if ( event != null ) {
										myEventList.add(event);
									}
								}
							}
						}
					}
				}
				catch (JSONException e)
				{
					Logger.Error("Exception decoding json object in MBVCA " );
					e.printStackTrace();
				}
		 }
		 return myEventList;
	 }// getEventList 
	
	 
	 @Override
	public Event getEventDetails(String eventId, String reference) {
		String eventStr = this.myRestClient.getEventDetails(eventId);
		
		Event event = null;
		
		if ( eventStr != null && !eventStr.isEmpty() ) {
			try {
				JSONObject json = new JSONObject(eventStr);
				if ( json!= null ) {
					event = this.parseEvent(json);
				}
			} catch (JSONException e) {
				event = null;
				Logger.Warning("Exception decoding json from eventful " + e.getMessage() );
			}
			
		}
		return event;
	}
	 
	 public List<Place> getPlaceList(Location location, PlaceCategoryFilter category, String radius, String query)
	 {	
		
		List<Place> myPlaceList = null;
		
		String keywords = query != null ? query : this.getPlaceCategory(category);
		String myListRequestClient = this.myRestClient.getPlaceList(keywords, location, 0, 0, Integer.valueOf(radius) );
		
		if ( myListRequestClient != null && !myListRequestClient.isEmpty() )
		 {
				try {
					JSONObject placesObject = new JSONObject(myListRequestClient);
					if ( placesObject != null && placesObject.has("venues") && !placesObject.isNull("venues"))
					{
						JSONObject venues = placesObject.getJSONObject("venues");
						JSONArray jsonPlaceList = null;// ? venues.getJSONArray("venue") : null;
					
						if (venues != null && venues.has("venue") && !venues.isNull("venue") ) {
							try {
								jsonPlaceList = venues.getJSONArray("venue");
							} catch (JSONException e ) {
								// try to get an object
								JSONObject aux = venues.getJSONObject("venue");
								if ( aux != null ) {
									jsonPlaceList = new JSONArray();
									jsonPlaceList.put(aux);
								}
							}
						}
						
						if ( jsonPlaceList != null && jsonPlaceList.length() > 0 )
						{
							
							myPlaceList = new LinkedList<Place>();
							
							for( int i = 0; i < jsonPlaceList.length(); i++ )
							{
								JSONObject iter = jsonPlaceList.getJSONObject(i);
								
								Place place = this.parsePlace(iter);
								if ( place != null ) {
									myPlaceList.add(place);
								}
							}
						}
					}
				}
				catch (JSONException e)
				{
					Logger.Error("Exception decoding json object in MBVCA " );
					e.printStackTrace();
				}
		 }		
		 
		 return myPlaceList;
	 }// getPlaceList
	
	 @Override
	public Place getPlaceDetails(String placeId) {
		Place place = null;
		String placeStr = this.myRestClient.getPlaceDetails(placeId);
		
		if ( placeStr != null && !placeStr.isEmpty() ) {
			try {
				JSONObject json = new JSONObject(placeStr);
				if ( json != null ) {
					place = this.parsePlace(json);
				}
			} catch (JSONException e ) {
				place = null;
				Logger.Error("Exception decoding place from eventful " + e.getMessage());
			}
		}
		return place;
	}

	@Override
	public SourceType getSource() {
		return SourceType.EVENTFUL;
	}
	
	private Event parseEvent( JSONObject iter ) {
		Event event = null;
		
		try {
			if ( iter != null && iter.has("id") && iter.has("title"))
			{
				event = new Event();
				event.setId(iter.getString("id"));
				event.setName(iter.getString("title"));
				
				if ( iter.has("description"))
				{
					event.setDescription(iter.getString("description"));
				}
				
				// Process the location
				if ( iter.has("latitude") && iter.has("longitude"))
				{
					Location eventLocation = new Location(iter.getString("latitude"),iter.getString("longitude") );
					
					StringBuilder myAddress = new StringBuilder(110);	
					
					if ( iter.has("venue_address") )
					{
						myAddress.append(iter.getString("venue_address")+",");
					}
					else if ( iter.has("city_name"))
					{
						myAddress.append(iter.getString("city_name")+",");
					}
					else if( iter.has("region_abbr"))
					{
						myAddress.append(iter.getString("region_abbr")+",");
					}
					else if( iter.has("postal_code"))
					{
						myAddress.append(iter.getString("postal_code")+",");
					}
					
					eventLocation.setAddress(myAddress.toString());
					event.setLocation(eventLocation);										
				}//set the location
	
				if ( iter.has("start_time"))
				{
					SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
					Date date;
					try {
						date = sdf.parse(iter.getString("start_time"));
						event.setTime(String.valueOf(date.getTime()/1000L));
					} catch (ParseException e) {											
						e.printStackTrace();
					}										
				}
				
				if ( iter.has("image") && !iter.isNull("image"))
				{
					JSONObject imageObject = iter.getJSONObject("image");								
					if(imageObject != null && imageObject.has("small"))
					{
						JSONObject small = imageObject.getJSONObject("small");
						if (small != null && small.has("url") && !small.getString("url").isEmpty() && !small.getString("url").equals("null"))
						{
							event.setImage( IMAGE_BASE_URL + small.getString("url"));
						}
					}										
				}
				event.setSource(SourceType.EVENTFUL);
			}
		} catch ( JSONException e ) {
			Logger.Warning("exception decoding json from eventful " + e.getMessage() );
		}
		return event;
	}
	
	private Place parsePlace( JSONObject iter )
	{
		Place place = null;
		try {
			if ( iter != null && iter.has("id") && iter.has("name"))
			{
				place = new Place();
				place.setId(iter.getString("id"));
				place.setSource(SourceType.EVENTFUL);
				place.setName(iter.getString("name"));	
				
				// Process the location
				if ( iter.has("latitude") && iter.has("longitude"))
				{
					Location placeLocation = new Location(iter.getString("latitude"),iter.getString("longitude") );
					
					StringBuilder myAddress = new StringBuilder(110);	
					
					if ( iter.has("venue_address") )
					{
						myAddress.append(iter.getString("venue_address")+",");
					}
					else if ( iter.has("city_name"))
					{
						myAddress.append(iter.getString("city_name")+",");
					}
					else if( iter.has("region_abbr"))
					{
						myAddress.append(iter.getString("region_abbr")+",");
					}
					else if( iter.has("postal_code"))
					{
						myAddress.append(iter.getString("postal_code")+",");
					}
					
					placeLocation.setAddress(myAddress.toString());
					place.setLocation(placeLocation);										
				}//set the location

				
				if ( iter.has("description") && iter.isNull("description"))
				{
					place.setDescription(iter.getString("description"));
				}
				
				
				// set eventlist at place
				if ( iter.has("events") && !iter.isNull("events") )
				{
					JSONObject events = iter.getJSONObject("events");
					JSONArray jsonEventList = events.getJSONArray("event");
					
					List<Event> myEventList = null;
					
					if ( jsonEventList != null && jsonEventList.length() > 0 )
					{
						
						myEventList = new LinkedList<Event>();
						
						for( int i = 0; i < jsonEventList.length(); i++ )
						{
							JSONObject jsonEvent = jsonEventList.getJSONObject(i);
							
							Event event = this.parseEvent(jsonEvent);
							
							if ( event != null )
							{
								myEventList.add(event);
							}
						}// end for
					}
					
					place.setEventsAtPlace(myEventList);
				}// end if
				
			}
		} catch (JSONException e ) {
			place = null;
			Logger.Error("Exception decoding place from eventful " + e.getMessage() );
		}
		return place;
	}
	
	@Override
	protected String getEventCategory(EventCategoryFilter filter) {
		// FIXME Should match general categories into Eventfull categories
		return null;
	}
	
	@Override
	protected String getPlaceCategory( PlaceCategoryFilter filter ) {
		 String result = null;
		 
		 if ( filter != null ) {
			 result = filter.toString().toLowerCase().replace('_', ' ');
		 }
		 return result;
	}
	
	protected String getDatesFromFilter(DateFilter filter) {
		String result = "All";
		
		switch ( filter ) {
		case TODAY:
			result = "Today";
			break;
		case THIS_WEEK:
			result = "This Week";
			break;
			
		case THIS_WEEKEND:
			long thisWeekend = DateUtils.getThisWeekendInMiliseconds();
			String start = DateFormat.format("yyyyMMdd", thisWeekend ).toString();	
			String end = DateFormat.format("yyyyMMdd", thisWeekend + DateUtils.ONE_DAY * 1000 ).toString();	
			result = start + "00-" + end + "23";
			break;
			
		case NEXT_WEEKEND:
			long nextWeekend = DateUtils.getNextWeekendInMiliseconds();
			String nextStart = DateFormat.format("yyyyMMdd", nextWeekend ).toString();	
			String nextEnd = DateFormat.format("yyyyMMdd", nextWeekend + DateUtils.ONE_DAY * 1000 ).toString();	
			result = nextStart + "00-" + nextEnd + "23";
			break;
			
		case NEXT_30_DAYS:
			long today = DateUtils.getTodayTimeInMiliseconds();
			String nextMonthStart = DateFormat.format("yyyyMMdd", today ).toString();	
			String nextMonthEnd = DateFormat.format("yyyyMMdd", today + DateUtils.ONE_DAY * 1000 ).toString();	
			result = nextMonthStart + "00-" + nextMonthEnd + "23";
			break;
			
			default:
				result="All";
		}
		return result;
	}
}// EventFullProvider



//http://api.eventful.com/json/venues/get?app_key=HrsPRcW3W49b6hZq&id=V0-001-000104270-1

//http://api.eventful.com/rest/venues/get?app_key=HrsPRcW3W49b6hZq&id=V0-001-000104270-1
