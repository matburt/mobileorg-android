package com.matburt.mobileorg.Gui.Capture;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import android.app.AlertDialog;
import android.content.ContentResolver;
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
import com.matburt.mobileorg.Parsing.OrgDatabaseOld;
import com.matburt.mobileorg.Services.TimeclockService;
import com.matburt.mobileorg.Synchronizers.Synchronizer;
import com.matburt.mobileorg.provider.OrgContract.OrgData;
import com.matburt.mobileorg.provider.OrgEdit;
import com.matburt.mobileorg.provider.OrgNode;
import com.matburt.mobileorg.provider.OrgProviderUtil;
import com.matburt.mobileorg.util.FileUtils;

public class EditActivity extends FragmentActivity {
	public final static String ACTIONMODE_CREATE = "create";
	public final static String ACTIONMODE_EDIT = "edit";
	public final static String ACTIONMODE_ADDCHILD = "add_child";

	private OrgNode node;
	private String actionMode;
		
	private EditDetailsFragment detailsFragment;
	private EditPayloadFragment payloadFragment;
	private EditPayloadFragment rawPayloadFragment;
	private ContentResolver resolver;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		this.resolver = getContentResolver();

		SharedPreferences appSettings = PreferenceManager
				.getDefaultSharedPreferences(getBaseContext());
		if (appSettings.getBoolean("fullscreen", true)) {
			requestWindowFeature(Window.FEATURE_NO_TITLE);
			getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
					WindowManager.LayoutParams.FLAG_FULLSCREEN);
		}
		
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
		
		setContentView(R.layout.edit);
		
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
		Intent intent = getIntent();
		this.actionMode = intent.getStringExtra("actionMode");
		long node_id = intent.getLongExtra("node_id", -1);	
		
		if (this.detailsFragment == null)
			this.detailsFragment = new EditDetailsFragment();
		if (this.payloadFragment == null)
			this.payloadFragment = new EditPayloadFragment();
		if (this.rawPayloadFragment == null)
			this.rawPayloadFragment = new EditPayloadFragment();
		
		String defaultTodo = PreferenceManager
				.getDefaultSharedPreferences(this).getString("defaultTodo", "");
		
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

			this.node = new OrgNode();
			this.detailsFragment.init(this.node, this.actionMode, defaultTodo, subject);
			this.payloadFragment.init(text, true);
			this.actionMode = ACTIONMODE_CREATE;
		} else if (this.actionMode.equals(ACTIONMODE_CREATE)) {
			this.node = new OrgNode();
			this.detailsFragment.init(this.node, this.actionMode, defaultTodo);
			this.payloadFragment.init("", true);
		} else if (this.actionMode.equals(ACTIONMODE_EDIT)) {
			this.node = new OrgNode(node_id, getContentResolver());
			this.detailsFragment.init(this.node, this.actionMode, defaultTodo);
			this.payloadFragment.init(this.node.getCleanedPayload(), true);
		} else if (this.actionMode.equals(ACTIONMODE_ADDCHILD)) {
			this.node = new OrgNode(node_id, getContentResolver());
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
	    
		if (this.node.id > -1) {
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
			runDeleteNode();
			return true;
			
		case R.string.menu_clockin:
			startTimeClockingService();
			return true;
			
		case R.string.menu_archive:
			runArchiveNode("archive");
			return true;
			
		case R.string.menu_archive_tosibling:
			runArchiveNode("archive-sibling");
			return true;		
		}
		return false;
	}
	
	private void runDeleteNode() {	
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setMessage(R.string.outline_delete_prompt)
				.setCancelable(false)
				.setPositiveButton(R.string.yes,
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int id) {
								deleteNode();
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
	
	private void runArchiveNode(final String editString) {	
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setMessage(R.string.outline_archive_prompt)
				.setCancelable(false)
				.setPositiveButton(R.string.yes,
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int id) {
								if(editString.equals("archive"))
									archiveNode();
								else
									archiveNodeToSibling();
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
	
	private void archiveNode() {
		OrgEdit edit = new OrgEdit(node, OrgEdit.TYPE.ARCHIVE, resolver);
		edit.write(resolver);
		resolver.delete(OrgData.buildIdUri(node.id), null, null);
	}
	
	private void archiveNodeToSibling() {
		OrgEdit edit = new OrgEdit(node, OrgEdit.TYPE.ARCHIVE_SIBLING, resolver);
		edit.write(resolver);
		
		// TODO
//		OrgNode parent = node.getParent();
//		if(parent != null) {
//			NodeWrapper child = parent.getChild("Archive");
//			if(child != null) {
//				node.setParent(child.getId());
//				child.close();
//			} else {
//				OrgNode archiveNode = new OrgNode();
//				archiveNode.name = "Archive";
//				archiveNode.parentId = parent.id;
//				archiveNode.fileId = parent.fileId;
//				long child_id = node.addNode(resolver);
//				node.setParent(child_id);
//			}
//		} else {
//			resolver.delete(OrgData.buildIdUri(node.id), null, null);
//		}
	}
	
	private void deleteNode() {
		OrgEdit edit = new OrgEdit(node, OrgEdit.TYPE.DELETE, resolver);
		edit.write(resolver);
		resolver.delete(OrgData.buildIdUri(node.id), null, null);
	}
	
	private void startTimeClockingService() {
		Intent intent = new Intent(EditActivity.this, TimeclockService.class);
		intent.putExtra(TimeclockService.NODE_ID, node.id);
		startService(intent);
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
	
	
	private void insertChangesIntoPayloadResidue() {
		node.getPayload().insertOrReplace("SCHEDULED:", detailsFragment.getScheduled());
		node.getPayload().insertOrReplace("DEADLINE:", detailsFragment.getDeadline());
	}
	
	private void save() {
		OrgNode newNode = new OrgNode();
		newNode.name = this.detailsFragment.getTitle();
		newNode.todo = this.detailsFragment.getTodo();
		newNode.priority = this.detailsFragment.getPriority();
		newNode.tags = this.detailsFragment.getTags();
		NodeWrapper newParent = this.detailsFragment.getLocation();
		StringBuilder newCleanedPayload = new StringBuilder(this.payloadFragment.getText());
		insertChangesIntoPayloadResidue();
		String newPayloadResidue = node.getPayload().getNewPayloadResidue();
				
		// TODO
//		if (this.actionMode.equals(ACTIONMODE_CREATE) || this.actionMode.equals(ACTIONMODE_ADDCHILD)) {
//			if (newParent == null) {
//				newNode.fileId = orgDB.addOrUpdateFile(FileUtils.CAPTURE_FILE,
//						FileUtils.CAPTURE_FILE_ALIAS, "", true);
//				newNode.parentId = orgDB.getFileNodeId(orgDB.getFilename(newNode.fileId));
//			} else {
//				newNode.fileId = newParent.getFileId();
//				newNode.parentId = newParent.getId();
//			}
//			
//			newNode.addNode(resolver);
//			
//			boolean addTimestamp = PreferenceManager.getDefaultSharedPreferences(
//					this).getBoolean("captureWithTimestamp", false);
//			if(addTimestamp)
//				newCleanedPayload.append("\n").append(getTimestamp()).append("\n");
//			
//			orgDB.addNodePayload(node.id, newCleanedPayload.toString() + newPayloadResidue);
//			
//			makeNewheadingEditNode(node.id, newParent);
//	
//			if(PreferenceManager.getDefaultSharedPreferences(
//					this).getBoolean("calendarEnabled", false))
//				appInst.getCalendarSyncService().insertNode(node.id);
//
//		} else if (this.actionMode.equals(ACTIONMODE_EDIT)) {
//			try {
//				makeEditNodes(newNode, newParent);
//			} catch (IOException e) {
//			}
//		}
		Intent intent = new Intent(Synchronizer.SYNC_UPDATE);
		intent.putExtra(Synchronizer.SYNC_DONE, true);
		intent.putExtra("showToast", false);
		sendBroadcast(intent);
	}
	
	private void makeNewheadingEditNode(long node_id, OrgNode parent) {
//		boolean generateEdits = parent != null && !parent.getFileName().equals(FileUtils.CAPTURE_FILE);
//		if(generateEdits == false)
//			return;
//
//		// Add new heading nodes need the entire content of node without star headings
//		OrgNode node = new OrgNode();
//		String newContent = orgDB.nodeToString(node_id, 1).replaceFirst("[\\*]*", "");
//		OrgProviderUtil.addEdit("addheading", parent.id, parent.name, "", newContent, getContentResolver());
	}
	
	/**
	 * Takes a Node and five strings, representing edits to the node.
	 * This function will generate a new edit entry for each value that was 
	 * changed.
	 */ 
	private void makeEditNodes(OrgNode newNode, OrgNode newParent) throws IOException {
		boolean generateEdits = !node.getFilename(resolver).equals(FileUtils.CAPTURE_FILE);
		OrgEdit edit = new OrgEdit(node, OrgEdit.TYPE.TODO, resolver);
		
//		if (!node.name.equals(newNode.name)) {
//			if (generateEdits)
//				OrgProviderUtil.addEdit("heading", node.getNodeId(resolver), newNode.name, node.name, newNode.name, resolver);
//			//node.setName(newNode.name);
//		}
//		if (newNode.todo != null && !node.todo.equals(newNode.todo)) {
//			if (generateEdits)
//				OrgProviderUtil.addEdit("todo", node.getNodeId(resolver), newNode.name, node.todo, newNode.todo, resolver);
//			//node.setTodo(newNode.todo);
//		}
//		if (newNode.priority != null && !node.priority.equals(newNode.priority)) {
//			if (generateEdits)
//				OrgProviderUtil.addEdit("priority", node.getNodeId(resolver), newNode.name, node.priority,
//					newNode.priority, getContentResolver());
//			//node.setPriority(newNode.priority);
//		}
//		if (!node.getCleanedPayload().equals(newNode.getCleanedPayload())
//				|| !node.getPayload().getPayloadResidue()
//						.equals(node.getPayload().getNewPayloadResidue())) {
//			String newRawPayload = node.getPayload()
//					.getNewPayloadResidue() + newNode.getCleanedPayload();
//
//			if (generateEdits)
//				OrgProviderUtil.addEdit("body", node.getNodeId(resolver), newNode.name, node.getRawPayload(), newRawPayload, resolver);
//			//node.setPayload(newRawPayload);
//		}
//		if(!node.tags.equals(newNode.tags)) {
//			if (generateEdits) {
//				OrgProviderUtil.addEdit("tags", node.getNodeId(resolver), newNode.name,
//						"",//node.getTagsWithoutInheritet(),
//						NodeWrapper.getTagsWithoutInheritet(newNode.tags), resolver);
//			}
//			//node.setTags(newNode.tags);
//		}
//		if(newNode.parentId != node.parentId) {
//			if(generateEdits) {
//				OrgProviderUtil.addEdit("refile", node.getNodeId(resolver), newNode.name, "", newNode.getNodeId(resolver), resolver);
//			}
//			//node.setParent(newParent);
//		}
	}
	
	public static String getTimestamp() {
		SimpleDateFormat sdf = new SimpleDateFormat("[yyyy-MM-dd EEE HH:mm]");		
		return sdf.format(new Date());
	}
}
