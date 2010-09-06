package com.matburt.mobileorg;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.Preference.OnPreferenceClickListener;
import android.widget.Toast;

public class SettingsActivity extends PreferenceActivity
{
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent prefsIntent = getIntent();
        int resourceID = prefsIntent.getIntExtra("prefs",R.xml.preferences);
        addPreferencesFromResource(resourceID);                
    }
}