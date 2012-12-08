package com.matburt.mobileorg.Gui.Wizard;

import java.util.ArrayList;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
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

public class WizardActivity extends Activity {

	private final class UIHandler extends Handler {
		public static final int DISPLAY_UI_TOAST = 0;

		// public static final int DISPLAY_UI_DIALOG = 1;

		public UIHandler(Looper looper) {
			super(looper);
		}

		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case UIHandler.DISPLAY_UI_TOAST: {
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

	static String TAG = "WizardActivity";

	// container
	PageFlipView wizard;
	// page 1 variables
	String syncSource;
	int syncWebDav, syncDropBox, syncUbuntuOne, syncSdCard, syncNull, syncSSH;
	RadioGroup syncGroup;
	// page 2 variables
	View loginPage;
	boolean loginAdded = false;
	Button loginButton;
	ProgressDialog progress;
	// dropbox variables
	DropboxAPI<AndroidAuthSession> dropboxApi;
	TextView dropboxAccountInfo;
	boolean dropboxLoginAttempted = false;
	boolean isLoggedIn = false;
	ArrayAdapter<String> dropboxFolders;
	// ubuntuone variables
	EditText ubuntuoneEmail;
	EditText ubuntuonePass;
	// sd card variables
	DirectoryBrowser directory;
	FolderAdapter directoryAdapter;
	String indexFilePath = "";
	// webdav variables
	EditText webdavUser;
	EditText webdavPass;
	EditText webdavUrl;
	Button webdavLoginButton;
	// ssh variables
	EditText sshUser;
	EditText sshPass;
	EditText sshPath;
	EditText sshHost;
	EditText sshPort;
	Button sshLoginButton;
	// page 3 variables
	ListView folderList;
	ArrayList<String> folders;
	Button doneButton;
	String dropboxPath = "";

	UIHandler uiHandler;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		OrgUtils.setTheme(this);
		super.onCreate(savedInstanceState);
		setContentView(R.layout.wizard);
		wizard = (PageFlipView) findViewById(R.id.wizard_parent);
		// setup page 1
		// get ids and pointers to sync radio buttons
		syncGroup = (RadioGroup) findViewById(R.id.sync_group);
		syncWebDav = ((RadioButton) findViewById(R.id.sync_webdav)).getId();
		syncDropBox = ((RadioButton) findViewById(R.id.sync_dropbox)).getId();
		syncUbuntuOne = ((RadioButton) findViewById(R.id.sync_ubuntuone))
				.getId();
		syncSdCard = ((RadioButton) findViewById(R.id.sync_sdcard)).getId();
		syncNull = ((RadioButton) findViewById(R.id.sync_null)).getId();
		syncSSH = ((RadioButton) findViewById(R.id.sync_ssh)).getId();
		syncGroup.clearCheck();
		syncGroup.setOnCheckedChangeListener(new Page1Listener());
		// setup dropbox
		Resources r = getResources();
		// setup progress dialog
		progress = new ProgressDialog(this);
		progress.setMessage(getString(R.string.please_wait));
		progress.setTitle(getString(R.string.signing_in));
		// when wizard first starts can't go to next page
		wizard.setNavButtonStateOnPage(0, false, PageFlipView.FIRST_PAGE);
	}

	/**
	 * Upon being resumed we can retrieve the current state. This allows us to
	 * update the state if it was changed at any time while paused.
	 */
	@Override
	protected void onResume() {
		super.onResume();
		// wizard.restoreLastPage();

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
				wizard.enablePage(1);
			} catch (IllegalStateException e) {
				showToast(String.format("Login failed: %s", e.toString()));
			}
		}
	}

	/**
	 * Any time we are paused we need to save away the current state, so it will
	 * be restored correctly when we are resumed.
	 */
	@Override
	protected void onPause() {
		super.onPause();
		wizard.saveCurrentPage();
	}

	class Page1Listener implements RadioGroup.OnCheckedChangeListener {

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
			wizard.enablePage(0);
			// debug
			// createDropboxList();
			// wizard.enablePage(1);
		}
	}

	void createNullConfig() {
		wizard.removePagesAfter(1);
		wizard.addPage(R.layout.wizard_null);
		doneButton = (Button) findViewById(R.id.wizard_done_button);
		wizard.setNavButtonStateOnPage(1, true, PageFlipView.LAST_PAGE);
		wizard.setDoneButtonOnClickListener(new FinishWizardButtonListener());
		wizard.enablePage(1);
	}

	void createSSHConfig() {
		wizard.removePagesAfter(1);
		wizard.addPage(R.layout.wizard_ssh);
		sshUser = (EditText) wizard.findViewById(R.id.wizard_ssh_username);
		sshPass = (EditText) wizard.findViewById(R.id.wizard_ssh_password);
		sshPath = (EditText) wizard.findViewById(R.id.wizard_ssh_path);
		sshHost = (EditText) wizard.findViewById(R.id.wizard_ssh_host);
		sshPort = (EditText) wizard.findViewById(R.id.wizard_ssh_port);
		webdavLoginButton = (Button) wizard
				.findViewById(R.id.wizard_ssh_login_button);
		doneButton = (Button) findViewById(R.id.wizard_done_button);
		webdavLoginButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				loginSSH();
			}
		});
		wizard.setNavButtonStateOnPage(1, true, PageFlipView.LAST_PAGE);
		wizard.setDoneButtonOnClickListener(new FinishWizardButtonListener());
		wizard.enablePage(1);
	}

	void createWebDAVConfig() {
		wizard.removePagesAfter(1);
		wizard.addPage(R.layout.wizard_webdav);
		webdavUser = (EditText) wizard
				.findViewById(R.id.wizard_webdav_username);
		webdavPass = (EditText) wizard
				.findViewById(R.id.wizard_webdav_password);
		webdavUrl = (EditText) wizard.findViewById(R.id.wizard_webdav_url);
		webdavLoginButton = (Button) wizard
				.findViewById(R.id.wizard_webdav_login_button);
		doneButton = (Button) findViewById(R.id.wizard_done_button);
		webdavLoginButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				loginWebdav();
			}
		});
		wizard.setNavButtonStateOnPage(1, true, PageFlipView.LAST_PAGE);
		wizard.setDoneButtonOnClickListener(new FinishWizardButtonListener());
		wizard.enablePage(1);
	}

	void createSDcardFolderSelector() {
		wizard.removePagesAfter(1);
		// add folder listview to wizard
		wizard.addPage(R.layout.wizard_folder_pick_list);
		wizard.setNavButtonStateOnPage(1, true, PageFlipView.LAST_PAGE);
		wizard.setDoneButtonOnClickListener(new FinishWizardButtonListener());

		directory = new LocalDirectoryBrowser(this);
		directoryAdapter = new FolderAdapter(this, R.layout.folder_adapter_row,
				directory.listFiles());
		directoryAdapter
				.setDoneButton((Button) findViewById(R.id.wizard_done_button));
		directoryAdapter.setDirectoryBrowser(directory);

		folderList = (ListView) wizard.findViewById(R.id.wizard_folder_list);
		folderList.setAdapter(directoryAdapter);
		directoryAdapter.notifyDataSetChanged();
	}

	void createDropboxLogin() {
		wizard.removePagesAfter(1);
		// add login page to wizard
		wizard.addPage(R.layout.wizard_dropbox);
		// enable nav buttons on that page
		wizard.setNavButtonStateOnPage(1, true, PageFlipView.MIDDLE_PAGE);
		wizard.disableAllNextActions(1);
		dropboxAccountInfo = (TextView) findViewById(R.id.wizard_dropbox_accountinfo);

		AppKeyPair appKeys = new AppKeyPair(
				getString(R.string.dropbox_consumer_key),
				getString(R.string.dropbox_consumer_secret));
		AndroidAuthSession session = new AndroidAuthSession(appKeys,
				AccessType.DROPBOX);
		dropboxApi = new DropboxAPI<AndroidAuthSession>(session);

		loginButton = (Button) wizard
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

	void createUbuntuLogin() {
		wizard.removePagesAfter(1);
		// add login page to wizard
		wizard.addPage(R.layout.wizard_ubuntuone);
		// enable nav buttons on that page
		wizard.setNavButtonStateOnPage(1, true, PageFlipView.MIDDLE_PAGE);
		wizard.disableAllNextActions(1);
		// get references to login forms
		ubuntuoneEmail = (EditText) wizard
				.findViewById(R.id.wizard_ubuntu_email);
		ubuntuonePass = (EditText) wizard
				.findViewById(R.id.wizard_ubuntu_password);
		// setup listener for buttons
		loginButton = (Button) wizard
				.findViewById(R.id.wizard_ubuntu_login_button);
		loginButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				if (isLoggedIn) {
					// We're going to log out
					// dropbox.deauthenticate();
				} else {
					// Try to log in
					loginUbuntuOne();
				}
			}
		});
	}

	void loginSSH() {
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

	void loginWebdav() {
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

	void loginUbuntuOne() {
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
			wizard.enablePage(1);
			uos.getBaseUser();
			createUbuntuOneList();
		} else {
			showToastRemote("Login Failed");
		}

	}

	void showToast(String msg) {
		Toast error = Toast.makeText(this, msg, Toast.LENGTH_LONG);
		error.show();
	}

	protected void showToastRemote(String message) {
		Message msg = uiHandler.obtainMessage(UIHandler.DISPLAY_UI_TOAST);
		msg.obj = message;
		uiHandler.sendMessage(msg);
	}

	void shake(View b) {
		Animation shake = AnimationUtils.loadAnimation(this, R.anim.shake);
		b.startAnimation(shake);
	}

	void storeKeys(String key, String secret) {
		// Save the access key for later
		SharedPreferences prefs = PreferenceManager
				.getDefaultSharedPreferences(getBaseContext());
		Editor edit = prefs.edit();
		edit.putString("dbPrivKey", key);
		edit.putString("dbPrivSecret", secret);
		edit.commit();
	}

	void createDropboxList() {
		wizard.addPage(R.layout.wizard_folder_pick_list);
		wizard.enablePage(1);
		// enable nav buttons on that page
		wizard.setNavButtonStateOnPage(2, true, PageFlipView.LAST_PAGE);
		wizard.setDoneButtonOnClickListener(new FinishWizardButtonListener());
		// setup directory browser
		directory = new DropboxDirectoryBrowser(this,
				dropboxApi);
		// setup directory browser adapter
		directoryAdapter = new FolderAdapter(this, R.layout.folder_adapter_row,
				directory.listFiles());
		directoryAdapter
				.setDoneButton((Button) findViewById(R.id.wizard_done_button));
		// bind adapter to browser
		directoryAdapter.setDirectoryBrowser(directory);
		// bind adapter to listview
		folderList = (ListView) wizard.findViewById(R.id.wizard_folder_list);
		folderList.setAdapter(directoryAdapter);
		directoryAdapter.notifyDataSetChanged();
		// debug
		// TODO Technically, this should be an async task app may crash
		// when list of root items is very long and network connection
		// is slow
	}

	void createUbuntuOneList() {
		wizard.addPage(R.layout.wizard_folder_pick_list);
		wizard.enablePage(1);

		// enable nav buttons on that page
		wizard.setNavButtonStateOnPage(2, true, PageFlipView.LAST_PAGE);
		wizard.setDoneButtonOnClickListener(new FinishWizardButtonListener());

		// setup directory browser
		UbuntuOneSynchronizer uos = new UbuntuOneSynchronizer((Context) this);
		uos.getBaseUser();
		directory = new UbuntuOneDirectoryBrowser(this, uos);

		// setup directory browser adapter
		directoryAdapter = new FolderAdapter(this, R.layout.folder_adapter_row,
				directory.listFiles());
		directoryAdapter
				.setDoneButton((Button) findViewById(R.id.wizard_done_button));

		// bind adapter to browser
		directoryAdapter.setDirectoryBrowser(directory);

		// bind adapter to listview
		folderList = (ListView) wizard.findViewById(R.id.wizard_folder_list);
		folderList.setAdapter(directoryAdapter);
		directoryAdapter.notifyDataSetChanged();
		// debug
		// TODO Technically, this should be an async task app may crash
		// when list of root items is very long and network connection
		// is slow
	}

	class FolderListListener implements AdapterView.OnItemClickListener {
		@Override
		public void onItemClick(AdapterView<?> parent, View v, int position,
				long id) {
			// showToast(position+" "+folders.get(position));
			dropboxPath = folders.get(position);
			doneButton.setEnabled(true);
		}
	}

	class FinishWizardButtonListener implements View.OnClickListener {
		@Override
		public void onClick(View v) {
			// end wizard
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

	public void setDirectoryBrowser(DirectoryBrowser d) {
		directory = d;
	}

	public String getCheckedDirectory() {
		if (currentChecked == -1)
			return "";
		// (Toast.makeText(context, directory.getAbsolutePath(currentChecked),
		// Toast.LENGTH_LONG)).show();
		return directory.getAbsolutePath(currentChecked);
	}

	public void setDoneButton(Button b) {
		doneButton = b;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		View row = convertView;
		TextView folder = null;
		CheckBox check = null;
		if (row == null) {
			LayoutInflater inflater = LayoutInflater.from(context);
			row = inflater.inflate(R.layout.folder_adapter_row, parent, false);
			folder = (TextView) row.findViewById(R.id.folder);
			folder.setOnClickListener(new View.OnClickListener() {

				@Override
				public void onClick(View folder) {
					int position = (Integer) folder.getTag();
					// FolderAdapter.this.clear();
					directory.browseTo(position);
					currentChecked = -1;
					FolderAdapter.this.notifyDataSetChanged();
					doneButton.setEnabled(false);
				}
			});
			check = (CheckBox) row.findViewById(R.id.checkbox);
			check.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {

				public void onCheckedChanged(CompoundButton buttonView,
						boolean isChecked) {
					// update last checked position
					int position = (Integer) buttonView.getTag();
					if (isChecked)
						currentChecked = position;
					else if (currentChecked == position)
						currentChecked = -1;
					FolderAdapter.this.notifyDataSetChanged();
					if (isChecked)
						doneButton.setEnabled(true);
				}
			});
		}
		folder = (TextView) row.findViewById(R.id.folder);
		folder.setText(directory.getDirectoryName(position));
		folder.setTag(new Integer(position));
		check = (CheckBox) row.findViewById(R.id.checkbox);
		// disable the "Up one level" checkbox; otherwise make sure its enabled
		if (position == 0 && !directory.isCurrentDirectoryRoot())
			check.setEnabled(false);
		else
			check.setEnabled(true);
		check.setTag(new Integer(position));
		// set check state. only one can be checked
		boolean status = (currentChecked == position) ? true : false;
		check.setChecked(status);
		return (row);
	}
}
