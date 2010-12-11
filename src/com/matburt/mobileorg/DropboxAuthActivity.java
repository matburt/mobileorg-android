import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.dropbox.client.DropboxAPI;
import com.dropbox.client.DropboxAPI.Config;


public class DropboxAuthActivity extends Activity {
    private static final String LT = "MobileOrg";
    private DropboxAPI api = new DropboxAPI();

    private TextView dbInfo;
    private EditText dbLogin;
    private EditText dbPassword;
    private Button dbSubmit;

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
    }

    public void onClick(View v) {
        if (v == this.dbSubmit) {
            if (this.hasToken) {
                // We're going to log out
                api.deauthenticate();
                clearKeys();
                setLoggedIn(false);
                mText.setText("");
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
        	Log.i(TAG, "Logged in already");
        } else {
        	setLoggedIn(false);
        	Log.i(TAG, "Not logged in");
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
    		mSubmit.setText("Log Out of Dropbox");
    	} else {
    		mSubmit.setText("Log In to Dropbox");
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
	        dbConfig = api.authenticateToken(keys[0], keys[1], mConfig);
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
	    	// TODO On a production app which you distribute, your consumer
	    	// key and secret should be obfuscated somehow.
	    	dbConfig.consumerKey=R.string.dropbox_consumer_key;
	    	dbConfig.consumerSecret=R.string.dropbox_consumer_secret;
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
        SharedPreferences prefs = getDefaultSharedPreferences(getBaseContext());
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
        SharedPreferences prefs = getDefaultSharedPreferences(getBaseContext());
        Editor edit = prefs.edit();
        edit.putString("dbPrivKey", key);
        edit.putString("dbPrivSecret", secret);
        edit.commit();
    }
    
    public void clearKeys() {
        SharedPreferences prefs = getDefaultSharedPreferences(getBaseContext());
        Editor edit = prefs.edit();
        edit.remove("dbPrivKey");
        edit.remove("dbPrivSecret");
        edit.commit();
    }    	
}