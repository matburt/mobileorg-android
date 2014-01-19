package com.matburt.mobileorg.OrgData;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.text.format.DateUtils;
import android.util.Log;

public class OrgNodeDate {

	public long beginTime = 0;
	public long endTime = 0;
	public int allDay = 0;
	public String type = "";
	private String title = "";

	private static final SimpleDateFormat dateTimeformatter = new SimpleDateFormat("yyyy-MM-dd HH:mm");
	private static final SimpleDateFormat dateformatter = new SimpleDateFormat("yyyy-MM-dd");
	
	private static final int DATE = 1;
	private static final int BEGIN_TIME = 2;
	private static final int END_TIME = 3;

	private static final String datePattern = "(\\d{1,2}\\:\\d{2})";  // d:dd or dd:dd
	private static final Pattern schedulePattern = Pattern
			.compile("(\\d{4}-\\d{2}-\\d{2})" // YYYY-MM-DD
					+ "(?:[^\\d]*)" // Strip out month
					+ datePattern + "?" // Begin time
					+ "(?:\\-" + datePattern + ")?"); // "-" followed by end time
	
	public OrgNodeDate (String date) throws IllegalArgumentException {
		Matcher schedule = schedulePattern.matcher(date);

		if (schedule.find()) {
			try {
				if(schedule.group(BEGIN_TIME) == null) { // event is an entire day event
					this.beginTime = dateformatter.parse(schedule.group(DATE)).getTime();
					
					this.endTime = this.beginTime + DateUtils.DAY_IN_MILLIS;
					this.allDay = 1;
				}
				else if (schedule.group(BEGIN_TIME) != null && schedule.group(END_TIME) != null) {
					this.beginTime = dateTimeformatter.parse(schedule.group(DATE) + " " + schedule.group(BEGIN_TIME)).getTime();
					this.endTime = dateTimeformatter.parse(schedule.group(DATE) + " " + schedule.group(END_TIME)).getTime();
					this.allDay = 0;
				} else if(schedule.group(BEGIN_TIME) != null) {
					this.beginTime = dateTimeformatter.parse(schedule.group(DATE) + " " + schedule.group(BEGIN_TIME)).getTime();
					this.endTime = beginTime + DateUtils.HOUR_IN_MILLIS;
					this.allDay = 0;
				}

				return;
			} catch (ParseException e) {
				Log.w("MobileOrg", "Unable to parse schedule: " + date);
			}
		} else
			throw new IllegalArgumentException("Could not create date out of entry");
	}
	
	public static String getDate(long dtStart, long dtEnd, boolean allDay) {
		String date;
		
		if (allDay)
			date = dateformatter.format(new Date(dtStart));
		else
			date = dateTimeformatter.format(new Date(dtStart));
		
		if (dtEnd > 0 && dtStart != dtEnd) {
			long timeDiff = dtEnd - dtStart;
			
			if(timeDiff <= DateUtils.DAY_IN_MILLIS) {
				SimpleDateFormat timeformatter = new SimpleDateFormat("HH:mm");
				String endTime = timeformatter.format(new Date(dtEnd));
				
				date += "-" + endTime;
			}	
		}
		
		return date;
	}
	
	/**
	 * Whether an event is in the past. True if event ended 24 hours ago or
	 * sometime in the future.
	 */
	public boolean isInPast() {
		return System.currentTimeMillis() - DateUtils.DAY_IN_MILLIS >= endTime;
	}
	
	public void setTitle(String title) {
		this.title = title;
	}
	
	public String getTitle() {
		return this.type + this.title;
	}
}
