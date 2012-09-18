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
	
	public void testGetScheduled() {
		OrgNodePayload payload = new OrgNodePayload(OrgTestUtils.testTimestampPayload);
		assertEquals(OrgTestUtils.testTimestampScheduled, payload.getScheduled());
	}
	
	public void testGetDeadline() {
		OrgNodePayload payload = new OrgNodePayload(OrgTestUtils.testTimestampPayload);
		assertEquals(OrgTestUtils.testTimestampDeadline, payload.getDeadline());
	}
	
	public void testGetTimestamp() {
		OrgNodePayload payload = new OrgNodePayload(OrgTestUtils.testTimestampPayload);
		assertEquals(OrgTestUtils.testTimestampTimestamp, payload.getTimestamp());
	}
	
	public void testModifyScheduled() {
		OrgNodePayload payload = new OrgNodePayload(OrgTestUtils.testTimestampPayload);
		final String newTimestamp = OrgTestUtils.testTimestampNotInPayload;
		payload.modifyDates(newTimestamp, null, null);
		assertEquals(newTimestamp, payload.getScheduled());
	}
	
	public void testModifyDeadline() {
		OrgNodePayload payload = new OrgNodePayload(OrgTestUtils.testTimestampPayload);
		final String newTimestamp = OrgTestUtils.testTimestampNotInPayload;
		payload.modifyDates(null, newTimestamp, null);
		assertEquals(newTimestamp, payload.getDeadline());
	}

	public void testModifyTimestamp() {
		OrgNodePayload payload = new OrgNodePayload(OrgTestUtils.testTimestampPayload);
		final String newTimestamp = OrgTestUtils.testTimestampNotInPayload;
		payload.modifyDates(null, null, newTimestamp);
		assertEquals(newTimestamp, payload.getDeadline());
	}
}
