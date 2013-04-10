package com.matburt.mobileorg.util;

import java.util.ArrayList;
import java.util.HashSet;

import com.matburt.mobileorg.OrgData.MobileOrgApplication;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.preference.PreferenceManager;
import android.text.TextUtils;

public class PreferenceUtils {
	private static final int DEFAULT_FONTSIZE = 14;

	
	public static boolean getCombineBlockAgendas() {
		Context context = MobileOrgApplication.getContext();
		try {
			return PreferenceManager.getDefaultSharedPreferences(context)
					.getBoolean("combineBlockAgendas", false);
		} catch (UnsupportedOperationException e) { return false; }
	}
	
	public static String getDefaultTodo() {
		Context context = MobileOrgApplication.getContext();
		return PreferenceManager.getDefaultSharedPreferences(context)
				.getString("defaultTodo", "");
	}

	public static HashSet<String> getExcludedTags() {
		Context context = MobileOrgApplication.getContext();
		String tags = PreferenceManager.getDefaultSharedPreferences(context).getString(
				"excludeTagsInheritance", null);
		
		if (tags == null)
			return null;
		
		HashSet<String> tagsSet = new HashSet<String>();
		for (String tag: tags.split(":")) {
			if(TextUtils.isEmpty(tag) == false)
				tagsSet.add(tag);
		}
		
		return tagsSet;
	}

	public static int getFontSize() {
		Context context = MobileOrgApplication.getContext();
		try {
			int fontSize = Integer.parseInt(PreferenceManager.getDefaultSharedPreferences(
					context).getString("fontSize", "14"));
			
			if (fontSize > 2)
				return fontSize;
		} catch (NumberFormatException e) {
		}
		
		return DEFAULT_FONTSIZE;
	}

	public static int getLevelOfRecursion() {
		Context context = MobileOrgApplication.getContext();
		return Integer.parseInt(PreferenceManager
				.getDefaultSharedPreferences(context).getString(
						"viewRecursionMax", "0"));
	}

	public static String getThemeName() {
		Context context = MobileOrgApplication.getContext();
	    SharedPreferences appSettings =
	            PreferenceManager.getDefaultSharedPreferences(context);
	    return appSettings.getString("theme", "Dark");
	}

	public static boolean useAdvancedCapturing() {
		Context context = MobileOrgApplication.getContext();
		return PreferenceManager.getDefaultSharedPreferences(context)
				.getBoolean("captureAdvanced", true);
	}

	public static boolean isSyncConfigured() {
		Context context = MobileOrgApplication.getContext();
		String syncSource = PreferenceManager.getDefaultSharedPreferences(context)
		.getString("syncSource", "");
		
		if(TextUtils.isEmpty(syncSource))
			return false;
		else
			return true;
	}

	public static boolean isUpgradedVersion() {
		Context context = MobileOrgApplication.getContext();
	    SharedPreferences appSettings =
	        PreferenceManager.getDefaultSharedPreferences(context);
	    SharedPreferences.Editor editor = appSettings.edit();
	    int versionCode = appSettings.getInt("appVersion", -1);
	    try {
	        PackageInfo pInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
	        int newVersion = pInfo.versionCode;
	        if (versionCode != -1 && versionCode != newVersion) {
	            editor.putInt("appVersion", newVersion);
	            editor.commit();
	            return true;
	        }
	    } catch (Exception e) { };
	    return false;
	}

	public static ArrayList<String> getSelectedTodos() {
		Context context = MobileOrgApplication.getContext();
	    SharedPreferences appSettings =
		        PreferenceManager.getDefaultSharedPreferences(context);
	    
	    ArrayList<String> todos = new ArrayList<String>();
	    
	    String todoString = appSettings.getString("selectedTodos", "").trim();
	    if (TextUtils.isEmpty(todoString))
	    	return todos;
	    
	    for (String todo : todoString.split(" ")) {
	    	if (TextUtils.isEmpty(todo))
	    		continue;
	    	else
	    		todos.add(todo);
	    }
	    
	    return todos;
	}
}
