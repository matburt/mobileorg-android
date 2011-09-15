package com.matburt.mobileorg.Settings;

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

public class WideLinearLayout extends LinearLayout {

    public WideLinearLayout(Context context) {
	super(context);
    }

    public WideLinearLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    	protected void onMeasure(int w, int h) {
    	super.onMeasure(w,h);
    	int width = MeasureSpec.getSize(w);
    	int height = MeasureSpec.getSize(h);
	int ws = MeasureSpec.makeMeasureSpec(width,MeasureSpec.EXACTLY);
	int hs = MeasureSpec.makeMeasureSpec(height,MeasureSpec.EXACTLY);
    	// and its children
    	for(int i=0; i<getChildCount(); i++) {
    	    View page = (View) getChildAt(i);
    	    page.measure(ws,hs);
    	}
	setMeasuredDimension(width,height);
    }
}
