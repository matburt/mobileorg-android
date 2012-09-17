package com.matburt.mobileorg.OrgData;

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;

import com.matburt.mobileorg.OrgData.OrgContract.OrgData;
import com.matburt.mobileorg.util.FileUtils;

public class OrgNode {

	public long id = -1;
	public long parentId = -1;
	public long fileId = -1;
	public long level = 0;
	public String priority = "";
	public String todo = "";
	public String tags = "";
	public String name = "";
	private String payload = "";
	
	private OrgNodePayload orgNodePayload = null;

	public OrgNode() {
	}
	
	public OrgNode(long id, ContentResolver resolver) {
		Cursor cursor = resolver.query(OrgData.buildIdUri(id),
				OrgData.DEFAULT_COLUMNS, null, null, null);
		if(cursor == null || cursor.moveToFirst() == false)
			throw new IllegalArgumentException("Node with id \"" + id + "\" not found");
		set(cursor);
		cursor.close();
	}

	public OrgNode(Cursor cursor) {
		set(cursor);
	}
	
	public void set(Cursor cursor) {
		if (cursor != null && cursor.getCount() > 0) {
			if(cursor.isBeforeFirst() || cursor.isAfterLast())
				cursor.moveToFirst();
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
	
	public void setFilename(String filename, ContentResolver resolver) {
		OrgFile file = new OrgFile(filename, resolver);
		this.fileId = file.nodeId;
	}
	
	private void preparePayload() {
		if(this.orgNodePayload == null)
			this.orgNodePayload = new OrgNodePayload(this.payload);
	}
	
	public void write(ContentResolver resolver) {
		if(id < 0)
			addNode(resolver);
		else
			updateNode(resolver);
	}
	
	private long addNode(ContentResolver resolver) {
		Uri uri = resolver.insert(OrgData.CONTENT_URI, getContentValues());
		this.id = Long.parseLong(OrgData.getId(uri));
		return id;
	}
	
	private int updateNode(ContentResolver resolver) {
		return resolver.update(OrgData.buildIdUri(id), getContentValues(), null, null);
	}
	
	public void updateAllNodes(ContentResolver resolver) {
		updateNode(resolver);
		
		String nodeId = getNodeId(resolver);
		if (nodeId.startsWith("olp:") == false) { // Update all nodes that have this :ID:
			String nodeIdQuery = "%" + nodeId + "%";
			resolver.update(OrgData.CONTENT_URI, getSimpleContentValues(),
					OrgData.PAYLOAD + " LIKE ?", new String[] { nodeIdQuery });
		}
	}
	
	public OrgNode findOriginalNode(ContentResolver resolver) {
		if(getFilename(resolver).equals(OrgFile.AGENDA_FILE) == false)
			return this;
		
		String nodeId = getNodeId(resolver);
		if (nodeId.startsWith("olp:") == false) { // Update all nodes that have this :ID:
			String nodeIdQuery = OrgData.PAYLOAD + " LIKE '%" + nodeId + "%'";
			try {
				OrgFile agendaFile = new OrgFile(OrgFile.AGENDA_FILE, resolver);
				if(agendaFile != null)
					nodeIdQuery += " AND NOT " + OrgData.FILE_ID + "=" + agendaFile.nodeId;
			} catch (IllegalArgumentException e) {}
			
			Cursor query = resolver.query(OrgData.CONTENT_URI,
					OrgData.DEFAULT_COLUMNS, nodeIdQuery, null,
					null);
			try {
				OrgNode node = new OrgNode(query);
				query.close();
				return node;
			} catch (IllegalArgumentException e) {
				if (query != null)
					query.close();
			}
		}
		
		return this;
	}
	
	private ContentValues getSimpleContentValues() {
		ContentValues values = new ContentValues();
		values.put(OrgData.NAME, name);
		values.put(OrgData.TODO, todo);
		values.put(OrgData.PAYLOAD, payload);
		values.put(OrgData.PRIORITY, priority);
		values.put(OrgData.TAGS, tags);
		return values;
	}
	
	private ContentValues getContentValues() {
		ContentValues values = new ContentValues();
		values.put(OrgData.NAME, name);
		values.put(OrgData.TODO, todo);
		values.put(OrgData.FILE_ID, fileId);
		values.put(OrgData.LEVEL, level);
		values.put(OrgData.PARENT_ID, parentId);
		values.put(OrgData.PAYLOAD, payload);
		values.put(OrgData.PRIORITY, priority);
		values.put(OrgData.TAGS, tags);
		return values;
	}
	

	/**
	 * This will split up the tag string that it got from the tag entry in the
	 * database. The leading and trailing : are stripped out from the tags by
	 * the parser. A double colon (::) means that the tags before it are
	 * inherited.
	 */
	public ArrayList<String> getTags() {
		ArrayList<String> result = new ArrayList<String>();

		if(tags == null)
			return result;
		
		String[] split = tags.split("\\:");

		for (String tag : split)
			result.add(tag);

		if (tags.endsWith(":"))
			result.add("");

		return result;
	}
	
	public ArrayList<OrgNode> getChildren(ContentResolver resolver) {
		ArrayList<OrgNode> result = new ArrayList<OrgNode>();
		
		Cursor childCursor = resolver.query(OrgData.buildChildrenUri(id),
				OrgData.DEFAULT_COLUMNS, null, null, null);
		
		childCursor.moveToFirst();
		
		while(childCursor.isAfterLast() == false) {
			result.add(new OrgNode(childCursor));
			childCursor.moveToNext();
		}
		
		childCursor.close();
		return result;
	}
	
	public ArrayList<String> getChildrenStringArray(ContentResolver resolver) {
		ArrayList<String> result = new ArrayList<String>();
		ArrayList<OrgNode> children = getChildren(resolver);

		for (OrgNode node : children)
			result.add(node.name);

		return result;
	}
	
	public OrgNode getChild(String name, ContentResolver resolver) {
		ArrayList<OrgNode> children = getChildren(resolver);
		
		for(OrgNode child: children) {
			if(child.name.equals(name))
				return child;
		}
		return null;
	}
	
	public boolean hasChildren(ContentResolver resolver) {
		Cursor childCursor = resolver.query(OrgData.buildChildrenUri(id),
				OrgData.DEFAULT_COLUMNS, null, null, null);
		
		int childCount = childCursor.getCount();
		childCursor.close();
		
		if(childCount > 0)
			return true;
		else
			return false;
	}
	
	public static boolean hasChildren (long node_id, ContentResolver resolver) {
		try {
			OrgNode node = new OrgNode(node_id, resolver);
			return node.hasChildren(resolver);
		} catch (IllegalArgumentException e) {
		}
		
		return false;
	}
	
	public OrgNode getParent(ContentResolver resolver) {
		Cursor cursor = resolver.query(OrgData.buildIdUri(this.parentId),
				OrgData.DEFAULT_COLUMNS, null, null, null);
		if(cursor.getCount() > 0)
			return new OrgNode(cursor);
		else
			return null;
	}
	
	public ArrayList<String> getSiblingsStringArray(ContentResolver resolver) {		
		OrgNode parent = getParent(resolver);
		if(parent != null)
			return parent.getChildrenStringArray(resolver);
		else
			throw new IllegalArgumentException("Couln't get parent for node " + name);
	}
	
	public OrgNode getSibling(String name, ContentResolver resolver) {		
		OrgNode parent = getParent(resolver);
		if(parent != null)
			return parent.getChild(name, resolver);
		else
			return null;
	}
	
	public boolean isNodeEditable(ContentResolver resolver) {
		if(id < 0) // Node is not in database
			return true;
		
		if(id >= 0 && parentId == -1) // Top-level node
			return false;
		
		try {
			OrgFile agendaFile = new OrgFile(OrgFile.AGENDA_FILE, resolver);
			if (agendaFile != null && agendaFile.nodeId == parentId) // Second level in agendas file
				return false;
		} catch (IllegalArgumentException e) {}

		return true;
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

		String id = orgNodePayload.getId();				
		if(id == null)
			return constructOlpId(resolver);
		
		return id;
	}
	
	public String constructOlpId(ContentResolver resolver) {
		StringBuilder result = new StringBuilder();
		result.insert(0, name);

		long currentParentId = this.parentId;
		while(currentParentId > 0) {
			OrgNode node = new OrgNode(currentParentId, resolver);
			currentParentId = node.parentId;

			if(currentParentId > 0)
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
		preparePayload();
		return this.orgNodePayload.getCleanedPayload();
	}
	
	public String getPayload() {
		return this.payload;
	}
	
	public void setPayload(String payload) {
		this.orgNodePayload = null;
		this.payload = payload;
	}
	
	public OrgNodePayload getOrgNodePayload() {
		preparePayload();
		return this.orgNodePayload;
	}
	
	public OrgEdit createParentNewheading(ContentResolver resolver) {
		OrgNode parent;
		try {
			parent = new OrgNode(this.parentId, resolver);
		} catch (IllegalArgumentException e) {
			throw new IllegalStateException("Parent for node " + this.name
					+ " does not exist");
		}

		boolean generateEdit = false;
		try {
			OrgFile file = new OrgFile(parent.fileId, resolver);
			generateEdit = file.isEditable();
		} catch (IllegalArgumentException e) {
			throw new IllegalStateException("Couln't find file of node " + parent.name);
		}
		
		if (generateEdit) {
			// Add new heading nodes; need the entire content of node without
			// star headings
			long tempLevel = level;
			level = 0;
			OrgEdit edit = new OrgEdit(parent, OrgEdit.TYPE.ADDHEADING, this.toString(), resolver);
			level = tempLevel;
			return edit;
		} else
			return new OrgEdit();
	}
	
	
	public void generateAndApplyEdits(OrgNode newNode, ContentResolver resolver) {
		ArrayList<OrgEdit> generateEditNodes = generateEditNodes(newNode, resolver);
		boolean generateEdits = !getFilename(resolver).equals(FileUtils.CAPTURE_FILE);
				
		if(generateEdits)
			for(OrgEdit edit: generateEditNodes)
				edit.write(resolver);
	}
	
	public ArrayList<OrgEdit> generateEditNodes(OrgNode newNode, ContentResolver resolver) {
		ArrayList<OrgEdit> edits = new ArrayList<OrgEdit>();

		if (!name.equals(newNode.name)) {
			edits.add(new OrgEdit(this, OrgEdit.TYPE.HEADING, newNode.name, resolver));
			this.name = newNode.name;			
		}
		if (newNode.todo != null && !todo.equals(newNode.todo)) {
			edits.add(new OrgEdit(this, OrgEdit.TYPE.TODO, newNode.todo, resolver));
			this.todo = newNode.todo;
		}
		if (newNode.priority != null && !priority.equals(newNode.priority)) {
			edits.add(new OrgEdit(this, OrgEdit.TYPE.PRIORITY, newNode.priority, resolver));
			this.priority = newNode.priority;
		}
		if (newNode.getPayload() != null && !newNode.getPayload().equals(getPayload())) {
			edits.add(new OrgEdit(this, OrgEdit.TYPE.BODY, newNode.getPayload(), resolver));
			setPayload(newNode.getPayload());
		}
		if (!tags.equals(newNode.tags)) {
			// TODO Use node.getTagsWithoutInheritet() instead
			edits.add(new OrgEdit(this, OrgEdit.TYPE.TAGS, newNode.tags, resolver));
			this.tags = newNode.tags;
		}
		if (newNode.parentId != parentId) {
			OrgNode parent = new OrgNode(newNode.parentId, resolver);
			String newId = parent.getNodeId(resolver);
			
			edits.add(new OrgEdit(this, OrgEdit.TYPE.REFILE, newId, resolver));
			this.parentId = newNode.parentId;
			this.fileId = newNode.fileId;
		}
		
		return edits;
	}


	public void parseLine(String thisLine, int numstars, boolean useTitleField) {
        String heading = thisLine.substring(numstars+1);
        this.level = numstars;
        
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
			.compile("^\\s?(?:([A-Z]{2,}:?\\s+)\\s*)?" + "(?:\\[\\#([^]]+)\\])?" + // Priority
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

	
	public void deleteNode(ContentResolver resolver) {
		OrgEdit edit = new OrgEdit(this, OrgEdit.TYPE.DELETE, resolver);
		edit.write(resolver);
		resolver.delete(OrgData.buildIdUri(id), null, null);
	}
	
	public void archiveNode(ContentResolver resolver) {
		OrgEdit edit = new OrgEdit(this, OrgEdit.TYPE.ARCHIVE, resolver);
		edit.write(resolver);
		resolver.delete(OrgData.buildIdUri(id), null, null);
	}
	
	public void archiveNodeToSibling(ContentResolver resolver) {
		OrgEdit edit = new OrgEdit(this, OrgEdit.TYPE.ARCHIVE_SIBLING, resolver);
		edit.write(resolver);
		
		// TODO Finish
//		OrgNode parent = node.getParent();
//		if(parent != null) {
//			NodeWrapper child = parent.getChild("Archive");
//			if(child != null) {
//				node.setParent(child.getId());
//				child.close();
//			} else {
//				OrgNode archiveNode = new OrgNode();
//				archiveNode.name = "Archive";
//				archiveNode.parentId = parent.id;
//				archiveNode.fileId = parent.fileId;
//				long child_id = node.addNode(resolver);
//				node.setParent(child_id);
//			}
//		} else {
//			resolver.delete(OrgData.buildIdUri(node.id), null, null);
//		}
	}
	
	public void addLogbook(long startTime, long endTime, String elapsedTime, ContentResolver resolver) {
		StringBuilder rawPayload = new StringBuilder(getPayload());
		rawPayload = OrgNodePayload.addLogbook(rawPayload, startTime, endTime, elapsedTime);
		
		boolean generateEdits = !getFilename(resolver).equals(FileUtils.CAPTURE_FILE);

		if(generateEdits) {
			OrgEdit edit = new OrgEdit(this, OrgEdit.TYPE.BODY, rawPayload.toString(), resolver);
			edit.write(resolver);
		}
		setPayload(rawPayload.toString());
	}
}