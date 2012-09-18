package com.matburt.mobileorg.util;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import com.matburt.mobileorg.R;
import com.matburt.mobileorg.OrgData.OrgFile;
import com.matburt.mobileorg.OrgData.OrgNode;
import com.matburt.mobileorg.Synchronizers.Synchronizer;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.widget.ArrayAdapter;
import android.widget.Spinner;

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
    
    public static void setupSpinnerWithEmpty(Spinner spinner, ArrayList<String> data,
			String selection) {
		data.add("");
		setupSpinner(spinner, data, selection);
    }
    
	public static void setupSpinner(Spinner spinner, ArrayList<String> data,
			String selection) {		
		if(!TextUtils.isEmpty(selection) && !data.contains(selection))
			data.add(selection);
		
		ArrayAdapter<String> adapter = new ArrayAdapter<String>(spinner.getContext(),
				android.R.layout.simple_spinner_item, data);
		adapter.setDropDownViewResource(R.layout.edit_spinner_layout);
		spinner.setAdapter(adapter);
		int pos = data.indexOf(selection);
		if (pos < 0) {
			pos = 0;
		}
		spinner.setSelection(pos, true);
	}
	
	public static OrgNode getCaptureIntentContents(Intent intent) {
		String subject = intent
				.getStringExtra("android.intent.extra.SUBJECT");
		String text = intent.getStringExtra("android.intent.extra.TEXT");

		if(text != null && subject != null) {
			subject = "[[" + text + "][" + subject + "]]";
			text = "";
		}
		
		if(subject == null)
			subject = "";
		if(text == null)
			text = "";

		OrgNode node = new OrgNode();
		node.name = subject;
		node.setPayload(text);
		return node;
	}
	

	public static long getNodeFromPath(String path, ContentResolver resolver) throws OrgFileNotFoundException {
		String filename = path.substring("file://".length(), path.length());
		
		// TODO Handle links to headings instead of simply stripping it out
		if(filename.indexOf(":") > -1)
			filename = filename.substring(0, filename.indexOf(":"));
				
		OrgFile file = new OrgFile(filename, resolver);
		return file.nodeId;
	}
	
	public static void announceUpdate(Context context) {
		Intent intent = new Intent(Synchronizer.SYNC_UPDATE);
		intent.putExtra(Synchronizer.SYNC_DONE, true);
		context.sendBroadcast(intent);
	}
}
