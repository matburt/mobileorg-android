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
import com.matburt.mobileorg.R;

public class PageFlipView extends HorizontalScrollView {
    static String TAG = "PageFlipView";
    static final int SWIPE_MIN_DISTANCE = 5;
    static final int SWIPE_THRESHOLD_VELOCITY = 300;
 
    //private ArrayList<ArticleListItem> mItems = null;
    GestureDetector mGestureDetector;
    int mActiveFeature = 0;
    LinearLayout container;

    public PageFlipView(Context context) {
	super(context);
    }

    public PageFlipView(Context context, AttributeSet attrs) {
	super(context, attrs);
	//get number of pages
	LayoutInflater inflater=
	    (LayoutInflater) context
	    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
	container=(LinearLayout)inflater.inflate(R.layout.wizard,null);
	Log.d(TAG,"Container count: "+container.getChildCount());
	//setup page flips
	//see http://blog.velir.com/index.php/2010/11/17/android-snapping-horizontal-scroll/
	mGestureDetector = new GestureDetector(getContext(),
					       new MyGestureDetector());
					       //new MyGestureDetector());
        setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                //If the user swipes
                if (mGestureDetector.onTouchEvent(event)) {
                     return true;
                }
                else if(event.getAction() == MotionEvent.ACTION_UP
		   || event.getAction() == MotionEvent.ACTION_CANCEL ){
                    int scrollX = getScrollX();
                    int featureWidth = v.getMeasuredWidth();
                    mActiveFeature = ((scrollX + (featureWidth/2))/featureWidth);
                    int scrollTo = mActiveFeature*featureWidth;
                    smoothScrollTo(scrollTo, 0);
                    return true;
                }
                else{
                    return false;
                }
            }
        });
    }

    class MyGestureDetector extends SimpleOnGestureListener {
        @Override
	    public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            try {
		Log.d(TAG,"velocity:"+String.valueOf(velocityX)+
		      " activeFeature:"+String.valueOf(mActiveFeature)+
		      " childCount:"+String.valueOf(container.getChildCount())+
		      " featureWidth:"+String.valueOf(getMeasuredWidth()));
                //right to left
                if(e1.getX() - e2.getX() > SWIPE_MIN_DISTANCE
		   && Math.abs(velocityX) > SWIPE_THRESHOLD_VELOCITY) {
                    int featureWidth = getMeasuredWidth();
                    mActiveFeature = (mActiveFeature < (container.getChildCount() - 1)) ?
			mActiveFeature + 1 : container.getChildCount() -1;
                    smoothScrollTo(mActiveFeature*featureWidth, 0);
                    return true;
                }
                //left to right
                else if (e2.getX() - e1.getX() > SWIPE_MIN_DISTANCE 
			 && Math.abs(velocityX) > SWIPE_THRESHOLD_VELOCITY) {
                    int featureWidth = getMeasuredWidth();
                    mActiveFeature = (mActiveFeature > 0) ? 
			mActiveFeature - 1 : 0;
                    smoothScrollTo(mActiveFeature*featureWidth, 0);
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

