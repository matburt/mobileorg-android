package com.matburt.mobileorg.Parsing;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.OutputStreamWriter;
import java.io.StringWriter;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.util.Log;

import com.matburt.mobileorg.MobileOrgApplication;

public class NodeWriter {
	private SharedPreferences appSettings;
	private OrgDatabase appdb;
	private Activity appActivity;
	public static final String LT = "MobileOrg";

	public NodeWriter(Activity parentActivity) {
		this.appActivity = parentActivity;
		this.appSettings = PreferenceManager
				.getDefaultSharedPreferences(parentActivity.getBaseContext());
		this.appdb = new OrgDatabase((Context) parentActivity);
	}
	
	public void write(Node node) {
		writeNode(node.generateNoteEntry());
	}

	public void editNote(String edittype, String nodeId, String nodeTitle,
			String oldValue, String newValue) {
		EditNode enode = new EditNode(edittype, nodeId, nodeTitle, oldValue,
				newValue);
		String transformed = enode.transformEditBuffer();
		writeNode(transformed);

		// Store it in the in-memory edit list, too
		MobileOrgApplication appInst = (MobileOrgApplication) this.appActivity
				.getApplication();
		appInst.edits.add(enode);
	}

	private boolean writeNode(String message) {
		String storageMode = this.appSettings.getString("storageMode", "");
		BufferedWriter writer = new BufferedWriter(new StringWriter());

		if (storageMode.equals("internal") || storageMode == null) {
			FileOutputStream fs;
			try {
				fs = this.appActivity.openFileOutput("mobileorg.org",
						Context.MODE_APPEND);
				writer = new BufferedWriter(new OutputStreamWriter(fs));
			} catch (java.io.FileNotFoundException e) {
				Log.e(LT, "Caught FNFE trying to open mobileorg.org file");
			} 
		} else if (storageMode.equals("sdcard")) {
			try {
				File root = Environment.getExternalStorageDirectory();
				File morgDir = new File(root, "mobileorg");
				morgDir.mkdir();
				if (morgDir.canWrite()) {
					File orgFileCard = new File(morgDir, "mobileorg.org");
					FileWriter orgFWriter = new FileWriter(orgFileCard, true);
					writer = new BufferedWriter(orgFWriter);
				} else {
					Log.e(LT, "Write permission denied");
					return false;
				}
			} catch (java.io.IOException e) {
				Log.e(LT, "IO Exception initializing writer on sdcard file");
				return false;
			}
		}

		try {
			writer.write(message);
			this.appdb.addOrUpdateFile("mobileorg.org", "New Notes", "");
			writer.close();
		} catch (java.io.IOException e) {
			Log.e(LT, "IO Exception trying to write file mobileorg.org");
			return false;
		}
		return true;
	}

	public void close() {
		this.appdb.close();
	}
}