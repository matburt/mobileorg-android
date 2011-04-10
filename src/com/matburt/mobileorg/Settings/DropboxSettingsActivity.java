package com.matburt.mobileorg.Settings;

import android.content.Intent;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;
import com.matburt.mobileorg.R;
import com.matburt.mobileorg.Synchronizers.DropboxAuthActivity;

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
            Intent loginIntent = new Intent(this, DropboxAuthActivity.class);
            startActivity(loginIntent);
        }
        return true;
    }
}