package com.matburt.mobileorg.provider;

import com.matburt.mobileorg.provider.OrgContract.Files;

import android.content.ContentResolver;
import android.database.Cursor;

public class OrgFile {
	public String filename = "";
	public String name = "";
	public String checksum = "";
	public boolean includeInOutline = true;
	
	public OrgFile() {
	}
	
	public OrgFile(Cursor cursor) {
		if (cursor != null && cursor.moveToFirst()) {
			this.name = cursor.getString(cursor.getColumnIndexOrThrow(Files.NAME));
			this.filename = cursor.getString(cursor.getColumnIndexOrThrow(Files.FILENAME));
			this.checksum = cursor.getString(cursor.getColumnIndexOrThrow(Files.CHECKSUM));
		}
	}
	
	public long getFileId(ContentResolver resolver) {
		Cursor cursor = resolver.query(Files.buildFilenameUri(filename),
				new String[] { Files.ID }, null, null, null);
		if(cursor == null || cursor.moveToFirst() == false || cursor.getColumnCount() == 0)
			throw new IllegalStateException("Could not fetch id for " + filename);
		
		return cursor.getLong(cursor.getColumnIndex(Files.ID));
	}
	
	public boolean equals(OrgFile file) {
		if (filename.equals(file.filename) && name.equals(file.name))
			return true;
		else
			return false;
	}
}
