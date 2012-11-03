package edu.fiu.cs.seniorproject;

import edu.fiu.cs.seniorproject.utils.Logger;
import android.app.Activity;
import android.content.Intent;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.Animation.AnimationListener;
import android.widget.NumberPicker;

public class FilterActivity extends Activity {
	
	private boolean filterExpanded = false;
	private Animation showFilterAnimation = null;
	private Animation hideFilterActivity = null;
	
	private MenuItem mExpandMenuItem = null;
	
	@Override
    public boolean onOptionsItemSelected(MenuItem item) {
    	Logger.Debug("click on menu item = " + item.getItemId());
        switch (item.getItemId()) {            
            case R.id.expand_collapse:
            	this.switchFilterView(item);
            	return true;
        }
        return super.onOptionsItemSelected(item);
    }
	
	protected void switchFilterView(MenuItem item) {
    	setFilterVisible(true);
    	filterExpanded = !filterExpanded;
    	
    	if ( filterExpanded ) {
    		item.setIcon(R.drawable.navigation_collapse_dark);    		
    		this.hideFilterActivity.cancel();    		
    		findViewById(R.id.filter_form).startAnimation(this.showFilterAnimation);
    		mExpandMenuItem = item;
    	} else {
    		item.setIcon(R.drawable.navigation_expand_dark);    		
    		this.showFilterAnimation.cancel();
    		findViewById(R.id.filter_form).startAnimation(this.hideFilterActivity);
    		mExpandMenuItem = null;
    	}
    }
	
	protected void hideFilters() {
		if ( mExpandMenuItem != null ) {
    		this.switchFilterView(mExpandMenuItem);
    	}
	}
	
	public void onFilterClick(View view) {
		this.onFilterClicked();
		this.hideFilters();
    }
	
	protected void onFilterClicked() {
	}
	
	protected void setupFilters() {
	        
        NumberPicker picker = (NumberPicker)findViewById(R.id.radius_picker);
        if ( picker != null ) {
        	picker.setMinValue(1);
        	picker.setMaxValue(20);
        	picker.setWrapSelectorWheel(false);
        }

        this.showFilterAnimation = AnimationUtils.loadAnimation(this, R.anim.slide_from_top);
        this.hideFilterActivity = AnimationUtils.loadAnimation(this, R.anim.slide_to_top);
        
        this.hideFilterActivity.setAnimationListener(new AnimationListener() {			
			@Override
			public void onAnimationStart(Animation animation) {
			}
			
			@Override
			public void onAnimationRepeat(Animation animation) {
			}
			
			@Override
			public void onAnimationEnd(Animation animation) {
				Logger.Debug("Animation completed. Hiding..");
				setFilterVisible(false);
			}
		});
	}
	
	protected void setFilterVisible(boolean visible) {
		int visibility = visible ? View.VISIBLE : View.GONE;
		
		findViewById(R.id.filter_form).setVisibility(visibility);
		findViewById(R.id.filter_ok).setVisibility(visibility);
		
		View category = findViewById(R.id.category_spinner);
		if ( category != null ) {
			category.setVisibility(visibility);
		}
		
		View dates = findViewById(R.id.dates_spinner);
		if ( dates != null ) {
			dates.setVisibility(visibility);
		}
		
		View picker = findViewById(R.id.radius_picker);
		if ( picker != null ) {
			picker.setVisibility(visibility);
		}
	}
	
	protected boolean isFilterExpanded() {
		return this.filterExpanded;
	}
	
	public void onSettingsClick(MenuItem view) {
    	Intent intent = new Intent(this, SettingsActivity.class);
    	this.startActivity(intent);
    } 
}
