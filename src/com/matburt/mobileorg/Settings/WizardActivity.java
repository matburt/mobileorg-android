package com.matburt.mobileorg.Settings;

import android.app.Activity;
import android.os.Bundle;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.content.Context;
import android.preference.PreferenceManager;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
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
}