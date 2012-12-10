package com.matburt.mobileorg.Gui.Capture;

import com.matburt.mobileorg.OrgData.OrgNode;

public interface EditHost {
	public boolean isNodeEditable();
	public OrgNode getOrgNode();
	public String getActionMode();
	
	public boolean isNodeRefilable();
	public OrgNode getParentOrgNode();
}
