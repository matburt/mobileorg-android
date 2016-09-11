package com.matburt.mobileorg.Synchronizers;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import com.matburt.mobileorg.util.OrgUtils;

/**
 * Created by bcoste on 17/06/16.
 */
public class AuthData {
    static AuthData mAuthData = null;
    private SharedPreferences appSettings;

    private String password;

    private AuthData(Context context) {
        this.appSettings = PreferenceManager
                .getDefaultSharedPreferences(context.getApplicationContext());
        this.password = "";
    }

    public static AuthData getInstance(Context context) {
        if (mAuthData == null) mAuthData = new AuthData(context);
        return mAuthData;
    }

    public boolean usePassword(){
        return appSettings.getBoolean("usePassword", true);
    }

    public String getPath() {
        return OrgUtils.rStrip(appSettings.getString("scpPath", ""));
    }

    public String getUser() {
        return OrgUtils.rStrip(appSettings.getString("scpUser", ""));
    }

    public String getHost() {
        return OrgUtils.rStrip(appSettings.getString("scpHost", ""));
    }

    public String getPubFile() {
        return OrgUtils.rStrip(appSettings.getString("scpPubFile", ""));
    }

    public Integer getPort() {
        return Integer.parseInt(appSettings.getString("scpPort", ""));
    }

    public String getPassword() {
        return OrgUtils.rStrip(appSettings.getString("scpPass", ""));
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public boolean isValid() {
        if (getPassword().equals("") ||
                getFileName().equals("") ||
                getUser().equals("") ||
                (getHost().equals("") && getPubFile().equals(""))) {
//            Log.i("MobileOrg", "Test Connection Failed for not being configured");
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
