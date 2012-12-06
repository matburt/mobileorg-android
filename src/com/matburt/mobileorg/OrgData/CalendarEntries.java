package com.matburt.mobileorg.OrgData;

import android.database.Cursor;
import android.text.TextUtils;

import com.matburt.mobileorg.Services.CalendarComptabilityWrappers.intEvents;

public class CalendarEntries {

	private int idColumn;
	private int dtStartColumn;
	private int dtEndColumn;
	private int titleColumn;
	private int descriptionColumn;
	private int locationColumn;

	public CalendarEntries(intEvents events, Cursor cursor) {		
		dtStartColumn = cursor.getColumnIndexOrThrow(events.DTSTART);
		dtEndColumn = cursor.getColumnIndexOrThrow(events.DTEND);
		titleColumn = cursor.getColumnIndexOrThrow(events.TITLE);
		idColumn = cursor.getColumnIndexOrThrow(events.CALENDAR_ID);
		descriptionColumn = cursor.getColumnIndexOrThrow(events.DESCRIPTION);
		locationColumn = cursor.getColumnIndexOrThrow(events.EVENT_LOCATION);
	}

	public CalendarEntry getEntryFromCursor(Cursor cursor) {
		CalendarEntry entry = new CalendarEntry();
		
		entry.dtStart = cursor.getLong(dtStartColumn);
		entry.dtEnd = cursor.getLong(dtEndColumn);
		entry.title = cursor.getString(titleColumn);
		entry.id = cursor.getLong(idColumn);
		entry.description = cursor.getString(descriptionColumn);
		entry.location = cursor.getString(locationColumn);
		
		return entry;
	}
	

	public class CalendarEntry {
		public String title = "";
		public String description = "";
		public String location = "";
		public long id = -1;
		public long dtStart = 0;
		public long dtEnd = 0;
		
		public boolean isEquals(OrgNodeDate entry) {
			return this.dtStart == entry.beginTime
					&& this.dtEnd == entry.endTime 
					&& entry.getTitle().startsWith(this.title);
		}
		
		public OrgNode getOrgNode() {
			OrgNode node = new OrgNode();
			node.name = this.title;
			
			String date = OrgNodeDate.getDate(this.dtStart, this.dtEnd);
			String formatedDate = OrgNodeTimeDate.formatDate(OrgNodeTimeDate.TYPE.Timestamp, date);
			
			String payload = formatedDate + "\n" + this.description;
			
			if(TextUtils.isEmpty(location) == false)
				payload += "\n:LOCATION: " + location;
			
			node.setPayload(payload);
			return node;
		}
	}
}
