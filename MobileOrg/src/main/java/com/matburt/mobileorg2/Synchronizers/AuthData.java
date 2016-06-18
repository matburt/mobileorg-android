package com.matburt.mobileorg2.Synchronizers;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

/**
 * Created by bcoste on 17/06/16.
 */
public class AuthData {
    static AuthData mAuthData = null;
    private SharedPreferences appSettings;

    private AuthData(Context context) {
        this.appSettings = PreferenceManager
                .getDefaultSharedPreferences(context.getApplicationContext());
    }

    static AuthData getInstance(Context context) {
        if (mAuthData == null) mAuthData = new AuthData(context);
        return mAuthData;
    }

    String getPath() {
        return appSettings.getString("scpPath", "");
    }

    String getUser() {
        return appSettings.getString("scpUser", "");
    }

    String getHost() {
        return appSettings.getString("scpHost", "");
    }

    String getPubFile() {
        return appSettings.getString("scpPubFile", "");
    }

    Integer getPort() {
        return Integer.parseInt(appSettings.getString("scpPort", ""));
    }

    String getPassword() {
        return appSettings.getString("scpPass", "");
    }

    public boolean isValid() {
        if (getPassword().equals("") ||
                getFileName().equals("") ||
                getUser().equals("") ||
                (getHost().equals("") && getPubFile().equals(""))) {
            Log.i("MobileOrg", "Test Connection Failed for not being configured");
            return false;
        }
        return true;
    }

    public String getFileName() {
        String path = getPath();
        String[] pathElements = path.split("/");
        if (pathElements.length > 0) {
            return pathElements[pathElements.length - 1];
        }
        return "";
    }

    public String getRootUrl() {
        String path = getPath();
        String[] pathElements = path.split("/");
        String directoryActual = "/";
        if (pathElements.length > 1) {
            for (int i = 0; i < pathElements.length - 1; i++) {
                if (pathElements[i].length() > 0) {
                    directoryActual += pathElements[i] + "/";
                }
            }
        }
        return directoryActual;
    }
}
