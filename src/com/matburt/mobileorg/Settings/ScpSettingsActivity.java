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

public class ScpSettingsActivity extends PreferenceActivity implements OnSharedPreferenceChangeListener
{
    public static final String KEY_SCP_PATH = "scpPath";
	public static final String KEY_SCP_PORT = "scpPort";
	public static final String KEY_SCP_USER = "scpUser";
	public static final String KEY_SCP_HOST = "scpHost";

	@SuppressLint("NewApi")
	@Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.scp_preferences);
    	// Disable transitions if configured
		if (Build.VERSION.SDK_INT >= 5 && !PreferenceManager.getDefaultSharedPreferences(this).getBoolean("viewAnimateTransitions", true)) {
			overridePendingTransition(0, 0);
		}
		SharedPreferences shared = PreferenceManager.getDefaultSharedPreferences(this);
		setPreferenceSummary(shared, KEY_SCP_PATH);
		setPreferenceSummary(shared, KEY_SCP_PORT);
		setPreferenceSummary(shared, KEY_SCP_USER);
		setPreferenceSummary(shared, KEY_SCP_HOST);
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
			if (key.equals(KEY_SCP_PATH) || key.equals(KEY_SCP_PORT) || key.equals(KEY_SCP_HOST) || key.equals(KEY_SCP_USER)) {
				String value = sharedPreferences.getString(key, "");
				pref.setSummary(value);
			}
		}
	}
}
