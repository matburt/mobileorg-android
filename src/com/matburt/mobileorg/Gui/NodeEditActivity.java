package com.matburt.mobileorg.Gui;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.ActionBar;
import android.support.v4.app.ActionBar.Tab;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.view.MenuInflater;

import com.matburt.mobileorg.R;
import com.matburt.mobileorg.Parsing.MobileOrgApplication;
import com.matburt.mobileorg.Parsing.NodeWrapper;
import com.matburt.mobileorg.Parsing.OrgDatabase;
import com.matburt.mobileorg.Parsing.OrgFile;
import com.matburt.mobileorg.Synchronizers.Synchronizer;

public class NodeEditActivity extends FragmentActivity {
	public final static String ACTIONMODE_CREATE = "create";
	public final static String ACTIONMODE_EDIT = "edit";

	private NodeWrapper node;
	private String actionMode;
	
	private OrgDatabase orgDB;
	private MobileOrgApplication appInst;
	
	private NodeEditDetailsFragment detailsFragment;
	private NodeEditBodyFragment payloadFragment;
	private NodeEditBodyFragment rawPayloadFragment;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		Intent intent = getIntent();
		this.actionMode = intent.getStringExtra("actionMode");

		this.appInst = (MobileOrgApplication) this.getApplication();
		this.orgDB = appInst.getDB();
		
		init();
		setupActionbar();
	}
	
	private void init() {
		Intent intent = getIntent();
		
		if (this.actionMode == null) {
			this.actionMode = ACTIONMODE_CREATE;

			// TODO Support intents
//			String subject = intent
//					.getStringExtra("android.intent.extra.SUBJECT");
//			String text = intent.getStringExtra("android.intent.extra.TEXT");
//			titleView.setText(subject);
//
//			node = new NodeWrapper(null);
//			payloadView.setText(text);
//			setSpinner(todoStateView, appInst.getDB().getTodos(), defaultTodo);
//			setSpinner(priorityView, appInst.getDB().getPriorities(), "");
		} else if (this.actionMode.equals(ACTIONMODE_CREATE)) {
			node = new NodeWrapper(null);
		} else if (this.actionMode.equals(ACTIONMODE_EDIT)) {
			long nodeId = intent.getLongExtra("node_id", 0);
			node = new NodeWrapper(appInst.getDB().getNode(nodeId));
		}
	}


	private void setupActionbar() {
		setContentView(R.layout.editnode);
		ActionBar actionbar = getSupportActionBar();

		actionbar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);

		this.detailsFragment = new NodeEditDetailsFragment(node, actionMode);
		ActionBar.Tab detailsTab = actionbar.newTab().setText("Details");
		detailsTab.setTabListener(new TabListener(detailsFragment, "details"));
		actionbar.addTab(detailsTab);
	    
		this.payloadFragment = new NodeEditBodyFragment(node.getCleanedPayload(orgDB), true);
		ActionBar.Tab payloadTab = actionbar.newTab().setText("Payload");
		payloadTab.setTabListener(new TabListener(payloadFragment, "payload"));
		actionbar.addTab(payloadTab);

	    this.rawPayloadFragment = new NodeEditBodyFragment(node.getRawPayload(orgDB), false);
	    ActionBar.Tab rawPayloadTab = actionbar.newTab().setText("Raw Payload");
	    rawPayloadTab.setTabListener(new TabListener(rawPayloadFragment, "raw_payload"));
	    actionbar.addTab(rawPayloadTab);
	}
	

	private class TabListener implements ActionBar.TabListener {
		Fragment fragment;

		public TabListener(Fragment fragment, String tag) {
			this.fragment = fragment;
			
			FragmentManager fragmentManager = getSupportFragmentManager();
			FragmentTransaction fragmentTransaction = fragmentManager
					.beginTransaction();
		    fragmentTransaction.add(R.id.editnode_fragment_container, fragment, tag);
		    fragmentTransaction.hide(fragment);
		    fragmentTransaction.commit();
		}

		@Override
		public void onTabReselected(Tab tab, FragmentTransaction ft) {
		}

		@Override
		public void onTabSelected(Tab tab, FragmentTransaction ft) {			
			// The line above throws a NullPointerException last time I tried...
			//ft.add(R.id.editnode_fragment_container, fragment, null);
			
			FragmentManager fragmentManager = getSupportFragmentManager();
			FragmentTransaction fragmentTransaction = fragmentManager
					.beginTransaction();
		    
		    fragmentTransaction.show(fragment);
		    fragmentTransaction.commit();
		}

		@Override
		public void onTabUnselected(Tab tab, FragmentTransaction ft) {
			if (fragment != null) {
				// The line above throws a NullPointerException last time I tried...
				//ft.detach(fragment);
				
				FragmentManager fragmentManager = getSupportFragmentManager();
				FragmentTransaction fragmentTransaction = fragmentManager
						.beginTransaction();
			    
				fragmentTransaction.hide(fragment);
			    fragmentTransaction.commit();
			}
		}
	};
	
    @Override
	public boolean onCreateOptionsMenu(android.support.v4.view.Menu menu) {
    	super.onCreateOptionsMenu(menu);
		MenuInflater inflater = getMenuInflater();
	    inflater.inflate(R.menu.nodeedit, menu);
		return true;
	}
	
	@Override
	public boolean onOptionsItemSelected(android.support.v4.view.MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
			// TODO do something
			return true;
			
		case R.id.nodeedit_save:
			save();
			setResult(RESULT_OK);
			finish();
			return true;
			
		case R.id.nodeedit_cancel:
			doCancel();
			return true;
		}
		return false;
	}
	
	@Override
	public void onBackPressed() {
		doCancel();
	}
	
	private void doCancel() {
		if(this.detailsFragment.hasEdits() == false) {
			setResult(RESULT_CANCELED);
			finish();
			return;
		}
		
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setMessage(R.string.node_edit_prompt)
				.setCancelable(false)
				.setPositiveButton(R.string.yes,
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int id) {
								setResult(RESULT_CANCELED);
								finish();
							}
						})
				.setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int id) {
						dialog.cancel();
					}
				});
		builder.create().show();
	}

	
	private void save() {
		String newTitle = this.detailsFragment.getTitle();
		String newTodo = this.detailsFragment.getTodo();
		String newPriority = this.detailsFragment.getPriority();
		String newPayload = this.payloadFragment.getText();
		String newTags = this.detailsFragment.getTagsSelection();
		
		if (this.actionMode.equals(ACTIONMODE_CREATE)) {
			MobileOrgApplication appInst = (MobileOrgApplication) this.getApplication();
			OrgDatabase orgDB = appInst.getDB();
			long file_id = orgDB.addOrUpdateFile(OrgFile.CAPTURE_FILE, "Captures", "", true);
			Long parent = orgDB.getFileNodeId(OrgFile.CAPTURE_FILE);
			long node_id = orgDB.addNode(parent, newTitle, newTodo, newPriority, newTags, file_id);
			
			boolean addTimestamp = PreferenceManager.getDefaultSharedPreferences(
					this).getBoolean("captureWithTimestamp", false);
			if(addTimestamp)
				newPayload += "\n" + getTimestamp() + "\n";
			
			orgDB.addNodePayload(node_id, newPayload);
			
		} else if (this.actionMode.equals(ACTIONMODE_EDIT)) {
			try {
				editNode(newTitle, newTodo, newPriority, newPayload, newTags);
			} catch (IOException e) {
			}
		}
		Intent intent = new Intent(Synchronizer.SYNC_UPDATE);
		intent.putExtra(Synchronizer.SYNC_DONE, true);
		intent.putExtra("showToast", false);
		sendBroadcast(intent);
	}
	
	/**
	 * Takes a Node and five strings, representing edits to the node.
	 * This function will generate a new edit entry for each value that was 
	 * changed.
	 */ 
	private void editNode(String newTitle, String newTodo,
			String newPriority, String newPayload, String newTags) throws IOException {
		boolean generateEdits = !node.getFileName(orgDB).equals(OrgFile.CAPTURE_FILE);
		
		if (!node.getName().equals(newTitle)) {
			if (generateEdits)
				orgDB.addEdit("heading", node.getNodeId(orgDB), newTitle, node.getName(), newTitle);
			node.setName(newTitle, orgDB);
		}
		if (newTodo != null && !node.getTodo().equals(newTodo)) {
			if (generateEdits)
				orgDB.addEdit("todo", node.getNodeId(orgDB), newTitle, node.getTodo(), newTodo);
			node.setTodo(newTodo, orgDB);
		}
		if (newPriority != null && !node.getPriority().equals(newPriority)) {
			if (generateEdits)
				orgDB.addEdit("priority", node.getNodeId(orgDB), newTitle, node.getPriority(),
					newPriority);
			node.setPriority(newPriority, orgDB);
		}
		if (!node.getCleanedPayload(orgDB).equals(newPayload)) {
			String newRawPayload = node.getPayloadResidue(orgDB) + newPayload;
	
			if (generateEdits)
				orgDB.addEdit("body", node.getNodeId(orgDB), newTitle, node.getRawPayload(orgDB), newRawPayload);
			node.setPayload(newRawPayload, orgDB);
		}
		if(!node.getTags().equals(newTags)) {
			if (generateEdits) {
				orgDB.addEdit("tags", node.getNodeId(orgDB), newTitle,
						node.getTagsWithoutInheritet(),
						NodeWrapper.getTagsWithoutInheritet(newTags));
			}
			node.setTags(newTags, orgDB);
		}
	}
	
	public static String getTimestamp() {
		SimpleDateFormat sdf = new SimpleDateFormat("[yyyy-MM-dd EEE HH:mm]");		
		return sdf.format(new Date());
	}
}
