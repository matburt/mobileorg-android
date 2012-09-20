package com.matburt.mobileorg.Settings.Synchronizers;

import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
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
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.scp_preferences);
		SharedPreferences shared = PreferenceManager.getDefaultSharedPreferences(this);
		setPreferenceSummary(shared, KEY_SCP_PATH);
		setPreferenceSummary(shared, KEY_SCP_PORT);
		setPreferenceSummary(shared, KEY_SCP_USER);
		setPreferenceSummary(shared, KEY_SCP_HOST);
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
