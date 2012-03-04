package com.matburt.mobileorg.Services;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.SQLException;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.provider.CalendarContract.CalendarAlerts;
import android.provider.CalendarContract.Calendars;
import android.provider.CalendarContract.Events;
import android.provider.CalendarContract.Reminders;
import android.text.format.DateUtils;
import android.util.Log;

import com.matburt.mobileorg.R;
import com.matburt.mobileorg.Parsing.NodeWrapper;
import com.matburt.mobileorg.Parsing.OrgDatabase;

public class CalendarSyncService {
	private final static String CALENDAR_ORGANIZER = "MobileOrg";

	private intCalendars intCalendars = new intCalendars();
	private intEvents intEvents = new intEvents();
	private intReminders intReminders = new intReminders();
	private intCalendarAlerts intCalendarAlerts = new intCalendarAlerts();

	private OrgDatabase db;
	private Context context;
	private SharedPreferences sharedPreferences;
	
	public CalendarSyncService(OrgDatabase db, Context context) {
		this.db = db;
		this.context = context;
		this.sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
		setupCalendar();
	}
	
	private int getCalendarID(String calendarName) {
		Cursor cursor = context.getContentResolver().query(
				intCalendars.CONTENT_URI,
				new String[] { intCalendars._ID,
						intCalendars.CALENDAR_DISPLAY_NAME }, null,
				null, null);
		if (cursor != null && cursor.moveToFirst()) {
			for (int i = 0; i < cursor.getCount(); i++) {
				int calId = cursor.getInt(0);
				String calName = cursor.getString(1);

				if (calName.equals(calendarName)) {
				    cursor.close();
					return calId;
				}
				cursor.moveToNext();
			}
		}
		cursor.close();
		return -1;
	}

	// TODO Speed up using bulkInserts
	private String insertEntry(String name, boolean isTodoActive, String payload, String orgID, long beginTime,
			long endTime, int allDay, String filename) throws IllegalArgumentException {		
		
		if (sharedPreferences.getBoolean("calendarShowDone", true) == false
				&& isTodoActive == false)
			return null;
		
		final String calendarName = PreferenceManager
				.getDefaultSharedPreferences(context).getString("calendarName",
						"");
		int calId = getCalendarID(calendarName);
		
		if(calId == -1)
			throw new IllegalArgumentException("Couldn't find selected calendar: " + calendarName);

		final String embeddedNodeMetadata = CALENDAR_ORGANIZER + ":" + filename;
		
		ContentValues values = new ContentValues();
		values.put(intEvents.CALENDAR_ID, calId);
		values.put(intEvents.TITLE, name);
		values.put(intEvents.DESCRIPTION, embeddedNodeMetadata + "\n" + payload);
		values.put(intEvents.EVENT_LOCATION, "");
		
		// Sync with google will delete organizer :(
		//values.put(intEvents.ORGANIZER, embeddedNodeMetadata);
		
		values.put(intEvents.DTSTART, beginTime);
		values.put(intEvents.DTEND, endTime);
		values.put(intEvents.ALL_DAY, allDay);
		values.put(intEvents.HAS_ALARM, 0);
		
		Uri uri = context.getContentResolver().insert(
				intEvents.CONTENT_URI, values);
		String nodeID = uri.getLastPathSegment();
		
		if (allDay == 0 && isTodoActive == true && sharedPreferences.getBoolean(
				"calendarReminder", false))
			addReminder(nodeID, beginTime, endTime);
		
		return nodeID;
	}
	
	private void addReminder(String eventID, long beginTime, long endTime) {
		if(beginTime < System.currentTimeMillis())
			return;
		
		String intervalString = sharedPreferences.getString("calendarReminderInterval", null);
		if(intervalString == null)
			throw new IllegalArgumentException("Invalid calendar reminder interval");
		long reminderTime = Integer.valueOf(intervalString);
		
		ContentValues values = new ContentValues();
		values.put(intReminders.MINUTES, reminderTime);
		values.put(intReminders.EVENT_ID, eventID);
		values.put(intReminders.METHOD, intReminders.METHOD_ALERT);
		context.getContentResolver().insert(
				intReminders.CONTENT_URI, values);
		
        ContentValues alertvalues = new ContentValues(); 
        alertvalues.put(intCalendarAlerts.EVENT_ID, eventID ); 
        alertvalues.put(intCalendarAlerts.BEGIN, beginTime ); 
        alertvalues.put(intCalendarAlerts.END, endTime ); 
        alertvalues.put(intCalendarAlerts.ALERT_TIME, reminderTime ); 
        alertvalues.put(intCalendarAlerts.STATE, intCalendarAlerts.STATE_SCHEDULED); 
        alertvalues.put(intCalendarAlerts.MINUTES, reminderTime ); 
		context.getContentResolver().insert(
				intCalendarAlerts.CONTENT_URI,
				alertvalues);
        
		ContentValues eventValues = new ContentValues();
		eventValues.put(intEvents.HAS_ALARM, 1);
		context.getContentResolver().update(
				ContentUris.withAppendedId(intEvents.CONTENT_URI,
						Long.valueOf(eventID)), eventValues, null, null);
		
	}
	
	public int deleteEntry(String calendarID) {
		return context.getContentResolver().delete(
				ContentUris.withAppendedId(intEvents.CONTENT_URI,
						Long.valueOf(calendarID)), null, null);
	}
	
	public int deleteFileEntries(String filename, Context context) {
		return context.getContentResolver().delete(intEvents.CONTENT_URI,
				"description LIKE ?",
				new String[] { CALENDAR_ORGANIZER + ":" + filename + "%" });
	}
	
	public int deleteAllEntries(Context context) {
		return context.getContentResolver()
				.delete(intEvents.CONTENT_URI, "description LIKE ?",
						new String[] { CALENDAR_ORGANIZER + "%" });
	}
	
	private Date getDateInMs(String date) throws ParseException {
		SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
		return formatter.parse(date);
	}
	
	private Date getTimeInMs(String time) throws ParseException {
		SimpleDateFormat formatter = new SimpleDateFormat("HH:mm");
		return formatter.parse(time);
	}
	
	private void insertNode(NodeWrapper node, String filename) throws IllegalArgumentException {
		final Pattern schedulePattern = Pattern
				.compile("(\\d{4}-\\d{2}-\\d{2})(?:[^\\d]*)(\\d{1,2}\\:\\d{2})?\\-?(\\d{1,2}\\:\\d{2})?");
		Matcher propm = schedulePattern.matcher(node.getDate(db));

		long beginTime;
		long endTime;
		int allDay;

		if (propm.find()) {
			try {
				beginTime = getDateInMs(propm.group(1)).getTime();
				beginTime += TimeZone.getDefault().getOffset(beginTime);
				
				long beginTimeOfDay;
				if (propm.group(2) != null) { // has hh:mm entry
					beginTimeOfDay = getTimeInMs(propm.group(2)).getTime();
					beginTime += beginTimeOfDay;
					allDay = 0;
					
					if (propm.group(3) != null) { // has hh:mm-hh:mm entry
						endTime = beginTime + getTimeInMs(propm.group(3)).getTime() - beginTimeOfDay;
					}
					else // event is one hour per default
						endTime = beginTime + DateUtils.HOUR_IN_MILLIS;
				} else { // event is an entire day event
					endTime = beginTime + DateUtils.DAY_IN_MILLIS;
					allDay = 1;
				}
				
				boolean isActive = db.isTodoActive(node.getTodo());
				
				insertEntry(node.getName(), isActive, node.getCleanedPayload(db),
						node.getNodeId(db), beginTime, endTime, allDay,
						filename);

			} catch (ParseException e) {
				Log.w("MobileOrg", "Unable to parse schedule of: " + node.getName() + " " + node.getDate(db));
			}
		} else
			Log.w("MobileOrg", "Unable to find time entry in schedule of: "
					+ node.getName());
	}

	public void insertFileEntries(String filename) throws IllegalArgumentException {
		Cursor scheduled = db.getFileSchedule(filename);

		if (scheduled == null)
			return;
		while (scheduled.isAfterLast() == false) {
			NodeWrapper node = new NodeWrapper(scheduled);

			insertNode(node, filename);
			scheduled.moveToNext();
		}

		scheduled.close();
	}

	public CharSequence[] getCalendars(Context context) {
		CharSequence[] result;

		try {
			Cursor cursor = context.getContentResolver().query(
					intCalendars.CONTENT_URI,
					new String[] { intCalendars._ID,
							intCalendars.CALENDAR_DISPLAY_NAME }, null, null,
					null);

			if (cursor.getCount() == 0) {
				result = new CharSequence[1];
				result[0] = context
						.getString(R.string.error_setting_no_calendar);
				return result;
			}

			result = new CharSequence[cursor.getCount()];

			if (cursor != null && cursor.moveToFirst()) {
				for (int i = 0; i < cursor.getCount(); i++) {
					result[i] = cursor.getString(1);
					cursor.moveToNext();
				}
			}
			cursor.close();
		} catch (SQLException e) {
			result = new CharSequence[1];
			result[0] = context.getString(R.string.error_setting_no_calendar);
		}

		return result;
	}
	
	public void syncFile(String filename) throws IllegalArgumentException {
		deleteFileEntries(filename, context);
		insertFileEntries(filename);
	}
	
	
	private class intEvents {
		public Uri CONTENT_URI;
		public String CALENDAR_ID = "calendar_id";
		public String TITLE = "title";
		public String DESCRIPTION = "description";
		public String EVENT_LOCATION = "eventLocation";
		public String ALL_DAY = "allDay";
		public String DTSTART = "dtstart";
		public String DTEND = "dtend";
		public String HAS_ALARM = "hasAlarm";	
		@SuppressWarnings("unused")
		public String ORGANIZER = "organizer";
	};
	
	private class intCalendars {
		public Uri CONTENT_URI;
		public String _ID = "_id";
		public String CALENDAR_DISPLAY_NAME = "displayName";
		@SuppressWarnings("unused")
		public String ACCOUNT_NAME = "accountName";
		public String VISIBLE = "selected";
	};
	
	private class intReminders {
		public Uri CONTENT_URI;
		public String MINUTES = "minutes";
		public String EVENT_ID = "event_id";
		public String METHOD = "method";
		public int METHOD_ALERT = 1;
	};
	
	private class intCalendarAlerts {
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
	private void setupCalendar() {
		try {
			intCalendars.CONTENT_URI = Calendars.CONTENT_URI;
			intCalendars._ID = Calendars._ID;
			intCalendars.ACCOUNT_NAME = Calendars.ACCOUNT_NAME;
			intCalendars.CALENDAR_DISPLAY_NAME = Calendars.CALENDAR_DISPLAY_NAME;
			intCalendars.VISIBLE = Calendars.VISIBLE;

			intEvents.CONTENT_URI = Events.CONTENT_URI;
			intEvents.CALENDAR_ID = Events.CALENDAR_ID;
			intEvents.TITLE = Events.TITLE;
			intEvents.DESCRIPTION = Events.DESCRIPTION;
			intEvents.EVENT_LOCATION = Events.EVENT_LOCATION;
			intEvents.ORGANIZER = Events.ORGANIZER;
			intEvents.ALL_DAY = Events.ALL_DAY;
			intEvents.DTEND = Events.DTEND;
			intEvents.DTSTART = Events.DTSTART;
			intEvents.HAS_ALARM = Events.HAS_ALARM;

			intReminders.CONTENT_URI = Reminders.CONTENT_URI;
			intReminders.MINUTES = Reminders.MINUTES;
			intReminders.EVENT_ID = Reminders.EVENT_ID;
			intReminders.METHOD_ALERT = Reminders.METHOD_ALERT;
			intReminders.METHOD = Reminders.METHOD;

			intCalendarAlerts.CONTENT_URI = CalendarAlerts.CONTENT_URI;
			intCalendarAlerts.STATE_SCHEDULED = CalendarAlerts.STATE_SCHEDULED;
			intCalendarAlerts.EVENT_ID = CalendarAlerts.EVENT_ID;
			intCalendarAlerts.BEGIN = CalendarAlerts.BEGIN;
			intCalendarAlerts.END = CalendarAlerts.END;
			intCalendarAlerts.ALERT_TIME = CalendarAlerts.ALARM_TIME;
			intCalendarAlerts.STATE = CalendarAlerts.STATE;
			intCalendarAlerts.MINUTES = CalendarAlerts.MINUTES;
		} catch (NoClassDefFoundError e) {
			setupBaseUris();
		}
	}
	
	private void setupBaseUris() {
		String baseUri = getCalendarUriBase();
		
		intCalendars.CONTENT_URI = Uri.parse(baseUri + "/calendars");
		intEvents.CONTENT_URI = Uri.parse(baseUri + "/events");
		intReminders.CONTENT_URI = Uri.parse(baseUri + "/reminders");
		intCalendarAlerts.CONTENT_URI = Uri.parse(baseUri + "/calendar_alerts");
	}
	
	/**
	 * Hack to get the proper uri.
	 */
	private String getCalendarUriBase() {
		String calendarUriBase = null;
		Uri calendars = Uri.parse("content://com.android.calendar/calendars");
		Cursor managedCursor = null;
		try {
			managedCursor = context.getContentResolver().query(calendars, null, null, null, null);
		} catch (Exception e) {
		}
		if (managedCursor != null) {
			calendarUriBase = "content://com.android.calendar";
		} else {
			calendars = Uri.parse("content://calendar/calendars");
			try {
				managedCursor = context.getContentResolver().query(calendars, null, null, null, null);
			} catch (Exception e) {
			}
			if (managedCursor != null) {
				calendarUriBase = "content://calendar";
			}
		}
		
		return calendarUriBase;
	}
}
