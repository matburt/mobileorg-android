package com.matburt.mobileorg.Synchronizers;

import java.io.BufferedReader;
import java.io.IOException;
import java.security.cert.CertificateException;
import java.util.ArrayList;
import java.util.HashMap;

import javax.net.ssl.SSLHandshakeException;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.matburt.mobileorg.R;
import com.matburt.mobileorg.Gui.FileDecryptionActivity;
import com.matburt.mobileorg.OrgData.OrgContract.Edits;
import com.matburt.mobileorg.OrgData.OrgContract.Files;
import com.matburt.mobileorg.OrgData.OrgEdit;
import com.matburt.mobileorg.OrgData.OrgFile;
import com.matburt.mobileorg.OrgData.OrgFileParser;
import com.matburt.mobileorg.OrgData.OrgProviderUtil;
import com.matburt.mobileorg.Services.CalendarSyncService;
import com.matburt.mobileorg.util.FileUtils;

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
	public static final String INDEX_FILE = "index.org";

	private Context context;
	private ContentResolver resolver;
	private SynchronizerInterface syncher;
	private SynchronizerNotification notify;

	public Synchronizer(Context context, SynchronizerInterface syncher, SynchronizerNotification notify) {
		this.context = context;
		this.resolver = context.getContentResolver();
		this.syncher = syncher;
		this.notify = notify;
	}

	/**
	 * Used to indicate to other systems if active synchronization is available
	 * (true) or if synchronization is implicit or non-existant (falsse)
	 */
	public boolean isEnabled() {
		return true;
	}

	public void sync(OrgFileParser parser, CalendarSyncService calendarSyncService) {
		sync(parser);
		calendarSyncService.syncFiles();
	}
	
	public void sync(OrgFileParser parser) {
		if (!syncher.isConfigured()) {
			notify.errorNotification("Sync not configured");
			return;
		}

		notify.setupNotification();
		notify.updateNotification(0,
				context.getString(R.string.sync_synchronizing_changes) + " "
						+ CAPTURE_FILE);
		try {
			pushCaptures();
			pull(parser);
		} catch (IOException e) {
			notify.finalizeNotification();
			notify.errorNotification("Error occured during sync: "
                              + e.getLocalizedMessage());
			Log.d("MobileOrg", e.getStackTrace().toString());
			return;
		} catch (CertificateException e) {
			notify.finalizeNotification();
			notify.errorNotification("Certificate Error occured during sync: "
                              + e.getLocalizedMessage());
			Log.d("MobileOrg", e.getStackTrace().toString());
			return;
		} catch (Exception e) {
			notify.finalizeNotification();
			notify.errorNotification("Error: " + e.toString());
            e.printStackTrace();
            Log.d("MobileOrg", e.toString());
            return;
        }
		notify.finalizeNotification();
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
		
		String localContents = "";
		
		try {
			OrgFile file = new OrgFile(filename, resolver);
			localContents += file.toString(resolver);
		} catch (IllegalArgumentException e) {}
		
		localContents += OrgEdit.editsToString(resolver);

		if (localContents.equals(""))
			return;
		String remoteContent = FileUtils.read(syncher.getRemoteFile(filename));
		notify.updateNotification(10);

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
	public void pull(OrgFileParser parser) throws SSLHandshakeException, CertificateException, IOException, Exception {
		notify.updateNotification(20, context.getString(R.string.downloading)
				+ " checksums.dat");
		String remoteChecksumContents = "";

		remoteChecksumContents = FileUtils.read(syncher.getRemoteFile("checksums.dat"));

		notify.updateNotification(40);

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

		filesToGet.remove(INDEX_FILE);
		notify.updateNotification(60, context.getString(R.string.downloading)
				+ " " + INDEX_FILE);
		String remoteIndexContents = "";

		remoteIndexContents = FileUtils.read(syncher.getRemoteFile(INDEX_FILE));

		OrgProviderUtil.setTodos(OrgFileParser.getTodosFromIndex(remoteIndexContents), resolver);
		OrgProviderUtil.setPriorities(OrgFileParser
				.getPrioritiesFromIndex(remoteIndexContents), resolver);
		OrgProviderUtil.setTags(OrgFileParser.getTagsFromIndex(remoteIndexContents), resolver);
		HashMap<String, String> filenameMap = OrgFileParser
				.getFilesFromIndex(remoteIndexContents);

		int i = 0;
		for (String filename : filesToGet) {
			i++;
			notify.updateNotification(i, context.getString(R.string.downloading) + " "
					+ filename, filesToGet.size());
			Log.d("MobileOrg",
					"Getting " + filename + "/" + filenameMap.get(filename));

			OrgFile orgFile = new OrgFile(filename, filenameMap.get(filename), remoteChecksums.get(filename));
			getAndParseFile(orgFile, parser);
		}
	}
	
	private void getAndParseFile(OrgFile orgFile, OrgFileParser parser)
			throws SSLHandshakeException, CertificateException, IOException,
			Exception {
		BufferedReader breader = syncher.getRemoteFile(orgFile.filename);

		if (breader == null) {
			Log.w("MobileOrg", "File does not seem to exist: " + orgFile.filename);
			return;
		}

		// TODO Generate checksum of file and compare to remoteChecksum
		
		try {
			new OrgFile(orgFile.filename, resolver).removeFile();
		} catch (IllegalArgumentException e) { /* file did not exist */ }
		
		if (orgFile.isEncrypted())
        	decryptAndParseFile(orgFile, breader);
        else {
        	parser.parse(orgFile, breader);
        }
	}
	
	private void decryptAndParseFile(OrgFile orgFile, BufferedReader reader) throws IOException {
		Intent intent = new Intent(context, FileDecryptionActivity.class);
		intent.putExtra("data", FileUtils.read(reader).getBytes());
		intent.putExtra("filename", orgFile.filename);
		intent.putExtra("filenameAlias", orgFile.name);
		intent.putExtra("checksum", orgFile.checksum);
		intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		context.startActivity(intent);
	}

	private void announceSyncDone() {
		Intent intent = new Intent(Synchronizer.SYNC_UPDATE);
		intent.putExtra(Synchronizer.SYNC_DONE, true);
		this.context.sendBroadcast(intent);
	}
	
	public void close() {
		syncher.postSynchronize();
	}
}
