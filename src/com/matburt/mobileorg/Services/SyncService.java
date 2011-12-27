package com.matburt.mobileorg.Services;

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

import com.matburt.mobileorg.Parsing.MobileOrgApplication;
import com.matburt.mobileorg.Synchronizers.DropboxSynchronizer;
import com.matburt.mobileorg.Synchronizers.SDCardSynchronizer;
import com.matburt.mobileorg.Synchronizers.SSHSynchronizer;
import com.matburt.mobileorg.Synchronizers.Synchronizer;
import com.matburt.mobileorg.Synchronizers.WebDAVSynchronizer;

public class SyncService extends Service implements SharedPreferences.OnSharedPreferenceChangeListener{
	private SharedPreferences appSettings;
	private MobileOrgApplication appInst;
	
	private Timer timer = new Timer();
	private Date lastSyncDate;
	private Boolean timerScheduled = false;
	private static long kMinimalSyncInterval = 30000;

	@Override
	public void onCreate() {
		super.onCreate();		
		this.appSettings = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
		this.appSettings.registerOnSharedPreferenceChangeListener(this);
		this.appInst = (MobileOrgApplication) this.getApplication();
	}
	
	@Override
	public void onStart(Intent intent, int startid) {
		startTimer();
	}
	
	@Override
	public void onDestroy() {
		stopTimer();
	}

	 @Override
	 public int onStartCommand(Intent intent, int flags, int startId) {
		 runSynchronizer();
		 return 0;
	 }

	private void runSynchronizer() {
		String syncSource = appSettings.getString("syncSource", "");
		final Synchronizer synchronizer;

		if (syncSource.equals("webdav"))
			synchronizer = new WebDAVSynchronizer(this, this.appInst);
		else if (syncSource.equals("sdcard"))
			synchronizer = new SDCardSynchronizer(this, this.appInst);
		else if (syncSource.equals("dropbox"))
			synchronizer = new DropboxSynchronizer(this, this.appInst);
		else if (syncSource.equals("scp"))
			synchronizer = new SSHSynchronizer(this, this.appInst);
		else
			return; // TODO Throw error
		
		if (!synchronizer.isConfigured())
			return; // TODO Throw error

		Thread syncThread = new Thread() {
			public void run() {
				try {
					synchronizer.sync();
				} catch (IOException e) {
				} finally {
					synchronizer.close();
				}
			}
		};

		syncThread.start();
		this.lastSyncDate = new Date();
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
	

	private void startTimer() {
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


	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}
	
	
	private void stopTimer() {
		if(this.timer != null) {
			this.timer.cancel();
			this.timer = new Timer();
			this.timerScheduled = false;
		}
		Log.d("MobileOrg", "Sync Service Unscheduled");
	}
}
