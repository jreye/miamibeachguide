package edu.fiu.cs.seniorproject.utils;

import java.lang.ref.WeakReference;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

import edu.fiu.cs.seniorproject.R;
import edu.fiu.cs.seniorproject.manager.DataManager;
import edu.fiu.cs.seniorproject.manager.DataManager.IBitmapDownloaderListener;

import android.content.Context;
import android.graphics.Bitmap;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.SimpleAdapter;

public class BitmapSimpleAdapter extends SimpleAdapter {

	private Map<String, BitmapDownloadListener> mLoaders = new Hashtable<String, BitmapDownloadListener>();
	private Map<View, BitmapDownloadListener> mViewMap = new WeakHashMap<View, BitmapDownloadListener>();
	
	public BitmapSimpleAdapter(Context context,
			List<? extends Map<String, ?>> data, int resource, String[] from, int[] to) {
		super(context, data, resource, from, to);
	}
	
	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		
		if ( convertView != null && this.mViewMap.containsKey(convertView) ) {	// is reusing a row. check for the downloader
			BitmapDownloadListener listener = this.mViewMap.get(convertView);
			if ( listener != null ) {
				listener.setTarget(null);
			}
			this.mViewMap.remove(convertView);
		}
		
		View result = super.getView(position, convertView, parent);
		
		if ( result != null ) {
			Object item = this.getItem(position);
			
			if ( item != null ) {
								
				ImageView iv = (ImageView)result.findViewById(R.id.image);
				
				if ( iv != null ) {
					
					@SuppressWarnings("unchecked")
					Hashtable<String, String> entry = (Hashtable<String, String>)item;
					
					if ( entry != null && !entry.containsKey("image") && entry.containsKey("latitude") && entry.containsKey("longitude") ) {
						String latitude = entry.get("latitude");
						String longitude = entry.get("longitude");
						entry.put("image", String.format("http://maps.googleapis.com/maps/api/streetview?size=100x100&location=%1$s,%2$s&sensor=true", latitude, longitude));
					}
					
					if ( entry != null && entry.containsKey("image") && entry.containsKey("id") ) {
					
						String id = (String)entry.get("id");
						
						if ( mLoaders.containsKey( id )) {	// we already have the bitmap
							
							BitmapDownloadListener listener = mLoaders.get(id);
							listener.setTarget(result);
							this.mViewMap.put(result, listener);
							if ( listener.mBitmap != null ) {
								iv.setImageBitmap(listener.mBitmap);
							} else {
								iv.setImageResource(R.drawable.main_icon);
							}
						} else {
							iv.setImageResource(R.drawable.main_icon);
							String image = (String)entry.get("image");
							if ( image != null && !image.isEmpty() ) {
								
								BitmapDownloadListener listener = new BitmapDownloadListener(id, result);
								listener.setTarget(result);
								this.mViewMap.put(result, listener);
								DataManager.getSingleton().downloadBitmap(image, listener);
								this.mLoaders.put(id, listener);
							}
						}
					} else {
						Logger.Debug("No image, using default image");
						iv.setImageResource(R.drawable.main_icon);
					}
				}
			} else {
				Logger.Warning("Unable to get item at " + position );
			}
		}
		
		return result;
	}
	
	private class BitmapDownloadListener implements IBitmapDownloaderListener {

		private String mId;
		private WeakReference<View> mTarget = null;
		public Bitmap mBitmap = null;
		
		public BitmapDownloadListener( String id, View target) {
			mId = id;
			mTarget = new WeakReference<View>(target);
		}
		
		public void setTarget(View target) {
			mTarget = target != null ? new WeakReference<View>(target) : null;
		}
		
		@Override
		public void OnCompleted(Bitmap bm) {
			if ( bm != null && mId != null ) {
				mBitmap = bm;
				
				if ( mTarget != null && mTarget.get() != null ) {
					ImageView iv = (ImageView)mTarget.get().findViewById(R.id.image);
					if ( iv != null ) {
						iv.setImageBitmap(bm);
					}
				}
			}
		}

		@Override
		public void OnFail() {		
			mBitmap = null;
		}
		
	}
}
