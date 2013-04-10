package com.matburt.mobileorg.Gui.Capture;

import android.content.ContentResolver;
import android.content.Intent;
import android.text.TextUtils;

import com.matburt.mobileorg.OrgData.OrgFile;
import com.matburt.mobileorg.OrgData.OrgNode;
import com.matburt.mobileorg.OrgData.OrgNodePayload;
import com.matburt.mobileorg.OrgData.OrgProviderUtils;

public abstract class EditActivityController {
	public final static String NODE_ID = "node_id";
	public final static String OLP_LOCATION = "olp_location";
	public final static String ACTIONMODE = "actionMode";
	public final static String ACTIONMODE_CREATE = "create";
	public final static String ACTIONMODE_EDIT = "edit";
	public final static String ACTIONMODE_ADDCHILD = "add_child";
	
	protected ContentResolver resolver;
	
	protected OrgNode node;
	private OrgNodePayload editPayload;

	public static EditActivityController getController(Intent intent,
			ContentResolver resolver, String defaultTodo) {
		String actionMode = intent.getStringExtra(ACTIONMODE);
		long node_id = intent.getLongExtra(NODE_ID, -1);
		String olpLocation = intent.getStringExtra(OLP_LOCATION);
		
		if (TextUtils.isEmpty(olpLocation) == false) {
			try {
				OrgNode parentNode = OrgProviderUtils.getOrgNodeFromOlpPath(
						olpLocation, resolver);
				node_id = parentNode.id;
			} catch (Exception e) {}
		}

		EditActivityController controller = EditActivityController
				.getController(actionMode, node_id, olpLocation, intent,
						resolver, defaultTodo);
		controller.resolver = resolver;

		return controller;
	}
	
	public static EditActivityController getController(String editMode,
			long node_id, String olpLocation, Intent intent,
			ContentResolver resolver, String defaultTodo) {
		if (editMode == null) {
			return new EditActivityControllerCreate(intent, defaultTodo);
		} else if (editMode.equals(ACTIONMODE_CREATE)) {
			return new EditActivityControllerCreate(defaultTodo);
		} else if (editMode.equals(ACTIONMODE_EDIT)) {
			return new EditActivityControllerEdit(node_id, resolver);
		} else if (editMode.equals(ACTIONMODE_ADDCHILD)) {
			return new EditActivityControllerAddChild(node_id, resolver, defaultTodo);
		} else {
			throw new IllegalArgumentException("unknown editMode : " + editMode);
		}
	}

	public abstract OrgNode getParentOrgNode();
	public abstract void saveEdits(OrgNode newNode);
	public abstract String getActionMode();
	public abstract boolean hasEdits(OrgNode newNode);
	
	public OrgNode getOrgNode() {
		return this.node;
	}
	
	public OrgNodePayload getOrgNodePayload() {
		if (this.editPayload == null)
			this.editPayload = new OrgNodePayload(node.getPayload());
	
		return this.editPayload;
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
		return getOrgNode().isNodeEditable(resolver);
	}
}
