package com.matburt.mobileorg.Gui;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.ListView;
import android.widget.Toast;

import com.markupartist.android.widget.ActionBar;
import com.markupartist.android.widget.ActionBar.Action;
import com.markupartist.android.widget.ActionBar.IntentAction;
import com.matburt.mobileorg.R;
import com.matburt.mobileorg.Parsing.MobileOrgApplication;
import com.matburt.mobileorg.Services.SyncService;
import com.matburt.mobileorg.Settings.SettingsActivity;
import com.matburt.mobileorg.Settings.WizardActivity;
import com.matburt.mobileorg.Synchronizers.Synchronizer;

public class OutlineActivity extends ListActivity
{
//	private static final int RESULT_DONTPOP = 1337;
	
	private MobileOrgApplication appInst;

	private long node_id;
	
	/**
	 * Keeps track of the last selected item chosen from the outline. When the
	 * outline resumes it will remember what node was selected. Purely cosmetic
	 * feature.
	 */
	private int lastSelection = 0;
	
	private OutlineCursorAdapter outlineAdapter;
	private SynchServiceReceiver syncReceiver;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.outline);		
		setupActionbar(this);
		
		this.appInst = (MobileOrgApplication) this.getApplication();
		
		Intent intent = getIntent();
		node_id = intent.getLongExtra("node_id", -1);

		if(this.node_id == -1) {
			if(this.appInst.isSyncConfigured() == false)
                this.showWizard();
		}

		registerForContextMenu(getListView());	
		
		this.syncReceiver = new SynchServiceReceiver();
		registerReceiver(this.syncReceiver, new IntentFilter(
				Synchronizer.SYNC_UPDATE));
				
		refreshDisplay();
	}
	
	public static void setupActionbar(Activity activity) {
		ActionBar actionBar = (ActionBar) activity.findViewById(R.id.actionbar);
		actionBar.setTitle("MobileOrg");

		actionBar.setHomeAction(new IntentAction(activity, new Intent(activity,
				OutlineActivity.class), R.drawable.icon));
        
		Intent intent2 = new Intent(activity, NodeEditActivity.class);
		intent2.putExtra("actionMode", NodeEditActivity.ACTIONMODE_CREATE);
        final Action otherAction = new IntentAction(activity, intent2, R.drawable.ic_menu_compose);
        actionBar.addAction(otherAction);
	}
	
	@Override
	public void onDestroy() {
		unregisterReceiver(this.syncReceiver);
		this.appInst = null;
		super.onDestroy();
	}
		
	/**
	 * Refreshes the outline display. Should be called when the underlying 
	 * data has been updated.
	 */
	private void refreshDisplay() {
		Cursor cursor;
		if (node_id >= 0)
			cursor = appInst.getDB().getNodeChildren(node_id);
		else
			cursor = appInst.getDB().getFileCursor();

        if (cursor == null) {
            return;
        }

		startManagingCursor(cursor);
		this.outlineAdapter = new OutlineCursorAdapter(this, cursor, appInst.getDB());
		this.setListAdapter(outlineAdapter);

		getListView().setSelection(this.lastSelection);
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
			runSync();
			return true;
		
		case R.id.menu_settings:
			return runShowSettings();
		
//		case R.id.menu_outline:
//			refreshDisplay();
//			return true;
//		
		case R.id.menu_capture:
			return runEditNewNodeActivity();
			
		case R.id.menu_search:
			return runSearch();
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
		if (this.node_id == -1) {
			menu.findItem(R.id.contextmenu_edit).setVisible(false);
		} else {
			menu.findItem(R.id.contextmenu_delete).setVisible(false);
		}
	}
	
	@Override
	public boolean onContextItemSelected(MenuItem item) {
		AdapterContextMenuInfo info = (AdapterContextMenuInfo) item
				.getMenuInfo();
		
		long node_id = getListAdapter().getItemId(info.position);

		switch (item.getItemId()) {
		case R.id.contextmenu_view:
			runViewNodeActivity(node_id);
			break;

		case R.id.contextmenu_edit:
			runEditNodeActivity(node_id);
			break;
			
		case R.id.contextmenu_delete:
			runDeleteNode(node_id);
			break;
		}

		return false;
	}

	@Override
	public void onListItemClick(ListView l, View v, int position, long id) {
		Long node_id = l.getItemIdAtPosition(position);

		this.lastSelection = position;
		
		if (this.appInst.getDB().hasNodeChildren(node_id))
			runExpandSelection(node_id);
		else
			runViewNodeActivity(node_id);
	}

    private void showWizard() {
        startActivity(new Intent(this, WizardActivity.class));
    }
    
    private void runSync() {
		startService(new Intent(this, SyncService.class));
    }
	
	private boolean runEditNewNodeActivity() {
		Intent intent = new Intent(this, NodeEditActivity.class);
		intent.putExtra("actionMode", NodeEditActivity.ACTIONMODE_CREATE);
		startActivity(intent);
		return true;
	}
	
	private void runEditNodeActivity(long nodeId) {
		Intent intent = new Intent(this,
				NodeEditActivity.class);
		intent.putExtra("actionMode", NodeEditActivity.ACTIONMODE_EDIT);
		intent.putExtra("node_id", nodeId);
		startActivity(intent);
	}

	private void runViewNodeActivity(long nodeId) {		
		Intent intent = new Intent(this, NodeViewActivity.class);
		intent.putExtra("node_id", nodeId);
		startActivity(intent);
	}

	private void runExpandSelection(long id) {
		Intent intent = new Intent(this, OutlineActivity.class);
		intent.putExtra("node_id", id);
		startActivity(intent);
	}
	
	private void runDeleteNode(final long node_id) {	
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setMessage("Are you sure you want to delete?")
				.setCancelable(false)
				.setPositiveButton("Yes",
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int id) {
								appInst.getDB().removeFile(node_id);
								refreshDisplay();
							}
						})
				.setNegativeButton("No", new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int id) {
						dialog.cancel();
					}
				});
		builder.create().show();
	}

	private boolean runSearch() {
		return onSearchRequested();
	}
	
	private boolean runShowSettings() {
		Intent intent = new Intent(this, SettingsActivity.class);
		startActivity(intent);
		return true;
	}

	private class SynchServiceReceiver extends BroadcastReceiver {
		@Override
		public void onReceive(Context context, Intent intent) {
			if (intent.getBooleanExtra(Synchronizer.SYNC_DONE, false)) {
				if (intent.getBooleanExtra("showToast", true))
					Toast.makeText(context, "Synchronization Successful",
							Toast.LENGTH_SHORT).show();
				refreshDisplay();
			}
		}
	}
	

//	@Override
//	public void onActivityResult(int requestCode, int resultCode, Intent intent) {
//
//		switch (requestCode) {
//		case NodeEncryption.DECRYPT_MESSAGE:
//			if (resultCode != RESULT_OK || intent == null)
//				return;
//			
//			Node node = this.appInst.nodestackTop();
//			this.appInst.popNodestack();
//			parseEncryptedNode(intent, node);
//			this.runExpandSelection(node);
//			break;
//		}
//	}
//	/**
//	 * This calls startActivityForResult() with Encryption.DECRYPT_MESSAGE. The
//	 * result is handled by onActivityResult() in this class, which calls a
//	 * function to parse the resulting plain text file.
//	 */
//	private void runDecryptAndExpandNode(Node node) {
//		// if suitable APG version is installed
//		if (NodeEncryption.isAvailable((Context) this)) {
//			// retrieve the encrypted file data
//			OrgFile orgfile = new OrgFile(node.name, getBaseContext());
//			byte[] rawData = orgfile.getRawFileData();
//			// save node so parsing function knows which node to parse into.
//			appInst.pushNodestack(node);
//			// and send it to APG for decryption
//			NodeEncryption.decrypt(this, rawData);
//		}
//	}
//
//	/**
//	 * This function is called with the results of
//	 * {@link #runDecryptAndExpandNode}.
//	 */
//	private void parseEncryptedNode(Intent data, Node node) {
//		OrgFileParser ofp = new OrgFileParser(getBaseContext(), appInst);
//
//		String decryptedData = data
//				.getStringExtra(NodeEncryption.EXTRA_DECRYPTED_MESSAGE);
//
//		//ofp.parse(node, new BufferedReader(new StringReader(decryptedData)));
//	}
}
