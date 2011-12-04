package com.matburt.mobileorg.Parsing;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class NodePayload {

	private StringBuilder payload = new StringBuilder();
	
	private Date schedule = null;
	private Date deadline = null;
	
	private HashMap<String, String> properties = new HashMap<String, String>();
	private String nodeId = "";
	
	public void add(String line) {
		this.payload.append(line + "\n");
	}
	
	public void set(String payload) {
		this.payload = new StringBuilder(payload);
	}
	
	public String getContent() {
		return this.payload.toString();
	}
	
	
	// This function does nothing yet, It's a reminder of how to find properties
//	private void findProperties() {
//		Pattern propertiesLine = Pattern.compile("^\\s*:[A-Z]+:");
//		Matcher propm = propertiesLine.matcher(this.payload);
//		
//		propm.find();
//	}

	
	// TODO Make more efficient
	public Date getDeadline() {
		//payload.indexOf("DEADLINE:");
		
		Pattern deadlineP = Pattern
				.compile("^.*DEADLINE: <(\\S+ \\S+)( \\S+)?>");

		try {
			Matcher deadlineM = deadlineP.matcher(this.payload);
			SimpleDateFormat dFormatter = new SimpleDateFormat("yyyy-MM-dd EEE");
			if (deadlineM.find()) {
				this.deadline = dFormatter.parse(deadlineM.group(1));
			}

		} catch (java.text.ParseException e) {
			// Log.e(LT, "Could not parse deadline");
		}

		return this.deadline;
	}
	
	// TODO Make more efficient
	public Date getScheduled() {
		//this.payload.indexOf("SCHEDULED:");
		
		Pattern schedP = Pattern.compile("^.*SCHEDULED: <(\\S+ \\S+)( \\S+)?>");

		SimpleDateFormat sFormatter = new SimpleDateFormat("yyyy-MM-dd EEE");
		Matcher schedM = schedP.matcher(this.payload);
		if (schedM.find()) {
			try {
				this.schedule = sFormatter.parse(schedM.group(1));
			} catch (ParseException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		return this.schedule;
	}

	public String formatDate() {
		String dateInfo = "";

		// Format Deadline and scheduled
		SimpleDateFormat formatter = new SimpleDateFormat("<yyyy-MM-dd EEE>");
		if (this.deadline != null)
			dateInfo += "DEADLINE: " + formatter.format(this.deadline) + " ";

		if (this.schedule != null)
			dateInfo += "SCHEDULED: " + formatter.format(this.schedule) + " ";

		return dateInfo;
	}
	
	
	public String getOriginalId() {
		if (payload.indexOf(":ORIGINAL_ID:") != -1) {
			String trimmedLine = payload.substring(
					payload.indexOf(":ORIGINAL_ID:") + 13).trim();
			this.addProperty("ORIGINAL_ID", trimmedLine);
			this.setNodeId(trimmedLine);
			return trimmedLine;
		} else
			return "";
	}

	public String getId() {
		if (payload.indexOf(":ID:") != -1) {
			String trimmedLine = payload.substring(
					payload.indexOf(":ID:") + 4).trim();
			this.addProperty("ID", trimmedLine);
			this.setNodeId(trimmedLine);
			return trimmedLine;
		} else
			return "";
	}
	
	public String getNodeId() {
		return this.nodeId;
	}
	
	public void setNodeId(String nodeId) {
		this.nodeId = nodeId;
	}
	
	void addProperty(String key, String val) {
		this.properties.put(key, val);
	}
}
