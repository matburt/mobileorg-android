package com.matburt.mobileorg.providers;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.Uri;

public class OrgContentProvider extends ContentProvider {
	public static final String AUTHORITY = "com.matburt.mobileorg.providers.OrgContentProvider";
	private OrgDatabaseHelper dbHelper;
	private static final UriMatcher uriMatcher;
	
	public static final String DATA_TABLE_NAME = "orgdata";
	private static final int DATA_TABLE = 1;

	public static final String FILES_TABLE_NAME = "files";
	private static final int FILES_TABLE = 2;

	public static final String EDITS_TABLE_NAME = "edits";
	private static final int EDITS_TABLE = 3;
	
	public static final String TAGS_TABLE_NAME = "tags";
	private static final int TAGS_TABLE = 4;
	
	public static final String TODOS_TABLE_NAME = "todos";
	private static final int TODOS_TABLE = 5;
	
	public static final String PRIORITIES_TABLE_NAME = "priorities";
	private static final int PRIORITIES_TABLE = 6;
	
	
	@Override
	public boolean onCreate() {
		this.dbHelper = new OrgDatabaseHelper(getContext());
		return false;
	}
	

	@Override
	public Cursor query(Uri uri, String[] projection, String selection,
			String[] selectionArgs, String sortOrder) {
		final String tableName = getTableNameFromUri(uri);

        SQLiteDatabase db = dbHelper.getReadableDatabase();
		Cursor cursor = db.query(tableName, projection, selection,
				selectionArgs, null, null, sortOrder);

        cursor.setNotificationUri(getContext().getContentResolver(), uri);
        return cursor;
	}
	
	@Override
	public int delete(Uri uri, String selection, String[] selectionArgs) {
		final String tableName = getTableNameFromUri(uri);
		
		SQLiteDatabase db = dbHelper.getWritableDatabase();
		int count = db.delete(tableName, selection, selectionArgs);
		getContext().getContentResolver().notifyChange(uri, null);
		return count;
	}

	@Override
	public Uri insert(Uri uri, ContentValues initialValues) {		
		final String tableName = getTableNameFromUri(uri);
		
		ContentValues values;
		if(initialValues != null)
			values = new ContentValues(initialValues);
		else
			values = new ContentValues();
		
		SQLiteDatabase db = dbHelper.getWritableDatabase();
		long rowId = db.insert(tableName, null, values);

		if (rowId > 0) {
			Uri noteUri = ContentUris.withAppendedId(uri, rowId);
			getContext().getContentResolver().notifyChange(noteUri, null);
			return noteUri;
		} else
			throw new SQLException("Failed to insert row into " + uri);
	}

	@Override
	public int update(Uri uri, ContentValues values, String selection,
			String[] selectionArgs) {
		final String tableName = getTableNameFromUri(uri);
		
		SQLiteDatabase db = dbHelper.getWritableDatabase();
		int count = db.update(tableName, values, selection, selectionArgs);
		getContext().getContentResolver().notifyChange(uri, null);
		return count;
	}

	@Override
	public String getType(Uri uri) {
		// TODO Auto-generated method stub
		return null;
	}
	
	private String getTableNameFromUri(Uri uri) {
		String tableName = null;

		switch(uriMatcher.match(uri)) {
		case DATA_TABLE:
			tableName = DATA_TABLE_NAME;
			break;
		case FILES_TABLE:
			tableName = FILES_TABLE_NAME;
			break;
		case EDITS_TABLE:
			tableName = EDITS_TABLE_NAME;
			break;
		case TAGS_TABLE:
			tableName = TAGS_TABLE_NAME;
			break;
		case TODOS_TABLE:
			tableName = TODOS_TABLE_NAME;
			break;
		case PRIORITIES_TABLE:
			tableName = PRIORITIES_TABLE_NAME;
			break;
			
		default:
			throw new IllegalArgumentException("Unknown URI " + uri);
		}
		
		return tableName;
	}

	static {
		uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
		uriMatcher.addURI(AUTHORITY, DATA_TABLE_NAME, DATA_TABLE);
		uriMatcher.addURI(AUTHORITY, FILES_TABLE_NAME, FILES_TABLE);
		uriMatcher.addURI(AUTHORITY, EDITS_TABLE_NAME, EDITS_TABLE);
		uriMatcher.addURI(AUTHORITY, TAGS_TABLE_NAME, TAGS_TABLE);
		uriMatcher.addURI(AUTHORITY, TODOS_TABLE_NAME, TODOS_TABLE);
		uriMatcher.addURI(AUTHORITY, PRIORITIES_TABLE_NAME, PRIORITIES_TABLE);
	}


	private static class OrgDatabaseHelper extends SQLiteOpenHelper {
		private static final String DATABASE_NAME = "MobileOrg.db";
		private static final int DATABASE_VERSION = 4;
		
		public OrgDatabaseHelper(Context context) {
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
//					+ "node_id text," // Org data id
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
}
