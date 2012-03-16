package com.matburt.mobileorg.test;

import java.util.ArrayList;
import java.util.HashMap;

import android.content.Context;
import android.database.Cursor;
import android.test.AndroidTestCase;

import com.matburt.mobileorg.Parsing.OrgDatabase;


public class OrgDatabaseTest extends AndroidTestCase {

	private Context context;
	private OrgDatabase db;
	private ArrayList<String> filenames;
		
	@Override
	protected void setUp() {
		//this.context = new IsolatedContext(null, mContext);
		this.context = getContext();
		this.db = new OrgDatabase(context);
		this.filenames = getFilenames();
	}
	
	public ArrayList<String> getFilenames() {
		HashMap<String,String> files = db.getFiles();
		
		ArrayList<String> filenames = new ArrayList<String>();

		for(String filename: files.keySet())
				filenames.add(filename);
		
		return filenames;
	}
	
	public void testFileCursor() {
		Cursor fileCursor = db.getFileCursor();
		assertNotNull(fileCursor);
		assertEquals(filenames.size() - 1, fileCursor.getCount());
	}
	
	public void testFilenames() {
		for (String filename : filenames) {
			long fileID = db.getFilenameId(filename);
			String rFilename = db.getFilename(fileID);

			assertEquals(filename, rFilename);
		}
	}
	
	public void testAddAndRemoveFile() {
		final String testFilename = "testfile";
		int preFilesize = db.getFiles().size();
		db.addOrUpdateFile(testFilename, testFilename, null, false);
		assertEquals(preFilesize+1, db.getFiles().size());
		db.removeFile(testFilename);
		assertEquals(preFilesize, db.getFiles().size());
	}

	public void testFileChecksums() {
		HashMap<String,String> fileChecksums = db.getFileChecksums();
		assertEquals(filenames.size(), fileChecksums.size());
	}
}
