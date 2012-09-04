package com.matburt.mobileorg.test.util;

import com.matburt.mobileorg.provider.OrgFile;
import com.matburt.mobileorg.provider.OrgNode;

public class OrgTestUtils {

	public static OrgNode getDefaultOrgNode() {
		OrgNode node = new OrgNode();
		node.name = "title";
		node.todo = "TODO";
		return node;
	}

	public static OrgFile getDefaultOrgFile() {
		OrgFile file = new OrgFile("filename", "name", "checksum");
		return file;
	}
}
