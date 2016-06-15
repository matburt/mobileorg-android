package com.matburt.mobileorg2.Synchronizers;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.text.TextUtils;
import android.util.Log;

import com.matburt.mobileorg2.Gui.SynchronizerNotificationCompat;
import com.matburt.mobileorg2.OrgData.OrgContract.Edits;
import com.matburt.mobileorg2.OrgData.OrgContract.Files;
import com.matburt.mobileorg2.OrgData.OrgFile;
import com.matburt.mobileorg2.OrgData.OrgFileParser;
import com.matburt.mobileorg2.OrgData.OrgProviderUtils;
import com.matburt.mobileorg2.R;
import com.matburt.mobileorg2.util.FileUtils;
import com.matburt.mobileorg2.util.OrgFileNotFoundException;
import com.matburt.mobileorg2.util.OrgUtils;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.security.cert.CertificateException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * This class implements many of the operations that need to be done on
 * synching. Instead of using it directly, create a {@link SyncManager}.
 *
 * When implementing a new synchronizer, the methods {@link #isConfigured()},
 * {@link #putRemoteFile(String, String)} and {@link #getRemoteFile(String)} are
 * needed.
 */
public class Synchronizer {
    public static final String SYNC_UPDATE = "com.matburt.mobileorg2.Synchronizer.action.SYNC_UPDATE";
    public static final String SYNC_DONE = "sync_done";
    public static final String SYNC_START = "sync_start";
    public static final String SYNC_PROGRESS_UPDATE = "progress_update";
    public static final String SYNC_SHOW_TOAST = "showToast";

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
    public HashSet<String> runSynchronizer(OrgFileParser parser) {
        if(syncher == null) {
            notify.errorNotification("Sync not configured");
            return new HashSet<>();
        }

        if (!syncher.isConfigured()) {
            notify.errorNotification("Sync not configured");
            return new HashSet<>();
        }

        if (!syncher.isConnectable()) {
            notify.errorNotification("No network connection available");
            return new HashSet<>();
        }

        try {
            announceStartSync();
            HashSet<String> changedFiles = syncher.synchronize();

            for (String filename : changedFiles) {
                OrgFile orgFile = new OrgFile(filename, filename,"");
                FileReader fileReader = new FileReader(syncher.getFilesDir() + filename);
                BufferedReader bufferedReader = new BufferedReader(fileReader);

                OrgFileParser.parseFile(orgFile, bufferedReader, parser, context);
            }

//            discardAgenda(filenameMap, remoteChecksums);
            announceSyncDone();
            return changedFiles;
        } catch (Exception e) {
            showErrorNotification(e);
            Log.e("Synchronizer", "Error synchronizing", e);
            OrgUtils.announceSyncDone(context);
            return new HashSet<>();
        }
    }

    /**
     * Remove the agenda.org file created by Emacs.
     * Instead the agenda is build by the app
     * @param filenameMap
     * @param remoteChecksums
     */
    void discardAgenda(HashMap<String, String> filenameMap, HashMap<String, String> remoteChecksums){
        filenameMap.remove(FileUtils.AGENDA_FILE);
        remoteChecksums.remove(FileUtils.AGENDA_FILE);
    }

    public void pushNewFiles(Set<String> remoteFiles, String indexFileContent, String remoteChecksumContent){
        for(String file : remoteFiles){
            Log.v("newFiles","remote file : "+file);
        }

        indexFileContent = indexFileContent.trim();
        remoteChecksumContent = remoteChecksumContent.trim();

        Cursor cursor = resolver.query(Files.CONTENT_URI,
                new String[]{Files.FILENAME}, null, null, null);
        Set<String> localFiles = new HashSet<>(OrgProviderUtils.cursorToArrayList(cursor));
        if(cursor != null) cursor.close();

        for(String file : localFiles) {
            Log.v("newFiles", "local file : " + file);
        }

        localFiles.removeAll(remoteFiles);
        for(String filename: localFiles){
            String content = OrgFile.getFileContentWithEdits(filename, resolver);
            try {
                syncher.putRemoteFile(filename, content);
            } catch (IOException e) {
                e.printStackTrace();
            }
            indexFileContent += "\n* [[file:"+filename+"]["+filename+"]]";
            remoteChecksumContent += "\n0  "+filename;
        }

        if(localFiles.size() > 0) try {
            syncher.putRemoteFile(FileUtils.INDEX_FILE, indexFileContent);
            syncher.putRemoteFile(FileUtils.CHECKSUM_FILE, remoteChecksumContent);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * This method will fetch the local and the remote version of the capture
     * file combine their content. This combined version is transfered to the
     * remote.
     */
    public void pushCaptures() throws IOException,
            CertificateException {
        final String filename = FileUtils.CAPTURE_FILE;
        Log.v("sync","sync running");

        notify.updateNotification("Uploading captures");

        String localContents = OrgFile.getFileContentWithEdits(filename, resolver);

        Log.v("sync","local content size : "+localContents.length());
        Log.v("sync","local content  : "+localContents);

        if (localContents.equals(""))
            return;

        try{
            String remoteContent = FileUtils.read(syncher.getRemoteFile(filename));
            if (remoteContent.indexOf("{\"error\":") == -1)
                localContents = remoteContent + "\n" + localContents;
        } catch (FileNotFoundException e){
            e.printStackTrace();
        }

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
    public ArrayList<String> pullChangedFiles(OrgFileParser parser,
                                              HashMap<String,String> remoteChecksums,
                                              HashMap<String,String> filenameMap) throws CertificateException, IOException {

        ArrayList<String> changedFiles = getFilesThatChangedRemotely(remoteChecksums);

        changedFiles.remove(FileUtils.INDEX_FILE);

        if(changedFiles.size() == 0)
            return changedFiles;

        announceProgressDownload(changedFiles.get(0), 0, changedFiles.size() + 1);

        Collections.sort(changedFiles, new OrgUtils.SortIgnoreCase());

        final int totalNumberOfFiles = changedFiles.size() + 2;
        int fileIndex = 1;
        for (String filename : changedFiles) {
            announceProgressDownload(filename, fileIndex++, totalNumberOfFiles);
            Log.d("MobileOrg", context.getString(R.string.downloading) +
                    " " + filename + "/" + filenameMap.get(filename));

            OrgFile orgFile = new OrgFile(filename, filenameMap.get(filename),
                    remoteChecksums.get(filename));
                   BufferedReader breader = syncher.getRemoteFile(orgFile.filename);
            OrgFileParser.parseFile(orgFile, breader, parser, context);

        }

        announceProgressDownload("", changedFiles.size() + 1, changedFiles.size() + 2);

        return changedFiles;
    }

    private HashMap<String, String> parseIndexFile(String remoteIndexContents) throws CertificateException, IOException {
        OrgProviderUtils.setPriorities(
                OrgFileParser.getPrioritiesFromIndex(remoteIndexContents),
                resolver);
        OrgProviderUtils.setTags(
                OrgFileParser.getTagsFromIndex(remoteIndexContents), resolver);
        HashMap<String, String> filenameMap = OrgFileParser
                .getFilesFromIndex(remoteIndexContents);
        return filenameMap;
    }

    /**
     Compare local and remote checksums to see which files have changed.
     @return the list of remotely changed files
     */
    private ArrayList<String> getFilesThatChangedRemotely(HashMap<String, String> remoteChecksums) {
        HashMap<String, String> localChecksums = OrgProviderUtils.getFileChecksums(resolver);
        Log.v("sync","size localchecksum : "+localChecksums.size());
        for(Map.Entry<String, String> s: localChecksums.entrySet()){
            Log.v("sync", "key test : "+s.getKey());
        }

        ArrayList<String> filesToGet = new ArrayList<String>();

        for (String key : remoteChecksums.keySet()) {
            Log.v("sync", "key : "+key);

            if (localChecksums.containsKey(key)){
                Log.v("sync","key : "+key+ "\ncompare "+localChecksums.get(key) + " with "+remoteChecksums.get(key));
            }
            if (localChecksums.containsKey(key)
                    && localChecksums.get(key).equals(remoteChecksums.get(key)))
                continue;
            filesToGet.add(key);
        }

        filesToGet.remove(FileUtils.CAPTURE_FILE);

        return filesToGet;
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
        if(syncher == null) return;
        syncher.postSynchronize();
    }

}
