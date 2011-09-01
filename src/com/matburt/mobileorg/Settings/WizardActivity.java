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
import android.widget.AdapterView;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup.LayoutParams;
import android.view.MotionEvent;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
//import android.widget.AdapterView;
import com.matburt.mobileorg.Views.*;
import com.matburt.mobileorg.Dropbox.*;
import com.matburt.mobileorg.R;
import com.dropbox.client.DropboxAPI;
import java.util.ArrayList;
import android.widget.ListView;
//import android.widget.ArrayAdapter;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.view.LayoutInflater;
import android.widget.TextView;
import android.widget.CompoundButton;
import java.util.ArrayList;

//import java.util.Map.Entry;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;

public class WizardActivity extends Activity {
    static String TAG="WizardActivity";
    
    //container
    PageFlipView wizard;
    //page 1 variables
    String syncSource;
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
    ArrayAdapter<String> dropboxFolders;
    //sd card variables
	DirectoryBrowser directory;
	FolderAdapter directoryAdapter;
	String indexFilePath = "";
    //page 3 variables
    ListView folderList;
    ArrayList<String> folders;
    Button doneButton;
    String dropboxPath = "";

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
    	super.onCreate(savedInstanceState);
    	setContentView(R.layout.wizard);
    	wizard = (PageFlipView) findViewById(R.id.wizard_parent);
    	//setup page 1
    	//get ids and pointers to sync radio buttons
    	syncGroup = (RadioGroup) findViewById(R.id.sync_group);
    	syncWebDav = ( (RadioButton) 
    			findViewById(R.id.sync_webdav) ).getId();
    	syncDropBox = ( (RadioButton) 
    			findViewById(R.id.sync_dropbox) ).getId();
    	syncSdCard = ( (RadioButton) 
    			findViewById(R.id.sync_sdcard) ).getId();
    	syncGroup.clearCheck();
    	syncGroup.setOnCheckedChangeListener(new Page1Listener());
    	//setup dropbox
    	Resources r = getResources();
    	String key=r.getString(R.string.dropbox_consumer_key, "invalid");
    	String secret=r.getString(R.string.dropbox_consumer_secret, "invalid");
    	dropbox = new Dropbox(this, key, secret);
    	//setup dropbox progress dialog
    	progress = new ProgressDialog(this);
    	progress.setMessage(getString(R.string.please_wait));
    	progress.setTitle(getString(R.string.signing_in));
    	//when wizard first starts can't go to next page
    	wizard.setNavButtonStateOnPage(0, false, PageFlipView.FIRST_PAGE );
    }

    /**
     * Upon being resumed we can retrieve the current state.  This allows us
     * to update the state if it was changed at any time while paused.
     */
    @Override
    protected void onResume() {
        super.onResume();
	//wizard.restoreLastPage();
    }

    /**
     * Any time we are paused we need to save away the current state, so it
     * will be restored correctly when we are resumed.
     */
    @Override
    protected void onPause() {
        super.onPause();
	wizard.saveCurrentPage();
    }

    class Page1Listener implements RadioGroup.OnCheckedChangeListener {

	@Override
	    public void onCheckedChanged(RadioGroup arg, int checkedId) {
	    if ( checkedId == syncWebDav )
	    	syncSource = "webdav";
	    else if ( checkedId == syncDropBox ) {
	    	syncSource = "dropbox";
	    	createDropboxLogin();
	    }
	    else if ( checkedId == syncSdCard) {
	    	syncSource = "sdcard";
	    	createSDcardFolderSelector();
	    }
	    //allow scrolling to next page
	    wizard.enablePage( 0 );
	    //debug
	    //createDropboxList();
	    //wizard.enablePage(1);
	}
    }

    void createSDcardFolderSelector() {
	wizard.removePagesAfter( 1 );
	//add folder listview to wizard
	wizard.addPage( R.layout.wizard_folder_pick_list );
	//enable nav buttons on that page
	wizard.setNavButtonStateOnPage(1,true,PageFlipView.LAST_PAGE);
	wizard.setDoneButtonOnClickListener(new FinishWizardButtonListener());
	//setup directory browser
	directory = new DirectoryBrowser.LocalDirectoryBrowser(this);
	//setup directory browser adapter
	directoryAdapter = new FolderAdapter( this, R.layout.folder_adapter_row, directory.list() );
	directoryAdapter.setDoneButton( (Button) findViewById(R.id.wizard_done_button) );
	//bind adapter to browser
	directoryAdapter.setDirectoryBrowser( directory );
	//bind adapter to listview
	folderList = (ListView) wizard.findViewById(R.id.wizard_folder_list);
	folderList.setAdapter( directoryAdapter );
	directoryAdapter.notifyDataSetChanged();
    }
    
    void createDropboxLogin() {
	wizard.removePagesAfter( 1 );
	//add login page to wizard
	wizard.addPage( R.layout.wizard_dropbox );
	//enable nav buttons on that page
	wizard.setNavButtonStateOnPage(1, true, PageFlipView.MIDDLE_PAGE);
	wizard.disableAllNextActions( 1 );
	//get references to login forms
    	dropboxEmail = (EditText) wizard
    	    .findViewById(R.id.wizard_dropbox_email);
    	dropboxPass = (EditText) wizard
    	    .findViewById(R.id.wizard_dropbox_password);
    	//setup listener for buttons
    	loginButton = (Button) wizard
    	    .findViewById(R.id.wizard_dropbox_login_button);
    	loginButton.setOnClickListener(new OnClickListener() {
            @Override
    		public void onClick(View v) {
            	if (isLoggedIn) {
    		    // We're going to log out
    		    dropbox.deauthenticate();
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

    //convience function
    void showToast(String msg) {
        Toast error = Toast.makeText(this, msg, Toast.LENGTH_LONG);
        error.show();
    }

    //ditto
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
			storeKeys(dropbox.getConfig().accessTokenKey, 
				  dropbox.getConfig().accessTokenSecret);
			createDropboxList();
			//allow scrolling to next page
			wizard.enablePage( 1 );
			//setLoggedIn(true);		
			//displayAccountInfo(mDropbox.accountInfo());
		}
    	
    };

    void storeKeys(String key, String secret) {
        // Save the access key for later
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
        Editor edit = prefs.edit();
        edit.putString("dbPrivKey", key);
        edit.putString("dbPrivSecret", secret);
        edit.commit();
    }

    void createDropboxList() {
	wizard.addPage( R.layout.wizard_folder_pick_list );
	wizard.enablePage( 1 );
	//enable nav buttons on that page
	wizard.setNavButtonStateOnPage(2, true, PageFlipView.LAST_PAGE);
	wizard.setDoneButtonOnClickListener(new FinishWizardButtonListener());
	//setup directory browser
	directory = new DirectoryBrowser.DropboxDirectoryBrowser(this,dropbox);
	//setup directory browser adapter
	directoryAdapter = new FolderAdapter( this, R.layout.folder_adapter_row, directory.list() );
	directoryAdapter.setDoneButton( (Button) findViewById(R.id.wizard_done_button) );
	//bind adapter to browser
	directoryAdapter.setDirectoryBrowser( directory );
	//bind adapter to listview
	folderList = (ListView) wizard.findViewById(R.id.wizard_folder_list);
	folderList.setAdapter( directoryAdapter );
	directoryAdapter.notifyDataSetChanged();
	//debug
        //TODO Technically, this should be an async task app may crash
	//when list of root items is very long and network connection
	//is slow
    }

    class FolderListListener 
	implements AdapterView.OnItemClickListener {
	@Override
	    public void onItemClick(AdapterView<?> parent,
				       View v, int position, long id) {
	    //showToast(position+" "+folders.get(position));
	    dropboxPath = folders.get(position);
	    doneButton.setEnabled(true);
	}
    }

    class FinishWizardButtonListener implements View.OnClickListener {
	@Override
	    public void onClick(View v) {
	    //end wizard
	    SharedPreferences appSettings = 
		PreferenceManager.getDefaultSharedPreferences(getBaseContext());
	    SharedPreferences.Editor editor = appSettings.edit();
	    editor.putString("syncSource", syncSource);
	    if ( syncSource.equals("webdav") ) ;
	    else if ( syncSource.equals("dropbox") )
		    editor.putString("dropboxPath", directoryAdapter.getCheckedDirectory() );
	    else if ( syncSource.equals("sdcard") ) 
		    editor.putString("indexFilePath", directoryAdapter.getCheckedDirectory() );
	    editor.putString("storageMode", "sdcard");
	    editor.commit();
	    finish();
	}
    }
}

class FolderAdapter extends ArrayAdapter<String> {
	int currentChecked = -1;
	DirectoryBrowser directory;
	Context context;
	Button doneButton;
	
	FolderAdapter(Context context, int resource, ArrayList<String> list) {
		super(context, resource, list);
		this.context = context;
	}

	public void setDirectoryBrowser(DirectoryBrowser d) { directory = d; }
	
	public String getCheckedDirectory() {
		if ( currentChecked == -1 ) return "";
		//(Toast.makeText(context, directory.getAbsolutePath(currentChecked), Toast.LENGTH_LONG)).show();
		return directory.getAbsolutePath(currentChecked);
	}
	
	public void setDoneButton(Button b) {
		doneButton = b;
	}
	
	@Override
	public View getView(int position, View convertView,
			    ViewGroup parent) {
	    View row=convertView;
	    TextView folder=null;
	    CheckBox check=null;
	    if (row==null) {
		LayoutInflater inflater=LayoutInflater.from(context);
		row=inflater.inflate(R.layout.folder_adapter_row, parent, false);
		folder=(TextView)row.findViewById(R.id.folder);
		folder.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View folder) {
				int position = (Integer) folder.getTag();
				//FolderAdapter.this.clear();
				directory.browseTo(position);
				currentChecked = -1;
				FolderAdapter.this.notifyDataSetChanged();
				doneButton.setEnabled(false);
			}
		});
		check=(CheckBox)row.findViewById(R.id.checkbox);
		check.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {

			public void onCheckedChanged(CompoundButton buttonView, 
					      boolean isChecked) {
			    //update last checked position
			    int position = (Integer) buttonView.getTag();
			    if ( isChecked ) currentChecked = position;
			    else if ( currentChecked == position ) 
				currentChecked = -1;
			    FolderAdapter.this.notifyDataSetChanged();
			    if ( isChecked ) doneButton.setEnabled(true);
			}
		    });
	    }
	    folder=(TextView)row.findViewById(R.id.folder);
	    folder.setText( directory.get(position) );
	    folder.setTag(new Integer(position));
	    check=(CheckBox)row.findViewById(R.id.checkbox);
	    // disable the "Up one level" checkbox; otherwise make sure its enabled
	    if ( position == 0 && !directory.isCurrentDirectoryRoot() ) 
		    check.setEnabled(false);
	    else check.setEnabled(true);
	    check.setTag(new Integer(position));
	    //set check state. only one can be checked
	    boolean status = ( currentChecked == position )
		? true : false ;
	    check.setChecked( status );
	    return(row);
	}
}