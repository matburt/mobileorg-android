package com.matburt.mobileorg.Settings;

import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.preference.Preference;
import android.util.Log;
import com.matburt.mobileorg.R;

public class SDCardSettingsActivity extends PreferenceActivity
{
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.sdsync_preferences);
    }
}