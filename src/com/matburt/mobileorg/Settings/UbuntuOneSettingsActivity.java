package com.matburt.mobileorg.Settings;

import android.annotation.SuppressLint;
import android.os.Build;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceManager;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;

import com.matburt.mobileorg.R;

public class UbuntuOneSettingsActivity extends PreferenceActivity implements OnPreferenceClickListener
{
    @SuppressLint("NewApi")
	@Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.ubuntuone_preferences);
    	// Disable transitions if configured
		if (Build.VERSION.SDK_INT >= 5 && !PreferenceManager.getDefaultSharedPreferences(this).getBoolean("animateTransitions", true)) {
			overridePendingTransition(0, 0);
		}
}

    public boolean onPreferenceClick(Preference p) {
        return true;
    }
}