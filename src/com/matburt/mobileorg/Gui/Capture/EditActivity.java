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
import com.matburt.mobileorg.R;
import com.matburt.mobileorg.OrgData.OrgEdit;
import com.matburt.mobileorg.OrgData.OrgFile;
import com.matburt.mobileorg.OrgData.OrgNode;
import com.matburt.mobileorg.OrgData.OrgProviderUtils;
import com.matburt.mobileorg.Services.SyncService;
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
		OrgUtils.setTheme(this);
		super.onCreate(savedInstanceState);
		setContentView(R.layout.edit);
		getSupportActionBar().setTitle(R.string.menu_capture);
		
		this.resolver = getContentResolver();
		
		SyncService.stopAlarm(this); // Don't run background sync while editing node
		
		initState();
		invalidateOptionsMenu();
	}
	
	@Override
	protected void onDestroy() {
		super.onDestroy();
		SyncService.startAlarm(this);
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

			if(node_id >= 0) {
				try {
					OrgNode parent = new OrgNode(node_id, resolver);
					this.node.parentId = parent.findOriginalNode(resolver).id;
				} catch (OrgNodeNotFoundException e) {
					this.node.parentId = node_id;					
				}
			}
			else
				this.node.parentId = OrgProviderUtils
						.getOrCreateCaptureFile(resolver).nodeId;
			
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
	
	public String getActionMode() {
		return this.actionMode;
	}
	
	public boolean isNodeEditable() {	
		return getOrgNode().isNodeEditable(resolver);
	}
	
	public boolean isPayloadEditable() {
		OrgNode node = getOrgNode();
		
		if(node.level == 0 && !node.name.equals(OrgFile.AGENDA_FILE_ALIAS))
			return true;
		else
			return isNodeEditable();
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

		if(isNodeEditable() == false)
			menu.findItem(R.id.nodeedit_save).setVisible(false);
	    
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
		}
		return false;
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
			OrgNode clonedNode;
			if (node.id == -1)
				clonedNode = new OrgNode();
			else
				clonedNode = new OrgNode(this.node.id, resolver);
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
		
		OrgUtils.announceSyncDone(this);
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
