package com.matburt.mobileorg.Gui.Capture;

import android.content.Intent;
import android.util.Log;

import com.matburt.mobileorg.OrgData.OrgNode;
import com.matburt.mobileorg.util.OrgUtils;


public class EditActivityControllerCreate extends EditActivityController {
	
	public EditActivityControllerCreate(String defaultTodo) {
		this.node = new OrgNode();
		this.node.todo = defaultTodo;
		Log.d("MobileOrg", "Created node with todo " + this.node.todo);
	}
	
	public EditActivityControllerCreate(Intent intent, String defaultTodo) {
		this.node = OrgUtils.getCaptureIntentContents(intent);
		this.node.todo = defaultTodo;
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
