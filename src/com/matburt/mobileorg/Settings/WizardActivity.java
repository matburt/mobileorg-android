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
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import java.util.List;
import android.widget.EditText;
import android.view.inputmethod.InputMethodManager;
import com.matburt.mobileorg.R;

public class WizardActivity extends Activity
    implements RadioGroup.OnCheckedChangeListener {

    //container
    PageFlipView wizard;
    //page 1 variables
    int syncWebDav, syncDropBox, syncSdCard;
    RadioGroup syncGroup; 
    //page 2 variables
    View loginPage;
    List<EditText> loginBoxes = new List<EditText>();

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.wizard);
	wizard = findViewById(R.id.wizard_parent);
	//setup page 1
	PageView page1Container = (PageView) findViewById(R.id.wizard_page1);
	LayoutInflater inflater=
	    (LayoutInflater) getApplicationContext()
	    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
	View page1=inflater.inflate(R.layout.wizard_page1,page1Container);
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

    /**
     * Upon being resumed we can retrieve the current state.  This allows us
     * to update the state if it was changed at any time while paused.
     */
    @Override
    protected void onResume() {
        super.onResume();
	wizard.restoreLastPage();
    }

    /**
     * Any time we are paused we need to save away the current state, so it
     * will be restored correctly when we are resumed.
     */
    @Override
    protected void onPause() {
        super.onPause();
	wizard.saveCurrentPage();
	//unselect current login boxes
	// for(EditText box : loginBoxes) 
	//     box.clearFocus();
	//hide keyboard if showing
	InputMethodManager imm = (InputMethodManager) 
	    getSystemService(INPUT_METHOD_SERVICE);
	imm.hideSoftInputFromWindow(getWindowToken(), 0);
    }

    @Override
    	public void onCheckedChanged(RadioGroup arg, int checkedId) {
    	SharedPreferences appSettings = 
    	    PreferenceManager.getDefaultSharedPreferences(getBaseContext());
    	SharedPreferences.Editor editor = appSettings.edit();
    	if ( checkedId == syncWebDav )
    	    editor.putString("syncSource", "webdav");
    	else if ( checkedId == syncDropBox ) {
    	    //editor.putString("syncSource", "dropbox");
	    createDropboxLogin();
	    //add login boxes to list ... for handling orientation
	    //changes
	    loginBoxes.clear();
	    loginBoxes.add( loginPage.findViewById(R.id.wizard_dropbox_email) );
	    loginBoxes.add( loginPage.findViewById(R.id.wizard_dropbox_password) );
	}
    	else if ( checkedId == syncSdCard)
    	    editor.putString("syncSource", "sdcard");
    	editor.commit();
    }
    
    void createDropboxLogin() {
	PageView page2 = (PageView) findViewById(R.id.wizard_page2);
	LayoutInflater inflater=
	    (LayoutInflater) LayoutInflater.from(getApplicationContext());
	loginPage = inflater.inflate(R.layout.wizard_dropbox,page2);
    }

    /* TODO: Unfocus login textboxes on orientation change */
}
