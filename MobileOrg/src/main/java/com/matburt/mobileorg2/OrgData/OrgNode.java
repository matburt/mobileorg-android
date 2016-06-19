package com.matburt.mobileorg2.OrgData;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.Log;

import com.matburt.mobileorg2.OrgData.OrgContract.OrgData;
import com.matburt.mobileorg2.util.OrgFileNotFoundException;
import com.matburt.mobileorg2.util.OrgNodeNotFoundException;
import com.matburt.mobileorg2.util.OrgUtils;

import java.util.ArrayList;
import java.util.HashMap;

public class OrgNode {

	public long id = -1;
	public long parentId = -1;
	public long fileId = -1;

	public long level = 0; // The headline level
	public String priority = ""; // The priority tag
	public String todo = "";    // The TODO state
	public String tags = "";
	public String tags_inherited = "";
	public String name = "";
    public long scheduled;
    public long deadline;
    // The ordering of the same level siblings
    public int position = 0;
    // The payload is a string containing the raw string corresponding to this mode
    private String payload = "";
    private OrgNodePayload orgNodePayload = null;

	public OrgNode() {
	}
	
	public OrgNode(OrgNode node) {
		this.level = node.level;
		this.priority = node.priority;
		this.todo = node.todo;
		this.tags = node.tags;
		this.tags_inherited = node.tags_inherited;
		this.name = node.name;
        this.position = node.position;
        this.scheduled = node.scheduled;
        this.deadline = node.deadline;
		setPayload(node.getPayload());
	}
	
	public OrgNode(long id, ContentResolver resolver) throws OrgNodeNotFoundException {
		Cursor cursor = resolver.query(OrgData.buildIdUri(id),
				OrgData.DEFAULT_COLUMNS, null, null, null);
		if(cursor == null)
            throw new OrgNodeNotFoundException("Node with id \"" + id + "\" not found");

        if(!cursor.moveToFirst()){
            cursor.close();
            throw new OrgNodeNotFoundException("Node with id \"" + id + "\" not found");
        }
		set(cursor);
        cursor.close();
	}

	public OrgNode(Cursor cursor) throws OrgNodeNotFoundException {
		set(cursor);
	}

    public static boolean hasChildren(long node_id, ContentResolver resolver) {
        try {
            OrgNode node = new OrgNode(node_id, resolver);
            return node.hasChildren(resolver);
        } catch (OrgNodeNotFoundException e) {
        }

        return false;
    }

    public void set(Cursor cursor) throws OrgNodeNotFoundException {
        if (cursor != null && cursor.getCount() > 0) {
			if(cursor.isBeforeFirst() || cursor.isAfterLast())
				cursor.moveToFirst();
			id = cursor.getLong(cursor.getColumnIndexOrThrow(OrgData.ID));
			parentId = cursor.getLong(cursor
                    .getColumnIndexOrThrow(OrgData.PARENT_ID));
			fileId = cursor.getLong(cursor
                    .getColumnIndexOrThrow(OrgData.FILE_ID));
			level = cursor.getLong(cursor.getColumnIndexOrThrow(OrgData.LEVEL));
			priority = cursor.getString(cursor
                    .getColumnIndexOrThrow(OrgData.PRIORITY));
			todo = cursor.getString(cursor.getColumnIndexOrThrow(OrgData.TODO));
			tags = cursor.getString(cursor.getColumnIndexOrThrow(OrgData.TAGS));
			tags_inherited = cursor.getString(cursor
					.getColumnIndexOrThrow(OrgData.TAGS_INHERITED));
			name = cursor.getString(cursor.getColumnIndexOrThrow(OrgData.NAME));
			payload = cursor.getString(cursor
					.getColumnIndexOrThrow(OrgData.PAYLOAD));
            position = cursor.getInt(cursor
                    .getColumnIndexOrThrow(OrgData.POSITION));
            scheduled = cursor.getLong(cursor
                    .getColumnIndexOrThrow(OrgData.SCHEDULED));
            deadline = cursor.getLong(cursor
                    .getColumnIndexOrThrow(OrgData.DEADLINE));

		} else {
			throw new OrgNodeNotFoundException(
					"Failed to create OrgNode from cursor");
		}

    }
	
	public String getFilename(ContentResolver resolver) {
		try {
			OrgFile file = new OrgFile(fileId, resolver);
			return file.filename;
		} catch (OrgFileNotFoundException e) {
			return "";
		}
	}
	
	public OrgFile getOrgFile(ContentResolver resolver) throws OrgFileNotFoundException {
		return new OrgFile(fileId, resolver);
	}
	
	public void setFilename(String filename, ContentResolver resolver) throws OrgFileNotFoundException {
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

    /**
     * Generate the family tree with all descendants nodes and starting at the current one
     * @return the ArrayList<OrgNode> containing all nodes
     */
    public ArrayList<OrgNode> getDescandants(ContentResolver resolver){
        ArrayList<OrgNode> result = new ArrayList<OrgNode>();
        result.add(this);
        for(OrgNode child: getChildren(resolver)) result.addAll(child.getDescandants(resolver));
        return result;
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
		if (!nodeId.startsWith("olp:")) { // Update all nodes that have this :ID:
			String nodeIdQuery = "%" + nodeId + "%";
			resolver.update(OrgData.CONTENT_URI, getSimpleContentValues(),
					OrgData.PAYLOAD + " LIKE ?", new String[] { nodeIdQuery });
		}
	}

	public OrgNode findOriginalNode(ContentResolver resolver) {
		if(parentId == -1)
			return this;

        if (!getFilename(resolver).equals(OrgFile.AGENDA_FILE))
			return this;

        String nodeId = getNodeId(resolver);
		if (!nodeId.startsWith("olp:")) { // Update all nodes that have this :ID:
			String nodeIdQuery = OrgData.PAYLOAD + " LIKE '%" + nodeId + "%'";
			try {
				OrgFile agendaFile = new OrgFile(OrgFile.AGENDA_FILE, resolver);
				if(agendaFile != null)
					nodeIdQuery += " AND NOT " + OrgData.FILE_ID + "=" + agendaFile.nodeId;
			} catch (OrgFileNotFoundException e) {}

            Cursor query = resolver.query(OrgData.CONTENT_URI,
					OrgData.DEFAULT_COLUMNS, nodeIdQuery, null,
					null);

            OrgNode node = null;
			try {
				node = new OrgNode(query);
			} catch (OrgNodeNotFoundException e) {

            } finally {
                if (query != null)  query.close();
            }
            return node;
        }

        return this;
	}

    public boolean isHabit() {
		preparePayload();
		return orgNodePayload.getProperty("STYLE").equals("habit");
	}
	
	private ContentValues getSimpleContentValues() {
		ContentValues values = new ContentValues();
		values.put(OrgData.NAME, name);
		values.put(OrgData.TODO, todo);
		values.put(OrgData.PAYLOAD, payload);
		values.put(OrgData.PRIORITY, priority);
		values.put(OrgData.TAGS, tags);
		values.put(OrgData.TAGS_INHERITED, tags_inherited);
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
        values.put(OrgData.TAGS_INHERITED, tags_inherited);
        values.put(OrgData.POSITION, position);
        values.put(OrgData.SCHEDULED, scheduled);
        values.put(OrgData.DEADLINE, deadline);
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
		return OrgProviderUtils.getOrgNodeChildren(id, resolver);
	}
	
	public ArrayList<String> getChildrenStringArray(ContentResolver resolver) {
		ArrayList<String> result = new ArrayList<String>();
		ArrayList<OrgNode> children = getChildren(resolver);

		for (OrgNode node : children)
			result.add(node.name);

		return result;
	}
	
	public OrgNode getChild(String name, ContentResolver resolver) throws OrgNodeNotFoundException {
        ArrayList<OrgNode> children = getChildren(resolver);

        for(OrgNode child: children) {
			if(child.name.equals(name))
				return child;
		}
		throw new OrgNodeNotFoundException("Couln't find child of node "
				+ this.name + " with name " + name);
	}
	
	public boolean hasChildren(ContentResolver resolver) {
		Cursor childCursor = resolver.query(OrgData.buildChildrenUri(id),
                OrgData.DEFAULT_COLUMNS, null, null, null);

        int childCount = childCursor.getCount();
		childCursor.close();

		return childCount > 0;
	}
	
	public OrgNode getParent(ContentResolver resolver) throws OrgNodeNotFoundException {
		Cursor cursor = resolver.query(OrgData.buildIdUri(this.parentId),
				OrgData.DEFAULT_COLUMNS, null, null, null);
        OrgNode parent = new OrgNode(cursor);
        if(cursor!=null) cursor.close();
		return parent;
	}


	public ArrayList<String> getSiblingsStringArray(ContentResolver resolver) {
		try {
			OrgNode parent = getParent(resolver);
			return parent.getChildrenStringArray(resolver);
		}
		catch (OrgNodeNotFoundException e) {
			throw new IllegalArgumentException("Couldn't get parent for node " + name);
		}
	}

	public ArrayList<OrgNode> getSiblings(ContentResolver resolver) {
        try {
            OrgNode parent = getParent(resolver);
            return parent.getChildren(resolver);
        } catch (OrgNodeNotFoundException e) {
			throw new IllegalArgumentException("Couldn't get parent for node " + name);
		}
	}

    public void shiftNextSiblingNodes(ContentResolver resolver){
        for(OrgNode sibling: getSiblings(resolver) ){
            if(sibling.position >= position && sibling.id != this.id) {
                ++sibling.position;
                sibling.updateNode(resolver);
                Log.v("position", "new pos : " + sibling.position);
                Log.v("position", sibling.getCleanedName());
            }
        }
    }

	public boolean isFilenode(ContentResolver resolver) {
		try {
			OrgFile file = new OrgFile(fileId, resolver);
			if(file.nodeId == this.id)
				return true;
		} catch (OrgFileNotFoundException e) {}
		
		return false;
	}
	
	public boolean isNodeEditable(ContentResolver resolver) {
		if(id < 0) // Node is not in database
			return true;
		
		if(id >= 0 && parentId < 0) // Top-level node
			return false;
		
		try {
			OrgFile agendaFile = new OrgFile(OrgFile.AGENDA_FILE, resolver);
			if (agendaFile != null && agendaFile.nodeId == parentId) // Second level in agendas file
				return false;
			
			if(fileId == agendaFile.id && name.startsWith(OrgFileParser.BLOCK_SEPARATOR_PREFIX))
				return false;
		} catch (OrgFileNotFoundException e) {}

		return true;
	}

	public String getCleanedName() {
		StringBuilder nameBuilder = new StringBuilder(this.name);
		
//		Matcher matcher = OutlineItem.urlPattern.matcher(nameBuilder);
//		while(matcher.find()) {
//			nameBuilder.delete(matcher.start(), matcher.end());
//			nameBuilder.insert(matcher.start(), matcher.group(1));
//			matcher = OutlineItem.urlPattern.matcher(nameBuilder);
//		}
//
//		return nameBuilder.toString();
		return this.name;
	}

	
	/**
	 * @return The :ID:, :ORIGINAL_ID: or olp link of node.
	 */
	public String getNodeId(ContentResolver resolver) {
		preparePayload();

		String id = orgNodePayload.getId();				
		if(id != null && id.equals("") == false)
			return id;
		else
			return getOlpId(resolver);
	}
	
	public String getOlpId(ContentResolver resolver) {
		StringBuilder result = new StringBuilder();
		
		ArrayList<OrgNode> nodesFromRoot;
		try {
			nodesFromRoot = OrgProviderUtils.getOrgNodePathFromTopLevel(
					parentId, resolver);
		} catch (IllegalStateException e) {
			return "";
		}
		
		if (nodesFromRoot.size() == 0) {
			try {
				return "olp:" + getOrgFile(resolver).name;
			} catch (OrgFileNotFoundException e) {
				return "";
			}
		}
			
		OrgNode topNode = nodesFromRoot.get(0);
		nodesFromRoot.remove(0);
		result.append("olp:" + topNode.getFilename(resolver) + ":");
		
		for(OrgNode node: nodesFromRoot)
			result.append(node.getStrippedNameForOlpPathLink() + "/");
		
		result.append(getStrippedNameForOlpPathLink());
		return result.toString();
	}
	
	/**
	 * Olp paths containing certain symbols can't be applied by org-mode. To
	 * prevent node names from injecting bad symbols, we strip them out here.
	 */
	private String getStrippedNameForOlpPathLink() {
		String result = this.name;
		result = result.replaceAll("\\[[^\\]]*\\]", ""); // Strip out "[*]"
		return result;
	}
	
	
	public String getCleanedPayload() {
		preparePayload();
		return this.orgNodePayload.getCleanedPayload();
	}
	
	public String getPayload() {
        preparePayload();
		return this.orgNodePayload.get();
	}

    public void setPayload(String payload) {
        this.orgNodePayload = null;
        this.payload = payload;
    }

    public HashMap getPropertiesPayload() {
        preparePayload();
        return this.orgNodePayload.getPropertiesPayload();
    }

    public void addDate(OrgNodeTimeDate date){
        this.orgNodePayload.insertOrReplaceDate(date);
        switch (date.type){
            case Deadline:
                deadline = date.getTimeInMillis();
                break;
            case Scheduled:
                scheduled = date.getTimeInMillis();
                break;
        }
        return;
    }
	
	public OrgNodePayload getOrgNodePayload() {
		preparePayload();
		return this.orgNodePayload;
	}
	
	public OrgEdit createParentNewheading(ContentResolver resolver) {
        OrgNode parent = null;
        try {
            parent = getParent(resolver);
        } catch (OrgNodeNotFoundException e) {
            e.printStackTrace();
        }
        this.level = parent.level + 1;

		try {
			OrgFile file = new OrgFile(parent.fileId, resolver);
		} catch (OrgFileNotFoundException e) {}

		// Add new heading nodes; need the entire content of node without
		// star headings
		long tempLevel = level;
		level = 0;
		OrgEdit edit = new OrgEdit(parent, OrgEdit.TYPE.ADDHEADING, this.toString(), resolver);
		level = tempLevel;
		return edit;
	}


	/**
	 * Build the the plain text string corresponding to this node
	 * @return the node in plain text
     */
	public String toString() {
		StringBuilder result = new StringBuilder();
		
		for(int i = 0; i < level; i++)
			result.append("*");
		result.append(" ");

		if (!TextUtils.isEmpty(todo))
			result.append(todo + " ");

		if (!TextUtils.isEmpty(priority))
			result.append("[#" + priority + "] ");

		result.append(name);
		
		if(tags != null && !TextUtils.isEmpty(tags))
			result.append(" ").append(":" + tags + ":");
		

		if (payload != null && !TextUtils.isEmpty(payload)){
			result.append("\n");
			if(level > 0) for(int i = 0;i<level+1; i++) result.append(" ");
			result.append(payload.trim());
		}

		return result.toString();
	}



	public boolean equals(OrgNode node) {
		return name.equals(node.name) && tags.equals(node.tags)
				&& priority.equals(node.priority) && todo.equals(node.todo)
				&& payload.equals(node.payload);
	}


    public void deleteNode(Context context) {
        OrgEdit.updateFile(this, null, context);
        context.getContentResolver().delete(OrgData.buildIdUri(id), null, null);
    }
	
	public void addAutomaticTimestamp() {
		Context context = MobileOrgApplication.getContext();
		boolean addTimestamp = PreferenceManager.getDefaultSharedPreferences(
				context).getBoolean("captureWithTimestamp", false);
		if(addTimestamp)
			setPayload(getPayload() + OrgUtils.getTimestamp());
	}
}
