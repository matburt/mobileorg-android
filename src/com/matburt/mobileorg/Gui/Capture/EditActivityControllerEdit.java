package com.matburt.mobileorg.Gui.Capture;

import android.content.ContentResolver;

import com.matburt.mobileorg.OrgData.OrgNode;
import com.matburt.mobileorg.util.OrgNodeNotFoundException;

public class EditActivityControllerEdit extends EditActivityController {
	
	private String nodeOlpPath;

	public EditActivityControllerEdit(long node_id, ContentResolver resolver) {
		try {
			this.node = new OrgNode(node_id, resolver).findOriginalNode(resolver);
			this.nodeOlpPath = node.getOlpId(resolver);
		} catch (OrgNodeNotFoundException e) {}
	}
	
	public OrgNode getParentOrgNode() {
		OrgNode parent;
		try {
			parent = node.getParent(resolver);
		} catch (OrgNodeNotFoundException e) {
			parent = new OrgNode();
			parent.parentId = -2;
		}
		return parent;	
	}

	@Override
	public void saveEdits(OrgNode newNode) {
		this.node.generateApplyWriteEdits(newNode, this.nodeOlpPath, resolver);
		this.node.updateAllNodes(resolver);		
	}

	@Override
	public String getActionMode() {
		return ACTIONMODE_EDIT;
	}
}
