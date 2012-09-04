package com.matburt.mobileorg.test.provider;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;

import android.database.Cursor;
import android.test.ProviderTestCase2;
import android.test.mock.MockContentResolver;

import com.matburt.mobileorg.Parsing.OrgFileParser;
import com.matburt.mobileorg.provider.OrgContract.OrgData;
import com.matburt.mobileorg.provider.OrgFile;
import com.matburt.mobileorg.provider.OrgNode;
import com.matburt.mobileorg.provider.OrgProvider;
import com.matburt.mobileorg.test.util.OrgTestFiles.SimpleOrgFiles;

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
	
	public void testParseParentChildRelation() {
		InputStream is = new ByteArrayInputStream(SimpleOrgFiles.orgFile.getBytes());
		BufferedReader breader = new BufferedReader(new InputStreamReader(is));
		final String name = "file alias";
		OrgFile orgFile = new OrgFile("GTD.org", name, "");
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
	
	public void testGetChecksums() {
		fail("Not implemented");
	}
	
	public void testGetFilesFromIndex() {
		fail("Not implemented");
	}
	
	public void testGetTodosFromIndex() {
		fail("Not implemented");
	}
	
	public void testGetPrioritiesFromIndex() {
		fail("Not implemented");
	}
	
	public void testGetTagsFromIndex() {
		fail("Not implemented");
	}
}
