package com.matburt.mobileorg.Parsing;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.text.TextUtils;
import android.text.format.DateUtils;
import android.util.Log;

public class NodePayload {
	private StringBuilder payload = new StringBuilder();
	/** These are the remains of the cleaned payload. */
	private StringBuilder payloadResidue = new StringBuilder();
	private StringBuilder newPayloadResidue = null;

	
	private String content = null;
	private String scheduled = null;
	private String deadline = null;
	private ArrayList<String> timestamps = new ArrayList<String>();
	
	private String id = null; // Can be :ID: (or :ORIGINAL_ID: for agendas.org)
	
	public NodePayload(String payload) {
		this.payload = new StringBuilder(payload);
	}
	
	public void add(String line) {
		this.payload.append(line + "\n");
	}
	
	public void setContent(String content) {
		this.content = content;
	}
	
	public String getContent() {
		if(this.content == null)
			cleanPayload();

		return this.content;
	}
	
	public String getPayloadResidue() {
		if(this.content == null)
			cleanPayload();

		return this.payloadResidue.toString();
	}
	
	public String getNewPayloadResidue() {
		if(this.newPayloadResidue == null)
			return this.payloadResidue.toString();
		else
			return this.newPayloadResidue.toString();
	}
		
	public String getId() {
		if(this.id == null)
			this.stripTags();
		
		return this.id;
	}
	
	private void cleanPayload() {
		this.scheduled = stripDate("SCHEDULED:");
		this.deadline = stripDate("DEADLINE:");
		stripTimestamps();

		stripTags();
		stripFileProperties();
		
		this.content = payload.toString().trim();
	}
	
	private void stripTimestamps() {
		String date = "";
		while((date = stripDate("")).equals("") == false) {
			this.timestamps.add(date); 
		}
	}
		
	private String stripDate(String scheduled) {		
		final Pattern scheduledLine = Pattern.compile(scheduled
				+ "\\s*<([^>]*)>(?:--<([^>]*)>)?");
		Matcher matcher = scheduledLine.matcher(payload.toString());
		
		String result = "";
		
		if(matcher.find()) {
			result = matcher.group(1);
			
			if(matcher.group(2) != null)
				result += matcher.group(2);
			
			payloadResidue.append(payload.substring(matcher.start(),
					matcher.end()) + "\n");
			payload.delete(matcher.start(), matcher.end());
		}	
		
		return result;
	}
	
	// TODO Convert to use pattern
	private void stripTags() {
		final Pattern propertiesLine = Pattern.compile(":[A-Za-z_]+:");
		Matcher propm = propertiesLine.matcher(this.payload);

		while(propm.find()) {
			String name = propm.group();
			
			int start = propm.start();
			int end;
			
			if(name.equals(":LOGBOOK:")) {
				end = payload.indexOf(":END:");
			} else
				end = payload.indexOf("\n", propm.end());

			if(end == -1)
				end = propm.end();
			else {
				String value = payload.substring(propm.end(), end);
				if(name.equals(":ID:") || name.equals(":ORIGINAL_ID:")) {
					this.id = value.trim();
				}
			}
			payloadResidue.append(payload.substring(start, end) + "\n");
			payload.delete(start, end);
			propm = propertiesLine.matcher(this.payload);
		}
	}
	
	// TODO Convert to use Pattern
	private void stripFileProperties() {
		while (true) {
			int start = payload.indexOf("#+");
			if (start == -1)
				break;
			
			int end = payload.indexOf("\n", start);
			if(end == -1)
				break;
			
			payloadResidue.append(payload.substring(start, end + 1) + "\n");
			payload.delete(start, end + 1);
		}
	}

	
	/**
	 * Returns a string containing the time at which a todo is scheduled or deadlined.
	 */
	public ArrayList<DateEntry> getDates() {
		ArrayList<DateEntry> result = new ArrayList<DateEntry>();
		
		if(this.scheduled == null) {
			this.scheduled = stripDate("SCHEDULED:");
			DateEntry scheduledEntry = getDateEntry(this.scheduled);
			if(scheduledEntry != null)
				result.add(scheduledEntry);
		}
		
		if(this.deadline == null) {
			this.deadline = stripDate("DEADLINE:");
			DateEntry deadlineEntry = getDateEntry(this.deadline);
			if(deadlineEntry != null)
				result.add(deadlineEntry);
		}
		
		stripTimestamps();
		for(String timestamp: this.timestamps) {
			DateEntry timestampEntry = getDateEntry(timestamp);
			if(timestampEntry != null)
				result.add(timestampEntry);
		}
		
		return result;
	}
	
	public class DateEntry {
		public long beginTime;
		public long endTime;
		public int allDay;
	}
	
	private DateEntry getDateEntry(String date)
			throws IllegalArgumentException {
		final Pattern schedulePattern = Pattern
				.compile("(\\d{4}-\\d{2}-\\d{2})(?:[^\\d]*)(\\d{1,2}\\:\\d{2})?\\-?(\\d{1,2}\\:\\d{2})?");
		
		Matcher propm = schedulePattern.matcher(date);
		DateEntry result = new DateEntry();

		if (propm.find()) {
			try {
				result.beginTime = getDateInMs(propm.group(1)).getTime();
				result.beginTime += TimeZone.getDefault().getOffset(result.beginTime);

				long beginTimeOfDay;
				if (propm.group(2) != null) { // has hh:mm entry
					beginTimeOfDay = getTimeInMs(propm.group(2)).getTime();
					result.beginTime += beginTimeOfDay;
					result.allDay = 0;

					if (propm.group(3) != null) { // has hh:mm-hh:mm entry
						result.endTime = result.beginTime
								+ getTimeInMs(propm.group(3)).getTime()
								- beginTimeOfDay;
					} else
						// event is one hour per default
						result.endTime = result.beginTime + DateUtils.HOUR_IN_MILLIS;
				} else { // event is an entire day event
					result.endTime = result.beginTime + DateUtils.DAY_IN_MILLIS;
					result.allDay = 1;
				}

				return result;
			} catch (ParseException e) {
				Log.w("MobileOrg",
						"Unable to parse schedule: " + date);
			}
		} else
			Log.w("MobileOrg", "Unable to find time entry of entry");
		return null;
	}
	
	private Date getDateInMs(String date) throws ParseException {
		SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
		return formatter.parse(date);
	}
	
	private Date getTimeInMs(String time) throws ParseException {
		SimpleDateFormat formatter = new SimpleDateFormat("HH:mm");
		return formatter.parse(time);
	}
	
	public String getScheduled() {
		if(this.scheduled == null)
			this.scheduled = stripDate("SCHEDULED:");
		
		return this.scheduled;
	}
	
	public String getDeadline() {
		if(this.deadline == null)
			this.deadline = stripDate("DEADLINE:");
		
		return this.deadline;
	}
	
	public void insertOrReplace(String key, String value) {
		if(newPayloadResidue == null)
			newPayloadResidue = new StringBuilder(payloadResidue);
		
		final Pattern schedulePattern = Pattern.compile(key + "\\s*<[^>]+>");
		Matcher matcher = schedulePattern.matcher(newPayloadResidue);

		if (matcher.find()) {
			if (TextUtils.isEmpty(value))
				newPayloadResidue.delete(matcher.start(), matcher.end());
			else
				newPayloadResidue.replace(matcher.start(), matcher.end(), value);
		}
		else if(TextUtils.isEmpty(value) == false)
			newPayloadResidue.insert(0, value).append("\n");
	}
}
