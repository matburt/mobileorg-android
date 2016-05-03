package com.matburt.mobileorg2.OrgData;

import android.app.Application;
import android.content.Context;

import com.matburt.mobileorg2.Services.SyncService;

public class MobileOrgApplication extends Application {
    
	private static MobileOrgApplication instance;
	
    @Override
    public void onCreate() {
        super.onCreate();
    	instance = this;
		SyncService.startAlarm(getApplicationContext());
    }
    
    public static Context getContext() {
    	return instance;
    }
}
