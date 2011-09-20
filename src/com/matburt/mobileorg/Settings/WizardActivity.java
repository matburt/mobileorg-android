package com.matburt.mobileorg.Settings;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.res.Resources;
import android.content.SharedPreferences.Editor;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup.LayoutParams;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.ScrollView;
import android.widget.Toast;
import com.matburt.mobileorg.Dropbox.*;
import com.matburt.mobileorg.R;
import com.dropbox.client.DropboxAPI;
import java.util.ArrayList;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;

public class WizardActivity extends Activity {
    static String TAG="WizardActivity";
    
    //container
    PageFlipView wizard;
    //page 1 variables
    int syncWebDav, syncDropBox, syncSdCard;
    RadioGroup syncGroup; 
    //page 2 variables
    View loginPage;
    boolean loginAdded=false;
    Button loginButton;
    ProgressDialog progress;
    //dropbox variables
    Dropbox dropbox;
    EditText dropboxEmail;
    EditText dropboxPass;
    boolean isLoggedIn=false;

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.wizard);
	wizard = (PageFlipView) findViewById(R.id.wizard_parent);
	//setup page 1
	// PageView page1Container = (PageView) findViewById(R.id.wizard_page1);
	// LayoutInflater inflater=
	//     (LayoutInflater) getApplicationContext()
	//     .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
	// View page1=inflater.inflate(R.layout.wizard_page1,page1Container);
	//get ids and pointers to sync radio buttons
	syncGroup = (RadioGroup) findViewById(R.id.sync_group);
	syncWebDav = ( (RadioButton) 
			findViewById(R.id.sync_webdav) ).getId();
	syncDropBox = ( (RadioButton) 
			findViewById(R.id.sync_dropbox) ).getId();
	syncSdCard = ( (RadioButton) 
			findViewById(R.id.sync_sdcard) ).getId();
	syncGroup.clearCheck();
	//setup click listener for sync radio group
	syncGroup.setOnCheckedChangeListener(new Page1Listener());
	//setup dropbox
	Resources r = getResources();
	String key=r.getString(R.string.dropbox_consumer_key, "invalid");
	String secret=r.getString(R.string.dropbox_consumer_secret, "invalid");
	dropbox = new Dropbox(this, key, secret);
	//setup progress dialog
	progress = new ProgressDialog(this);
        progress.setMessage("Please wait...");
        progress.setTitle("Signing in");
	//disable all page swypes
	wizard.disableAllNavButtons();
    }

    /**
     * Upon being resumed we can retrieve the current state.  This allows us
     * to update the state if it was changed at any time while paused.
     */
    @Override
    protected void onResume() {
        super.onResume();
	//Log.d(TAG,"onResume: loading... "+String.valueOf(wizard.getCurrentPage()));
	wizard.restoreLastPage();
	//Log.d(TAG,"onResume: done... "+String.valueOf(wizard.getCurrentPage()));
    }

    /**
     * Any time we are paused we need to save away the current state, so it
     * will be restored correctly when we are resumed.
     */
    @Override
    protected void onPause() {
        super.onPause();
	//Log.d(TAG,"saving state>>>>>>>>>>>>>>>>>>>>>");
	//Log.d(TAG,"onPause: "+String.valueOf(wizard.getCurrentPage()));
	wizard.saveCurrentPage();
    }

    class Page1Listener implements RadioGroup.OnCheckedChangeListener {

	@Override
	    public void onCheckedChanged(RadioGroup arg, int checkedId) {
	    SharedPreferences appSettings = 
		PreferenceManager.getDefaultSharedPreferences(getBaseContext());
	    SharedPreferences.Editor editor = appSettings.edit();
	    if ( checkedId == syncWebDav )
		editor.putString("syncSource", "webdav");
	    else if ( checkedId == syncDropBox ) {
		//editor.putString("syncSource", "dropbox");
		createDropboxLogin();
	    }
	    else if ( checkedId == syncSdCard)
		editor.putString("syncSource", "sdcard");
	    editor.commit();
	    //unfreeze first page
	    wizard.setNavButtonState( true, 0 );
	}
    }
    
    void createDropboxLogin() {
	ViewGroup page2 = (ViewGroup) 
	    findViewById(R.id.wizard_page2_container); //parent scrollview
	page2 = (ViewGroup) page2.getChildAt(0); //linearlayout
	LayoutInflater inflater=
	    (LayoutInflater) LayoutInflater.from(getApplicationContext());
	loginPage = inflater.inflate(R.layout.wizard_dropbox,
				     null);
	//remove current page 2 and re-add dropbox login screen
	if ( loginAdded ) page2.removeViewAt(0);
	page2.addView(loginPage, 0);
	loginAdded = true;
	//get references to login forms
	dropboxEmail = (EditText) page2
	    .findViewById(R.id.wizard_dropbox_email);
	dropboxPass = (EditText) page2
	    .findViewById(R.id.wizard_dropbox_password);
	//setup listener for buttons
	loginButton = (Button) page2
	    .findViewById(R.id.wizard_dropbox_login_button);
	loginButton.setOnClickListener(new OnClickListener() {
            @Override
		public void onClick(View v) {
            	if (isLoggedIn) {
		    // We're going to log out
		    dropbox.deauthenticate();
		    //setLoggedIn(false);
		    //mText.setText("");
            	} else {
		    // Try to log in
		    loginDropbox();
            	}
            }
	    });
	//debug
	dropboxEmail.setText("uri@frankandrobot.com");
    }

    void loginDropbox() {
	if (dropbox.isAuthenticated()) {
	    // If we're already authenticated, we don't need to get
	    // the login info
	    progress.show();
	    dropbox.login(dropboxListener);
    	} 
	else {
	    String email = dropboxEmail.getText().toString();
	    if (email.length() < 5 
		|| email.indexOf("@") < 0 
		|| email.indexOf(".") < 0) {
		shake(dropboxEmail);
		dropboxEmail.requestFocus();
		showToast("Invalid e-mail");
		return;
	    }
	    String password = dropboxPass.getText().toString();
	    if (password.length() < 6) {
		shake(dropboxPass);
		dropboxPass.requestFocus();
		showToast("Password too short");
		return;
	    }
	    // It's good to do Dropbox API (and any web API) calls
	    // in a separate thread, so we don't get a force-close
	    // due to the UI thread stalling.
	    progress.show();
	    dropbox.login(dropboxListener, email, password);
	}
    }

    void showToast(String msg) {
        Toast error = Toast.makeText(this, msg, Toast.LENGTH_LONG);
        error.show();
    }

    void shake(View b) {
	Animation shake = AnimationUtils
	    .loadAnimation(this, R.anim.shake);
	b.startAnimation(shake);
    }

    /**
     * Notifies our Activity when a login process succeeded or failed.
     */
    DropboxLoginListener dropboxListener = new DropboxLoginListener() {

		@Override
		public void loginFailed(String message) {
			progress.dismiss();
			showToast("Login failed: "+message);
			//shake buttons
			shake(dropboxPass);
			shake(dropboxEmail);
			//setLoggedIn(false);
		}

		@Override
		public void loginSuccessfull() {
			progress.dismiss();
			showToast("Logged in!");
			loginButton.setEnabled(false);
			//setLoggedIn(true);		
			//displayAccountInfo(mDropbox.accountInfo());
		}
    	
    };
}
