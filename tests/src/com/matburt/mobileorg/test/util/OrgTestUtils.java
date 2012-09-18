package com.matburt.mobileorg.test.util;

import android.content.ContentResolver;
import android.util.Log;

import com.matburt.mobileorg.OrgData.OrgFile;
import com.matburt.mobileorg.OrgData.OrgNode;
import com.matburt.mobileorg.OrgData.OrgProviderUtil;
import com.matburt.mobileorg.util.OrgFileNotFoundException;

public class OrgTestUtils {

	public static final String defaultTestfilename = "test file.org";
	public static final String defaultTestfileAlias = "delete me";

	
	public static final String testId = "E7C7B57B-F557-47AD-A80D-6B6F466F4A7C";
	public static final String testIdPayload = 
			":PROPERTIES:\n" + 
			":ID:       " + testId + "\n" + 
			":END:";
	public static final String testIdAgendasPayload = "\n\n" +
			"   :PROPERTIES:\n" + 
			"   :ORIGINAL_ID: " + testId + "\n" +
			"   :END:";
	
	
	public static final String testTimestampNotInPayload = "2222-12-30";
	public static final String testTimestampScheduled = "2012-05-18 Tue";
	public static final String testTimestampDeadline = "2032-09-18 Thu";
	public static final String testTimestampTimestamp = "3012-02-24 Fri";
	public static final String testTimestampPayload = 
			"SCHEDULED: <" + testTimestampScheduled + "> DEADLINE: <" + testTimestampDeadline + ">\n" +
			"<" + testTimestampTimestamp + ">  \n" +
			testIdPayload;
	public static final String testTimestampPayloadSimple = "<" + testTimestampTimestamp + ">";
	
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
		OrgFile file = OrgProviderUtil.getOrCreateFile(defaultTestfilename,
				defaultTestfileAlias, resolver);
		Log.d("MobileOrg", "*** Created file " + file.nodeId);
		
		OrgNode child = new OrgNode();
		child.name = "child";
		child.parentId = file.nodeId;
		child.fileId = file.id;
		child.write(resolver);
		Log.d("MobileOrg", "*** Created child " + child.id);
		
		OrgNode child2 = new OrgNode();
		child2.name = "child2";
		child2.parentId = file.nodeId;
		child2.fileId = file.id;
		child2.write(resolver);
		Log.d("MobileOrg", "*** Created child2 " + child2.id);

		
		OrgNode child2Child = new OrgNode();
		child2Child.name = "child2Child";
		child2Child.parentId = child2.id;
		child2Child.fileId = file.id;
		child2Child.write(resolver);
		Log.d("MobileOrg", "*** Created child2Child " + child2Child.id);
		
		return child2Child;
	}
	
	public static void cleanupParentScenario(ContentResolver resolver) {
		try {
			OrgFile file = new OrgFile(defaultTestfilename, resolver);
			file.setResolver(resolver);
			file.removeFile();
		} catch (OrgFileNotFoundException e) {}
	}
	
	public static OrgFile getDefaultOrgFile() {
		OrgFile file = new OrgFile(defaultTestfilename, defaultTestfileAlias, "checksum");
		return file;
	}
}
