package com.matburt.mobileorg.OrgData;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;

import com.matburt.mobileorg.OrgData.OrgContract.Files;
import com.matburt.mobileorg.OrgData.OrgContract.OrgData;
import com.matburt.mobileorg.util.OrgFileNotFoundException;
import com.matburt.mobileorg.util.OrgNodeNotFoundException;

public class OrgFile {
	public static final String CAPTURE_FILE = "mobileorg.org";
	public static final String CAPTURE_FILE_ALIAS = "Captures";
	public static final String AGENDA_FILE = "agendas.org";
	public static final String AGENDA_FILE_ALIAS = "Agenda Views";
	
	public String filename = "";
	public String name = "";
	public String checksum = "";
	public boolean includeInOutline = true;
	public long id = -1;
	public long nodeId = -1;
		
	public OrgFile() {
	}
	
	public OrgFile(String filename, String name, String checksum) {
		this.checksum = checksum;
		this.filename = filename;
		
        if (name == null || name.equals("null"))
            this.name = filename;
        else
        	this.name = name;
	}
	
	public OrgFile(Cursor cursor) throws OrgFileNotFoundException {
		set(cursor);
	}
	
	public OrgFile(long id, ContentResolver resolver) throws OrgFileNotFoundException {
		Cursor cursor = resolver.query(Files.buildIdUri(id),
				Files.DEFAULT_COLUMNS, null, null, null);
		if(cursor == null || cursor.getCount() < 1)
			throw new OrgFileNotFoundException("File with id \"" + id + "\" not found");
		set(cursor);
		cursor.close();
	}
	
	public OrgFile(String filename, ContentResolver resolver) throws OrgFileNotFoundException {
		Cursor cursor = resolver.query(Files.CONTENT_URI,
				Files.DEFAULT_COLUMNS, Files.FILENAME + "=?", new String[] {filename}, null);
		if(cursor == null || cursor.getCount() <= 0)
			throw new OrgFileNotFoundException("File \"" + filename + "\" not found");
		set(cursor);
		cursor.close();
	}
	
	public void set(Cursor cursor) throws OrgFileNotFoundException {
		if (cursor != null && cursor.getCount() > 0) {
			if(cursor.isBeforeFirst() || cursor.isAfterLast())
				cursor.moveToFirst();
			this.name = cursor.getString(cursor.getColumnIndexOrThrow(Files.NAME));
			this.filename = cursor.getString(cursor.getColumnIndexOrThrow(Files.FILENAME));
			this.checksum = cursor.getString(cursor.getColumnIndexOrThrow(Files.CHECKSUM));
			this.id = cursor.getLong(cursor.getColumnIndexOrThrow(Files.ID));
			this.nodeId = cursor.getLong(cursor.getColumnIndexOrThrow(Files.NODE_ID));
		} else {
			throw new OrgFileNotFoundException(
					"Failed to create OrgFile from cursor");
		}	
	}
	
	public void write(ContentResolver resolver) {
		if (id >= 0 && doesFileExist(resolver))
			updateFile(resolver);
		else
			addFile(resolver);
	}
	
	public boolean doesFileExist(ContentResolver resolver) {
		Cursor cursor = resolver.query(Files.buildFilenameUri(filename),
				Files.DEFAULT_COLUMNS, null, null, null);
		int count = cursor.getCount();
		cursor.close();
		
		if(count > 0)
			return true;
		else
			return false;		
	}
	
	public OrgNode getOrgNode(ContentResolver resolver) {
		try {
			return new OrgNode(this.nodeId, resolver);
		} catch (OrgNodeNotFoundException e) {
			throw new IllegalStateException("Org node for file " + filename
					+ " should exist");
		}
	}
	
	public void addFile(ContentResolver resolver) {
		if(includeInOutline)
			this.nodeId = addFileOrgDataNode(resolver);
		
		this.id = addFileNode(nodeId, resolver);
		ContentValues values = new ContentValues();
		values.put(OrgData.FILE_ID, id);
		resolver.update(OrgData.buildIdUri(nodeId), values, null, null);
	}
	
	private long addFileNode(long nodeId, ContentResolver resolver) {
		ContentValues values = new ContentValues();
		values.put(Files.FILENAME, filename);
		values.put(Files.NAME, name);
		values.put(Files.CHECKSUM, checksum);
		values.put(Files.NODE_ID, nodeId);
		
		Uri uri = resolver.insert(Files.CONTENT_URI, values);
		return Long.parseLong(Files.getId(uri));
	}
	
	private long addFileOrgDataNode(ContentResolver resolver) {
		ContentValues orgdata = new ContentValues();
		orgdata.put(OrgData.NAME, name);
		orgdata.put(OrgData.TODO, "");
		orgdata.put(OrgData.PRIORITY, "");
		orgdata.put(OrgData.LEVEL, 0);
		orgdata.put(OrgData.PARENT_ID, -1);
		
		Uri uri = resolver.insert(OrgData.CONTENT_URI, orgdata);
		long nodeId = Long.parseLong(OrgData.getId(uri));
		return nodeId;
	}
	
	public long updateFile(ContentResolver resolver) {
		return -1;
	}
	
	public long removeFile(ContentResolver resolver) {
		long entriesRemoved = removeFileOrgDataNodes(resolver);
		removeFileNode(resolver);
		return entriesRemoved;
	}
	
	private long removeFileNode(ContentResolver resolver) {
		return resolver.delete(Files.buildIdUri(id), Files.NAME + "=? AND "
				+ Files.FILENAME + "=?", new String[] { name, filename });
	}
	
	private long removeFileOrgDataNodes(ContentResolver resolver) {
		int total = resolver.delete(OrgData.CONTENT_URI, OrgData.FILE_ID + "=?",
				new String[] { Long.toString(id) });
		total += resolver.delete(OrgData.buildIdUri(nodeId), null, null);
		return total;
	}
	
	public boolean isEncrypted() {
		return filename.endsWith(".gpg") || filename.endsWith(".pgp")
				|| filename.endsWith(".enc") || filename.endsWith(".asc");
	}
	
	public boolean generateEditsForFile() {
		if(filename.equals(CAPTURE_FILE))
			return false;
		if(filename.equals(AGENDA_FILE))
			return false;
		return true;
	}
	
	public boolean equals(OrgFile file) {
		if (filename.equals(file.filename) && name.equals(file.name))
			return true;
		else
			return false;
	}
	
	public String toString(ContentResolver resolver) {		
		return OrgProviderUtils.nodesToString(nodeId, 0, resolver).toString();
	}
}
