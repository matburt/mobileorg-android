package com.matburt.mobileorg.test.OrgData;

import com.matburt.mobileorg.OrgData.OrgNode;
import com.matburt.mobileorg.OrgData.OrgNodePayload;
import com.matburt.mobileorg.OrgData.OrgNodeTimeDate;
import com.matburt.mobileorg.test.util.OrgTestUtils;
import com.matburt.mobileorg.test.util.OrgTestUtils.TestTimestampPayload;

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
	
	
	public void testScheduledGet() {
		OrgNodePayload payload = new OrgNodePayload(TestTimestampPayload.payload);
		assertEquals(TestTimestampPayload.scheduled, payload.getScheduled());
	}
	
	public void testDeadlineGet() {
		OrgNodePayload payload = new OrgNodePayload(TestTimestampPayload.payload);
		assertEquals(TestTimestampPayload.deadline, payload.getDeadline());
	}
	
	public void testTimestampGetSimple() {
		OrgNodePayload payload = new OrgNodePayload(TestTimestampPayload.payloadSimple);
		assertEquals(TestTimestampPayload.timestamp, payload.getTimestamp());
	}
	
	public void testTimestampGet() {
		OrgNodePayload payload = new OrgNodePayload(TestTimestampPayload.payload);
		assertEquals(TestTimestampPayload.timestamp, payload.getTimestamp());
	}
	
	
	public void testScheduledModify() {
		OrgNodePayload payload = new OrgNodePayload(TestTimestampPayload.payload);
		final String newTimestamp = TestTimestampPayload.timestampNotInPayload;
		payload.insertOrReplaceDate(OrgNodeTimeDate.TYPE.Scheduled, newTimestamp);
		assertEquals(newTimestamp, payload.getScheduled());
	}
	
	public void testDeadlineModify() {
		OrgNodePayload payload = new OrgNodePayload(TestTimestampPayload.payload);
		final String newTimestamp = TestTimestampPayload.timestampNotInPayload;
		payload.insertOrReplaceDate(OrgNodeTimeDate.TYPE.Deadline, newTimestamp);
		assertEquals(newTimestamp, payload.getDeadline());
	}

	public void testTimestampModify() {
		OrgNodePayload payload = new OrgNodePayload(TestTimestampPayload.payload);
		final String newTimestamp = TestTimestampPayload.timestampNotInPayload;
		payload.insertOrReplaceDate(OrgNodeTimeDate.TYPE.Timestamp, newTimestamp);
		assertEquals(newTimestamp, payload.getTimestamp());
	}
	
	
	public void testScheduledInsertNew() {
		OrgNodePayload payload = new OrgNodePayload(OrgTestUtils.testIdAgendasPayload);
		final String newTimestamp = TestTimestampPayload.timestampNotInPayload;
		payload.insertOrReplaceDate(OrgNodeTimeDate.TYPE.Scheduled, newTimestamp);
		assertEquals(newTimestamp, payload.getScheduled());
	}
	
	public void testDeadlineInsertNew() {
		OrgNodePayload payload = new OrgNodePayload(OrgTestUtils.testIdAgendasPayload);
		final String newTimestamp = TestTimestampPayload.timestampNotInPayload;
		payload.insertOrReplaceDate(OrgNodeTimeDate.TYPE.Deadline, newTimestamp);
		assertEquals(newTimestamp, payload.getDeadline());
	}

	public void testTimestampInsertNew() {
		OrgNodePayload payload = new OrgNodePayload(OrgTestUtils.testIdAgendasPayload);
		final String newTimestamp = TestTimestampPayload.timestampNotInPayload;
		payload.insertOrReplaceDate(OrgNodeTimeDate.TYPE.Timestamp, newTimestamp);
		assertEquals(newTimestamp, payload.getTimestamp());
	}
}
