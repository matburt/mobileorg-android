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
		
		String name = cursor.getString(cursor.getColumnIndex("name"));
		
		if(name == null)
			return "";
		else
			return name;
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
	
	public String getPayloadResidue() {
		if(this.cursor == null)
			return "";
		
		String result = cursor.getString(cursor.getColumnIndex("payload"));

		if(result == null)
			return "";

		NodePayload payload = new NodePayload(result);
		return payload.getPayloadResidue();
	}
	
	public String getCleanedPayload() {
		if(this.cursor == null)
			return "";
		
		String result = cursor.getString(cursor.getColumnIndex("payload"));

		if(result == null)
			return "";

		NodePayload payload = new NodePayload(result);
		return payload.getContent();
	}
	
	public String getRawPayload() {
		if(this.cursor == null)
			return "";
		
		String result = cursor.getString(cursor.getColumnIndex("payload"));

		if(result == null)
			return "";

		return result;
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
	
	/**
	 * @return The :ID: or :ORIGINAL_ID: field of the payload.
	 */
	public String getNodeId(OrgDatabase db) {
		if(cursor == null)
			return "";
		
		NodePayload payload = new NodePayload(getRawPayload());
		String id = payload.getId();
				
		if(id == null)
			return constructOlpId(db);
		
		return id;
	}
	
	private String constructOlpId(OrgDatabase db) {
		StringBuilder result = new StringBuilder();
		result.insert(0, getName());
		
		long parentId = getParentId();

		while(parentId > 0) {
			NodeWrapper node = new NodeWrapper(db.getNode(parentId));
			parentId = node.getParentId();

			if(parentId > 0)
				result.insert(0, node.getName() + "/");
			else { // Get file nodes real name
				String filename = db.getFileName(node.getId());
				result.insert(0, filename + ":");
			}
		}
		
		result.insert(0, "olp:");
		return result.toString();
	}

	private long getParentId() {
		if(cursor == null)
			return -1;
		
		return cursor.getInt(cursor.getColumnIndex("parent_id"));
	}
	
	/**
	 * @return The internal id of the node. Used for mapping to database.
	 */
	public long getId() {
		if(cursor == null)
			return -1;
		return cursor.getInt(cursor.getColumnIndex("_id"));
	}

	public void setName(String name, OrgDatabase db) {
		db.updateNodeField(getId(), "name", name);
	}

	public void setTodo(String todo, OrgDatabase db) {
		db.updateNodeField(getId(), "todo", todo);
	}

	public void setPriority(String priority, OrgDatabase db) {
		db.updateNodeField(getId(), "priority", priority);
	}

	public void setPayload(String payload, OrgDatabase db) {
		db.addNodePayload(getId(), payload);
	}

	public void setTags(String tags, OrgDatabase db) {
		db.updateNodeField(getId(), "tags", tags);
	}
}

