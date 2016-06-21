package com.matburt.mobileorg2.Synchronizers;

import android.content.ContentResolver;
import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import com.matburt.mobileorg2.Gui.SynchronizerNotification;
import com.matburt.mobileorg2.Gui.SynchronizerNotificationCompat;
import com.matburt.mobileorg2.OrgData.OrgFile;
import com.matburt.mobileorg2.OrgData.OrgFileParser;
import com.matburt.mobileorg2.R;
import com.matburt.mobileorg2.util.OrgUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.security.cert.CertificateException;
import java.util.HashSet;

/**
 * The base class of all the synchronizers.
 * The singleton instance of the class can be retreived using getInstance()
 * This class implements many of the operations that need to be done on
 * synching. Instead of using it directly, create a {@link SyncManager}.
 * <p/>
 * When implementing a new synchronizer, the methods {@link #isConfigured()},
 * {@link #putRemoteFile(String, String)} and {@link #getRemoteFile(String)} are
 * needed.
 */
public abstract class SynchronizerManager {
    public static final String SYNC_UPDATE = "com.matburt.mobileorg2.SynchronizerManager.action.SYNC_UPDATE";
    public static final String SYNC_DONE = "sync_done";
    public static final String SYNC_START = "sync_start";
    public static final String SYNC_PROGRESS_UPDATE = "progress_update";
    public static final String SYNC_SHOW_TOAST = "showToast";
    private static SynchronizerManager mSynchronizerManager = null;
    protected Context context;
    private ContentResolver resolver;
    private SynchronizerNotificationCompat notify;


    protected SynchronizerManager(Context context) {
        this.context = context;
        this.resolver = context.getContentResolver();

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.HONEYCOMB)
            this.notify = new SynchronizerNotification(context);
        else
            this.notify = new SynchronizerNotificationCompat(context);
    }

    public static SynchronizerManager getInstance() {
            return mSynchronizerManager;
    }

    public static void setInstance(SynchronizerManager instance){
        mSynchronizerManager = instance;
    }
    public boolean isEnabled() {
        return true;
    }

    /**
     *
     * @param instance
     */
    static public void updateSynchronizer(SynchronizerManager instance){

    }

    /**
     * @return List of files that where changed.
     */
    public HashSet<String> runSynchronizer(OrgFileParser parser) {
        if (!isConfigured()) {
            notify.errorNotification("Sync not configured");
            return new HashSet<>();
        }

        if (!isConnectable()) {
            notify.errorNotification("No network connection available");
            return new HashSet<>();
        }

        try {
            announceStartSync();
            SyncResult pulledFiles = synchronize();

            for (String filename : pulledFiles.deletedFiles) {
                Log.v("sync", "deleted file: " + filename);

                OrgFile orgFile = new OrgFile(filename, resolver);
                orgFile.removeFile(context);
            }

            for (String filename : pulledFiles.newFiles) {
                Log.v("sync", "new file: " + filename);
                OrgFile orgFile = new OrgFile(filename, filename);
                FileReader fileReader = new FileReader(getAbsoluteFilesDir(context) + "/" + filename);
                BufferedReader bufferedReader = new BufferedReader(fileReader);

                OrgFileParser.parseFile(orgFile, bufferedReader, parser, context);
            }

            for (String filename : pulledFiles.changedFiles) {
                Log.v("sync", "changed file : " + filename);
                OrgFile orgFile = new OrgFile(filename, filename);
                FileReader fileReader = new FileReader(getAbsoluteFilesDir(context) + "/" + filename);
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

    abstract public String getRelativeFilesDir();

    public String getAbsoluteFilesDir(Context context) {
        return context.getFilesDir() + "/" + getRelativeFilesDir();
    }

    /**
     * Delete all files from the synchronized repository
     * except repository configuration files
     * @param context
     */
    public void clearRepository(Context context) {
        File dir = new File(getAbsoluteFilesDir(context));
        for (File file : dir.listFiles()) {
            file.delete();
        }
    }


    /**
     * Called before running the synchronizer to ensure that it's configuration
     * is in a valid state.
     */
    abstract boolean isConfigured();

    /**
     * Called before running the synchronizer to ensure it can connect.
     */
    abstract boolean isConnectable();

    /**
     * Replaces the file on the remote end with the given content.
     *
     * @param filename Name of the file, without path
     * @param contents Content of the new file
     */
    abstract void putRemoteFile(String filename, String contents)
            throws IOException;

    /**
     * Returns a BufferedReader to the remote file.
     *
     * @param filename
     *            Name of the file, without path
     */
    abstract BufferedReader getRemoteFile(String filename)
            throws IOException, CertificateException;

    abstract SyncResult synchronize();


    /**
     * Use this to disconnect from any services and cleanup.
     */
    public abstract void postSynchronize();

    /**
     * Synchronize a new file
     *
     * @param filename
     */
    abstract public void addFile(String filename);


}
