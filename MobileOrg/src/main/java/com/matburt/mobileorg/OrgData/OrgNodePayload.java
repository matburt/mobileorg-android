package com.matburt.mobileorg.OrgData;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
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

    public HashMap getPropertiesPayload()
    {
        
        HashMap propsHashMap = new HashMap();
        
        if (this.payload != null) {
            
            Pattern propsRegex = Pattern.compile(":PROPERTIES:\\s*\n(.*):END:", Pattern.DOTALL);
            Pattern propertyRegex = Pattern.compile("^\\s*:([^:]+):\\s*(.+)$", Pattern.MULTILINE);
            Matcher propsMatcher = propsRegex.matcher(this.payload);
            
            if (propsMatcher.find()) {
                
                String properties = propsMatcher.group(1);
                Matcher propertyMatcher = propertyRegex.matcher(properties);
                
                while (propertyMatcher.find()) {
                    String property = propertyMatcher.group(1);
                    String value = propertyMatcher.group(2);
                    propsHashMap.put(property, value);
                }
                
            }
            
        }

        if (!propsHashMap.isEmpty()) {
            return propsHashMap;
        }
        
        return null;
        
    }
	
	private void prepareCleanedPayload() {
		if(this.cleanPayload == null)
			this.cleanPayload = new StringBuilder(this.payload);
	}
	
	private void cleanPayload() {
		this.scheduled = getScheduled();
		this.deadline = getDeadline();
		this.timestamp = getTimestamp();

		stripProperties();
		stripFileProperties();
	}
	
	public String getScheduled() {
		if(this.scheduled == null)
			this.scheduled = stripDate(OrgNodeTimeDate.TYPE.Scheduled);
		
		return this.scheduled;
	}
	
	public String getDeadline() {
		if(this.deadline == null)
			this.deadline = stripDate(OrgNodeTimeDate.TYPE.Deadline);
		
		return this.deadline;
	}
	
	public String getTimestamp() {
		if(this.timestamp == null)
			this.timestamp = stripDate(OrgNodeTimeDate.TYPE.Timestamp);
		
		return this.timestamp;
	}
	
	
	private Pattern getTimestampMatcher(OrgNodeTimeDate.TYPE type) {
		final String timestampPattern =  "<([^>]+)>" + "(?:\\s*--\\s*<([^>]+)>)?";
		final String timestampLookbehind = "(?<!(?:SCHEDULED:|DEADLINE:)\\s?)";
		
		String pattern;
		if(type == OrgNodeTimeDate.TYPE.Timestamp)
			pattern = timestampLookbehind + "(" + timestampPattern + ")";
		else
			pattern = "(" + OrgNodeTimeDate.typeToFormated(type) + "\\s*" + timestampPattern + ")";
		
		return Pattern.compile(pattern);
	}
	
	private String stripDate(OrgNodeTimeDate.TYPE type) {		
		prepareCleanedPayload();

		Matcher matcher = getTimestampMatcher(type).matcher(
				cleanPayload.toString());
		
		String result = "";
		
		if(matcher.find()) {
			result = matcher.group(2);

			if(matcher.group(3) != null)
				result += matcher.group(3);
			
			cleanPayload.delete(matcher.start(), matcher.end());
		}
		
		return result;
	}
	
	public void insertOrReplaceDate(OrgNodeTimeDate.TYPE type, String date) {
		Matcher matcher = getTimestampMatcher(type).matcher(payload);
		
		String formatedDate = OrgNodeTimeDate.formatDate(type, date);
		
		if (matcher.find()) {
			if (TextUtils.isEmpty(date)) // Date was set to empty
				payload.delete(matcher.start(), matcher.end());
			else // Replace existing date
				payload.replace(matcher.start(1), matcher.end(), formatedDate);
		}
		else if(TextUtils.isEmpty(date) == false) // Insert new date
			payload.insert(0, formatedDate + "\n");
		
		resetCachedValues();
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
	
	
	public ArrayList<OrgNodeDate> getDates(String title) {
		ArrayList<OrgNodeDate> result = new ArrayList<OrgNodeDate>();

		try {
			OrgNodeDate scheduledEntry = new OrgNodeDate(getScheduled());
			scheduledEntry.type = "SC: ";
			scheduledEntry.setTitle(title);
			result.add(scheduledEntry);
		} catch (IllegalArgumentException e) {}
		
		try {
			OrgNodeDate deadlineEntry = new OrgNodeDate(getDeadline());
			deadlineEntry.type = "DL: ";
			deadlineEntry.setTitle(title);
			result.add(deadlineEntry);
		} catch (IllegalArgumentException e) {}

		try {
			OrgNodeDate timestampEntry = new OrgNodeDate(getTimestamp());
			timestampEntry.type = "";
			timestampEntry.setTitle(title);
			result.add(timestampEntry);
		} catch (IllegalArgumentException e) {}
		
		return result;
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
