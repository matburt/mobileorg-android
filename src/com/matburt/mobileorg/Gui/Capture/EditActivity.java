package com.matburt.mobileorg.Gui.Capture;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.ActionBar;
import android.support.v4.app.ActionBar.Tab;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.MenuItem;
import android.support.v4.view.SubMenu;
import android.support.v4.view.Window;
import android.view.MenuInflater;
import android.view.WindowManager;

import com.matburt.mobileorg.R;
import com.matburt.mobileorg.Parsing.MobileOrgApplication;
import com.matburt.mobileorg.Parsing.NodeWrapper;
import com.matburt.mobileorg.Parsing.OrgDatabase;
import com.matburt.mobileorg.Parsing.OrgFile;
import com.matburt.mobileorg.Services.TimeclockService;
import com.matburt.mobileorg.Synchronizers.Synchronizer;

public class EditActivity extends FragmentActivity {
	public final static String ACTIONMODE_CREATE = "create";
	public final static String ACTIONMODE_EDIT = "edit";
	public final static String ACTIONMODE_ADDCHILD = "add_child";


	private NodeWrapper node;
	private String actionMode;
	
	private OrgDatabase orgDB;
	private MobileOrgApplication appInst;
	
	private EditDetailsFragment detailsFragment;
	private EditPayloadFragment payloadFragment;
	private EditPayloadFragment rawPayloadFragment;
	private long node_id;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		SharedPreferences appSettings = PreferenceManager
				.getDefaultSharedPreferences(getBaseContext());
		if (appSettings.getBoolean("fullscreen", true)) {
			requestWindowFeature(Window.FEATURE_NO_TITLE);
			getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
					WindowManager.LayoutParams.FLAG_FULLSCREEN);
		}
		
		setContentView(R.layout.edit);

		Intent intent = getIntent();
		this.actionMode = intent.getStringExtra("actionMode");
		this.node_id = intent.getLongExtra("node_id", -1);

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

			if(text != null && subject != null) {
				subject = "[[" + text + "][" + subject + "]]";
				text = "";
			}
			
			if(subject == null)
				subject = "";
			if(text == null)
				text = "";

			node = new NodeWrapper(null, orgDB);
			this.detailsFragment.init(this.node, this.actionMode, defaultTodo, subject);
			this.payloadFragment.init(text, true);
			this.actionMode = ACTIONMODE_CREATE;
		} else if (this.actionMode.equals(ACTIONMODE_CREATE)) {
			node = new NodeWrapper(null, orgDB);
			this.detailsFragment.init(this.node, this.actionMode, defaultTodo);
			this.payloadFragment.init("", true);
		} else if (this.actionMode.equals(ACTIONMODE_EDIT)) {
			long nodeId = intent.getLongExtra("node_id", 0);
			node = new NodeWrapper(appInst.getDB().getNode(nodeId), orgDB);
			this.detailsFragment.init(this.node, this.actionMode, defaultTodo);
			this.payloadFragment.init(this.node.getCleanedPayload(), true);
		} else if (this.actionMode.equals(ACTIONMODE_ADDCHILD)) {
			node = new NodeWrapper(this.node_id, orgDB);
			this.detailsFragment.init(this.node, this.actionMode, defaultTodo);
			this.payloadFragment.init("", true);
		}

		this.rawPayloadFragment.init(node.getRawPayload(), false);
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
	    
		if (this.node_id > -1) {
			SubMenu subMenu = menu.addSubMenu(R.string.menu_advanced);
			MenuItem item = subMenu.add(R.string.menu_advanced,
					R.string.menu_delete, 0, R.string.menu_delete);
			item.setIcon(R.drawable.ic_input_delete);

			item = subMenu.add(R.string.menu_advanced, R.string.menu_clockin,
					1, R.string.menu_clockin);
			item.setIcon(R.drawable.ic_menu_today);
			
			item = subMenu.add(R.string.menu_advanced, R.string.menu_archive,
					1, R.string.menu_archive);
			
			item = subMenu.add(R.string.menu_advanced, R.string.menu_archive_tosibling,
					1, R.string.menu_archive_tosibling);

			MenuItem subMenuItem = subMenu.getItem();
			subMenuItem.setIcon(R.drawable.ic_menu_moreoverflow);
			subMenuItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
		}
		return true;
	}
	
	@Override
	public boolean onOptionsItemSelected(android.support.v4.view.MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
			doCancel();
			return true;
			
		case R.id.nodeedit_save:
			save();
			setResult(RESULT_OK);
			finish();
			return true;
			
		case R.id.nodeedit_cancel:
			doCancel();
			return true;
			
		case R.string.menu_delete:
			runDeleteNode(this.node_id);
			return true;
			
		case R.string.menu_clockin:
			startTimeClockingService(this.node_id);
			return true;
			
		case R.string.menu_archive:
			runArchiveNode(this.node_id, "archive");
			return true;
			
		case R.string.menu_archive_tosibling:
			runArchiveNode(this.node_id, "archive-sibling");
			return true;		
		}
		return false;
	}
	
	private void runDeleteNode(final long node_id) {	
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setMessage(R.string.outline_delete_prompt)
				.setCancelable(false)
				.setPositiveButton(R.string.yes,
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int id) {
								deleteNode(node_id);
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
	
	private void runArchiveNode(final long node_id, final String editString) {	
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setMessage(R.string.outline_archive_prompt)
				.setCancelable(false)
				.setPositiveButton(R.string.yes,
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int id) {
								if(editString.equals("archive"))
									archiveNode(node_id);
								else
									archiveNodeToSibling(node_id);
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
	
	private void archiveNode(long node_id) {
		NodeWrapper node = new NodeWrapper(node_id, appInst.getDB());
		appInst.getDB().addEdit("archive", node.getNodeId(),
				node.getName(), "", "");
		node.close();
		appInst.getDB().deleteNode(node_id);
	}
	
	private void archiveNodeToSibling(long node_id) {
		NodeWrapper node = new NodeWrapper(node_id, appInst.getDB());
		appInst.getDB().addEdit("archive-sibling", node.getNodeId(),
				node.getName(), "", "");
		
		NodeWrapper parent = node.getParent();
		if(parent != null) {
			NodeWrapper child = parent.getChild("Archive");
			if(child != null) {
				node.setParent(child.getId());
				child.close();
			} else {
				long child_id = appInst.getDB().addNode(parent.getId(), "Archive", "", "", "", parent.getFileId());
				node.setParent(child_id);
			}
			parent.close();
			node.close();
		} else {
			node.close();
			appInst.getDB().deleteNode(node_id);
		}
	}
	
	private void deleteNode(long node_id) {
		NodeWrapper node = new NodeWrapper(node_id, appInst.getDB());
		appInst.getDB().addEdit("delete", node.getNodeId(),
				node.getName(), "", "");
		node.close();
		appInst.getDB().deleteNode(node_id); // TODO make recursive
	}
	
	private void startTimeClockingService(long nodeId) {
		Intent intent = new Intent(EditActivity.this, TimeclockService.class);
		intent.putExtra(TimeclockService.NODE_ID, nodeId);
		startService(intent);
	}
	
	@Override
	public void onBackPressed() {
		doCancel();
	}
	
	private void doCancel() {
		if(this.detailsFragment.hasEdits() == false &&
                   this.payloadFragment.hasEdits() == false) {
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
	
	
	private void insertChangesIntoPayloadResidue() {
		node.getPayload().insertOrReplace("SCHEDULED:", detailsFragment.getScheduled());
		node.getPayload().insertOrReplace("DEADLINE:", detailsFragment.getDeadline());
	}
	
	private void save() {
		String newTitle = this.detailsFragment.getTitle();
		String newTodo = this.detailsFragment.getTodo();
		String newPriority = this.detailsFragment.getPriority();
		String newTags = this.detailsFragment.getTags();
		NodeWrapper newParent = this.detailsFragment.getLocation();
		StringBuilder newCleanedPayload = new StringBuilder(this.payloadFragment.getText());
		insertChangesIntoPayloadResidue();
		String newPayloadResidue = node.getPayload().getNewPayloadResidue();
						
		if (this.actionMode.equals(ACTIONMODE_CREATE) || this.actionMode.equals(ACTIONMODE_ADDCHILD)) {
			MobileOrgApplication appInst = (MobileOrgApplication) this.getApplication();
			OrgDatabase orgDB = appInst.getDB();
			long node_id, parent_id;
                        long file_id;

                        if (newParent == null) {
                                file_id = orgDB.addOrUpdateFile(OrgFile.CAPTURE_FILE, OrgFile.CAPTURE_FILE_ALIAS, "", true);
                                parent_id = orgDB.getFileNodeId(orgDB.getFilename(file_id));
                        } else {
                                file_id = newParent.getFileId();
                                parent_id = newParent.getId();
                        }
			node_id = orgDB.addNode(parent_id, newTitle, newTodo, newPriority, newTags, file_id);
			
			boolean addTimestamp = PreferenceManager.getDefaultSharedPreferences(
					this).getBoolean("captureWithTimestamp", false);
			if(addTimestamp)
				newCleanedPayload.append("\n").append(getTimestamp()).append("\n");
			
			orgDB.addNodePayload(node_id, newCleanedPayload.toString() + newPayloadResidue);
			
			makeNewheadingEditNode(node_id, newParent);
	
			if(PreferenceManager.getDefaultSharedPreferences(
					this).getBoolean("calendarEnabled", false))
				appInst.getCalendarSyncService().insertNode(node_id);

		} else if (this.actionMode.equals(ACTIONMODE_EDIT)) {
			try {
				makeEditNodes(newTitle, newTodo, newPriority,
						newCleanedPayload.toString(), newTags, newParent);
			} catch (IOException e) {
			}
		}
		Intent intent = new Intent(Synchronizer.SYNC_UPDATE);
		intent.putExtra(Synchronizer.SYNC_DONE, true);
		intent.putExtra("showToast", false);
		sendBroadcast(intent);
	}
	
	private void makeNewheadingEditNode(long node_id, NodeWrapper parent) {
		boolean generateEdits = parent != null && !parent.getFileName().equals(OrgFile.CAPTURE_FILE);
		if(generateEdits == false)
			return;

		// Add new heading nodes need the entire content of node without star headings
		String newContent = orgDB.nodeToString(node_id, 1).replaceFirst("[\\*]*", "");
		orgDB.addEdit("addheading", parent.getNodeId(), parent.getName(), "", newContent);
	}
	
	/**
	 * Takes a Node and five strings, representing edits to the node.
	 * This function will generate a new edit entry for each value that was 
	 * changed.
	 */ 
	private void makeEditNodes(String newTitle, String newTodo,
			String newPriority, String newCleanedPayload, String newTags, NodeWrapper newParent) throws IOException {
		boolean generateEdits = !node.getFileName().equals(OrgFile.CAPTURE_FILE);
		
		if (!node.getName().equals(newTitle)) {
			if (generateEdits)
				orgDB.addEdit("heading", node.getNodeId(), newTitle, node.getName(), newTitle);
			node.setName(newTitle);
		}
		if (newTodo != null && !node.getTodo().equals(newTodo)) {
			if (generateEdits)
				orgDB.addEdit("todo", node.getNodeId(), newTitle, node.getTodo(), newTodo);
			node.setTodo(newTodo);
		}
		if (newPriority != null && !node.getPriority().equals(newPriority)) {
			if (generateEdits)
				orgDB.addEdit("priority", node.getNodeId(), newTitle, node.getPriority(),
					newPriority);
			node.setPriority(newPriority);
		}
		if (!node.getCleanedPayload().equals(newCleanedPayload)
				|| !node.getPayload().getPayloadResidue()
						.equals(node.getPayload().getNewPayloadResidue())) {
			String newRawPayload = node.getPayload()
					.getNewPayloadResidue() + newCleanedPayload;

			if (generateEdits)
				orgDB.addEdit("body", node.getNodeId(), newTitle, node.getRawPayload(), newRawPayload);
			node.setPayload(newRawPayload);
		}
		if(!node.getTags().equals(newTags)) {
			if (generateEdits) {
				orgDB.addEdit("tags", node.getNodeId(), newTitle,
						node.getTagsWithoutInheritet(),
						NodeWrapper.getTagsWithoutInheritet(newTags));
			}
			node.setTags(newTags);
		}
		if(newParent.getId() != node.getParentId()) {
			if(generateEdits) {
				orgDB.addEdit("refile", node.getNodeId(), newTitle, "", newParent.getNodeId());
			}
			node.setParent(newParent);
		}
	}
	
	public static String getTimestamp() {
		SimpleDateFormat sdf = new SimpleDateFormat("[yyyy-MM-dd EEE HH:mm]");		
		return sdf.format(new Date());
	}
}
