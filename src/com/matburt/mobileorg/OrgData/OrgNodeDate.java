package com.matburt.mobileorg.OrgData;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.text.format.DateUtils;
import android.util.Log;

public class OrgNodeDate {

	public long beginTime;
	public long endTime;
	public int allDay;
	public String type = "";
	
	public OrgNodeDate (String date)
			throws IllegalArgumentException {
		final Pattern schedulePattern = Pattern
				.compile("(\\d{4}-\\d{2}-\\d{2})(?:[^\\d]*)(\\d{1,2}\\:\\d{2})?\\-?(\\d{1,2}\\:\\d{2})?");
		
		Matcher propm = schedulePattern.matcher(date);

		if (propm.find()) {
			try {
				if(propm.group(2) == null) { // event is an entire day event
					beginTime = getTimeInMs(propm.group(1), "00:00").getTime();
					endTime = beginTime + DateUtils.DAY_IN_MILLIS;
					allDay = 1;
				}
				else if (propm.group(2) != null) { // has hh:mm entry
					beginTime = getTimeInMs(propm.group(1), propm.group(2)).getTime();
					allDay = 0;

					if (propm.group(3) != null) { // has hh:mm-hh:mm entry
						endTime = getTimeInMs(propm.group(1), propm.group(3)).getTime();
					} else // event is one hour per default
						endTime = beginTime + DateUtils.HOUR_IN_MILLIS;
				}

				return;
			} catch (ParseException e) {
				Log.w("MobileOrg",
						"Unable to parse schedule: " + date);
			}
		} else
			throw new IllegalArgumentException("Could not create date out of entry");
	}
	
	private Date getTimeInMs(String date, String time) throws ParseException {
		SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm");
		return formatter.parse(date + " " + time);
	}

}
