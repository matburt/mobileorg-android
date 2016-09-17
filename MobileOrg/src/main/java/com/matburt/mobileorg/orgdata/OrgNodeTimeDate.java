package com.matburt.mobileorg.orgdata;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;

import com.matburt.mobileorg.orgdata.OrgContract.Timestamps;
import com.matburt.mobileorg.orgdata.OrgDatabase.Tables;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Collections;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Map;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class OrgNodeTimeDate {
	private static final String timestampPattern = "<((\\d{4})-(\\d{1,2})-(\\d{1,2}))(?:[^\\d]*)"
			+ "((\\d{1,2})\\:(\\d{2}))?(-((\\d{1,2})\\:(\\d{2})))?[^>]*>";
	// Trick for the initialization of a static map
	private static final Map<TYPE, Pattern> patterns;

	static {
		Map<TYPE, Pattern> tmpMap = new HashMap<>();
		tmpMap.put(TYPE.Deadline, Pattern.compile("DEADLINE:\\s*"+timestampPattern));
		tmpMap.put(TYPE.Scheduled, Pattern.compile("SCHEDULED:\\s*"+timestampPattern));
		patterns = Collections.unmodifiableMap(tmpMap);
	}

	public TYPE type = TYPE.Scheduled;
	public int year = -1;
	public int monthOfYear = -1;
	public int dayOfMonth = -1;
	public int startTimeOfDay = -1;
	public int startMinute = -1;
	public int endTimeOfDay = -1;
	public int endMinute = -1;
	public int matchStart = -1, matchEnd = -1;
	public OrgNodeTimeDate(TYPE type) {
		this.type = type;
	}


	public OrgNodeTimeDate(TYPE type, String line){
		this.type = type;
		parseDate(line);
	}

	public OrgNodeTimeDate(TYPE type, int day, int month, int year) {
		this(type);
		setDate(day, month, year);
	}

	public OrgNodeTimeDate(TYPE type, int day, int month, int year, int startTimeOfDay, int startMinute) {
		this(type, day, month, year);
		setTime(startTimeOfDay, startMinute);
	}

    public OrgNodeTimeDate(long epochTimeInSec){
        setEpochTime(epochTimeInSec, false);
    }

	/**
	 * OrgNodeTimeDate ctor from the database
	 * @param type
	 * @param nodeId The OrgNode ID associated with this timestamp
     */
	public OrgNodeTimeDate(TYPE type, long nodeId){

		String todoQuery = "SELECT " +
				OrgContract.formatColumns(
						Tables.TIMESTAMPS,
						Timestamps.DEFAULT_COLUMNS ) +
				" FROM " + Tables.TIMESTAMPS +
				" WHERE " + Timestamps.NODE_ID + " = " + nodeId +
				"   AND " + Timestamps.TYPE+ " = " + type.ordinal() +
				" ORDER BY " + Timestamps.TIMESTAMP;

		this.type = type;

		Cursor cursor = OrgDatabase.getInstance().getReadableDatabase().rawQuery(todoQuery, null);
		set(cursor);
	}

	public static String typeToFormated(TYPE type) {
		switch (type) {
			case Scheduled:
				return "SCHEDULED: ";
			case Deadline:
				return "DEADLINE: ";
			case Timestamp:
				return "";
			default:
				return "";
		}
	}

	public static String formatDate(TYPE type, String timestamp) {
		if (TextUtils.isEmpty(timestamp))
			return "";
		else {
			return OrgNodeTimeDate.typeToFormated(type) + "<" + timestamp + ">";
		}
	}

	static public Pattern getTimestampMatcher(OrgNodeTimeDate.TYPE type) {
		final String timestampPattern = "<([^>]+)(\\d\\d:\\d\\d)>"; // + "(?:\\s*--\\s*<([^>]+)>)?"; for ranged date
//		final String timestampLookbehind = "\\s*(?<!(?:SCHEDULED:|DEADLINE:)\\s?)";

//		String pattern;
//		if(type == OrgNodeTimeDate.TYPE.Timestamp)
//			pattern = timestampLookbehind + "(" + timestampPattern + ")";
//		else

		String pattern = "\\s*(" + OrgNodeTimeDate.typeToFormated(type) + "\\s*" + timestampPattern + ")";

		return Pattern.compile(pattern);
	}

	public void set(Cursor cursor) {

		if (cursor != null && cursor.getCount() > 0) {
			if(cursor.isBeforeFirst() || cursor.isAfterLast())
				cursor.moveToFirst();

			long epochTime = cursor.getLong(cursor.getColumnIndexOrThrow(Timestamps.TIMESTAMP));

			boolean allDay = cursor.getLong(cursor.getColumnIndexOrThrow(Timestamps.ALL_DAY)) == 1;

            setEpochTime(epochTime, allDay);
		}
	}

    /**
     * Reset the OrgNodeTimeDate with this epochTime is seconds
     * @param epochTimeInSec
     * @param allDay
     */
    void setEpochTime(long epochTimeInSec, boolean allDay){
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeZone(TimeZone.getTimeZone("GMT0"));
        calendar.setTimeInMillis(epochTimeInSec * 1000L);
        year = calendar.get(Calendar.YEAR);
        monthOfYear = calendar.get(Calendar.MONTH);
        dayOfMonth = calendar.get(Calendar.DAY_OF_MONTH);
        if(!allDay){
            startMinute = calendar.get(Calendar.MINUTE);
            startTimeOfDay = calendar.get(Calendar.HOUR_OF_DAY);
        }
    }

	public void setDate(int day, int month, int year) {
		this.dayOfMonth = day;
		this.monthOfYear = month;
		this.year = year;
	}

	public void setTime(int startTimeOfDay, int startMinute) {
		this.startTimeOfDay = startTimeOfDay;
		this.startMinute = startMinute;
	}

	public void setToCurrentDate() {
		final Calendar c = Calendar.getInstance();
		this.year = c.get(Calendar.YEAR);
		this.monthOfYear = c.get(Calendar.MONTH) + 1;
		this.dayOfMonth = c.get(Calendar.DAY_OF_MONTH);
	}

	public void parseDate(String line) {
		if(line == null)
			return;

//		Log.v("timemap", "map : "+type);
//		Log.v("timemap", "line : "+line);
		if(patterns.get(type)==null) return;
		Matcher propm = patterns.get(type).matcher(line);
		if (propm.find()) {
//			Log.v("timemap", "here");
			matchStart = propm.start();
			matchEnd = propm.end();
			try {
				year = Integer.parseInt(propm.group(2));
				monthOfYear = Integer.parseInt(propm.group(3));
				dayOfMonth = Integer.parseInt(propm.group(4));

				if (propm.group(6) != null && propm.group(7) != null) {
					startTimeOfDay = Integer.parseInt(propm.group(6));
					startMinute = Integer.parseInt(propm.group(7));
				}

				endTimeOfDay = Integer.parseInt(propm.group(10));
				endMinute = Integer.parseInt(propm.group(11));
			} catch (NumberFormatException e) {}
		}
	}
	
	public String getDate() {
		if(year < 0 || monthOfYear < 0 || dayOfMonth < 0) return "";
		return String.format("%d-%02d-%02d", year, monthOfYear + 1, dayOfMonth);
	}

	public String getStartTime() {
		if(startMinute < 0 || startTimeOfDay < 0) return "";
		return String.format("%02d:%02d", startTimeOfDay, startMinute);
	}

	public String getEndTime() {
		return String.format("%02d:%02d", endTimeOfDay, endMinute);
	}

	/**
	 * Return the number of seconds elapsed since the start of 1st January 1970
	 * @return
     */
	public long getEpochTime(){
		if(year == -1 || dayOfMonth == -1 || monthOfYear == -1) return -1;
		int hour = startTimeOfDay > -1 ? startTimeOfDay : 0;
		int minute = startMinute > -1 ? startMinute : 0;
		GregorianCalendar calendar = new GregorianCalendar(year, monthOfYear, dayOfMonth, hour, minute);
		calendar.setTimeZone(TimeZone.getTimeZone("GMT0"));
		return calendar.getTimeInMillis() / 1000L;
	}

	/**
	 * Check if event is all day long
	 * An event is considered all day if startTimeOfDay or startMinute is undefined
	 * @return
     */
	public long isAllDay(){
		return (startTimeOfDay < 0 || startMinute < 0) ? 1 : 0;
	}

    /**
     * Format the string according to the parameter isDate
     * @param isDate: date formating or time formating
     * @return
     */
	public String toString(boolean isDate) {
        GregorianCalendar calendar = new GregorianCalendar(year, monthOfYear, dayOfMonth, startTimeOfDay, startMinute);
        calendar.setTimeZone(TimeZone.getTimeZone("GMT0"));
        DateFormat instance = isDate ? SimpleDateFormat.getDateInstance() : SimpleDateFormat.getTimeInstance(DateFormat.SHORT);
        instance.setTimeZone(TimeZone.getTimeZone("GMT0"));
        return instance.format(calendar.getTime());

	}
	
	public String toFormatedString() {
		return formatDate(type, getDate());
	}
	
	private String getStartTimeFormated() {
		String time = getStartTime().toString();

		if (startTimeOfDay == -1
				|| startMinute == -1 || TextUtils.isEmpty(time))
			return "";
		else
			return " " + time;
	}
	
	private String getEndTimeFormated() {
		String time = getEndTime().toString();

		if (endTimeOfDay == -1
				|| endMinute == -1 || TextUtils.isEmpty(time))
			return "";
		else
			return "-" + time;
	}

    static public void deleteTimestamp(Context context, long nodeId, String where){
        Uri uri = OrgContract.Timestamps.buildIdUri(nodeId);
        context.getContentResolver().delete(uri, where, null);
    }

	public void update(Context context, long nodeId, long fileId) {
        deleteTimestamp(context, nodeId, Timestamps.TYPE + "="+type.ordinal());
		if (getEpochTime() < 0) return;
		context.getContentResolver().
				insert(
                        OrgContract.Timestamps.buildIdUri(nodeId),
						getContentValues(nodeId, fileId));
		Log.v("OrgNodeTimeDate","update epoch : "+getEpochTime() + " with type : "+type);
	}

	private ContentValues getContentValues(long nodeId, long fileId) {
		ContentValues values = new ContentValues();
		values.put(Timestamps.ALL_DAY, isAllDay());
		values.put(Timestamps.TIMESTAMP, getEpochTime());
		values.put(Timestamps.TYPE, type.ordinal());
		values.put(Timestamps.NODE_ID, nodeId);
		values.put(Timestamps.FILE_ID, fileId);
		return values;
	}

	public enum TYPE {
		Scheduled,
		Deadline,
		Timestamp,
		InactiveTimestamp
	}

	/**
	 * Check if this was at least a day before date
	 * @param date
	 * @return
	 */
	public boolean isBefore(OrgNodeTimeDate date) {
		return this.year < date.year ||
				(this.year == date.year && (this.monthOfYear < date.monthOfYear ||
						(this.monthOfYear == date.monthOfYear && this.dayOfMonth < date.dayOfMonth)));
	}

	/**
	 * Check the crrent date is between start date (excluded) and to date(excluded)
	 * WARNING, can also be between end (first) and start (at the end) !!
	 * @param start: the starting date
	 * @param end:   the ending date
	 * @return
	 */
	public boolean isBetween(OrgNodeTimeDate start, OrgNodeTimeDate end) {
		return
				start.isBefore(this) && this.isBefore(end)
						|| end.isBefore(this) && this.isBefore(start);
	}


}
