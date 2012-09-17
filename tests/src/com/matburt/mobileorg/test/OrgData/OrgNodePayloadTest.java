package com.matburt.mobileorg.test.OrgData;

import com.matburt.mobileorg.OrgData.OrgNode;
import com.matburt.mobileorg.OrgData.OrgNodePayload;
import com.matburt.mobileorg.test.util.OrgTestUtils;

import android.test.AndroidTestCase;

public class OrgNodePayloadTest extends AndroidTestCase {

	public void testGetIdFromId() {
		OrgNodePayload payload = new OrgNodePayload(OrgTestUtils.testIdPayload);
		assertEquals(OrgTestUtils.testId, payload.getId());
	}
	
	public void testGetIdFromOrigId() {
		OrgNodePayload payload = new OrgNodePayload(OrgTestUtils.testIdAgendasPayload);
		assertEquals(OrgTestUtils.testId, payload.getId());
	}
	
	public void testOrgNodeGetId() {
		OrgNode node = new OrgNode();
		node.setPayload(OrgTestUtils.testIdAgendasPayload);
		assertEquals(OrgTestUtils.testId, node.getNodeId(getContext().getContentResolver()));
	}
}
