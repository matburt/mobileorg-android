package com.matburt.mobileorg.Gui.Capture;

import android.content.Intent;

import com.matburt.mobileorg.OrgData.OrgNode;
import com.matburt.mobileorg.util.OrgUtils;


public class EditActivityControllerCreate extends EditActivityController {
	
	public EditActivityControllerCreate() {
		this.node = new OrgNode();
	}
	
	public EditActivityControllerCreate(Intent intent) {
		this.node = OrgUtils.getCaptureIntentContents(intent);
	}
	
	@Override
	public boolean isNodeEditable() {
		return false;
	}
	
	public OrgNode getParentOrgNode() {
		return null;
	}

	@Override
	public void saveEdits(OrgNode newNode) {
		newNode.level = 1;
		newNode.write(resolver);		
	}

	@Override
	public String getActionMode() {
		return ACTIONMODE_CREATE;
	}
}
