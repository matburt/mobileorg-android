package com.matburt.mobileorg.Synchronizers;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

import com.dropbox.client.DropboxAPI;
import com.dropbox.client.DropboxAPI.Config;
import com.dropbox.client.DropboxAPI.FileDownload;
import com.matburt.mobileorg.R;
import com.matburt.mobileorg.Parsing.NodeWriter;

public class DropboxSynchronizer extends Synchronizer {
    private DropboxAPI dropboxAPI = new DropboxAPI();
    private com.dropbox.client.DropboxAPI.Config dropboxConfig;

    public DropboxSynchronizer(Context parentContext) {
    	super(parentContext);
        connect();
    }

    public boolean isConfigured() {
        if (this.appSettings.getString("dropboxPath", null) == null)
            return false;
        return true;
    }
    
    public void push() throws IOException {
        BufferedReader reader = this.getReadHandle(NodeWriter.ORGFILE);
		StringBuilder fileContents = new StringBuilder();
		String line;
		
		while ((line = reader.readLine()) != null) {
		    fileContents.append(line);
		    fileContents.append("\n");
		}
		
		this.appendDropboxFile(NodeWriter.ORGFILE, fileContents.toString());
    }

    public void pull() throws IOException {
        String indexFilePath = this.appSettings.getString("dropboxPath", "");
		if(!indexFilePath.startsWith("/")) {
			indexFilePath = "/" + indexFilePath;
		}
        String masterStr = this.fetchOrgFileString(indexFilePath);
        Log.i(LT, "Contents: " + masterStr);

		if (masterStr.equals("")) {
			throw new IOException(r.getString(R.string.error_file_not_found,
					indexFilePath), null);
		}
        
        HashMap<String, String> masterList = this.getOrgFilesFromMaster(masterStr);
        ArrayList<HashMap<String, Boolean>> todoLists = this.getTodos(masterStr);
        ArrayList<ArrayList<String>> priorityLists = this.getPriorities(masterStr);
        this.appdb.setTodoList(todoLists);
        this.appdb.setPriorityList(priorityLists);
        String pathActual = this.getRootPath();
 
        //Get checksums file
        masterStr = this.fetchOrgFileString(pathActual + "checksums.dat");
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
            this.fetchAndSaveOrgFile(pathActual + masterList.get(key),
                                     masterList.get(key));
            this.appdb.addOrUpdateFile(masterList.get(key),
                                       key,
                                       newChecksums.get(key));
        }
    }

	public BufferedReader fetchOrgFile(String orgPath) throws IOException {
		FileDownload fd = dropboxAPI.getFileStream("dropbox", orgPath, null);

		if (fd == null || fd.is == null) {
			throw new IOException(r.getString(R.string.dropbox_fetch_error,
					orgPath, "Error downloading file"), null);
		}

		return new BufferedReader(new InputStreamReader(fd.is));
	}

    
    private String getRootPath() {
        String dbPath = this.appSettings.getString("dropboxPath","");
        return dbPath.substring(0, dbPath.lastIndexOf("/")+1);
    }

    private void appendDropboxFile(String file, String content) throws IOException {
        String pathActual = getRootPath();
        String originalContent = fetchOrgFileString(pathActual + file);
        String newContent = "";
 
        if (originalContent.indexOf("{\"error\":") == -1)
            newContent = originalContent + "\n" + content;
        else
            newContent = content;

        this.removeFile(NodeWriter.ORGFILE);
        BufferedWriter writer = this.getWriteHandle(NodeWriter.ORGFILE);

        // Rewriting the mobileorg file with the contents on Dropbox is dangerous
        // but the api sucks and automatically uses the File object's name when figuring
        // out what to call the remote file
        writer.write(newContent);
        writer.close();

        File uploadFile = this.getFile(NodeWriter.ORGFILE);
        this.dropboxAPI.putFile("dropbox", pathActual, uploadFile);
        this.removeFile(NodeWriter.ORGFILE);
        // NOTE: Will need to download and compare file since dropbox api sucks and won't
        //       return the status code
        // if (something) {
        //     this.appdb.removeFile("mobileorg.org");
        //     if (storageMode.equals("internal") || storageMode == null) {
        //         this.rootContext.deleteFile("mobileorg.org");
        //     }
        //     else if (storageMode.equals("sdcard")) {
        //         File root = Environment.getExternalStorageDirectory();
        //         File morgDir = new File(root, "mobileorg");
        //         File morgFile = new File(morgDir, "mobileorg.org");
        //         morgFile.delete();
        //     }
        // }
    }

    private File getFile(String fileName) {
        String storageMode = this.appSettings.getString("storageMode", null);
        if (storageMode.equals("internal") || storageMode == null) {
            File morgFile = new File("/data/data/com.matburt.mobileorg/files", fileName);
            return morgFile;
        }
        else if (storageMode.equals("sdcard")) {
            File root = Environment.getExternalStorageDirectory();
            File morgDir = new File(root, "mobileorg");
            File morgFile = new File(morgDir, fileName);
            if (!morgFile.exists()) {
                Log.i(LT, "Did not find " + fileName + " file, not pushing.");
                return null;
            }
            return morgFile;
        }
        
        return null;
    }

    
    /**
     * This handles authentication if the user's token & secret
     * are stored locally, so we don't have to store user-name & password
     * and re-send every time.
     */
    private void connect() {
    	if (dropboxConfig == null) {
    		dropboxConfig = getConfig();
    	}
    	String keys[] = getKeys();
    	if (keys != null) {
	        dropboxConfig = dropboxAPI.authenticateToken(keys[0], keys[1], dropboxConfig);
	        if (dropboxConfig != null) {
	            return;
	        }
    	}
    	showToast("Failed user authentication for stored login tokens.  Go to 'Configure Synchronizer Settings' and make sure you are logged in");
    }
    
    
    private Config getConfig() {
    	if (dropboxConfig == null) {
	    	dropboxConfig = dropboxAPI.getConfig(null, false);
	    	dropboxConfig.consumerKey=r.getString(R.string.dropbox_consumer_key, "invalid");
	    	dropboxConfig.consumerSecret=r.getString(R.string.dropbox_consumer_secret, "invalid");
	    	dropboxConfig.server="api.dropbox.com";
	    	dropboxConfig.contentServer="api-content.dropbox.com";
	    	dropboxConfig.port=80;
    	}
    	return dropboxConfig;
    }
    
    /**
     * Shows keeping the access keys returned from Trusted Authenticator in a local
     * store, rather than storing user name & password, and re-authenticating each
     * time (which is not to be done, ever).
     * 
     * @return Array of [access_key, access_secret], or null if none stored
     */
    private String[] getKeys() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(
                                          this.rootContext.getApplicationContext());
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

    private void showToast(String msg) {
        Toast error = Toast.makeText(this.rootContext, msg, Toast.LENGTH_LONG);
        error.show();
    }
}
