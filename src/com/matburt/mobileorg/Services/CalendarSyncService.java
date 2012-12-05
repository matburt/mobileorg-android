package com.matburt.mobileorg.Services;

import java.util.ArrayList;
import java.util.HashSet;

import android.app.Service;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.SQLException;
import android.net.Uri;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.text.format.Time;
import android.util.Log;

import com.matburt.mobileorg.R;
import com.matburt.mobileorg.OrgData.OrgFile;
import com.matburt.mobileorg.OrgData.OrgNode;
import com.matburt.mobileorg.OrgData.OrgNodeDate;
import com.matburt.mobileorg.OrgData.OrgProviderUtils;
import com.matburt.mobileorg.util.OrgFileNotFoundException;
import com.matburt.mobileorg.util.OrgNodeNotFoundException;

public class CalendarSyncService extends Service implements
		SharedPreferences.OnSharedPreferenceChangeListener {
	public final static String CLEARDB = "clearDB";
	public final static String FILELIST = "filelist";

	private final static String CALENDAR_ORGANIZER = "MobileOrg";

	private CalendarComptabilityWrappers calendar;

	private Context context;
	private SharedPreferences sharedPreferences;

	private ContentResolver resolver;

	private String calendarName = "";
	private int calendarId = -1;
	private Integer reminderTime = 0;
	private boolean reminderEnabled = false;
	private boolean showDone = false;
	private boolean showPast = true;
	private boolean showHabits = false;
	private HashSet<String> activeTodos = new HashSet<String>();
	private HashSet<String> allTodos = new HashSet<String>();
	
	private Thread syncThread = null;
	
	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}
	
	@Override
	public void onCreate() {
		super.onCreate();
		this.resolver = getContentResolver();
		this.context = getBaseContext();
		this.sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
		this.sharedPreferences.registerOnSharedPreferenceChangeListener(this);
		this.calendar = new CalendarComptabilityWrappers(context);
		refreshPreferences();
	}
	
	@Override
	public void onDestroy() {
		this.sharedPreferences.unregisterOnSharedPreferenceChangeListener(this);
		super.onDestroy();
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		Log.d("MobileOrg", "Cal:onstartCommand()");
		
		refreshPreferences();
		final String[] fileList = intent.getStringArrayExtra(FILELIST);
		final boolean clearDB = intent.getBooleanExtra(CLEARDB, false);
		this.syncThread = new Thread() {
			public void run() {
				if(clearDB)
					deleteAllEntries(getApplicationContext());
				else if (fileList != null)
					syncFiles(fileList);
				else
					syncFiles();
				
				syncThread = null;
			}
		};
		
		this.syncThread.run();
		
		return 0;
	}
	
	private void refreshPreferences() {
		this.reminderEnabled = sharedPreferences.getBoolean(
				"calendarReminder", false);

		if(reminderEnabled) {
			String intervalString = sharedPreferences.getString("calendarReminderInterval", "0");
			if(intervalString == null)
				throw new IllegalArgumentException("Invalid calendar reminder interval");
			this.reminderTime = Integer.valueOf(intervalString);
		}
		
		this.showDone = sharedPreferences.getBoolean("calendarShowDone", true);
		this.showPast = sharedPreferences.getBoolean("calendarShowPast", true);
		this.showHabits = sharedPreferences.getBoolean("calendarHabits", true);	
		this.calendarName = PreferenceManager
				.getDefaultSharedPreferences(context).getString("calendarName",
						"");
		this.calendarId = getCalendarID(calendarName);
		this.activeTodos = new HashSet<String>(OrgProviderUtils.getActiveTodos(resolver));
		this.allTodos = new HashSet<String>(OrgProviderUtils.getTodos(resolver));
	}
	
	private void syncFiles() {
		this.deleteAllEntries(context);
		
		ArrayList<String> files = OrgProviderUtils.getFilenames(resolver);
		files.remove(OrgFile.AGENDA_FILE);
		for(String filename: files)
			insertFileEntries(filename);
	}	
	
	private void syncFile(String filename) throws IllegalArgumentException {
		deleteFileEntries(filename, context);
		insertFileEntries(filename);
	}
	
	public void syncFiles(String[] files) {
		Log.d("MobileOrg", "starting to sync cal " + files.length);
		for(String file: files) {
			syncFile(file);
		}
	}
	
	public int deleteAllEntries(Context context) {
		refreshPreferences();
		return context.getContentResolver()
				.delete(calendar.events.CONTENT_URI, calendar.events.DESCRIPTION + " LIKE ?",
						new String[] { CALENDAR_ORGANIZER + "%" });
	}

	public int deleteFileEntries(String filename, Context context) {
		refreshPreferences();
		return context.getContentResolver().delete(calendar.events.CONTENT_URI,
				calendar.events.DESCRIPTION + " LIKE ?",
				new String[] { CALENDAR_ORGANIZER + ":" + filename + "%" });
	}

	
	public static CharSequence[] getCalendars(Context context) {
		CharSequence[] result = new CharSequence[1];
		result[0] = context.getString(R.string.error_setting_no_calendar);

		try {
			CalendarComptabilityWrappers calendar = new CalendarComptabilityWrappers(context);
			Cursor cursor = context.getContentResolver().query(
					calendar.calendars.CONTENT_URI,
					new String[] { calendar.calendars._ID,
							calendar.calendars.CALENDAR_DISPLAY_NAME }, null, null,
					null);
			if(cursor == null)
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
		} catch (SQLException e) {}

		return result;
	}
	
	
	public void insertNode(long node_id) {
		OrgNode node;
		try {
			node = new OrgNode(node_id, resolver);
		} catch (OrgNodeNotFoundException e) {
			return;
		}

		try {
			insertNode(node, node.getOrgFile(resolver).filename);
		} catch (OrgFileNotFoundException e) {
			insertNode(node, "");
		}
	}
	
	private void insertFileEntries(String filename) throws IllegalArgumentException {
		Cursor scheduled;
		
		try {
			scheduled = OrgProviderUtils.getFileSchedule(filename, this.showHabits,
					resolver);
		} catch (OrgFileNotFoundException e) {
			return;
		}

		while (scheduled.isAfterLast() == false) {
			try {
				OrgNode node = new OrgNode(scheduled);
				insertNode(node, filename);
			} catch (OrgNodeNotFoundException e) {}
			scheduled.moveToNext();
		}

		scheduled.close();
	}
	
	private void insertNode(OrgNode node, String filename)
			throws IllegalArgumentException {
		boolean isActive = true;
		
		if(allTodos.contains(node.todo))
			isActive = this.activeTodos.contains(node.todo);
		
		String cleanedName = node.getCleanedName();
		
		for (OrgNodeDate date : node.getOrgNodePayload().getDates()) {
			insertEntry(cleanedName, isActive, node.getCleanedPayload(),
					Long.toString(node.id), date, filename,
					node.getOrgNodePayload().getProperty("LOCATION"));
		}
	}

	// TODO Speed up using bulkInserts
	private String insertEntry(String name, boolean isTodoActive, String payload, 
			String orgID, OrgNodeDate date, String filename, String location) throws IllegalArgumentException {
		
		if (this.showDone == false && isTodoActive == false)
			return null;
				
		if(this.calendarId == -1)
			throw new IllegalArgumentException("Couldn't find selected calendar: " + calendarName);
		
		if(this.showPast == false && date.isInPast())
			return null;
		
		ContentValues values = new ContentValues();
		values.put(calendar.events.CALENDAR_ID, this.calendarId);
		values.put(calendar.events.TITLE, date.type + name);
		values.put(calendar.events.DESCRIPTION, CALENDAR_ORGANIZER + ":" + filename + "\n" + payload);
		values.put(calendar.events.EVENT_LOCATION, location);
		
		// Sync with google will overwrite organizer :(
		//values.put(intEvents.ORGANIZER, embeddedNodeMetadata);
		
		values.put(calendar.events.DTSTART, date.beginTime);
		values.put(calendar.events.DTEND, date.endTime);
		values.put(calendar.events.ALL_DAY, date.allDay);
		values.put(calendar.events.HAS_ALARM, 0);
		values.put(calendar.events.EVENT_TIMEZONE, Time.getCurrentTimezone());
		
		Uri uri = context.getContentResolver().insert(
				calendar.events.CONTENT_URI, values);
		String nodeID = uri.getLastPathSegment();
		
		if (date.allDay == 0 	&& this.reminderEnabled)
			addReminder(nodeID, date.beginTime, date.endTime);
		
		return nodeID;
	}

	private void addReminder(String eventID, long beginTime, long endTime) {
		if(beginTime < System.currentTimeMillis())
			return;
		
		ContentValues reminderValues = new ContentValues();
		reminderValues.put(calendar.reminders.MINUTES, this.reminderTime);
		reminderValues.put(calendar.reminders.EVENT_ID, eventID);
		reminderValues.put(calendar.reminders.METHOD, calendar.reminders.METHOD_ALERT);
		context.getContentResolver().insert(calendar.reminders.CONTENT_URI, reminderValues);
		
        ContentValues alertvalues = new ContentValues(); 
        alertvalues.put(calendar.calendarAlerts.EVENT_ID, eventID ); 
        alertvalues.put(calendar.calendarAlerts.BEGIN, beginTime ); 
        alertvalues.put(calendar.calendarAlerts.END, endTime ); 
        alertvalues.put(calendar.calendarAlerts.ALERT_TIME, this.reminderTime ); 
        alertvalues.put(calendar.calendarAlerts.STATE, calendar.calendarAlerts.STATE_SCHEDULED); 
        alertvalues.put(calendar.calendarAlerts.MINUTES, this.reminderTime ); 
		context.getContentResolver().insert(calendar.calendarAlerts.CONTENT_URI,
				alertvalues);
        
		ContentValues eventValues = new ContentValues();
		eventValues.put(calendar.events.HAS_ALARM, 1);
		context.getContentResolver().update(
				ContentUris.withAppendedId(calendar.events.CONTENT_URI,
						Long.valueOf(eventID)), eventValues, null, null);
	}
	
	private int getCalendarID(String calendarName) {
		Cursor cursor = context.getContentResolver().query(
				calendar.calendars.CONTENT_URI,
				new String[] { calendar.calendars._ID,
						calendar.calendars.CALENDAR_DISPLAY_NAME }, null,
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
			cursor.close();
		}
		return -1;
	}

	@SuppressWarnings("unused")
	private int deleteEntry(String nodeCalendarID) {
		return context.getContentResolver().delete(
				ContentUris.withAppendedId(calendar.events.CONTENT_URI,
						Long.valueOf(nodeCalendarID)), null, null);
	}
	
	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,
			String key) {
		if (key.startsWith("calendar")) {
			syncFiles();
		}
	}
}
