package com.matburt.mobileorg.provider;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;

import com.matburt.mobileorg.Parsing.NodePayload;
import com.matburt.mobileorg.Parsing.NodeWrapper;
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
	
	public NodePayload nodePayload = new NodePayload("");

	public OrgNode() {
	}
	
	public OrgNode(long nodeId, ContentResolver resolver) {
		Cursor cursor = resolver.query(OrgData.buildIdUri(nodeId),
				OrgData.DEFAULT_COLUMNS, null, null, null);
		if(cursor == null || cursor.getCount() < 1)
			throw new IllegalArgumentException("Node with id \"" + id + "\" not found");
		set(cursor);
		cursor.close();
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
	
	public String getFilename(ContentResolver resolver) {
		OrgFile file = new OrgFile(fileId, resolver);
		return file.filename;
	}
	
	private void preparePayload() {
		if(this.nodePayload == null)
			this.nodePayload = new NodePayload(this.payload);
	}
	
	public long addNode(ContentResolver resolver) {
		ContentValues values = new ContentValues();
		values.put(OrgData.NAME, name);
		values.put(OrgData.TODO, todo);
		values.put(OrgData.FILE_ID, fileId);
		values.put(OrgData.LEVEL, level);
		values.put(OrgData.PARENT_ID, parentId);
		values.put(OrgData.PAYLOAD, payload);
		values.put(OrgData.PRIORITY, priority);
		values.put(OrgData.TAGS, tags);
		Uri uri = resolver.insert(OrgData.CONTENT_URI, values);
		this.id = Long.parseLong(OrgData.getId(uri));
		return id;
	}
	
	/**
	 * This is called when generating the olp link to the node. This path can't
	 * have any "[" or "]" in it's path. For example having [1/3] in the title
	 * will prevent org-mode from applying the edit. This method will strip that
	 * out of the name.
	 */
	private String getOlpName() {
		return name.replaceAll("\\[[^\\]]*\\]", "");
	}
	
	/**
	 * @return The :ID: or :ORIGINAL_ID: field of the payload.
	 */
	public String getNodeId(ContentResolver resolver) {
		preparePayload();

		String id = nodePayload.getId();				
		if(id == null)
			return constructOlpId(resolver);
		
		return id;
	}
	
	public String constructOlpId(ContentResolver resolver) {
		StringBuilder result = new StringBuilder();
		result.insert(0, name);

		while(parentId > 0) {
			OrgNode node = new OrgNode(parentId, resolver);
			parentId = node.parentId;

			if(parentId > 0)
				result.insert(0, node.getOlpName() + "/");
			else { // Get file nodes real name
				String filename = node.getFilename(resolver);
				result.insert(0, filename + ":");
			}
		}
		
		result.insert(0, "olp:");
		return result.toString();
	}
	
	
	public String getCleanedPayload() {
		return this.nodePayload.getContent();
	}
	
	public String getRawPayload() {
		return this.payload;
	}
	
	public NodePayload getPayload() {
		return this.nodePayload;
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