package com.matburt.mobileorg.OrgData;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;

import com.matburt.mobileorg.OrgData.OrgContract.Edits;
import com.matburt.mobileorg.OrgData.OrgContract.Files;
import com.matburt.mobileorg.OrgData.OrgContract.OrgData;
import com.matburt.mobileorg.OrgData.OrgContract.Search;
import com.matburt.mobileorg.OrgData.OrgDatabase.Tables;
import com.matburt.mobileorg.util.SelectionBuilder;

public class OrgProvider extends ContentProvider {
	public static final String AUTHORITY = OrgContract.CONTENT_AUTHORITY;
	private OrgDatabase dbHelper;
	private static final UriMatcher uriMatcher = buildUriMatcher();
	
	private static final int ORGDATA = 100;
	private static final int ORGDATA_ID = 101;
	private static final int ORGDATA_PARENT = 102;
	private static final int ORGDATA_CHILDREN = 103;
	
	private static final int FILES = 200;
	private static final int FILES_ID = 201;
	private static final int FILES_FILENAME = 202;

	private static final int EDITS = 300;
	private static final int EDITS_ID = 301;

	private static final int TAGS = 400;
	private static final int TODOS = 500;
	private static final int PRIORITIES = 600;
	private static final int SEARCH = 700;
	private static final int TIMED = 710;

	private static UriMatcher buildUriMatcher() {
		final UriMatcher uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
		uriMatcher.addURI(AUTHORITY, "orgdata", ORGDATA);
		uriMatcher.addURI(AUTHORITY, "orgdata/*", ORGDATA_ID);
		uriMatcher.addURI(AUTHORITY, "orgdata/*/parent", ORGDATA_PARENT);
		uriMatcher.addURI(AUTHORITY, "orgdata/*/children", ORGDATA_CHILDREN);
		
		uriMatcher.addURI(AUTHORITY, "files", FILES);
		uriMatcher.addURI(AUTHORITY, "files/*", FILES_ID);
		uriMatcher.addURI(AUTHORITY, "files/*/filename", FILES_FILENAME);
		
		uriMatcher.addURI(AUTHORITY, "edits", EDITS);
		uriMatcher.addURI(AUTHORITY, "edits/*", EDITS_ID);
		
		uriMatcher.addURI(AUTHORITY, "tags", TAGS);
		uriMatcher.addURI(AUTHORITY, "todos", TODOS);
		uriMatcher.addURI(AUTHORITY, "priorities", PRIORITIES);
		
		uriMatcher.addURI(AUTHORITY, "search/*", SEARCH);
		uriMatcher.addURI(AUTHORITY, "timed/*", TIMED);

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
       final SQLiteDatabase db = dbHelper.getReadableDatabase();

       final SelectionBuilder builder = buildSelectionFromUri(uri);
       return builder.where(selection, selectionArgs).query(db, projection, sortOrder);
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
	public int delete(Uri uri, String selection, String[] selectionArgs) {		
		final SQLiteDatabase db = dbHelper.getWritableDatabase();
		final SelectionBuilder builder = buildSelectionFromUri(uri);
		int count = builder.where(selection, selectionArgs).delete(db);
		getContext().getContentResolver().notifyChange(uri, null);
		return count;
	}
	
	@Override
	public int update(Uri uri, ContentValues values, String selection,
			String[] selectionArgs) {
		final SQLiteDatabase db = dbHelper.getWritableDatabase();
		final SelectionBuilder builder = buildSelectionFromUri(uri);
		int count = builder.where(selection, selectionArgs).update(db, values);
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
			return builder.table(Tables.ORGDATA);
		case ORGDATA_ID:
			return builder.table(Tables.ORGDATA).where(OrgData.ID + "=?", OrgData.getId(uri));
		case ORGDATA_PARENT:
			return builder.table(Tables.ORGDATA).where(OrgData.ID + "=?", OrgData.getId(uri));
		case ORGDATA_CHILDREN:
			return builder.table(Tables.ORGDATA).where(OrgData.PARENT_ID + "=?", OrgData.getId(uri));
		case FILES:
			return builder.table(Tables.FILES);
		case FILES_ID:
			return builder.table(Tables.FILES).where(Files.ID + "=?", Files.getId(uri));
		case FILES_FILENAME:
			return builder.table(Tables.FILES).where(Files.FILENAME + "=?", Files.getFilename(uri));
		case EDITS:
			return builder.table(Tables.EDITS);
		case EDITS_ID:
			return builder.table(Tables.EDITS).where(Edits.ID + "=?", Edits.getId(uri));
		case TAGS:
			return builder.table(Tables.TAGS);
		case TODOS:
			return builder.table(Tables.TODOS);
		case PRIORITIES:
			return builder.table(Tables.PRIORITIES);
		case SEARCH:
			final String search = Search.getSearchTerm(uri);
			return builder.table(Tables.ORGDATA).where("name LIKE %?%", search);
		default:
			throw new IllegalArgumentException("Unknown URI " + uri);
		}
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
