package com.matburt.mobileorg;

import android.app.Service;
import java.util.TimerTask;
import java.util.Timer;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

public class MobileOrgSyncService extends Service {
	private Timer timer = new Timer();
	private Boolean timerScheduled = false;
	private SharedPreferences appSettings;
	private ReportableError syncError;
	
	@Override
	public void onCreate() {
		super.onCreate();
		Log.d("MobileOrg", "Sync service created");
		
		this.appSettings = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
	}

	@Override
	public void onDestroy() {
		if(this.timer != null) {
			this.timer.cancel();
			this.timerScheduled = false;
		}
	}

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}
	
	@Override
	public void onStart(Intent intent, int startid) {
		Log.d("MobileOrg", "Sync Service Scheduled");
		if(!this.timerScheduled) {
			this.timerScheduled = true;
			timer.scheduleAtFixedRate(new TimerTask() {
					public void run() {
						Log.d("MobileOrg", "Sync Service Fired");
						try {
							runSynchronizer();
						} catch(Throwable e){ 
							//Noop. When the service is first fired
							//during startup some errors might be
							//thrown, for instance if the MobileOrg db
							//is stored on the sd card but the sd card
							//isn't mounted yet.  Catch the error here
							//so the service doesn't die
						}
					}
				}, 0, 30*60*1000); //Run every 30 minutes.  Hard setting this for now
		}
	}

	public void runSynchronizer() {
        String userSynchro = this.appSettings.getString("syncSource","");
        final Synchronizer appSync;
        if (userSynchro.equals("webdav")) {
            appSync = new WebDAVSynchronizer(this);
        }
        else if (userSynchro.equals("sdcard")) {
            appSync = new SDCardSynchronizer(this);
        }
        else if (userSynchro.equals("dropbox")) {
            appSync = new DropboxSynchronizer(this);
        }
        else {
            return;
        }

        if (!appSync.checkReady()) {
            return;
        }

        Thread syncThread = new Thread() {
                public void run() {
                	try {
                		syncError = null;
	                    appSync.pull();
	                    appSync.push();
                	}
                	catch(ReportableError e) {
                		syncError = e;
                	}
                    finally {
                        appSync.close();
                    }
				}
			};
        syncThread.start();
    }
}