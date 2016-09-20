package com.matburt.mobileorg.synchronizers;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.matburt.mobileorg.util.FileUtils;
import com.matburt.mobileorg.util.OrgUtils;

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

    public static AuthData getInstance(Context context) {
        if (mAuthData == null) mAuthData = new AuthData(context);
        return mAuthData;
    }

    public boolean usePassword() {
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

    public Integer getPort() {
        return Integer.parseInt(appSettings.getString("scpPort", ""));
    }

    public String getPassword() {
        return OrgUtils.rStrip(appSettings.getString("scpPass", ""));
    }

    static public String PRIVATE_KEY = "dsa";
    static public String PUBLIC_KEY = PRIVATE_KEY + ".pub";

    public static String getPrivateKeyPath(Context context){
        return context.getFilesDir().getAbsoluteFile() + "/" + PRIVATE_KEY;
    }

    public static String getPublicKeyPath(Context context){
        return context.getFilesDir().getAbsoluteFile() + "/" + PUBLIC_KEY;
    }

    public static String getPublicKey(Context context){
        return FileUtils.read(context, getPublicKeyPath(context));
    }


}
