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
import com.matburt.mobileorg.OrgData.CalendarEntries;
import com.matburt.mobileorg.OrgData.CalendarEntries.CalendarEntry;
import com.matburt.mobileorg.OrgData.OrgFile;
import com.matburt.mobileorg.OrgData.OrgNode;
import com.matburt.mobileorg.OrgData.OrgNodeDate;
import com.matburt.mobileorg.OrgData.OrgProviderUtils;
import com.matburt.mobileorg.util.MultiMap;
import com.matburt.mobileorg.util.OrgFileNotFoundException;
import com.matburt.mobileorg.util.OrgNodeNotFoundException;
import com.matburt.mobileorg.util.OrgUtils;

public class CalendarSyncService extends Service implements
		SharedPreferences.OnSharedPreferenceChangeListener {
	public final static String CLEARDB = "clearDB";
	public final static String PULL = "pull";
	public static final String PUSH = "push";
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

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

	@Override
	public void onCreate() {
		super.onCreate();
		this.resolver = getContentResolver();
		this.context = getBaseContext();
		this.sharedPreferences = PreferenceManager
				.getDefaultSharedPreferences(context);
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
		refreshPreferences();
		final String[] fileList = intent.getStringArrayExtra(FILELIST);
		final boolean clearDB = intent.getBooleanExtra(CLEARDB, false);
		final boolean pull = intent.getBooleanExtra(PULL, false);
		final boolean push = intent.getBooleanExtra(PUSH, false);
		
		new Thread() {
			public void run() {
				if (clearDB) {
					if (fileList != null)
						deleteFileEntries(fileList);
					else
						deleteEntries();
				} 
				
				if (push) {
					if (fileList != null)
						syncFiles(fileList);
					else
						syncFiles();
				}
				
				if(pull) {
					assimilateCalendar();
				}
			}
		}.run();

		return 0;
	}

	private void syncFiles() {
		// deleteEntries();

		ArrayList<String> files = OrgProviderUtils.getFilenames(resolver);
		files.remove(OrgFile.AGENDA_FILE);
		for (String filename : files)
			syncFile(filename);
	}

	private void syncFiles(String[] files) {
		for (String filename : files) {
			if (filename.equals(OrgFile.AGENDA_FILE) == false) {
				// deleteFileEntries(filename);
				syncFile(filename);
			}
		}
	}

	private int deleteEntries() {
		refreshPreferences();
		return context.getContentResolver().delete(calendar.events.CONTENT_URI,
				calendar.events.DESCRIPTION + " LIKE ?",
				new String[] { CALENDAR_ORGANIZER + "%" });
	}

	private void deleteFileEntries(String[] files) {
		for (String file : files) {
			deleteFileEntries(file);
		}
	}
	
	private int deleteFileEntries(String filename) {
		refreshPreferences();
		return context.getContentResolver().delete(calendar.events.CONTENT_URI,
				calendar.events.DESCRIPTION + " LIKE ?",
				new String[] { CALENDAR_ORGANIZER + ":" + filename + "%" });
	}


	private int inserted = 0;
	private int deleted = 0;
	private int unchanged = 0;

	private void syncFile(String filename) {
		inserted = 0;
		deleted = 0;
		unchanged = 0;

		Cursor scheduledQuery;
		try {
			scheduledQuery = OrgProviderUtils.getFileSchedule(filename,
					this.showHabits, resolver);
		} catch (OrgFileNotFoundException e) {
			return;
		}

		MultiMap<CalendarEntry> entries = getCalendarEntries(filename);

		while (scheduledQuery.isAfterLast() == false) {
			try {
				OrgNode node = new OrgNode(scheduledQuery);
				syncNode(node, entries, filename);
			} catch (OrgNodeNotFoundException e) {
			}
			scheduledQuery.moveToNext();
		}
		scheduledQuery.close();

		removeCalendarEntries(entries);

		Log.d("MobileOrg", "Calendar (" + filename + ") Inserted: " + inserted
				+ " and deleted: " + deleted + " unchanged: " + unchanged);
	}

	private void syncNode(OrgNode node, MultiMap<CalendarEntry> entries,
			String filename) {
		boolean isActive = true;
		if (allTodos.contains(node.todo))
			isActive = this.activeTodos.contains(node.todo);

		for (OrgNodeDate date : node.getOrgNodePayload().getDates(
				node.getCleanedName())) {
			try {
				CalendarEntry insertedEntry = getInsertedEntry(date, entries);
				entries.remove(date.beginTime, insertedEntry);
				unchanged++;
			} catch (IllegalArgumentException e) {
				String insertedEntry = insertEntry(isActive,
						node.getCleanedPayload(), Long.toString(node.id), date,
						filename,
						node.getOrgNodePayload().getProperty("LOCATION"));

				if (insertedEntry != null)
					inserted++;
			}
		}
	}

	private void assimilateCalendar() {
		Cursor query = getUnassimilatedCalendarCursor();
		
		CalendarEntries entriesParser = new CalendarEntries(calendar.events,
				query);
		
		while(query.isAfterLast() == false) {
			CalendarEntry entry = entriesParser.getEntryFromCursor(query);
			OrgNode node = entry.getOrgNode();
			
			OrgFile captureFile = OrgProviderUtils
					.getOrCreateCaptureFile(getContentResolver());
			node.fileId = captureFile.id;
			node.parentId = captureFile.nodeId;
			node.level = 1;
			
			node.write(getContentResolver());
			OrgUtils.announceSyncDone(this);
			
			deleteEntry(entry);
			
			query.moveToNext();
		}
		
		query.close();
	}
			
	private Cursor getUnassimilatedCalendarCursor() {
		String[] eventsProjection = new String[] { calendar.events.CALENDAR_ID,
				calendar.events.DTSTART, calendar.events.DTEND,
				calendar.events.DESCRIPTION, calendar.events.TITLE,
				calendar.events.EVENT_LOCATION };

		Cursor query = context.getContentResolver().query(
				calendar.events.CONTENT_URI, eventsProjection,
				calendar.events.DESCRIPTION + " NOT LIKE ?",
				new String[] { CALENDAR_ORGANIZER + "%" },
				null);
		query.moveToFirst();
		
		return query;
	}
	
	private Cursor getCalendarCursor(String filename) {
		String[] eventsProjection = new String[] { calendar.events.CALENDAR_ID,
				calendar.events.DTSTART, calendar.events.DTEND,
				calendar.events.DESCRIPTION, calendar.events.TITLE,
				calendar.events.EVENT_LOCATION };

		Cursor query = context.getContentResolver().query(
				calendar.events.CONTENT_URI, eventsProjection,
				calendar.events.DESCRIPTION + " LIKE ?",
				new String[] { CALENDAR_ORGANIZER + ":" + filename + "%" },
				null);
		query.moveToFirst();
		
		return query;
	}
	
	private MultiMap<CalendarEntry> getCalendarEntries(String filename) {
		refreshPreferences();

		Cursor query = getCalendarCursor(filename);

		MultiMap<CalendarEntry> map = new MultiMap<CalendarEntry>();
		CalendarEntries entriesParser = new CalendarEntries(calendar.events,
				query);

		while (query.isAfterLast() == false) {
			CalendarEntry entry = entriesParser.getEntryFromCursor(query);
			map.put(entry.dtStart, entry);

			query.moveToNext();
		}

		return map;
	}
	
	/**
	 * Checks if the given entry is contained given multiMap.
	 * @throws IllegalArgumentException When entry is not found
	 */
	private CalendarEntry getInsertedEntry(OrgNodeDate date,
			MultiMap<CalendarEntry> entries) throws IllegalArgumentException {
		ArrayList<CalendarEntry> matches = entries.get(date.beginTime);

		if (matches == null)
			throw new IllegalArgumentException();

		for (CalendarEntry entry : matches) {
			if (entry.isEquals(date))
				return entry;
		}

		throw new IllegalArgumentException();
	}

	
	private void removeCalendarEntries(MultiMap<CalendarEntry> entries) {
		for (Long entryKey : entries.keySet()) {
			for (CalendarEntry entry : entries.get(entryKey)) {
				Log.d("MobileOrg", "Deleting entry for " + entry.title);
				deleteEntry(entry);
				deleted++;
			}
		}
	}


	private String insertEntry(boolean isTodoActive, String payload,
			String orgID, OrgNodeDate date, String filename, String location)
			throws IllegalArgumentException {

		if (this.showDone == false && isTodoActive == false)
			return null;

		if (this.calendarId == -1)
			throw new IllegalArgumentException(
					"Couldn't find selected calendar: " + calendarName);

		if (this.showPast == false && date.isInPast())
			return null;

		ContentValues values = new ContentValues();
		values.put(calendar.events.CALENDAR_ID, this.calendarId);
		values.put(calendar.events.TITLE, date.getTitle());
		values.put(calendar.events.DESCRIPTION, CALENDAR_ORGANIZER + ":"
				+ filename + "\n" + payload);
		values.put(calendar.events.EVENT_LOCATION, location);

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


	private int deleteEntry(CalendarEntry entry) {
		return context.getContentResolver().delete(
				ContentUris.withAppendedId(calendar.events.CONTENT_URI,
						entry.id), null, null);
	}
	
	private int getCalendarID(String calendarName) {
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


	private void refreshPreferences() {
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

		this.showDone = sharedPreferences.getBoolean("calendarShowDone", true);
		this.showPast = sharedPreferences.getBoolean("calendarShowPast", true);
		this.showHabits = sharedPreferences.getBoolean("calendarHabits", true);
		this.calendarName = PreferenceManager.getDefaultSharedPreferences(
				context).getString("calendarName", "");
		this.calendarId = getCalendarID(calendarName);
		this.activeTodos = new HashSet<String>(
				OrgProviderUtils.getActiveTodos(resolver));
		this.allTodos = new HashSet<String>(OrgProviderUtils.getTodos(resolver));
	}

	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,
			String key) {
		if (key.startsWith("calendar")) {
			syncFiles();
		}
	}
}
