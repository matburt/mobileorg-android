package com.matburt.mobileorg.Services;

import android.annotation.SuppressLint;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.CalendarContract.CalendarAlerts;
import android.provider.CalendarContract.Calendars;
import android.provider.CalendarContract.Events;
import android.provider.CalendarContract.Reminders;

public class CalendarComptabilityWrappers {
	
	public intEvents events = new intEvents();
	public intCalendars calendars = new intCalendars();
	public intReminders reminders = new intReminders();
	public intCalendarAlerts calendarAlerts = new intCalendarAlerts();
	private Context context;

	public String[] eventsProjection;
	
	public CalendarComptabilityWrappers(Context context) {
		this.context = context;
		initCalendar();
		
		this.eventsProjection = new String[] { events.CALENDAR_ID,
				events.DTSTART, events.DTEND, events.DESCRIPTION, events.TITLE,
				events.EVENT_LOCATION, events._ID, events.ALL_DAY };
	}
	
	public class intEvents {
		public Uri CONTENT_URI;
		public String CALENDAR_ID = "calendar_id";
		public String TITLE = "title";
		public String DESCRIPTION = "description";
		public String EVENT_LOCATION = "eventLocation";
		public String ALL_DAY = "allDay";
		public String DTSTART = "dtstart";
		public String DTEND = "dtend";
		public String HAS_ALARM = "hasAlarm";	
		public String ORGANIZER = "organizer";
		public String EVENT_TIMEZONE = "eventTimezone";
		public String _ID = "_id";
		public String AVAILABILITY = "availability";
		public Integer AVAILABILITY_BUSY = 0;
		public Integer AVAILABILITY_FREE = 1;
		public Integer AVAILABILITY_TENTATIVE = 2;
	};
	
	public class intCalendars {
		public Uri CONTENT_URI;
		public String _ID = "_id";
		public String CALENDAR_DISPLAY_NAME = "displayName";
		public String ACCOUNT_NAME = "accountName";
		public String VISIBLE = "selected";
	};
	
	public class intReminders {
		public Uri CONTENT_URI;
		public String MINUTES = "minutes";
		public String EVENT_ID = "event_id";
		public String METHOD = "method";
		public int METHOD_ALERT = 1;
	};
	
	public class intCalendarAlerts {
		public Uri CONTENT_URI;
		public String EVENT_ID = "event_id";
		public String BEGIN = "begin";
		public String END = "end";
		public String ALERT_TIME = "alarmTime";
		public String MINUTES = "minutes";
		public String STATE = "state";
		public int STATE_SCHEDULED = 0;
	};
	
	/**
	 * Hack to support phones with Android <3.0
	 */
	@SuppressLint("NewApi")
	private void initCalendar() {
		try {
			calendars.CONTENT_URI = Calendars.CONTENT_URI;
			calendars._ID = Calendars._ID;
			calendars.ACCOUNT_NAME = Calendars.ACCOUNT_NAME;
			calendars.CALENDAR_DISPLAY_NAME = Calendars.CALENDAR_DISPLAY_NAME;
			calendars.VISIBLE = Calendars.VISIBLE;

			events.CONTENT_URI = Events.CONTENT_URI;
			events.CALENDAR_ID = Events.CALENDAR_ID;
			events.TITLE = Events.TITLE;
			events.DESCRIPTION = Events.DESCRIPTION;
			events.EVENT_LOCATION = Events.EVENT_LOCATION;
			events.ORGANIZER = Events.ORGANIZER;
			events.ALL_DAY = Events.ALL_DAY;
			events.DTEND = Events.DTEND;
			events.DTSTART = Events.DTSTART;
			events.HAS_ALARM = Events.HAS_ALARM;
			events.EVENT_TIMEZONE = Events.EVENT_TIMEZONE;
			events._ID = Events._ID;

			reminders.CONTENT_URI = Reminders.CONTENT_URI;
			reminders.MINUTES = Reminders.MINUTES;
			reminders.EVENT_ID = Reminders.EVENT_ID;
			reminders.METHOD_ALERT = Reminders.METHOD_ALERT;
			reminders.METHOD = Reminders.METHOD;

			calendarAlerts.CONTENT_URI = CalendarAlerts.CONTENT_URI;
			calendarAlerts.STATE_SCHEDULED = CalendarAlerts.STATE_SCHEDULED;
			calendarAlerts.EVENT_ID = CalendarAlerts.EVENT_ID;
			calendarAlerts.BEGIN = CalendarAlerts.BEGIN;
			calendarAlerts.END = CalendarAlerts.END;
			calendarAlerts.ALERT_TIME = CalendarAlerts.ALARM_TIME;
			calendarAlerts.STATE = CalendarAlerts.STATE;
			calendarAlerts.MINUTES = CalendarAlerts.MINUTES;
		} catch (NoClassDefFoundError e) {
			// The classes referenced above are not available on pre 4.0 phones.
			setupBaseUris();
		}
	}
	
	private void setupBaseUris() {
		String baseUri = getCalendarUriBase();
		
		calendars.CONTENT_URI = Uri.parse(baseUri + "/calendars");
		events.CONTENT_URI = Uri.parse(baseUri + "/events");
		reminders.CONTENT_URI = Uri.parse(baseUri + "/reminders");
		calendarAlerts.CONTENT_URI = Uri.parse(baseUri + "/calendar_alerts");
	}
	
	/**
	 * Hack to get the proper calendar uri for pre 4.0 phones. 
	 */
	public String getCalendarUriBase() {
		String calendarUriBase = null;
		Uri calendars = Uri.parse("content://com.android.calendar/calendars");
		Cursor managedCursor = null;
		try {
			managedCursor = context.getContentResolver().query(calendars, null, null, null, null);
		} catch (Exception e) {
		}
		if (managedCursor != null) {
			calendarUriBase = "content://com.android.calendar";
			managedCursor.close();
		} else {
			calendars = Uri.parse("content://calendar/calendars");
			try {
				managedCursor = context.getContentResolver().query(calendars, null, null, null, null);
			} catch (Exception e) {
			}
			if (managedCursor != null) {
				calendarUriBase = "content://calendar";
				managedCursor.close();
			}
		}
		
		return calendarUriBase;
	}
}
