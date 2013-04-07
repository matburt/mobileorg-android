package com.matburt.mobileorg.test.Gui;

import android.app.Instrumentation;
import android.content.ContentResolver;
import android.content.Intent;
import android.test.ActivityInstrumentationTestCase2;

import com.matburt.mobileorg.Gui.Capture.EditActivity;
import com.matburt.mobileorg.Gui.Capture.EditActivityController;
import com.matburt.mobileorg.OrgData.OrgNode;
import com.matburt.mobileorg.OrgData.OrgContract.OrgData;
import com.matburt.mobileorg.test.util.OrgTestUtils;

public class EditActivityTest extends ActivityInstrumentationTestCase2<EditActivity> {

	private EditActivity activity;
	private ContentResolver resolver;
	private Instrumentation instrumentation;
	private long nodeId;

	public EditActivityTest() {
		super(EditActivity.class);
	}

	@Override
	public void setUp() throws Exception {
		super.setUp();

		this.instrumentation = getInstrumentation();
		this.resolver = instrumentation.getContext().getContentResolver();
	}
	
	@Override
	public void tearDown() throws Exception {
		resolver.delete(OrgData.buildIdUri(nodeId), null, null);
		super.tearDown();
	}
	
	private void prepareActivityWithNode(OrgNode node) {
		node.write(resolver);
		this.nodeId = node.id;

		Intent intent = new Intent();
		intent.putExtra(EditActivityController.ACTIONMODE, EditActivityController.ACTIONMODE_EDIT);
		intent.putExtra(EditActivityController.NODE_ID, node.id);
		setActivityIntent(intent);
		
		setActivityInitialTouchMode(false);
		this.activity = getActivity();
	}
	
	public void testSimple() {
		OrgNode node = new OrgNode();
		prepareActivityWithNode(node);
		
		assertFalse(activity.hasEdits());
		OrgNode newNode = activity.getEditedNode();
		assertTrue(node.equals(newNode));
	}
	
	public void testGetUneditedBasic() {
		OrgNode node = OrgTestUtils.getComplexOrgNode();
		prepareActivityWithNode(node);
		
		OrgNode newNode = activity.getEditedNode();
		assertTrue(node.equals(newNode));
	}
}
