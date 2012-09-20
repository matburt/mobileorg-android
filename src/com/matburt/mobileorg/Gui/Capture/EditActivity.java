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
import com.matburt.mobileorg.Gui.ViewActivity;
import com.matburt.mobileorg.OrgData.OrgEdit;
import com.matburt.mobileorg.OrgData.OrgFile;
import com.matburt.mobileorg.OrgData.OrgNode;
import com.matburt.mobileorg.Services.TimeclockService;
import com.matburt.mobileorg.util.OrgFileNotFoundException;
import com.matburt.mobileorg.util.OrgNodeNotFoundException;
import com.matburt.mobileorg.util.OrgUtils;

public class EditActivity extends SherlockFragmentActivity implements
		PayloadFragment.OnPayloadModifiedListener,
		DatesFragment.OnDatesModifiedListener {
	public final static String NODE_ID = "node_id";
	public final static String ACTIONMODE = "actionMode";
	public final static String ACTIONMODE_CREATE = "create";
	public final static String ACTIONMODE_EDIT = "edit";
	public final static String ACTIONMODE_ADDCHILD = "add_child";

	/**
	 * Used by create or add_child, in case underlying data changes and parent
	 * can't be found on save.
	 */
	private String nodeOlpPath = "";
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
			try {
				this.node = new OrgNode(node_id, getContentResolver()).findOriginalNode(resolver);
				this.nodeOlpPath = node.getOlpId(resolver);
			} catch (OrgNodeNotFoundException e) {}
		} else if (this.actionMode.equals(ACTIONMODE_ADDCHILD)) {
			this.node = new OrgNode();
			this.node.parentId = node_id;
			
			try {
				OrgNode parent = new OrgNode(node_id, resolver);
				this.nodeOlpPath = parent.getOlpId(resolver);
			} catch (OrgNodeNotFoundException e) {}
		}
	}
	
	public OrgNode getParentOrgNode() {
		if (this.actionMode.equals(ACTIONMODE_EDIT)) {
			OrgNode parent;
			try {
				parent = node.getParent(resolver);
			} catch (OrgNodeNotFoundException e) {
				parent = new OrgNode();
				parent.parentId = -2;
			}
			return parent;
		} else if (this.actionMode.equals(ACTIONMODE_CREATE))
			return null;
		else if (this.actionMode.equals(ACTIONMODE_ADDCHILD)) {			
			try {
				OrgNode parent = new OrgNode(this.node.parentId, resolver);
				return parent;
			} catch (OrgNodeNotFoundException e) {}
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
		
		SubMenu subMenu = menu.addSubMenu(R.string.menu_advanced);
		MenuItem subMenuItem = subMenu.getItem();
		subMenuItem.setIcon(R.drawable.ic_menu_moreoverflow);
		subMenuItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
		MenuItem item = subMenu.add(R.string.menu_advanced,
				R.string.contextmenu_view, 0, R.string.contextmenu_view);
		item.setIcon(R.drawable.ic_menu_view);
		
		if (this.node != null && this.node.id >= 0 && isNodeModifiable()) {
			createNodeSubMenu(subMenu);
			subMenuItem.setVisible(true);
		}
		else if(this.node != null && this.node.isFilenode(getContentResolver())) {
			createFileNodeSubMenu(subMenu);
			subMenuItem.setVisible(true);
		}
	    
    	return super.onCreateOptionsMenu(menu);
    }
    
    private void createNodeSubMenu(Menu subMenu) {
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
    }
    
    private void createFileNodeSubMenu(Menu subMenu) {
		MenuItem item = subMenu.add(R.string.menu_advanced,
				R.string.menu_delete_file, 0, R.string.menu_delete_file);
		item.setIcon(R.drawable.ic_menu_delete);
		item = subMenu.add(R.string.menu_advanced, R.string.menu_recover,
				1, R.string.menu_recover);
		item.setIcon(R.drawable.ic_menu_archive);
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
			
		case R.string.contextmenu_view:
			runViewNodeActivity();
			return true;
			
		case R.string.menu_recover:
			runRecover();
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
			file.removeFile(resolver);
			OrgUtils.announceUpdate(this);
			finish();
		} catch (OrgFileNotFoundException e) {}
	}
	
	private void runViewNodeActivity() {		
		Intent intent = new Intent(this, ViewActivity.class);
		intent.putExtra(ViewActivity.NODE_ID, node.id);
		startActivity(intent);
	}
	
	private void runTimeClockingService() {
		Intent intent = new Intent(EditActivity.this, TimeclockService.class);
		intent.putExtra(TimeclockService.NODE_ID, node.id);
		startService(intent);
	}
	
	private void runRecover() {
		try {
			OrgFile orgFile = this.node.getOrgFile(resolver);
			Log.d("MobileOrg", orgFile.toString(resolver));
		} catch (OrgFileNotFoundException e) {
			e.printStackTrace();
		}
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
		
		int numberOfEdits = 0;
		try {
			OrgNode clonedNode = new OrgNode(this.node.id, resolver);
			numberOfEdits = clonedNode.generateApplyEditNodes(newNode, resolver).size();
		} catch (OrgNodeNotFoundException e) {}
		
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
			newNode.level = 1;
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
			this.node.generateApplyWriteEdits(newNode, this.nodeOlpPath, resolver);
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

		return newNode;
	}

	@Override
	public void onDatesModified() {
		PayloadFragment payloadFragment = (PayloadFragment) getSupportFragmentManager()
				.findFragmentByTag("payloadFragment");
		payloadFragment.switchToView();
	}
	
	@Override
	public void onPayloadModified() {
		DatesFragment datesFragment = (DatesFragment) getSupportFragmentManager()
				.findFragmentByTag("datesFragment");
		datesFragment.setupDates();
	}

	@Override
	public void onPayloadStartedEdit() {
		DatesFragment datesFragment = (DatesFragment) getSupportFragmentManager()
				.findFragmentByTag("datesFragment");
		datesFragment.setModifable(false);
	}

	@Override
	public void onPayloadEndedEdit() {
		DatesFragment datesFragment = (DatesFragment) getSupportFragmentManager()
				.findFragmentByTag("datesFragment");
		datesFragment.setModifable(true);
	}
}
