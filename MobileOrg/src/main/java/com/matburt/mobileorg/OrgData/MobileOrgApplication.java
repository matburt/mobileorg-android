package com.matburt.mobileorg.OrgData;

import android.app.Application;
import android.content.Context;
import android.preference.PreferenceManager;

import com.matburt.mobileorg.Services.SyncService;
import com.matburt.mobileorg.Synchronizers.DropboxSynchronizer;
import com.matburt.mobileorg.Synchronizers.NullSynchronizer;
import com.matburt.mobileorg.Synchronizers.SDCardSynchronizer;
import com.matburt.mobileorg.Synchronizers.SSHSynchronizer;
import com.matburt.mobileorg.Synchronizers.Synchronizer;
import com.matburt.mobileorg.Synchronizers.UbuntuOneSynchronizer;
import com.matburt.mobileorg.Synchronizers.WebDAVSynchronizer;

public class MobileOrgApplication extends Application {
    
	private static MobileOrgApplication instance;

    public static Context getContext() {
        return instance;
    }
    
    @Override
    public void onCreate() {
        super.onCreate();
    	instance = this;

        OrgDatabase.startDB(this);
        startSynchronizer();
        OrgFileParser.startParser(this);
        SyncService.startAlarm(this);
    }

    public void startSynchronizer() {
        String syncSource =
                PreferenceManager
                        .getDefaultSharedPreferences(getApplicationContext())
                        .getString("syncSource", "");
        Context c = getApplicationContext();

        if (syncSource.equals("webdav"))
            Synchronizer.setInstance(new WebDAVSynchronizer(c));
        else if (syncSource.equals("sdcard"))
            Synchronizer.setInstance(new SDCardSynchronizer(c));
        else if (syncSource.equals("dropbox"))
            Synchronizer.setInstance(new DropboxSynchronizer(c));
        else if (syncSource.equals("ubuntu"))
            Synchronizer.setInstance(new UbuntuOneSynchronizer(c));
        else if (syncSource.equals("scp"))
            Synchronizer.setInstance(new SSHSynchronizer(c));
        else
            Synchronizer.setInstance(new NullSynchronizer(c));
    }
}
