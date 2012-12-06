package com.matburt.mobileorg.Services;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

public class MobileOrgStartupIntentReceiver extends BroadcastReceiver {

	private SharedPreferences appSettings;

	private boolean shouldStartService(Context context) {
		this.appSettings = PreferenceManager
				.getDefaultSharedPreferences(context.getApplicationContext());
		return this.appSettings.getBoolean("doAutoSync", false);
	}

	@Override
	public void onReceive(Context context, Intent intent) {
		if (this.shouldStartService(context)) {
			SyncService.startAlarm(context);
		}
		
		Intent calIntent = new Intent(context, CalendarSyncService.class);
		calIntent.putExtra(CalendarSyncService.FILELIST, new String[] {});
		context.startService(calIntent);
	}
}