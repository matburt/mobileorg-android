package com.matburt.mobileorg.Parsing;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.text.TextUtils;

public class NodePayload {
	private StringBuilder payload = new StringBuilder();
	/** These are the remains of the cleaned payload. */
	private StringBuilder payloadResidue = new StringBuilder();
	private StringBuilder newPayloadResidue = null;

	
	private String content = null;
	private String scheduled = null;
	
	private String deadline = null;
	@SuppressWarnings("unused")
	private String timestamp = null;
	
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
	
	private String cleanPayload() {
		this.scheduled = stripDate("SCHEDULED:");
		this.deadline = stripDate("DEADLINE:");
		this.timestamp = stripDate("");

		stripTags();
		stripFileProperties();
		
		return payload.toString().trim();
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
	public String getDate() {		
		if(this.scheduled == null)
			this.scheduled = stripDate("SCHEDULED:");
		
		if(TextUtils.isEmpty(this.scheduled) && this.deadline == null) {
			this.deadline = stripDate("DEADLINE:");
			return this.deadline;
		}
		
		return this.scheduled;
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
