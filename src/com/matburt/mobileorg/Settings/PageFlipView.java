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
    WideLinearLayout container; // TODO do we need this?
    NextPageListener nextPageListener; // for handling next page
				       // button clicks
    PreviousPageListener previousPageListener; // for handling
					       // previous page button
					       // clicks
    int currentPage = 0;
    int screenWidth;

    ArrayList<EditText> editBoxes; //TODO do we need this?

    public PageFlipView(Context context) {
	super(context);
    }

    public PageFlipView(Context context, AttributeSet attrs) {
	super(context, attrs);
	//setup page flips
	nextPageListener = new NextPageListener();
	previousPageListener = new PreviousPageListener();
	//see http://blog.velir.com/index.php/2010/11/17/android-snapping-horizontal-scroll/
	mGestureDetector = new GestureDetector(getContext(),
					       new PageSwipeDetector());
        setOnTouchListener(this);
    }


    //This is the code for making the child views the same size as the
    //screen
    @Override
	protected void onMeasure(int w, int h) {
	int width = MeasureSpec.getSize(w);
	int height = MeasureSpec.getSize(h);
	Log.d(TAG, "Setting screen width to " + width);
	Log.d(TAG, "Setting screen height to " + height);
	int ws = MeasureSpec.makeMeasureSpec(width,MeasureSpec.EXACTLY);
	int hs = MeasureSpec.makeMeasureSpec(height,MeasureSpec.EXACTLY);
	// Also tell screen width to our only child
	container.setWidth(width);
	//container.measure(ws,hs);
	// and its children
	// for(int i=0; i<container.getChildCount(); i++) {
	//     View page = (View) container.getChildAt(i);
	//     page.measure(ws,hs);
	// }
	//setMeasuredDimension(width,height);
	super.onMeasure(w,h);
    }

    @Override
	public void onFinishInflate() {
	container = (WideLinearLayout) findViewById(R.id.wizard_container);
	Log.d(TAG,"Container count: "+container.getChildCount());
	//add onclick listeners for next/prev buttons
	for(int i=0; i<container.getChildCount(); i++) {
	    PageView page = (PageView) container.getChildAt(i);
	    //last page doesn't have a next button
	    if ( i != container.getChildCount() - 1 )
		page.getNextButton().setOnClickListener(nextPageListener);
	    //first page doesn't have a previous button
	    if ( i != 0 )
		page.getPreviousButton()
		    .setOnClickListener(previousPageListener);
	}
	//remove previous button from first page
	PageView page = (PageView) container.getChildAt(0);
	page.getPreviousButton().setVisibility(View.GONE);
	//remove next button from last page
	page = (PageView) container
	    .getChildAt( container.getChildCount() - 1 );
	page.getNextButton().setVisibility(View.GONE);
    }

    public void setEditBoxes(ArrayList e) { editBoxes = e; }

    public int getCurrentPage() { return currentPage; }

    public void setCurrentPage(int i) { currentPage = i; }

    public void restoreLastPage() {
        SharedPreferences prefs = ((Activity) getContext()).getPreferences(0); 
        currentPage = prefs.getInt("currentPage", 0);
	//scroll to last loaded page
	post(new Runnable() {
		@Override
		    public void run() {
		    scrollTo(currentPage*getMeasuredWidth(), 0);
                }
            });
    }

    public void saveCurrentPage() {
        SharedPreferences prefs = ((Activity) getContext()).getPreferences(0); 
    	SharedPreferences.Editor editor = prefs.edit();
	//save current page
        editor.putInt("currentPage", getCurrentPage());
        editor.commit();
    }

    //Code for setting up the page swipes and scrolling
    @Override
	public boolean onTouch(View v, MotionEvent event) {
	//If the user swipes
	if (mGestureDetector.onTouchEvent(event)) {
	    return true;
	}
	else if (event.getAction() == MotionEvent.ACTION_UP
		|| event.getAction() == MotionEvent.ACTION_CANCEL ){
	    int scrollX = getScrollX();
	    int featureWidth = v.getMeasuredWidth();
	    //TODO clean up this code
	    currentPage = ((scrollX + (featureWidth/2))/featureWidth);
	    int scrollTo = currentPage*featureWidth;
	    smoothScrollTo(scrollTo, 0);
	    return true;
	}
	else{
	    return false;
	}
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

    //hide keyboard if showing    
    void hideKeyboard() {
	InputMethodManager imm = (InputMethodManager) 
	    ((Activity)getContext())
	    .getSystemService(Context.INPUT_METHOD_SERVICE);
	imm.hideSoftInputFromWindow(getWindowToken(), 0);
    }

    class NextPageListener implements View.OnClickListener {
	@Override
	    public void onClick(View v) {
	    scrollRight();
	}
    }

    class PreviousPageListener implements View.OnClickListener {
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

