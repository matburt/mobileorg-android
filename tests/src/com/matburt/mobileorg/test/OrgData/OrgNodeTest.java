package com.matburt.mobileorg.test.OrgData;

import java.util.ArrayList;

import android.database.Cursor;
import android.test.ProviderTestCase2;
import android.test.mock.MockContentResolver;

import com.matburt.mobileorg.OrgData.OrgEdit;
import com.matburt.mobileorg.OrgData.OrgFile;
import com.matburt.mobileorg.OrgData.OrgNode;
import com.matburt.mobileorg.OrgData.OrgProvider;
import com.matburt.mobileorg.OrgData.OrgContract.Edits;
import com.matburt.mobileorg.OrgData.OrgContract.OrgData;
import com.matburt.mobileorg.test.util.OrgTestUtils;

public class OrgNodeTest extends ProviderTestCase2<OrgProvider> {

	private MockContentResolver resolver;

	public OrgNodeTest() {
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
	
	public void testNodeToStringSimple() {
		OrgNode node = new OrgNode();
		node.name = "my simple test";
		node.todo = "TODO";
		node.level = 3;
		
		assertEquals("*** TODO my simple test", node.toString());
	}
	
	public void testParseLineIntoNodeSimple() {
		OrgNode node = new OrgNode();
		node.name = "my simple test";
		node.todo = "TODO";
		node.level = 3;
		OrgNode parsedNode = new OrgNode();
		final String testHeading = "*** TODO my simple test";
		parsedNode.parseLine(testHeading, 3, true);
		
		assertTrue(node.equals(parsedNode));
	}
	
	public void testAddNodeSimple() {
		OrgNode node = OrgTestUtils.getDefaultOrgNode();
		node.write(resolver);
		
		Cursor cursor = resolver.query(OrgData.buildIdUri(node.id),
				OrgData.DEFAULT_COLUMNS, null, null, null);
		assertNotNull(cursor);
		assertEquals(1, cursor.getCount());
		OrgNode insertedNode = new OrgNode(cursor);
		cursor.close();
		
		assertTrue(node.equals(insertedNode));
	}
	
	public void testAddAndUpdateNode() {
		OrgNode node = OrgTestUtils.getDefaultOrgNode();
		node.write(resolver);
		
		node.todo = "DONE";
		node.write(resolver);
		
		Cursor orgDataCursor = resolver.query(OrgData.CONTENT_URI, null, null,
				null, null);
		assertEquals(1, orgDataCursor.getCount());
		orgDataCursor.close();
		Cursor cursor = resolver.query(OrgData.buildIdUri(node.id),
				OrgData.DEFAULT_COLUMNS, null, null, null);
		assertNotNull(cursor);
		assertEquals(1, cursor.getCount());
		OrgNode insertedNode = new OrgNode(cursor);
		cursor.close();
		
		assertTrue(node.equals(insertedNode));
	}
	
	public void testGetParentSimple() {
		OrgNode node = OrgTestUtils.getDefaultOrgNode();
		node.write(resolver);
		
		OrgNode childNode = OrgTestUtils.getDefaultOrgNode();
		childNode.parentId = node.id;
		childNode.write(resolver);
		
		OrgNode parent = childNode.getParent(resolver);
		assertEquals(node.id, parent.id);
	}
	
	public void testGetParentFileNode() {
		OrgFile file = OrgTestUtils.getDefaultOrgFile();
		file.setResolver(resolver);
		file.write();
		OrgNode node = OrgTestUtils.getDefaultOrgNode();
		node.parentId = file.nodeId;
		node.write(resolver);
		
		OrgNode parent = node.getParent(resolver);
		assertEquals(file.nodeId, parent.id);
	}
	
	public void testGetParentWithTopLevel() {
		OrgFile file = OrgTestUtils.getDefaultOrgFile();
		file.setResolver(resolver);
		file.write();
		
		OrgNode node = new OrgNode(file.nodeId, resolver);
		OrgNode parent = node.getParent(resolver);
		
		assertNull(parent);
	}
	
	public void testGetChildrenSimple() {
		OrgNode node = OrgTestUtils.getDefaultOrgNode();
		node.write(resolver);
		
		OrgNode child1 = OrgTestUtils.getDefaultOrgNode();
		child1.parentId = node.id;
		child1.write(resolver);
		OrgNode child2 = OrgTestUtils.getDefaultOrgNode();
		child2.parentId = node.id;
		child2.write(resolver);
		
		ArrayList<OrgNode> children = node.getChildren(resolver);
		assertEquals(2, children.size());
	}

	public void testArchiveNode() {
		OrgNode childNode = OrgTestUtils.setupParentScenario(resolver);
		childNode.archiveNode(resolver);

		try {
			new OrgNode(childNode.id, resolver);
			fail("Node should not exist");
		} catch (IllegalArgumentException e) {}

		OrgTestUtils.cleanupParentScenario(resolver);
	}
	
	public void testArchiveNodeGeneratesEdit() {
		OrgNode node = OrgTestUtils.getDefaultOrgNode();
		node.write(resolver);
		
		Cursor editCursor = resolver.query(Edits.CONTENT_URI, Edits.DEFAULT_COLUMNS, null, null, null);
		int baseOfEdits = editCursor.getCount();
		editCursor.close();
		
		OrgEdit edit = node.archiveNode(resolver);
		edit.type.equals(OrgEdit.TYPE.ARCHIVE);
		
		Cursor editCursor2 = resolver.query(Edits.CONTENT_URI, Edits.DEFAULT_COLUMNS, null, null, null);
		int numberOfEdits = editCursor2.getCount();
		editCursor2.close();
				
		assertEquals(baseOfEdits + 1, numberOfEdits);
	}
	
	public void testArchiveNodeToSibling() {
		OrgNode childNode = OrgTestUtils.setupParentScenario(resolver);
		OrgNode parent = childNode.getParent(resolver);
		assertNotNull(parent);

		childNode.archiveNodeToSibling(resolver);
				
		OrgNode archiveNode = parent.getChild(OrgNode.ARCHIVE_NODE, resolver);
		assertNotNull(archiveNode);
		
		assertEquals(archiveNode.id, childNode.parentId);
		assertEquals(archiveNode.fileId, childNode.fileId);
	}
	
	public void testArchiveNodeToSiblingGeneratesEdit() {
		OrgNode node = OrgTestUtils.setupParentScenario(resolver);
		node.write(resolver);
		
		Cursor editCursor = resolver.query(Edits.CONTENT_URI, Edits.DEFAULT_COLUMNS, null, null, null);
		int baseOfEdits = editCursor.getCount();
		editCursor.close();
		
		OrgEdit edit = node.archiveNodeToSibling(resolver);
		edit.type.equals(OrgEdit.TYPE.ARCHIVE_SIBLING);
		
		Cursor editCursor2 = resolver.query(Edits.CONTENT_URI, Edits.DEFAULT_COLUMNS, null, null, null);
		int numberOfEdits = editCursor2.getCount();
		editCursor2.close();
				
		assertEquals(baseOfEdits + 1, numberOfEdits);
	}
}
