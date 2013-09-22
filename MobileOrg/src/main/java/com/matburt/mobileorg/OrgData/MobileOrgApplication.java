package com.matburt.mobileorg.OrgData;

import android.app.Application;
import android.content.Context;

import com.matburt.mobileorg.Services.SyncService;

public class MobileOrgApplication extends Application {
    
	private static MobileOrgApplication instance;
	
    @Override
    public void onCreate() {
    	instance = this;
		SyncService.startAlarm(getApplicationContext());
    }
    
    public static Context getContext() {
    	return instance;
    }
}
