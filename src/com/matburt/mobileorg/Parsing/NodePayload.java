package com.matburt.mobileorg.Parsing;

import java.text.SimpleDateFormat;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

class NodePayload {

	private StringBuilder payload = new StringBuilder();
	/** These are the remains of the cleaned payload. */
	private StringBuilder payloadResidue = new StringBuilder();
	
	private String content = null;
	private String scheduled = null;
	private String deadline = null;
	
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
			this.content = cleanPayload();

		return this.content;
	}
	
	public String getPayloadResidue() {
		if(this.content == null)
			cleanPayload();
			
		return this.payloadResidue.toString();
	}
		
	public String getId() {
		if(this.id == null)
			this.stripTags();
		
		return this.id;
	}
	
	private String cleanPayload() {
		this.scheduled = stripDate("SCHEDULED:");
		this.deadline = stripDate("DEADLINE:");

		stripTags();
		stripFileProperties();
		
		return payload.toString().trim();
	}
	
	private String stripDate(String scheduled) {
		int index = payload.indexOf(scheduled);
		
		if(index == -1)
			return "";
		
		int start = payload.indexOf("<", index);
		int end = payload.indexOf(">", start);
		
		String date = payload.substring(start + 1, end);
		
		payloadResidue.append(payload.substring(index, end + 1) + "\n");
		payload.delete(index, end + 1);
		
		return date;
	}
	
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
	 * Returns a string containing the time at which a todo is scheduled.
	 */
	public String getTime() {
		String time = "            ";
		
		if(this.scheduled == null)
			this.scheduled = stripDate("SCHEDULED:");
		
		int start = this.scheduled.indexOf(":");
		
		if(start != -1)
			time = this.scheduled.substring(start-2, start+3) + " ";
		
		return time;
	}

	public String datesToString() {
		String dateInfo = "";

		try{
		SimpleDateFormat formatter = new SimpleDateFormat("<yyyy-MM-dd EEE>");
		if (this.deadline != null && this.deadline.length() > 0)
			dateInfo += "DEADLINE: " + formatter.format(this.deadline) + " ";

		if (this.scheduled != null && this.scheduled.length() > 0)
			dateInfo += "SCHEDULED: " + formatter.format(this.scheduled) + " ";
		} catch(IllegalArgumentException e) { dateInfo = "";}
		
		return dateInfo;
	}
}
