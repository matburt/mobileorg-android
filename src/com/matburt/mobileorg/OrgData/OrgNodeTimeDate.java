package com.matburt.mobileorg.OrgData;

import java.util.Calendar;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class OrgNodeTimeDate {

	public int year;
	public int monthOfYear;
	public int dayOfMonth;
	public int startTimeOfDay = -1;
	public int startMinute = -1;
	public int endTimeOfDay = -1;
	public int endMinute = -1;

	public OrgNodeTimeDate() {
		final Calendar c = Calendar.getInstance();
		this.year = c.get(Calendar.YEAR);
		this.monthOfYear = c.get(Calendar.MONTH) + 1;
		this.dayOfMonth = c.get(Calendar.DAY_OF_MONTH);
	}
	
	public void parseDate(String date) {
		final Pattern schedulePattern = Pattern
				.compile("((\\d{4})-(\\d{1,2})-(\\d{1,2}))(?:[^\\d]*)"
						+ "((\\d{1,2})\\:(\\d{2}))?(-((\\d{1,2})\\:(\\d{2})))?");

		Matcher propm = schedulePattern.matcher(date);

		if (propm.find()) {
			OrgNodeTimeDate timeDate = new OrgNodeTimeDate();

			try {
				timeDate.year = Integer.parseInt(propm.group(2));
				timeDate.monthOfYear = Integer.parseInt(propm.group(3));
				timeDate.dayOfMonth = Integer.parseInt(propm.group(4));

				timeDate.startTimeOfDay = Integer.parseInt(propm.group(6));
				timeDate.startMinute = Integer.parseInt(propm.group(7));

				timeDate.endTimeOfDay = Integer.parseInt(propm.group(10));
				timeDate.endMinute = Integer.parseInt(propm.group(11));
			} catch (NumberFormatException e) {}
		} else
			throw new IllegalArgumentException("Could not parse date: " + date);
	}
}
