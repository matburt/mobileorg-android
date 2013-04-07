package com.matburt.mobileorg.Gui.Capture;

import android.content.ContentResolver;
import android.util.Log;

import com.matburt.mobileorg.OrgData.OrgEdit;
import com.matburt.mobileorg.OrgData.OrgNode;
import com.matburt.mobileorg.OrgData.OrgProviderUtils;
import com.matburt.mobileorg.util.OrgNodeNotFoundException;


public class EditActivityControllerAddChild extends EditActivityController {
	
	private String nodeOlpPath;

	public EditActivityControllerAddChild(long node_id, ContentResolver resolver) {
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

	@Override
	public OrgNode getParentOrgNode() {
		try {
			OrgNode parent = new OrgNode(this.node.parentId, resolver);
			return parent;
		} catch (OrgNodeNotFoundException e) {}
		
		return new OrgNode();
	}

	@Override
	public void saveEdits(OrgNode newNode) {
		try {
			OrgEdit edit = newNode.createParentNewheading(resolver);
			edit.write(resolver);
		} catch (IllegalStateException e) {
			Log.e("MobileOrg", e.getLocalizedMessage());
		}
		newNode.write(resolver);		
	}

	@Override
	public String getActionMode() {
		return ACTIONMODE_ADDCHILD;
	}
}
