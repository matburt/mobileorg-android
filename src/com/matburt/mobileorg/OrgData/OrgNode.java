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
	
	private OrgNodePayload nodePayload = null;

	public OrgNode() {
	}
	
	public OrgNode(long nodeId, ContentResolver resolver) {
		Cursor cursor = resolver.query(OrgData.buildIdUri(nodeId),
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
		if(this.nodePayload == null)
			this.nodePayload = new OrgNodePayload(this.payload);
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
		String nodeId = getNodeId(resolver);
		
		if(nodeId.startsWith("olp:")) {
			resolver.update(OrgData.buildIdUri(id), getContentValues(), null, null);
		} else { // Update all nodes that have this :ID:
			String nodeIdQuery = "%" + nodeId + "%";
			resolver.update(OrgData.CONTENT_URI, getContentValues(), OrgData.PAYLOAD + " LIKE ?", new String[]{nodeIdQuery});
		}
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
	
	
	public boolean isNodeEditable(ContentResolver resolver) {
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
		// TODO Fix cleaning of payloads
//		preparePayload();
//		return this.nodePayload.getContent();
		return this.payload;
	}
	
	public String getRawPayload() {
		return this.payload;
	}
	
	public OrgNodePayload getPayload() {
		preparePayload();
		return this.nodePayload;
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
		if (!getCleanedPayload().equals(newNode.getCleanedPayload())
				|| !getPayload().getPayloadResidue().equals(
						getPayload().getNewPayloadResidue())) {
			String newRawPayload = getPayload().getNewPayloadResidue()
					+ newNode.getCleanedPayload();

			edits.add(new OrgEdit(this, OrgEdit.TYPE.BODY, newRawPayload, resolver));
			setPayload(newRawPayload);
		}
		if (!tags.equals(newNode.tags)) {
			// TODO Use node.getTagsWithoutInheritet() instead
			edits.add(new OrgEdit(this, OrgEdit.TYPE.TAGS, newNode.tags, resolver));
			this.tags = newNode.tags;
		}
		if (newNode.parentId != parentId) {
			edits.add(new OrgEdit(this, OrgEdit.TYPE.REFILE, newNode.getNodeId(resolver), resolver));
			this.parentId = newNode.parentId;
		}
		
		return edits;
	}
	
	public void setPayload(String newRawPayload) {
		this.nodePayload = null;
		this.payload = newRawPayload;
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
		StringBuilder rawPayload = new StringBuilder(getRawPayload());
		rawPayload = OrgNodePayload.addLogbook(rawPayload, startTime, endTime, elapsedTime);
		
		boolean generateEdits = !getFilename(resolver).equals(FileUtils.CAPTURE_FILE);

		if(generateEdits) {
			OrgEdit edit = new OrgEdit(this, OrgEdit.TYPE.BODY, rawPayload.toString(), resolver);
			edit.write(resolver);
		}
		setPayload(rawPayload.toString());
	}
}