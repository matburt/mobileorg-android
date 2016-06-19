package com.matburt.mobileorg2.Synchronizers;

import android.content.ContentResolver;
import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import com.matburt.mobileorg2.Gui.SynchronizerNotificationCompat;
import com.matburt.mobileorg2.OrgData.OrgFile;
import com.matburt.mobileorg2.OrgData.OrgFileParser;
import com.matburt.mobileorg2.R;
import com.matburt.mobileorg2.util.OrgUtils;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.security.cert.CertificateException;
import java.util.HashSet;

/**
 * This class implements many of the operations that need to be done on
 * synching. Instead of using it directly, create a {@link SyncManager}.
 * <p/>
 * When implementing a new synchronizer, the methods {@link #isConfigured()},
 * {@link #putRemoteFile(String, String)} and {@link #getRemoteFile(String)} are
 * needed.
 */
public class SynchronizerManager {
    public static final String SYNC_UPDATE = "com.matburt.mobileorg2.SynchronizerManager.action.SYNC_UPDATE";
    public static final String SYNC_DONE = "sync_done";
    public static final String SYNC_START = "sync_start";
    public static final String SYNC_PROGRESS_UPDATE = "progress_update";
    public static final String SYNC_SHOW_TOAST = "showToast";
    private static SynchronizerManager mSynchronizerManager = null;
    private Context context;
    private ContentResolver resolver;
    private Synchronizer syncher;
    private SynchronizerNotificationCompat notify;

    private SynchronizerManager(Context context, Synchronizer syncher, SynchronizerNotificationCompat notify) {
        this.context = context;
        this.resolver = context.getContentResolver();
        this.syncher = syncher;
        this.notify = notify;
    }

    public static SynchronizerManager getInstance(Context context, Synchronizer syncher, SynchronizerNotificationCompat notify) {
        if (mSynchronizerManager == null)
            mSynchronizerManager = new SynchronizerManager(context, syncher, notify);
        return mSynchronizerManager;
    }

    public Synchronizer getSyncher() {
        return syncher;
    }

    public boolean isEnabled() {
        return true;
    }

    /**
     * @return List of files that where changed.
     */
    public HashSet<String> runSynchronizer(OrgFileParser parser) {
        if (syncher == null) {
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
            SyncResult pulledFiles = syncher.synchronize();

            for (String filename : pulledFiles.deletedFiles) {
                Log.v("sync", "deleted file: " + filename);

                OrgFile orgFile = new OrgFile(filename, resolver);
                orgFile.removeFile(context);
            }

            for (String filename : pulledFiles.newFiles) {
                Log.v("sync", "new file: " + filename);
                OrgFile orgFile = new OrgFile(filename, filename);
                FileReader fileReader = new FileReader(syncher.getAbsoluteFilesDir(context) + "/" + filename);
                BufferedReader bufferedReader = new BufferedReader(fileReader);

                OrgFileParser.parseFile(orgFile, bufferedReader, parser, context);
            }

            for (String filename : pulledFiles.changedFiles) {
                Log.v("sync", "changed file : " + filename);
                OrgFile orgFile = new OrgFile(filename, filename);
                FileReader fileReader = new FileReader(syncher.getAbsoluteFilesDir(context) + "/" + filename);
                BufferedReader bufferedReader = new BufferedReader(fileReader);

                OrgFileParser.parseFile(orgFile, bufferedReader, parser, context);
            }

            announceSyncDone();
            return pulledFiles.changedFiles;
        } catch (Exception e) {
            showErrorNotification(e);
            Log.e("SynchronizerManager", "Error synchronizing", e);
            OrgUtils.announceSyncDone(context);
            return new HashSet<>();
        }
    }

    private void announceStartSync() {
        notify.setupNotification();
        OrgUtils.announceSyncStart(context);
    }

    private void announceProgressUpdate(int progress, String message) {
        if (message != null && TextUtils.isEmpty(message) == false)
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
        if (syncher == null) return;
        syncher.postSynchronize();
    }

}
