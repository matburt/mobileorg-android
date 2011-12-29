package com.matburt.mobileorg.Synchronizers;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.widget.RemoteViews;

import com.matburt.mobileorg.R;
import com.matburt.mobileorg.Gui.OutlineActivity;
import com.matburt.mobileorg.Parsing.MobileOrgApplication;
import com.matburt.mobileorg.Parsing.OrgDatabase;
import com.matburt.mobileorg.Parsing.OrgFile;

/**
 * This class implements many of the operations that need to be done on
 * synching. Instead of using it directly, create a {@link SyncManager}.
 * 
 * When implementing a new synchronizer, the methods {@link #isConfigured()},
 * {@link #putRemoteFile(String, String)} and {@link #getRemoteFile(String)} are
 * needed.
 */
abstract public class Synchronizer {
	public static final String SYNC_UPDATE = "com.matburt.mobileorg.Synchronizer.action.SYNC_UPDATE";
	public final static String SYNC_DONE = "sync_done";
	
	/**
	 * Called before running the synchronizer to ensure that it's configuration
	 * is in a valid state.
	 */
	protected abstract boolean isConfigured();
	
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
        this.appdb = new OrgDatabase(context);
        this.appSettings = PreferenceManager.getDefaultSharedPreferences(
                                   context.getApplicationContext());
        this.appInst = appInst;        
	}

	public void sync() {
		if (isConfigured() == false) {
			displayErrorNotification("Sync not configured");
			return;
		}

		setupNotification();
		updateNotification(0, "Uploading " + OrgFile.CAPTURE_FILE);
		try {
			push(OrgFile.CAPTURE_FILE);
			pull();
		} catch (IOException e) {
			displayErrorNotification("Error occured during sync: "
					+ e.getLocalizedMessage());
			return;
		}
		finalizeNotification();
		announceSyncDone();
	}

	/**
	 * This method will fetch the local and the remote version of a file and
	 * combine their content. This combined version is transfered to the remote.
	 */
	private void push(String filename) throws IOException {
    	OrgFile orgFile = new OrgFile(filename, context);
    	String localContents = orgFile.read();

    	String remoteContent = OrgFile.read(getRemoteFile(filename));
		updateNotification(10);
    	
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
	private void pull() throws IOException {
		updateNotification(20, "Downloading index file");
		String remoteIndexContents = OrgFile.read(getRemoteFile("index.org"));
		
        ArrayList<HashMap<String, Boolean>> todoLists = getTodos(remoteIndexContents);
        this.appdb.setTodoList(todoLists);

        ArrayList<ArrayList<String>> priorityLists = getPriorities(remoteIndexContents);
        this.appdb.setPriorityList(priorityLists);

		updateNotification(40, "Downloading checksum file");
        String remoteChecksumContents = OrgFile.read(getRemoteFile("checksums.dat"));
		updateNotification(60);

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
			updateNotification(i, "Downloading " + filename, filesToGet.size());

            OrgFile orgfile = new OrgFile(filename, context);
            orgfile.fetch(getRemoteFile(filename));

            // TODO Generate checksum of file and compare to remoteChecksum
			appInst.addOrUpdateFile(filename, key, remoteChecksums.get(key));
        }
	}
	
	private NotificationManager notificationManager;
	private Notification notification;
	private int notifyRef = 1;
	
	private void setupNotification() {
		this.notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
		Intent notifyIntent = new Intent(context, OutlineActivity.class);
		notifyIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);

		PendingIntent contentIntent = PendingIntent.getActivity(context, 0,
				notifyIntent, 0);

		notification = new Notification(R.drawable.icon,
				"Started synchronization", System.currentTimeMillis());
		
		notification.contentIntent = contentIntent;
		notification.flags = notification.flags | Notification.FLAG_ONGOING_EVENT;
		notification.contentView = new RemoteViews(context
				.getPackageName(), R.layout.sync_notification);
		
		notification.contentView.setImageViewResource(R.id.status_icon, R.drawable.icon);
        notification.contentView.setTextViewText(R.id.status_text, "Synchronizing...");
        notification.contentView.setProgressBar(R.id.status_progress, 100, 0, false);
		notificationManager.notify(notifyRef, notification);
	}

	private void updateNotification(int progress) {
		notification.contentView.setProgressBar(R.id.status_progress, 100, progress, false);
        notificationManager.notify(notifyRef, notification);
	}

	private void updateNotification(int progress, String message) {		
        notification.contentView.setTextViewText(R.id.status_text, message);
		notification.contentView.setProgressBar(R.id.status_progress, 100, progress, false);
        notificationManager.notify(notifyRef, notification);
	}

	private void updateNotification(int fileNumber, String message, int totalFiles) {
		int partialProgress = ((40 / totalFiles) * fileNumber);
		notification.contentView.setProgressBar(R.id.status_progress, 100, 60 + partialProgress, false);
        notification.contentView.setTextViewText(R.id.status_text, message);
        notificationManager.notify(notifyRef, notification);
	}
	
	private void finalizeNotification() {
		notificationManager.cancel(notifyRef);
	}
	
	private void displayErrorNotification(String message) {
        notification.contentView.setTextViewText(R.id.status_text, message);
        // TODO Cancel old and create new notification
        notificationManager.notify(notifyRef, notification);
	}

	private void announceSyncDone() {
		Intent intent = new Intent(Synchronizer.SYNC_UPDATE);
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
			if(chksTuple.length >= 2)
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
