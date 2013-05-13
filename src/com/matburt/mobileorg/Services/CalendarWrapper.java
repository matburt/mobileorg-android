package com.matburt.mobileorg.Services;

import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.SQLException;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.text.format.Time;

import com.matburt.mobileorg.R;
import com.matburt.mobileorg.OrgData.CalendarEntry;
import com.matburt.mobileorg.OrgData.OrgNodeDate;

public class CalendarWrapper {

	private final static String CALENDAR_ORGANIZER = "MobileOrg";
	
	private Context context;
	private SharedPreferences sharedPreferences;

	public CalendarComptabilityWrappers calendar;

	private String calendarName = "";
	private int calendarId = -1;
	private Integer reminderTime = 0;
	private boolean reminderEnabled = false;
	
	public CalendarWrapper(Context context) {
		this.context = context;
		this.sharedPreferences = PreferenceManager
				.getDefaultSharedPreferences(context);
		this.calendar = new CalendarComptabilityWrappers(context);

	}
	
	public int deleteEntries() {
		refreshPreferences();
		return context.getContentResolver().delete(calendar.events.CONTENT_URI,
				calendar.events.DESCRIPTION + " LIKE ?",
				new String[] { CALENDAR_ORGANIZER + "%" });
	}

	public void deleteFileEntries(String[] files) {
		for (String file : files) {
			deleteFileEntries(file);
		}
	}
	
	public int deleteFileEntries(String filename) {
		refreshPreferences();
		return context.getContentResolver().delete(calendar.events.CONTENT_URI,
				calendar.events.DESCRIPTION + " LIKE ?",
				new String[] { CALENDAR_ORGANIZER + ":" + filename + "%" });
	}
	
	public String insertEntry(OrgNodeDate date, String payload,
			String filename, String location, String busy) throws IllegalArgumentException {

		if (this.calendarId == -1)
			throw new IllegalArgumentException(
					"Couldn't find selected calendar: " + calendarName);

		ContentValues values = new ContentValues();
		values.put(calendar.events.CALENDAR_ID, this.calendarId);
		values.put(calendar.events.TITLE, date.getTitle());
		values.put(calendar.events.DESCRIPTION, CALENDAR_ORGANIZER + ":"
				+ filename + "\n" + payload);
		values.put(calendar.events.EVENT_LOCATION, location);

		// If a busy state was given, send that info to calendar
		if (busy != null) {
			// Trying to be reasonably tolerant with respect to the accepted values.
			if (busy.equals("nil") || busy.equals("0") ||
			    busy.equals("no")  || busy.equals("available"))
				values.put(calendar.events.AVAILABILITY, calendar.events.AVAILABILITY_FREE);
		
			else if (busy.equals("t")   || busy.equals("1") ||
				 busy.equals("yes") || busy.equals("busy"))
				values.put(calendar.events.AVAILABILITY, calendar.events.AVAILABILITY_BUSY);

			else if (busy.equals("2") || busy.equals("tentative") || busy.equals("maybe"))
				values.put(calendar.events.AVAILABILITY, calendar.events.AVAILABILITY_TENTATIVE);
		}
		
		// Sync with google will overwrite organizer :(
		// values.put(intEvents.ORGANIZER, embeddedNodeMetadata);

		values.put(calendar.events.DTSTART, date.beginTime);
		values.put(calendar.events.DTEND, date.endTime);
		values.put(calendar.events.ALL_DAY, date.allDay);
		values.put(calendar.events.HAS_ALARM, 0);
		values.put(calendar.events.EVENT_TIMEZONE, Time.getCurrentTimezone());

		Uri uri = context.getContentResolver().insert(
				calendar.events.CONTENT_URI, values);
		String nodeID = uri.getLastPathSegment();

		if (date.allDay == 0 && this.reminderEnabled)
			addReminder(nodeID, date.beginTime, date.endTime);

		return nodeID;
	}

	
	private void addReminder(String eventID, long beginTime, long endTime) {
		if (beginTime < System.currentTimeMillis())
			return;

		ContentValues reminderValues = new ContentValues();
		reminderValues.put(calendar.reminders.MINUTES, this.reminderTime);
		reminderValues.put(calendar.reminders.EVENT_ID, eventID);
		reminderValues.put(calendar.reminders.METHOD,
				calendar.reminders.METHOD_ALERT);
		context.getContentResolver().insert(calendar.reminders.CONTENT_URI,
				reminderValues);

		ContentValues alertvalues = new ContentValues();
		alertvalues.put(calendar.calendarAlerts.EVENT_ID, eventID);
		alertvalues.put(calendar.calendarAlerts.BEGIN, beginTime);
		alertvalues.put(calendar.calendarAlerts.END, endTime);
		alertvalues.put(calendar.calendarAlerts.ALERT_TIME, this.reminderTime);
		alertvalues.put(calendar.calendarAlerts.STATE,
				calendar.calendarAlerts.STATE_SCHEDULED);
		alertvalues.put(calendar.calendarAlerts.MINUTES, this.reminderTime);
		context.getContentResolver().insert(
				calendar.calendarAlerts.CONTENT_URI, alertvalues);

		ContentValues eventValues = new ContentValues();
		eventValues.put(calendar.events.HAS_ALARM, 1);
		context.getContentResolver().update(
				ContentUris.withAppendedId(calendar.events.CONTENT_URI,
						Long.valueOf(eventID)), eventValues, null, null);
	}


	public int deleteEntry(CalendarEntry entry) {
		return context.getContentResolver().delete(
				ContentUris.withAppendedId(calendar.events.CONTENT_URI,
						entry.id), null, null);
	}
	
	public int getCalendarID(String calendarName) {
		Cursor cursor = context.getContentResolver().query(
				calendar.calendars.CONTENT_URI,
				new String[] { calendar.calendars._ID,
						calendar.calendars.CALENDAR_DISPLAY_NAME }, null, null,
				null);
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
			cursor.close();
		}
		return -1;
	}

	public Cursor getUnassimilatedCalendarCursor() {
		Cursor query = context.getContentResolver().query(
				calendar.events.CONTENT_URI,
				calendar.eventsProjection,
				calendar.events.CALENDAR_ID + "=? AND "
						+ calendar.events.DESCRIPTION + " NOT LIKE ?",
				new String[] { Integer.toString(this.calendarId),
						CALENDAR_ORGANIZER + "%" }, null);
		query.moveToFirst();
		
		return query;
	}
	
	public Cursor getCalendarCursor(String filename) {
		Cursor query = context.getContentResolver().query(
				calendar.events.CONTENT_URI, calendar.eventsProjection,
				calendar.events.DESCRIPTION + " LIKE ?",
				new String[] { CALENDAR_ORGANIZER + ":" + filename + "%" },
				null);
		query.moveToFirst();
		
		return query;
	}
	
	public static CharSequence[] getCalendars(Context context) {
		CharSequence[] result = new CharSequence[1];
		result[0] = context.getString(R.string.error_setting_no_calendar);

		try {
			CalendarComptabilityWrappers calendar = new CalendarComptabilityWrappers(
					context);
			Cursor cursor = context.getContentResolver().query(
					calendar.calendars.CONTENT_URI,
					new String[] { calendar.calendars._ID,
							calendar.calendars.CALENDAR_DISPLAY_NAME }, null,
					null, null);
			if (cursor == null)
				return result;

			if (cursor.getCount() == 0) {
				cursor.close();
				return result;
			}

			if (cursor.moveToFirst()) {
				result = new CharSequence[cursor.getCount()];

				for (int i = 0; i < cursor.getCount(); i++) {
					result[i] = cursor.getString(1);
					cursor.moveToNext();
				}
			}
			cursor.close();
		} catch (SQLException e) {
		}

		return result;
	}
	
	public void refreshPreferences() {
		this.reminderEnabled = sharedPreferences.getBoolean("calendarReminder",
				false);

		if (reminderEnabled) {
			String intervalString = sharedPreferences.getString(
					"calendarReminderInterval", "0");
			if (intervalString == null)
				throw new IllegalArgumentException(
						"Invalid calendar reminder interval");
			this.reminderTime = Integer.valueOf(intervalString);
		}
		
		this.calendarName = PreferenceManager.getDefaultSharedPreferences(
				context).getString("calendarName", "");
		this.calendarId = getCalendarID(calendarName);
	}
}
