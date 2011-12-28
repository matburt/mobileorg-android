package com.matburt.mobileorg.Services;

import java.io.IOException;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
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

public class SyncService extends Service implements
		SharedPreferences.OnSharedPreferenceChangeListener {
	private static final String START_ALARM = "START_ALARM";

	private SharedPreferences appSettings;
	private MobileOrgApplication appInst;

	private AlarmManager alarmManager;
	private PendingIntent alarmIntent;
	private boolean alarmScheduled = false;

	@Override
	public void onCreate() {
		super.onCreate();
		this.appSettings = PreferenceManager
				.getDefaultSharedPreferences(getApplicationContext());
		this.appSettings.registerOnSharedPreferenceChangeListener(this);
		this.appInst = (MobileOrgApplication) this.getApplication();
		this.alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
	}

	@Override
	public void onDestroy() {
		unsetAlarm();
	}

	public static void startAlarm(Context context) {
		Intent intent = new Intent(context, SyncService.class);
		intent.putExtra("action", SyncService.START_ALARM);
		context.startService(intent);
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		String action = intent.getStringExtra("action");
		if (action != null && action.equals(START_ALARM))
			setAlarm();
		else
			runSynchronizer();
		return 0;
	}

	private void runSynchronizer() {
		Log.d("MobileOrg", "Starting synch");
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
			return;

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
	}

	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,
			String key) {
		if (key.equals("doAutoSync")) {
			if (sharedPreferences.getBoolean("doAutoSync", false)
					&& !this.alarmScheduled)
				setAlarm();
			else if (!sharedPreferences.getBoolean("doAutoSync", false)
					&& this.alarmScheduled)
				unsetAlarm();
		} else if (key.equals("autoSyncInterval"))
			resetAlarm();
	}

	private void setAlarm() {
		if (!this.alarmScheduled) {
			boolean doAutoSync = this.appSettings.getBoolean("doAutoSync",
					false);
			if (doAutoSync) {
				int interval = Integer.parseInt(this.appSettings.getString(
						"autoSyncInterval", "1800000"), 10);

				this.alarmIntent = PendingIntent.getService(appInst, 0,
						new Intent(this, SyncService.class), 0);
				alarmManager.setRepeating(AlarmManager.RTC,
						System.currentTimeMillis() + interval, interval,
						alarmIntent);

				this.alarmScheduled = true;
			}
		}
	}

	private void unsetAlarm() {
		if (this.alarmScheduled) {
			this.alarmManager.cancel(this.alarmIntent);
			this.alarmScheduled = false;
		}
	}

	private void resetAlarm() {
		unsetAlarm();
		setAlarm();
	}

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}
}
