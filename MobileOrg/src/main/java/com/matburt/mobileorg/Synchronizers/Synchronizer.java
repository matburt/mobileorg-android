package com.matburt.mobileorg.Synchronizers;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.text.TextUtils;
import android.util.Log;

import com.matburt.mobileorg.Gui.FileDecryptionActivity;
import com.matburt.mobileorg.Gui.SynchronizerNotificationCompat;
import com.matburt.mobileorg.OrgData.OrgContract.Edits;
import com.matburt.mobileorg.OrgData.OrgContract.Files;
import com.matburt.mobileorg.OrgData.OrgEdit;
import com.matburt.mobileorg.OrgData.OrgFile;
import com.matburt.mobileorg.OrgData.OrgFileParser;
import com.matburt.mobileorg.OrgData.OrgProviderUtils;
import com.matburt.mobileorg.R;
import com.matburt.mobileorg.util.FileUtils;
import com.matburt.mobileorg.util.OrgFileNotFoundException;
import com.matburt.mobileorg.util.OrgUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.security.cert.CertificateException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

import javax.net.ssl.SSLHandshakeException;

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
	public static final String SYNC_DONE = "sync_done";
	public static final String SYNC_START = "sync_start";
	public static final String SYNC_PROGRESS_UPDATE = "progress_update";
	public static final String SYNC_SHOW_TOAST = "showToast";
	
	public static final String CAPTURE_FILE = "mobileorg.org";
	public static final String INDEX_FILE = "index.org";

	private Context context;
	private ContentResolver resolver;
	private SynchronizerInterface syncher;
	private SynchronizerNotificationCompat notify;

	public Synchronizer(Context context, SynchronizerInterface syncher, SynchronizerNotificationCompat notify) {
		this.context = context;
		this.resolver = context.getContentResolver();
		this.syncher = syncher;
		this.notify = notify;
	}

 	public boolean isEnabled() {
		return true;
	}
	
 	/**
 	 * @return List of files that where changed.
 	 */
	public ArrayList<String> runSynchronizer(OrgFileParser parser) {
		if (!syncher.isConfigured()) {
			notify.errorNotification("Sync not configured");
			return new ArrayList<String>();
		}
		
		if (!syncher.isConnectable()) {
			notify.errorNotification("No network connection available");
			return new ArrayList<String>();
		}
		
		try {
			announceStartSync();
			ArrayList<String> changedFiles = pull(parser);
			pushCaptures();
			announceSyncDone();
			return changedFiles;
		} catch (Exception e) {
			showErrorNotification(e);
            Log.e("Synchronizer", "Error synchronizing", e);
            OrgUtils.announceSyncDone(context);
			return new ArrayList<String>();
		}
	}

	/**
	 * This method will fetch the local and the remote version of the capture
	 * file combine their content. This combined version is transfered to the
	 * remote.
	 */
	public void pushCaptures() throws IOException,
			CertificateException, SSLHandshakeException {
		final String filename = CAPTURE_FILE;
		
		notify.updateNotification("Uploading captures");
		
		String localContents = "";
		
		try {
			OrgFile file = new OrgFile(filename, resolver);
			localContents += file.toString(resolver);
		} catch (OrgFileNotFoundException e) {}
		
		localContents += OrgEdit.editsToString(resolver);

		if (localContents.equals(""))
			return;
		String remoteContent = FileUtils.read(syncher.getRemoteFile(filename));

		if (remoteContent.indexOf("{\"error\":") == -1)
			localContents = remoteContent + "\n" + localContents;

		syncher.putRemoteFile(filename, localContents);
		
		try {
			new OrgFile(filename, resolver).removeFile(resolver);
		} catch (OrgFileNotFoundException e) {}

		resolver.delete(Edits.CONTENT_URI, null, null);
		resolver.delete(Files.buildFilenameUri(filename), null, null);
	}

	/**
	 * This method will download index.org and checksums.dat from the remote
	 * host. Using those files, it determines the other files that need updating
	 * and downloads them.
	 * @return 
	 */
	public ArrayList<String> pull(OrgFileParser parser) throws SSLHandshakeException, CertificateException, IOException {
		HashMap<String,String> remoteChecksums = getAndParseChecksumFile();
		ArrayList<String> changedFiles = getFilesThatChangedRemotely(remoteChecksums);
		
		if(changedFiles.size() == 0)
			return changedFiles;
		
		changedFiles.remove(INDEX_FILE);
		announceProgressDownload(INDEX_FILE, 0, changedFiles.size() + 2);
		HashMap<String,String> filenameMap = getAndParseIndexFile();
		
		Collections.sort(changedFiles, new OrgUtils.SortIgnoreCase());
		
		pull(parser, changedFiles, filenameMap, remoteChecksums);
		announceProgressDownload("", changedFiles.size() + 1, changedFiles.size() + 2);
		
		return changedFiles;
	}
	
	private void pull(OrgFileParser parser, ArrayList<String> filesToGet,
			HashMap<String, String> filenameMap,
			HashMap<String, String> remoteChecksums)
			throws SSLHandshakeException, CertificateException, IOException {
		final int totalNumberOfFiles = filesToGet.size() + 2;
		int fileIndex = 1;
		for (String filename : filesToGet) {
			announceProgressDownload(filename, fileIndex++, totalNumberOfFiles);
			Log.d("MobileOrg", context.getString(R.string.downloading) +
					" " + filename + "/" + filenameMap.get(filename));

			OrgFile orgFile = new OrgFile(filename, filenameMap.get(filename),
					remoteChecksums.get(filename));
			getAndParseFile(orgFile, parser);
		}
	}

	private HashMap<String, String> getAndParseIndexFile() throws SSLHandshakeException, CertificateException, IOException {
		String remoteIndexContents = FileUtils.read(syncher.getRemoteFile(INDEX_FILE));
		OrgProviderUtils.setTodos(
				OrgFileParser.getTodosFromIndex(remoteIndexContents), resolver);
		OrgProviderUtils.setPriorities(
				OrgFileParser.getPrioritiesFromIndex(remoteIndexContents),
				resolver);
		OrgProviderUtils.setTags(
				OrgFileParser.getTagsFromIndex(remoteIndexContents), resolver);
		HashMap<String, String> filenameMap = OrgFileParser
				.getFilesFromIndex(remoteIndexContents);
		return filenameMap;
	}
	
	private HashMap<String, String> getAndParseChecksumFile() throws SSLHandshakeException, CertificateException, IOException {
		String remoteChecksumContents = FileUtils.read(syncher.getRemoteFile("checksums.dat"));

		HashMap<String, String> remoteChecksums = OrgFileParser
				.getChecksums(remoteChecksumContents);
		return remoteChecksums;
	}
	
	private ArrayList<String> getFilesThatChangedRemotely(HashMap<String, String> remoteChecksums) {
		HashMap<String, String> localChecksums = OrgProviderUtils.getFileChecksums(resolver);
		
		ArrayList<String> filesToGet = new ArrayList<String>();

		for (String key : remoteChecksums.keySet()) {
			if (localChecksums.containsKey(key)
					&& localChecksums.get(key).equals(remoteChecksums.get(key)))
				continue;
			filesToGet.add(key);
		}
		
		filesToGet.remove(CAPTURE_FILE);
		
		return filesToGet;
	}
	
	private void getAndParseFile(OrgFile orgFile, OrgFileParser parser)
			throws CertificateException, IOException {
		
		BufferedReader breader = syncher.getRemoteFile(orgFile.filename);

		// TODO Generate checksum of file and compare to remoteChecksum
		
		try {
			new OrgFile(orgFile.filename, resolver).removeFile(resolver);
		} catch (OrgFileNotFoundException e) { /* file did not exist */ }
		
		if (orgFile.isEncrypted())
        	decryptAndParseFile(orgFile, breader);
        else {
        	parser.parse(orgFile, breader, this.context);
        }
	}
	
	private void decryptAndParseFile(OrgFile orgFile, BufferedReader reader) {
		try {
			Intent intent = new Intent(context, FileDecryptionActivity.class);
			intent.putExtra("data", FileUtils.read(reader).getBytes());
			intent.putExtra("filename", orgFile.filename);
			intent.putExtra("filenameAlias", orgFile.name);
			intent.putExtra("checksum", orgFile.checksum);
			intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			context.startActivity(intent);	
		} catch(IOException e) {}
	}
	

	private void announceStartSync() {
		notify.setupNotification();
		OrgUtils.announceSyncStart(context);
	}
	
	private void announceProgressUpdate(int progress, String message) {
		if(message != null && TextUtils.isEmpty(message) == false)
			notify.updateNotification(progress, message);
		else
			notify.updateNotification(progress);
		OrgUtils.announceSyncUpdateProgress(progress, context);
	}
	
	private void announceProgressDownload(String filename, int fileIndex, int totalFiles) {
		int progress = 0;
		if (totalFiles > 0)
			progress = (100 / totalFiles) * fileIndex;
		String message = context.getString(R.string.downloading) + " " + filename;
		announceProgressUpdate(progress, message);
	}
	
	private void showErrorNotification(Exception exception) {
		notify.finalizeNotification();
		
		String errorMessage = "";
        if (CertificateException.class.isInstance(exception)) {
			errorMessage = "Certificate Error occured during sync: "
					+ exception.getLocalizedMessage();
		} else {
			errorMessage = "Error: " + exception.getLocalizedMessage();
		}
		
		notify.errorNotification(errorMessage);
	}
	
	private void announceSyncDone() {
		announceProgressUpdate(100, "Done synchronizing");
		notify.finalizeNotification();
		OrgUtils.announceSyncDone(context);
	}
	
	public void close() {
		syncher.postSynchronize();
	}
}
