package com.matburt.mobileorg.Views;

import android.util.Log;
import android.app.Activity;
import android.os.Bundle;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.content.Context;
import android.preference.PreferenceManager;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.view.Display;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.view.ViewGroup;
import android.widget.Button;
import android.view.ViewGroup.LayoutParams;
import android.util.AttributeSet;
import android.view.WindowManager;
import android.graphics.Canvas;
import android.view.LayoutInflater;
import android.view.View;
import com.matburt.mobileorg.R;

public class PageView extends RelativeLayout {
    static String TAG = "PageView";
    Button nextButton, previousButton;
    Context context;

    public PageView(Context context) {
	super(context);
    }

    public PageView(Context context, AttributeSet attrs) {
	super(context, attrs);
	this.context = context;
    }

    @Override
	public void onFinishInflate() {
	// LayoutInflater inflater=
	//     (LayoutInflater) LayoutInflater.from(context);
	// //Add next button
	// View button=inflater.inflate(R.layout.wizard_next_button,this);
	// nextButton = (Button) button.findViewById(R.id.wizard_next_button);
	// //Add previous button
	// button=inflater.inflate(R.layout.wizard_previous_button,this);
	// previousButton = (Button) button.findViewById(R.id.wizard_previous_button);
    }

    public Button getNextButton() { return nextButton; }

    public Button getPreviousButton() { return previousButton; }

    // @Override 
    // 	protected void onMeasure(int widthMeasureSpec, 
    // 				 int heightMeasureSpec){
    // 	int parentWidth = MeasureSpec.getSize(widthMeasureSpec);
    // 	int parentHeight = MeasureSpec.getSize(heightMeasureSpec);
    // 	// setMeasuredDimension(parentWidth, parentHeight);
    // 	// setLayoutParams(new 
    // 	// 		LinearLayout
    // 	// 		.LayoutParams(parentWidth,
    // 	// 			      parentHeight)
    // 	// 		);
    // 	// super.onMeasure(parentWidth, parentHeight);
    // 	setMeasuredDimension(measureWidth(widthMeasureSpec),
    // 			     measureHeight(heightMeasureSpec));

    // 	setLayoutParams(new 
    // 			LinearLayout
    // 			.LayoutParams(
    // 				      measureWidth(widthMeasureSpec),
    // 				      measureHeight(heightMeasureSpec))
    // 			);
    // 	super.onMeasure(widthMeasureSpec,
    // 	 		measureHeight(heightMeasureSpec));
    // 	//widthMeasureSpec, heightMeasureSpec);
    // }

    public int measureWidth(int measureSpec) {
        int result = 0;
        int specMode = MeasureSpec.getMode(measureSpec);
        int specSize = MeasureSpec.getSize(measureSpec);
        Display display = ( (WindowManager)
    			    getContext().getSystemService(Context.WINDOW_SERVICE) )
    	    .getDefaultDisplay(); 
        int screenWidth = display.getWidth();
        int screenHeight = display.getHeight();
        if (specMode == MeasureSpec.EXACTLY) {
    	    // We were told how big to be
    	    result = specSize;
        } else {
    	    // Measure the text
    	    result = screenWidth;
    	    if (specMode == MeasureSpec.AT_MOST) {
    		// Respect AT_MOST value if that was what is called for by measureSpec
    		result = Math.min(result, specSize);
    	    }
        }
    	Log.d(TAG, "Width: "+String.valueOf(result));
        return result;
    }
    
    public int measureHeight(int measureSpec) {
        int result = 0;
        int specMode = MeasureSpec.getMode(measureSpec);
        int specSize = MeasureSpec.getSize(measureSpec);
        Display display = ( (WindowManager)
    			    getContext().getSystemService(Context.WINDOW_SERVICE) )
    	    .getDefaultDisplay();
        int screenWidth = display.getWidth();
        int screenHeight = display.getHeight();
        if (specMode == MeasureSpec.EXACTLY) {
    	    // We were told how big to be
    	    result = specSize;
        } else {
    	    // Measure the text (beware: ascent is a negative number)
    	    result = screenHeight;
    	    if (specMode == MeasureSpec.AT_MOST) {
    		// Respect AT_MOST value if that was what is called for by measureSpec
    		result = Math.min(result, specSize);
    	    }
        }
    	Log.d(TAG, "Height: "+String.valueOf(result));
        return result;
    }
    
    // @Override
    // 	public void onDraw(Canvas canvas) {
    // 	super.onDraw(canvas);
    // }
}

