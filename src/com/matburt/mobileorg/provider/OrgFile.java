package com.matburt.mobileorg.provider;

import com.matburt.mobileorg.provider.OrgContract.Files;
import com.matburt.mobileorg.provider.OrgContract.OrgData;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;

public class OrgFile {
	public String filename = "";
	public String name = "";
	public String checksum = "";
	public boolean includeInOutline = true;
	public long id = -1;
	public long nodeId = -1;
	
	private ContentResolver resolver;
	
	public OrgFile() {
	}
	
	public OrgFile(String filename, String name, String checksum) {
		this.filename = filename;
		this.name = name;
		this.checksum = checksum;
	}
	
	public OrgFile(Cursor cursor) {
		set(cursor);
	}
	
	public OrgFile(long fileId, ContentResolver resolver) {
		Cursor cursor = resolver.query(Files.buildFileIdUri(fileId),
				Files.DEFAULT_COLUMNS, null, null, null);
		set(cursor);
		cursor.close();
		this.resolver = resolver;
	}
	
	public void set(Cursor cursor) {
		if (cursor != null && cursor.moveToFirst()) {
			this.name = cursor.getString(cursor.getColumnIndexOrThrow(Files.NAME));
			this.filename = cursor.getString(cursor.getColumnIndexOrThrow(Files.FILENAME));
			this.checksum = cursor.getString(cursor.getColumnIndexOrThrow(Files.CHECKSUM));
			this.id = cursor.getLong(cursor.getColumnIndexOrThrow(Files.ID));
			this.nodeId = cursor.getLong(cursor.getColumnIndexOrThrow(Files.NODE_ID));
		}
	}
	
	public void setResolver(ContentResolver resolver) {
		this.resolver = resolver;
	}
	
	public void write() {
		if (id == -1)
			addFile();
		else
			updateFile();
	}
	
	public long addFile() {
		long nodeId = -1;
		if(includeInOutline)
			this.nodeId = addFileOrgDataNode(); 
		
		this.id = addFileNode(nodeId);
		return id;
	}
	
	private long addFileNode(long nodeId) {
		ContentValues values = new ContentValues();
		values.put(Files.FILENAME, filename);
		values.put(Files.NAME, name);
		values.put(Files.CHECKSUM, checksum);
		values.put(Files.NODE_ID, nodeId);
		assert(resolver != null);
		Uri uri = resolver.insert(Files.CONTENT_URI, values);
		return Long.parseLong(Files.getId(uri));
	}
	
	private long addFileOrgDataNode() {
		ContentValues orgdata = new ContentValues();
		orgdata.put(OrgData.NAME, name);
		orgdata.put(OrgData.TODO, "");
		orgdata.put(OrgData.PARENT_ID, -1);
		assert(resolver != null);
		Uri uri = resolver.insert(OrgData.CONTENT_URI, orgdata);
		long id = Long.parseLong(OrgData.getId(uri));
		return id;
	}
	
	public long updateFile() {
		return -1;
	}
	
	public void removeFile() {
		removeFileOrgDataNodes();
		removeFileNode();
	}
	
	private long removeFileNode() {
		assert(resolver != null);
		return resolver.delete(Files.buildFileIdUri(id), Files.NAME + "=? AND "
				+ Files.FILENAME + "=?", new String[] { name, filename });
	}
	
	private long removeFileOrgDataNodes() {
		assert(resolver != null);
		return resolver.delete(OrgData.CONTENT_URI, OrgData.FILE_ID + "=?",
				new String[] { Long.toString(id) });
	}
	
	public boolean equals(OrgFile file) {
		if (filename.equals(file.filename) && name.equals(file.name))
			return true;
		else
			return false;
	}
}
