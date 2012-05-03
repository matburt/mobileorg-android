package com.matburt.mobileorg.provider;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class OrgDatabase extends SQLiteOpenHelper {
	private static final String DATABASE_NAME = "MobileOrg.db";
	private static final int DATABASE_VERSION = 4;
	
	public interface Tables {
		String EDITS = "edits";
		String FILES = "files";
		String PRIORITIES = "priorities";
		String TAGS = "tags";
		String TODOS = "todos";
		String ORGDATA = "orgdata";
	}
	
	public OrgDatabase(Context context) {
		super(context, DATABASE_NAME, null, DATABASE_VERSION);
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
				+ "parent_id integer default -1," // orgdata:_id of parent node
				+ "file_id integer," // files:_id of file node
//				+ "node_id text," // Org data id
				+ "level integer default 0,"
				+ "priority text,"
				+ "todo text,"
				+ "tags text,"
				+ "payload text,"
				+ "name text)");
	}
	
	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		switch (newVersion) {
		case 4:
			db.execSQL("DROP TABLE IF EXISTS priorities");
			db.execSQL("DROP TABLE IF EXISTS files");
			db.execSQL("DROP TABLE IF EXISTS todos");
			db.execSQL("DROP TABLE IF EXISTS edits");
			db.execSQL("DROP TABLE IF EXISTS orgdata");
			break;
		}
		onCreate(db);
	}	
}
