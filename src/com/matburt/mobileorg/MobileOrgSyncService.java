package com.matburt.mobileorg;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Environment;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.util.Log;
import com.matburt.mobileorg.Error.ErrorReporter;
import com.matburt.mobileorg.Error.ReportableError;
import com.matburt.mobileorg.Parsing.Node;
import com.matburt.mobileorg.Parsing.OrgFileParser;
import com.matburt.mobileorg.Synchronizers.DropboxSynchronizer;
import com.matburt.mobileorg.Synchronizers.SDCardSynchronizer;
import com.matburt.mobileorg.Synchronizers.Synchronizer;
import com.matburt.mobileorg.Synchronizers.WebDAVSynchronizer;

import java.io.File;
import java.util.*;

public class MobileOrgSyncService extends Service implements SharedPreferences.OnSharedPreferenceChangeListener{
	private Timer timer = new Timer();
	private Date lastSyncDate;
	private Boolean timerScheduled = false;
	private SharedPreferences appSettings;
	private ReportableError syncError;
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
			if(doAutoSync) {
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
					
					runParser();
				}
			};
        syncThread.start();
		this.lastSyncDate = new Date();
    }

	public void runParser() {
        MobileOrgApplication appInst = (MobileOrgApplication)this.getApplication();
		MobileOrgDatabase appdb = new MobileOrgDatabase((Context)this);
        HashMap<String, String> allOrgList = appdb.getOrgFiles();
        String storageMode = this.appSettings.getString("storageMode", "");
        String userSynchro = this.appSettings.getString("syncSource","");
        String orgBasePath = "";

        if (userSynchro.equals("sdcard")) {
            String indexFile = this.appSettings.getString("indexFilePath","");
            File fIndexFile = new File(indexFile);
            orgBasePath = fIndexFile.getParent() + "/";
        }
        else {
            orgBasePath = Environment.getExternalStorageDirectory().getAbsolutePath() +
                          "/mobileorg/";
        }

        OrgFileParser ofp = new OrgFileParser(allOrgList,
                                              storageMode,
                                              userSynchro,
                                              appdb,
                                              orgBasePath);
        try {
        	ofp.parse();
        	appInst.rootNode = ofp.rootNode;
            appInst.edits = ofp.parseEdits();
			Collections.sort(appInst.rootNode.subNodes, Node.comparator);
        }
        catch(Throwable e) {
        	ErrorReporter.displayError(this, "An error occurred during parsing: " + e.toString());
        }

		appdb.close();
    }
}