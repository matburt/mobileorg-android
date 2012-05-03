package com.matburt.mobileorg.provider;

import android.database.Cursor;

import com.matburt.mobileorg.Parsing.NodePayload;
import com.matburt.mobileorg.provider.OrgContract.OrgData;

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
	
	public OrgNode(Cursor cursor) {
		id = cursor.getLong(cursor.getColumnIndex("_id"));
		parentId = cursor.getLong(cursor.getColumnIndex(OrgData.PARENT_ID));
		fileId = cursor.getLong(cursor.getColumnIndex(OrgData.FILE_ID));
		level = cursor.getLong(cursor.getColumnIndex(OrgData.LEVEL));
		priority = cursor.getString(cursor.getColumnIndex(OrgData.PRIORITY));
		todo = cursor.getString(cursor.getColumnIndex(OrgData.TODO));
		tags = cursor.getString(cursor.getColumnIndex(OrgData.TAGS));
		name = cursor.getString(cursor.getColumnIndex(OrgData.NAME));
	}
	
	public NodePayload getPayload() {
		return this.payload;
	}
}
