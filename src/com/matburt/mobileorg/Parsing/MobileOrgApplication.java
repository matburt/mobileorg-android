package com.matburt.mobileorg.Parsing;

import java.util.ArrayList;

import android.app.Application;
import android.preference.PreferenceManager;

import com.matburt.mobileorg.Services.SyncService;

public class MobileOrgApplication extends Application {
    public ArrayList<EditNode> edits;
    private OrgDatabase appdb;
    
    @Override
    public void onCreate() {
    	this.appdb = new OrgDatabase(this);
		init();
		SyncService.startAlarm(this);
    }
    
    private void init() {
		if (this.appdb == null || this.appdb.getFiles().isEmpty())
			return;
	}
  
    
    public OrgDatabase getDB() {
    	return this.appdb;
    }
    
    public boolean isSyncConfigured() {
    	String syncSource = PreferenceManager.getDefaultSharedPreferences(this)
		.getString("syncSource", "");
    	
    	if(syncSource.isEmpty())
    		return false;
    	else
    		return true;
    }
}
