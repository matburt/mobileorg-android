package com.matburt.mobileorg.Settings;

import android.util.Log;
import android.app.Activity;
import android.os.Bundle;
import android.content.Context;
import android.view.Display;
import android.widget.LinearLayout;
import android.widget.HorizontalScrollView;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.util.AttributeSet;
import android.view.WindowManager;
import android.graphics.Canvas;
import android.view.GestureDetector; 
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.View;
import android.view.MotionEvent;
import android.view.LayoutInflater;
import android.preference.PreferenceManager;
import android.content.SharedPreferences;
import android.widget.EditText;
import java.util.ArrayList;
import android.view.inputmethod.InputMethodManager;
import android.graphics.Rect;
import android.view.ViewGroup.LayoutParams;
import com.matburt.mobileorg.R;

public class PageFlipView extends HorizontalScrollView 
    implements View.OnTouchListener {

    static String TAG = "PageFlipView";
    //for page flips, scrolling
    static final int SWIPE_MIN_DISTANCE = 5;
    static final int SWIPE_THRESHOLD_VELOCITY = 50;
    GestureDetector mGestureDetector;
    WideLinearLayout container;
    NextPageButtonListener nextPageButtonListener; // for handling
						   // next page button
						   // clicks
    PreviousPageButtonListener previousPageButtonListener; // ditto
    int maxScrollX;
    int maxEnabledPage = -1;
    int currentPage = 0;
    int screenWidth;

    public PageFlipView(Context context) {
	super(context);
    }

    public PageFlipView(Context context, AttributeSet attrs) {
	super(context, attrs);
	//setup page flips for next/prev buttons
	nextPageButtonListener = new NextPageButtonListener();
	previousPageButtonListener = new PreviousPageButtonListener();
	//see http://blog.velir.com/index.php/2010/11/17/android-snapping-horizontal-scroll/
	//setup page swiping
	mGestureDetector = new GestureDetector(getContext(),
	 				       new PageSwipeDetector());
        setOnTouchListener(this);
    }

    //Make the child views the same size as the screen
    @Override
	protected void onMeasure(int w, int h) {
	int width = MeasureSpec.getSize(w);
	// Tell screen width to our only child
	container.setWidth(width);
	//default to w and h given in XML
	super.onMeasure(w,h);
    }

    //stuff we can do only after view is constructed
    @Override
	public void onFinishInflate() {
	container = (WideLinearLayout) findViewById(R.id.wizard_container);
	Log.d(TAG,"Container count: "+container.getChildCount());
	//add onclick listeners for next/prev buttons
	for(int i=0; i<container.getChildCount(); i++) {
	    View pageContainer = (View) container.getChildAt(i);
	    //last page doesn't have a next button
	    if ( i != container.getChildCount() - 1 )
		pageContainer.findViewById(R.id.wizard_next_button)
		    .setOnClickListener(nextPageButtonListener);
	    //first page doesn't have a previous button
	    if ( i != 0 ) 
		pageContainer.findViewById(R.id.wizard_previous_button)
		    .setOnClickListener(previousPageButtonListener);
	}
    }

    //starting from given page, disable all next buttons, and disable
    //swiping to the right.
    public void disableAllNextActions(int page) {
	//first disable next buttons
	if ( container != null )
	    for(int i=page; i<container.getChildCount(); i++) {
		//get the pageview container
		View pageContainer = (View) container.getChildAt(i);
		//last page doesn't have a next button
		if ( i != container.getChildCount() - 1 )
		    pageContainer.findViewById(R.id.wizard_next_button)
			.setEnabled(false);
	    }	    
	//user can't scroll past current page
	maxScrollX = page * getMeasuredWidth();
    }

    //disable/enable all buttons for given page
    public void setNavButtonState(boolean state, int page) { 
	//get the pageview container
	View pageContainer = (View) container.getChildAt(page);
	//last page doesn't have a next button
	if ( page != container.getChildCount() - 1 )
	    pageContainer.findViewById(R.id.wizard_next_button)
		.setEnabled(state);
	//first page doesn't have a previous button
	if ( page != 0 )
	    pageContainer.findViewById(R.id.wizard_previous_button)
		.setEnabled(state);
    }

    //enable prev/next buttons for give page and allow scrolling
    //to next page
    public void enablePage(int page) {
	if ( page == -1 ) { 
	    maxScrollX = 0;
	    maxEnabledPage = -1;
	    return;
	}
	setNavButtonState(true, page);
	maxScrollX = (page+1) * getMeasuredWidth();
	maxEnabledPage = ( page > maxEnabledPage ) ?
	    page : maxEnabledPage;
    }

    public int getNumberOfPages() { return container.getChildCount(); }

    public int getCurrentPage() { return currentPage; }

    public void setCurrentPage(int i) { currentPage = i; }

    //used for orientation change
    public void restoreLastPage() {
        SharedPreferences prefs = ((Activity) getContext()).getPreferences(0); 
        currentPage = prefs.getInt("currentPage", 0);
	maxEnabledPage = prefs.getInt("maxEnabledPage", -1);
	//scroll to last loaded page
	post(new Runnable() {
		@Override
		    public void run() {
		    enablePage(maxEnabledPage);
		    scrollTo(currentPage*getMeasuredWidth(), 0);
		}
            });
    }

    //ditto
    public void saveCurrentPage() {
        SharedPreferences prefs = ((Activity) getContext()).getPreferences(0); 
    	SharedPreferences.Editor editor = prefs.edit();
	//save current page
        editor.putInt("currentPage", getCurrentPage());
	editor.putInt("maxEnabledPage", maxEnabledPage);
        editor.commit();
    }
    
    //if scroll more than %25 of screen width, then automagically
    //scroll to next page
    @Override
    	public boolean onTouch(View v, MotionEvent event) {
	boolean actionUp=false;
    	//If the user swipes
    	if (mGestureDetector.onTouchEvent(event)) return true;
	//else
    	switch (event.getAction()) {
    	case MotionEvent.ACTION_UP:
    	case MotionEvent.ACTION_POINTER_UP:
    	case MotionEvent.ACTION_CANCEL:
    	    int scrollX = getScrollX();
    	    int featureWidth = getMeasuredWidth();
    	    //if scroll more than %25 of width, then go to next page
    	    currentPage = ((scrollX + (featureWidth/4*3))/featureWidth);
    	    Log.d(TAG,"scrollX: "+scrollX+" featureWidth: "+(featureWidth/4*3)
    	     	  +" page: "+currentPage);
    	    int scrollTo = currentPage*featureWidth;
    	    smoothScrollTo(scrollTo, 0);
	    //if you don't return true, a child view will interfere
	    //with the scrolling
	    actionUp=true;
    	    break;
    	default: break;
    	}
        return actionUp;
    }

    //Basically, keep HorizontalScrollView from scrolling past current
    //page, until it's OK to do so (ex: user puts password)
    @Override
	public void onScrollChanged(int l, int t, int oldl, int oldt) {
	//Log.d(TAG,"scroll: "+l+", "+oldl+" maxscroll: "+maxScrollX);
	if ( l > maxScrollX ) { 
	    //if trying to scroll past maximum allowed, then snap back
	    scrollTo(oldl,0);
	    //recalculate current page if necessary
	    currentPage = ( currentPage * getMeasuredWidth() == maxScrollX ) ?
		--currentPage : currentPage;
	}
	else super.onScrollChanged(l,t,oldl,oldt);
    }

    class NextPageButtonListener implements View.OnClickListener {
	@Override
	    public void onClick(View v) {
	    scrollRight();
	}
    }

    class PreviousPageButtonListener implements View.OnClickListener {
	@Override
	    public void onClick(View v) {
	    scrollLeft();
	}
    }

    //hide keyboard if showing    
    void hideKeyboard() {
	InputMethodManager imm = (InputMethodManager) 
	    ((Activity)getContext())
	    .getSystemService(Context.INPUT_METHOD_SERVICE);
	imm.hideSoftInputFromWindow(getWindowToken(), 0);
    }

    void scrollRight() {
	hideKeyboard();
	//unfocus login boxes
	View selectedBox = findFocus();
	if (selectedBox != null) selectedBox.clearFocus();
	//scroll
	int featureWidth = getMeasuredWidth();
	currentPage = (currentPage < (container.getChildCount() - 1)) ?
	    currentPage + 1 : container.getChildCount() - 1;
	smoothScrollTo(currentPage*featureWidth, 0);

     }

    void scrollLeft() {
	hideKeyboard();
	//unfocus login boxes
	View selectedBox = findFocus();
	if (selectedBox != null) selectedBox.clearFocus();
	//scroll
	int featureWidth = getMeasuredWidth();
	currentPage = (currentPage > 0) ? 
	    currentPage - 1 : 0;
	smoothScrollTo(currentPage*featureWidth, 0);
    }

    class PageSwipeDetector extends SimpleOnGestureListener {
        @Override
	    public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            //try {
		// Log.d(TAG,"velocity:"+String.valueOf(velocityX)+
		//       " activeFeature:"+String.valueOf(currentPage)+
		//       " childCount:"+String.valueOf(container.getChildCount())+
		//       " featureWidth:"+String.valueOf(getMeasuredWidth()));
                //right to left
                //if(e1.getX() - e2.getX() > SWIPE_MIN_DISTANCE
		if( velocityX < 0
		   && Math.abs(velocityX) > SWIPE_THRESHOLD_VELOCITY) {
                    scrollRight();
		    return true;
                }
                //left to right
		//else if (e2.getX() - e1.getX() > SWIPE_MIN_DISTANCE 
		else if ( velocityX > 0
			  && Math.abs(velocityX) > SWIPE_THRESHOLD_VELOCITY) {
		    scrollLeft();
                    return true;
                }
            // } catch (Exception e) {
	    // 	Log.e(TAG, "There was an error processing the Fling event:" + e.getMessage());
            // }
            return false;
        }

	@Override  
	    public boolean onDown(MotionEvent e) {  
	    Log.v(TAG, "onDown");  
	    return true;  
	}  
    }
}

