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
import java.io.FileOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;

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
        String storageMode = this.appSettings.getString("storageMode", "");
        BufferedReader reader = null;
        String fileContents = "";

        if (storageMode.equals("internal") || storageMode == null) {
            FileInputStream fs;
            try {
                fs = rootActivity.openFileInput("mobileorg.org");
                reader = new BufferedReader(new InputStreamReader(fs));
            }
            catch (java.io.FileNotFoundException e) {
            	Log.i(LT, "Did not find mobileorg.org file, not pushing.");
                return;
            }
        }
        else if (storageMode.equals("sdcard")) {
            try {
                File root = Environment.getExternalStorageDirectory();
                File morgDir = new File(root, "mobileorg");
                File morgFile = new File(morgDir, "mobileorg.org");
                if (!morgFile.exists()) {
                    Log.i(LT, "Did not find mobileorg.org file, not pushing.");
                    return;
                }
                FileReader orgFReader = new FileReader(morgFile);
                reader = new BufferedReader(orgFReader);
            }
            catch (java.io.IOException e) {
                throw new ReportableError(
                		r.getString(R.string.error_file_read, "mobileorg.org"),
                		e);
            }
        }
        else {
        	throw new ReportableError(
        			r.getString(R.string.error_local_storage_method_unknown, storageMode),
        			null);
        }

        String thisLine = "";
        try {
            while ((thisLine = reader.readLine()) != null) {
                fileContents += thisLine + "\n";
            }
        }
        catch (java.io.IOException e) {
        	throw new ReportableError(
            		r.getString(R.string.error_file_read, "mobileorg.org"),
            		e);
        }

        String indexFile = this.appSettings.getString("indexFilePath", "");
        File fIndexFile = new File(indexFile);
        String basePath = fIndexFile.getParent();
        
        this.appendSDCardFile(basePath + "/mobileorg.org", fileContents);
        this.appdb.removeFile("mobileorg.org");

        if (storageMode.equals("internal") || storageMode == null) {
            this.rootActivity.deleteFile("mobileorg.org");
        }
        else if (storageMode.equals("sdcard")) {
            File root = Environment.getExternalStorageDirectory();
            File morgDir = new File(root, "mobileorg");
            File morgFile = new File(morgDir, "mobileorg.org");
            morgFile.delete();
        }
    }

    private void appendSDCardFile(String path,
                                  String content) throws NotFoundException, ReportableError {
        String originalContent = "";
        try {
            originalContent = this.readFile(path) + "\n";
        }
        catch (java.io.FileNotFoundException e) {}
        String newContent = originalContent + content;
        this.putFile(path, newContent);
    }

    private void putFile(String path,
                         String content) throws NotFoundException, ReportableError {
        Log.d(LT, "Writing to mobileorg.org file at: " + path);
        BufferedWriter fWriter;
        try {
            File fMobileOrgFile = new File(path);
            FileWriter orgFWriter = new FileWriter(fMobileOrgFile, true);
            fWriter = new BufferedWriter(orgFWriter);
            fWriter.write(content);
            fWriter.flush();
            fWriter.close();
        }
        catch (java.io.IOException e) {
            throw new ReportableError(
                    r.getString(R.string.error_file_write, path),
                    e);                  

        }
    }

    public void pull() throws NotFoundException, ReportableError {
        String indexFile = this.appSettings.getString("indexFilePath","");
        Log.d(LT, "Index file at: " + indexFile);
        File fIndexFile = new File(indexFile);
        String basePath = fIndexFile.getParent();
        String filebuffer = "";
        try {
            filebuffer = this.readFile(indexFile);
        }
        catch (java.io.FileNotFoundException e) {
            throw new ReportableError(
                    r.getString(R.string.error_file_not_found, indexFile),
                    e);
        }
        HashMap<String, String> masterList = this.getOrgFilesFromMaster(filebuffer);
        ArrayList<ArrayList<String>> todoLists = this.getTodos(filebuffer);
        ArrayList<ArrayList<String>> priorityLists = this.getPriorities(filebuffer);
        this.appdb.setTodoList(todoLists);
        this.appdb.setPriorityList(priorityLists);

        for (String key : masterList.keySet()) { 
            Log.d(LT, "Fetching: " + key + ": " + basePath + "/" + masterList.get(key));
            this.appdb.addOrUpdateFile(masterList.get(key), key);
        }
    }

    private String readFile(String filePath) throws ReportableError,
                                                    java.io.FileNotFoundException {
        FileInputStream readerIS;
        BufferedReader fReader;
        File inpfile = new File(filePath);
        try {
            readerIS = new FileInputStream(inpfile);
            fReader = new BufferedReader(new InputStreamReader(readerIS));
        }
        catch (java.io.FileNotFoundException e) {
            Log.d(LT, "Could not locate file " + filePath);
            throw e;
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
        Pattern getOrgFiles = Pattern.compile("\\[file:(.*?\\.(?:org|pgp|gpg|enc))\\]\\[(.*?)\\]\\]");
        Matcher m = getOrgFiles.matcher(master);
        HashMap<String, String> allOrgFiles = new HashMap<String, String>();
        while (m.find()) {
            allOrgFiles.put(m.group(2), m.group(1));
        }

        return allOrgFiles;
    }

    //NOTE: This is a common method and needs to be generalized
    private ArrayList<ArrayList<String>> getTodos(String master) {
        Pattern getTodos = Pattern.compile("#\\+TODO:\\s+([\\s\\w-]*)(\\| ([\\s\\w-]*))*");
        Matcher m = getTodos.matcher(master);
        ArrayList<ArrayList<String>> todoList = new ArrayList<ArrayList<String>>();
        while (m.find()) {
            ArrayList<String> holding = new ArrayList<String>();
            for (int idx = 1; idx <= m.groupCount(); idx++) {
                if (m.group(idx) != null &&
                    m.group(idx).indexOf("|") == -1 &&
                    m.group(idx).length() > 0) {
                    String[] grouping = m.group(idx).split("\\s+");
                    for (int jdx = 0; jdx < grouping.length; jdx++) {
                        holding.add(grouping[jdx].trim());
                    }
                }
            }
            todoList.add(holding);
        }
        return todoList;
    }

    private ArrayList<ArrayList<String>> getPriorities(String master) {
        Pattern getPriorities = Pattern.compile("#\\+ALLPRIORITIES:\\s+([A-Z\\s]*)");
        Matcher t = getPriorities.matcher(master);
        ArrayList<ArrayList<String>> priorityList = new ArrayList<ArrayList<String>>();
        while (t.find()) {
            ArrayList<String> holding = new ArrayList<String>();
            if (t.group(1) != null &&
                t.group(1).length() > 0) {
                String[] grouping = t.group(1).split("\\s+");
                for (int jdx = 0; jdx < grouping.length; jdx++) {
                    holding.add(grouping[jdx].trim());
                }
            }
            priorityList.add(holding);
        }
        return priorityList;
    }

}