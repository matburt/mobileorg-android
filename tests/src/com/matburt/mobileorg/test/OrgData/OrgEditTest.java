package com.matburt.mobileorg.test.OrgData;

import java.util.ArrayList;

import android.database.Cursor;
import android.test.ProviderTestCase2;
import android.test.mock.MockContentResolver;

import com.matburt.mobileorg.OrgData.OrgEdit;
import com.matburt.mobileorg.OrgData.OrgFile;
import com.matburt.mobileorg.OrgData.OrgNode;
import com.matburt.mobileorg.OrgData.OrgProvider;
import com.matburt.mobileorg.OrgData.OrgProviderUtils;
import com.matburt.mobileorg.OrgData.OrgContract.Edits;
import com.matburt.mobileorg.test.util.OrgTestUtils;
import com.matburt.mobileorg.util.OrgNodeNotFoundException;

public class OrgEditTest extends ProviderTestCase2<OrgProvider> {

	private MockContentResolver resolver;

	public OrgEditTest() {
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
		final int numberOfEdits = 2;
		
		ArrayList<OrgEdit> generatedEdits = node.generateApplyEditNodes(editedNode,
				resolver);
		assertEquals(numberOfEdits, generatedEdits.size());
	}

	
	public void testNewHeadingSimple() throws OrgNodeNotFoundException {
		OrgFile file = OrgTestUtils.getDefaultOrgFile();
		file.write(resolver);
		
		OrgNode fileNode = new OrgNode(file.nodeId, resolver);
		
		OrgNode node = OrgTestUtils.getDefaultOrgNode();
		node.fileId = fileNode.fileId;
		node.parentId = fileNode.id;
		
		OrgEdit edit = node.createParentNewheading(resolver);
		assertEquals(OrgEdit.TYPE.ADDHEADING, edit.type);
	}
	
	
	public void testNewHeadingDefaultFile() {
		OrgNode capturefileNode = OrgProviderUtils
				.getOrCreateCaptureFile(resolver).getOrgNode(resolver);
		assertTrue(capturefileNode.fileId >= 0);

		OrgNode node = OrgTestUtils.getDefaultOrgNode();
		node.fileId = capturefileNode.fileId;
		node.parentId = capturefileNode.id;

		OrgEdit edit = node.createParentNewheading(resolver);
		assertEquals(null, edit.type);
	}

	
	public void testEditsToStringSimple() throws OrgNodeNotFoundException {
		OrgFile file = OrgTestUtils.getDefaultOrgFile();
		file.write(resolver);
		OrgNode fileNode = new OrgNode(file.nodeId, resolver);
		
		OrgNode node = OrgTestUtils.getDefaultOrgNode();
		node.fileId = fileNode.fileId;
		node.parentId = fileNode.id;
		
		node.createParentNewheading(resolver).write(resolver);
		
		node.level = 0;
		String correctEditString = new OrgEdit(fileNode,
				OrgEdit.TYPE.ADDHEADING, node.toString(), resolver).toString();
		
		String editsString = OrgEdit.editsToString(resolver);
		assertEquals(correctEditString.trim(), editsString.trim());
	}
}
