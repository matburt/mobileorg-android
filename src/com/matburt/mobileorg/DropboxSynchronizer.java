package com.matburt.mobileorg;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.ArrayList;
import java.lang.IllegalArgumentException;
import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.content.res.Resources.NotFoundException;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.util.Log;
import android.text.TextUtils;
import android.os.AsyncTask;
import android.widget.Toast;

import com.dropbox.client.DropboxAPI;
import com.dropbox.client.DropboxAPI.Config;
import com.dropbox.client.DropboxAPI.FileDownload;

public class DropboxSynchronizer implements Synchronizer {
    private SharedPreferences appSettings;
    private Activity rootActivity;
    private MobileOrgDatabase appdb;
    private Resources r;
    private boolean pushedStageFile = false;
    private static final String LT = "MobileOrg";
    private boolean hasToken = false;

    private DropboxAPI api = new DropboxAPI();
    private Config dbConfig;

    DropboxSynchronizer(Activity parentActivity) {
        this.rootActivity = parentActivity;
        this.r = this.rootActivity.getResources();
        this.appdb = new MobileOrgDatabase((Context)parentActivity);
        this.appSettings = PreferenceManager.getDefaultSharedPreferences(
                                      parentActivity.getBaseContext());
        this.connect();
    }

    public void close() {
        this.appdb.close();
    }

    public void push() throws NotFoundException, ReportableError {
        Log.i(LT, "Push is currently unsupported");
    }

    public boolean checkReady() {
        //check key and secret also
        //possibly attempt to login and return false if it fails
        if (this.appSettings.getString("dropboxPath", "").equals(""))
            return false;
        return true;
    }

    public void setLoggedIn(boolean loggedIn) {
    	this.hasToken = loggedIn;
    }

    public void pull() throws NotFoundException, ReportableError {
        String indexFilePath = this.appSettings.getString("dropboxPath", "");
        String masterStr = this.fetchOrgFile(indexFilePath);
        Log.i(LT, "Contents: " + masterStr);
        if (masterStr.equals("")) {
            throw new ReportableError(
                            r.getString(R.string.error_file_not_found, indexFilePath),
                            null);
        }
        HashMap<String, String> masterList = this.getOrgFilesFromMaster(masterStr);
        ArrayList<HashMap<String, Boolean>> todoLists = this.getTodos(masterStr);
        ArrayList<ArrayList<String>> priorityLists = this.getPriorities(masterStr);
        this.appdb.setTodoList(todoLists);
        this.appdb.setPriorityList(priorityLists);
        String pathActual = this.getRootPath();
        //Get checksums file
        masterStr = this.fetchOrgFile(pathActual + "checksums.dat");
        HashMap<String, String> newChecksums = this.getChecksums(masterStr);
        HashMap<String, String> oldChecksums = this.appdb.getChecksums();

        //Get other org files
        for (String key : masterList.keySet()) {
            if (oldChecksums.containsKey(key) &&
                newChecksums.containsKey(key) &&
                oldChecksums.get(key).equals(newChecksums.get(key)))
                continue;
            Log.d(LT, "Fetching: " +
                  key + ": " + pathActual + masterList.get(key));
            String fileContents = this.fetchOrgFile(pathActual +
                                                    masterList.get(key));
            String storageMode = this.appSettings.getString("storageMode", "");
            BufferedWriter writer = new BufferedWriter(new StringWriter());

            if (storageMode.equals("internal") || storageMode == null) {
                FileOutputStream fs;
                try {
                    String normalized = masterList.get(key).replace("/", "_");
                    fs = rootActivity.openFileOutput(normalized, 0);
                    writer = new BufferedWriter(new OutputStreamWriter(fs));
                }
                catch (java.io.FileNotFoundException e) {
                	throw new ReportableError(
                    		r.getString(R.string.error_file_not_found, key),
                    		e);
                }
            }
            else if (storageMode.equals("sdcard")) {

                try {
                    File root = Environment.getExternalStorageDirectory();
                    File morgDir = new File(root, "mobileorg");
                    morgDir.mkdir();
                    if (morgDir.canWrite()){
                        File orgFileCard = new File(morgDir, masterList.get(key));
                        File orgDirCard = orgFileCard.getParentFile();
                        orgDirCard.mkdirs();
                        FileWriter orgFWriter = new FileWriter(orgFileCard);
                        writer = new BufferedWriter(orgFWriter);
                    }
                    else {
                        throw new ReportableError(
                        		r.getString(R.string.error_file_permissions,
                                            morgDir.getAbsolutePath()),
                        		null);
                    }
                } catch (java.io.IOException e) {
                    throw new ReportableError(
                    		"IO Exception initializing writer on sdcard file",
                    		e);
                }
            }
            else {
                throw new ReportableError(
                		r.getString(R.string.error_local_storage_method_unknown, storageMode),
                		null);
            }

            try {
            	writer.write(fileContents);
            	this.appdb.addOrUpdateFile(masterList.get(key), key, newChecksums.get(key));
                writer.flush();
                writer.close();
            }
            catch (java.io.IOException e) {
                throw new ReportableError(
                		r.getString(R.string.error_file_write, masterList.get(key)),
                		e);
            }
        }
    }

    private String getRootPath() throws ReportableError {
        String dbPath = this.appSettings.getString("dropboxPath","");
        return dbPath.substring(0, dbPath.lastIndexOf("/")+1);
    }

    private String fetchOrgFile(String orgPath) throws ReportableError {
        Log.i(LT, "Downloading " + orgPath);
        FileDownload fd = api.getFileStream("dropbox", orgPath, null);
        BufferedReader reader = new BufferedReader(new InputStreamReader(fd.is));
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

    private HashMap<String, String> getOrgFilesFromMaster(String master) {
        Pattern getOrgFiles = Pattern.compile("\\[file:(.*?\\.(?:org|pgp|gpg|enc))\\]\\[(.*?)\\]\\]");
        Matcher m = getOrgFiles.matcher(master);
        HashMap<String, String> allOrgFiles = new HashMap<String, String>();
        while (m.find()) {
            Log.i(LT, "Found org file: " + m.group(2));
            allOrgFiles.put(m.group(2), m.group(1));
        }

        return allOrgFiles;
    }

    private HashMap<String, String> getChecksums(String master) {
        HashMap<String, String> chksums = new HashMap<String, String>();
        for (String eachLine : master.split("[\\n\\r]+")) {
            if (TextUtils.isEmpty(eachLine))
                continue;
            String[] chksTuple = eachLine.split("\\s+");
            chksums.put(chksTuple[1], chksTuple[0]);
        }
        return chksums;
    }

    private ArrayList<HashMap<String, Boolean>> getTodos(String master) {
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

    public void connect() {
        String[] keys = getKeys();
        if (keys != null) {
        	setLoggedIn(true);
        	Log.i(LT, "Logged in to Dropbox already");
        } else {
        	setLoggedIn(false);
        	Log.i(LT, "Not logged in to Dropbox");
        }

        if (!authenticate()) {
            Log.e(LT,"Could not authenticate with Dropbox");
        }
    }

    public void showToast(String msg) {
        Toast error = Toast.makeText(this.rootActivity, msg, Toast.LENGTH_LONG);
        error.show();
    }

    /**
     * This handles authentication if the user's token & secret
     * are stored locally, so we don't have to store user-name & password
     * and re-send every time.
     */
    protected boolean authenticate() {
    	if (dbConfig == null) {
    		dbConfig = getConfig();
    	}
    	String keys[] = getKeys();
    	if (keys != null) {
	        dbConfig = api.authenticateToken(keys[0], keys[1], dbConfig);
	        if (dbConfig != null) {
	            return true;
	        }
    	}
    	showToast("Failed user authentication for stored login tokens.");
    	setLoggedIn(false);
    	return false;
    }
    
    protected Config getConfig() {
    	if (dbConfig == null) {
	    	dbConfig = api.getConfig(null, false);
	    	dbConfig.consumerKey=r.getString(R.string.dropbox_consumer_key, "invalid");
	    	dbConfig.consumerSecret=r.getString(R.string.dropbox_consumer_secret, "invalid");
	    	dbConfig.server="api.dropbox.com";
	    	dbConfig.contentServer="api-content.dropbox.com";
	    	dbConfig.port=80;
    	}
    	return dbConfig;
    }
    
    public void setConfig(Config conf) {
    	dbConfig = conf;
    }
    
    /**
     * Shows keeping the access keys returned from Trusted Authenticator in a local
     * store, rather than storing user name & password, and re-authenticating each
     * time (which is not to be done, ever).
     * 
     * @return Array of [access_key, access_secret], or null if none stored
     */
    public String[] getKeys() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(
                                          this.rootActivity.getBaseContext());
        String key = prefs.getString("dbPrivKey", null);
        String secret = prefs.getString("dbPrivSecret", null);
        if (key != null && secret != null) {
        	String[] ret = new String[2];
        	ret[0] = key;
        	ret[1] = secret;
        	return ret;
        } else {
        	return null;
        }
    }
}
