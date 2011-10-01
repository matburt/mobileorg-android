package com.matburt.mobileorg.Synchronizers;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.preference.PreferenceManager;
import android.text.TextUtils;

import com.matburt.mobileorg.Parsing.OrgDatabase;
import com.matburt.mobileorg.Parsing.OrgFile;

abstract public class Synchronizer {
	public OrgDatabase appdb = null;
	public SharedPreferences appSettings = null;
	public Context context = null;
	public static final String LT = "MobileOrg";
	public Resources r;

	public Synchronizer(Context context) {
        this.context = context;
        this.r = this.context.getResources();
        this.appdb = new OrgDatabase((Context)context);
        this.appSettings = PreferenceManager.getDefaultSharedPreferences(
                                   context.getApplicationContext());
	}

	public abstract boolean isConfigured();
	public abstract void pull() throws IOException;
	public abstract void push() throws IOException;
	protected abstract BufferedReader getFile(String filePath) throws IOException;
	
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


	protected void updateFiles (String remoteIndex, String actualPath) throws IOException {
		String masterStr = OrgFile.fileToString(getFile(remoteIndex));
        
        ArrayList<HashMap<String, Boolean>> todoLists = getTodos(masterStr);
        this.appdb.setTodoList(todoLists);

        ArrayList<ArrayList<String>> priorityLists = getPriorities(masterStr);
        this.appdb.setPriorityList(priorityLists);
        
        String urlActual = actualPath;

		// Get checksums file
		masterStr = OrgFile.fileToString(urlActual + "checksums.dat", context);
		HashMap<String, String> newChecksums = getChecksums(masterStr);
		HashMap<String, String> oldChecksums = this.appdb.getChecksums();


		
		HashMap<String, String> masterList = getOrgFilesFromMaster(masterStr);
		
		// Get other org files
		for (String key : masterList.keySet()) {
			if (oldChecksums.containsKey(key) && newChecksums.containsKey(key)
					&& oldChecksums.get(key).equals(newChecksums.get(key)))
				continue;
			OrgFile.fetchAndSaveOrgFile(getFile(urlActual + masterList.get(key)),
					masterList.get(key), context);
			this.appdb.addOrUpdateFile(masterList.get(key), key,
					newChecksums.get(key));
		}
	}
	
	private static HashMap<String, String> getOrgFilesFromMaster(String master) {
		Pattern getOrgFiles = Pattern.compile("\\[file:(.*?)\\]\\[(.*?)\\]\\]");
		Matcher m = getOrgFiles.matcher(master);
		HashMap<String, String> allOrgFiles = new HashMap<String, String>();
		while (m.find()) {
			allOrgFiles.put(m.group(2), m.group(1));
		}

		return allOrgFiles;
	}

	private static HashMap<String, String> getChecksums(String master) {
		HashMap<String, String> chksums = new HashMap<String, String>();
		for (String eachLine : master.split("[\\n\\r]+")) {
			if (TextUtils.isEmpty(eachLine))
				continue;
			String[] chksTuple = eachLine.split("\\s+");
			chksums.put(chksTuple[1], chksTuple[0]);
		}
		return chksums;
	}

	private static ArrayList<HashMap<String, Boolean>> getTodos(String master) {
		Pattern getTodos = Pattern
				.compile("#\\+TODO:\\s+([\\s\\w-]*)(\\| ([\\s\\w-]*))*");
		Matcher m = getTodos.matcher(master);
		ArrayList<HashMap<String, Boolean>> todoList = new ArrayList<HashMap<String, Boolean>>();
		while (m.find()) {
			String lastTodo = "";
			HashMap<String, Boolean> holding = new HashMap<String, Boolean>();
			Boolean isDone = false;
			for (int idx = 1; idx <= m.groupCount(); idx++) {
				if (m.group(idx) != null && m.group(idx).length() > 0) {
					if (m.group(idx).indexOf("|") != -1) {
						isDone = true;
						continue;
					}
					String[] grouping = m.group(idx).split("\\s+");
					for (int jdx = 0; jdx < grouping.length; jdx++) {
						lastTodo = grouping[jdx].trim();
						holding.put(grouping[jdx].trim(), isDone);
					}
				}
			}
			if (!isDone) {
				holding.put(lastTodo, true);
			}
			todoList.add(holding);
		}
		return todoList;
	}

	private static ArrayList<ArrayList<String>> getPriorities(String master) {
		Pattern getPriorities = Pattern
				.compile("#\\+ALLPRIORITIES:\\s+([A-Z\\s]*)");
		Matcher t = getPriorities.matcher(master);
		
		ArrayList<ArrayList<String>> priorityList = new ArrayList<ArrayList<String>>();
		
		while (t.find()) {
			ArrayList<String> holding = new ArrayList<String>();
			if (t.group(1) != null && t.group(1).length() > 0) {
				String[] grouping = t.group(1).split("\\s+");
				for (int jdx = 0; jdx < grouping.length; jdx++) {
					holding.add(grouping[jdx].trim());
				}
			}
			priorityList.add(holding);
		}
		return priorityList;
	}
}
