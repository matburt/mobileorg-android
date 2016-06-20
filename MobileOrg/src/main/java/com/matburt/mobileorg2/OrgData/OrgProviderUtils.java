package com.matburt.mobileorg2.OrgData;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.text.TextUtils;
import android.util.Log;

import com.matburt.mobileorg2.OrgData.OrgContract.Edits;
import com.matburt.mobileorg2.OrgData.OrgContract.Files;
import com.matburt.mobileorg2.OrgData.OrgContract.OrgData;
import com.matburt.mobileorg2.OrgData.OrgContract.Priorities;
import com.matburt.mobileorg2.OrgData.OrgContract.Tags;
import com.matburt.mobileorg2.OrgData.OrgContract.Todos;
import com.matburt.mobileorg2.util.FileUtils;
import com.matburt.mobileorg2.util.OrgFileNotFoundException;
import com.matburt.mobileorg2.util.OrgNodeNotFoundException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

public class OrgProviderUtils {

	/**
	 *
	 * @param context
	 * @return the list of nodes corresponding to a file
	 */
	public static List<OrgNode> getFileNodes(Context context){
        return OrgProviderUtils.getOrgNodeChildren(-1, context.getContentResolver());
    }


	public static ArrayList<String> getFilenames(ContentResolver resolver) {
		ArrayList<String> result = new ArrayList<String>();

		Cursor cursor = resolver.query(Files.CONTENT_URI, Files.DEFAULT_COLUMNS,
				null, null, Files.DEFAULT_SORT);
		cursor.moveToFirst();

		while (!cursor.isAfterLast()) {
			OrgFile orgFile = new OrgFile();
			
			try {
				orgFile.set(cursor);
				result.add(orgFile.filename);
			} catch (OrgFileNotFoundException e) {}
			cursor.moveToNext();
		}

		cursor.close();
		return result;
	}


	/**
	 * Query the DB for the list of files
	 * @param resolver
     * @return
     */
	public static ArrayList<OrgFile> getFiles(ContentResolver resolver) {
		ArrayList<OrgFile> result = new ArrayList<>();

		Cursor cursor = resolver.query(Files.CONTENT_URI, Files.DEFAULT_COLUMNS,
				null, null, Files.DEFAULT_SORT);
		if(cursor == null) return result;
		cursor.moveToFirst();

		while (!cursor.isAfterLast()) {
			OrgFile orgFile = new OrgFile();

			try {
				orgFile.set(cursor);
				result.add(orgFile);
			} catch (OrgFileNotFoundException e) {}
			cursor.moveToNext();
		}

		cursor.close();
		return result;
	}

	public static String getNonNullString(Cursor cursor, int index){
		String result = cursor.getString(index);
		return result!=null ? result : "";
	}


	public static void addTodos(HashMap<String, Boolean> todos,
								ContentResolver resolver) {
		if(todos == null) return;
		for (String name : todos.keySet()) {
			ContentValues values = new ContentValues();
			values.put(Todos.NAME, name);
			values.put(Todos.GROUP, 0);

			if (todos.get(name))
				values.put(Todos.ISDONE, 1);

			try{
				resolver.insert(Todos.CONTENT_URI, values);
			} catch (Exception e){
				e.printStackTrace();
			}
		}
	}
	public static ArrayList<String> getTodos(ContentResolver resolver) {
		Cursor cursor = resolver.query(Todos.CONTENT_URI, new String[] { Todos.NAME }, null, null, Todos.ID);
		if(cursor==null) return new ArrayList<>();

		ArrayList<String> todos = cursorToArrayList(cursor);

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
		ArrayList<String> list = new ArrayList<>();
		if(cursor == null) return list;
		cursor.moveToFirst();

		while (!cursor.isAfterLast()) {
			list.add(cursor.getString(0));
			cursor.moveToNext();
		}
		return list;
	}

	public static ArrayList<OrgNode> getOrgNodePathFromTopLevel(long node_id, ContentResolver resolver) {
		ArrayList<OrgNode> nodes = new ArrayList<OrgNode>();
		
		long currentId = node_id;
		while(currentId >= 0) {
			try {
				OrgNode node = new OrgNode(currentId, resolver);
				nodes.add(node);
				currentId = node.parentId;
			} catch (OrgNodeNotFoundException e) {
				throw new IllegalStateException("Couldn't build entire path to root from a given node");
			}
		}
		
		Collections.reverse(nodes);
		return nodes;
	}


	public static void clearDB(ContentResolver resolver) {
		resolver.delete(OrgData.CONTENT_URI, null, null);
		resolver.delete(Files.CONTENT_URI, null, null);
		resolver.delete(Edits.CONTENT_URI, null, null);
	}

	public static ArrayList<String> getActiveTodos(ContentResolver resolver) {
		ArrayList<String> result = new ArrayList<String>();

		Cursor cursor = resolver.query(Todos.CONTENT_URI,
				Todos.DEFAULT_COLUMNS, null, null, null);
		if(cursor == null)
			return result;
		
		cursor.moveToFirst();
		
		while (cursor.isAfterLast() == false) {
			int isdone = cursor.getInt(cursor.getColumnIndex(Todos.ISDONE));

			if (isdone == 0)
				result.add(cursor.getString(cursor.getColumnIndex(Todos.NAME)));
			cursor.moveToNext();
		}
		cursor.close();
		return result;
	}
	
	public static boolean isTodoActive(String todo, ContentResolver resolver) {
		if(TextUtils.isEmpty(todo))
			return true;

		Cursor cursor = resolver.query(Todos.CONTENT_URI, Todos.DEFAULT_COLUMNS, Todos.NAME + " = ?",
				new String[] { todo }, null);		
		
		if(cursor.getCount() > 0) {
			cursor.moveToFirst();
			int isdone = cursor.getInt(cursor.getColumnIndex(Todos.ISDONE));
			cursor.close();

			return isdone == 0;
		}

		if(cursor!=null) cursor.close();

		return false;
	}
	
	public static Cursor getFileSchedule(String filename, boolean showHabits, ContentResolver resolver) throws OrgFileNotFoundException {
		OrgFile file = new OrgFile(filename, resolver);
		
		String whereQuery = OrgData.FILE_ID + "=? AND (" + OrgData.PAYLOAD + " LIKE '%<%>%'";

		if(showHabits == false)
			whereQuery += " AND NOT " + OrgData.PAYLOAD + " LIKE '%:STYLE: habit%'";
		
		whereQuery += ")";
			
		Cursor cursor = resolver.query(OrgData.CONTENT_URI, OrgData.DEFAULT_COLUMNS, whereQuery,
				new String[] { Long.toString(file.id) }, null);
		cursor.moveToFirst();
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
		} catch (OrgFileNotFoundException e) {}
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

	public static String getChangesString(ContentResolver content) {
		int changes = OrgProviderUtils.getChangesCount(content);
		if(changes > 0)
			return "[" + changes + "]";
		else
			return "";
	}

	public static ArrayList<OrgNode> getOrgNodeChildren(long nodeId, ContentResolver resolver) {
		
		String sort = nodeId == -1 ? OrgData.NAME_SORT : OrgData.POSITION_SORT;
		Log.v("sort", "sort : " + sort);
		Cursor childCursor = resolver.query(OrgData.buildChildrenUri(nodeId),
				OrgData.DEFAULT_COLUMNS, null, null, sort);

		if(childCursor!=null) {
			ArrayList<OrgNode> result = orgDataCursorToArrayList(childCursor);
			childCursor.close();
			return result;
		}else{
			return new ArrayList<OrgNode>();
		}
	}
	
	public static ArrayList<OrgNode> orgDataCursorToArrayList(Cursor cursor) {
		ArrayList<OrgNode> result = new ArrayList<OrgNode>();

		cursor.moveToFirst();
		
		while(cursor.isAfterLast() == false) {
			try {
				result.add(new OrgNode(cursor));
			} catch (OrgNodeNotFoundException e) {}
			cursor.moveToNext();
		}
		
		return result;
	}
}
