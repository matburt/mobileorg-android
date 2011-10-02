package com.matburt.mobileorg.Synchronizers;

import java.io.IOException;

import android.app.ProgressDialog;
import android.content.Context;
import android.preference.PreferenceManager;

public class SynchManager {
	final Synchronizer appSync;

	public SynchManager(Context context) {
		String userSynchro = PreferenceManager.getDefaultSharedPreferences(context).getString("syncSource", "");
		if (userSynchro.equals("webdav")) {
			appSync = new WebDAVSynchronizer(context);
		} else if (userSynchro.equals("sdcard")) {
			appSync = new SDCardSynchronizer(context);
		} else if (userSynchro.equals("dropbox")) {
			appSync = new DropboxSynchronizer(context);
		} else
			appSync = null;
	}
	
	public boolean isConfigured() {
		return appSync.isConfigured();
	}
	
	public boolean sync(ProgressDialog progressDialog) throws IOException {
		if (!isConfigured())
			return false;
		
		appSync.sync(progressDialog);
		return true;
	}
	
	public void close() {
		appSync.close();
	}
}