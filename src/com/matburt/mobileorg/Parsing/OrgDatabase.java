package com.matburt.mobileorg.Parsing;

import java.util.ArrayList;
import java.util.HashMap;

import com.matburt.mobileorg.Services.CalendarSyncService;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.DatabaseUtils.InsertHelper;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteStatement;
import android.preference.PreferenceManager;
import android.text.TextUtils;

public class OrgDatabase extends SQLiteOpenHelper {
	private static final String DATABASE_NAME = "MobileOrg.db";
	private static final int DATABASE_VERSION = 3;
	
	private final static String[] nodeFields = {"_id", "name", "todo", "tags", "priority",
		"payload", "parent_id", "file_id"};

	@SuppressWarnings("unused")
	private int orgdata_idColumn;
	private int orgdata_nameColumn;
	private int orgdata_todoColumn;
	private int orgdata_tagsColumn;
	private int orgdata_priorityColumn;
	@SuppressWarnings("unused")
	private int orgdata_payloadColumn;
	private int orgdata_parentidColumn;
	private int orgdata_fileidColumn;
	
	private Context context;
	private SQLiteDatabase db;
	private InsertHelper orgdataInsertHelper;
	private SQLiteStatement addPayloadStatement;


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
				+ "node_id integer," //orgdata:_id of files' root node
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
		db.execSQL("CREATE TABLE IF NOT EXISTS tags("
				+ "_id integer primary key autoincrement,"
				+ "taggroup integer,"
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
				+ "parent_id integer," // orgdata:_id of parent node
				+ "file_id integer," // files:_id of file node
//				+ "node_id text," // Org data id
				+ "level integer default 0,"
				+ "priority text,"
				+ "todo text,"
				+ "tags text,"
				+ "payload text,"
				+ "name text)");
	}

	
/***************************
 * Functions for accessing the files table.
 ***************************/

	public Cursor getFileCursor() {
		// This gets all of the org file nodes
		return db
				.query("orgdata JOIN files ON (orgdata._id = files.node_id)",
						nodeJoinFields, null, null, null, null, "orgdata.name ASC");
	}
	
	private final static String[] nodeJoinFields = {"orgdata._id", "orgdata.name", "orgdata.todo", "orgdata.tags", "orgdata.priority",
		"orgdata.payload", "orgdata.parent_id", "orgdata.file_id"};
	
	
	public long getFileId(String filename) {
		Cursor cursor = db.query("files", new String[] { "node_id" },
				"filename=?", new String[] {filename}, null, null, null);
		
		if(cursor.getCount() == 0) {
			cursor.close();
			return -1;
		}
		
		cursor.moveToFirst();
		long node_id = cursor.getInt(0);
		cursor.close();
		return node_id;
	}
	
	public String getFilenameFromNodeId(Long node_id) {
		Cursor cursor = db.query("files", new String[] { "filename" },
				"node_id=?", new String[] {node_id.toString()}, null, null, null);
		
		if(cursor.getCount() == 0) {
			cursor.close();
			return "";
		}
		
		cursor.moveToFirst();
		String filename = cursor.getString(cursor.getColumnIndex("filename"));
		cursor.close();
		return filename;
	}
	
	public String getFilename(Long file_id) {
		Cursor cursor = db.query("files", new String[] { "filename" },
				"_id=?", new String[] {file_id.toString()}, null, null, null);
		
		if(cursor.getCount() == 0) {
			cursor.close();
			return "";
		}
		
		cursor.moveToFirst();
		String filename = cursor.getString(cursor.getColumnIndex("filename"));
		cursor.close();
		return filename;
	}
	
	
	public long getFilenameId(String filename) {
		Cursor cursor = db.query("files", new String[] { "_id" },
				"filename=?", new String[] {filename}, null, null, null);
		
		if(cursor.getCount() == 0) {
			cursor.close();
			return -1;
		}
		
		cursor.moveToFirst();
		long id = cursor.getInt(0);
		cursor.close();
		return id;
	}
	

	public void removeFile(String filename) {
		OrgFile orgfile = new OrgFile(filename, context);
		orgfile.remove();
		
		Long file_id = this.getFilenameId(filename);
		db.delete("orgdata", "file_id = ?", new String[] { file_id.toString() });
		db.delete("files", "filename = ?", new String[] { filename });
		
		boolean calendarEnabled = PreferenceManager
				.getDefaultSharedPreferences(context).getBoolean(
						"calendarEnabled", false);
		if(calendarEnabled)
			new CalendarSyncService(this, context).deleteFileEntries(filename, context);
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
	
	public long addOrUpdateFile(String filename, String name, String checksum, boolean includeInOutline) {
		long file_id = this.getFilenameId(filename);
	
		if(file_id >= 0)
			return file_id;

		db.beginTransaction();

		ContentValues orgdata = new ContentValues();
		orgdata.put("name", name);
		orgdata.put("todo", "");
		
		ContentValues values = new ContentValues();

		if(includeInOutline) {
			long id = db.insert("orgdata", null, orgdata);
			values.put("node_id", id);
		}
		
		values.put("filename", filename);
		values.put("name", name);
		values.put("checksum", checksum);
		
		file_id = db.insert("files", null, values);	
		
		db.setTransactionSuccessful();
		db.endTransaction();
		
		return file_id;
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
	
/***************************
 * Fast insert functions into orgdata table for synchronizing. 
 ***************************/
	
	public long addNode(Long parentid, String name, String todo,
			String priority, String tags, long file_id) {
		prepareOrgdataInsert();

		orgdataInsertHelper.bind(orgdata_parentidColumn, parentid);
		orgdataInsertHelper.bind(orgdata_nameColumn, name);
		orgdataInsertHelper.bind(orgdata_todoColumn, todo);
		orgdataInsertHelper.bind(orgdata_priorityColumn, priority);
		orgdataInsertHelper.bind(orgdata_fileidColumn, file_id);
		orgdataInsertHelper.bind(orgdata_tagsColumn, tags);
		
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
			this.orgdata_fileidColumn = orgdataInsertHelper.getColumnIndex("file_id");
			this.orgdata_tagsColumn = orgdataInsertHelper.getColumnIndex("tags");
		}
		orgdataInsertHelper.prepareForInsert();
	}
		
	public void addNodePayload(Long id, final String payload) {
		if(addPayloadStatement == null)
			addPayloadStatement = this.db
					.compileStatement("UPDATE orgdata SET payload=? WHERE _id=?");
		
		addPayloadStatement.bindString(1, payload);
		addPayloadStatement.bindLong(2, id);
		addPayloadStatement.execute();
	}
	
	
/***************************
 * Functions to access orgdata table. 
 ***************************/
	
	public Cursor getNode(Long id) {
		Cursor cursor = db.query("orgdata", nodeFields, "_id=?",
				new String[] { id.toString() }, null, null, null);
		
		cursor.moveToFirst();
		return cursor;
	}
	
	public void updateNodeField(NodeWrapper node, String entry, String value) {
		ContentValues values = new ContentValues();
		values.put(entry, value);

		String nodeId = node.getNodeId(this);
		
		if(nodeId.startsWith("olp:")) {
			db.update("orgdata", values, "_id=?",
					new String[] { new Long(node.getId()).toString() });
		} else { // Update all nodes that have this :ID:
			String nodeIdQuery = "%" + nodeId + "%";
			db.update("orgdata", values, "payload LIKE ?", new String[]{nodeIdQuery});
		}
	}
	
	/**
	 * Utility function used to retrieve the full payload of agenda items.
	 */
	public String getNodePayloadReal(String nodeId) {
		String nodeIdQuery = "%:ID:%" + nodeId + "%";
		Cursor cursor = db.query("orgdata", nodeFields, "payload LIKE ?",
				new String[] { nodeIdQuery }, null, null, null);
		
		if(cursor.getCount() == 0)
			return null;
		
		cursor.moveToFirst();
		return cursor.getString(cursor.getColumnIndex("payload"));
	}
	
	public Cursor getNodeChildren(Long id) {
		Cursor cursor = db.query("orgdata", nodeFields, "parent_id=?",
				new String[] { id.toString() }, null, null, "_id ASC");
		return cursor;
	}
	
	public boolean hasNodeChildren(Long id) {
		Cursor cursor = getNodeChildren(id);
		int childCount = cursor.getCount();
		cursor.close();
		if(childCount > 0)
			return true;
		else
			return false;
	}
	
	public boolean isNodeEditable(Long node_id) {
		Cursor cursor = db.query("files", new String[] { "_id" }, "node_id=?",
				new String[] { node_id.toString() }, null, null, null);
		int count = cursor.getCount();
		cursor.close();
		
		if(count > 0)
			return false;
		else
			return true;
	}
	
	// TODO Make recursive
	public boolean deleteNode(Long node_id) {
		int result = db.delete("orgdata", "_id=?", new String[] {node_id.toString()});
		
		if(result == 0)
			return false;
		else
			return true;
	}
	
	public void cloneNode(Long node_id, Long parent_id, Long target_file_id) {
		NodeWrapper node = new NodeWrapper(this.getNode(node_id));
		
		long new_node_id = this.addNode(parent_id, node.getName(), node.getTodo(),
				node.getPriority(), node.getTags(), target_file_id);
		
		Cursor children = this.getNodeChildren(node_id);
		children.moveToFirst();
		
		while(children.isAfterLast() == false) {
			cloneNode(children.getLong(children.getColumnIndex("_id")),
					new_node_id, target_file_id);
			children.moveToNext();
		}
		children.close();
		
		this.addNodePayload(new_node_id, node.getRawPayload(this));
		node.close();
	}
	
	public Cursor getFileSchedule(String filename) {
		long file_id = this.getFilenameId(filename);
		
		String whereQuery = "file_id=? AND (payload LIKE '%<%>%')";
		
		if (PreferenceManager.getDefaultSharedPreferences(context).getBoolean(
				"calendarHabits", true) == false)
			whereQuery += "AND NOT payload LIKE '%:STYLE: habit%'";
			
		Cursor cursor = db.query("orgdata", nodeFields, whereQuery,
				new String[] { Long.toString(file_id) }, null, null, null);
		cursor.moveToFirst();
		return cursor;
	}
	
/***************************
 * Functions with regards to edits. 
***************************/	
	
	public void addEdit(String edittype, String nodeId, String nodeTitle,
			String oldValue, String newValue) {
		// TODO Check whether to generate edits here
		ContentValues values = new ContentValues();
		values.put("type", edittype);
		values.put("data_id", nodeId);
		values.put("title", nodeTitle);
		values.put("old_value", oldValue);
		values.put("new_value", newValue);
		
		db.insert("edits", null, values);
	}

	public String fileToString(String filename) {		
		long fileNodeId = getFileId(filename);
		
		if(fileNodeId < 0)
			return "";
		
		return nodesToString(fileNodeId, 0).toString();
	}
	
	private StringBuilder nodesToString(long node_id, long level) {
		StringBuilder result = new StringBuilder();
		
		result.append(nodeToString(node_id, level));
		
		Cursor cursor = getNodeChildren(node_id);
		cursor.moveToFirst();
		
		while(cursor.isAfterLast() == false) {
			result.append(nodesToString(cursor.getLong(cursor
					.getColumnIndex("_id")), level + 1));
			cursor.moveToNext();
		}
		cursor.close();
		return result;
	}
	
	public String nodeToString(long node_id, long level) {
		// TODO Maybe add payload of file node
		if(level == 0) // This is a file node
			return "";
		
		Cursor cursor = getNode(node_id);
		final String todo = cursor.getString(cursor.getColumnIndex("todo"));
		final String name = cursor.getString(cursor.getColumnIndex("name"));
		final String priority = cursor.getString(cursor.getColumnIndex("priority"));
		final String payload = cursor.getString(cursor.getColumnIndex("payload"));
		final String tags = cursor.getString(cursor.getColumnIndex("tags"));
		cursor.close();
		
		StringBuilder result = new StringBuilder();
		
		for(int i = 0; i < level; i++)
			result.append("*");
		result.append(" ");

		if (TextUtils.isEmpty(todo) == false)
			result.append(todo + " ");

		if (TextUtils.isEmpty(priority) == false)
			result.append("[#" + priority + "] ");

		result.append(name + " ");
		
		if(tags != null && TextUtils.isEmpty(tags) == false)
			result.append(":" + tags + ":");
		
		result.append("\n");

		if (payload != null && TextUtils.isEmpty(payload) == false)
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
			result.append(editToString(
					cursor.getString(cursor.getColumnIndex("data_id")),
					cursor.getString(cursor.getColumnIndex("title")),
					cursor.getString(cursor.getColumnIndex("type")),
					cursor.getString(cursor.getColumnIndex("old_value")),
					cursor.getString(cursor.getColumnIndex("new_value"))));
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
	
	public void clearEdits() {
		db.delete("edits", null, null);
	}

	
/***************************
 * Functions to access priorities, tags and todo table. 
 ***************************/

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
	
	public boolean isTodoActive(String todo) {
		Cursor cursor = db.query("todos", new String[] {"isdone"}, "name = ?",
				new String[] { todo }, null, null, null);		
		
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

	public ArrayList<HashMap<String, Integer>> getGroupedTodos() {
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
	
	public ArrayList<String> getTags() {
		Cursor cursor = db.query("tags", new String[] { "name" },
				null, null, null, null, "_id");

		ArrayList<String> tags = cursorToArrayList(cursor);

		cursor.close();
		return tags;
	}

	public void setTags(ArrayList<String> priorities) {
		db.beginTransaction();
		db.delete("tags", null, null);

		for (String priority : priorities) {
			ContentValues values = new ContentValues();
			values.put("name", priority);
			db.insert("tags", null, values);
		}

		db.setTransactionSuccessful();
		db.endTransaction();
	}

	
/***************************
 * Misc. functions.
***************************/

	public void clearDB() {
		db.delete("orgdata", null, null);
		db.delete("files", null, null);
		db.delete("edits", null, null);
	}

	
	public Cursor search(String query) {		
		Cursor cursor = db.rawQuery(
				"SELECT * FROM orgdata WHERE name LIKE ?",
				new String[] { query });
		
		return cursor;
	}
	

	/**
	 * Handles the internal org file: links.
	 */
	public long getNodeFromPath(String path) {
		String file = path.substring("file://".length(), path.length());
		
		// TODO Handle links to headings instead of simply stripping it out
		if(file.indexOf(":") > -1)
			file = file.substring(0, file.indexOf(":"));
				
		Cursor cursor = getNode(getFileId(file));
		
		if(cursor.getCount() == 0) {
			cursor.close();
			return -1;
		}
		
		long nodeId = cursor.getLong(cursor.getColumnIndex("_id"));
		cursor.close();

		return nodeId;
	}
	
	/**
	 * This method might be useful to implement the file+headline links.
	 */
	@SuppressWarnings("unused")
	private long findNodeWithName(Cursor nodes, String name) {
		while(nodes.isAfterLast() == false) {
			String nodeName = nodes.getString(nodes.getColumnIndex("name"));
			if(nodeName.equals(name))
				return nodes.getLong(nodes.getColumnIndex("_id"));
		}
		return -1;
	}

	
	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		switch (newVersion) {
		case 2:
			db.execSQL("DROP TABLE IF EXISTS priorities");
			db.execSQL("DROP TABLE IF EXISTS files");
			db.execSQL("DROP TABLE IF EXISTS todos");
			db.execSQL("DROP TABLE IF EXISTS edits");
			db.execSQL("DROP TABLE IF EXISTS orgdata");
			break;
		}

		onCreate(db);
	}

	private ArrayList<String> cursorToArrayList(Cursor cursor) {
		ArrayList<String> list = new ArrayList<String>();
		cursor.moveToFirst();

		while (cursor.isAfterLast() == false) {
			list.add(cursor.getString(cursor.getColumnIndex("name")));
			cursor.moveToNext();
		}
		return list;
	}
	
}