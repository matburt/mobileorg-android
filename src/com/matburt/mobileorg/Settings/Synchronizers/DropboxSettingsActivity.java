package com.matburt.mobileorg.Settings.Synchronizers;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;

import com.matburt.mobileorg.R;
import com.matburt.mobileorg.Synchronizers.DropboxAuthActivity;

public class DropboxSettingsActivity extends PreferenceActivity implements OnPreferenceClickListener, OnSharedPreferenceChangeListener
{
    private Preference triggerLogin;
    public static final String KEY_DROPBOX_PATH = "dropboxPath";
  	
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.dropbox_preferences);
        triggerLogin = (Preference)findPreference("dropboxLogin");
        triggerLogin.setOnPreferenceClickListener(this);
        setPreferenceSummary(PreferenceManager.getDefaultSharedPreferences(this), KEY_DROPBOX_PATH);
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
    
    public boolean onPreferenceClick(Preference p) {
        if (p == this.triggerLogin) {
            Intent loginIntent = new Intent(this, DropboxAuthActivity.class);
            startActivity(loginIntent);
        }
        return true;
    }
}