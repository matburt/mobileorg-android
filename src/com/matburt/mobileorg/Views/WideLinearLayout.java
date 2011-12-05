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
import android.view.ViewGroup;
import com.matburt.mobileorg.R;

public class WideLinearLayout extends LinearLayout {

    int screenWidth;

    public WideLinearLayout(Context context) {
	super(context);
    }

    public WideLinearLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public void setWidth(int w) {
	screenWidth = w;
    }

    @Override
    	protected void onMeasure(int w, int h) {
    	int width = MeasureSpec.getSize(screenWidth);
    	int height = MeasureSpec.getSize(h);
    	// and its children
    	for(int i=0; i<getChildCount(); i++) {
    	    View page = (View) getChildAt(i);
    	    //page.measure(screenWidth,hs);
	    ViewGroup.LayoutParams lp = page.getLayoutParams();
	    lp.width = screenWidth;
	    lp.height = height;
    	}
	//setMeasuredDimension(width,height);
    	super.onMeasure(w,h);
    }
}
