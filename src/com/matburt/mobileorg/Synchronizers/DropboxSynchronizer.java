package com.matburt.mobileorg.Synchronizers;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

import com.dropbox.client2.DropboxAPI;
import com.dropbox.client2.DropboxAPI.DropboxInputStream;
import com.dropbox.client2.android.AndroidAuthSession;
import com.dropbox.client2.exception.DropboxException;
import com.dropbox.client2.exception.DropboxUnlinkedException;
import com.dropbox.client2.session.AccessTokenPair;
import com.dropbox.client2.session.AppKeyPair;
import com.dropbox.client2.session.Session.AccessType;
import com.matburt.mobileorg.R;
import com.matburt.mobileorg.util.FileUtils;
import com.matburt.mobileorg.util.OrgUtils;

public class DropboxSynchronizer implements SynchronizerInterface {

	private String remoteIndexPath;
	private String remotePath;

    private boolean isLoggedIn = false;
	 
	private DropboxAPI<AndroidAuthSession> dropboxApi;
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

    private AndroidAuthSession buildSession() {
        AppKeyPair appKeyPair = new AppKeyPair(context.getString(R.string.dropbox_consumer_key),
                                               context.getString(R.string.dropbox_consumer_secret));
        AndroidAuthSession session;

        String[] stored = getKeys();
        if (stored != null) {
            AccessTokenPair accessToken = new AccessTokenPair(stored[0], stored[1]);
            session = new AndroidAuthSession(appKeyPair, AccessType.DROPBOX, accessToken);
        } else {
            session = new AndroidAuthSession(appKeyPair, AccessType.DROPBOX);
        }

        return session;
    }

    public boolean isConfigured() {
        return isLoggedIn && !this.remoteIndexPath.equals("");
    }

    
    public void putRemoteFile(String filename, String contents) throws IOException {       
		FileUtils orgFile = new FileUtils(filename, context);
        BufferedWriter writer =  orgFile.getWriter();
        writer.write(contents);
        writer.close();
    
        File uploadFile = orgFile.getFile();
        FileInputStream fis = new FileInputStream(uploadFile);
        try {
            this.dropboxApi.putFileOverwrite(this.remotePath + filename, fis, uploadFile.length(), null);
        } catch (DropboxUnlinkedException e) {
            throw new IOException("Dropbox Authentication Failed, re-run setup wizard");
        } catch (DropboxException e) {
            throw new IOException("Uploading " + filename + " because: " + e.toString());
        } finally {
            if (fis != null) {
                try {
                    fis.close();
                } catch (IOException e) {}
            }
        }
    }

	public BufferedReader getRemoteFile(String filename) throws IOException {
		String filePath = this.remotePath + filename;
        try {
            DropboxInputStream is = dropboxApi.getFileStream(filePath, null);
            BufferedReader fileReader = new BufferedReader(new InputStreamReader(is));
            return fileReader;
        } catch (DropboxUnlinkedException e) {
            throw new IOException("Dropbox Authentication Failed, re-run setup wizard");
        } catch (DropboxException e) {
            throw new IOException("Fetching " + filename + ": " + e.toString());
        }
	}

    
    /**
     * This handles authentication if the user's token & secret
     * are stored locally, so we don't have to store user-name & password
     * and re-send every time.
     */
    private void connect() {

        AndroidAuthSession session = buildSession();
        dropboxApi = new DropboxAPI<AndroidAuthSession>(session);
        if (!dropboxApi.getSession().isLinked()) {
            isLoggedIn = false;
            Log.d("MobileOrg", "Dropbox account was unlinked...");
            //throw new IOException("Dropbox Authentication Failed, re-run setup wizard");
        }
        else {
            isLoggedIn = true;
        }
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
		final String u_msg = msg;
		final Handler mHandler = new Handler();
		final Runnable mRunPost = new Runnable() {
			public void run() {
				Toast.makeText(context, u_msg, Toast.LENGTH_LONG).show();
			}
		};

		new Thread() {
			public void run() {
				mHandler.post(mRunPost);
			}
		}.start();
	}


	@Override
	public void postSynchronize() {
	}

	@Override
	public boolean isConnectable() {
		return OrgUtils.isNetworkOnline(context);
	}
}
