package com.matburt.mobileorg.Gui.Capture;

import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;

import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.actionbarsherlock.view.SubMenu;
import com.matburt.mobileorg.R;
import com.matburt.mobileorg.OrgData.OrgEdit;
import com.matburt.mobileorg.OrgData.OrgFile;
import com.matburt.mobileorg.OrgData.OrgNode;
import com.matburt.mobileorg.Services.TimeclockService;
import com.matburt.mobileorg.util.OrgUtils;

public class EditActivity extends SherlockFragmentActivity {
	public final static String NODE_ID = "node_id";
	public final static String ACTIONMODE = "actionMode";
	public final static String ACTIONMODE_CREATE = "create";
	public final static String ACTIONMODE_EDIT = "edit";
	public final static String ACTIONMODE_ADDCHILD = "add_child";

	private OrgNode node;
	private String actionMode;

	private ContentResolver resolver;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.edit);
		this.resolver = getContentResolver();
		
		initState();
		invalidateOptionsMenu();
	}
	
	private void initState() {
		Intent intent = getIntent();
		this.actionMode = intent.getStringExtra(ACTIONMODE);
		long node_id = intent.getLongExtra(NODE_ID, -1);	
		
		if (this.actionMode == null) {
			this.node = OrgUtils.getCaptureIntentContents(intent);
			this.actionMode = ACTIONMODE_CREATE;
		} else if (this.actionMode.equals(ACTIONMODE_CREATE)) {
			this.node = new OrgNode();
		} else if (this.actionMode.equals(ACTIONMODE_EDIT)) {
			this.node = new OrgNode(node_id, getContentResolver()).findOriginalNode(resolver);
		} else if (this.actionMode.equals(ACTIONMODE_ADDCHILD)) {
			this.node = new OrgNode();
			this.node.parentId = node_id;
		}
	}
	
	public OrgNode getParentOrgNode() {
		if (this.actionMode.equals(ACTIONMODE_EDIT)) {
			OrgNode parent = node.getParent(resolver);

			if (parent == null) {
				parent = new OrgNode();
				parent.parentId = -2;
			}
			return parent;
		} else if (this.actionMode.equals(ACTIONMODE_CREATE))
			return null;
		else if (this.actionMode.equals(ACTIONMODE_ADDCHILD)) {			
			try {
				OrgNode parent = new OrgNode(this.node.parentId, resolver);
				Log.d("MobileOrg", "Setting parent " + this.node.parentId);
				return parent;
			} catch (IllegalArgumentException e) {}
		}
		
		return new OrgNode();
	}
	
	public OrgNode getOrgNode() {
		if(this.node == null)
			this.node = new OrgNode();
		return this.node;
	}
	
	public boolean isNodeModifiable() {
		return getOrgNode().isNodeEditable(resolver);
	}
	
	public boolean isNodeRefilable() {
		if(this.actionMode.equals(ACTIONMODE_CREATE))
			return false;
		else
			return getOrgNode().isNodeEditable(resolver);
	}
	
    @Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getSupportMenuInflater().inflate(R.menu.edit, menu);

		if(isNodeModifiable() == false)
			menu.findItem(R.id.nodeedit_save).setVisible(false);
		
		if (this.node != null && this.node.id >= 0 && isNodeModifiable()) {
			SubMenu subMenu = menu.addSubMenu(R.string.menu_advanced);
			MenuItem item = subMenu.add(R.string.menu_advanced,
					R.string.menu_delete, 0, R.string.menu_delete);
			item.setIcon(R.drawable.ic_menu_delete);

			item = subMenu.add(R.string.menu_advanced, R.string.menu_archive,
					1, R.string.menu_archive);
			item.setIcon(R.drawable.ic_menu_archive);
			
			item = subMenu.add(R.string.menu_advanced, R.string.menu_archive_tosibling,
					1, R.string.menu_archive_tosibling);
			item.setIcon(R.drawable.ic_menu_archive);

			item = subMenu.add(R.string.menu_advanced, R.string.menu_clockin,
					1, R.string.menu_clockin);
			item.setIcon(R.drawable.ic_menu_today);

			MenuItem subMenuItem = subMenu.getItem();
			subMenuItem.setIcon(R.drawable.ic_menu_moreoverflow);
			subMenuItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
		} else if(this.node != null && this.node.isFilenode(getContentResolver())) {
			SubMenu subMenu = menu.addSubMenu(R.string.menu_advanced);
			MenuItem item = subMenu.add(R.string.menu_advanced,
					R.string.menu_delete_file, 0, R.string.menu_delete_file);
			item.setIcon(R.drawable.ic_menu_delete);
			MenuItem subMenuItem = subMenu.getItem();
			subMenuItem.setIcon(R.drawable.ic_menu_moreoverflow);
			subMenuItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
		}
	    
    	return super.onCreateOptionsMenu(menu);
    }
    
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
			doCancel();
			return true;
			
		case R.id.nodeedit_save:
			saveEdits();
			setResult(RESULT_OK);
			finish();
			return true;
			
		case R.id.nodeedit_cancel:
			doCancel();
			return true;
			
		case R.string.menu_delete:
			runDeleteNode();
			return true;
			
		case R.string.menu_delete_file:
			runDeleteFileNode();
			return true;
			
		case R.string.menu_clockin:
			runTimeClockingService();
			return true;
			
		case R.string.menu_archive:
			runArchiveNode(false);
			return true;
			
		case R.string.menu_archive_tosibling:
			runArchiveNode(true);
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
								node.deleteNode(resolver);
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
	
	private void runArchiveNode(final boolean archiveToSibling) {	
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setMessage(R.string.outline_archive_prompt)
				.setCancelable(false)
				.setPositiveButton(R.string.yes,
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int id) {
								archiveNode(archiveToSibling);
							}
						})
				.setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int id) {
						dialog.cancel();
					}
				});
		builder.create().show();
	}

	private void archiveNode(boolean archiveToSibling) {
		if(hasEdits())
			saveEdits();
		
		if(archiveToSibling)
			node.archiveNodeToSibling(resolver);
		else
			node.archiveNode(resolver);
		OrgUtils.announceUpdate(getBaseContext());
		finish();
	}
	
	private void runDeleteFileNode() {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setMessage(R.string.outline_delete_prompt)
				.setCancelable(false)
				.setPositiveButton(R.string.yes,
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int id) {
								deleteFileNode();
							}
						})
				.setNegativeButton(R.string.no,
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int id) {
								dialog.cancel();
							}
						});
		builder.create().show();
	}
	
	private void deleteFileNode() {
		try {
			OrgFile file = new OrgFile(node.fileId, resolver);
			file.removeFile();
			OrgUtils.announceUpdate(this);
			finish();
		} catch (IllegalArgumentException e) {}
	}
	
	private void runTimeClockingService() {
		Intent intent = new Intent(EditActivity.this, TimeclockService.class);
		intent.putExtra(TimeclockService.NODE_ID, node.id);
		startService(intent);
	}
	
	@Override
	public void onBackPressed() {
		doCancel();
	}
	
	private void doCancel() {
		if(hasEdits() == false) {
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
	
	public boolean hasEdits() {
		OrgNode newNode = getEditedNode();
		OrgNode clonedNode = new OrgNode(this.node.id, resolver);
		int numberOfEdits = clonedNode.generateApplyEditNodes(newNode, resolver).size();
		
		if(numberOfEdits > 0)
			return true;
		else
			return false;
	}
	
	
	public void saveEdits() {
		OrgNode newNode = getEditedNode();
		
		boolean addTimestamp = PreferenceManager.getDefaultSharedPreferences(
				this).getBoolean("captureWithTimestamp", false);
		if(addTimestamp)
			newNode.getOrgNodePayload().add(OrgUtils.getTimestamp());
		
		
		if (this.actionMode.equals(ACTIONMODE_CREATE)) {
			newNode.write(resolver);
		} else if (this.actionMode.equals(ACTIONMODE_ADDCHILD)) {
			try {
				OrgEdit edit = newNode.createParentNewheading(resolver);
				edit.write(resolver);
			} catch (IllegalStateException e) {
				Log.e("MobileOrg", e.getLocalizedMessage());
			}
			newNode.write(resolver);

		} else if (this.actionMode.equals(ACTIONMODE_EDIT)) {
			this.node.generateApplyWriteEdits(newNode, resolver);
			this.node.updateAllNodes(resolver);
		}
		
		OrgUtils.announceUpdate(this);
	}
	
	public OrgNode getEditedNode() {
		HeadingFragment headingFragment = (HeadingFragment) getSupportFragmentManager()
				.findFragmentByTag("headingFragment");
		OrgNode newNode = headingFragment.getEditedOrgNode();
		
		TagsFragment tagsFragment = (TagsFragment) getSupportFragmentManager()
				.findFragmentByTag("tagsFragment");
		newNode.tags = tagsFragment.getTags();
		
		LocationFragment locationFragment = (LocationFragment) getSupportFragmentManager()
				.findFragmentByTag("locationFragment");
		OrgNode newParent = locationFragment.getLocationSelection();
		newNode.parentId = newParent.id;
		newNode.fileId = newParent.fileId;
		
		PayloadFragment payloadFragment = (PayloadFragment) getSupportFragmentManager()
				.findFragmentByTag("payloadFragment");
		newNode.setPayload(payloadFragment.getPayload());

		DatesFragment datesFragment = (DatesFragment) getSupportFragmentManager()
				.findFragmentByTag("datesFragment");
		newNode.getOrgNodePayload().modifyDates(datesFragment.getScheduled(),
				datesFragment.getDeadline(), datesFragment.getTimestamp());

		return newNode;
	}
}
