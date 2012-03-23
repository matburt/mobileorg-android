package com.matburt.mobileorg.Parsing;

import java.util.ArrayList;

import android.database.Cursor;

public class NodeWrapper {

	private Cursor cursor;
	private NodePayload payload;
	
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
	
	/**
	 * This function prepares the payload field of this class. It will try to
	 * replace payloads of agenda items with the real value of the original
	 * node. We have to do this to guarantee that agenda items will have the
	 * full payload and that those payload can be edited correctly.
	 */
	private void preparePayload(OrgDatabase db) {
		if(this.payload != null) {
			return;
		}
		
		if(this.cursor == null) {
			this.payload = new NodePayload("");
			return;
		}

		String result = cursor.getString(cursor.getColumnIndex("payload"));

		if(result == null) {
			this.payload = new NodePayload("");
			return;
		}
		
		this.payload = new NodePayload(result);
		
		if(!this.getFileName(db).equals("agendas.org"))
			return;
		
		String orgId = payload.getId();
		
		if(orgId == null || orgId.startsWith("olp:")) {
			return;
		}
		
		if(result.indexOf(":ORIGINAL_ID:") != -1) {
			String realPayload = db.getNodePayloadReal(orgId);
			if(realPayload == null)
				return;
			
			db.updateNodeField(this, "payload", realPayload);
			this.cursor = db.getNode(this.getId());
			this.payload = new NodePayload(realPayload);
			return;
		}
	}
	
	public NodePayload getPayload(OrgDatabase db) {
		preparePayload(db);
		return this.payload;
	}
	
	public String getCleanedPayload(OrgDatabase db) {
		preparePayload(db);
		return payload.getContent();
	}
	
	public String getRawPayload(OrgDatabase db) {
		preparePayload(db);
		
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
		
		String tags = cursor.getString(tagsColumn);
		if(tags == null)
			return "";
			
		return tags;
	}
	
	/**
	 * This will split up the tag string that it got from the tag entry in the
	 * database. The leading and trailing : are stripped out from the tags by
	 * the parser. A double colon (::) means that the tags before it are inherited.
	 */
	public ArrayList<String> getTagList() {
		ArrayList<String> result = new ArrayList<String>();		
		
		String tags = getTags();
		String[] split = tags.split("\\:");
		
		for(String tag: split)
			result.add(tag);
		
		if(tags.endsWith(":"))
			result.add("");
		
		return result;
	}
	
	public String getTagsWithoutInheritet() {
		return getTagsWithoutInheritet(getTags());
	}
	
	public static String getTagsWithoutInheritet(String tags) {
		int doubleColIndex = tags.indexOf("::");

		if (doubleColIndex == -1)
			return tags;
		else
			return tags.substring(doubleColIndex + "::".length());
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
		preparePayload(db);

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
				result.insert(0, node.getOlpName() + "/");
			else { // Get file nodes real name
				String filename = db.getFilenameFromNodeId(node.getId());
				result.insert(0, filename + ":");
			}
		}
		
		result.insert(0, "olp:");
		return result.toString();
	}
	
	/**
	 * This is called when generating the olp link to the node. This path can't
	 * have any "[" or "]" in it's path. For example having [1/3] in the title
	 * will prevent org-mode from applying the edit. This method will strip that
	 * out of the name.
	 */
	private String getOlpName() {
		return getName().replaceAll("\\[[^\\]]*\\]", "");
	}

	private long getParentId() {
		if(cursor == null)
			return -1;
		
		return cursor.getInt(cursor.getColumnIndex("parent_id"));
	}
	
	public String getFileName(OrgDatabase db) {
		if(cursor == null)
			return "";
		
		int columnIndex = cursor.getColumnIndex("file_id");
		
		if(columnIndex == -1)
			return "";
		
		long file_id = cursor.getLong(columnIndex);
		
		return db.getFilename(file_id);
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
		db.updateNodeField(this, "name", name);
	}

	public void setTodo(String todo, OrgDatabase db) {
		db.updateNodeField(this, "todo", todo);
	}

	public void setPriority(String priority, OrgDatabase db) {
		db.updateNodeField(this, "priority", priority);
	}

	public void setPayload(String payload, OrgDatabase db) {
		db.updateNodeField(this, "payload", payload);
	}

	public void setTags(String tags, OrgDatabase db) {
		db.updateNodeField(this, "tags", tags);
	}
	
	public void setParent(Long parentId, OrgDatabase db) {
		db.updateNodeField(this, "parent_id", parentId.toString());
	}
	
	public void close() {
		if(cursor != null)
			this.cursor.close();
	}

	public void addLogbook(long startTime, int hour, int minute) {
		// TODO Write new logbook drawer
	}
}

