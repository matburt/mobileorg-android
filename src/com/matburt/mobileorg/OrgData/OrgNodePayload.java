package com.matburt.mobileorg.OrgData;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.text.TextUtils;

public class OrgNodePayload {
	private StringBuilder payload = new StringBuilder();
	
	/** This is a "cache" for the cleaned payload. */
	private StringBuilder cleanPayload = null;
	
	/** Can be :ID: or :ORIGINAL_ID: (for nodes agendas.org) */
	private String id = null;

	private String scheduled = null;
	private String deadline = null;
	private String timestamp = null;
	
	public OrgNodePayload(String payload) {
		if(payload == null)
			payload = "";
		set(payload);
	}
	
	public void set(String payload) {
		this.payload = new StringBuilder(payload);
		resetCachedValues();
	}
	
	private void resetCachedValues() {
		this.cleanPayload = null;
		this.scheduled = null;
		this.deadline = null;
		this.timestamp = null;
		this.id = null;
	}
	
	public String get() {
		return this.payload.toString();
	}
	
	public void add(String line) {
		set(this.payload.toString() + "\n" + line + "\n");
		this.cleanPayload = null;
	}
	
	public String getCleanedPayload() {
		if(this.cleanPayload == null)
			cleanPayload();

		return this.cleanPayload.toString().trim();
	}
	
	public String getId() {
		if(this.id == null)
			stripProperties();
			//this.id = getProperty("ID");
		
		return this.id;
	}
	
	
	private void prepareCleanedPayload() {
		if(this.cleanPayload == null)
			this.cleanPayload = new StringBuilder(this.payload);
	}
	
	private void cleanPayload() {
		this.scheduled = stripDate("SCHEDULED:");
		this.deadline = stripDate("DEADLINE:");
		this.timestamp = stripDate("");

		stripProperties();
		stripFileProperties();
	}
		
	private String stripDate(String scheduled) {		
		prepareCleanedPayload();
		
		final Pattern scheduledLine = Pattern.compile(scheduled
				+ "\\s*<([^>]*)>(?:--<([^>]*)>)?");
		Matcher matcher = scheduledLine.matcher(cleanPayload.toString());
		
		String result = "";
		
		if(matcher.find()) {
			result = matcher.group(1);
			
			if(matcher.group(2) != null)
				result += matcher.group(2);
			
			cleanPayload.delete(matcher.start(), matcher.end());
		}	
		
		return result;
	}
	
	private ArrayList<String> stripProperties() {
		prepareCleanedPayload();
		ArrayList<String> properties = new ArrayList<String>();
		final Pattern propertiesLine = Pattern.compile(":[A-Za-z_]+:");
		Matcher propm = propertiesLine.matcher(this.cleanPayload);

		while(propm.find()) {
			String name = propm.group();
			
			int start = propm.start();
			int end;
			
			if(name.equals(":LOGBOOK:")) {
				end = cleanPayload.indexOf(":END:");
			} else
				end = cleanPayload.indexOf("\n", propm.end());

			if(end == -1)
				end = propm.end();
			else {
				String value = cleanPayload.substring(propm.end(), end);
				if(name.equals(":ID:") || name.equals(":ORIGINAL_ID:")) {
					this.id = value.trim();
				}
			}
			properties.add(cleanPayload.substring(start, end) + "\n");
			cleanPayload.delete(start, end);
			propm = propertiesLine.matcher(this.cleanPayload);
		}
		
		return properties;
	}
	
	private ArrayList<String> stripFileProperties() {
		prepareCleanedPayload();
		ArrayList<String> fileProperties = new ArrayList<String>();
		while (true) {
			int start = cleanPayload.indexOf("#+");
			if (start == -1)
				break;
			
			int end = cleanPayload.indexOf("\n", start);
			if(end == -1)
				break;
			
			fileProperties.add(cleanPayload.substring(start, end + 1) + "\n");
			cleanPayload.delete(start, end + 1);
		}
		
		return fileProperties;
	}

	
	public String getProperty(String property) {
		final Pattern propertiesLine = Pattern.compile(":"+property+":([^\\n]+)");
		Matcher propm = propertiesLine.matcher(this.payload);
		
		if(propm.find())
			return propm.group(1).trim();
		else
			return "";
	}
	
	public ArrayList<OrgNodeDate> getDates() {
		ArrayList<OrgNodeDate> result = new ArrayList<OrgNodeDate>();
		
		if (this.scheduled == null)
			this.scheduled = stripDate("SCHEDULED:");

		try {
			OrgNodeDate scheduledEntry = new OrgNodeDate(this.scheduled);
			scheduledEntry.type = "SC: ";
			result.add(scheduledEntry);
		} catch (IllegalArgumentException e) {}
		
		if (this.deadline == null)
			this.deadline = stripDate("DEADLINE:");
		
		try {
			OrgNodeDate deadlineEntry = new OrgNodeDate(this.deadline);
			deadlineEntry.type = "DL: ";
			result.add(deadlineEntry);
		} catch (IllegalArgumentException e) {}
		
		if (this.timestamp == null)
			this.timestamp = stripDate("");

		try {
			OrgNodeDate timestampEntry = new OrgNodeDate(this.timestamp);
			timestampEntry.type = "";
			result.add(timestampEntry);
		} catch (IllegalArgumentException e) {}
		
		return result;
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
	
	public String getTimestamp() {
		if(this.timestamp == null)
			this.timestamp = stripDate("");
		
		return this.timestamp;
	}
	
	public void modifyDates(String scheduled, String deadline, String timestamp) {
		insertOrReplaceDate("SCHEDULED", scheduled);
		insertOrReplaceDate("DEADLINE", deadline);
		//insertOrReplaceDate("", timestamp);
		resetCachedValues();
	}
	
	// TODO Fix
	public void insertOrReplaceDate(String dateType, String date) {
		final Pattern schedulePattern = Pattern.compile(dateType + "\\s*<[^>]+>");
		Matcher matcher = schedulePattern.matcher(payload);

		if (matcher.find()) {
			if (TextUtils.isEmpty(date))
				payload.delete(matcher.start(), matcher.end());
			else
				payload.replace(matcher.start(), matcher.end(), date);
		}
		else if(TextUtils.isEmpty(date) == false)
			payload.insert(0, date).append("\n");
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

