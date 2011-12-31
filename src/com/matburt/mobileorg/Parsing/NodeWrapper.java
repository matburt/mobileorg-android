package com.matburt.mobileorg.Parsing;

import android.database.Cursor;

public class NodeWrapper {

	private Cursor cursor;
	
	public NodeWrapper(Cursor cursor) {
		this.cursor = cursor;
	}
	
	public String getName() {
		return cursor.getString(cursor.getColumnIndex("name"));
	}
	
	public String getPayload() {
		return cursor.getString(cursor.getColumnIndex("payload"));
	}
	
	public String getTags() {
		return cursor.getString(cursor.getColumnIndex("tags"));
	}
	
	public String getTodo() {
		return cursor.getString(cursor.getColumnIndex("todo"));
	}
	
	public String getPriority() {
		return cursor.getString(cursor.getColumnIndex("priority"));
	}
	
	public String getNodeId() {
		return cursor.getString(cursor.getColumnIndex("node_id"));
	}
	
	public boolean hasChildren() {
		return false;
	}
	
	public Cursor getChildren() {
		return null;
	}
}
