package com.matburt.mobileorg.Parsing;

import java.util.ArrayList;

import android.database.Cursor;

public class NodeWrapper {

	private Cursor cursor;
	
	public NodeWrapper(long node_id, OrgDatabase db) {
		this.cursor = db.getNode(node_id);
	}
	
	public NodeWrapper(Cursor cursor) {
		this.cursor = cursor;
	}
	
	public String getName() {
		if(cursor == null)
			return "";
		
		return cursor.getString(cursor.getColumnIndex("name"));
	}
	
	public ArrayList<NodeWrapper> getChildren(OrgDatabase db) {
		ArrayList<NodeWrapper> result = new ArrayList<NodeWrapper>();
		
		if(!db.hasNodeChildren(this.getId()))
			return result;
		
		Cursor nodeChildren = db.getNodeChildren(this.getId());
		nodeChildren.moveToFirst();
		
		
		while(nodeChildren.isAfterLast() == false) {
			long id = (new NodeWrapper(nodeChildren)).getId();
			result.add(new NodeWrapper(id, db));
			nodeChildren.moveToNext();
		}
		
		return result;
	}
	
	public String getPayload() {
		if(this.cursor == null)
			return "";
		
		String result = cursor.getString(cursor.getColumnIndex("payload"));

		if(result == null)
			return "";

		NodePayload payload = new NodePayload(result);
		return payload.getContent();
	}
	
	public String getTags() {
		if(cursor == null)
			return "";
		
		int tagsColumn = cursor.getColumnIndex("tags");
		
		if(tagsColumn == -1)
			return "";
		
		return cursor.getString(tagsColumn);
	}
	
	public String getTodo() {
		if(cursor == null)
			return "";
		
		int todoColumn = cursor.getColumnIndex("todo");
		
		if(todoColumn == -1)
			return "";
		
		return cursor.getString(todoColumn);
	}
	
	public String getPriority() {
		if(cursor == null)
			return "";
		
		int priorityColumn = cursor.getColumnIndex("priority");
		
		if(priorityColumn == -1)
			return "";
		
		return cursor.getString(priorityColumn);
	}
	
	public String getNodeId() {
		if(cursor == null)
			return "";
		return cursor.getString(cursor.getColumnIndex("node_id"));
	}
	
	public long getId() {
		if(cursor == null)
			return -1;
		return cursor.getInt(cursor.getColumnIndex("_id"));
	}
}

