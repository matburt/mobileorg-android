package com.matburt.mobileorg.Gui;

import java.io.BufferedReader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;

import android.app.Dialog;
import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
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
import com.matburt.mobileorg.Error.ErrorReporter;
import com.matburt.mobileorg.Error.ReportableError;
import com.matburt.mobileorg.Parsing.NodeEncryption;
import com.matburt.mobileorg.Parsing.OrgDatabase;
import com.matburt.mobileorg.Parsing.Node;
import com.matburt.mobileorg.Parsing.OrgFileParser;
import com.matburt.mobileorg.Settings.SettingsActivity;
import com.matburt.mobileorg.Synchronizers.DropboxSynchronizer;
import com.matburt.mobileorg.Synchronizers.SDCardSynchronizer;
import com.matburt.mobileorg.Synchronizers.Synchronizer;
import com.matburt.mobileorg.Synchronizers.WebDAVSynchronizer;

public class OutlineActivity extends ListActivity
{
	private static final int RUNFOR_EXPAND = 1;
	private static final int RUNFOR_NEWNODE = 3;

	MobileOrgApplication appInst;
	private OrgDatabase appdb;
	private SharedPreferences appSettings;

	private int displayIndex;
	private ArrayList<Integer> origSelection = null;
	
	private Dialog newSetupDialog;
	private boolean newSetupDialog_shown = false;

	final Handler syncHandler = new Handler();
	private ReportableError syncError;
	private ProgressDialog syncDialog;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		this.appdb = new OrgDatabase((Context) this);
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
		
	/**
	 * Refreshes the outline display. Should be called when the underlying 
	 * data has been updated.
	 */
	private void refreshDisplay() {
		this.setListAdapter(new OutlineListAdapter(this,
				this.appInst.rootNode, this.appInst.nodeSelection,
				this.appInst.edits, this.appdb.getGroupedTodods()));

		this.origSelection = MobileOrgApplication.copySelection(this.appInst.nodeSelection);

		getListView().setSelection(displayIndex);
	}
	
	/**
	 * Runs the parser and refreshes outline by calling {@link #refreshDisplay}.
	 * If parsing didn't result in any files, display a newSetup dialog.
	 */
	private void runParser() {
		try {
			OrgFileParser ofp = new OrgFileParser(getBaseContext(), appInst);
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
			return runEditNewNodeActivity();
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
			runViewNodeActivity(node);
			break;

		case R.id.contextmenu_edit:
			runEditNodeActivity(node, info.position);
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
				runViewNodeActivity(node);
			} else {
				runEditNodeActivity(node, position);
			}
		}
	}
	
	private boolean runEditNewNodeActivity() {
		Intent intent = new Intent(this, EditNodeActivity.class);
		intent.putExtra("actionMode", EditNodeActivity.ACTIONMODE_CREATE);
		startActivityForResult(intent, RUNFOR_NEWNODE);
		return true;
	}
	
	private void runEditNodeActivity(Node node, int position) {
		Intent intent = new Intent(this,
				EditNodeActivity.class);
		intent.putExtra("actionMode", EditNodeActivity.ACTIONMODE_EDIT);
		intent.putIntegerArrayListExtra("nodePath",
				this.appInst.nodeSelection);
		this.appInst.pushSelection(position);
		startActivity(intent);
	}

	private void runViewNodeActivity(Node node) {
		Intent intent = new Intent(this, ViewNodeActivity.class);
		String docBuffer = node.name + "\n\n" + node.payload;
		intent.putExtra("txtValue", docBuffer);
		startActivity(intent);
	}

	private void runExpandSelection(ArrayList<Integer> selection) {
		Intent intent = new Intent(this, OutlineActivity.class);
		intent.putIntegerArrayListExtra("nodePath", selection);
		startActivityForResult(intent, RUNFOR_EXPAND);
	}

	
	private boolean runShowSettings() {
		Intent intent = new Intent(this, SettingsActivity.class);
		startActivity(intent);
		return true;
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent intent) {

		switch (requestCode) {

		case RUNFOR_EXPAND:
			displayIndex = this.appInst.lastIndex();
			this.appInst.popSelection();
			break;

		case RUNFOR_NEWNODE:
			if(resultCode == RESULT_OK) {
				this.runParser();
			}
			break;

		case NodeEncryption.DECRYPT_MESSAGE:
			if (resultCode != RESULT_OK || intent == null) {
				this.appInst.popSelection();
				return;
			}

			parseEncryptedNode(intent);
			runExpandSelection(this.appInst.nodeSelection);
			break;
		}
	}

	/**
	 * This calls startActivityForResult() with Encryption.DECRYPT_MESSAGE. The
	 * result is handled by onActivityResult() in this class, which calls a
	 * function to parse the resulting plain text file.
	 */
	private void decryptNode(Node node) {
		// if suitable APG version is installed
		if (NodeEncryption.isAvailable((Context) this)) {
			// retrieve the encrypted file data
			OrgFileParser ofp = new OrgFileParser(getBaseContext(), appInst);
			byte[] rawData = ofp.getRawFileData(node.name);
			// and send it to APG for decryption
			NodeEncryption.decrypt(this, rawData);
		} else {
			this.appInst.popSelection();
		}
	}
	
	/**
	 * This function is called with the results of {@link #decryptNode}.
	 */
	private void parseEncryptedNode(Intent data) {
		OrgFileParser ofp = new OrgFileParser(getBaseContext(), appInst);
		Node node = this.appInst.getSelectedNode();

		String decryptedData = data
				.getStringExtra(NodeEncryption.EXTRA_DECRYPTED_MESSAGE);

		ofp.parse(node, new BufferedReader(new StringReader(
				decryptedData)));
	}


	private void showNewUserWindow() {
		if (this.newSetupDialog_shown) {
			this.newSetupDialog.cancel();
		}
		newSetupDialog = new Dialog(this);
		newSetupDialog.setContentView(R.layout.outline_unconfigured);
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
			Toast error = Toast.makeText((Context) this,
					getString(R.string.error_synchronizer_not_configured),
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
	
	final Runnable syncUpdateResults = new Runnable() {
		public void run() {
			postSynchronize();
		}
	};
	
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
