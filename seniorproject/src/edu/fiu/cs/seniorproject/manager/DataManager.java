package edu.fiu.cs.seniorproject.manager;

import java.util.LinkedList;
import java.util.List;

import android.location.Location;

import edu.fiu.cs.seniorproject.data.Event;
import edu.fiu.cs.seniorproject.data.Place;
import edu.fiu.cs.seniorproject.data.SourceType;
import edu.fiu.cs.seniorproject.data.provider.DataProvider;
import edu.fiu.cs.seniorproject.data.provider.MBVCAProvider;

public class DataManager {

	private LinkedList<DataProvider> mProviderList = new LinkedList<DataProvider>();
	private static DataManager mSingleton = null;
	
	private DataManager() {
		// register all the providers
		mProviderList.add(new MBVCAProvider());
		//mProviderList.add(new GPProvider());
	}
	
	public List<Event> getEventList(Location location, String category, String radius, String query) {
		List<Event> result = null;
		
		if ( mProviderList.size() > 0 ) {
			for( int i = 0; i < mProviderList.size(); i++ ) {
				DataProvider provider = mProviderList.get(i);
				
				if ( provider.supportEvents() ) {
					List<Event> providerList = provider.getEventList(location, category, radius, query);
					
					if ( providerList != null && providerList.size() > 0 ) {
						if ( result == null ) {
							result = providerList;
						} else {
							result.addAll(providerList);
						}
					}
				}
			}
		}
		return result;
	}
	
	
	public List<Place> getPlaceList(Location location, String category, String radius, String query) {
		List<Place> result = null;
		
		if ( mProviderList.size() > 0 ) {
			for( int i = 0; i < mProviderList.size(); i++ ) {
				DataProvider provider = mProviderList.get(i);
				
				if ( provider.supportPlaces() ) {
					List<Place> providerList = provider.getPlaceList(location, category, radius, query);
					
					if ( providerList != null && providerList.size() > 0 ) {
						if ( result == null ) {
							result = providerList;
						} else {
							result.addAll(providerList);
						}
					}
				}
			}
		}
		return result;
	}
	
	public Event getEventDetails( String eventId, SourceType source ) {
		Event result = null;
		for( int i = 0; i < mProviderList.size(); i++ ) {
			DataProvider provider = mProviderList.get(i);
			if ( provider.supportEvents() && provider.getSource() == source ) {
				result = provider.getEventDetails(eventId);
				break;
			}
		}
		return result;
	}
	
	public Place getPlaceDetails( String placeId, String reference, SourceType source ) {
		Place result = null;
		for( int i = 0; i < mProviderList.size(); i++ ) {
			DataProvider provider = mProviderList.get(i);
			if ( provider.supportPlaces() && provider.getSource() == source ) {
				result = provider.getPlaceDetails(placeId, reference);
				break;
			}
		}
		return result;
	}
	
	public static DataManager getSingleton() {
		if ( mSingleton == null ) {
			mSingleton = new DataManager();
		}
		return mSingleton;
	}
}
