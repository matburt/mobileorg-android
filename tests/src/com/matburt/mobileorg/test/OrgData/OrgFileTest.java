package com.matburt.mobileorg.test.OrgData;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;

import android.database.Cursor;
import android.test.ProviderTestCase2;
import android.test.mock.MockContentResolver;

import com.matburt.mobileorg.OrgData.OrgContract.Files;
import com.matburt.mobileorg.OrgData.OrgContract.OrgData;
import com.matburt.mobileorg.OrgData.OrgDatabase;
import com.matburt.mobileorg.OrgData.OrgFile;
import com.matburt.mobileorg.OrgData.OrgFileParser;
import com.matburt.mobileorg.OrgData.OrgNode;
import com.matburt.mobileorg.OrgData.OrgProvider;
import com.matburt.mobileorg.test.util.OrgTestFiles.SimpleOrgFiles;

public class OrgFileTest extends ProviderTestCase2<OrgProvider> {

	private MockContentResolver resolver;

	public OrgFileTest() {
		super(OrgProvider.class, OrgProvider.class.getName());
	}

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		this.resolver = getMockContentResolver();
	}
	
	@Override
	protected void tearDown() throws Exception {
	}

	public void testAddFileSimple() {
		OrgFile orgFile = new OrgFile("filename", "name", "checksum");
		orgFile.setResolver(resolver);
		orgFile.addFile();
		
		OrgFile insertedFile = new OrgFile(orgFile.id, resolver);
		assertTrue(orgFile.equals(insertedFile));
		
		Cursor dataCursor = resolver.query(
				OrgData.buildIdUri(insertedFile.nodeId),
				OrgData.DEFAULT_COLUMNS, null, null, null);
		assertNotNull(dataCursor);
		assertEquals(1, dataCursor.getCount());
		OrgNode node = new OrgNode(dataCursor);
		dataCursor.close();
		assertEquals(node.name, orgFile.name);
	}
	
	public void testRemoveFileSimple() {
		OrgFile orgFile = new OrgFile("filename", "name", "checksum");
		orgFile.setResolver(resolver);
		orgFile.addFile();
		OrgFile insertedFile = new OrgFile(orgFile.id, resolver);
		insertedFile.removeFile();
		
		Cursor filesCursor = resolver.query(Files.buildIdUri(orgFile.id),
				Files.DEFAULT_COLUMNS, null, null, null);
		assertEquals(0, filesCursor.getCount());
		filesCursor.close();
		
		Cursor dataCursor = resolver.query(OrgData.buildIdUri(insertedFile.nodeId),
				OrgData.DEFAULT_COLUMNS, null, null, null);
		assertEquals(0, dataCursor.getCount());
		dataCursor.close();
	}

	public void testFileToStringSimple() {
		final String filename = "filename";
		InputStream is = new ByteArrayInputStream(SimpleOrgFiles.orgFile.getBytes());
		BufferedReader breader = new BufferedReader(new InputStreamReader(is));
		OrgFile orgFile = new OrgFile(filename, "file alias", "");
		
		OrgDatabase db = new OrgDatabaseStub(getMockContext());
		OrgFileParser parser = new OrgFileParser(db, resolver);
		parser.parse(orgFile, breader);
		db.close();

		OrgFile file = new OrgFile(filename, resolver);
		String fileString = file.toString(resolver);
		assertEquals(SimpleOrgFiles.orgFile.trim(), fileString.trim());
	}
}	
