package com.matburt.mobileorg.test.Synchronizers;

import java.util.ArrayList;

import com.matburt.mobileorg.OrgData.OrgNode;
import com.matburt.mobileorg.OrgData.OrgNodePayload.DateEntry;

import android.test.AndroidTestCase;

public class CalendarSyncServiceTest extends AndroidTestCase {

	
	public void testOrgNodePayloadGetDates() {
		OrgNode node = new OrgNode();
		node.setPayload("<2012-09-13 Thu>");
		ArrayList<DateEntry> dates = node.getOrgNodePayload().getDates();
		
		assertEquals(1, dates.size());
	}
}
