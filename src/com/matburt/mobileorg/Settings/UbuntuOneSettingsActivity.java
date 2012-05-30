package com.matburt.mobileorg.Settings;

import android.os.Bundle;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;

import com.matburt.mobileorg.R;

public class UbuntuOneSettingsActivity extends PreferenceActivity implements OnPreferenceClickListener
{
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.ubuntuone_preferences);
    }

    public boolean onPreferenceClick(Preference p) {
        return true;
    }
}