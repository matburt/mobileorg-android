package com.matburt.mobileorg2.OrgData;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;

import com.matburt.mobileorg2.OrgData.OrgContract.Files;
import com.matburt.mobileorg2.OrgData.OrgContract.OrgData;
import com.matburt.mobileorg2.Synchronizers.Synchronizer;
import com.matburt.mobileorg2.Synchronizers.SynchronizerManager;
import com.matburt.mobileorg2.util.OrgFileNotFoundException;
import com.matburt.mobileorg2.util.OrgNodeNotFoundException;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;

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
        if (cursor == null || cursor.getCount() < 1) {
            if (cursor != null) cursor.close();
            throw new OrgFileNotFoundException("File with id \"" + id + "\" not found");
		}

		set(cursor);
		cursor.close();
	}

	public OrgFile(String filename, ContentResolver resolver) throws OrgFileNotFoundException {
		Cursor cursor = resolver.query(Files.CONTENT_URI,
                Files.DEFAULT_COLUMNS, Files.FILENAME + "=?", new String[]{filename}, null);
        if (cursor == null || cursor.getCount() <= 0) {
            if (cursor != null) cursor.close();
            throw new OrgFileNotFoundException("File \"" + filename + "\" not found");
		}

		set(cursor);
		cursor.close();
	}

	public void set(Cursor cursor) throws OrgFileNotFoundException {
		if (cursor != null && cursor.getCount() > 0) {
            if (cursor.isBeforeFirst() || cursor.isAfterLast())
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

	public boolean doesFileExist(ContentResolver resolver) {
		Cursor cursor = resolver.query(Files.buildFilenameUri(filename),
				Files.DEFAULT_COLUMNS, null, null, null);
		int count = cursor.getCount();
		cursor.close();

		return count > 0;
	}

	public OrgNode getOrgNode(ContentResolver resolver) {
		try {
			return new OrgNode(this.nodeId, resolver);
		} catch (OrgNodeNotFoundException e) {
			throw new IllegalStateException("Org node for file " + filename
					+ " should exist");
		}
	}

    /**
     * Add the file to the DB, then create file on disk, then synchronize the added file
     *
     * @param context
     */
    public void addFile(Context context) {
        ContentResolver resolver = context.getContentResolver();
        if (includeInOutline)
            this.nodeId = addFileOrgDataNode(resolver);

		this.id = addFileNode(nodeId, resolver);
		ContentValues values = new ContentValues();
		values.put(OrgData.FILE_ID, id);
		resolver.update(OrgData.buildIdUri(nodeId), values, null, null);
        updateFile("", context);
        SynchronizerManager.getInstance(null, null, null).getSyncher().addFile(filename);
    }

	/*
	Insert a new file node in the database
	 */
	private long addFileNode(long nodeId, ContentResolver resolver) {
		ContentValues values = new ContentValues();
		values.put(Files.FILENAME, filename);
		values.put(Files.NAME, name);
		values.put(Files.CHECKSUM, checksum);
		values.put(Files.NODE_ID, nodeId);

		Uri uri = resolver.insert(Files.CONTENT_URI, values);
		Log.v("uri", "uri : " + uri);
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

    /**
     * Replace the old content of the file by 'content'
     *
     * @param content the new content of the file
     * @param context
     */
    public void updateFile(String content, Context context) {
        File file = new File(getFilePath(context));
        FileOutputStream outputStream = null;
        Log.v("sync", "writing to disk a bit");
        try {
            outputStream = new FileOutputStream(file);
            Log.v("sync", "writing to disk a log");
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return;
        }

        try {
            Log.v("sync", "writing to disk a content : " + content);
            outputStream.write(content.getBytes());
            outputStream.close();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {

        }
    }

	/**
	 * Remove all OrgData nodes associated with this file from the DB
	 * Remove this OrgFile node from the DB
     *
	 * @param resolver
	 * @return the number of OrgData nodes removed
     */
    public long removeFile(Context context) {
        ContentResolver resolver = context.getContentResolver();
        new File(getFilePath(context)).delete();

		long entriesRemoved = removeFileOrgDataNodes(resolver);
		removeFileNode(resolver);
        return entriesRemoved;
	}

	private long removeFileNode(ContentResolver resolver) {
		return resolver.delete(Files.buildIdUri(id), Files.NAME + "=? AND "
                + Files.FILENAME + "=?", new String[]{name, filename});
    }

	private long removeFileOrgDataNodes(ContentResolver resolver) {
		int total = resolver.delete(OrgData.CONTENT_URI, OrgData.FILE_ID + "=?",
                new String[]{Long.toString(id)});
        total += resolver.delete(OrgData.buildIdUri(nodeId), null, null);
		return total;
	}

	public boolean isEncrypted() {
		return filename.endsWith(".gpg") || filename.endsWith(".pgp")
				|| filename.endsWith(".enc") || filename.endsWith(".asc");
	}

	public boolean equals(OrgFile file) {
		return filename.equals(file.filename) && name.equals(file.name);
    }

    /**
     * @return the absolute filename
     */
    public String getFilePath(Context context) {
        Synchronizer synchronizer = SynchronizerManager.getInstance(null, null, null).getSyncher();
        return synchronizer.getAbsoluteFilesDir(context) + "/" + filename;
    }
}
