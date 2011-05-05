package com.matburt.mobileorg.Synchronizers;

import android.content.Context;
import android.content.res.Resources.NotFoundException;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.util.Log;
import com.matburt.mobileorg.Error.ReportableError;
import com.matburt.mobileorg.MobileOrgDatabase;
import com.matburt.mobileorg.R;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;

public class SDCardSynchronizer extends Synchronizer
{
    public SDCardSynchronizer(Context parentContext) {
        this.rootContext = parentContext;
        this.r = this.rootContext.getResources();
        this.appdb = new MobileOrgDatabase((Context)parentContext);
        this.appSettings = PreferenceManager.getDefaultSharedPreferences(
                                   parentContext.getApplicationContext());
    }

    public void push() throws NotFoundException, ReportableError {
        String storageMode = this.appSettings.getString("storageMode", "");
        BufferedReader reader = null;
        String fileContents = "";

        if (storageMode.equals("internal") || storageMode == null) {
            FileInputStream fs;
            try {
                fs = rootContext.openFileInput("mobileorg.org");
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

    public boolean checkReady() {
        if (this.appSettings.getString("indexFilePath", "").equals(""))
            return false;
        return true;
    }

    public void pull() throws NotFoundException, ReportableError {
        String indexFile = this.appSettings.getString("indexFilePath","");
        Log.d(LT, "Index file at: " + indexFile);
        File fIndexFile = new File(indexFile);
        String basePath = fIndexFile.getParent();
        String chkPath = basePath + "/checksums.dat";
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
        ArrayList<HashMap<String, Boolean>> todoLists = this.getTodos(filebuffer);
        ArrayList<ArrayList<String>> priorityLists = this.getPriorities(filebuffer);
        this.appdb.setTodoList(todoLists);
        this.appdb.setPriorityList(priorityLists);

        try {
            filebuffer = this.readFile(chkPath);
        }
        catch (java.io.FileNotFoundException e) {
            throw new ReportableError(
                    r.getString(R.string.error_file_not_found, chkPath),
                    e);
        }
        HashMap<String, String> newChecksums = this.getChecksums(filebuffer);
        HashMap<String, String> oldChecksums = this.appdb.getChecksums();
        for (String key : masterList.keySet()) { 
            if (oldChecksums.containsKey(key) &&
                newChecksums.containsKey(key) &&
                oldChecksums.get(key).equals(newChecksums.get(key)))
                continue;
            Log.d(LT, "Fetching: " + key + ": " + basePath + "/" + masterList.get(key));
            this.appdb.addOrUpdateFile(masterList.get(key), key, newChecksums.get(key));
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
}