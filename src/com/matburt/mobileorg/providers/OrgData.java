package com.matburt.mobileorg.providers;

import java.util.ArrayList;

import android.content.ContentResolver;
import android.database.Cursor;
import android.net.Uri;

public class OrgData {
	public static final Uri CONTENT_URI = Uri.parse("content://"
			                + OrgContentProvider.AUTHORITY + "/orgdata");
	
	public static final String ID = "_id";
	public static final String PARENT_ID = "parent_id";
	public static final String FILE_ID = "file_id";
	public static final String LEVEL = "level";
	public static final String PRIORITY = "priority";
	public static final String TODO = "todo";
	public static final String TAGS = "tags";
	public static final String PAYLOAD = "payload";
	public static final String NAME = "name";
	public static final String[] PROJECTION = { ID, PARENT_ID, FILE_ID, LEVEL,
			PRIORITY, TODO, TAGS, PAYLOAD, NAME };
	
	private static final OrgData instance = new OrgData();

	private OrgData() {}

	public static OrgData getInstance() {
		return instance;
	}
	
	public OrgNode getOrgNodeFromId(ContentResolver contentResolver, long id) {
		OrgNode result = null;

		Cursor cursor = contentResolver.query(OrgData.CONTENT_URI, OrgData.PROJECTION,
				OrgData.ID + "='" + id + "'", null, null);

		if (cursor != null) {
			if (cursor.moveToNext())
				result = getOrgNodeFromCursor(cursor);
			cursor.close();
		}

		if(result == null)
			throw new IllegalArgumentException("Couln't find node with id " + id);
		
		return result;
	}
	
	public ArrayList<Long> getChildrenFromId(ContentResolver contentResolver, long id) {
		ArrayList<Long> result = new ArrayList<Long>();

		Cursor cursor = contentResolver.query(OrgData.CONTENT_URI,
				new String[] {"_id"}, OrgData.PARENT_ID + "='?'" + id + "'", null, null);

		if (cursor != null) {
			cursor.moveToFirst();
			while(!cursor.isAfterLast())
				result.add(cursor.getLong(cursor.getColumnIndex(OrgData.ID)));
			cursor.close();
		}

		return result;
	}
	
	private OrgNode getOrgNodeFromCursor(Cursor cursor) {
		OrgNode node = new OrgNode();
		node.id = cursor.getLong(cursor.getColumnIndex(OrgData.ID));
		node.parentId = cursor.getLong(cursor.getColumnIndex(OrgData.PARENT_ID));
		node.fileId = cursor.getLong(cursor.getColumnIndex(OrgData.FILE_ID));
		node.level = cursor.getLong(cursor.getColumnIndex(OrgData.LEVEL));
		node.priority = cursor.getString(cursor.getColumnIndex(OrgData.PRIORITY));
		node.todo = cursor.getString(cursor.getColumnIndex(OrgData.TODO));
		node.tags = cursor.getString(cursor.getColumnIndex(OrgData.TAGS));
		node.name = cursor.getString(cursor.getColumnIndex(OrgData.NAME));
		return node;
	}

}
