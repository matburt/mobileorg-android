package com.matburt.mobileorg.Gui.Capture;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.ActionBar;
import android.support.v4.app.ActionBar.Tab;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentTransaction;
import android.text.TextUtils;
import android.view.MenuInflater;

import com.matburt.mobileorg.R;
import com.matburt.mobileorg.Parsing.MobileOrgApplication;
import com.matburt.mobileorg.Parsing.NodeWrapper;
import com.matburt.mobileorg.Parsing.OrgDatabase;
import com.matburt.mobileorg.Parsing.OrgFile;
import com.matburt.mobileorg.Synchronizers.Synchronizer;

public class EditActivity extends FragmentActivity {
	public final static String ACTIONMODE_CREATE = "create";
	public final static String ACTIONMODE_EDIT = "edit";

	private NodeWrapper node;
	private String actionMode;
	
	private OrgDatabase orgDB;
	private MobileOrgApplication appInst;
	
	private EditDetailsFragment detailsFragment;
	private EditPayloadFragment payloadFragment;
	private EditPayloadFragment rawPayloadFragment;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.edit);

		Intent intent = getIntent();
		this.actionMode = intent.getStringExtra("actionMode");

		this.appInst = (MobileOrgApplication) this.getApplication();
		this.orgDB = appInst.getDB();
		
		if (savedInstanceState != null) {
			this.detailsFragment = (EditDetailsFragment) getSupportFragmentManager()
					.getFragment(savedInstanceState,
							EditDetailsFragment.class.getName());
			this.payloadFragment = (EditPayloadFragment) getSupportFragmentManager()
					.getFragment(savedInstanceState,
							EditPayloadFragment.class.getName());
			this.rawPayloadFragment = (EditPayloadFragment) getSupportFragmentManager()
					.getFragment(savedInstanceState,
							EditPayloadFragment.class.getName() + "raw");
		}
		
		init();
		setupActionbarTabs(savedInstanceState);
	}

	
	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		getSupportFragmentManager().putFragment(outState,
				EditPayloadFragment.class.getName(), payloadFragment);
		getSupportFragmentManager().putFragment(outState,
				EditPayloadFragment.class.getName() + "raw", rawPayloadFragment);
		getSupportFragmentManager().putFragment(outState,
				EditDetailsFragment.class.getName(), detailsFragment);
        outState.putInt("tab", getSupportActionBar().getSelectedNavigationIndex());
	}
	
	private void init() {
		if (this.detailsFragment == null)
			this.detailsFragment = new EditDetailsFragment();
		if (this.payloadFragment == null)
			this.payloadFragment = new EditPayloadFragment();
		if (this.rawPayloadFragment == null)
			this.rawPayloadFragment = new EditPayloadFragment();
		
		String defaultTodo = PreferenceManager
				.getDefaultSharedPreferences(this).getString("defaultTodo", "");
		Intent intent = getIntent();
		
		if (this.actionMode == null) {
			String subject = intent
					.getStringExtra("android.intent.extra.SUBJECT");
			String text = intent.getStringExtra("android.intent.extra.TEXT");

			node = new NodeWrapper(null);
			this.detailsFragment.init(this.node, this.actionMode, defaultTodo, subject);
			this.payloadFragment.init(text, true);
			this.actionMode = ACTIONMODE_CREATE;
		} else if (this.actionMode.equals(ACTIONMODE_CREATE)) {
			node = new NodeWrapper(null);
			this.detailsFragment.init(this.node, this.actionMode, defaultTodo);
			this.payloadFragment.init("", true);
		} else if (this.actionMode.equals(ACTIONMODE_EDIT)) {
			long nodeId = intent.getLongExtra("node_id", 0);
			node = new NodeWrapper(appInst.getDB().getNode(nodeId));
			this.detailsFragment.init(this.node, this.actionMode, defaultTodo);
			this.payloadFragment.init(node.getCleanedPayload(orgDB), true);
		}

		this.rawPayloadFragment.init(node.getRawPayload(orgDB), false);
	}


	private void setupActionbarTabs(Bundle savedInstanceState) {
		ActionBar actionbar = getSupportActionBar();

		actionbar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);

		ActionBar.Tab detailsTab = actionbar.newTab().setText("Details");
		detailsTab.setTabListener(new TabListener(detailsFragment, "details"));
		actionbar.addTab(detailsTab);
	    
		ActionBar.Tab payloadTab = actionbar.newTab().setText("Payload");
		payloadTab.setTabListener(new TabListener(payloadFragment, "payload"));
		actionbar.addTab(payloadTab);

	    ActionBar.Tab rawPayloadTab = actionbar.newTab().setText("Raw Payload");
	    rawPayloadTab.setTabListener(new TabListener(rawPayloadFragment, "raw_payload"));
	    actionbar.addTab(rawPayloadTab);
	    
		if (savedInstanceState != null) {
            actionbar.setSelectedNavigationItem(savedInstanceState.getInt("tab", 0));
        }
	}
	

	private class TabListener implements ActionBar.TabListener {
		Fragment fragment;

		public TabListener(Fragment fragment, String tag) {
			this.fragment = fragment;

			FragmentTransaction fragmentTransaction = getSupportFragmentManager()
					.beginTransaction();

			if (fragment != null && fragment.isAdded() == false) {
				fragmentTransaction.add(R.id.editnode_fragment_container,
						fragment, tag);
			}

			fragmentTransaction.hide(fragment).commit();
		}

		@Override
		public void onTabSelected(Tab tab, FragmentTransaction ft) {			
			FragmentTransaction fragmentTransaction = getSupportFragmentManager()
					.beginTransaction();
		    fragmentTransaction.show(fragment).commit();
		}

		@Override
		public void onTabUnselected(Tab tab, FragmentTransaction ft) {
			if (fragment != null) {
				FragmentTransaction fragmentTransaction = getSupportFragmentManager()
						.beginTransaction();
				fragmentTransaction.hide(fragment).commit();
			}
		}

		@Override
		public void onTabReselected(Tab tab, FragmentTransaction ft) {
		}
	};
	
    @Override
	public boolean onCreateOptionsMenu(android.support.v4.view.Menu menu) {
    	super.onCreateOptionsMenu(menu);
		MenuInflater inflater = getMenuInflater();
	    inflater.inflate(R.menu.edit, menu);
		return true;
	}
	
	@Override
	public boolean onOptionsItemSelected(android.support.v4.view.MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
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

	private StringBuilder insertOrReplace(StringBuilder payloadResidue,
			String key, String value) {
		final Pattern schedulePattern = Pattern.compile(key + "\\s*<[^>]+>");
		Matcher matcher = schedulePattern.matcher(payloadResidue);

		if (matcher.find()) {
			if (TextUtils.isEmpty(value))
				payloadResidue.delete(matcher.start(), matcher.end());
			else
				payloadResidue.replace(matcher.start(), matcher.end(), value);
		}
		else if(TextUtils.isEmpty(value) == false)
			payloadResidue.insert(0, value).append("\n");

		return payloadResidue;
	}
	
	private StringBuilder getNewPayloadResidue() {
		StringBuilder result = new StringBuilder();
		
		StringBuilder originalPayloadResidue = new StringBuilder(node.getPayloadResidue(orgDB));
		
		String newScheduled = this.detailsFragment.getScheduled();
		String newDeadline = this.detailsFragment.getDeadline();

		result = insertOrReplace(originalPayloadResidue, "SCHEDULED:", newScheduled);
		result = insertOrReplace(result, "DEADLINE:", newDeadline);
		
		return result;
	}
	
	private void save() {
		String newTitle = this.detailsFragment.getTitle();
		String newTodo = this.detailsFragment.getTodo();
		String newPriority = this.detailsFragment.getPriority();
		String newTags = this.detailsFragment.getTags();
		StringBuilder newCleanedPayload = new StringBuilder(this.payloadFragment.getText());
		StringBuilder newPayloadResidue = getNewPayloadResidue();
						
		if (this.actionMode.equals(ACTIONMODE_CREATE)) {
			MobileOrgApplication appInst = (MobileOrgApplication) this.getApplication();
			OrgDatabase orgDB = appInst.getDB();
			long file_id = orgDB.addOrUpdateFile(OrgFile.CAPTURE_FILE, "Captures", "", true);
			Long parent = orgDB.getFileNodeId(OrgFile.CAPTURE_FILE);
			long node_id = orgDB.addNode(parent, newTitle, newTodo, newPriority, newTags, file_id);
			
			boolean addTimestamp = PreferenceManager.getDefaultSharedPreferences(
					this).getBoolean("captureWithTimestamp", false);
			if(addTimestamp)
				newCleanedPayload.append("\n").append(getTimestamp()).append("\n");
			
			orgDB.addNodePayload(node_id, newCleanedPayload.toString() + newPayloadResidue.toString());
			
		} else if (this.actionMode.equals(ACTIONMODE_EDIT)) {

			try {
				editNode(newTitle, newTodo, newPriority,
						newCleanedPayload.toString(),
						newPayloadResidue.toString(), newTags);
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
			String newPriority, String newCleanedPayload, String newPayloadResidue, String newTags) throws IOException {
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
		if (!node.getCleanedPayload(orgDB).equals(newCleanedPayload)
				|| !node.getPayloadResidue(orgDB).equals(newPayloadResidue)) {
			String newRawPayload = newPayloadResidue + newCleanedPayload;
	
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
