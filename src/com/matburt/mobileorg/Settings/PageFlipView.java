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
    boolean[] rightFlipEnabled;
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
	//mGestureDetector = new GestureDetector(getContext(),
	//					       new PageSwipeDetector());
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
	    //get the pageview container
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
	rightFlipEnabled = new boolean[getNumberOfPages()];
    }

    //disable next/prev buttons
    public void disableAllNavButtons() {
	if ( container != null ) {
	    for(int i=0; i<container.getChildCount(); i++) {
		//get the pageview container
		View pageContainer = (View) container.getChildAt(i);
		//last page doesn't have a next button
		if ( i != container.getChildCount() - 1 )
		    pageContainer.findViewById(R.id.wizard_next_button)
			.setEnabled(false);
		//first page doesn't have a previous button
		if ( i != 0 )
		    pageContainer.findViewById(R.id.wizard_previous_button)
			.setEnabled(false);
	    }
	    for(int i=0; i<getNumberOfPages(); i++)
		rightFlipEnabled[i] = false;
	    maxScrollX = 0;
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
	//then user can't scroll past current page
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
	rightFlipEnabled[ page ] = state;
    }

    //enable prev/next buttons for give page and allow scrolling
    //to next page
    public void enablePage(int page) {
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
		    enablePage(currentPage);
		    scrollTo(currentPage*getMeasuredWidth(), 0);
		}
            });
    }

    public void saveCurrentPage() {
        SharedPreferences prefs = ((Activity) getContext()).getPreferences(0); 
    	SharedPreferences.Editor editor = prefs.edit();
	//save current page
        editor.putInt("currentPage", getCurrentPage());
	editor.putInt("maxEnabledPage", maxEnabledPage);
        editor.commit();
    }

    float oldPos;
    boolean DRAG = false;

    //Code for setting up the page swipes and scrolling
    @Override
	public boolean onTouch(View v, MotionEvent event) {
	return false;
	//If the user swipes
	// if (mGestureDetector.onTouchEvent(event)) {
	//     return true;
	// }
	//else 
	// switch (event.getAction()) {
	// // case MotionEvent.ACTION_DOWN:
	// // case MotionEvent.ACTION_POINTER_DOWN:
	// //     oldPos = (int) event.getX();
	// //     Log.d(TAG, "DRAG=true" );
	// //     DRAG = true;
	// //     break;
	// case MotionEvent.ACTION_MOVE:
	//     if (DRAG) {
	//     // 	if ( event.getX() - oldPos > 0 ) { // right scroll
	//     // 	    if ( !rightFlipEnabled[ currentPage ] ) {
	//     // 		Log.d(TAG,"ActionMove: Page swype disabled");
	//     // 		 	return true;
	//     // 	    }
	//     // 	}		
	//     // } else {
	//     // 	oldPos = event.getX();
	//     // 	Log.d(TAG, "DRAG=true" );
	//     // 	DRAG = true;
	//     // 	return true;
	//     }
	//     break;
	// case MotionEvent.ACTION_UP:
	// case MotionEvent.ACTION_POINTER_UP:
	// case MotionEvent.ACTION_CANCEL:
	//     DRAG=false;
	//     // if ( !rightFlipEnabled[ currentPage ] ) {
	//     // 	Log.d(TAG,"Page swype disabled");
	//     // 	return true;
	//     // }
	//     break;
	// //     int scrollX = getScrollX();
	// //     int featureWidth = v.getMeasuredWidth();
	// //     //TODO clean up this code
	// //     int newPage = ((scrollX + (featureWidth/2))/featureWidth);
	// //     currentPage = newPage;
	// //     int scrollTo = currentPage*featureWidth;
	// //     smoothScrollTo(scrollTo, 0);
	// //     return true;
	// // }
	// }
	// return false;
    }

    void scrollRight() {
	hideKeyboard();
	int featureWidth = getMeasuredWidth();
	currentPage = (currentPage < (container.getChildCount() - 1)) ?
	    currentPage + 1 : container.getChildCount() -1;
	smoothScrollTo(currentPage*featureWidth, 0);
	//unfocus login boxes
	View selectedBox = findFocus();
	if (selectedBox != null) selectedBox.clearFocus();
     }

    void scrollLeft() {
	hideKeyboard();
	int featureWidth = getMeasuredWidth();
	currentPage = (currentPage > 0) ? 
	    currentPage - 1 : 0;
	smoothScrollTo(currentPage*featureWidth, 0);
	//unfocus login boxes
	View selectedBox = findFocus();
	if (selectedBox != null) selectedBox.clearFocus();
    }

    @Override
	public void onScrollChanged (int l, int t, int oldl, int oldt) {
	Log.d(TAG,"scroll: "+l+", "+oldl+" maxscroll: "+maxScrollX);
	if ( l > maxScrollX ) { 
	    //if trying to scroll past maximum allowed, then snap back
	    scrollTo(oldl,0);
	    //recalculate current page if necessary
	    currentPage = ( currentPage * getMeasuredWidth() == maxScrollX ) ?
		--currentPage : currentPage;
	}
	else super.onScrollChanged(l,t,oldl,oldt);
    }

    //hide keyboard if showing    
    void hideKeyboard() {
	InputMethodManager imm = (InputMethodManager) 
	    ((Activity)getContext())
	    .getSystemService(Context.INPUT_METHOD_SERVICE);
	imm.hideSoftInputFromWindow(getWindowToken(), 0);
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

    class PageSwipeDetector extends SimpleOnGestureListener {
        @Override
	    public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            try {
		Log.d(TAG,"velocity:"+String.valueOf(velocityX)+
		      " activeFeature:"+String.valueOf(currentPage)+
		      " childCount:"+String.valueOf(container.getChildCount())+
		      " featureWidth:"+String.valueOf(getMeasuredWidth()));
                //right to left
                if(e1.getX() - e2.getX() > SWIPE_MIN_DISTANCE
		   && Math.abs(velocityX) > SWIPE_THRESHOLD_VELOCITY) {
		    if ( !rightFlipEnabled[ currentPage ] ) {
			Log.d(TAG,"Page swype disabled");
			return true;
		    }
                    scrollRight();
		    return true;
                }
                //left to right
                else if (e2.getX() - e1.getX() > SWIPE_MIN_DISTANCE 
			 && Math.abs(velocityX) > SWIPE_THRESHOLD_VELOCITY) {
		    scrollLeft();
                    return true;
                }
            } catch (Exception e) {
		Log.e(TAG, "There was an error processing the Fling event:" + e.getMessage());
            }
            return false;
        }

	@Override  
	    public boolean onDown(MotionEvent e) {  
	    Log.v(TAG, "onDown");  
	    return true;  
	}  
    }
}

