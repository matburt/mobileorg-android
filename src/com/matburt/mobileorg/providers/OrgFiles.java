package com.matburt.mobileorg.providers;

import android.content.ContentResolver;
import android.database.Cursor;
import android.net.Uri;


public class OrgFiles {
	public static final Uri CONTENT_URI = Uri.parse("content://"
            + OrgContentProvider.AUTHORITY + "/files");
	
	public static final String ID = "_id";
	public static final String NODE_ID = "node_id";
	public static final String FILENAME = "filename";
	public static final String NAME = "name";
	public static final String CHECKSUM = "checksum";
	
	private static final OrgFiles instance = new OrgFiles();
	private OrgFiles() {
	}	
	
	public static OrgFiles getInstance() {
		return instance;
	}
	
	public long getNodeIdFromFilename(ContentResolver contentResolver, String filename) {
		long result = -1;

		Cursor cursor = contentResolver.query(OrgFiles.CONTENT_URI, null,
				OrgFiles.FILENAME + "='" + filename + "'", null, null);

		if (cursor != null) {
			if (cursor.moveToNext()) {
				int index = cursor.getColumnIndex(OrgFiles.NODE_ID);
				result = cursor.getLong(index);
			}
			cursor.close();
		}

		return result;
	}
	
	public long add(String filename, String name, String checksum, boolean includeInOutline) {
		return 0;
	}
}
