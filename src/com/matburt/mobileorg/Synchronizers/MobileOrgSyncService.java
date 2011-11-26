package com.matburt.mobileorg.Synchronizers;

import java.io.IOException;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.util.Log;

import com.matburt.mobileorg.MobileOrgApplication;
import com.matburt.mobileorg.Error.ErrorReporter;
import com.matburt.mobileorg.Parsing.OrgFileParser;

public class MobileOrgSyncService extends Service implements SharedPreferences.OnSharedPreferenceChangeListener{
	private Timer timer = new Timer();
	private Date lastSyncDate;
	private Boolean timerScheduled = false;
	private SharedPreferences appSettings;
	private static long kMinimalSyncInterval = 30000;
	
	@Override
	public void onCreate() {
		super.onCreate();
		Log.d("MobileOrg", "Sync service created");
		
		this.appSettings = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
		this.appSettings.registerOnSharedPreferenceChangeListener(this);
	}

	@Override
	public void onDestroy() {
		stopTimer();
	}

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}
	
	@Override
	public void onStart(Intent intent, int startid) {
		startTimer();
	}

	public void startTimer() {
		if(!this.timerScheduled) {
			boolean doAutoSync = this.appSettings.getBoolean("doAutoSync", false);
			if(doAutoSync) { //This may can be removed since we are checking this at a higher level
				String intervalStr = this.appSettings.getString("autoSyncInterval", "1800000");
				int interval = Integer.parseInt(intervalStr, 10);
				
				//Prevent sync from firing within 30 seconds of last sync
				long delay = 0;
				if(lastSyncDate != null) {
					long difference = System.currentTimeMillis() - lastSyncDate.getTime();
					if(difference < kMinimalSyncInterval) {
						delay = kMinimalSyncInterval - difference;
					}
				}
				
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
					}, delay, interval);
				this.timerScheduled = true;
				Log.d("MobileOrg", "Sync Service Scheduled");
			}
		}
	}

	public void stopTimer() {
		if(this.timer != null) {
			this.timer.cancel();
			this.timer = new Timer();
			this.timerScheduled = false;
		}
		Log.d("MobileOrg", "Sync Service Unscheduled");
	}

	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
		if(key.equals("doAutoSync")) {
			if(sharedPreferences.getBoolean("doAutoSync", false) && !this.timerScheduled) {
				startTimer();
			} else if(!sharedPreferences.getBoolean("doAutoSync", false) && this.timerScheduled) {
				stopTimer();
			}
		} else if(key.equals("autoSyncInterval")) {
			stopTimer();
			startTimer();
		}
	}

	public void runSynchronizer() {
		final SyncManager syncman = new SyncManager(this);

		if (!syncman.isConfigured()) {
			return;
		}

		Thread syncThread = new Thread() {
			public void run() {
				try {
					syncman.sync();
				} catch (IOException e) {
				} finally {
					syncman.close();
				}

				runParser();
			}
		};
		syncThread.start();
		this.lastSyncDate = new Date();
	}

	public void runParser() {
        MobileOrgApplication appInst = (MobileOrgApplication)this.getApplication();

        OrgFileParser ofp = new OrgFileParser(getBaseContext(), appInst);
        try {
        	ofp.runParser(appSettings, appInst);
        }
        catch(Throwable e) {
        	ErrorReporter.displayError(this, "An error occurred during parsing: " + e.toString());
        }
    }
}