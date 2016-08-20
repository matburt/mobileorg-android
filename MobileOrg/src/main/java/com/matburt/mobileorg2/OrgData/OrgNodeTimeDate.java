package com.matburt.mobileorg2.OrgData;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;

import java.sql.Time;
import java.util.Calendar;
import java.util.Collections;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.matburt.mobileorg2.OrgData.OrgContract.Timestamps;
import com.matburt.mobileorg2.OrgData.OrgDatabase.Tables;

public class OrgNodeTimeDate {
	public TYPE type = TYPE.Scheduled;

	public int year = -1;
	public int monthOfYear = -1;
	public int dayOfMonth = -1;
	public int startTimeOfDay = -1;
	public int startMinute = -1;
	public int endTimeOfDay = -1;
	public int endMinute = -1;
	public int matchStart = -1, matchEnd = -1;
	
	public enum TYPE {
		Scheduled,
		Deadline,
		Timestamp,
		InactiveTimestamp
	}

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
		this(type);
		setDate(day, month, year);
		setTime(startTimeOfDay, startMinute);
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
				"   AND " + Timestamps.TYPE+ " = " + type.ordinal();

		Log.v("todo", "query : " + todoQuery);
		this.type = type;

		Cursor cursor = OrgDatabase.getInstance().getReadableDatabase().rawQuery(todoQuery, null);
		set(cursor);
	}

	public void set(Cursor cursor) {

		if (cursor != null && cursor.getCount() > 0) {
			if(cursor.isBeforeFirst() || cursor.isAfterLast())
				cursor.moveToFirst();

			long epochTime = cursor.getLong(cursor.getColumnIndexOrThrow(Timestamps.TIMESTAMP));

			boolean allDay = cursor.getLong(cursor.getColumnIndexOrThrow(Timestamps.ALL_DAY)) == 1;

			Calendar calendar = Calendar.getInstance();
			calendar.setTimeInMillis(epochTime * 1000L);
			year = calendar.get(Calendar.YEAR);
			monthOfYear = calendar.get(Calendar.MONTH);
			dayOfMonth = calendar.get(Calendar.DAY_OF_MONTH);
			if(!allDay){
				startMinute = calendar.get(Calendar.MINUTE);
				startTimeOfDay = calendar.get(Calendar.HOUR_OF_DAY);
			}
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

		Log.v("timemap", "map : "+type);
		Log.v("timemap", "line : "+line);
		if(patterns.get(type)==null) return;
		Matcher propm = patterns.get(type).matcher(line);
		if (propm.find()) {
			Log.v("timemap", "here");
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
		return String.format("%d-%02d-%02d", year, monthOfYear, dayOfMonth);
	}
	
	public String getStartTime() {
		if(startMinute < 0 || startTimeOfDay < 0) return "";
		return String.format("%02d:%02d", startTimeOfDay, startMinute);
	}
	
	public String getEndTime() {
		return String.format("%02d:%02d", endTimeOfDay, endMinute);
	}

	public long getEpochTime(){
		int hour = startTimeOfDay > -1 ? startTimeOfDay : 0;
		int minute = startMinute > -1 ? startMinute : 0;

		if(year == -1 || dayOfMonth == -1 || monthOfYear == -1) return -1;
		Log.v("time", "epochtime : "+new GregorianCalendar(year, monthOfYear, dayOfMonth, hour, minute).getTimeInMillis()/1000L);
		return new GregorianCalendar(year, monthOfYear, dayOfMonth, hour, minute).getTimeInMillis()/1000L;
	}

	/**
	 * Check if event is all day long
	 * An event is considered all day if startTimeOfDay or startMinute is undefined
	 * @return
     */
	public long isAllDay(){
		return (startTimeOfDay < 0 || startMinute < 0) ? 1 : 0;
	}
	
	public String toString() {
		return getDate() + getStartTimeFormated() + getEndTimeFormated();
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
		final String timestampPattern =  "<([^>]+)(\\d\\d:\\d\\d)>"; // + "(?:\\s*--\\s*<([^>]+)>)?"; for ranged date
//		final String timestampLookbehind = "\\s*(?<!(?:SCHEDULED:|DEADLINE:)\\s?)";

//		String pattern;
//		if(type == OrgNodeTimeDate.TYPE.Timestamp)
//			pattern = timestampLookbehind + "(" + timestampPattern + ")";
//		else

		String pattern = "\\s*(" + OrgNodeTimeDate.typeToFormated(type) + "\\s*" + timestampPattern + ")";

		return Pattern.compile(pattern);
	}


	public void update(Context context, long nodeId, long fileId) {
		Uri uri = OrgContract.Timestamps.buildIdUri(nodeId);
		context.getContentResolver().delete(uri, Timestamps.TYPE + "="+type.ordinal(), null);
		context.getContentResolver().
				insert(
						uri,
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

}
