package com.matburt.mobileorg.OrgData;

import android.app.Application;

import com.matburt.mobileorg.Services.SyncService;

public class MobileOrgApplication extends Application {
    
    @Override
    public void onCreate() {
		SyncService.startAlarm(getApplicationContext());
    }
}
