package com.matburt.mobileorg.Parsing;

import java.util.ArrayList;
import java.util.HashMap;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class OrgDatabase extends SQLiteOpenHelper {
	private static final String DATABASE_NAME = "MobileOrg";
	private static final int DATABASE_VERSION = 3;

	public OrgDatabase(Context context) {
		super(context, DATABASE_NAME, null, DATABASE_VERSION);
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		db.execSQL("CREATE TABLE IF NOT EXISTS files"
				+ " (file text,"
				+ " name text,"
				+ " checksum text)");
		db.execSQL("CREATE TABLE IF NOT EXISTS todos"
				+ " (id integer primary key autoincrement,"
				+ " todogroup integer,"
				+ " name text,"
				+ " isdone integer default 0)");
		db.execSQL("CREATE TABLE IF NOT EXISTS priorities"
				+ " (id integer primary key autoincrement,"
				+ " name text)");
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
		}

		onCreate(db);
	}

	public void removeFile(String filename) {
		SQLiteDatabase db = this.getWritableDatabase();
		db.delete("files", "file = ?", new String[] { filename });
		db.close();
	}

	public void addOrUpdateFile(String file, String name, String checksum) {
		ContentValues values = new ContentValues();
		values.put("file", file);
		values.put("name", name);
		values.put("checksum", checksum);

		SQLiteDatabase db = this.getWritableDatabase();
		db.beginTransaction();
		db.delete("files", "file=? AND name=?", new String[] { file, name });
		db.insert("files", null, values);
		db.setTransactionSuccessful();
		db.endTransaction();
		db.close();
	}

	public HashMap<String, String> getFiles() {
		HashMap<String, String> allFiles = new HashMap<String, String>();
		SQLiteDatabase db = this.getReadableDatabase();

		Cursor cursor = db.query("files", new String[] { "file", "name" },
				null, null, null, null, null);
		cursor.moveToFirst();

		while (cursor.isAfterLast() == false) {
			allFiles.put(cursor.getString(0), cursor.getString(1));
			cursor.moveToNext();
		}

		cursor.close();
		db.close();
		return allFiles;
	}

	public HashMap<String, String> getFileChecksums() {
		HashMap<String, String> checksums = new HashMap<String, String>();
		SQLiteDatabase db = this.getReadableDatabase();

		Cursor cursor = db.query("files", new String[] { "file", "checksum" },
				null, null, null, null, null);
		cursor.moveToFirst();

		while (cursor.isAfterLast() == false) {
			checksums.put(cursor.getString(0), cursor.getString(1));
			cursor.moveToNext();
		}

		cursor.close();
		db.close();
		return checksums;
	}

	public void setTodos(ArrayList<HashMap<String, Boolean>> todos) {
		SQLiteDatabase db = this.getWritableDatabase();
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
		db.close();
	}

	public ArrayList<String> getTodos() {
		SQLiteDatabase db = this.getReadableDatabase();

		Cursor cursor = db.query("todos", new String[] { "name" }, null, null,
				null, null, "id");

		ArrayList<String> todos = cursorToArrayList(cursor);

		cursor.close();
		db.close();
		return todos;
	}

	public ArrayList<HashMap<String, Integer>> getGroupedTodods() {
		ArrayList<HashMap<String, Integer>> todos = new ArrayList<HashMap<String, Integer>>();
		SQLiteDatabase db = this.getReadableDatabase();
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
		db.close();
		return todos;
	}

	public ArrayList<String> getPriorities() {
		SQLiteDatabase db = this.getReadableDatabase();
		Cursor cursor = db.query("priorities", new String[] { "name", "id" },
				null, null, null, null, "id");

		ArrayList<String> priorities = cursorToArrayList(cursor);

		cursor.close();
		db.close();
		return priorities;
	}

	public void setPriorities(ArrayList<String> priorities) {
		SQLiteDatabase db = this.getWritableDatabase();
		db.beginTransaction();
		db.delete("priorities", null, null);

		for (String priority : priorities) {
			ContentValues values = new ContentValues();
			values.put("name", priority);
			db.insert("priorities", null, values);
		}

		db.setTransactionSuccessful();
		db.endTransaction();
		db.close();
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
}