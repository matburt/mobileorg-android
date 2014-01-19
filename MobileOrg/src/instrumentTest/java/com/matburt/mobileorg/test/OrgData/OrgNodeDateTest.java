package com.matburt.mobileorg.test.OrgData;

import java.util.Calendar;

import com.matburt.mobileorg.OrgData.OrgNodeDate;

import android.test.AndroidTestCase;

public class OrgNodeDateTest extends AndroidTestCase {

	private static final String dateString = "2000-11-24";
	private static final String timeBeginString = "13:15";
	private static final String timeEndString = "15:15";
	private static final String timeMidnight = "00:00";
	private Calendar getDefaultCalendar() {
		Calendar cal = Calendar.getInstance();
		cal.set(Calendar.YEAR, 2000);
		cal.set(Calendar.MONTH, 10);
		cal.set(Calendar.DAY_OF_MONTH, 24);
		cal.set(Calendar.HOUR_OF_DAY, 13);
		cal.set(Calendar.MINUTE, 15);
		cal.set(Calendar.SECOND, 0);
		cal.set(Calendar.MILLISECOND, 0);
		return cal;
	}
	
	public void testGetDateDate() {
		final long timeInMillis = getDefaultCalendar().getTimeInMillis();
		
		String date = OrgNodeDate.getDate(timeInMillis, 0, true);
		
		assertEquals(dateString, date);
	}
	
	public void testGetDateDateTime() {
		final long timeInMillis = getDefaultCalendar().getTimeInMillis();
		
		String date = OrgNodeDate.getDate(timeInMillis, timeInMillis, false);
		
		assertEquals(dateString + " " + timeBeginString, date);
	}
	
	public void testGetDateTimeSpan() {
		Calendar cal = getDefaultCalendar();
		final long startTimeInMillis = cal.getTimeInMillis();		
		cal.set(Calendar.HOUR_OF_DAY, 15);
		final long endTimeInMillis = cal.getTimeInMillis();

		String date = OrgNodeDate.getDate(startTimeInMillis, endTimeInMillis, false);
		
		assertEquals(dateString + " " + timeBeginString + "-" + timeEndString, date);
	}

	/**
	 * Tests that the start time of dates with a begin time are equal to
	 * the time in milliseconds of an equivalent Calendar.
	 */
	public void testDateEqualsCalendar() {
		OrgNodeDate date = new OrgNodeDate(dateString + " " + timeBeginString);
		long calTime = getDefaultCalendar().getTimeInMillis();

		assertEquals(date.beginTime, calTime);
	}

	/**
	 * Tests that the start time of dates without a time part (all-day
	 * events) are equal to the start time of dates that start at
	 * midnight.
	 */
	public void testAllDayEqualsMidnight() {
		OrgNodeDate allDay = new OrgNodeDate(dateString);
		OrgNodeDate midnight = new OrgNodeDate(dateString + " " + timeMidnight);

		assertEquals(allDay.beginTime, midnight.beginTime);
	}
}
