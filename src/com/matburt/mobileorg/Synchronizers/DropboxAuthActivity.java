package com.matburt.mobileorg.Synchronizers;

import android.app.Activity;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.res.Resources;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import com.dropbox.client.DropboxAPI;
import com.dropbox.client.DropboxAPI.Config;
import com.matburt.mobileorg.R;


public class DropboxAuthActivity extends Activity implements OnClickListener {
    private static final String LT = "MobileOrg";
    private DropboxAPI api = new DropboxAPI();

    private TextView dbInfo;
    private EditText dbLogin;
    private EditText dbPassword;
    private Button dbSubmit;
    private Resources r;

    private boolean hasToken = false;
    private Config dbConfig;
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.androidauth);
        
        dbInfo = (TextView)findViewById(R.id.dbCurrentToken);
        dbLogin = (EditText)findViewById(R.id.dbAuthLogin);
        dbPassword = (EditText)findViewById(R.id.dbAuthPassword);
        dbSubmit = (Button)findViewById(R.id.dbAuthSubmit);
        dbSubmit.setOnClickListener(this);
        r = this.getResources();
        this.populateInfo();
    }

    public void onClick(View v) {
        if (v == this.dbSubmit) {
            if (this.hasToken) {
                // We're going to log out
                api.deauthenticate();
                clearKeys();
                setLoggedIn(false);
                dbInfo.setText("Not logged in");
            } else {
                // Try to log in
                getAccountInfo();
            }
        }
    }
        
    public void populateInfo() {
        String[] keys = getKeys();
        if (keys != null) {
        	setLoggedIn(true);
        	Log.i(LT, "Logged in to Dropbox already");
        } else {
        	setLoggedIn(false);
        	Log.i(LT, "Not logged in to Dropbox");
        }
        if (authenticate()) {
        	// We can query the account info already, since we have stored 
        	// credentials
        	getAccountInfo();
        }
    }

    /**
     * This lets us use the Dropbox API from the LoginAsyncTask
     */
    public DropboxAPI getAPI() {
    	return api;
    }

    /**
     * Convenience function to change UI state based on being logged in
     */
    public void setLoggedIn(boolean loggedIn) {
    	this.hasToken = loggedIn;
    	this.dbLogin.setEnabled(!loggedIn);
    	this.dbPassword.setEnabled(!loggedIn);
    	if (loggedIn) {
    		dbSubmit.setText("Log Out of Dropbox");
    	} else {
    		dbSubmit.setText("Log In to Dropbox");
    	}
    }

    public void showToast(String msg) {
        Toast error = Toast.makeText(this, msg, Toast.LENGTH_LONG);
        error.show();
    }
    
    private void getAccountInfo() {
    	if (api.isAuthenticated()) {
    		// If we're already authenticated, we don't need to get the login info
	        LoginAsyncTask login = new LoginAsyncTask(this, null, null, getConfig());
	        login.execute();    		
    	} else {
    	
	        String email = dbLogin.getText().toString();
	        if (email.length() < 5 || email.indexOf("@") < 0 || email.indexOf(".") < 0) {
	            showToast("Error, invalid e-mail");
	            return;
	        }
	
	        String password = dbPassword.getText().toString();
	        if (password.length() < 6) {
	            showToast("Error, password too short");
	            return;
	        }

	        // It's good to do Dropbox API (and any web API) calls in a separate thread,
	        // so we don't get a force-close due to the UI thread stalling.
	        LoginAsyncTask login = new LoginAsyncTask(this, email, password, getConfig());
	        login.execute();
    	}
    }

    /**
     * Displays some useful info about the account, to demonstrate
     * that we've successfully logged in
     * @param account
     */
    public void displayAccountInfo(DropboxAPI.Account account) {
    	if (account != null) {
    		String info = "Name: " + account.displayName + "\n" +
    			"E-mail: " + account.email + "\n" + 
    			"User ID: " + account.uid + "\n" +
    			"Quota: " + account.quotaQuota;
    		this.dbInfo.setText(info);
    	}
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
    	clearKeys();
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
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
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
    
    /**
     * Shows keeping the access keys returned from Trusted Authenticator in a local
     * store, rather than storing user name & password, and re-authenticating each
     * time (which is not to be done, ever).
     */
    public void storeKeys(String key, String secret) {
        // Save the access key for later
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
        Editor edit = prefs.edit();
        edit.putString("dbPrivKey", key);
        edit.putString("dbPrivSecret", secret);
        edit.commit();
    }
    
    public void clearKeys() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
        Editor edit = prefs.edit();
        edit.remove("dbPrivKey");
        edit.remove("dbPrivSecret");
        edit.commit();
    }    	
}

class LoginAsyncTask extends AsyncTask<Void, Void, Integer> {
    private static final String LT = "LoginAsyncTask";

    String mUser;
    String mPassword;
    String mErrorMessage="";
    DropboxAuthActivity mDropboxAuth;
    DropboxAPI.Config mConfig;
    DropboxAPI.Account mAccount;
    
    // Will just log in
    public LoginAsyncTask(DropboxAuthActivity act,
                          String user,
                          String password,
                          DropboxAPI.Config config) {
        super();

        mDropboxAuth = act;
        mUser = user;
        mPassword = password;
        mConfig = config;
    }

    @Override
    protected Integer doInBackground(Void... params) {
        try {
        	DropboxAPI api = mDropboxAuth.getAPI();
        	
        	int success = DropboxAPI.STATUS_NONE;
        	if (!api.isAuthenticated()) {
	            mConfig = api.authenticate(mConfig, mUser, mPassword);
	            mDropboxAuth.setConfig(mConfig);
            
	            success = mConfig.authStatus;

	            if (success != DropboxAPI.STATUS_SUCCESS) {
	            	return success;
	            }
        	}
        	mAccount = api.accountInfo();

        	if (!mAccount.isError()) {
        		return DropboxAPI.STATUS_SUCCESS;
        	} else {
        		Log.e(LT, "Account info error: " + mAccount.httpCode + " " + mAccount.httpReason);
        		return DropboxAPI.STATUS_FAILURE;
        	}
        } catch (Exception e) {
            Log.e(LT, "Error in logging in.", e);
            return DropboxAPI.STATUS_NETWORK_ERROR;
        }
    }

    @Override
    protected void onPostExecute(Integer result) {
        if (result == DropboxAPI.STATUS_SUCCESS) {
        	if (mConfig != null && mConfig.authStatus == DropboxAPI.STATUS_SUCCESS) {
            	mDropboxAuth.storeKeys(mConfig.accessTokenKey, mConfig.accessTokenSecret);
            	mDropboxAuth.setLoggedIn(true);
            	mDropboxAuth.showToast("Logged into Dropbox");
            }
        	if (mAccount != null) {
        		mDropboxAuth.displayAccountInfo(mAccount);
        	}
        } else {
        	if (result == DropboxAPI.STATUS_NETWORK_ERROR) {
        		mDropboxAuth.showToast("Network error: " + mConfig.authDetail);
        	} else {
        		mDropboxAuth.showToast("Unsuccessful login.");
        	}
        }
    }

}
