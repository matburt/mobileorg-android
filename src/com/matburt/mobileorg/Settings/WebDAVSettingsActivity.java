package com.matburt.mobileorg.Settings;

import android.annotation.SuppressLint;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Build;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;

import com.matburt.mobileorg.R;

public class WebDAVSettingsActivity extends PreferenceActivity implements OnSharedPreferenceChangeListener
{
    public static final String KEY_WEB_URL = "webUrl";
    public static final String KEY_WEB_USER = "webUser";
    public static final String KEY_WEB_PASS = "webPass";
    
	@SuppressLint("NewApi")
	@Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.webdav_preferences);
    	// Disable transitions if configured
		if (Build.VERSION.SDK_INT >= 5 && !PreferenceManager.getDefaultSharedPreferences(this).getBoolean("viewAnimateTransitions", true)) {
			overridePendingTransition(0, 0);
		}
		SharedPreferences shared = PreferenceManager.getDefaultSharedPreferences(this);
		setPreferenceSummary(shared, KEY_WEB_URL);
		setPreferenceSummary(shared, KEY_WEB_USER);
	}
    @SuppressLint("NewApi")
	@Override
	public void finish() {
		super.finish();
		// Disable transitions if configured
		if (Build.VERSION.SDK_INT >= 5 && !PreferenceManager.getDefaultSharedPreferences(this).getBoolean("viewAnimateTransitions", true)) {
			overridePendingTransition(0, 0);
		}	
	}
    @Override
    public void onPause() {
    	getPreferenceScreen().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
    	super.onPause();
    }

    @Override
    public void onResume() {
    	super.onResume();
    	getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,
    		String key) {
    	// Set up the initial values following the Settings design guidelines for Ice Cream Sandwich
    	// Settings should show their current value instead of a description
    	setPreferenceSummary(sharedPreferences, key);
    }

    protected void setPreferenceSummary(SharedPreferences sharedPreferences, String key) {
    	Preference pref = findPreference(key);
    	if (pref != null) {
    		if (key.equals(KEY_WEB_URL) || key.equals(KEY_WEB_USER)) {
    			String value = sharedPreferences.getString(key, "");
    			pref.setSummary(value);
    		}
    	}
    }
}