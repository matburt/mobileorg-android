package com.matburt.mobileorg.test.util;

import com.matburt.mobileorg.OrgData.OrgFile;
import com.matburt.mobileorg.OrgData.OrgNode;

public class OrgTestUtils {

	public static OrgNode getDefaultOrgNode() {
		OrgNode node = new OrgNode();
		node.name = "title";
		node.todo = "TODO";
		return node;
	}

	public static OrgNode getComplexOrgNode() {
		OrgNode node = new OrgNode();
		node.name = "My complicated name";
		node.todo = "TODO";
		node.priority = "C";
		node.tags = "tag1:tag2::tag3";
		node.setPayload("my complex payload");
		return node;
	}
	
	public static OrgFile getDefaultOrgFile() {
		OrgFile file = new OrgFile("filename", "name", "checksum");
		return file;
	}
}
