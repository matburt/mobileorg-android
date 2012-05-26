package com.matburt.mobileorg.Synchronizers;

import java.io.BufferedReader;
import java.io.IOException;
import java.security.cert.CertificateException;
import java.util.ArrayList;
import java.util.HashMap;

import javax.net.ssl.SSLHandshakeException;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.RemoteViews;

import com.matburt.mobileorg.R;
import com.matburt.mobileorg.Gui.FileDecryptionActivity;
import com.matburt.mobileorg.Gui.OutlineActivity;
import com.matburt.mobileorg.Parsing.OrgFileOld;
import com.matburt.mobileorg.Parsing.OrgFileParser;
import com.matburt.mobileorg.provider.OrgContract.Files;
import com.matburt.mobileorg.provider.OrgFile;
import com.matburt.mobileorg.provider.OrgProviderUtil;
import com.matburt.mobileorg.provider.OrgContract.Edits;

/**
 * This class implements many of the operations that need to be done on
 * synching. Instead of using it directly, create a {@link SyncManager}.
 * 
 * When implementing a new synchronizer, the methods {@link #isConfigured()},
 * {@link #putRemoteFile(String, String)} and {@link #getRemoteFile(String)} are
 * needed.
 */
public class Synchronizer {
	public static final String SYNC_UPDATE = "com.matburt.mobileorg.Synchronizer.action.SYNC_UPDATE";
	public final static String SYNC_DONE = "sync_done";
	
	public static final String CAPTURE_FILE = "mobileorg.org";

	protected Context context;
	protected SharedPreferences appSettings;
	private OrgFileParser parser;
	private ContentResolver resolver;
	private SynchronizerInterface syncher;

	public Synchronizer(Context context, SynchronizerInterface syncher) {
		this.context = context;
		this.resolver = context.getContentResolver();
		this.appSettings = PreferenceManager
				.getDefaultSharedPreferences(context.getApplicationContext());
		this.syncher = syncher;
	}

	/**
	 * Used to indicate to other systems if active synchronization is available
	 * (true) or if synchronization is implicit or non-existant (falsse)
	 */
	public boolean isEnabled() {
		return true;
	}

	public void sync(OrgFileParser parser) {
		this.parser = parser;
		if (!syncher.isConfigured()) {
			errorNotification("Sync not configured");
			return;
		}

		setupNotification();
		updateNotification(0,
				context.getString(R.string.sync_synchronizing_changes) + " "
						+ CAPTURE_FILE);
		try {
			pushCaptures();
			pull();
		} catch (IOException e) {
            finalizeNotification();
			errorNotification("Error occured during sync: "
                              + e.getLocalizedMessage());
			Log.d("MobileOrg", e.getStackTrace().toString());
			return;
		} catch (CertificateException e) {
            finalizeNotification();
			errorNotification("Certificate Error occured during sync: "
                              + e.getLocalizedMessage());
			Log.d("MobileOrg", e.getStackTrace().toString());
			return;
		} catch (Exception e) {
            finalizeNotification();
            errorNotification("Error: " + e.toString());
            Log.d("MobileOrg", e.getStackTrace().toString());
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
	public void pushCaptures() throws Exception, IOException,
			CertificateException, SSLHandshakeException {
		final String filename = CAPTURE_FILE;
		String localContents = OrgProviderUtil.fileToString(filename, resolver);
		localContents += OrgProviderUtil.editsToString(resolver);

		if (localContents.equals(""))
			return;
		String remoteContent = OrgFileOld.read(syncher.getRemoteFile(filename));
		updateNotification(10);

		if (remoteContent.indexOf("{\"error\":") == -1)
			localContents = remoteContent + "\n" + localContents;

		syncher.putRemoteFile(filename, localContents);

		resolver.delete(Edits.CONTENT_URI, null, null);
		resolver.delete(Files.buildFilenameUri(filename), null, null);
	}

	/**
	 * This method will download index.org and checksums.dat from the remote
	 * host. Using those files, it determines the other files that need updating
	 * and downloads them.
	 */
	public void pull() throws SSLHandshakeException, CertificateException, IOException, Exception {
		updateNotification(20, context.getString(R.string.downloading)
				+ " checksums.dat");
		String remoteChecksumContents = "";

		remoteChecksumContents = OrgFileOld.read(syncher.getRemoteFile("checksums.dat"));

		updateNotification(40);

		HashMap<String, String> remoteChecksums = OrgFileParser
				.getChecksums(remoteChecksumContents);
		HashMap<String, String> localChecksums = OrgProviderUtil.getFileChecksums(resolver);

		ArrayList<String> filesToGet = new ArrayList<String>();

		for (String key : remoteChecksums.keySet()) {
			if (localChecksums.containsKey(key)
					&& localChecksums.get(key).equals(remoteChecksums.get(key)))
				continue;
			filesToGet.add(key);
		}

		filesToGet.remove(CAPTURE_FILE);

		if (filesToGet.size() == 0)
			return;

		filesToGet.remove("index.org");
		updateNotification(60, context.getString(R.string.downloading)
				+ " index.org");
		String remoteIndexContents = "";

		remoteIndexContents = OrgFileOld.read(syncher.getRemoteFile("index.org"));

		OrgProviderUtil.setTodos(OrgFileParser.getTodosFromIndex(remoteIndexContents), resolver);
		OrgProviderUtil.setPriorities(OrgFileParser
				.getPrioritiesFromIndex(remoteIndexContents), resolver);
		OrgProviderUtil.setTags(OrgFileParser.getTagsFromIndex(remoteIndexContents), resolver);
		HashMap<String, String> filenameMap = OrgFileParser
				.getFilesFromIndex(remoteIndexContents);

		int i = 0;
		for (String filename : filesToGet) {
			i++;
			updateNotification(i, context.getString(R.string.downloading) + " "
					+ filename, filesToGet.size());
			Log.d("MobileOrg",
					"Getting " + filename + "/" + filenameMap.get(filename));

			OrgFile orgFile = new OrgFile(filename, filenameMap.get(filename), remoteChecksums.get(filename));
			getAndParseFile(orgFile);
		}
	}
	
	private void getAndParseFile(OrgFile orgFile)
			throws SSLHandshakeException, CertificateException, IOException,
			Exception {
		BufferedReader breader = syncher.getRemoteFile(orgFile.filename);

		if (breader == null) {
			Log.w("MobileOrg", "File does not seem to exist: " + orgFile.filename);
			return;
		}

		// TODO Generate checksum of file and compare to remoteChecksum
		
		if (orgFile.isEncrypted())
        	decryptAndParseFile(orgFile, breader);
        else {
        	parser.parse(orgFile, breader);
        }
	}
	
	private void decryptAndParseFile(OrgFile orgFile, BufferedReader reader) throws IOException {
		Intent intent = new Intent(context, FileDecryptionActivity.class);
		intent.putExtra("data", OrgFileOld.read(reader).getBytes());
		intent.putExtra("filename", orgFile.filename);
		intent.putExtra("filenameAlias", orgFile.name);
		intent.putExtra("checksum", orgFile.checksum);
		intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		context.startActivity(intent);
	}

	
	
	
	private NotificationManager notificationManager;
	private Notification notification;
	private int notifyRef = 1;

    private void errorNotification(String errorMsg) {
		this.notificationManager = (NotificationManager) context
				.getSystemService(Context.NOTIFICATION_SERVICE);
		Intent notifyIntent = new Intent(context, OutlineActivity.class);
		notifyIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);

		PendingIntent contentIntent = PendingIntent.getActivity(context, 0,
				notifyIntent, 0);

		notification = new Notification(R.drawable.icon,
				"Synchronization Failed", System.currentTimeMillis());
		
		notification.contentIntent = contentIntent;
		notification.flags = notification.flags;
		notification.contentView = new RemoteViews(context
				.getPackageName(), R.layout.sync_notification);
		
		notification.contentView.setImageViewResource(R.id.status_icon, R.drawable.icon);
        notification.contentView.setTextViewText(R.id.status_text, errorMsg);
        notification.contentView.setProgressBar(R.id.status_progress, 100, 100, false);
		notificationManager.notify(notifyRef, notification);
    }
	
	private void setupNotification() {
		this.notificationManager = (NotificationManager) context
				.getSystemService(Context.NOTIFICATION_SERVICE);
		Intent notifyIntent = new Intent(context, OutlineActivity.class);
		notifyIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP
				| Intent.FLAG_ACTIVITY_SINGLE_TOP);

		PendingIntent contentIntent = PendingIntent.getActivity(context, 0,
				notifyIntent, 0);

		notification = new Notification(R.drawable.icon,
				"Started synchronization", System.currentTimeMillis());

		notification.contentIntent = contentIntent;
		notification.flags = notification.flags
				| Notification.FLAG_ONGOING_EVENT;
		notification.contentView = new RemoteViews(context.getPackageName(),
				R.layout.sync_notification);

		notification.contentView.setImageViewResource(R.id.status_icon,
				R.drawable.icon);
		notification.contentView.setTextViewText(R.id.status_text,
				"Synchronizing...");
		notification.contentView.setProgressBar(R.id.status_progress, 100, 0,
				false);
		notificationManager.notify(notifyRef, notification);
	}

	private void updateNotification(int progress) {
		notification.contentView.setProgressBar(R.id.status_progress, 100,
				progress, false);
		notificationManager.notify(notifyRef, notification);
	}

	private void updateNotification(int progress, String message) {
		notification.contentView.setTextViewText(R.id.status_text, message);
		notification.contentView.setProgressBar(R.id.status_progress, 100,
				progress, false);
		notificationManager.notify(notifyRef, notification);
	}

	private void updateNotification(int fileNumber, String message,
			int totalFiles) {
		int partialProgress = ((40 / totalFiles) * fileNumber);
		notification.contentView.setProgressBar(R.id.status_progress, 100,
				60 + partialProgress, false);
		notification.contentView.setTextViewText(R.id.status_text, message);
		notificationManager.notify(notifyRef, notification);
	}

	private void finalizeNotification() {
		notificationManager.cancel(notifyRef);
	}
	
	public void announceSyncDone() {
		Intent intent = new Intent(Synchronizer.SYNC_UPDATE);
		intent.putExtra(SYNC_DONE, true);
		this.context.sendBroadcast(intent);
	}

	public void close() {
		syncher.postSynchronize();
	}
}
