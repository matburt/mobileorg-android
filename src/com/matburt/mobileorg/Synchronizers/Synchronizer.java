package com.matburt.mobileorg.Synchronizers;

import java.io.IOException;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.preference.PreferenceManager;

import com.matburt.mobileorg.Parsing.OrgDatabase;

abstract public class Synchronizer {
	public OrgDatabase appdb = null;
	public SharedPreferences appSettings = null;
	public Context rootContext = null;
	public static final String LT = "MobileOrg";
	public Resources r;

	public Synchronizer(Context parentContext) {
        this.rootContext = parentContext;
        this.r = this.rootContext.getResources();
        this.appdb = new OrgDatabase((Context)parentContext);
        this.appSettings = PreferenceManager.getDefaultSharedPreferences(
                                   parentContext.getApplicationContext());
	}

	public abstract boolean isConfigured();
	public abstract void pull() throws IOException;
	public abstract void push() throws IOException;
	
	public boolean synch() throws IOException {
		if (!isConfigured())
			return false;

		pull();
		push();
		return true;
	}
	
	public void close() {
		if (this.appdb != null)
			this.appdb.close();
	}

}
