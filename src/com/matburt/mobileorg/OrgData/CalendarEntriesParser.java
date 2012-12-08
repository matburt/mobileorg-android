package com.matburt.mobileorg.OrgData;

import android.database.Cursor;

import com.matburt.mobileorg.Services.CalendarComptabilityWrappers.intEvents;

public class CalendarEntriesParser {

	private int idColumn;
	private int dtStartColumn;
	private int dtEndColumn;
	private int titleColumn;
	private int descriptionColumn;
	private int locationColumn;
	private int allDayColumn;

	public CalendarEntriesParser(intEvents events, Cursor cursor) {		
		dtStartColumn = cursor.getColumnIndexOrThrow(events.DTSTART);
		dtEndColumn = cursor.getColumnIndexOrThrow(events.DTEND);
		titleColumn = cursor.getColumnIndexOrThrow(events.TITLE);
		idColumn = cursor.getColumnIndexOrThrow(events._ID);
		descriptionColumn = cursor.getColumnIndexOrThrow(events.DESCRIPTION);
		locationColumn = cursor.getColumnIndexOrThrow(events.EVENT_LOCATION);
		allDayColumn = cursor.getColumnIndexOrThrow(events.ALL_DAY);
	}

	public CalendarEntry getEntryFromCursor(Cursor cursor) {
		CalendarEntry entry = new CalendarEntry();
		
		entry.dtStart = cursor.getLong(dtStartColumn);
		entry.dtEnd = cursor.getLong(dtEndColumn);
		entry.title = cursor.getString(titleColumn);
		entry.id = cursor.getLong(idColumn);
		entry.description = cursor.getString(descriptionColumn);
		entry.location = cursor.getString(locationColumn);
		entry.allDay = cursor.getInt(allDayColumn);
		
		return entry;
	}
}
