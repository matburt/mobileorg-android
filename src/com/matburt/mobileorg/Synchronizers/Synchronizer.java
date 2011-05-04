package com.matburt.mobileorg.Synchronizers;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.content.res.Resources.NotFoundException;
import android.os.Environment;
import android.text.TextUtils;
import android.util.Log;
import com.matburt.mobileorg.Error.ReportableError;
import com.matburt.mobileorg.MobileOrgDatabase;
import com.matburt.mobileorg.R;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

abstract public class Synchronizer
{
    public MobileOrgDatabase appdb = null;
    public SharedPreferences appSettings = null;
    public Context rootContext = null;
    public static final String LT = "MobileOrg";
    public Resources r;
    final private int BUFFER_SIZE = 23 * 1024;

    public abstract void pull() throws NotFoundException, ReportableError;
    public abstract void push() throws NotFoundException, ReportableError;
    public abstract boolean checkReady();

    public void close() {
        if (this.appdb != null)
            this.appdb.close();
    }

    public BufferedReader fetchOrgFile(String orgPath) throws NotFoundException, ReportableError{
        return null;
    }

    public void fetchAndSaveOrgFile(String orgPath, String destPath) throws ReportableError {
        BufferedReader reader = this.fetchOrgFile(orgPath);
        BufferedWriter writer = this.getWriteHandle(destPath);

        char[] baf = new char[BUFFER_SIZE];
        int actual = 0;
        try {
            while (actual != -1) {
                writer.write(baf, 0, actual);
                actual = reader.read(baf, 0, BUFFER_SIZE);
            }
            writer.close();
        }
        catch (java.io.IOException e) {
            throw new ReportableError(
                           r.getString(R.string.error_file_write,
                                       orgPath),
                           e);

        }
    }

    public String fetchOrgFileString(String orgPath) throws ReportableError {
        BufferedReader reader = this.fetchOrgFile(orgPath);
        if (reader == null) {
            return "";
        }
        String fileContents = "";
        String thisLine = "";
        try {
            while ((thisLine = reader.readLine()) != null) {
                fileContents += thisLine + "\n";
            }
        }
        catch (java.io.IOException e) {
               throw new ReportableError(
                       r.getString(R.string.error_file_read, orgPath),
                       e);
        }
        return fileContents;
    }

    BufferedWriter getWriteHandle(String localRelPath) throws ReportableError {
        String storageMode = this.appSettings.getString("storageMode", "");
        BufferedWriter writer = null;
        if (storageMode.equals("internal") || storageMode == null) {
            FileOutputStream fs;
            try {
                String normalized = localRelPath.replace("/", "_");
                fs = this.rootContext.openFileOutput(normalized, Context.MODE_PRIVATE);
                writer = new BufferedWriter(new OutputStreamWriter(fs));
            }
            catch (java.io.FileNotFoundException e) {
                Log.e(LT, "Caught FNFE trying to open file " + localRelPath);
                throw new ReportableError(
                        r.getString(R.string.error_file_not_found,
                                    localRelPath),
                        e);
            }
            catch (java.io.IOException e) {
                Log.e(LT, "IO Exception initializing writer on file " + localRelPath);
                throw new ReportableError(
                        r.getString(R.string.error_file_not_found, localRelPath),
                        e);
            }
        }
        else if (storageMode.equals("sdcard")) {
            try {
                File root = Environment.getExternalStorageDirectory();
                File morgDir = new File(root, "mobileorg");
                morgDir.mkdir();
                if (morgDir.canWrite()){
                    File orgFileCard = new File(morgDir, localRelPath);
                    FileWriter orgFWriter = new FileWriter(orgFileCard, true);
                    writer = new BufferedWriter(orgFWriter);
                }
                else {
                    Log.e(LT, "Write permission denied on " + localRelPath);
                    throw new ReportableError(
                            r.getString(R.string.error_file_permissions,
                                        morgDir.getAbsolutePath()),
                            null);
                }
            } catch (java.io.IOException e) {
                Log.e(LT, "IO Exception initializing writer on sdcard file: " + localRelPath);
                throw new ReportableError(
                        "IO Exception initializing writer on sdcard file",
                        e);
            }
        }
        else {
            Log.e(LT, "Unknown storage mechanism " + storageMode);
                throw new ReportableError(
                		r.getString(R.string.error_local_storage_method_unknown,
                                    storageMode),
                		null);
        }
        return writer;
    }

    BufferedReader getReadHandle(String localRelPath) throws ReportableError {
        String storageMode = this.appSettings.getString("storageMode", "");
        BufferedReader reader;
        if (storageMode.equals("internal") || storageMode == null) {
            FileInputStream fs;
            try {
                fs = rootContext.openFileInput(localRelPath);
                reader = new BufferedReader(new InputStreamReader(fs));
            }
            catch (java.io.FileNotFoundException e) {
            	Log.i(LT, "Did not find " + localRelPath + " file, not pushing.");
                return null;
            }
        }
        else if (storageMode.equals("sdcard")) {
            try {
                File root = Environment.getExternalStorageDirectory();
                File morgDir = new File(root, "mobileorg");
                File morgFile = new File(morgDir, localRelPath);
                if (!morgFile.exists()) {
                    Log.i(LT, "Did not find " + localRelPath + " file, not pushing.");
                    return null;
                }
                FileReader orgFReader = new FileReader(morgFile);
                reader = new BufferedReader(orgFReader);
            }
            catch (java.io.IOException e) {
                throw new ReportableError(
                		r.getString(R.string.error_file_read, localRelPath),
                		e);
            }
        }
        else {
        	throw new ReportableError(
        			r.getString(R.string.error_local_storage_method_unknown, storageMode),
        			null);
        }
        return reader;
    }

    void removeFile(String filePath) {
            this.appdb.removeFile(filePath);
            String storageMode = this.appSettings.getString("storageMode", "");
            if (storageMode.equals("internal") || storageMode == null) {
                this.rootContext.deleteFile(filePath);
            }
            else if (storageMode.equals("sdcard")) {
                File root = Environment.getExternalStorageDirectory();
                File morgDir = new File(root, "mobileorg");
                File morgFile = new File(morgDir, filePath);
                morgFile.delete();
            }
    }

    HashMap<String, String> getOrgFilesFromMaster(String master) {
        Pattern getOrgFiles = Pattern.compile("\\[file:(.*?\\.(?:org|pgp|gpg|enc))\\]\\[(.*?)\\]\\]");
        Matcher m = getOrgFiles.matcher(master);
        HashMap<String, String> allOrgFiles = new HashMap<String, String>();
        while (m.find()) {
            Log.i(LT, "Found org file: " + m.group(2));
            allOrgFiles.put(m.group(2), m.group(1));
        }

        return allOrgFiles;
    }

    HashMap<String, String> getChecksums(String master) {
        HashMap<String, String> chksums = new HashMap<String, String>();
        for (String eachLine : master.split("[\\n\\r]+")) {
            if (TextUtils.isEmpty(eachLine))
                continue;
            String[] chksTuple = eachLine.split("\\s+");
            chksums.put(chksTuple[1], chksTuple[0]);
        }
        return chksums;
    }

    ArrayList<HashMap<String, Boolean>> getTodos(String master) {
        Pattern getTodos = Pattern.compile("#\\+TODO:\\s+([\\s\\w-]*)(\\| ([\\s\\w-]*))*");
        Matcher m = getTodos.matcher(master);
        ArrayList<HashMap<String, Boolean>> todoList = new ArrayList<HashMap<String, Boolean>>();
        while (m.find()) {
            HashMap<String, Boolean> holding = new HashMap<String, Boolean>();
            Boolean isDone = false;
            for (int idx = 1; idx <= m.groupCount(); idx++) {
                if (m.group(idx) != null &&
                    m.group(idx).length() > 0) {
                    if (m.group(idx).indexOf("|") != -1) {
                        isDone = true;
                        continue;
                    }
                    String[] grouping = m.group(idx).split("\\s+");
                    for (int jdx = 0; jdx < grouping.length; jdx++) {
                        holding.put(grouping[jdx].trim(),
                                    isDone);
                    }
                }
            }
            todoList.add(holding);
        }
        return todoList;
    }

    ArrayList<ArrayList<String>> getPriorities(String master) {
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
