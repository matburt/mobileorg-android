package com.matburt.mobileorg.Gui;

import java.io.BufferedReader;
import java.io.File;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;

import android.app.Activity;
import android.app.Dialog;
import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import com.matburt.mobileorg.MobileOrgApplication;
import com.matburt.mobileorg.R;
import com.matburt.mobileorg.Capture.Capture;
import com.matburt.mobileorg.Capture.ViewNodeDetailsActivity;
import com.matburt.mobileorg.Error.ErrorReporter;
import com.matburt.mobileorg.Error.ReportableError;
import com.matburt.mobileorg.Parsing.Encryption;
import com.matburt.mobileorg.Parsing.MobileOrgDatabase;
import com.matburt.mobileorg.Parsing.Node;
import com.matburt.mobileorg.Parsing.OrgFileParser;
import com.matburt.mobileorg.Settings.SettingsActivity;
import com.matburt.mobileorg.Synchronizers.DropboxSynchronizer;
import com.matburt.mobileorg.Synchronizers.SDCardSynchronizer;
import com.matburt.mobileorg.Synchronizers.Synchronizer;
import com.matburt.mobileorg.Synchronizers.WebDAVSynchronizer;

public class OutlineActivity extends ListActivity
{
	@SuppressWarnings("unused")
	private static final String LT = "MobileOrg";

	private static final int RUNFOR_EXPAND = 1;
	private static final int RUNFOR_PARSER = 3;
	private int displayIndex;
	private ProgressDialog syncDialog;
	private MobileOrgDatabase appdb;
	private ReportableError syncError;
	private Dialog newSetupDialog;
	private boolean newSetupDialog_shown = false;
	public SharedPreferences appSettings;
	final Handler syncHandler = new Handler();
	private ArrayList<Integer> origSelection = null;
	MobileOrgApplication appInst = null;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		this.appdb = new MobileOrgDatabase((Context) this);
		this.appInst = (MobileOrgApplication) this.getApplication();
		this.appSettings = PreferenceManager
				.getDefaultSharedPreferences(getBaseContext());

		registerForContextMenu(getListView());

		if (!appInst.isSynchConfigured())
			this.runShowSettings();

		if (this.appSettings.getBoolean("doAutoSync", false)) {
			Intent serviceIntent = new Intent();
			serviceIntent.setAction("com.matburt.mobileorg.SYNC_SERVICE");
			this.startService(serviceIntent);
		}
		
		if (this.appInst.rootNode == null)
			this.runParser();
	}
	
	@Override
	public void onResume() {
		super.onResume();
		
		noClueWhatThisDoes();
		this.refreshDisplay();
	}
	
	private void noClueWhatThisDoes() {
		Intent nodeIntent = getIntent();
		ArrayList<Integer> intentNodePath = nodeIntent
				.getIntegerArrayListExtra("nodePath");
		
		if (intentNodePath != null) {
			appInst.nodeSelection = MobileOrgApplication.copySelection(intentNodePath);
			
			// Why would you put something into the intent again?!
			nodeIntent.putIntegerArrayListExtra("nodePath", null);
		} else {
			appInst.nodeSelection = MobileOrgApplication.copySelection(this.origSelection);
		}
	}
		
	/*
	 * Refreshes the outline display. Should be called when the underlying 
	 * data has been updated.
	 */
	private void refreshDisplay() {
		this.setListAdapter(new OutlineViewAdapter(this,
				this.appInst.rootNode, this.appInst.nodeSelection,
				this.appInst.edits, this.appdb.getTodos()));

		this.origSelection = MobileOrgApplication.copySelection(this.appInst.nodeSelection);

		getListView().setSelection(displayIndex);
	}
	
	/*
	 * Runs the parser and refreshes outline by calling refreshDisplay().
	 * If parsing didn't result in any files, display a newSetup dialog.
	 */
	private void runParser() {
		try {
			OrgFileParser ofp = new OrgFileParser(appSettings, appInst, appdb);
			ofp.runParser(appSettings, appInst);
		} catch (Throwable e) {
			ErrorReporter.displayError(
					this, "An error occurred during parsing, try re-syncing: "
							+ e.toString());
		}
		
		HashMap<String, String> allOrgList = this.appdb.getOrgFiles();
		if (allOrgList.isEmpty()) {
			this.showNewUserWindow();
		} else if (this.newSetupDialog_shown) {
			newSetupDialog_shown = false;
			newSetupDialog.cancel();
		}

		this.refreshDisplay();
	}
	
	@Override
	public void onDestroy() {
		this.appdb.close();
		super.onDestroy();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
	    inflater.inflate(R.menu.outline_menu, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.menu_sync:
			runSynchronizer();
			return true;
		
		case R.id.menu_settings:
			return runShowSettings();
		
		case R.id.menu_outline:
			runExpandSelection(new ArrayList<Integer>());
			return true;
		
		case R.id.menu_capture:
			return runCapture();
		}
		return false;
	}

	@Override
	public void onCreateContextMenu(ContextMenu menu, View v,
			ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, v, menuInfo);
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.outline_contextmenu, menu);

		// Prevents editing of file nodes.
		if (this.appInst.nodeSelection == null)
			menu.findItem(R.id.contextmenu_edit).setVisible(false);
	}

	@Override
	public boolean onContextItemSelected(MenuItem item) {
		AdapterContextMenuInfo info = (AdapterContextMenuInfo) item
				.getMenuInfo();
		Node node = (Node) getListAdapter().getItem(info.position);

		switch (item.getItemId()) {
		case R.id.contextmenu_view:
			runSimpleNodeDisplay(node);
			break;

		case R.id.contextmenu_edit:
			runViewNodeDetails(node, info.position);
			break;
		}

		return false;
	}

	@Override
	public void onListItemClick(ListView l, View v, int position, long id) {
		this.appInst.pushSelection(position);
		Node node = (Node) l.getItemAtPosition(position);

		if (node.encrypted && !node.parsed) {
			decryptNode(node);
			return;
		}
		
		if (node.hasChildren()) {
			runExpandSelection(this.appInst.nodeSelection);
		} else {
			this.displayIndex = this.appInst.lastIndex();
			this.appInst.popSelection();

			if (node.isSimple()) {
				runSimpleNodeDisplay(node);
			} else {
				runViewNodeDetails(node, position);
			}
		}
	}
	
	private void runSimpleNodeDisplay(Node node) {
		Intent textIntent = new Intent(this, SimpleTextDisplay.class);
		String docBuffer = node.name + "\n\n" + node.payload;
		textIntent.putExtra("txtValue", docBuffer);
		startActivity(textIntent);
	}
	
	private void runViewNodeDetails(Node node, int position) {
		Intent dispIntent = new Intent(this,
				ViewNodeDetailsActivity.class);
		dispIntent.putExtra("actionMode", "edit");
		dispIntent.putIntegerArrayListExtra("nodePath",
				this.appInst.nodeSelection);
		this.appInst.pushSelection(position);
		startActivity(dispIntent);
	}
	
	private void runExpandSelection(ArrayList<Integer> selection) {
		Intent dispIntent = new Intent(this, OutlineActivity.class);
		dispIntent.putIntegerArrayListExtra("nodePath", selection);
		startActivityForResult(dispIntent, RUNFOR_EXPAND);
	}
	
	private boolean runCapture() {
		Intent captureIntent = new Intent(this, Capture.class);
		captureIntent.putExtra("actionMode", "create");
		startActivityForResult(captureIntent, RUNFOR_PARSER);
		return true;
	}
	
	private boolean runShowSettings() {
		Intent settingsIntent = new Intent(this, SettingsActivity.class);
		startActivity(settingsIntent);
		return true;
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {

		switch (requestCode) {
		case RUNFOR_PARSER:
			this.runParser();
			break;

		case Encryption.DECRYPT_MESSAGE:
			if (resultCode != Activity.RESULT_OK || data == null) {
				this.appInst.popSelection();
				return;
			}

			parseEncryptedNode(data);
			runExpandSelection(this.appInst.nodeSelection);
			break;

		default:
			displayIndex = this.appInst.lastIndex();
			this.appInst.popSelection();
			break;
		}
	}

	private void parseEncryptedNode(Intent data) {
		Node thisNode = this.appInst.getSelectedNode();
		String userSynchro = this.appSettings.getString("syncSource", "");
		String orgBasePath = "";
		if (userSynchro.equals("sdcard")) {
			String indexFile = this.appSettings.getString("indexFilePath",
					"");
			File fIndexFile = new File(indexFile);
			orgBasePath = fIndexFile.getParent() + "/";
		} else {
			orgBasePath = Environment.getExternalStorageDirectory()
					.getAbsolutePath() + "/mobileorg/";
		}
		String decryptedData = data
				.getStringExtra(Encryption.EXTRA_DECRYPTED_MESSAGE);
		OrgFileParser ofp = new OrgFileParser(appdb.getOrgFiles(),
				appSettings.getString("storageMode", ""), userSynchro, appdb, orgBasePath);

		ofp.parse(thisNode, new BufferedReader(new StringReader(
				decryptedData)));
	}
	
	private void decryptNode(Node thisNode) {
			// if suitable APG version is installed
			if (Encryption.isAvailable((Context) this)) {
				// retrieve the encrypted file data
				String userSynchro = this.appSettings.getString("syncSource",
						"");
				String orgBasePath = "";
				if (userSynchro.equals("sdcard")) {
					String indexFile = this.appSettings.getString(
							"indexFilePath", "");
					File fIndexFile = new File(indexFile);
					orgBasePath = fIndexFile.getParent() + "/";
				} else {
					orgBasePath = Environment.getExternalStorageDirectory()
							.getAbsolutePath() + "/mobileorg/";
				}

				byte[] rawData = OrgFileParser.getRawFileData(orgBasePath,
						thisNode.name);
				// and send it to APG for decryption
				Encryption.decrypt(this, rawData);
			} else {
				this.appInst.popSelection();
			}
	}

	private void showNewUserWindow() {
		if (this.newSetupDialog_shown) {
			this.newSetupDialog.cancel();
		}
		newSetupDialog = new Dialog(this);
		newSetupDialog.setContentView(R.layout.empty_main);
		Button syncButton = (Button) newSetupDialog
				.findViewById(R.id.dialog_run_sync);
		syncButton.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				runSynchronizer();
			}
		});
		Button settingsButton = (Button) newSetupDialog
				.findViewById(R.id.dialog_show_settings);
		settingsButton.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				runShowSettings();
			}
		});
		newSetupDialog.setTitle("Synchronize Org Files");
		newSetupDialog.show();
		this.newSetupDialog_shown = true;
	}

	

	final Runnable syncUpdateResults = new Runnable() {
		public void run() {
			postSynchronize();
		}
	};
	
	private void runSynchronizer() {
		String userSynchro = this.appSettings.getString("syncSource", "");
		final Synchronizer appSync;
		if (userSynchro.equals("webdav")) {
			appSync = new WebDAVSynchronizer(this);
		} else if (userSynchro.equals("sdcard")) {
			appSync = new SDCardSynchronizer(this);
		} else if (userSynchro.equals("dropbox")) {
			appSync = new DropboxSynchronizer(this);
		} else {
			this.runShowSettings();
			return;
		}

		if (!appSync.checkReady()) {
			Toast error = Toast
					.makeText(
							(Context) this,
							"You have not fully configured the synchronizer.  Make sure you visit the 'Configure Synchronizer Settings' in the Settings menu",
							Toast.LENGTH_LONG);
			error.show();
			this.runShowSettings();
			return;
		}

		Thread syncThread = new Thread() {
			public void run() {
				try {
					syncError = null;
					appSync.pull();
					appSync.push();
					Log.d("MobileOrg" + this, "Finished parsing...");
				} catch (ReportableError e) {
					syncError = e;
				} finally {
					appSync.close();
				}
				syncHandler.post(syncUpdateResults);
			}
		};
		syncThread.start();
		syncDialog = ProgressDialog.show(this, "",
				getString(R.string.sync_wait), true);
	}

	private void postSynchronize() {
		syncDialog.dismiss();
		if (this.syncError != null) {
			ErrorReporter.displayError(this, this.syncError);
		} else {
			this.runParser();
			this.onResume();
		}
	}
}
