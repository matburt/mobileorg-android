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
import android.text.format.DateUtils;
import android.util.Log;

import com.matburt.mobileorg.Parsing.NodeWrapper;
import com.matburt.mobileorg.Parsing.OrgDatabase;

public class CalendarSyncService {
	private final static String CALENDAR_AUTH = "com.android.calendar";
	private final static String CALENDAR_ORGANIZER = "MobileOrg";

	private OrgDatabase db;
	private Context context;
	
	public CalendarSyncService(OrgDatabase db, Context context) {
		this.db = db;
		this.context = context;
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

				if (calName.equals(calName)) {
				    cursor.close();
					return calId;
				}
				cursor.moveToNext();
			}
		}
		cursor.close();
		return -1;
	}

	// TODO Speed up by using bulkInserts
	private String insertEntry(String name, String payload, String orgID, long beginTime,
			long endTime, int allDay, String filename) {
		int calId = getCalendarID("Personal");
		
		if(calId == -1)
			return null;

		ContentValues values = new ContentValues();
		values.put("calendar_id", calId);
		values.put("title", name);
		values.put("description", payload);
		//values.put("eventLocation", "");
		values.put("organizer", CALENDAR_ORGANIZER + ":" + filename);
		
		values.put("dtstart", beginTime);
		values.put("dtend", endTime);
		values.put("allDay", allDay);
		values.put("hasAlarm", 1);
		
		Uri uri = context.getContentResolver().insert(
				Uri.parse("content://" + CALENDAR_AUTH + "/events"), values);
		
		return uri.getLastPathSegment();
	}
	
	public int deleteEntry(String calendarID) {
		return context.getContentResolver().delete(
				Uri.parse("content://" + CALENDAR_AUTH + "/events/"
						+ calendarID), null, null);
	}
	
	static public int deleteFileEntries(String filename, Context context) {
		return context.getContentResolver().delete(
				Uri.parse("content://" + CALENDAR_AUTH + "/events/"),
				"organizer=?",
				new String[] { CALENDAR_ORGANIZER + ":" + filename });
	}
	
	static public int deleteAllEntries(Context context) {
		return context.getContentResolver().delete(
				Uri.parse("content://" + CALENDAR_AUTH + "/events/"),
				"organizer LIKE ?", new String[] { CALENDAR_ORGANIZER + "%" });
	}
	
	private Date getDateInMs(String date) throws ParseException {
		SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd EEE");
		return formatter.parse(date);
	}
	
	private Date getTimeInMs(String time) throws ParseException {
		SimpleDateFormat formatter = new SimpleDateFormat("HH:mm");
		return formatter.parse(time);
	}
	
	private void insertNode(NodeWrapper node, String filename) {
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

				insertEntry(node.getName(), node.getCleanedPayload(db),
						node.getNodeId(db), beginTime, endTime, allDay,
						filename);

			} catch (ParseException e) {
				Log.w("MobileOrg", "Unable to parse: " + node.getName());
			}
		} else
			Log.w("MobileOrg", "Couln't parse schedule of " + node.getName());
	}

	public void insertFileEntries(String filename) {
		Cursor scheduled = db.getFileSchedule(filename);
		
		if(scheduled == null)
			return;
		
		while(scheduled.isAfterLast() == false) {
			NodeWrapper node = new NodeWrapper(scheduled);
			insertNode(node, filename);
			scheduled.moveToNext();
		}
		
		scheduled.close();
	}

	@SuppressWarnings("unused")
	private void readCalendars() {
		int calId = getCalendarID("Personal");
		
		if (calId != -1) {

			Cursor query = context.getContentResolver().query(
					Uri.parse("content://" + CALENDAR_AUTH + "/events"),
					new String[] {"_id", "title"}, null, null, null);
			
			if(query != null && query.moveToFirst()) {
				while(query.isAfterLast() == false) {
					Log.d("MobileOrg", "" + query.getString(query.getColumnIndex("title")));
					query.moveToNext();
				}
			}
			query.close();
		}
	}
	
	public void syncFile(String filename) {
		deleteFileEntries(filename, context);
		insertFileEntries(filename);
	}

	
//	// Projection array. Creating indices for this array instead of doing
//	  // dynamic lookups improves performance.
//	  public static final String[] EVENT_PROJECTION = new String[] {
//	    Calendars._ID,                           // 0
//	    Calendars.ACCOUNT_NAME,                  // 1
//	    Calendars.CALENDAR_DISPLAY_NAME          // 2
//	  };
//	  
//	  // The indices for the projection array above.
//	  private static final int PROJECTION_ID_INDEX = 0;
//	  private static final int PROJECTION_ACCOUNT_NAME_INDEX = 1;
//	  private static final int PROJECTION_DISPLAY_NAME_INDEX = 2;
//	
//	public void writeTest() {
//		// Run query
//		Cursor cur = null;
//		ContentResolver cr = context.getContentResolver();
//		Uri uri = Calendars.CONTENT_URI;
//		String selection = "((" + Calendars.ACCOUNT_NAME + " = ?) AND (" 
//		                        + Calendars.ACCOUNT_TYPE + " = ?))";
//		String[] selectionArgs = new String[] {"hdweiss@gmail.com", "com.google"}; 
//		// Submit the query and get a Cursor object back. 
//		cur = cr.query(uri, EVENT_PROJECTION, selection, selectionArgs, null);
//		
//		Log.d("MobileOrg", "Starting to query calendar");
//		
//		// Use the cursor to step through the returned records
//		while (cur.moveToNext()) {
//		    long calID = 0;
//		    String displayName = null;
//		    String accountName = null;        
//		      
//		    // Get the field values
//		    calID = cur.getLong(PROJECTION_ID_INDEX);
//		    displayName = cur.getString(PROJECTION_DISPLAY_NAME_INDEX);
//		    accountName = cur.getString(PROJECTION_ACCOUNT_NAME_INDEX);
//		              
//		    // Do something with the values...
//		    Log.d("MobileOrg", calID + " " + displayName + " " + accountName);
//		}
//	}

}
