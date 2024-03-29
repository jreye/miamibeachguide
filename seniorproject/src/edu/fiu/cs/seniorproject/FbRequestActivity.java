package edu.fiu.cs.seniorproject;

import java.util.ArrayList;
import java.util.List;

import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.view.Menu;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.facebook.FacebookActivity;
import com.facebook.Session;
import com.facebook.SessionState;
import com.facebook.android.DialogError;
import com.facebook.android.Facebook;
import com.facebook.android.Facebook.DialogListener;
import com.facebook.android.FacebookError;

import edu.fiu.cs.seniorproject.utils.Logger;

public class FbRequestActivity extends FacebookActivity {

	private Facebook facebook = null;
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fb_request);
        
        facebook = new Facebook(getResources().getString(R.string.fb_app_id));
        
        if ( this.getSession() != null ) {
        	this.closeSession();
        }
        
        Resources resources = getResources();
        Intent intent = this.getIntent();
        if ( intent != null ) {
        	TextView tv = (TextView)findViewById(R.id.title);
        	
        	if ( tv != null ) {
	        	if ( intent.hasExtra("title")) {
	        		tv.setText(intent.getStringExtra("title") );
	        	} else {
	        		tv.setText(resources.getString(R.string.join_me));
	        	}
        	}
        	
        	tv = (TextView)findViewById(R.id.message);
        	if ( tv != null ) {
        		if ( intent.hasExtra("message")) {
	        		tv.setText(intent.getStringExtra("message") );
	        	} else {
	        		tv.setText(resources.getString(R.string.join_invite) + resources.getString(R.string.miami_beach));
	        	}
        	}
        }
        
        List<String> permissions = new ArrayList<String>(1);
        permissions.add("publish_stream");
        
        this.openSessionForPublish(getResources().getString(R.string.fb_app_id), permissions );        
    }

    @SuppressWarnings("deprecation")
	@Override
    protected void onSessionStateChange(SessionState state, Exception exception) {
      // user has either logged in or not ...
      if (state.isOpened()) {
    	  Logger.Debug("session is opened!!!");
    	  // Set the Facebook instance session variables
    	  
          facebook.setAccessToken(Session.getActiveSession().getAccessToken());
          facebook.setAccessExpires(Session.getActiveSession().getExpirationDate().getTime());
      } else {
    	  Logger.Debug("session is closed. exiting!!!");
    	  //this.finish();
      }
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_fb_request, menu);
        return true;
    }
    
    public void onPostButtonClick(View v) {
    	Session session = this.getSession();
    	
    	if ( session != null && session.isOpened() ) {
    		this.sendRequestDialog();
    	} else {
    		List<String> permissions = new ArrayList<String>(1);
            permissions.add("publish_stream");            
            this.openSessionForPublish(getResources().getString(R.string.fb_app_id), permissions );  
    	}
    }
    
    private void sendRequestDialog() {
    	Bundle params = new Bundle();
    	
    	TextView tv = (TextView)findViewById(R.id.title);
    	if ( tv != null ) {
    		params.putString("title", tv.getText().toString());
    	}
    	tv = (TextView)findViewById(R.id.message);
    	if ( tv != null ) {
    		params.putString("message", tv.getText().toString());
    	}
    	
    	this.facebook.dialog(this, "apprequests", params, new DialogListener() {
			
			@Override
			public void onFacebookError(FacebookError e) {
				Toast.makeText(FbRequestActivity.this, getResources().getString(R.string.error_posting_request), Toast.LENGTH_SHORT).show();
			}
			
			@Override
			public void onError(DialogError e) {
				Toast.makeText(FbRequestActivity.this, getResources().getString(R.string.error_posting_request), Toast.LENGTH_SHORT).show();
			}
			
			@Override
			public void onComplete(Bundle values) {
				FbRequestActivity.this.finish();
			}
			
			@Override
			public void onCancel() {
				Logger.Debug("User cancel the post");
			}
		});
    }
    
}
