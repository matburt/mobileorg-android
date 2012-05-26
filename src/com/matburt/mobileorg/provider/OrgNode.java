package com.matburt.mobileorg.provider;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.database.Cursor;
import android.text.TextUtils;
import android.util.Log;

import com.matburt.mobileorg.provider.OrgContract.OrgData;

public class OrgNode {

	public long id = -1;
	public long parentId = -1;
	public long fileId = -1;
	public long level = 0;
	public String priority = "";
	public String todo = "";
	public String tags = "";
	public String name = "";
	public String payload = "";

	public OrgNode() {
	}

	public OrgNode(Cursor cursor) {
		set(cursor);
	}
	
	public void set(Cursor cursor) {
		if (cursor != null && cursor.moveToFirst()) {
			id = cursor.getLong(cursor.getColumnIndexOrThrow("_id"));
			parentId = cursor.getLong(cursor
					.getColumnIndexOrThrow(OrgData.PARENT_ID));
			fileId = cursor.getLong(cursor
					.getColumnIndexOrThrow(OrgData.FILE_ID));
			level = cursor.getLong(cursor.getColumnIndexOrThrow(OrgData.LEVEL));
			priority = cursor.getString(cursor
					.getColumnIndexOrThrow(OrgData.PRIORITY));
			todo = cursor.getString(cursor.getColumnIndexOrThrow(OrgData.TODO));
			tags = cursor.getString(cursor.getColumnIndexOrThrow(OrgData.TAGS));
			name = cursor.getString(cursor.getColumnIndexOrThrow(OrgData.NAME));
			payload = cursor.getString(cursor
					.getColumnIndexOrThrow(OrgData.PAYLOAD));
		} else {
			throw new IllegalArgumentException(
					"Failed to create OrgNode from cursor");
		}	
	}
	
	public void parseLine(String thisLine, int numstars, boolean useTitleField) {
        String heading = thisLine.substring(numstars+1);
        
    	Matcher matcher = titlePattern.matcher(heading);
		if (matcher.find()) {
			if (matcher.group(TODO_GROUP) != null) {
				String tempTodo = matcher.group(TODO_GROUP).trim();
				// TODO Only accept valid todo keywords as todo
				if (TextUtils.isEmpty(tempTodo) == false) { //&& isValidTodo(tempTodo)) {
					todo = tempTodo;
				} else {
					name = tempTodo + " ";
				}
			}
			if (matcher.group(PRIORITY_GROUP) != null)
				priority = matcher.group(PRIORITY_GROUP);
			
			name += matcher.group(TITLE_GROUP);
			
			if(useTitleField && matcher.group(AFTER_GROUP) != null) {
				int start = matcher.group(AFTER_GROUP).indexOf("TITLE:");
				int end = matcher.group(AFTER_GROUP).indexOf("</after>");
				
				if(start > -1 && end > -1) {
					String title = matcher.group(AFTER_GROUP).substring(
							start + 7, end);
					
					name = title + ">" + name;
				}
			}
			
			tags = matcher.group(TAGS_GROUP);
			if (tags == null)
					tags = "";
			
		} else {
			Log.w("MobileOrg", "Title not matched: " + heading);
			name = heading;
		}
    }

//	private boolean isValidTodo(String todo) {
//		for(HashMap<String, Integer> aTodo : this.todos) {
//			if(aTodo.containsKey(todo)) return true;
//		}
//		return false;
//	}
 
    private static final int TODO_GROUP = 1;
    private static final int PRIORITY_GROUP = 2;
    private static final int TITLE_GROUP = 3;
    private static final int TAGS_GROUP = 4;
    private static final int AFTER_GROUP = 7;
    
	private static final Pattern titlePattern = Pattern
			.compile("^\\s?(?:([A-Z]{2,}:?\\s+)\\s*)?" + "(?:\\[\\#(.*)\\])?" + // Priority
					"(.*?)" + 											// Title
					"\\s*(?::([^\\s]+):)?" + 							// Tags
					"(\\s*[!\\*])*" + 									// Habits
					"(<before>.*</before>)?" + 							// Before
					"(<after>.*</after>)?" + 							// After
					"$");												// End of line
	

	public String getPayload() {
		return this.payload;
	}
	
	public String toString() {
		StringBuilder result = new StringBuilder();
		
		for(int i = 0; i < level; i++)
			result.append("*");
		result.append(" ");

		if (TextUtils.isEmpty(todo) == false)
			result.append(todo + " ");

		if (TextUtils.isEmpty(priority) == false)
			result.append("[#" + priority + "] ");

		result.append(name);
		
		if(tags != null && TextUtils.isEmpty(tags) == false)
			result.append(" ").append(":" + tags + ":");
		

		if (payload != null && TextUtils.isEmpty(payload) == false)
			result.append("\n").append(payload);

		return result.toString();
	}

	public boolean equals(OrgNode node) {
		if (name.equals(node.name) && tags.equals(node.tags)
				&& priority.equals(node.priority) && todo.equals(node.todo)
				&& payload.equals(node.payload))
			return true;
		else
			return false;
	}

}