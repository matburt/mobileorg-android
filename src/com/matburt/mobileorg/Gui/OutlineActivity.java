package com.matburt.mobileorg.Gui;

import java.io.BufferedReader;
import java.io.StringReader;
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
import com.matburt.mobileorg.Parsing.Node;
import com.matburt.mobileorg.Parsing.NodeEncryption;
import com.matburt.mobileorg.Parsing.OrgFileParser;
import com.matburt.mobileorg.Settings.SettingsActivity;
import com.matburt.mobileorg.Synchronizers.DropboxSynchronizer;
import com.matburt.mobileorg.Synchronizers.SDCardSynchronizer;
import com.matburt.mobileorg.Synchronizers.Synchronizer;
import com.matburt.mobileorg.Synchronizers.WebDAVSynchronizer;

public class OutlineActivity extends ListActivity
{
	private static final int RESULT_DONTPOP = 1337;
	
	private static final int RUNFOR_EXPAND = 1;
	private static final int RUNFOR_EDITNODE = 2;
	private static final int RUNFOR_NEWNODE = 3;

	private MobileOrgApplication appInst;
	private SharedPreferences appSettings;

	private int depth;
	
	private Dialog newSetupDialog;
	private boolean newSetupDialog_shown = false;

	final Handler syncHandler = new Handler();
	private ReportableError syncError;
	private ProgressDialog syncDialog;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		this.appInst = (MobileOrgApplication) this.getApplication();
		this.appSettings = PreferenceManager
				.getDefaultSharedPreferences(getBaseContext());
		
		Intent intent = getIntent();
		this.depth = intent.getIntExtra("depth", 1);

		if (!appInst.isSynchConfigured())
			this.runShowSettings();
		
		if (this.appInst.rootNode == null) {
			this.runParser();
			appInst.pushNodestack(appInst.rootNode);
		}

		registerForContextMenu(getListView());
	}
	
	@Override
	public void onResume() {
		super.onResume();

		// If this is the case the parser has invalidated node
		if (this.depth > this.appInst.nodestackSize()) {
			this.setResult(RESULT_DONTPOP);
			finish();
		}
		
		refreshDisplay();
	}
		
	/**
	 * Refreshes the outline display. Should be called when the underlying 
	 * data has been updated.
	 */
	private void refreshDisplay() {
		this.setListAdapter(new OutlineListAdapter(this, appInst.nodestackTop()));

		// TODO Fix this
		// getListView().setSelection(depth);
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

		if (this.appInst.getOrgFiles().isEmpty()) {
			this.showNewUserWindow();
		} else if (this.newSetupDialog_shown) {
			newSetupDialog_shown = false;
			newSetupDialog.cancel();
		}

		refreshDisplay();
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
			appInst.clearNodestack();
			onResume();
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
		if (this.appInst.nodestackSize() < 2)
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
			runEditNodeActivity(node);
			break;
		}

		return false;
	}

	@Override
	public void onListItemClick(ListView l, View v, int position, long id) {
		Node node = (Node) l.getItemAtPosition(position);

		if (node.encrypted && !node.parsed) {
			decryptNode(node);
			return;
		}
		
		if (node.hasChildren()) {
			runExpandSelection(node);
		} else {

			if (node.isSimple()) {
				runViewNodeActivity(node);
			} else {
				runEditNodeActivity(node);
			}
		}
	}
	
	private boolean runEditNewNodeActivity() {
		Intent intent = new Intent(this, NodeEditActivity.class);
		intent.putExtra("actionMode", NodeEditActivity.ACTIONMODE_CREATE);
		startActivityForResult(intent, RUNFOR_NEWNODE);
		return true;
	}
	
	private void runEditNodeActivity(Node node) {
		Intent intent = new Intent(this,
				NodeEditActivity.class);
		intent.putExtra("actionMode", NodeEditActivity.ACTIONMODE_EDIT);

		this.appInst.pushNodestack(node);
		startActivity(intent);
	}

	private void runViewNodeActivity(Node node) {
		Intent intent = new Intent(this, NodeViewActivity.class);
		String docBuffer = node.name + "\n\n" + node.payload;
		intent.putExtra("txtValue", docBuffer);
		startActivity(intent);
	}

	private void runExpandSelection(Node node) {
		appInst.pushNodestack(node);
		Intent intent = new Intent(this, OutlineActivity.class);
		
		int childDepth = this.depth + 1;
		intent.putExtra("depth", childDepth);
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
			if(resultCode != RESULT_DONTPOP)
				this.appInst.popNodestack();
			break;

		case RUNFOR_EDITNODE:		
			appInst.popNodestack();
			break;
			
		case RUNFOR_NEWNODE:
			if(resultCode == RESULT_OK) {
				this.runParser();
			}
			break;

		case NodeEncryption.DECRYPT_MESSAGE:
			if (resultCode != RESULT_OK || intent == null) {
				this.appInst.popNodestack();
				return;
			}

			parseEncryptedNode(intent);
			runExpandSelection(this.appInst.nodestackTop());
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
			this.appInst.popNodestack();
		}
	}
	
	/**
	 * This function is called with the results of {@link #decryptNode}.
	 */
	private void parseEncryptedNode(Intent data) {
		OrgFileParser ofp = new OrgFileParser(getBaseContext(), appInst);
		Node node = this.appInst.nodestackTop();

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
