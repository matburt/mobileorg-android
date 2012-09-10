package com.matburt.mobileorg.OrgData;

import java.util.ArrayList;
import java.util.HashMap;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.database.Cursor;
import android.text.TextUtils;
import android.util.Log;

import com.matburt.mobileorg.OrgData.OrgContract.Edits;
import com.matburt.mobileorg.OrgData.OrgContract.Files;
import com.matburt.mobileorg.OrgData.OrgContract.OrgData;
import com.matburt.mobileorg.OrgData.OrgContract.Priorities;
import com.matburt.mobileorg.OrgData.OrgContract.Tags;
import com.matburt.mobileorg.OrgData.OrgContract.Todos;
import com.matburt.mobileorg.util.FileUtils;

public class OrgProviderUtil {
	
	
	public static void createNodeWithNewheadingEditnode(OrgNode node, OrgNode parent, String newPayload, ContentResolver resolver) {
		if (parent == null) {
			OrgFile file;
			try {
				file = new OrgFile(FileUtils.CAPTURE_FILE, resolver);
			} catch (IllegalArgumentException e) {
				file = new OrgFile(FileUtils.CAPTURE_FILE, FileUtils.CAPTURE_FILE_ALIAS, "");
				file.setResolver(resolver);
				file.write();
			}

			node.parentId = file.nodeId;
			node.fileId = file.id;
		} else {
			node.fileId = parent.fileId;
			node.parentId = parent.id;
		}
		
		node.setPayload(newPayload);
		node.write(resolver);
		
		makeNewheadingEditNode(node, parent, resolver);
	}
	
	private static void makeNewheadingEditNode(OrgNode node, OrgNode parent, ContentResolver resolver) {
		boolean generateEdit = parent != null && !parent.getFilename(resolver).equals(FileUtils.CAPTURE_FILE);
		if(generateEdit == false)
			return;

		// Add new heading nodes; need the entire content of node without star headings
		long tempLevel = node.level;
		node.level = 0;
		OrgEdit edit = new OrgEdit(parent, OrgEdit.TYPE.ADDHEADING, node.toString(), resolver);
		edit.write(resolver);
		node.level = tempLevel;
	}
	
	public static HashMap<String, String> getFileChecksums(ContentResolver resolver) {
		HashMap<String, String> checksums = new HashMap<String, String>();

		Cursor cursor = resolver.query(Files.CONTENT_URI, Files.DEFAULT_COLUMNS,
				null, null, null);
		cursor.moveToFirst();

		while (cursor.isAfterLast() == false) {
			OrgFile orgFile = new OrgFile(cursor);
			checksums.put(orgFile.filename, orgFile.checksum);
			cursor.moveToNext();
		}

		cursor.close();
		return checksums;
	}
	
	public static ArrayList<String> getFilenames(ContentResolver resolver) {
		ArrayList<String> checksums = new ArrayList<String>();

		Cursor cursor = resolver.query(Files.CONTENT_URI, Files.DEFAULT_COLUMNS,
				null, null, null);
		cursor.moveToFirst();

		while (cursor.isAfterLast() == false) {
			OrgFile orgFile = new OrgFile(cursor);
			checksums.add(orgFile.filename);
			cursor.moveToNext();
		}

		cursor.close();
		return checksums;
	}
	
	public static void setTodos(ArrayList<HashMap<String, Boolean>> todos,
			ContentResolver resolver) {
		resolver.delete(Todos.CONTENT_URI, null, null);

		int grouping = 0;
		for (HashMap<String, Boolean> entry : todos) {
			for (String name : entry.keySet()) {
				ContentValues values = new ContentValues();
				values.put(Todos.NAME, name);
				values.put(Todos.GROUP, grouping);

				if (entry.get(name))
					values.put(Todos.ISDONE, 1);
				resolver.insert(Todos.CONTENT_URI, values);
			}
			grouping++;
		}
	}
	public static ArrayList<String> getTodos(ContentResolver resolver) {
		Cursor cursor = resolver.query(Todos.CONTENT_URI, new String[] { Todos.NAME }, null, null, Todos.ID);
		ArrayList<String> todos = cursorToArrayList(cursor);

		cursor.close();
		return todos;
	}
	
	public static void setPriorities(ArrayList<String> priorities, ContentResolver resolver) {
		resolver.delete(Priorities.CONTENT_URI, null, null);

		for (String priority : priorities) {
			ContentValues values = new ContentValues();
			values.put(Priorities.NAME, priority);
			resolver.insert(Priorities.CONTENT_URI, values);
		}
	}
	public static ArrayList<String> getPriorities(ContentResolver resolver) {
		Cursor cursor = resolver.query(Priorities.CONTENT_URI, new String[] { Priorities.NAME },
				null, null, Priorities.ID);
		ArrayList<String> priorities = cursorToArrayList(cursor);

		cursor.close();
		return priorities;
	}
	

	public static void setTags(ArrayList<String> priorities, ContentResolver resolver) {
		resolver.delete(Tags.CONTENT_URI, null, null);

		for (String priority : priorities) {
			ContentValues values = new ContentValues();
			values.put(Tags.NAME, priority);
			resolver.insert(Tags.CONTENT_URI, values);
		}
	}
	public static ArrayList<String> getTags(ContentResolver resolver) {
		Cursor cursor = resolver.query(Tags.CONTENT_URI, new String[] { Tags.NAME },
				null, null, Tags.ID);
		ArrayList<String> tags = cursorToArrayList(cursor);

		cursor.close();
		return tags;
	}
	
	public static ArrayList<String> cursorToArrayList(Cursor cursor) {
		ArrayList<String> list = new ArrayList<String>();
		cursor.moveToFirst();

		while (cursor.isAfterLast() == false) {
			list.add(cursor.getString(cursor.getColumnIndex("name")));
			cursor.moveToNext();
		}
		return list;
	}
	
	static StringBuilder nodesToString(long node_id, long level, ContentResolver resolver) {
		StringBuilder result = new StringBuilder();
		
		OrgNode node = new OrgNode(node_id, resolver);
		
		if(level != 0) { // Don't add top level file node heading
			result.append(node.toString());
			result.append("\n");
		}
		
		Cursor childrenCursor = resolver.query(OrgData.buildChildrenUri(node_id), OrgData.DEFAULT_COLUMNS, null, null, null);
		childrenCursor.moveToFirst();
				
		while(childrenCursor.isAfterLast() == false) {
			result.append(nodesToString(childrenCursor.getLong(childrenCursor
					.getColumnIndex(OrgData.ID)), level + 1, resolver));
			childrenCursor.moveToNext();
		}
		childrenCursor.close();
		return result;
	}
	
	public static void clearDB(ContentResolver resolver) {
		resolver.delete(OrgData.CONTENT_URI, null, null);
		resolver.delete(Files.CONTENT_URI, null, null);
		resolver.delete(Edits.CONTENT_URI, null, null);
	}
	
	
	public static OrgNode getOrgNodeFromFilename(String name, ContentResolver resolver) {
		OrgFile file = new OrgFile(name, resolver);
		return new OrgNode(file.nodeId, resolver);
	}
	
	public static boolean isTodoActive(String todo, ContentResolver resolver) {
		Cursor cursor = resolver.query(Todos.CONTENT_URI, Todos.DEFAULT_COLUMNS, Todos.NAME + " = ?",
				new String[] { todo }, null);		
		
		if(TextUtils.isEmpty(todo))
			return true;
		
		if(cursor.getCount() > 0) {
			cursor.moveToFirst();
			int isdone = cursor.getInt(0);
			cursor.close();
			
			if(isdone == 0)
				return true;
			else
				return false;
		}
		
		return false;
	}
	
	public static Cursor getFileSchedule(String filename, boolean calendarHabits, ContentResolver resolver) {
		OrgFile file = new OrgFile(filename, resolver);
		
		String whereQuery = OrgData.FILE_ID + "=? AND (" + OrgData.PAYLOAD + " LIKE '%<%>%')";

		if(calendarHabits)
			whereQuery += "AND NOT" + OrgData.PAYLOAD + " LIKE '%:STYLE: habit%'";
			
		Cursor cursor = resolver.query(OrgData.CONTENT_URI, OrgData.DEFAULT_COLUMNS, whereQuery,
				new String[] { Long.toString(file.id) }, null);
		cursor.moveToFirst();
		Log.d("MobileOrg", "Found " + cursor.getCount() + " entries");
		return cursor;
	}
	
	public static Cursor search(String query, ContentResolver resolver) {
		Cursor cursor = resolver.query(OrgData.CONTENT_URI, OrgData.DEFAULT_COLUMNS,
				OrgData.NAME + " LIKE ?", new String[] { query },
				OrgData.DEFAULT_SORT);
		
		return cursor;
	}
	
	public static int getChangesCount(ContentResolver resolver) {
		int changes = 0;
		Cursor cursor = resolver.query(Edits.CONTENT_URI,
				Edits.DEFAULT_COLUMNS, null, null, null);
		if(cursor != null) {
			changes += cursor.getCount();
			cursor.close();
		}
		
		long file_id = -2;
		try {
			file_id = new OrgFile(FileUtils.CAPTURE_FILE, resolver).nodeId;
		} catch (IllegalArgumentException e) {}
		cursor = resolver.query(OrgData.CONTENT_URI, OrgData.DEFAULT_COLUMNS, OrgData.FILE_ID + "=?",
				new String[] { Long.toString(file_id) }, null);
		if(cursor != null) {
			int captures = cursor.getCount();
			if(captures > 0)
				changes += captures;
			cursor.close();
		}
		
		return changes;
	}
}
