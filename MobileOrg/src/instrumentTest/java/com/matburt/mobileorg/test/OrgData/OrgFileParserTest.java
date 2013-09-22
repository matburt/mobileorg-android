package com.matburt.mobileorg.test.OrgData;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;

import android.database.Cursor;
import android.test.ProviderTestCase2;
import android.test.mock.MockContentResolver;
import android.util.Log;

import com.matburt.mobileorg.OrgData.OrgContract.OrgData;
import com.matburt.mobileorg.OrgData.OrgFile;
import com.matburt.mobileorg.OrgData.OrgFileParser;
import com.matburt.mobileorg.OrgData.OrgNode;
import com.matburt.mobileorg.OrgData.OrgProvider;
import com.matburt.mobileorg.OrgData.OrgProviderUtils;
import com.matburt.mobileorg.test.util.OrgTestFiles;
import com.matburt.mobileorg.test.util.OrgTestFiles.OrgFileWithEmphasisedNode;
import com.matburt.mobileorg.test.util.OrgTestFiles.OrgFileWithStarNewlineNode;
import com.matburt.mobileorg.test.util.OrgTestFiles.OrgIndexWithFileDirectorySpaces;
import com.matburt.mobileorg.test.util.OrgTestFiles.SimpleOrgFiles;
import com.matburt.mobileorg.test.util.OrgTestUtils;
import com.matburt.mobileorg.util.OrgNodeNotFoundException;

public class OrgFileParserTest extends ProviderTestCase2<OrgProvider> {

	private MockContentResolver resolver;
	private OrgDatabaseStub db;
	private OrgFileParser parser;
	
	public OrgFileParserTest() {
		super(OrgProvider.class, OrgProvider.class.getName());
	}

	
	@Override
	protected void setUp() throws Exception {
		super.setUp();
		this.resolver = getMockContentResolver();
		this.db = new OrgDatabaseStub(getMockContext());
		this.parser = new OrgFileParser(db, resolver);
	}
	
	@Override
	protected void tearDown() throws Exception {
		this.db.close();
		super.tearDown();
	}
	
	public void testParseSimple() {
		Cursor cursor = resolver.query(OrgData.CONTENT_URI, OrgData.DEFAULT_COLUMNS, 
				null, null, null);
		assertNotNull(cursor);
		cursor.close();
		
		InputStream is = new ByteArrayInputStream(SimpleOrgFiles.orgFile.getBytes());
		BufferedReader breader = new BufferedReader(new InputStreamReader(is));
		OrgFile orgFile = new OrgFile("new file", "file alias", "");
		parser.parse(orgFile, breader);
		
		// it's 2 because the top file orgdata entry is not inserted through database stub
		assertEquals(2, db.fastInsertNodeCalls); 
		assertEquals(3, db.fastInsertNodePayloadCalls);

		cursor = resolver.query(OrgData.CONTENT_URI, OrgData.DEFAULT_COLUMNS,
				null, null, null);
		assertEquals(3, cursor.getCount());
		cursor.close();

		assertTrue(orgFile.id > -1);
		cursor = resolver.query(OrgData.CONTENT_URI, OrgData.DEFAULT_COLUMNS, OrgData.FILE_ID + "=?",
				new String[] { Long.toString(orgFile.id) }, OrgData.ID + " DESC");
		assertEquals(3, cursor.getCount());
		cursor.close();
	}
	
	public void testParseParentChildRelation() throws OrgNodeNotFoundException {
		InputStream is = new ByteArrayInputStream(SimpleOrgFiles.orgFile.getBytes());
		BufferedReader breader = new BufferedReader(new InputStreamReader(is));
		final String name = "file alias";
		OrgFile orgFile = new OrgFile("GTD.org", name, "");
		OrgProviderUtils.setTodos(OrgTestUtils.getTodos(), resolver);
		parser.parse(orgFile, breader);

		Cursor cursor = resolver.query(OrgData.CONTENT_URI, OrgData.DEFAULT_COLUMNS, OrgData.NAME + "=?",
				new String[] { name }, null);
		OrgNode fileNode = new OrgNode(cursor);
		cursor.close();
		
		cursor = resolver.query(OrgData.CONTENT_URI, OrgData.DEFAULT_COLUMNS, OrgData.NAME + "=?",
				new String[] { SimpleOrgFiles.orgFileTopHeading }, null);
		OrgNode topNode = new OrgNode(cursor);
		cursor.close();
		
		cursor = resolver.query(OrgData.CONTENT_URI, OrgData.DEFAULT_COLUMNS, OrgData.NAME + "=?",
				new String[] { SimpleOrgFiles.orgFileChildHeading }, null);
		OrgNode childNode = new OrgNode(cursor);
		cursor.close();

		assertEquals(-1, fileNode.parentId);
		assertEquals(fileNode.id, topNode.parentId);
		assertEquals(topNode.id, childNode.parentId);
	}
	
	public void testGetFilesFromIndex() {
		HashMap<String,String> files = OrgFileParser.getFilesFromIndex(SimpleOrgFiles.indexFile);
		
		for(String file: SimpleOrgFiles.files) {
			if(files.get(file) == null)
				fail("Didn't find all files");
		}
	}
	
	public void testGetFilesFromIndexWithSpaces() {
		final String filename = OrgIndexWithFileDirectorySpaces.filename;
		final String fileAlias = OrgIndexWithFileDirectorySpaces.fileAlias;
		HashMap<String,String> files = OrgFileParser.getFilesFromIndex(OrgIndexWithFileDirectorySpaces.indexFile);
		
		assertTrue(files.containsKey(filename));
		String retrievedFileAlias = files.get(filename);
		
		Log.d("MobileOrg", files.toString());
		
		assertEquals(fileAlias, retrievedFileAlias);
	}
	
	public void testGetFilesFromIndexWithSpacesWithoutAlias() {
		final String filename = OrgIndexWithFileDirectorySpaces.filenameWithoutAlias;
		HashMap<String,String> files = OrgFileParser.getFilesFromIndex(OrgIndexWithFileDirectorySpaces.indexFile);
		
		assertTrue(files.containsKey(filename));
		String retrievedFileAlias = files.get(filename);
		assertEquals(filename, retrievedFileAlias);
	}
	
	public void testGetTodosFromIndex() {
		ArrayList<String> tagsFromIndex = OrgFileParser.getTagsFromIndex(SimpleOrgFiles.indexFile);
		
		for(String tag: SimpleOrgFiles.tags) {
			if(tagsFromIndex.contains(tag) == false)
				fail("Didn't find all tags");
		}
	}
	
	public void testGetPrioritiesFromIndex() {
		ArrayList<String> prioritiesFromIndex = OrgFileParser.getPrioritiesFromIndex(SimpleOrgFiles.indexFile);
		
		for(String priorities: SimpleOrgFiles.priorities) {
			if(prioritiesFromIndex.contains(priorities) == false)
				fail("Didn't find all priorities");
		}
	}
	
	public void testGetTagsFromIndex() {
		ArrayList<String> tagsFromIndex = OrgFileParser.getTagsFromIndex(SimpleOrgFiles.indexFile);
		
		for(String tag: SimpleOrgFiles.tags) {
			if(tagsFromIndex.contains(tag) == false)
				fail("Didn't find all tags");
		}
	}
	
	public void testGetTagsFromIndexEmptyTags() {
		ArrayList<String> tagsFromIndex = OrgFileParser.getTagsFromIndex(OrgTestFiles.indexFileWithEmptyDrawers);
		assertEquals(0, tagsFromIndex.size());
	}
	
	
	/*
	 * Tests for bug when a *emphasised* word begins a line. The parser could
	 * mistakenly parse it as new OrgNode.
	 */
	public void testParseFileWithEmphasisNode() {
		InputStream is = new ByteArrayInputStream(OrgFileWithEmphasisedNode.orgFile.getBytes());
		BufferedReader breader = new BufferedReader(new InputStreamReader(is));
		OrgFile orgFile = new OrgFile("new file", "file alias", "");
		parser.parse(orgFile, breader);
		
		assertEquals(OrgFileWithEmphasisedNode.numberOfHeadings, db.fastInsertNodeCalls);
		assertTrue(db.fastInsertNodePayloadCalls >= 1);
	}
	
	/*
	 * Tests for bug causing crash when lines containing '*' followed by
	 * newline.
	 */
	public void testParseFileWithStarNewline() {
		InputStream is = new ByteArrayInputStream(OrgFileWithStarNewlineNode.orgFile.getBytes());
		BufferedReader breader = new BufferedReader(new InputStreamReader(is));
		OrgFile orgFile = new OrgFile("new file", "file alias", "");
		parser.parse(orgFile, breader);
		
		assertEquals(OrgFileWithStarNewlineNode.numberOfHeadings, db.fastInsertNodeCalls);
		assertTrue(db.fastInsertNodePayloadCalls >= 1);
	}
}
