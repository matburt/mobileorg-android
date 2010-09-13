package com.matburt.mobileorg;
import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.content.res.Resources.NotFoundException;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.util.Log;
import java.io.FileInputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.io.File;

public class SDCardSynchronizer implements Synchronizer
{
    private SharedPreferences appSettings;
    private Activity rootActivity;
    private MobileOrgDatabase appdb;
    private Resources r;
    private static final String LT = "MobileOrg";

    SDCardSynchronizer(Activity parentActivity) {
        this.rootActivity = parentActivity;
        this.r = this.rootActivity.getResources();
        this.appdb = new MobileOrgDatabase((Context)parentActivity);
        this.appSettings = PreferenceManager.getDefaultSharedPreferences(
                                   parentActivity.getBaseContext());
    }

    public void close() {
        this.appdb.close();
    }

    public void push() throws NotFoundException, ReportableError {

    }

    public void pull() throws NotFoundException, ReportableError {
        String indexFile = this.appSettings.getString("indexFilePath","");
        Log.d(LT, "Index file at: " + indexFile);
        File fIndexFile = new File(indexFile);
        String basePath = fIndexFile.getParent();
        String filebuffer = this.readFile(indexFile);
        HashMap<String, String> masterList = this.getOrgFilesFromMaster(filebuffer);

        for (String key : masterList.keySet()) {
            Log.d(LT, "Fetching: " + key + ": " + basePath + masterList.get(key));
            this.appdb.addOrUpdateFile(masterList.get(key), key);
        }
    }

    private String readFile(String filePath) throws ReportableError {
        FileInputStream readerIS;
        BufferedReader fReader;
        File inpfile = new File(filePath);
        try {
            readerIS = new FileInputStream(inpfile);
            fReader = new BufferedReader(new InputStreamReader(readerIS));
        }
        catch (java.io.FileNotFoundException e) {
            throw new ReportableError(r.getString(R.string.error_file_not_found, filePath),
                                      e);
        }
        String fileBuffer = "";
        String fileLine = "";
        try {
            while ((fileLine = fReader.readLine()) != null) {
                fileBuffer += fileLine + "\n";
            }
        }
        catch (java.io.IOException e) {
            throw new ReportableError(
                    r.getString(R.string.error_file_read, filePath),
                    e);                  
        }
        return fileBuffer;
    }

    //NOTE: This is a common method and needs to be generalized
    private HashMap<String, String> getOrgFilesFromMaster(String master) {
        Pattern getOrgFiles = Pattern.compile("\\[file:(.*?\\.org)\\]\\[(.*?)\\]\\]");
        Matcher m = getOrgFiles.matcher(master);
        HashMap<String, String> allOrgFiles = new HashMap<String, String>();
        while (m.find()) {
            allOrgFiles.put(m.group(2), m.group(1));
        }

        return allOrgFiles;
    }
}