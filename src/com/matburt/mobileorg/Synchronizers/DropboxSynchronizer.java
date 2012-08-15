package com.matburt.mobileorg.Synchronizers;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.widget.Toast;

import com.dropbox.client.DropboxAPI;
import com.dropbox.client.DropboxAPI.Config;
import com.dropbox.client.DropboxAPI.FileDownload;
import com.matburt.mobileorg.R;
import com.matburt.mobileorg.util.FileUtils;

public class DropboxSynchronizer implements SynchronizerInterface {

	private String remoteIndexPath;
	private String remotePath;
	 
	private DropboxAPI dropboxAPI = new DropboxAPI();
    private com.dropbox.client.DropboxAPI.Config dropboxConfig;
	private Context context;
    
    public DropboxSynchronizer(Context context) {
    	this.context = context;

		SharedPreferences sharedPreferences = PreferenceManager
				.getDefaultSharedPreferences(context);
    	
		this.remoteIndexPath = sharedPreferences.getString("dropboxPath", "");
		if (!this.remoteIndexPath.startsWith("/")) {
			this.remoteIndexPath = "/" + this.remoteIndexPath;
		}

		String dbPath = sharedPreferences.getString("dropboxPath","");
		this.remotePath = dbPath.substring(0, dbPath.lastIndexOf("/")+1);
    	
        connect();
    }


    public boolean isConfigured() {
        if (this.remoteIndexPath.equals(""))
            return false;
        return true;
    }

    
    public void putRemoteFile(String filename, String contents) throws IOException {
    	//this.appdb.removeFile(filename);
       
		FileUtils orgFile = new FileUtils(filename, context);
        BufferedWriter writer =  orgFile.getWriter();
        writer.write(contents);
        writer.close();
    
        File uploadFile = orgFile.getFile();
        this.dropboxAPI.putFile("dropbox", this.remotePath, uploadFile);
    }

	public BufferedReader getRemoteFile(String filename) throws IOException {
		String filePath = this.remotePath + filename;
		FileDownload fd = dropboxAPI.getFileStream("dropbox", filePath, null);

		if (fd == null || fd.is == null) {
			throw new IOException(context.getResources().getString(R.string.dropbox_fetch_error,
					filePath, "Error downloading file"));
		}

		BufferedReader fileReader = new BufferedReader(new InputStreamReader(fd.is));

		return fileReader;
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
	    	dropboxConfig.consumerKey = context.getResources().getString(R.string.dropbox_consumer_key, "invalid");
	    	dropboxConfig.consumerSecret = context.getResources().getString(R.string.dropbox_consumer_secret, "invalid");
	    	dropboxConfig.server = "api.dropbox.com";
	    	dropboxConfig.contentServer = "api-content.dropbox.com";
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
                                          this.context.getApplicationContext());
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
        Toast error = Toast.makeText(this.context, msg, Toast.LENGTH_LONG);
        error.show();
    }


	@Override
	public void postSynchronize() {
	}
}
