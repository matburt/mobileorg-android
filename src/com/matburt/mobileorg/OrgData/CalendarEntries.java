package com.matburt.mobileorg.OrgData;

import android.database.Cursor;

import com.matburt.mobileorg.Services.CalendarComptabilityWrappers.intEvents;

public class CalendarEntries {

	private int idColumn;
	private int dtStartColumn;
	private int dtEndColumn;
	private int titleColumn;

	public CalendarEntries(intEvents events, Cursor cursor) {		
		dtStartColumn = cursor.getColumnIndexOrThrow(events.DTSTART);
		dtEndColumn = cursor.getColumnIndexOrThrow(events.DTEND);
		titleColumn = cursor.getColumnIndexOrThrow(events.TITLE);
		idColumn = cursor.getColumnIndexOrThrow(events.CALENDAR_ID);
	}

	
	public CalendarEntry getEntryFromCursor(Cursor cursor) {
		CalendarEntry entry = new CalendarEntry();
		
		entry.dtStart = cursor.getLong(dtStartColumn);
		entry.dtEnd = cursor.getLong(dtEndColumn);
		entry.title = cursor.getString(titleColumn);
		entry.id = cursor.getLong(idColumn);
		
		return entry;
	}
	
	public class CalendarEntry {
		public String title = "";
		public long id = -1;
		public long dtStart = 0;
		public long dtEnd = 0;
		
		public boolean isEquals(OrgNodeDate entry) {
			return this.dtStart == entry.beginTime
					&& this.dtEnd == entry.endTime 
					&& entry.getTitle().startsWith(this.title);
		}
	}
}
