package com.matburt.mobileorg.Parsing;

import android.database.Cursor;

public class NodeWrapper {

	private Cursor cursor;
	
	public NodeWrapper(long node_id, OrgDatabase db) {
		this.cursor = db.getNode(node_id);
		this.cursor.moveToFirst();
	}
	
	public NodeWrapper(Cursor cursor) {
		this.cursor = cursor;
		this.cursor.moveToFirst();
	}
	
	public String getName() {
		return cursor.getString(cursor.getColumnIndex("name"));
	}
	
	public String getPayload() {
		//return cursor.getString(cursor.getColumnIndex("payload"));
		return "";
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
	
	public long getId() {
		return cursor.getInt(cursor.getColumnIndex("_id"));
	}
}
