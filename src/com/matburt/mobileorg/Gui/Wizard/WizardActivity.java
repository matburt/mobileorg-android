package com.matburt.mobileorg.Gui.Wizard;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.preference.PreferenceManager;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.dropbox.client2.DropboxAPI;
import com.dropbox.client2.DropboxAPI.Account;
import com.dropbox.client2.android.AndroidAuthSession;
import com.dropbox.client2.exception.DropboxException;
import com.dropbox.client2.session.AccessTokenPair;
import com.dropbox.client2.session.AppKeyPair;
import com.dropbox.client2.session.Session.AccessType;
import com.matburt.mobileorg.R;
import com.matburt.mobileorg.Synchronizers.SSHSynchronizer;
import com.matburt.mobileorg.Synchronizers.UbuntuOneSynchronizer;
import com.matburt.mobileorg.Synchronizers.WebDAVSynchronizer;
import com.matburt.mobileorg.util.OrgUtils;

public class WizardActivity extends Activity implements RadioGroup.OnCheckedChangeListener {

	private final class UIHandler extends Handler {
		public static final int DISPLAY_UI_TOAST = 0;

		public UIHandler(Looper looper) {
			super(looper);
		}

		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case DISPLAY_UI_TOAST: {
				Context context = getApplicationContext();
				Toast t = Toast.makeText(context, (String) msg.obj,
						Toast.LENGTH_LONG);
				progress.dismiss();
				t.show();
			}
			default:
				break;
			}
		}
	}

	// container
	private WizardView wizardView;
	// page 1 variables
	private String syncSource;
	private int syncWebDav, syncDropBox, syncUbuntuOne, syncSdCard, syncNull, syncSSH;
	private RadioGroup syncGroup;
	// page 2 variables
	private Button loginButton;
	private ProgressDialog progress;
	// dropbox variables
	private DropboxAPI<AndroidAuthSession> dropboxApi;
	private TextView dropboxAccountInfo;
	private boolean dropboxLoginAttempted = false;
	private boolean isLoggedIn = false;
	// ubuntuone variables
	private EditText ubuntuoneEmail;
	private EditText ubuntuonePass;
	// sd card variables
	private DirectoryBrowser<?> directory;
	private FolderAdapter directoryAdapter;
	// webdav variables
	private EditText webdavUser;
	private EditText webdavPass;
	private EditText webdavUrl;
	private Button webdavLoginButton;
	// ssh variables
	private EditText sshUser;
	private EditText sshPass;
	private EditText sshPath;
	private EditText sshHost;
	private EditText sshPort;
	// page 3 variables
	private ListView folderList;

	private UIHandler uiHandler;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		OrgUtils.setTheme(this);
		super.onCreate(savedInstanceState);
		setContentView(R.layout.wizard);
		
		wizardView = (WizardView) findViewById(R.id.wizard_parent);
		// when wizard first starts can't go to next page
		wizardView.setNavButtonStateOnPage(0, false, WizardView.FIRST_PAGE);


		
		// setup page 1
		// get ids and pointers to sync radio buttons
		syncGroup = (RadioGroup) findViewById(R.id.sync_group);
		syncGroup.clearCheck();
		syncGroup.setOnCheckedChangeListener(this);

		
		syncWebDav = ((RadioButton) findViewById(R.id.sync_webdav)).getId();
		syncDropBox = ((RadioButton) findViewById(R.id.sync_dropbox)).getId();
		syncUbuntuOne = ((RadioButton) findViewById(R.id.sync_ubuntuone))
				.getId();
		syncSdCard = ((RadioButton) findViewById(R.id.sync_sdcard)).getId();
		syncNull = ((RadioButton) findViewById(R.id.sync_null)).getId();
		syncSSH = ((RadioButton) findViewById(R.id.sync_ssh)).getId();

		// setup progress dialog
		progress = new ProgressDialog(this);
		progress.setMessage(getString(R.string.please_wait));
		progress.setTitle(getString(R.string.signing_in));
	}



	@Override
	protected void onResume() {
		super.onResume();

		if (dropboxLoginAttempted
				&& dropboxApi.getSession().authenticationSuccessful()) {
			dropboxLoginAttempted = false;
			try {
				// MANDATORY call to complete auth.
				// Sets the access token on the session
				dropboxApi.getSession().finishAuthentication();
				AccessTokenPair tokens = dropboxApi.getSession()
						.getAccessTokenPair();
				storeKeys(tokens.key, tokens.secret);
				showToast("Logged in!");
				try {
					Account accountInfo = dropboxApi.accountInfo();
					dropboxAccountInfo.setText("User: "
							+ accountInfo.displayName + "; Id: "
							+ String.valueOf(accountInfo.uid));
				} catch (DropboxException e) {
				}
				loginButton.setEnabled(false);
				createDropboxList();
				wizardView.enablePage(1);
			} catch (IllegalStateException e) {
				showToast(String.format("Login failed: %s", e.toString()));
			}
		}
	}

	@Override
	protected void onPause() {
		super.onPause();
		wizardView.saveCurrentPage();
	}


	@Override
	public void onCheckedChanged(RadioGroup arg, int checkedId) {
		if (checkedId == syncWebDav) {
			syncSource = "webdav";
			createWebDAVConfig();
		} else if (checkedId == syncDropBox) {
			syncSource = "dropbox";
			createDropboxLogin();
		} else if (checkedId == syncUbuntuOne) {
			syncSource = "ubuntu";
			createUbuntuLogin();
		} else if (checkedId == syncSdCard) {
			syncSource = "sdcard";
			createSDcardFolderSelector();
		} else if (checkedId == syncSSH) {
			syncSource = "scp";
			createSSHConfig();
		} else if (checkedId == syncNull) {
			syncSource = "null";
			createNullConfig();
		}
		// allow scrolling to next page
		wizardView.enablePage(0);
	}

	private void setupDoneButton() {
		Button done = (Button) wizardView.findViewById(R.id.wizard_done_button);
		done.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				saveSettings();
				finish();
			}
		});
	}
	
	
	private void createNullConfig() {
		wizardView.removePagesAfter(1);
		wizardView.addPage(R.layout.wizard_null);
		wizardView.setNavButtonStateOnPage(1, true, WizardView.LAST_PAGE);
		wizardView.enablePage(1);
	}

	private void createSSHConfig() {
		wizardView.removePagesAfter(1);
		wizardView.addPage(R.layout.wizard_ssh);
		sshUser = (EditText) wizardView.findViewById(R.id.wizard_ssh_username);
		sshPass = (EditText) wizardView.findViewById(R.id.wizard_ssh_password);
		sshPath = (EditText) wizardView.findViewById(R.id.wizard_ssh_path);
		sshHost = (EditText) wizardView.findViewById(R.id.wizard_ssh_host);
		sshPort = (EditText) wizardView.findViewById(R.id.wizard_ssh_port);
		webdavLoginButton = (Button) wizardView
				.findViewById(R.id.wizard_ssh_login_button);
		webdavLoginButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				loginSSH();
			}
		});
		wizardView.setNavButtonStateOnPage(1, true, WizardView.LAST_PAGE);
		wizardView.enablePage(1);
	}

	private void createWebDAVConfig() {
		wizardView.removePagesAfter(1);
		wizardView.addPage(R.layout.wizard_webdav);
		webdavUser = (EditText) wizardView
				.findViewById(R.id.wizard_webdav_username);
		webdavPass = (EditText) wizardView
				.findViewById(R.id.wizard_webdav_password);
		webdavUrl = (EditText) wizardView.findViewById(R.id.wizard_webdav_url);
		webdavLoginButton = (Button) wizardView
				.findViewById(R.id.wizard_webdav_login_button);
		webdavLoginButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				loginWebdav();
			}
		});
		wizardView.setNavButtonStateOnPage(1, true, WizardView.LAST_PAGE);
		wizardView.enablePage(1);
	}

	private void createSDcardFolderSelector() {
		wizardView.removePagesAfter(1);
		wizardView.addPage(R.layout.wizard_folder_pick_list);
		wizardView.setNavButtonStateOnPage(1, true, WizardView.LAST_PAGE);

		directory = new LocalDirectoryBrowser(this);
		directoryAdapter = new FolderAdapter(this, R.layout.folder_adapter_row,
				directory.listFiles());
		directoryAdapter
				.setDoneButton((Button) findViewById(R.id.wizard_done_button));
		directoryAdapter.setDirectoryBrowser(directory);

		folderList = (ListView) wizardView.findViewById(R.id.wizard_folder_list);
		folderList.setAdapter(directoryAdapter);
		directoryAdapter.notifyDataSetChanged();
	}

	private void createDropboxLogin() {
		wizardView.removePagesAfter(1);
		wizardView.addPage(R.layout.wizard_dropbox);
		wizardView.setNavButtonStateOnPage(1, true, WizardView.MIDDLE_PAGE);
		wizardView.disableAllNextActions(1);
		dropboxAccountInfo = (TextView) findViewById(R.id.wizard_dropbox_accountinfo);

		AppKeyPair appKeys = new AppKeyPair(
				getString(R.string.dropbox_consumer_key),
				getString(R.string.dropbox_consumer_secret));
		AndroidAuthSession session = new AndroidAuthSession(appKeys,
				AccessType.DROPBOX);
		dropboxApi = new DropboxAPI<AndroidAuthSession>(session);

		loginButton = (Button) wizardView
				.findViewById(R.id.wizard_dropbox_login_button);
		loginButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				if (isLoggedIn) {
					dropboxApi.getSession().unlink();
					// need to clear the keys
				} else {
					dropboxLoginAttempted = true;
					dropboxApi.getSession().startAuthentication(
							WizardActivity.this);
				}
			}
		});
	}
	
	private void createDropboxList() {
		wizardView.addPage(R.layout.wizard_folder_pick_list);
		wizardView.enablePage(1);
		// enable nav buttons on that page
		wizardView.setNavButtonStateOnPage(2, true, WizardView.LAST_PAGE);
		
		
		// setup directory browser
		directory = new DropboxDirectoryBrowser(this, dropboxApi);
		// setup directory browser adapter
		directoryAdapter = new FolderAdapter(this, R.layout.folder_adapter_row,
				directory.listFiles());
		directoryAdapter
				.setDoneButton((Button) findViewById(R.id.wizard_done_button));
		// bind adapter to browser
		directoryAdapter.setDirectoryBrowser(directory);
		// bind adapter to listview
		folderList = (ListView) wizardView.findViewById(R.id.wizard_folder_list);
		folderList.setAdapter(directoryAdapter);
		directoryAdapter.notifyDataSetChanged();
		// TODO Technically, this should be an async task app may crash
		// when list of root items is very long and network connection
		// is slow
	}
	
	

	private void createUbuntuLogin() {
		wizardView.removePagesAfter(1);
		wizardView.addPage(R.layout.wizard_ubuntuone);
		wizardView.setNavButtonStateOnPage(1, true, WizardView.MIDDLE_PAGE);
		wizardView.disableAllNextActions(1);
		ubuntuoneEmail = (EditText) wizardView
				.findViewById(R.id.wizard_ubuntu_email);
		ubuntuonePass = (EditText) wizardView
				.findViewById(R.id.wizard_ubuntu_password);
		loginButton = (Button) wizardView
				.findViewById(R.id.wizard_ubuntu_login_button);
		loginButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				if (isLoggedIn == false) {
					loginUbuntuOne();
				}
			}
		});
	}

	
	
	private void loginSSH() {
		final String pathActual = sshPath.getText().toString();
		final String passActual = sshPass.getText().toString();
		final String userActual = sshUser.getText().toString();
		final String hostActual = sshHost.getText().toString();
		String portNumGiven = sshPort.getText().toString();
		int portNum;
		if (portNumGiven.trim().equals("")) {
			portNum = 22;
		} else {
			portNum = Integer.parseInt(portNumGiven);
		}
		final int portActual = portNum;

		final Context ctxt = this;
		progress.show();

		Thread uiThread = new HandlerThread("UIHandler");
		uiThread.start();
		uiHandler = new UIHandler(((HandlerThread) uiThread).getLooper());

		Thread loginThread = new Thread() {
			public void run() {
				SSHSynchronizer sds = new SSHSynchronizer(ctxt);
				String extra = sds.testConnection(pathActual, userActual,
						passActual, hostActual, portActual);
				if (extra != null) {
					showToastRemote("Login failed: " + extra);
					return;
				}
				showToastRemote("Login succeeded");
			}
		};
		loginThread.start();
	}

	private void loginWebdav() {
		final String urlActual = webdavUrl.getText().toString();
		final String passActual = webdavPass.getText().toString();
		final String userActual = webdavUser.getText().toString();
		final Context ctxt = this;
		progress.show();

		Thread uiThread = new HandlerThread("UIHandler");
		uiThread.start();
		uiHandler = new UIHandler(((HandlerThread) uiThread).getLooper());

		Thread loginThread = new Thread() {
			public void run() {
				WebDAVSynchronizer wds = new WebDAVSynchronizer(ctxt);
				String extra = wds.testConnection(urlActual, userActual,
						passActual);
				if (extra != null) {
					showToastRemote("Login failed: " + extra);
					return;
				}
				showToastRemote("Login succeeded");
			}
		};
		loginThread.start();
	}

	private void loginUbuntuOne() {
		final UbuntuOneSynchronizer uos = new UbuntuOneSynchronizer(
				(Context) this);
		uos.username = ubuntuoneEmail.getText().toString();
		uos.password = ubuntuonePass.getText().toString();

		// move this into another thread, so we don't get an ANR if the network
		// is unavailable
		Thread uiThread = new HandlerThread("UIHandler");
		uiThread.start();
		uiHandler = new UIHandler(((HandlerThread) uiThread).getLooper());
		showToast("Logging in, please wait");
		if (uos.login()) {
			showToastRemote("Login Successfull");
			loginButton.setEnabled(false);
			wizardView.enablePage(1);
			uos.getBaseUser();
			createUbuntuOneList();
		} else {
			showToastRemote("Login Failed");
		}
	}


	private void createUbuntuOneList() {
		wizardView.addPage(R.layout.wizard_folder_pick_list);
		wizardView.enablePage(1);

		wizardView.setNavButtonStateOnPage(2, true, WizardView.LAST_PAGE);

		UbuntuOneSynchronizer uos = new UbuntuOneSynchronizer((Context) this);
		uos.getBaseUser();
		directory = new UbuntuOneDirectoryBrowser(this, uos);

		directoryAdapter = new FolderAdapter(this, R.layout.folder_adapter_row,
				directory.listFiles());
		directoryAdapter
				.setDoneButton((Button) findViewById(R.id.wizard_done_button));

		directoryAdapter.setDirectoryBrowser(directory);

		folderList = (ListView) wizardView.findViewById(R.id.wizard_folder_list);
		folderList.setAdapter(directoryAdapter);
		directoryAdapter.notifyDataSetChanged();
		// debug
		// TODO Technically, this should be an async task app may crash
		// when list of root items is very long and network connection
		// is slow
	}
	

	private void showToast(String msg) {
		Toast error = Toast.makeText(this, msg, Toast.LENGTH_LONG);
		error.show();
	}

	private void showToastRemote(String message) {
		Message msg = uiHandler.obtainMessage(UIHandler.DISPLAY_UI_TOAST);
		msg.obj = message;
		uiHandler.sendMessage(msg);
	}


	private void storeKeys(String key, String secret) {
		// Save the access key for later
		SharedPreferences prefs = PreferenceManager
				.getDefaultSharedPreferences(getBaseContext());
		Editor edit = prefs.edit();
		edit.putString("dbPrivKey", key);
		edit.putString("dbPrivSecret", secret);
		edit.commit();
	}
	
	private void saveSettings() {
		SharedPreferences appSettings = PreferenceManager
				.getDefaultSharedPreferences(getBaseContext());
		SharedPreferences.Editor editor = appSettings.edit();
		editor.putString("syncSource", syncSource);
		if (syncSource.equals("webdav")) {
			editor.putString("webUrl", webdavUrl.getText().toString());
			editor.putString("webPass", webdavPass.getText().toString());
			editor.putString("webUser", webdavUser.getText().toString());
		} else if (syncSource.equals("scp")) {
			editor.putString("scpPath", sshPath.getText().toString());
			editor.putString("scpUser", sshUser.getText().toString());
			editor.putString("scpPass", sshPass.getText().toString());
			editor.putString("scpHost", sshHost.getText().toString());
			if (sshPort.getText().toString().trim().equals("")) {
				editor.putString("scpPort", "22");
			} else {
				editor.putString("scpPort", sshPort.getText().toString());
			}
		} else if (syncSource.equals("dropbox"))
			editor.putString("dropboxPath",
					directoryAdapter.getCheckedDirectory() + "/");
		else if (syncSource.equals("ubuntu"))
			editor.putString("ubuntuOnePath",
					directoryAdapter.getCheckedDirectory() + "/");
		else if (syncSource.equals("sdcard"))
			editor.putString("indexFilePath",
					directoryAdapter.getCheckedDirectory());
		editor.putString("storageMode", "sdcard");
		editor.commit();
	}
}
