package com.matburt.mobileorg.providers;

import android.content.ContentResolver;
import android.database.Cursor;

import com.matburt.mobileorg.Parsing.NodePayload;

public class OrgNode {
	public long id = -1;
	public long parentId = -1;
	public long fileId = -1;
	public long level = 0;
	public String priority = "";
	public String todo = "";
	public String tags = "";
	public String name = "";
	private NodePayload payload;

	public OrgNode() {
	}
	
	public boolean generateEdits(OrgNode oldNode) {
		return false;
	}
	
	public String getFilename(ContentResolver contentResolver) {
		String result = null;
		Cursor cursor = contentResolver.query(OrgFiles.CONTENT_URI,
				new String[] { OrgFiles.FILENAME }, OrgFiles.ID + "='?'",
				new String[] { Long.toString(fileId) }, null);
		
		if(cursor != null) {
			if(cursor.moveToNext())
				result = cursor.getString(cursor.getColumnIndex(OrgFiles.FILENAME));
			cursor.close();
		}
		
		if(result == null)
			throw new IllegalStateException("Could not find file with id " + fileId);
		
		return result;
	}
}
