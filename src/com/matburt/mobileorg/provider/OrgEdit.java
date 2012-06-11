package com.matburt.mobileorg.provider;

import com.matburt.mobileorg.provider.OrgContract.Edits;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;

public class OrgEdit {

	public enum TYPE {
		HEADING,
		TODO,
		PRIORITY,
		BODY,
		TAGS,
		REFILE,
		ARCHIVE,
		ARCHIVE_SIBLING,
		DELETE
	};
	
	public TYPE type = TYPE.HEADING;
	public String nodeId = "";
	public String title = "";
	public String oldValue = "";
	public String newValue = "";
		
	public OrgEdit() {
	}
	
	public OrgEdit(OrgNode node, TYPE type, ContentResolver resolver) {
		this.title = node.name;
		this.nodeId = node.getNodeId(resolver);
		this.type = type;
		
		setOldValue(node);
	}
	
	public OrgEdit(OrgNode node, TYPE type, String newValue, ContentResolver resolver) {
		this.title = node.name;
		this.nodeId = node.getNodeId(resolver);
		this.type = type;
		this.newValue = newValue;
		
		setOldValue(node);
	}
	
	public OrgEdit(Cursor cursor) {
		set(cursor);
	}
	
	public void set(Cursor cursor) {
		if (cursor != null && cursor.moveToFirst()) {
			this.nodeId = cursor.getString(cursor.getColumnIndexOrThrow(Edits.DATA_ID));
			this.title = cursor.getString(cursor.getColumnIndexOrThrow(Edits.TITLE));
			this.oldValue = cursor.getString(cursor.getColumnIndexOrThrow(Edits.OLD_VALUE));
			this.newValue = cursor.getString(cursor.getColumnIndexOrThrow(Edits.NEW_VALUE));
			setType(cursor.getString(cursor.getColumnIndexOrThrow(Edits.TYPE)));
		}
	}
	
	private void setOldValue(OrgNode node) {
		switch(type) {
		case TODO:
			this.oldValue = node.todo;
			break;
		case HEADING:
			this.oldValue = node.name;
			break;
		case PRIORITY:
			this.oldValue = node.priority;
			break;
		case BODY:
			this.oldValue = node.getRawPayload();
			break;
		case TAGS:
			this.oldValue = node.tags;
			break;
		}
	}
	
	
	public void setType(String string) {
		this.type = TYPE.valueOf(string.toUpperCase());
	}
	
	public String getType() {
		return type.name().toLowerCase();
	}

	public long write(ContentResolver resolver) {
		ContentValues values = new ContentValues();
		values.put(Edits.TYPE, getType());
		values.put(Edits.DATA_ID, nodeId);
		values.put(Edits.TITLE, title);
		values.put(Edits.OLD_VALUE, oldValue);
		values.put(Edits.NEW_VALUE, newValue);
		
		Uri uri = resolver.insert(Edits.CONTENT_URI, values);
		return Long.parseLong(Edits.getId(uri));
	}
	
	public String toString() {
		if (nodeId.indexOf("olp:") != 0)
			nodeId = "id:" + nodeId;
		
		StringBuilder result = new StringBuilder();
		result.append("* F(edit:" + getType() + ") [[" + nodeId + "]["
				+ title.trim() + "]]\n");
		result.append("** Old value\n" + oldValue.trim() + "\n");
		result.append("** New value\n" + newValue.trim() + "\n");
		result.append("** End of edit" + "\n\n");
				
		return result.toString().replace(":ORIGINAL_ID:", ":ID:");
	}

	public boolean compare(OrgEdit edit) {
		return type.equals(edit.type) && oldValue.equals(edit.oldValue)
				&& newValue.equals(edit.newValue) && title.equals(edit.title)
				&& nodeId.equals(edit.nodeId);
	}

}
