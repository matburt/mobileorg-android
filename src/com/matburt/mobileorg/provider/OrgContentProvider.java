package com.matburt.mobileorg.provider;

import util.SelectionBuilder;
import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;

import com.matburt.mobileorg.Parsing.OrgDatabase;
import com.matburt.mobileorg.provider.OrgContracts.OrgData;
import com.matburt.mobileorg.provider.OrgDatabase.Tables;

public class OrgContentProvider extends ContentProvider {
	public static final String AUTHORITY = "com.matburt.mobileorg.provider.OrgContentProvider";
	private OrgDatabase dbHelper;
	private static final UriMatcher uriMatcher = buildUriMatcher();
	
	private static final int ORGDATA = 100;
	private static final int ORGDATA_ID = 101;
	private static final int ORGDATA_PARENT = 102;
	private static final int ORGDATA_CHILDREN = 103;
	
	private static final int FILES = 200;
	private static final int FILES_ID = 201;
	private static final int FILES_NAME = 202;

	private static final int EDITS = 300;
	private static final int TAGS = 400;
	private static final int TODOS = 500;
	private static final int PRIORITIES = 600;

	private static UriMatcher buildUriMatcher() {
		final UriMatcher uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
		uriMatcher.addURI(AUTHORITY, "orgdata", ORGDATA);
		uriMatcher.addURI(AUTHORITY, "orgdata/*", ORGDATA_ID);
		uriMatcher.addURI(AUTHORITY, "orgdata/*/parent", ORGDATA_PARENT);
		uriMatcher.addURI(AUTHORITY, "orgdata/*/children", ORGDATA_CHILDREN);

		uriMatcher.addURI(AUTHORITY, "files", FILES);
		uriMatcher.addURI(AUTHORITY, "files/*", FILES_ID);
		uriMatcher.addURI(AUTHORITY, "files/name/*", FILES_NAME);
		
		uriMatcher.addURI(AUTHORITY, "edits", EDITS);
		uriMatcher.addURI(AUTHORITY, "tags", TAGS);
		uriMatcher.addURI(AUTHORITY, "todos", TODOS);
		uriMatcher.addURI(AUTHORITY, "priorities", PRIORITIES);
		return uriMatcher;
	}
	
	@Override
	public boolean onCreate() {
		this.dbHelper = new OrgDatabase(getContext());
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
	public Uri insert(Uri uri, ContentValues contentValues) {		
		final String tableName = getTableNameFromUri(uri);
		
		if(contentValues == null)
			contentValues = new ContentValues();
		
		SQLiteDatabase db = dbHelper.getWritableDatabase();
		long rowId = db.insert(tableName, null, contentValues);

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
	
	private SelectionBuilder buildSelectionFromUri(Uri uri) {
		final SelectionBuilder builder = new SelectionBuilder();

		switch (uriMatcher.match(uri)) {
		case ORGDATA:
			builder.table(Tables.ORGDATA);
			break;

		case ORGDATA_ID:
			builder.table(Tables.ORGDATA).where(OrgData.ID, OrgData.getId(uri));
			break;
		case FILES:
			builder.table(Tables.FILES);
			break;
		case EDITS:
			builder.table(Tables.EDITS);
			break;
		case TAGS:
			builder.table(Tables.TAGS);
			break;
		case TODOS:
			builder.table(Tables.TODOS);
			break;
		case PRIORITIES:
			builder.table(Tables.PRIORITIES);
			break;

		default:
			throw new IllegalArgumentException("Unknown URI " + uri);
		}

		return builder;
	}
	
	private String getTableNameFromUri(Uri uri) {
		String tableName = null;

		switch(uriMatcher.match(uri)) {
		case ORGDATA:
			tableName = Tables.ORGDATA;
			break;
		case FILES:
			tableName = Tables.FILES;
			break;
		case EDITS:
			tableName = Tables.EDITS;
			break;
		case TAGS:
			tableName = Tables.TAGS;
			break;
		case TODOS:
			tableName = Tables.TODOS;
			break;
		case PRIORITIES:
			tableName = Tables.PRIORITIES;
			break;
			
		default:
			throw new IllegalArgumentException("Unknown URI " + uri);
		}
		
		return tableName;
	}
}
