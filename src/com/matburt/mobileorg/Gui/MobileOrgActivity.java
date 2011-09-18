package com.matburt.mobileorg.Gui;

import java.io.BufferedReader;
import java.io.File;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collections;
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

public class MobileOrgActivity extends ListActivity
{
	@SuppressWarnings("unused")
	private static final String LT = "MobileOrg";

	private static final int RUN_PARSER = 3;
	private int displayIndex;
	private ProgressDialog syncDialog;
	private MobileOrgDatabase appdb;
	private ReportableError syncError;
	private Dialog newSetupDialog;
	private boolean newSetupDialog_shown = false;
	private SharedPreferences appSettings;
	final Handler syncHandler = new Handler();
	private ArrayList<Integer> origSelection = null;
	private boolean first = true;
	MobileOrgApplication appInst = null;

	final Runnable syncUpdateResults = new Runnable() {
		public void run() {
			postSynchronize();
		}
	};

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		this.appdb = new MobileOrgDatabase((Context) this);
		this.appInst = (MobileOrgApplication) this
				.getApplication();
		this.appSettings = PreferenceManager
				.getDefaultSharedPreferences(getBaseContext());

		registerForContextMenu(getListView());

		if (this.appSettings.getString("syncSource", "").equals("")
				|| (this.appSettings.getString("syncSource", "").equals(
						"webdav") && this.appSettings.getString("webUrl", "")
						.equals(""))
				|| (this.appSettings.getString("syncSource", "").equals(
						"sdcard") && this.appSettings.getString(
						"indexFilePath", "").equals(""))) {
			this.showSettings();
		}

		if (this.appSettings.getBoolean("doAutoSync", false)) {
			Intent serviceIntent = new Intent();
			serviceIntent.setAction("com.matburt.mobileorg.SYNC_SERVICE");
			this.startService(serviceIntent);
		}
	}
	
	@Override
	public void onResume() {
		super.onResume();
		Intent nodeIntent = getIntent();
		this.appInst = (MobileOrgApplication) this
				.getApplication(); // Is this really necessary or is it enough in onCreate() ?
		ArrayList<Integer> intentNodePath = nodeIntent
				.getIntegerArrayListExtra("nodePath");
		if (intentNodePath != null) {
			appInst.nodeSelection = copySelection(intentNodePath);
			nodeIntent.putIntegerArrayListExtra("nodePath", null);
		} else {
			appInst.nodeSelection = copySelection(this.origSelection);
		}
		
		populateDisplay();
	}

	@Override
	public void onDestroy() {
		this.appdb.close();
		super.onDestroy();
	}

	private void runParser() {
		HashMap<String, String> allOrgList = this.appdb.getOrgFiles();
		if (allOrgList.isEmpty()) {
			return;
		}
		String storageMode = this.getStorageLocation();
		String userSynchro = this.appSettings.getString("syncSource", "");
		String orgBasePath = "";

		if (userSynchro.equals("sdcard")) {
			String indexFile = this.appSettings.getString("indexFilePath", "");
			File fIndexFile = new File(indexFile);
			orgBasePath = fIndexFile.getParent() + "/";
		} else {
			orgBasePath = Environment.getExternalStorageDirectory()
					.getAbsolutePath() + "/mobileorg/";
		}

		OrgFileParser ofp = new OrgFileParser(allOrgList, storageMode,
				userSynchro, this.appdb, orgBasePath);
		try {
			ofp.parse();
			this.appInst.rootNode = ofp.rootNode;
			this.appInst.edits = ofp.parseEdits();
			Collections.sort(this.appInst.rootNode.children, Node.comparator);
		} catch (Throwable e) {
			ErrorReporter.displayError(
					this,
					"An error occurred during parsing, try re-syncing: "
							+ e.toString());
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
				showSettings();
			}
		});
		newSetupDialog.setTitle("Synchronize Org Files");
		newSetupDialog.show();
		this.newSetupDialog_shown = true;
	}



	public void populateDisplay() {
		if (this.appInst.rootNode == null) {
			this.runParser();
		}

		HashMap<String, String> allOrgList = this.appdb.getOrgFiles();
		if (allOrgList.isEmpty()) {
			this.showNewUserWindow();
		} else if (this.newSetupDialog_shown) {
			newSetupDialog_shown = false;
			newSetupDialog.cancel();
		}

		if (first) {
			this.setListAdapter(new MobileOrgViewAdapter(this, this.appInst.rootNode,
					this.appInst.nodeSelection, this.appInst.edits, this.appdb.getTodos()));
			if (this.appInst.nodeSelection != null) {
				this.origSelection = copySelection(this.appInst.nodeSelection);
			} else {
				this.origSelection = null;
			}
			Log.d("MobileOrg" + this, " first redisplay, origSelection="
					+ nodeSelectionStr(this.origSelection));

			getListView().setSelection(displayIndex);
			first = false;

			// setTitle(generateTitle());
		}
	}

	@SuppressWarnings("unused")
	private String generateTitle() {
		String title = "";

		if (this.appInst.nodeSelection != null) {
			ArrayList<Integer> nodeSelectionBackup = new ArrayList<Integer>();
			for (Integer item : this.appInst.nodeSelection)
				nodeSelectionBackup.add(item);

			while (nodeSelectionBackup.size() > 0) {
				title = this.appInst.getNode(nodeSelectionBackup).nodeTitle + "$"
						+ title;
				nodeSelectionBackup.remove(nodeSelectionBackup.size() - 1);
			}
		}
		return title;
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
	    inflater.inflate(R.menu.activity_menu, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.menu_sync:
			this.runSynchronizer();
			return true;
		case R.id.menu_settings:
			return this.showSettings();
		case R.id.menu_outline:
			Intent dispIntent = new Intent(this, MobileOrgActivity.class);
			dispIntent.putIntegerArrayListExtra("nodePath",
					new ArrayList<Integer>());
			startActivity(dispIntent);
			return true;
		case R.id.menu_capture:
			return this.runCapture();
		}
		return false;
	}

	@Override
	public void onCreateContextMenu(ContextMenu menu, View v,
			ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, v, menuInfo);
		MenuInflater inflater = getMenuInflater();
	    inflater.inflate(R.menu.activity_contextmenu, menu);
	    
		if (this.appInst.nodeSelection == null)
	    menu.findItem(R.id.contextmenu_edit).setVisible(false);
	}

	@Override
	public boolean onContextItemSelected(MenuItem item) {
		AdapterContextMenuInfo info = (AdapterContextMenuInfo) item
				.getMenuInfo();
		Node n = (Node) getListAdapter().getItem(info.position);

		Intent textIntent = new Intent();

		switch (item.getItemId()) {
		case R.id.contextmenu_view:
			textIntent.setClass(this, SimpleTextDisplay.class);
			String txtValue = n.nodeTitle + "\n\n" + n.payload;
			textIntent.putExtra("txtValue", txtValue);
			textIntent.putExtra("nodeTitle", n.name);
			break;

		case R.id.contextmenu_edit:
			textIntent.setClass(this, ViewNodeDetailsActivity.class);
			textIntent.putExtra("actionMode", "edit");

			this.appInst.pushSelection(info.position);
			textIntent.putIntegerArrayListExtra("nodePath",
					this.appInst.nodeSelection);
			break;
		}
		startActivity(textIntent);
		return false;
	}

	@Override
	public void onListItemClick(ListView l, View v, int position, long id) {

		this.appInst.pushSelection(position);
		Node node = (Node) l.getItemAtPosition(position);

		if (decryptNode(node))
			return;

		if (node.hasChildren()) {
			expandSelection(this.appInst.nodeSelection);
		} else {
			displayIndex = this.appInst.lastIndex();
			this.appInst.popSelection();

			if (node.isSimple()) {
				Intent textIntent = new Intent(this, SimpleTextDisplay.class);
				String docBuffer = node.name + "\n\n" + node.payload;

				textIntent.putExtra("txtValue", docBuffer);
				startActivity(textIntent);
			} else {
				Intent dispIntent = new Intent(this,
						ViewNodeDetailsActivity.class);

				dispIntent.putExtra("actionMode", "edit");
				dispIntent.putIntegerArrayListExtra("nodePath",
						this.appInst.nodeSelection);
				this.appInst.pushSelection(position);
				startActivity(dispIntent);
			}
		}
	}

	private boolean decryptNode(Node thisNode) {
		if (thisNode.encrypted && !thisNode.parsed) {
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
			return true;
		}
		return false;
	}

	static String nodeSelectionStr(ArrayList<Integer> nodes) {
		if (nodes != null) {
			String tmp = "";

			for (Integer i : nodes) {
				if (tmp.length() > 0)
					tmp += ",";
				tmp += i;
			}
			return tmp;
		}
		return "null";
	}

	static private ArrayList<Integer> copySelection(ArrayList<Integer> selection) {
		if (selection == null)
			return null;
		else
			return new ArrayList<Integer>(selection);
	}

	private void expandSelection(ArrayList<Integer> selection) {
		Intent dispIntent = new Intent(this, MobileOrgActivity.class);
		dispIntent.putIntegerArrayListExtra("nodePath", selection);
		startActivityForResult(dispIntent, 1);
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		Log.d("MobileOrg" + this, "onActivityResult");
		if (requestCode == RUN_PARSER) {
			this.runParser();
		} else if (requestCode == Encryption.DECRYPT_MESSAGE) {
			if (resultCode != Activity.RESULT_OK || data == null) {
				this.appInst.popSelection();
				return;
			}

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
					getStorageLocation(), userSynchro, appdb, orgBasePath);

			ofp.parse(thisNode, new BufferedReader(new StringReader(
					decryptedData)));
			expandSelection(this.appInst.nodeSelection);
		} else {
			displayIndex = this.appInst.lastIndex();
			this.appInst.popSelection();
		}
	}

	private boolean showSettings() {
		Intent settingsIntent = new Intent(this, SettingsActivity.class);
		startActivity(settingsIntent);
		return true;
	}

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
			this.showSettings();
			return;
		}

		if (!appSync.checkReady()) {
			Toast error = Toast
					.makeText(
							(Context) this,
							"You have not fully configured the synchronizer.  Make sure you visit the 'Configure Synchronizer Settings' in the Settings menu",
							Toast.LENGTH_LONG);
			error.show();
			this.showSettings();
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

	private boolean runCapture() {
		Intent captureIntent = new Intent(this, Capture.class);
		captureIntent.putExtra("actionMode", "create");
		startActivityForResult(captureIntent, 3);
		return true;
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

	private String getStorageLocation() {
		return this.appSettings.getString("storageMode", "");
	}
}
