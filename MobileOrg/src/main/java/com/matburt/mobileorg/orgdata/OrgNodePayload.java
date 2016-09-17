package com.matburt.mobileorg.orgdata;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class OrgNodePayload {
	private StringBuilder payload = new StringBuilder();
	
	/** This is a "cache" for the cleaned payload. */
	private StringBuilder cleanPayload = null;
	
	/** Can be :ID: or :ORIGINAL_ID: (for nodes agendas.org) */
	private String id = null;


	private HashMap<OrgNodeTimeDate.TYPE, String> cachedTimestamps;

	public OrgNodePayload(String payload) {
		if(payload == null)
			payload = "";

		this.cachedTimestamps = new HashMap<>();
        this.cleanPayload = new StringBuilder(payload);
        set(payload);
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
		if (logbookIndex == -1)
			payload.insert(0, ":LOGBOOK:\n" + line + "\n:END:\n");
		else
			payload.insert(logbookIndex + ":LOGBOOK:".length(), "\n" + line);
		return payload;
	}
	
	public void set(String payload) {
		this.payload = new StringBuilder(payload);
		resetCachedValues();
	}
	
	private void resetCachedValues() {
		this.cleanPayload.setLength(0);
		this.id = null;
		this.cachedTimestamps.clear();
	}
	
	public String get() {
		return this.payload.toString();
	}

	public void add(String line) {
		set(this.payload.toString() + "\n" + line + "\n");
		this.cleanPayload = null;
	}
	
	public String getCleanedPayload() {
		cleanPayload();
		return this.cleanPayload.toString();
	}

	private void trimEachLine() {
		String cleanPayload = this.cleanPayload.toString();

		String lines[] = cleanPayload.split("\\n");
		int N = lines.length;
		if (N == 0){
			this.cleanPayload = new StringBuilder("");
			return;
		}

		String result = lines[0].trim();
		for (int i = 1; i < N; ++i) {
			String trimedLine = lines[i].trim();
			if (trimedLine.length() > 0) result += "\n" + trimedLine;
		}
		this.cleanPayload = new StringBuilder(result);
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

	private void cleanPayload() {
		cleanPayload = payload;
		for(OrgNodeTimeDate.TYPE type: OrgNodeTimeDate.TYPE.values())
			getTimestamp(type);

		stripProperties();
		stripFileProperties();
		trimEachLine();
	}

	public String getTimestamp(OrgNodeTimeDate.TYPE type) {
		String result = cachedTimestamps.get(type);
		if(result != null) return result; // return cached result if any

		OrgNodeTimeDate timeDate = new OrgNodeTimeDate(type, cleanPayload.toString());
		if(timeDate.matchStart > -1) cleanPayload.delete(timeDate.matchStart, timeDate.matchEnd);
		cachedTimestamps.put(type, timeDate.toString());

		return cachedTimestamps.get(type);
	}

	public void insertOrReplaceDate(OrgNodeTimeDate date) {
		Matcher matcher = OrgNodeTimeDate.getTimestampMatcher(date.type).matcher(payload);

		String formatedDate = date.toFormatedString();

		if (matcher.find()) {
//			if (TextUtils.isEmpty(date)) // Date was set to empty
//				payload.delete(matcher.start(), matcher.end());
//			else // Replace existing date
			payload.replace(matcher.start(1), matcher.end(), formatedDate);
		}
//		else if(TextUtils.isEmpty(date) == false) // Insert new date
		else payload.insert(0, formatedDate + "\n");

		resetCachedValues();
	}

	private ArrayList<String> stripProperties() {
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
			cleanPayload.delete(start, end+1);
			propm = propertiesLine.matcher(this.cleanPayload);
		}

		return properties;
	}
	
	private ArrayList<String> stripFileProperties() {
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

        for(OrgNodeTimeDate.TYPE type: OrgNodeTimeDate.TYPE.values()){
            try {
                OrgNodeDate scheduledEntry = new OrgNodeDate(getTimestamp(type));
                scheduledEntry.type = type;
                scheduledEntry.setTitle(title);
                result.add(scheduledEntry);
            } catch (IllegalArgumentException e) {}
        }

		return result;
	}

	public long sumClocks() {
		// TODO implement
		return 0;
	}
}
