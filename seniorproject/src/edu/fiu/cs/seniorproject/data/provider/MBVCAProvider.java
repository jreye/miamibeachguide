package edu.fiu.cs.seniorproject.data.provider;

import java.util.LinkedList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import edu.fiu.cs.seniorproject.client.MBVCAClient;
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

public class MBVCAProvider extends DataProvider 
{
	private final static String IMAGE_BASE_URL = "http://www.miamibeachapi.com";
	
	private final MBVCAClient mMBVCAClient;
	
	public MBVCAProvider()
	{
		this.mMBVCAClient  = new MBVCAClient(AppConfig.MBVCA_TOKEN);
	}

	@Override
	public List<Event> getEventList(Location location, EventCategoryFilter category, String radius, String query, DateFilter date) {
		List<Event> result = null;
		
		long[] times = new long[]{0,0};
		this.getTimeByDateFilter(date, times);
		String events = mMBVCAClient.getEventList(location, getEventCategory(category), radius, times[0], times[1], query );
		if ( events != null && !events.isEmpty() ) {
			try {
				JSONObject eventsObject = new JSONObject(events);
				
				if ( eventsObject != null && eventsObject.has("solodev_view") && !eventsObject.isNull("solodev_view")) {
					JSONArray eventList = eventsObject.getJSONArray("solodev_view");
					
					if ( eventList != null && eventList.length() > 0 ) {
						
						result = new LinkedList<Event>();
						for( int i = 0; i < eventList.length(); i++ ) {
							JSONObject iter = eventList.getJSONObject(i);
							Event event = this.parseEvent(iter);
							
							if ( event != null ) {
								result.add(event);
							}
						}
					}
				}
			} catch (JSONException e) {
				Logger.Debug("events = " + events);
				Logger.Error("Exception decoding json object in MBVCA " + e.getMessage());
			}
		}
		return result;
	}

	@Override
	public List<Place> getPlaceList(Location location, PlaceCategoryFilter category, String radius, String query) {
		List<Place> result = null;
		
		String places = this.mMBVCAClient.getPlaceList(location, getPlaceCategory(category), "0.5");
		
		if ( places != null && !places.isEmpty() ) {
			try {
				JSONObject placeObject = new JSONObject(places);
				if ( placeObject != null && placeObject.has("solodev_view") ) {
					JSONArray placeList = placeObject.getJSONArray("solodev_view");
					if ( placeList != null && placeList.length() > 0 ) {
						result = new LinkedList<Place>();
						for( int i = 0; i < placeList.length(); i++ ) {
							JSONObject iter = placeList.getJSONObject(i);
							
							Place place = this.parsePlace(iter);
							if ( place != null ) {
								result.add(place);
							}
						}
					}
				}
			} catch (JSONException e ) {
				Logger.Warning("Exception decoding place list " + e.getMessage());
			}
		}
		return result;
	}

	@Override
	public Event getEventDetails(String eventId, String reference) {
		Event event = null;
		String eventStr = this.mMBVCAClient.getEventDetails(eventId);
		
		if ( eventStr != null && !eventStr.isEmpty() ) {
			try {
				JSONObject eventsObject = new JSONObject(eventStr);
				
				if ( eventsObject != null && eventsObject.has("solodev_view")) {
					JSONArray eventList = eventsObject.getJSONArray("solodev_view");
					
					if ( eventList != null && eventList.length() > 0 ) {
						event = this.parseEvent(eventList.getJSONObject(0));
					}
				}
			} catch (JSONException e) {
				Logger.Warning("Exception decoding event " + e.getMessage() );
			}
		}
		return event;
	}

	@Override
	public Place getPlaceDetails(String placeId) {
		Place place = null;
		
		String places = this.mMBVCAClient.getPlaceDetails(placeId);
		
		if ( places != null && !places.isEmpty() ) {
			try {
				JSONObject placeObject = new JSONObject(places);
				if ( placeObject != null && placeObject.has("solodev_view") && !placeObject.isNull("solodev_view") ) {
					JSONArray placeList = placeObject.getJSONArray("solodev_view");
					if ( placeList != null && placeList.length() > 0 ) {
						place = this.parsePlace(placeList.getJSONObject(0));
					}
				}
			} catch (JSONException e ) {
				Logger.Warning("Exception decoding place " + e.getMessage() );
			}
		}
		return place;
	}

	@Override
	public SourceType getSource() {
		return SourceType.MBVCA;
	}
	
	private void getTimeByDateFilter(DateFilter filter, long[] times ) {
		times[0] = DateUtils.getTodayTimeInMiliseconds() / 1000L;
		
		switch (filter) {
			case TODAY:
				times[1] = times[0] + DateUtils.ONE_DAY; 
				break;
	
			case THIS_WEEK:
				times[1] = times[0] + DateUtils.SEVEN_DAYS;
				break;
				
			case THIS_WEEKEND:
				times[1] = times[0] + 2 * DateUtils.ONE_DAY; 
				break;
				
			case NEXT_30_DAYS:
				times[1] = times[0] + DateUtils.ONE_MONTH; 
				break;
			default:
				times[1] = times[0] + DateUtils.ONE_DAY; 
				break;
		}
	}
	
	private Event parseEvent(JSONObject iter) {
		Event event = null;
		try {
			if ( iter != null && iter.has("calendar_entry_id") && iter.has("name")) {
				event = new Event();
				event.setId(iter.getString("calendar_entry_id"));
				event.setName(iter.getString("name"));
				
				if ( iter.has("description")) {
					event.setDescription(iter.getString("description"));
				}
				
				if ( iter.has("lat") && iter.has("lng")) {
					Location eventLocation = new Location(String.valueOf( iter.getDouble("lat") ),String.valueOf(iter.getDouble("lng")) );
					
					if ( iter.has("venue") ) {
						eventLocation.setAddress( iter.getString("venue"));
					} else if ( iter.has("location")) {
						eventLocation.setAddress("location");
					}
					
					event.setLocation(eventLocation);
				}
	
				if ( iter.has("start_time")) {
					event.setTime(String.valueOf(iter.getInt("start_time")));
				}
				
				if ( iter.has("image")) {
					String image = iter.getString("image");
					
					if ( !image.isEmpty() && !image.equals("null")) {
						event.setImage( IMAGE_BASE_URL + iter.getString("image"));
					}
				}				
				
				event.setSource(SourceType.MBVCA);
			} 
		} catch (JSONException e ) {
			event = null;
			Logger.Warning("Exception decoding event in MBVCA " + e.getMessage());
		}
		return event;
	}
	
	private Place parsePlace(JSONObject iter) {
		Place place = null;
		if ( iter != null && iter.has("datatable_entry_id") && ( iter.has("last_name") || iter.has("name")) ) {
			place = new Place();
			try {
				place.setId( String.valueOf( iter.getInt("datatable_entry_id")));
				place.setSource(SourceType.MBVCA);
				
				if ( iter.has("name")) {
					place.setName(iter.getString("name"));
				} else {
					place.setName(iter.getString("last_name"));
				}
				
				if ( iter.has("lat") && iter.has("lng")) {
					Location location = new Location(String.valueOf( iter.getDouble("lat") ),String.valueOf(iter.getDouble("lng")) );
					
					if ( iter.has("prem_full_address")) {
						location.setAddress(iter.getString("prem_full_address"));
					} else if ( iter.has("mail_full_address")) {
						location.setAddress("mail_full_address");
					}
					place.setLocation(location);
				}
				
				if ( iter.has("image")) {
					String image = iter.getString("image");
					
					if ( !image.isEmpty() && !image.equals("null")) {
						place.setImage( IMAGE_BASE_URL + iter.getString("image"));
					}
				}
				
				if ( iter.has("url")) {
					place.setWebsite( IMAGE_BASE_URL + iter.getString("url"));
				}
				
			} catch (JSONException e) {
				place = null;
				Logger.Warning("Exception decoding place " + e.getMessage() );
			}
		}
		return place;
	}
	
	@Override
	protected String getEventCategory( EventCategoryFilter filter ) {
		 return filter == EventCategoryFilter.NONE ? null : String.valueOf(filter.Value());
	 }
}
