package com.matburt.mobileorg.Services;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.text.format.DateUtils;
import android.util.Log;

import com.matburt.mobileorg.Parsing.NodeWrapper;
import com.matburt.mobileorg.Parsing.OrgDatabase;

public class CalendarSyncService {
	private String CALENDAR_AUTH = null;
	private final static String CALENDAR_ORGANIZER = "MobileOrg";

	private OrgDatabase db;
	private Context context;
	
	public CalendarSyncService(OrgDatabase db, Context context) {
		this.db = db;
		this.context = context;
		this.CALENDAR_AUTH = getCalendarUriBase();
	}
	
	private int getCalendarID(String calendarName) {
		Cursor cursor = context.getContentResolver()
				.query(Uri.parse("content://" + CALENDAR_AUTH + "/calendars"),
						new String[] { "_id", "displayName" }, "selected=1",
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
			calendarUriBase = "com.android.calendar";
		} else {
			calendars = Uri.parse("content://calendar/calendars");
			try {
				managedCursor = context.getContentResolver().query(calendars, null, null, null, null);
			} catch (Exception e) {
			}
			if (managedCursor != null) {
				calendarUriBase = "calendar";
			}
		}
		
		return calendarUriBase;
	}

	// TODO Speed up using bulkInserts
	private String insertEntry(String name, boolean isTodoActive, String payload, String orgID, long beginTime,
			long endTime, int allDay, String filename) throws IllegalArgumentException {		
		final String calendarName = PreferenceManager
				.getDefaultSharedPreferences(context).getString("calendarName",
						"");
		int calId = getCalendarID(calendarName);
		
		if(calId == -1)
			throw new IllegalArgumentException("Couldn't find selected calendar: " + calendarName);

		final String embeddedNodeMetadata = CALENDAR_ORGANIZER + ":" + filename;
		
		ContentValues values = new ContentValues();
		values.put("calendar_id", calId);
		values.put("title", name);
		values.put("description", embeddedNodeMetadata + "\n" + payload);
		values.put("eventLocation", "");
		
		// Sync will with google will delete organizer :(
		//values.put("organizer", embeddedNodeMetadata);
		
		values.put("dtstart", beginTime);
		values.put("dtend", endTime);
		values.put("allDay", allDay);
		values.put("hasAlarm", 0);
		
		Uri uri = context.getContentResolver().insert(
				Uri.parse("content://" + CALENDAR_AUTH + "/events"), values);
		String nodeID = uri.getLastPathSegment();
		
		if (allDay == 0 && isTodoActive == true && PreferenceManager.getDefaultSharedPreferences(context).getBoolean(
				"calendarReminder", false))
			addReminder(nodeID, beginTime, endTime);
		
		return nodeID;
	}
	
	private void addReminder(String eventID, long beginTime, long endTime) {
		if(beginTime < System.currentTimeMillis())
			return;
		
		String intervalString = PreferenceManager.getDefaultSharedPreferences(
				context).getString("calendarReminderInterval", null);
		if(intervalString == null)
			throw new IllegalArgumentException("Invalid calendar reminder interval");
		long reminderTime = Integer.valueOf(intervalString);
		
		ContentValues values = new ContentValues();
		values.put("minutes", reminderTime);
		values.put("event_id", eventID);
		values.put("method", 1);
		context.getContentResolver().insert(
				Uri.parse("content://" + CALENDAR_AUTH + "/reminders"), values);
		
        ContentValues alertvalues = new ContentValues(); 
        alertvalues.put( "event_id", eventID ); 
        alertvalues.put( "begin", beginTime ); 
        alertvalues.put( "end", endTime ); 
        alertvalues.put( "alarmTime", reminderTime ); 
        alertvalues.put( "state", 0 ); 
        alertvalues.put( "minutes", reminderTime ); 
		context.getContentResolver().insert(
				Uri.parse("content://" + CALENDAR_AUTH + "/calendar_alerts"),
				alertvalues);
        
		ContentValues eventValues = new ContentValues();
		eventValues.put("hasAlarm", 1);
		context.getContentResolver().update(
				Uri.parse("content://" + CALENDAR_AUTH + "/events/" + eventID),
				eventValues, null, null);
		
	}
	
	public int deleteEntry(String calendarID) {
		return context.getContentResolver().delete(
				Uri.parse("content://" + CALENDAR_AUTH + "/events/"
						+ calendarID), null, null);
	}
	
	public int deleteFileEntries(String filename, Context context) {
		return context.getContentResolver().delete(
				Uri.parse("content://" + CALENDAR_AUTH + "/events/"),
				"description LIKE ?",
				new String[] { CALENDAR_ORGANIZER + ":" + filename + "%" });
	}
	
	public int deleteAllEntries(Context context) {
		return context.getContentResolver().delete(
				Uri.parse("content://" + CALENDAR_AUTH + "/events/"),
				"description LIKE ?", new String[] { CALENDAR_ORGANIZER + "%" });
	}
	
	private Date getDateInMs(String date) throws ParseException {
		SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd EEE");
		return formatter.parse(date);
	}
	
	private Date getTimeInMs(String time) throws ParseException {
		SimpleDateFormat formatter = new SimpleDateFormat("HH:mm");
		return formatter.parse(time);
	}
	
	private void insertNode(NodeWrapper node, String filename) throws IllegalArgumentException {
		final Pattern schedulePattern = Pattern
				.compile("(\\d{4}-\\d{2}-\\d{2}\\s\\w{3})\\s?(\\d{1,2}\\:\\d{2})?\\-?(\\d{1,2}\\:\\d{2})?");
		Matcher propm = schedulePattern.matcher(node.getScheduled(db));

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
				Log.w("MobileOrg", "Unable to parse schedule of: " + node.getName());
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
		
		Cursor cursor = context.getContentResolver()
				.query(Uri.parse("content://" + CALENDAR_AUTH + "/calendars"),
						new String[] { "_id", "displayName" }, "selected=1",
						null, null);
		
		result = new CharSequence[cursor.getCount()];
		
		if (cursor != null && cursor.moveToFirst()) {
			for (int i = 0; i < cursor.getCount(); i++) {
				result[i] = cursor.getString(1);
				cursor.moveToNext();
			}
		}
		cursor.close();
		return result;
	}
	
	public void syncFile(String filename) throws IllegalArgumentException {
		deleteFileEntries(filename, context);
		insertFileEntries(filename);
	}
}
