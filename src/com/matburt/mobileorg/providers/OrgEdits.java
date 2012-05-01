package com.matburt.mobileorg.providers;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.net.Uri;

public class OrgEdits {
	public static final Uri CONTENT_URI = Uri.parse("content://"
            + OrgContentProvider.AUTHORITY + "/edits");
	
	private static final OrgEdits instance = new OrgEdits();

	private OrgEdits() {}

	public static OrgEdits getInstance() {
		return instance;
	}
	
	public void addNewHeading(ContentResolver contentResolver, OrgNode node) {	
	}

	public void addEdit(ContentResolver contentResolver, String edittype,
			String nodeId, String nodeTitle, String oldValue, String newValue) {
		ContentValues values = new ContentValues();
		values.put("type", edittype);
		values.put("data_id", nodeId);
		values.put("title", nodeTitle);
		values.put("old_value", oldValue);
		values.put("new_value", newValue);
		
		contentResolver.insert(OrgEdits.CONTENT_URI, values);
	}
}

