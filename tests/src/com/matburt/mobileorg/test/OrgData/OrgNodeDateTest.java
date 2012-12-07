package com.matburt.mobileorg.test.OrgData;

import java.util.Calendar;

import com.matburt.mobileorg.OrgData.OrgNodeDate;

import android.test.AndroidTestCase;

public class OrgNodeDateTest extends AndroidTestCase {

	private static final String dateString = "2000-11-24";
	private static final String timeBeginString = "13:15";
	private static final String timeEndString = "15:15";
	private Calendar getDefaultCalendar() {
		Calendar cal = Calendar.getInstance();
		cal.set(Calendar.YEAR, 2000);
		cal.set(Calendar.MONTH, 10);
		cal.set(Calendar.DAY_OF_MONTH, 24);
		cal.set(Calendar.HOUR_OF_DAY, 13);
		cal.set(Calendar.MINUTE, 15);
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

}
