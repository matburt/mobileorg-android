package com.matburt.mobileorg.Settings;

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
import android.widget.ScrollView;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import com.matburt.mobileorg.R;

public class WizardActivity extends Activity
    implements RadioGroup.OnCheckedChangeListener {

    int syncWebDav, syncDropBox, syncSdCard;
    RadioGroup syncGroup; 

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.wizard);

	//setup horizontal scrollview
	Display display = getWindowManager().getDefaultDisplay(); 
	int screenWidth = display.getWidth();
	int screenHeight = display.getHeight();
	//set width of all setup screens
	// LinearLayout wizardLayout = (LinearLayout) findViewById(R.id.wizard_layout);
	// ScrollView wizardPage;
        // for (int i=0; i<wizardLayout.getChildCount(); i++) {
	//     wizardPage = (ScrollView) wizardLayout.getChildAt(i);
	//     wizardPage.setLayoutParams(new 
	// 			       ViewGroup.LayoutParams(
	// 						      screenWidth, 
	// 						      screenHeight));
	// }

	//get ids and pointers to sync radio buttons
	syncGroup = (RadioGroup) findViewById(R.id.sync_group);
	syncWebDav = ( (RadioButton) 
			findViewById(R.id.sync_webdav) ).getId();
	syncDropBox = ( (RadioButton) 
			findViewById(R.id.sync_dropbox) ).getId();
	syncSdCard = ( (RadioButton) 
			findViewById(R.id.sync_sdcard) ).getId();
	//setup click listener for sync radio group
	syncGroup.setOnCheckedChangeListener(this);
    }

    @Override
    	public void onCheckedChanged(RadioGroup arg, int checkedId) {
    	SharedPreferences appSettings = 
    	    PreferenceManager.getDefaultSharedPreferences(getBaseContext());
    	SharedPreferences.Editor editor = appSettings.edit();
    	if ( checkedId == syncWebDav )
    	    editor.putString("syncSource", "webdav");
    	else if ( checkedId == syncDropBox )
    	    editor.putString("syncSource", "dropbox");
    	else if ( checkedId == syncSdCard)
    	    editor.putString("syncSource", "sdcard");
    	editor.commit();
    }

    public class PageView extends ScrollView {

	public PageView(Context context) {
	    super(context);
	}

	public PageView(Context context, AttributeSet attrs) {
	    super(context, attrs);
	}

	@Override
	    protected void onMeasure(int widthMeasureSpec, 
				     int heightMeasureSpec) {
	    setMeasuredDimension(measureWidth(widthMeasureSpec),
				 measureHeight(heightMeasureSpec));
	}

	public int measureWidth(int measureSpec) {
	    int result = 0;
	    int specMode = MeasureSpec.getMode(measureSpec);
	    int specSize = MeasureSpec.getSize(measureSpec);
	    Display display = getWindowManager().getDefaultDisplay(); 
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
	    return result;
	}

	public int measureHeight(int measureSpec) {
	    int result = 0;
	    int specMode = MeasureSpec.getMode(measureSpec);
	    int specSize = MeasureSpec.getSize(measureSpec);
	    Display display = getWindowManager().getDefaultDisplay(); 
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
	    return result;
	}
    }
}

class TestMine {
    
}