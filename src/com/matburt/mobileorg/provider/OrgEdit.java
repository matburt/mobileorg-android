package com.matburt.mobileorg.provider;

import com.matburt.mobileorg.provider.OrgContract.Edits;

import android.content.ContentResolver;
import android.database.Cursor;

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
	
	public TYPE type;
	public String nodeId = "";
	public String title = "";
	public String oldValue = "";
	public String newValue = "";
	
	public OrgEdit(OrgNode node, TYPE type, ContentResolver resolver) {
		this.type = type;
		this.title = node.name;
		this.nodeId = node.getNodeId(resolver);
	}
	
	public OrgEdit(Cursor cursor) {
		set(cursor);
	}
	
	public void set(Cursor cursor) {
		this.nodeId = cursor.getString(cursor.getColumnIndex(Edits.DATA_ID));
		this.title = cursor.getString(cursor.getColumnIndex(Edits.TITLE));
		this.oldValue = cursor.getString(cursor.getColumnIndex(Edits.OLD_VALUE));
		this.newValue = cursor.getString(cursor.getColumnIndex(Edits.NEW_VALUE));
		setType(cursor.getString(cursor.getColumnIndex(Edits.TYPE)));
	}	
	
	public void setType(String string) {
		this.type = TYPE.valueOf(string);
	}
	
	public String getType() {
		return type.name();
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

}
