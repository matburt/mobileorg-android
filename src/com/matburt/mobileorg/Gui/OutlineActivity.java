package com.matburt.mobileorg.Gui;

import java.io.BufferedReader;
import java.io.IOException;
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
import com.matburt.mobileorg.Parsing.Node;
import com.matburt.mobileorg.Parsing.NodeEncryption;
import com.matburt.mobileorg.Parsing.OrgFileParser;
import com.matburt.mobileorg.Settings.SettingsActivity;
import com.matburt.mobileorg.Synchronizers.SyncManager;

public class OutlineActivity extends ListActivity
{
	private static final int RESULT_DONTPOP = 1337;
	
	private static final int RUNFOR_EXPAND = 1;
	private static final int RUNFOR_EDITNODE = 2;
	private static final int RUNFOR_NEWNODE = 3;

	private MobileOrgApplication appInst;
	private SharedPreferences appSettings;

	/**
	 * Keeps track of the depth of the tree. This is used to recursively finish
	 * OutlineActivities, updating the display properly on changes to the
	 * underlying data structure.
	 */
	private int depth;
	
	/**
	 * Keeps track of the last selected item chosen from the outline. When the
	 * outline resumes it will remember what node was selected. Purely cosmetic
	 * feature.
	 */
	private int lastSelection = 0;
	
	private Dialog newSetupDialog;
	private boolean newSetupDialog_shown = false;

	final Handler syncHandler = new Handler();
	private IOException syncError;
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

		// If this is the case, the parser has invalidated nodes
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
		getListView().setSelection(lastSelection);
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
		
		appInst.refreshNodestack();
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
		if (this.depth == 1) {
			menu.findItem(R.id.contextmenu_edit).setVisible(false);
		} else {
			menu.findItem(R.id.contextmenu_delete).setVisible(false);
		}
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
			
		case R.id.contextmenu_delete:
			runDeleteNode(node);
			break;
		}

		return false;
	}

	@Override
	public void onListItemClick(ListView l, View v, int position, long id) {
		Node node = (Node) l.getItemAtPosition(position);
		this.lastSelection = position;

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
		/* Pushes the given Node to the nodestack, to give it as argument to
		 * NodeEditActivity, which pops the node after use. We probably want to
		 * find a more elegant solution. */
		this.appInst.pushNodestack(node);
		
		Intent intent = new Intent(this,
				NodeEditActivity.class);
		intent.putExtra("actionMode", NodeEditActivity.ACTIONMODE_EDIT);
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
	
	private void runDeleteNode(Node node) {
		// TODO Maybe prompt with a yes-no dialog
		appInst.deleteFile(node.name);
		refreshDisplay();
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
		final SyncManager synchman = new SyncManager(this);

		if (!synchman.isConfigured()) {
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
					synchman.sync();
				} catch (IOException e) {
					syncError = e;
				} finally {
					synchman.close();
				}
				syncHandler.post(syncUpdateResults);
			}
		};
		syncThread.start();
		syncDialog = ProgressDialog.show(this, "",
				getString(R.string.sync_wait), true);
	}
	
	private final Runnable syncUpdateResults = new Runnable() {
		public void run() {
			postSynchronize();
		}
	};
	
	private void postSynchronize() {
		syncDialog.dismiss();
		if (this.syncError != null) {
			ErrorReporter.displayError(this, this.syncError.getMessage());
		} else {
			this.runParser();
			this.onResume();
		}
	}
}
