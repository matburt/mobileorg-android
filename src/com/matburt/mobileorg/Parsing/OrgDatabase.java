package com.matburt.mobileorg.Parsing;

import java.util.ArrayList;
import java.util.HashMap;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.DatabaseUtils.InsertHelper;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteStatement;

public class OrgDatabase extends SQLiteOpenHelper {
	private static final String DATABASE_NAME = "MobileOrg";
	private static final int DATABASE_VERSION = 12;
	
	private final static String[] nodeFields = {"_id", "name", "todo", "tags", "priority",
		"payload", "parent_id"};
	
	@SuppressWarnings("unused")
	private int orgdata_idColumn;
	private int orgdata_nameColumn;
	private int orgdata_todoColumn;
	private int orgdata_tagsColumn;
	private int orgdata_priorityColumn;
	@SuppressWarnings("unused")
	private int orgdata_payloadColumn;
	private int orgdata_parentidColumn;
	
	private Context context;
	private SQLiteDatabase db;
	private InsertHelper orgdataInsertHelper;

	public OrgDatabase(Context context) {
		super(context, DATABASE_NAME, null, DATABASE_VERSION);
		this.context = context;
		this.db = this.getWritableDatabase();
	}
	
	public SQLiteDatabase getDB() {
		return this.db;
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		db.execSQL("CREATE TABLE IF NOT EXISTS files("
				+ "_id integer primary key autoincrement,"
				+ "node_id integer," // Id that gives file node in orgdata
				+ "filename text,"
				+ "name text,"
				+ "checksum text)");
		db.execSQL("CREATE TABLE IF NOT EXISTS todos("
				+ "_id integer primary key autoincrement,"
				+ "todogroup integer,"
				+ "name text,"
				+ "isdone integer default 0)");
		db.execSQL("CREATE TABLE IF NOT EXISTS priorities("
				+ "_id integer primary key autoincrement,"
				+ "name text)");
		db.execSQL("CREATE TABLE IF NOT EXISTS edits("
				+ "_id integer primary key autoincrement,"
				+ "type text,"
				+ "title text,"
				+ "data_id integer," 
				+ "old_value text,"
				+ "new_value text,"
				+ "changed integer)");
		db.execSQL("CREATE TABLE IF NOT EXISTS orgdata ("
				+ "_id integer primary key autoincrement,"
				+ "parent_id integer,"
				+ "level integer default 0,"
				+ "node_id text," // Org data id
				+ "priority text,"
				+ "todo text,"
				+ "tags text,"
				+ "payload text,"
				+ "name text)");
	}

	public long addNode(Long parentid, String name, String todo,
			String priority, ArrayList<String> tags) {
		prepareOrgdataInsert();

		orgdataInsertHelper.bind(orgdata_parentidColumn, parentid);
		orgdataInsertHelper.bind(orgdata_nameColumn, name);
		orgdataInsertHelper.bind(orgdata_todoColumn, todo);
		orgdataInsertHelper.bind(orgdata_priorityColumn, priority);

		if (tags != null) {
			StringBuilder tagString = new StringBuilder();
			for (String tag : tags) {
				tagString.append(":");
				tagString.append(tag);
			}
			tagString.deleteCharAt(0);
			orgdataInsertHelper.bind(orgdata_tagsColumn, tagString.toString());
		}
		
		return orgdataInsertHelper.execute();
	}

	private void prepareOrgdataInsert() {
		if(this.orgdataInsertHelper == null) {
			this.orgdataInsertHelper = new InsertHelper(db, "orgdata");
			this.orgdata_idColumn = orgdataInsertHelper.getColumnIndex("_id");
			this.orgdata_nameColumn = orgdataInsertHelper.getColumnIndex("name");
			this.orgdata_todoColumn = orgdataInsertHelper.getColumnIndex("todo");
			this.orgdata_priorityColumn = orgdataInsertHelper.getColumnIndex("priority");
			this.orgdata_payloadColumn = orgdataInsertHelper.getColumnIndex("payload");
			this.orgdata_parentidColumn = orgdataInsertHelper.getColumnIndex("parent_id");
		}
		orgdataInsertHelper.prepareForInsert();
	}

	
	SQLiteStatement addPayload;
	
	public void addNodePayload(Long node_id, final String payload) {
		if(addPayload == null)
			addPayload = this.db.compileStatement("UPDATE orgdata SET payload=? WHERE _id=?");
		
		addPayload.bindString(1, payload);
		addPayload.bindLong(2, node_id);
		addPayload.execute();
//		ContentValues values = new ContentValues();
//		values.put("payload", payload);
//		db.update("orgdata", values, "_id=?", new String[] {node_id.toString()});
	}

	
	public Cursor getNodeChildren(Long node_id) {
		Cursor cursor = db.query("orgdata", nodeFields, "parent_id=?",
				new String[] { node_id.toString() }, null, null, null);
		return cursor;
	}
	
	public boolean hasNodeChildren(Long node_id) {
		if(getNodeChildren(node_id).getCount() > 0)
			return true;
		else
			return false;
	}
	
	public Cursor getNode(Long id) {
		Cursor cursor = db.query("orgdata", nodeFields, "_id=?", new String[] {id.toString()} , null, null, null);
		cursor.moveToFirst();
		return cursor;
	}
	
	public Cursor search(String query) {
		Cursor cursor = db.query("orgdata", nodeFields, null, null, null, null, null);
		
		cursor.moveToFirst();
		return cursor;
	}
	
	public long getFileId(String filename) {
		Cursor cursor = db.query("files", new String[] { "node_id" },
				"filename=?", new String[] {filename}, null, null, null);
		
		if(cursor.getCount() == 0)
			return -1;
		
		cursor.moveToFirst();
		return cursor.getInt(0);
	}
	
	public void addEdit(String edittype, String nodeId, String nodeTitle,
			String oldValue, String newValue) {

	public Cursor getFileCursor() {
		// This gets all of the org file nodes
		return db.rawQuery("SELECT data.* FROM orgdata data JOIN" 
				+ "(SELECT f.node_id FROM files f) file on file.node_id = data._id;", null);
	}
	
	public long getFileId(String filename) {
		Cursor cursor = db.query("files", new String[] { "node_id" },
				"filename=?", new String[] {filename}, null, null, null);
		
		if(cursor.getCount() == 0)
			return -1;
		
		cursor.moveToFirst();
		return cursor.getInt(0);
	}
	
	public void addEdit(String edittype, String nodeId, String nodeTitle,
			String oldValue, String newValue) {

		ContentValues values = new ContentValues();
		values.put("type", edittype);
		values.put("data_it", nodeId);
		values.put("title", nodeTitle);
		values.put("old_value", oldValue);
		values.put("new_value", newValue);
		
		db.insert("edits", null, values);
	}

	// TODO Make recursive
	public String fileToString(String filename) {
		StringBuilder result = new StringBuilder();
		
		long file_id = getFileId(filename);
		
		if(file_id < 0)
			return "";
		
		Cursor cursor = getNodeChildren(file_id);
		cursor.moveToFirst();
		
		while(cursor.isAfterLast() == false) {
			result.append(nodeToString(cursor));
			cursor.moveToNext();
		}
		
		return result.toString();
	}
	
	private String nodeToString(Cursor cursor) {
		final String todo = cursor.getString(cursor.getColumnIndex("todo"));
		final String name = cursor.getString(cursor.getColumnIndex("name"));
		final String priority = cursor.getString(cursor.getColumnIndex("priority"));
		final String payload = cursor.getString(cursor.getColumnIndex("payload"));
		// TODO Add Tags
		
		StringBuilder result = new StringBuilder();
		result.append("* ");

		if (!todo.equals(""))
			result.append(todo + " ");

		if (!priority.equals(""))
			result.append("[#" + priority + "] ");

		result.append(name + "\n");

		if (payload != null && payload.length() > 0)
			result.append(payload + "\n");

		result.append("\n");
		return result.toString();
	}
	
	public String editsToString() {		
		Cursor cursor = db.query("edits", new String[] { "data_id", "title",
				"type", "old_value", "new_value" }, null, null, null, null, null);
		cursor.moveToFirst();

		StringBuilder result = new StringBuilder();
		while (cursor.isAfterLast() == false) {
			result.append(editToString(cursor.getString(0),
					cursor.getString(1), cursor.getString(2),
					cursor.getString(3), cursor.getString(4)));
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
		return result.toString();
	}
	
	public void clearChanges() {
		db.delete("edits", null, null);
	}
		
	public void clearDB() {
		db.delete("orgdata", null, null);
		db.delete("files", null, null);
	}

	public void removeFile(String filename) {
		OrgFile orgfile = new OrgFile(filename, context);
		orgfile.remove();
		
		db.delete("files", "filename = ?", new String[] { filename });
	}

	public void removeFile(Long node_id) {
		Cursor cursor = db.query("files", new String[] { "filename" },
				"node_id=?", new String[] { node_id.toString() }, null, null,
				null);
		cursor.moveToFirst();
		String filename = cursor.getString(cursor.getColumnIndex("filename"));
		cursor.close();
		removeFile(filename);
	}
	
	public void addOrUpdateFile(String filename, String name, String checksum, boolean includeInOutline) {
		ContentValues values = new ContentValues();
		values.put("filename", filename);
		values.put("name", name);
		values.put("checksum", checksum);

		ContentValues orgdata = new ContentValues();
		orgdata.put("name", name);
		orgdata.put("todo", "");
		
		db.beginTransaction();
		
		if(includeInOutline) {
			long id = db.insert("orgdata", null, orgdata);
			values.put("node_id", id);
		}
		
		db.delete("files", "filename=? AND name=?", new String[] { filename, name });
		db.insert("files", null, values);	
		
		db.setTransactionSuccessful();
		db.endTransaction();
	}

	
	public HashMap<String, String> getFiles() {
		HashMap<String, String> allFiles = new HashMap<String, String>();

		Cursor cursor = db.query("files", new String[] { "filename", "name" },
				null, null, null, null, "name");
		cursor.moveToFirst();

		while (cursor.isAfterLast() == false) {
			allFiles.put(cursor.getString(0), cursor.getString(1));
			cursor.moveToNext();
		}

		cursor.close();
		return allFiles;
	}

	public HashMap<String, String> getFileChecksums() {
		HashMap<String, String> checksums = new HashMap<String, String>();

		Cursor cursor = db.query("files", new String[] { "filename", "checksum" },
				null, null, null, null, null);
		cursor.moveToFirst();

		while (cursor.isAfterLast() == false) {
			checksums.put(cursor.getString(cursor.getColumnIndex("filename")),
					cursor.getString(cursor.getColumnIndex("checksum")));
			cursor.moveToNext();
		}

		cursor.close();
		return checksums;
	}

	public void setTodos(ArrayList<HashMap<String, Boolean>> todos) {
		db.beginTransaction();
		db.delete("todos", null, null);

		int grouping = 0;
		for (HashMap<String, Boolean> entry : todos) {
			for (String name : entry.keySet()) {
				ContentValues values = new ContentValues();
				values.put("name", name);
				values.put("todogroup", grouping);

				if (entry.get(name))
					values.put("isdone", 1);
				db.insert("todos", null, values);
			}
			grouping++;
		}

		db.setTransactionSuccessful();
		db.endTransaction();
	}

	public ArrayList<String> getTodos() {
		Cursor cursor = db.query("todos", new String[] { "name" }, null, null,
				null, null, "_id");

		ArrayList<String> todos = cursorToArrayList(cursor);

		cursor.close();
		return todos;
	}

	public ArrayList<HashMap<String, Integer>> getGroupedTodods() {
		ArrayList<HashMap<String, Integer>> todos = new ArrayList<HashMap<String, Integer>>();
		Cursor cursor = db.query("todos", new String[] { "todogroup", "name",
				"isdone" }, null, null, null, null, "todogroup");

		if (cursor.getCount() > 0) {
			HashMap<String, Integer> grouping = new HashMap<String, Integer>();
			int resultgroup = 0;

			for (cursor.moveToFirst(); cursor.isAfterLast() == false; cursor
					.moveToNext()) {
				// If new result group, create new grouping
				if (resultgroup != cursor.getInt(0)) {
					resultgroup = cursor.getInt(0);
					todos.add(grouping);
					grouping = new HashMap<String, Integer>();
				}
				// Add item to grouping
				grouping.put(cursor.getString(1), cursor.getInt(2));
			}

			todos.add(grouping);
		}

		cursor.close();
		return todos;
	}

	public ArrayList<String> getPriorities() {
		Cursor cursor = db.query("priorities", new String[] { "name" },
				null, null, null, null, "_id");

		ArrayList<String> priorities = cursorToArrayList(cursor);

		cursor.close();
		return priorities;
	}

	public void setPriorities(ArrayList<String> priorities) {
		db.beginTransaction();
		db.delete("priorities", null, null);

		for (String priority : priorities) {
			ContentValues values = new ContentValues();
			values.put("name", priority);
			db.insert("priorities", null, values);
		}

		db.setTransactionSuccessful();
		db.endTransaction();
	}

	private ArrayList<String> cursorToArrayList(Cursor cursor) {
		ArrayList<String> list = new ArrayList<String>();
		cursor.moveToFirst();

		while (cursor.isAfterLast() == false) {
			list.add(cursor.getString(0));
			cursor.moveToNext();
		}
		return list;
	}
	
	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		switch (newVersion) {
		case 2:
			db.execSQL("DROP TABLE IF EXISTS priorities");
			break;
		case 3:
			db.execSQL("DROP TABLE IF EXISTS files");
			db.execSQL("DROP TABLE IF EXISTS todos");
			break;
		case 4:
			db.execSQL("DROP TABLE IF EXISTS files");
			break;
		case 5:
			db.execSQL("DROP TABLE IF EXISTS files");
			break;
		case 9:

			break;
		}

		db.execSQL("DROP TABLE IF EXISTS files");
		db.execSQL("DROP TABLE IF EXISTS todos");
		db.execSQL("DROP TABLE IF EXISTS priorities");
		db.execSQL("DROP TABLE IF EXISTS edits");
		db.execSQL("DROP TABLE IF EXISTS orgdata");
		onCreate(db);
	}
}