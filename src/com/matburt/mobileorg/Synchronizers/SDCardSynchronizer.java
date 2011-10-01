package com.matburt.mobileorg.Synchronizers;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;

import com.matburt.mobileorg.Parsing.OrgFile;

import android.content.Context;
import android.os.Environment;
import android.util.Log;

public class SDCardSynchronizer extends Synchronizer
{
    public SDCardSynchronizer(Context parentContext) {
    	super(parentContext);
    }
    

    public boolean isConfigured() {
        if (this.appSettings.getString("indexFilePath", "").equals(""))
            return false;
        return true;
    }

    public void push() throws IOException {
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
        

        String thisLine = "";
            while ((thisLine = reader.readLine()) != null) {
                fileContents += thisLine + "\n";
            }
        

        String indexFile = this.appSettings.getString("indexFilePath", "");
        File fIndexFile = new File(indexFile);
        String basePath = fIndexFile.getParent();

        this.appendSDCardFile(basePath + "/mobileorg.org", fileContents);
    }

    private void appendSDCardFile(String path,
                                  String content) throws IOException {
        String originalContent = "";
        try {
            originalContent = this.readFile(path) + "\n";
        }
        catch (java.io.FileNotFoundException e) {}
        String newContent = originalContent + content;
        this.putFile(path, newContent);
    }

    private void putFile(String path,
                         String content) throws IOException {
        Log.d(LT, "Writing to mobileorg.org file at: " + path);
        BufferedWriter fWriter;

            File fMobileOrgFile = new File(path);
            FileWriter orgFWriter = new FileWriter(fMobileOrgFile, true);
            fWriter = new BufferedWriter(orgFWriter);
            fWriter.write(content);
            fWriter.close();
    }


    public void pull() throws IOException {
        String indexFile = this.appSettings.getString("indexFilePath","");
        Log.d(LT, "Index file at: " + indexFile);
        File fIndexFile = new File(indexFile);
        String basePath = fIndexFile.getParent();
        String chkPath = basePath + "/checksums.dat";
        String filebuffer = "";

            filebuffer = this.readFile(indexFile);


        HashMap<String, String> masterList = OrgFile.getOrgFilesFromMaster(filebuffer);
        ArrayList<HashMap<String, Boolean>> todoLists = OrgFile.getTodos(filebuffer);
        ArrayList<ArrayList<String>> priorityLists = OrgFile.getPriorities(filebuffer);
        this.appdb.setTodoList(todoLists);
        this.appdb.setPriorityList(priorityLists);

            filebuffer = this.readFile(chkPath);


        HashMap<String, String> newChecksums = OrgFile.getChecksums(filebuffer);
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

    private String readFile(String filePath) throws IOException {
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
            while ((fileLine = fReader.readLine()) != null) {
                fileBuffer += fileLine + "\n";
            }
        
        return fileBuffer;
    }
}