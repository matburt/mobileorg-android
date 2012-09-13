package com.matburt.mobileorg.test.util;

import android.content.ContentResolver;

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
	
	public static OrgNode setupParentScenario(ContentResolver resolver) {
		OrgFile file = getDefaultOrgFile();
		file.setResolver(resolver);
		file.write();
		
		OrgNode child = getDefaultOrgNode();
		child.parentId = file.nodeId;
		child.fileId = file.id;
		child.write(resolver);
		
		OrgNode child2 = getDefaultOrgNode();
		child2.parentId = file.nodeId;
		child2.fileId = file.id;
		child2.write(resolver);
		
		OrgNode child2Child = getDefaultOrgNode();
		child2Child.parentId = child2.id;
		child2Child.fileId = file.id;
		child2Child.write(resolver);
		
		return child2Child;
	}
	
	public static void cleanupParentScenario(ContentResolver resolver) {
		String filename = getDefaultOrgFile().filename;
		OrgFile file = new OrgFile(filename, resolver);
		file.setResolver(resolver);
		file.removeFile();
	}
	
	public static OrgFile getDefaultOrgFile() {
		OrgFile file = new OrgFile("test file", "name", "checksum");
		return file;
	}
}
