package com.matburt.mobileorg.Synchronizers;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.preference.PreferenceManager;
import android.text.TextUtils;

import com.matburt.mobileorg.Parsing.MobileOrgApplication;
import com.matburt.mobileorg.Parsing.OrgDatabase;
import com.matburt.mobileorg.Parsing.OrgFile;
import com.matburt.mobileorg.Services.SyncService;

/**
 * This class implements many of the operations that need to be done on
 * synching. Instead of using it directly, create a {@link SyncManager}.
 * 
 * When implementing a new synchronizer, the methods {@link #isConfigured()},
 * {@link #putRemoteFile(String, String)} and {@link #getRemoteFile(String)} are
 * needed.
 */
abstract public class Synchronizer {
	public final static String SYNC_PROGRESS = "sync_progress";
	public final static String SYNC_MESSAGE = "sync_message";
	public final static String SYNC_FILES = "sync_files";
	public final static String SYNC_FILES_TOTAL = "sync_total";
	public final static String SYNC_DONE = "sync_done";
	
	/**
	 * Called before running the synchronizer to ensure that it's configuration
	 * is in a valid state.
	 */
	public abstract boolean isConfigured();
	
	/**
	 * Replaces the file on the remote end with the given content.
	 * @param filename Name of the file, without path
	 * @param contents Content of the new file
	 */
	protected abstract void putRemoteFile(String filename, String contents) throws IOException;

	/**
	 * Returns a BufferedReader to the remote file.
	 * @param filename Name of the file, without path
	 */
	protected abstract BufferedReader getRemoteFile(String filename) throws IOException;
	
	/** 
	 * Use this to disconnect from any services and cleanup.
	 */
	protected abstract void postSynchronize();

	protected OrgDatabase appdb;
	protected SharedPreferences appSettings;
	protected Context context;
	protected Resources r;
	private MobileOrgApplication appInst;

	Synchronizer(Context context, MobileOrgApplication appInst) {
        this.context = context;
        this.r = this.context.getResources();
        this.appdb = new OrgDatabase((Context)context);
        this.appSettings = PreferenceManager.getDefaultSharedPreferences(
                                   context.getApplicationContext());
        this.appInst = appInst;
        
        announceSyncMessage("Connecting to service");
	}

	public void sync() throws IOException {
		announceSyncMessage("Uploading " + OrgFile.CAPTURE_FILE);
		push(OrgFile.CAPTURE_FILE);
		announceSyncProgress(20);
		pull();
		announceSyncDone();
	}

	/**
	 * This method will fetch the local and the remote version of a file and
	 * combine their content. This combined version is transfered to the remote.
	 */
	protected void push(String filename) throws IOException {
    	OrgFile orgFile = new OrgFile(filename, context);
    	String localContents = orgFile.read();

    	String remoteContent = OrgFile.read(getRemoteFile(filename));
    	announceSyncProgress(10);
    	
        if(localContents.equals(""))
        	return;
        
        if (remoteContent.indexOf("{\"error\":") == -1)
            localContents = remoteContent + "\n" + localContents;
		
		putRemoteFile(filename, localContents);
		
		appInst.removeFile(filename);
	}
	
	/**
	 * This method will download index.org and checksums.dat from the remote
	 * host. Using those files, it determines the other files that need updating
	 * and downloads them.
	 */
	protected void pull() throws IOException {
		announceSyncMessage("Downloading index file");
		String remoteIndexContents = OrgFile.read(getRemoteFile("index.org"));
		announceSyncProgress(40);
        
        ArrayList<HashMap<String, Boolean>> todoLists = getTodos(remoteIndexContents);
        this.appdb.setTodoList(todoLists);

        ArrayList<ArrayList<String>> priorityLists = getPriorities(remoteIndexContents);
        this.appdb.setPriorityList(priorityLists);

        announceSyncMessage("Downloading checksum file");
        String remoteChecksumContents = OrgFile.read(getRemoteFile("checksums.dat"));
        announceSyncProgress(60);

		HashMap<String, String> remoteChecksums = getChecksums(remoteChecksumContents);
		HashMap<String, String> localChecksums = this.appdb.getChecksums();
		
		HashMap<String, String> fileChecksumMap = getOrgFilesFromMaster(remoteIndexContents);
		
		ArrayList<String> filesToGet = new ArrayList<String>();
		
        for (String key : fileChecksumMap.keySet()) {     	
            if (localChecksums.containsKey(key) &&
                remoteChecksums.containsKey(key) &&
                localChecksums.get(key).equals(remoteChecksums.get(key)))
                continue;
            
            filesToGet.add(key);
        }
        
        int i = 0;
        for(String key: filesToGet) {
        	String filename = fileChecksumMap.get(key);
        	
        	i++;
			announceSyncFile("Downloading " + filename, i, filesToGet.size());

            OrgFile orgfile = new OrgFile(filename, context);
            orgfile.fetch(getRemoteFile(filename));

			appInst.addOrUpdateFile(filename, key, remoteChecksums.get(key));
        }
	}
	
	
	private void announceSyncProgress(int progress) {
		Intent intent = new Intent(SyncService.SYNC_UPDATE);
		intent.putExtra(SYNC_PROGRESS, progress);
		this.context.sendBroadcast(intent);
	}

	private void announceSyncFile(String message, int number, int total) {
		Intent intent = new Intent(SyncService.SYNC_UPDATE);
		intent.putExtra(SYNC_MESSAGE, message);
		intent.putExtra(SYNC_FILES, number);
		intent.putExtra(SYNC_FILES_TOTAL, total);
		this.context.sendBroadcast(intent);
	}
	
	private void announceSyncMessage(String message) {
		Intent intent = new Intent(SyncService.SYNC_UPDATE);
		intent.putExtra(SYNC_MESSAGE, message);
		this.context.sendBroadcast(intent);
	}

	private void announceSyncDone() {
		Intent intent = new Intent(SyncService.SYNC_UPDATE);
		intent.putExtra(SYNC_DONE, true);
		this.context.sendBroadcast(intent);
	}
	
	private HashMap<String, String> getOrgFilesFromMaster(String master) {
		Pattern getOrgFiles = Pattern.compile("\\[file:(.*?)\\]\\[(.*?)\\]\\]");
		Matcher m = getOrgFiles.matcher(master);
		HashMap<String, String> allOrgFiles = new HashMap<String, String>();
		while (m.find()) {
			allOrgFiles.put(m.group(2), m.group(1));
		}

		return allOrgFiles;
	}

	private HashMap<String, String> getChecksums(String master) {
		HashMap<String, String> chksums = new HashMap<String, String>();
		for (String eachLine : master.split("[\\n\\r]+")) {
			if (TextUtils.isEmpty(eachLine))
				continue;
			String[] chksTuple = eachLine.split("\\s+");
			chksums.put(chksTuple[1], chksTuple[0]);
		}
		return chksums;
	}

	private ArrayList<HashMap<String, Boolean>> getTodos(String master) {
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
					for (String group : grouping) {
						lastTodo = group.trim();
						holding.put(group.trim(), isDone);
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

	private ArrayList<ArrayList<String>> getPriorities(String master) {
		Pattern getPriorities = Pattern
				.compile("#\\+ALLPRIORITIES:\\s+([A-Z\\s]*)");
		Matcher t = getPriorities.matcher(master);
		
		ArrayList<ArrayList<String>> priorityList = new ArrayList<ArrayList<String>>();
		
		while (t.find()) {
			ArrayList<String> holding = new ArrayList<String>();
			if (t.group(1) != null && t.group(1).length() > 0) {
				String[] grouping = t.group(1).split("\\s+");
				for (String group : grouping) {
					holding.add(group.trim());
				}
			}
			priorityList.add(holding);
		}
		return priorityList;
	}
		
	public void close() {
		if (this.appdb != null)
			this.appdb.close();
		this.postSynchronize();
	}
}
