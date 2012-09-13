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
import com.matburt.mobileorg.OrgData.OrgProviderUtil;
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
		assertEquals(insertedFile.id, orgFile.id);
		assertEquals(insertedFile.nodeId, orgFile.nodeId);
		
		OrgNode node = new OrgNode(orgFile.nodeId, resolver);
		assertEquals(node.name, orgFile.name);
		assertEquals(node.fileId, orgFile.id);
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
	
	public void testCreateCaptureFileOrgNode () {
		OrgNode capturefileNode = OrgProviderUtil.getOrCreateCaptureFileOrgNode(resolver);
		
		assertTrue(capturefileNode.id >= 0);
		assertTrue(capturefileNode.fileId >= 0);
		
		try {
			OrgFile file = new OrgFile(capturefileNode.fileId, resolver);
			assertEquals(OrgFile.CAPTURE_FILE, file.filename);
		} catch (IllegalArgumentException e) {
			fail("File node not created");
		}
		
		try {
			OrgNode node = new OrgNode(capturefileNode.id, resolver);
			assertEquals(OrgFile.CAPTURE_FILE_ALIAS, node.name);
		} catch (IllegalArgumentException e) {
			fail("OrgNode not created");
		}
	}
	
	public void testGetCaptureFileOrgNode () {
		OrgNode node1 = OrgProviderUtil.getOrCreateCaptureFileOrgNode(resolver);

		assertNotNull(node1);
		assertTrue(node1.id >= 0);
		assertTrue(node1.fileId >= 0);

		OrgNode node2 = OrgProviderUtil.getOrCreateCaptureFileOrgNode(resolver);
		assertNotNull(node2);

		assertEquals(node1.id, node2.id);
		assertEquals(node1.fileId, node2.fileId);
		assertEquals(node1.name, node2.name);
	}
}	
