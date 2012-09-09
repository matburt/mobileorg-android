package com.matburt.mobileorg.OrgData;

import android.app.Application;
import android.preference.PreferenceManager;
import android.text.TextUtils;

import com.matburt.mobileorg.Services.CalendarSyncService;
import com.matburt.mobileorg.Services.SyncService;

public class MobileOrgApplication extends Application {
	private CalendarSyncService calendarSyncService;
    
    @Override
    public void onCreate() {
		SyncService.startAlarm(getApplicationContext());
    }

    
//    public CalendarSyncService getCalendarSyncService() {
//    	if(this.calendarSyncService == null)
//    		this.calendarSyncService = new CalendarSyncService(getDB(), this);
//    	
//    	return this.calendarSyncService;
//    }
    
    public boolean isSyncConfigured() {
    	String syncSource = PreferenceManager.getDefaultSharedPreferences(getApplicationContext())
		.getString("syncSource", "");
    	
    	if(TextUtils.isEmpty(syncSource))
    		return false;
    	else
    		return true;
    }
    
    public String getChangesString() {
    	int changes = OrgProviderUtil.getChangesCount(getContentResolver());
    	if(changes > 0)
    		return "[" + changes + "]";
    	else
    		return "";
    }
}
