package com.matburt.mobileorg.OrgData;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.text.TextUtils;
import android.text.format.DateUtils;
import android.text.format.Time;
import android.util.Log;

public class OrgNodePayload {
	private StringBuilder payload = new StringBuilder();
	/** These are the remains of the cleaned payload. */
	private StringBuilder payloadResidue = new StringBuilder();
	private StringBuilder newPayloadResidue = null;

	
	private String content = null;
	private String scheduled = null;
	private String deadline = null;
	private ArrayList<String> timestamps = new ArrayList<String>();
	
	private String id = null; // Can be :ID: (or :ORIGINAL_ID: for agendas.org)
	
	public OrgNodePayload(String payload) {
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
			this.stripProperties();
		
		return this.id;
	}
	
	public String getProperty(String property) {
		String residue = getPayloadResidue();
		final Pattern propertiesLine = Pattern.compile(":"+property+":([^\\n]+)");
		Matcher propm = propertiesLine.matcher(residue);
		
		if(propm.find())
			return propm.group(1).trim();
		else
			return "";
	}
	
	private void cleanPayload() {
		this.scheduled = stripDate("SCHEDULED:");
		this.deadline = stripDate("DEADLINE:");
		stripTimestamps();

		stripProperties();
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
	
	private void stripProperties() {
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
			if(scheduledEntry != null) {
				scheduledEntry.type = "SC: ";
				result.add(scheduledEntry);
			}
		}
		
		if(this.deadline == null) {
			this.deadline = stripDate("DEADLINE:");
			DateEntry deadlineEntry = getDateEntry(this.deadline);
			if(deadlineEntry != null) {
				deadlineEntry.type = "DL: ";
				result.add(deadlineEntry);
			}
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
		public String type = "";
	}
	
	private DateEntry getDateEntry(String date)
			throws IllegalArgumentException {
		final Pattern schedulePattern = Pattern
				.compile("(\\d{4}-\\d{2}-\\d{2})(?:[^\\d]*)(\\d{1,2}\\:\\d{2})?\\-?(\\d{1,2}\\:\\d{2})?");
		
		Matcher propm = schedulePattern.matcher(date);
		DateEntry result = new DateEntry();

		if (propm.find()) {
			try {
				if(propm.group(2) == null) { // event is an entire day event
					result.beginTime = getTimeInMs(propm.group(1), "00:00").getTime();
					result.endTime = result.beginTime + DateUtils.DAY_IN_MILLIS;
					result.allDay = 1;
				}
				else if (propm.group(2) != null) { // has hh:mm entry
					result.beginTime = getTimeInMs(propm.group(1), propm.group(2)).getTime();
					result.allDay = 0;

					if (propm.group(3) != null) { // has hh:mm-hh:mm entry
						result.endTime = getTimeInMs(propm.group(1), propm.group(3)).getTime();
					} else // event is one hour per default
						result.endTime = result.beginTime + DateUtils.HOUR_IN_MILLIS;
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
	
	private Date getTimeInMs(String date, String time) throws ParseException {
		SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm");
		return formatter.parse(date + " " + time);
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
	
	public long sumClocks() {
		// TODO implement
		return 0;
	}
	
	private static String formatClockEntry(long time) {
		SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd EEE HH:mm");
		return "[" + formatter.format(new Date(time)) + "]";
	}

	public static StringBuilder addLogbook(StringBuilder payload, long startTime, long endTime, String elapsedTime) {
		// TODO Add => total to end
		String line = "CLOCK: " + formatClockEntry(startTime) + "--"
				+ formatClockEntry(endTime) + " =>  " + elapsedTime;
		
		int logbookIndex = payload.indexOf(":LOGBOOK:");
		if(logbookIndex == -1)
			payload.insert(0, ":LOGBOOK:\n" + line + "\n:END:\n");
		else
			payload.insert(logbookIndex + ":LOGBOOK:".length(), "\n" + line);
		return payload;
	}
	
}

