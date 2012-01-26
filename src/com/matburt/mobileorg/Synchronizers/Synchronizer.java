package com.matburt.mobileorg.Synchronizers;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.RemoteViews;

import com.matburt.mobileorg.R;
import com.matburt.mobileorg.Gui.OutlineActivity;
import com.matburt.mobileorg.Parsing.MobileOrgApplication;
import com.matburt.mobileorg.Parsing.OrgDatabase;
import com.matburt.mobileorg.Parsing.OrgFile;
import com.matburt.mobileorg.Parsing.OrgFileParser;

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

	protected Context context;
	protected OrgDatabase appdb;
	protected SharedPreferences appSettings;
	protected Resources r;

	Synchronizer(Context context, MobileOrgApplication appInst) {
        this.context = context;
        this.r = this.context.getResources();
        this.appSettings = PreferenceManager.getDefaultSharedPreferences(
                                   context.getApplicationContext());
        this.appdb = appInst.getDB();
	}

	public void sync() {
		if (isConfigured() == false) {
			displayErrorNotification("Sync not configured");
			return;
		}

		setupNotification();
		updateNotification(0, "Synchronizing changes " + OrgFile.CAPTURE_FILE);
		try {
			pushCaptures();
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
	 * This method will fetch the local and the remote version of the capture
	 * file combine their content. This combined version is transfered to the
	 * remote.
	 */
	private void pushCaptures() throws IOException {
		final String filename = OrgFile.CAPTURE_FILE;
    	String localContents = this.appdb.fileToString(filename);
    	localContents += this.appdb.editsToString();

        if(localContents.equals(""))
        	return;
    	
    	String remoteContent = OrgFile.read(getRemoteFile(filename));
		updateNotification(10);

        if (remoteContent.indexOf("{\"error\":") == -1)
            localContents = remoteContent + "\n" + localContents;
		
		putRemoteFile(filename, localContents);
		
		this.appdb.clearEdits();
		this.appdb.removeFile(filename);
	}
	
	/**
	 * This method will download index.org and checksums.dat from the remote
	 * host. Using those files, it determines the other files that need updating
	 * and downloads them.
	 */
	private void pull() throws IOException {
		updateNotification(20, "Downloading checksum file");
        String remoteChecksumContents = OrgFile.read(getRemoteFile("checksums.dat"));
		updateNotification(40);

		HashMap<String, String> remoteChecksums = OrgFileParser.getChecksums(remoteChecksumContents);
		HashMap<String, String> localChecksums = this.appdb.getFileChecksums();
		
		ArrayList<String> filesToGet = new ArrayList<String>();

        for (String key : remoteChecksums.keySet()) {     	
            if (localChecksums.containsKey(key) &&
                localChecksums.get(key).equals(remoteChecksums.get(key)))
                continue;
            filesToGet.add(key);
        }

        filesToGet.remove("mobileorg.org");

        if(filesToGet.size() == 0)
        	return;
        
		filesToGet.remove("index.org");
		updateNotification(60, "Downloading index file");
		String remoteIndexContents = OrgFile.read(getRemoteFile("index.org"));
				
        this.appdb.setTodos(OrgFileParser.getTodosFromIndex(remoteIndexContents));
        this.appdb.setPriorities(OrgFileParser.getPrioritiesFromIndex(remoteIndexContents));
		HashMap<String, String> filenameMap = OrgFileParser.getFilesFromIndex(remoteIndexContents);
		this.appdb.addOrUpdateFile("index.org", filenameMap.get("index.org"),
				remoteChecksums.get("index.org"), false);
        
        OrgFileParser parser = new OrgFileParser(this.appdb);
        int i = 0;
        for(String filename: filesToGet) {        	
        	i++;
			updateNotification(i, "Downloading " + filename, filesToGet.size());
			Log.d("MobileOrg", "Getting " + filename + "/" + filenameMap.get(filename));
			this.appdb.removeFile(filename);
			long file_id = this.appdb.addOrUpdateFile(filename, filenameMap.get(filename), remoteChecksums.get(filename), true);
            BufferedReader rfile = getRemoteFile(filename);
            if (rfile == null) {
                Log.w("MobileOrg", "File does not seem to exist: " + filename);
                continue;
            }
			// TODO Generate checksum of file and compare to remoteChecksum
            parser.parse(filename, rfile, this.appdb, file_id);
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
		
	public void close() {
		this.postSynchronize();
	}
}
