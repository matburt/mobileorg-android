package com.matburt.mobileorg.Synchronizers;

import java.io.IOException;

import android.content.Context;
import android.preference.PreferenceManager;

/**
 * This is a wrapper class for {@link Synchronizer}. It should be used instead of
 * creating instances of {@link Synchronizer} directly.
 */
public class SyncManager {
	final Synchronizer synchronizer;

	public SyncManager(Context context) {
		String userSynchro = PreferenceManager.getDefaultSharedPreferences(context).getString("syncSource", "");
		if (userSynchro.equals("webdav")) {
			synchronizer = new WebDAVSynchronizer(context);
		} else if (userSynchro.equals("sdcard")) {
			synchronizer = new SDCardSynchronizer(context);
		} else if (userSynchro.equals("dropbox")) {
			synchronizer = new DropboxSynchronizer(context);
		} else
			synchronizer = null;
	}
	
	public boolean isConfigured() {
		if(synchronizer == null)
			return false;
		
		return synchronizer.isConfigured();
	}
	
	public boolean sync() throws IOException {
		if (!isConfigured())
			return false;
		
		synchronizer.sync();
		return true;
	}
	
	public void close() {
		synchronizer.close();
	}
}