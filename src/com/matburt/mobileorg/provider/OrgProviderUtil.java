package com.matburt.mobileorg.provider;

import java.util.ArrayList;
import java.util.HashMap;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.database.Cursor;

import com.matburt.mobileorg.provider.OrgContract.Edits;
import com.matburt.mobileorg.provider.OrgContract.Files;
import com.matburt.mobileorg.provider.OrgContract.OrgData;
import com.matburt.mobileorg.provider.OrgContract.Priorities;
import com.matburt.mobileorg.provider.OrgContract.Tags;
import com.matburt.mobileorg.provider.OrgContract.Todos;
import com.matburt.mobileorg.util.FileUtils;

public class OrgProviderUtil {
	
	
	public static void createNode(OrgNode node, OrgNode parent, String newPayload, ContentResolver resolver) {
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
		
		node.payload = newPayload;
		node.write(resolver);
		
		makeNewheadingEditNode(node, parent, resolver);
	}
	
	private static void makeNewheadingEditNode(OrgNode node, OrgNode parent, ContentResolver resolver) {
		boolean generateEdit = parent != null && !parent.getFilename(resolver).equals(FileUtils.CAPTURE_FILE);
		if(generateEdit == false)
			return;

		// Add new heading nodes need the entire content of node without star headings
		long tempLevel = node.level;
		node.level = 0;
		OrgEdit edit = new OrgEdit(parent, OrgEdit.TYPE.ADDHEADING, node.toString(), resolver);
		edit.write(resolver);
		node.level = tempLevel;
	}
	
	public static HashMap<String, String> getFileChecksums(ContentResolver resolver) {
		HashMap<String, String> checksums = new HashMap<String, String>();

		Cursor cursor = resolver.query(Files.CONTENT_URI, new String[] { Files.FILENAME, Files.CHECKSUM },
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
	
	
	public static String fileToString(String filename, ContentResolver resolver) {		
		try {
			OrgFile orgFile = new OrgFile(filename, resolver);
			return nodesToString(orgFile.nodeId, 0, resolver).toString();
		} catch(IllegalArgumentException e) {
			return "";
		}
	}
	
	private static StringBuilder nodesToString(long node_id, long level, ContentResolver resolver) {
		StringBuilder result = new StringBuilder();
		
		Cursor cursor = resolver.query(OrgData.buildIdUri(node_id), OrgData.DEFAULT_COLUMNS, null, null, null);
		OrgNode node = new OrgNode(cursor);
		cursor.close();
		result.append(node.toString());
		
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

	
	public static String editsToString(ContentResolver resolver) {		
		Cursor cursor = resolver.query(Edits.CONTENT_URI,
				Edits.DEFAULT_COLUMNS, null, null, null);
		cursor.moveToFirst();

		StringBuilder result = new StringBuilder();
		while (cursor.isAfterLast() == false) {
			result.append(new OrgEdit(cursor).toString());
			cursor.moveToNext();
		}
		
		cursor.close();
		return result.toString();
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

//	public static ArrayList<HashMap<String, Integer>> getGroupedTodos(ContentResolver resolver) {
//		ArrayList<HashMap<String, Integer>> todos = new ArrayList<HashMap<String, Integer>>();
//		Cursor cursor = resolver.query(Todos.CONTENT_URI, Todos.DEFAULT_COLUMNS, null, null, Todos.GROUP);
//
//		if (cursor.getCount() > 0) {
//			HashMap<String, Integer> grouping = new HashMap<String, Integer>();
//			int resultgroup = 0;
//
//			for (cursor.moveToFirst(); cursor.isAfterLast() == false; cursor
//					.moveToNext()) {
//				// If new result group, create new grouping
//				if (resultgroup != cursor.getInt(0)) {
//					resultgroup = cursor.getInt(0);
//					todos.add(grouping);
//					grouping = new HashMap<String, Integer>();
//				}
//				// Add item to grouping
//				grouping.put(cursor.getString(1), cursor.getInt(2));
//			}
//
//			todos.add(grouping);
//		}
//
//		cursor.close();
//		return todos;
//	}
}
