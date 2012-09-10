package com.matburt.mobileorg.util;

import java.text.SimpleDateFormat;
import java.util.Date;

import android.content.Context;
import android.preference.PreferenceManager;
import android.text.TextUtils;

public class OrgUtils {
	
	public static String getTimestamp() {
		SimpleDateFormat sdf = new SimpleDateFormat("[yyyy-MM-dd EEE HH:mm]");		
		return sdf.format(new Date());
	}
	
	public static String getDefaultTodo(Context context) {
		return PreferenceManager.getDefaultSharedPreferences(context)
				.getString("defaultTodo", "");
	}
    
    public static boolean isSyncConfigured(Context context) {
    	String syncSource = PreferenceManager.getDefaultSharedPreferences(context)
		.getString("syncSource", "");
    	
    	if(TextUtils.isEmpty(syncSource))
    		return false;
    	else
    		return true;
    }
}
