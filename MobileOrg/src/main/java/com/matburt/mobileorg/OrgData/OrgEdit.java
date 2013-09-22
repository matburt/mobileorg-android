package com.matburt.mobileorg.OrgData;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;

import com.matburt.mobileorg.OrgData.OrgContract.Edits;

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
		DELETE,
		ADDHEADING
	};
	
	public TYPE type = null;
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
		if (cursor != null && cursor.getCount() > 0) {
			if(cursor.isBeforeFirst() || cursor.isAfterLast())
				cursor.moveToFirst();
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
			this.oldValue = node.getPayload();
			break;
		case TAGS:
			this.oldValue = node.tags;
			break;
		default:
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
		if(this.type == null)
			return -1;
		
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

	public static String editsToString(ContentResolver resolver) {		
		Cursor cursor = resolver.query(Edits.CONTENT_URI,
				Edits.DEFAULT_COLUMNS, null, null, null);
		cursor.moveToFirst();

		StringBuilder result = new StringBuilder();
		while (cursor.isAfterLast() == false) {
			result.append(new OrgEdit(cursor).toString());
			cursor.moveToNext();
		}
		
		cursor.close();
		return result.toString();
	}

/** Legacy code for parsing edits */
	// TODO Re-enable or delete parsing of edits
//	private Pattern editTitlePattern = Pattern
//			.compile("F\\((edit:.*?)\\) \\[\\[(.*?)\\]\\[(.*?)\\]\\]");
//    
//    public ArrayList<EditNode> parseEdits() {
//        Pattern editTitlePattern = Pattern.compile("F\\((edit:.*?)\\) \\[\\[(.*?)\\]\\[(.*?)\\]\\]");
//        Pattern createTitlePattern = Pattern.compile("^\\*\\s+(.*)");
// 
//        ArrayList<EditNode> edits = new ArrayList<EditNode>();
//        OrgFile orgfile = new OrgFile(OrgFile.CAPTURE_FILE, context);
//        BufferedReader breader = orgfile.getReader();
//        if (breader == null)
//            return edits;
//
//        String thisLine;
//        boolean awaitingOldVal = false;
//        boolean awaitingNewVal = false;
//        EditNode thisNode = null;
//
//        try {
//            while ((thisLine = breader.readLine()) != null) {
//                Matcher editm = editTitlePattern.matcher(thisLine);
//                Matcher createm = createTitlePattern.matcher(thisLine);
//                if (editm.find()) {
//                    thisNode = new EditNode();
//                    if (editm.group(1) != null)
//                        thisNode.editType = editm.group(1).split(":")[1];
//                    if (editm.group(2) != null)
//                        thisNode.nodeId = editm.group(2).split(":")[1];
//                    if (editm.group(3) == null)
//                        thisNode.title = editm.group(3);
//                }
//                else if (createm.find()) {
//                }
//                else {
//                    if (thisLine.indexOf("** Old value") != -1) {
//                        awaitingOldVal = true;
//                        continue;
//                    }
//                    else if (thisLine.indexOf("** New value") != -1) {
//                        awaitingOldVal = false;
//                        awaitingNewVal = true;
//                        continue;
//                    }
//                    else if (thisLine.indexOf("** End of edit") != -1) {
//                        awaitingNewVal = false;
//                        edits.add(thisNode);
//                    }
//
//                    if (awaitingOldVal) {
//                        thisNode.oldVal += thisLine;
//                    }
//                    if (awaitingNewVal) {
//                        thisNode.newVal += thisLine;
//                    }
//                }
//            }
//        }
//        catch (java.io.IOException e) {
//            return null;
//        }
//        return edits;
//    }
}
