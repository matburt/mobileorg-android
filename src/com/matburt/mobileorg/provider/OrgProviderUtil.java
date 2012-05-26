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

public class OrgProviderUtil {
	
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
				values.put("name", name);
				values.put("todogroup", grouping);

				if (entry.get(name))
					values.put("isdone", 1);
				resolver.insert(Todos.CONTENT_URI, values);
			}
			grouping++;
		}
	}
	public static ArrayList<String> getTodos(ContentResolver resolver) {
		Cursor cursor = resolver.query(Todos.CONTENT_URI, new String[] { "name" }, null, null, "_id");
		ArrayList<String> todos = cursorToArrayList(cursor);

		cursor.close();
		return todos;
	}
	
	public static void setPriorities(ArrayList<String> priorities, ContentResolver resolver) {
		resolver.delete(Priorities.CONTENT_URI, null, null);

		for (String priority : priorities) {
			ContentValues values = new ContentValues();
			values.put("name", priority);
			resolver.insert(Priorities.CONTENT_URI, values);
		}
	}
	public static ArrayList<String> getPriorities(ContentResolver resolver) {
		Cursor cursor = resolver.query(Priorities.CONTENT_URI, new String[] { "name" },
				null, null, "_id");
		ArrayList<String> priorities = cursorToArrayList(cursor);

		cursor.close();
		return priorities;
	}
	

	public static void setTags(ArrayList<String> priorities, ContentResolver resolver) {
		resolver.delete(Tags.CONTENT_URI, null, null);

		for (String priority : priorities) {
			ContentValues values = new ContentValues();
			values.put("name", priority);
			resolver.insert(Tags.CONTENT_URI, values);
		}
	}
	public static ArrayList<String> getTags(ContentResolver resolver) {
		Cursor cursor = resolver.query(Tags.CONTENT_URI, new String[] { "name" },
				null, null, "_id");
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
			result.append(editToString(
					cursor.getString(cursor.getColumnIndex(Edits.DATA_ID)),
					cursor.getString(cursor.getColumnIndex(Edits.TITLE)),
					cursor.getString(cursor.getColumnIndex(Edits.TYPE)),
					cursor.getString(cursor.getColumnIndex(Edits.OLD_VALUE)),
					cursor.getString(cursor.getColumnIndex(Edits.NEW_VALUE))));
			cursor.moveToNext();
		}
		
		cursor.close();
		return result.toString();
	}

	private static String editToString(String nodeId, String title, String editType,
			String oldVal, String newVal) {
		if (nodeId.indexOf("olp:") != 0)
			nodeId = "id:" + nodeId;
		
		StringBuilder result = new StringBuilder();
		result.append("* F(edit:" + editType + ") [[" + nodeId + "]["
				+ title.trim() + "]]\n");
		result.append("** Old value\n" + oldVal.trim() + "\n");
		result.append("** New value\n" + newVal.trim() + "\n");
		result.append("** End of edit" + "\n\n");
				
		return result.toString().replace(":ORIGINAL_ID:", ":ID:");
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
