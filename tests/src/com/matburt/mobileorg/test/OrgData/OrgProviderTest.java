package com.matburt.mobileorg.test.OrgData;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;

import android.database.Cursor;
import android.test.ProviderTestCase2;
import android.test.mock.MockContentResolver;

import com.matburt.mobileorg.OrgData.OrgDatabase;
import com.matburt.mobileorg.OrgData.OrgEdit;
import com.matburt.mobileorg.OrgData.OrgFile;
import com.matburt.mobileorg.OrgData.OrgFileParser;
import com.matburt.mobileorg.OrgData.OrgNode;
import com.matburt.mobileorg.OrgData.OrgProvider;
import com.matburt.mobileorg.OrgData.OrgProviderUtil;
import com.matburt.mobileorg.OrgData.OrgContract.Edits;
import com.matburt.mobileorg.OrgData.OrgContract.Files;
import com.matburt.mobileorg.OrgData.OrgContract.OrgData;
import com.matburt.mobileorg.test.util.OrgTestUtils;
import com.matburt.mobileorg.test.util.OrgTestFiles.SimpleOrgFiles;

public class OrgProviderTest extends ProviderTestCase2<OrgProvider> {

	private MockContentResolver resolver;

	public OrgProviderTest() {
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
	
	public void testAddEditSimple() {
		OrgEdit edit = new OrgEdit();
		edit.title = "title";
		edit.newValue = "new value";
		edit.oldValue = "old value";
		edit.type = OrgEdit.TYPE.HEADING;
		edit.nodeId = "node id";
		long editId = edit.write(resolver);
		
		Cursor cursor = resolver.query(Edits.buildIdUri(editId),
				Edits.DEFAULT_COLUMNS, null, null, null);
		assertNotNull(cursor);
		assertEquals(1, cursor.getCount());
		
		OrgEdit insertedEdit = new OrgEdit(cursor);
		cursor.close();
		assertTrue(edit.compare(insertedEdit));
	}
	
	public void testGenerateEditsSimple() {
		OrgNode node = OrgTestUtils.getDefaultOrgNode();
		OrgNode editedNode = OrgTestUtils.getDefaultOrgNode();
		assertTrue(node.equals(editedNode));
		editedNode.name += "2";
		editedNode.todo += "OO";
		
		ArrayList<OrgEdit> generatedEdits = node.generateEditNodes(editedNode,
				resolver);
		assertEquals(2, generatedEdits.size());
	}

	public void testChildrenUriSimple() {
		OrgNode node = OrgTestUtils.getDefaultOrgNode();
		node.write(resolver);
		
		OrgNode child1 = OrgTestUtils.getDefaultOrgNode();
		child1.parentId = node.id;
		child1.write(resolver);
		OrgNode child2 = OrgTestUtils.getDefaultOrgNode();
		child2.parentId = node.id;
		child2.write(resolver);
		
		Cursor cursor = resolver.query(OrgData.buildChildrenUri(node.id),
				OrgData.DEFAULT_COLUMNS, null, null, null);
		int numberOfChildren = cursor.getCount();
		cursor.close();
		
		assertEquals(2, numberOfChildren);
	}
	
	public void testCreateNodeNullParent() {
		OrgNode node = OrgTestUtils.getDefaultOrgNode();
		OrgProviderUtil.createNodeWithNewheadingEditnode(node, null, "", resolver);
		
		OrgNode insertedNode = new OrgNode(node.id, resolver);
		
		assertTrue(node.equals(insertedNode));
	}
	
	public void testCreateNodeNullParentMultiple() {
		OrgNode node = OrgTestUtils.getDefaultOrgNode();
		OrgNode node2 = OrgTestUtils.getDefaultOrgNode();

		OrgProviderUtil.createNodeWithNewheadingEditnode(node, null, "", resolver);
		OrgProviderUtil.createNodeWithNewheadingEditnode(node2, null, "", resolver);
		
		assertEquals(node.fileId, node2.fileId);
	}
	
	public void testCreateNodeCaptureFileNoEditsGenerated() {
		OrgNode node = OrgTestUtils.getDefaultOrgNode();
		OrgProviderUtil.createNodeWithNewheadingEditnode(node, null, "", resolver);
		
		Cursor cursor = resolver.query(Edits.CONTENT_URI,
				Edits.DEFAULT_COLUMNS, null, null, null);
		assertEquals(0, cursor.getCount());
		cursor.close();
	}
	
	public void testCreateNodeGenerateEdits() {
		OrgFile file = OrgTestUtils.getDefaultOrgFile();
		file.setResolver(resolver);
		file.write();
		
		OrgNode fileNode = new OrgNode(file.nodeId, resolver);
		
		OrgNode node = OrgTestUtils.getDefaultOrgNode();
		OrgProviderUtil.createNodeWithNewheadingEditnode(node, fileNode, "", resolver);
		
		Cursor cursor = resolver.query(Edits.CONTENT_URI,
				Edits.DEFAULT_COLUMNS, null, null, null);
		assertEquals(1, cursor.getCount());
		cursor.close();
	}
	
	public void testEditsToStringSimple() {
		OrgFile file = OrgTestUtils.getDefaultOrgFile();
		file.setResolver(resolver);
		file.write();
		
		OrgNode fileNode = new OrgNode(file.nodeId, resolver);
		
		OrgNode node = OrgTestUtils.getDefaultOrgNode();
		OrgProviderUtil.createNodeWithNewheadingEditnode(node, fileNode, "", resolver);
		
		node.level = 0;
		OrgEdit edit = new OrgEdit(fileNode, OrgEdit.TYPE.ADDHEADING, node.toString(), resolver);
		String correctEditString = edit.toString();
		
		String editsString = OrgEdit.editsToString(resolver);
		assertEquals(correctEditString.trim(), editsString.trim());
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
