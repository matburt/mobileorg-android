package com.matburt.mobileorg.orgdata;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;

import com.matburt.mobileorg.orgdata.OrgContract.Files;
import com.matburt.mobileorg.orgdata.OrgContract.OrgData;
import com.matburt.mobileorg.orgdata.OrgContract.Timestamps;
import com.matburt.mobileorg.synchronizers.Synchronizer;
import com.matburt.mobileorg.util.FileUtils;
import com.matburt.mobileorg.util.OrgFileNotFoundException;
import com.matburt.mobileorg.util.OrgNodeNotFoundException;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.ArrayList;

public class OrgFile {
	public static final String CAPTURE_FILE = "mobileorg.org";
	public static final String CAPTURE_FILE_ALIAS = "Captures";
	public static final String AGENDA_FILE = "agendas.org";
	public static final String AGENDA_FILE_ALIAS = "Agenda Views";

	public String filename = "";
	public String name = "";
	public String comment = "";

	public long id = -1;
	public long nodeId = -1;

	public OrgFile() {
	}

	public OrgFile(String filename, String name) {
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

	/**
	 * Edit the org file on disk to incorporate new modifications
	 * @param node: Any node from the file
	 * @param context: context
	 */
	static public void updateFile(OrgNode node, Context context) {
		ContentResolver resolver = context.getContentResolver();
		try {
			OrgFile file = new OrgFile(node.fileId, resolver);
			file.updateFile(file.toString(resolver), context);
		} catch (OrgFileNotFoundException e) {
			e.printStackTrace();
		}
	}

	public void set(Cursor cursor) throws OrgFileNotFoundException {
		if (cursor != null && cursor.getCount() > 0) {
            if (cursor.isBeforeFirst() || cursor.isAfterLast())
                cursor.moveToFirst();
			this.name = OrgProviderUtils.getNonNullString(cursor,
					cursor.getColumnIndexOrThrow(Files.NAME));
			this.filename = OrgProviderUtils.getNonNullString(cursor,
					cursor.getColumnIndexOrThrow(Files.FILENAME));
			this.id = cursor.getLong(cursor.getColumnIndexOrThrow(Files.ID));
			this.nodeId = cursor.getLong(cursor.getColumnIndexOrThrow(Files.NODE_ID));
			this.comment = OrgProviderUtils.getNonNullString(cursor,
					cursor.getColumnIndexOrThrow(Files.COMMENT));
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
	 * Add the file to the DB, then create file on disk if does not exist
	 *
     * @param context
     */
    public void addFile(Context context) {
        ContentResolver resolver = context.getContentResolver();
		this.nodeId = addFileOrgDataNode(resolver);

		this.id = addFileNode(nodeId, resolver);
		ContentValues values = new ContentValues();
		values.put(OrgData.FILE_ID, id);
		resolver.update(OrgData.buildIdUri(nodeId), values, null, null);
		if(!new File(getFilePath(context)).exists()) createFile(context);
	}

	/**
	 Insert a new file node in the "Files" database
	 **/
	private long addFileNode(long nodeId, ContentResolver resolver) {
		ContentValues values = new ContentValues();
		values.put(Files.FILENAME, filename);
		values.put(Files.NAME, name);
		values.put(Files.NODE_ID, nodeId);

		Uri uri = resolver.insert(Files.CONTENT_URI, values);
//		Log.v("uri", "uri : " + uri);
		return Long.parseLong(Files.getId(uri));
	}

	/**
	 Insert the root OrgNode of this file in the "OrgData" database
	 **/
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
	 * Create a file on disk
	 * @param context
     */
	public void createFile(Context context){
		updateFile("", context);
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
        try {
            outputStream = new FileOutputStream(file);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return;
        }

        try {
            outputStream.write(content.getBytes());
            outputStream.close();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {

        }
    }

	/**
	 * 1) Remove all OrgNode(s) associated with this file from the DB
	 * 2) Remove this OrgFile node from the DB
	 * 3) Remove file from disk
     *
	 * @param context
	 * @param fromDisk: whether or not the file has to be deleted from disk
	 * @return the number of OrgData nodes removed
     */
	public long removeFile(Context context, boolean fromDisk) {
		ContentResolver resolver = context.getContentResolver();

		long entriesRemoved = removeFileOrgDataNodes(resolver);
		removeFileNode(resolver);
		if (fromDisk) new File(getFilePath(context)).delete();

        return entriesRemoved;
	}

	private long removeFileNode(ContentResolver resolver) {
		int total = 0;
		try {
			total += resolver.delete(Files.buildIdUri(id), Files.NAME + "=? AND "
					+ Files.FILENAME + "=?", new String[]{name, filename});
		} catch (IllegalArgumentException e){
			// Uri does not exist, no need to delete
		}
		return total;
    }

	/**
	 * Update this file in the DB
	 * @param resolver
	 * @param values
     * @return
     */
	public long updateFileInDB(ContentResolver resolver, ContentValues values) {
		return resolver.update(Files.buildIdUri(id), values, Files.NAME + "=? AND "
                + Files.FILENAME + "=?", new String[]{name, filename});
    }

	private long removeFileOrgDataNodes(ContentResolver resolver) {
		int total = 0;
		try{
			resolver.delete(Timestamps.CONTENT_URI, Timestamps.FILE_ID + "=?",
					new String[]{Long.toString(id)});

			total += resolver.delete(OrgData.CONTENT_URI, OrgData.FILE_ID + "=?",
					new String[]{Long.toString(id)});
			total += resolver.delete(OrgData.buildIdUri(nodeId), null, null);
		} catch (IllegalArgumentException e){
			// DB does not exist. No need to delete.
		}

//		Log.v("sync","remove all nodes : "+total);
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
		return Synchronizer.getInstance().getAbsoluteFilesDir(context) + "/" + filename;
	}

	/**
	 * Query the state of the file (conflicted or not)
	 * @return
     */
	public State getState(){
		return comment.equals("conflict") ? State.kConflict : State.kOK;
	}

	public String toString(ContentResolver resolver) {
		String result = "";
		OrgNode root = null;
		try {
			root = new OrgNode(nodeId, resolver);
			OrgNodeTree tree = new OrgNodeTree(root, resolver);
			ArrayList<OrgNode> res = OrgNodeTree.getFullNodeArray(tree, true);
			for (OrgNode node : res) {
//				Log.v("content", "content");
//				Log.v("content", node.toString());
				result += FileUtils.stripLastNewLine(node.toString()) + "\n";
			}
		} catch (OrgNodeNotFoundException e) {
			e.printStackTrace();
		}
		return result;
	}


	public enum State {
		kOK,
		kConflict
	}
}

