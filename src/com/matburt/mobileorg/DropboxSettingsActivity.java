package com.matburt.mobileorg;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.Preference.OnPreferenceClickListener;
import android.widget.Toast;
import android.content.Intent;

public class DropboxSettingsActivity extends PreferenceActivity implements OnPreferenceClickListener
{
    private Preference triggerLogin;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.dropbox_preferences);
        triggerLogin = (Preference)findPreference("dropboxLogin");
        triggerLogin.setOnPreferenceClickListener(this);
    }

    public boolean onPreferenceClick(Preference p) {
        if (p == this.triggerLogin) {
            Intent loginIntent = new Intent();
            loginIntent.setClassName("com.matburt.mobileorg",
                                     "com.matburt.mobileorg.DropboxAuthActivity");
            startActivity(loginIntent);
        }
        return true;
    }
}