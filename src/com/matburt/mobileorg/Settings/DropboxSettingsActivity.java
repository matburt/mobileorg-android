package com.matburt.mobileorg.Settings;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Build;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;

import com.matburt.mobileorg.R;
import com.matburt.mobileorg.Synchronizers.DropboxAuthActivity;

@SuppressLint("NewApi")
public class DropboxSettingsActivity extends PreferenceActivity implements OnPreferenceClickListener, OnSharedPreferenceChangeListener
{
    public static final String KEY_DROPBOX_PATH = "dropboxPath";
	private Preference triggerLogin;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.dropbox_preferences);
        triggerLogin = (Preference)findPreference("dropboxLogin");
        triggerLogin.setOnPreferenceClickListener(this);
    	// Disable transitions if configured
		if (Build.VERSION.SDK_INT >= 5 && !PreferenceManager.getDefaultSharedPreferences(this).getBoolean("viewAnimateTransitions", true)) {
			overridePendingTransition(0, 0);
		}
		setPreferenceSummary(PreferenceManager.getDefaultSharedPreferences(this), KEY_DROPBOX_PATH);
   }

    public boolean onPreferenceClick(Preference p) {
        if (p == this.triggerLogin) {
            Intent loginIntent = new Intent(this, DropboxAuthActivity.class);
            startActivity(loginIntent);
        }
        return true;
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
			if (key.equals(KEY_DROPBOX_PATH)) {
				String value = sharedPreferences.getString(key, "");
				pref.setSummary(value);
			}
		}
	}
}