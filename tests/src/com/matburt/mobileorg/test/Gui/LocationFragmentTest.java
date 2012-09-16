package com.matburt.mobileorg.test.Gui;

import android.app.Instrumentation;
import android.content.ContentResolver;
import android.content.Intent;
import android.test.ActivityInstrumentationTestCase2;

import com.matburt.mobileorg.Gui.Capture.EditActivity;
import com.matburt.mobileorg.Gui.Capture.LocationFragment;
import com.matburt.mobileorg.OrgData.OrgContract.OrgData;
import com.matburt.mobileorg.OrgData.OrgFile;
import com.matburt.mobileorg.OrgData.OrgNode;
import com.matburt.mobileorg.OrgData.OrgProviderUtil;
import com.matburt.mobileorg.test.util.OrgTestUtils;

public class LocationFragmentTest extends ActivityInstrumentationTestCase2<EditActivity> {
	private final String LOCATION_FRAGMENT = "locationFragment";
	
	private EditActivity activity;
	private ContentResolver resolver;
	private Instrumentation instrumentation;
	
	private LocationFragment locationFragment;
	private long nodeId = -1;

	public LocationFragmentTest() {
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
		if(nodeId >= 0)
			resolver.delete(OrgData.buildIdUri(nodeId), null, null);
		this.nodeId = -1;
		super.tearDown();
	}
	
	private void prepareActivityWithNode(OrgNode node, String actionMode) {
		Intent intent = new Intent();
		intent.putExtra(EditActivity.ACTIONMODE, actionMode);
		intent.putExtra(EditActivity.NODE_ID, node.id);
		setActivityIntent(intent);
		
		setActivityInitialTouchMode(false);
		this.activity = getActivity();
		this.resolver = activity.getContentResolver();
		
		this.locationFragment = ((LocationFragment) this.activity
				.getSupportFragmentManager().findFragmentByTag(LOCATION_FRAGMENT));
	}
	
	public void testSetup() {
		OrgNode node = OrgTestUtils.getDefaultOrgNode();
		node.write(resolver);
		this.nodeId = node.id;
		
		prepareActivityWithNode(node, EditActivity.ACTIONMODE_EDIT);
		
		assertNotNull(this.activity);
		assertNotNull(this.locationFragment);
	}
	
	public void test_Create_Simple() {
		OrgNode node = new OrgNode();
		
		prepareActivityWithNode(node, EditActivity.ACTIONMODE_CREATE);
		OrgNode locationNode = locationFragment.getLocationSelection();
		
		OrgNode captureFile = OrgProviderUtil.getOrCreateCaptureFile(resolver).getOrgNode(resolver);
		assertEquals(captureFile.fileId, locationNode.fileId);
		assertEquals(captureFile.id, locationNode.id);
	}
	
	public void test_Addchild_ToplevelFile() {
		OrgFile file = OrgProviderUtil.getOrCreateFile("test file.org", "delete me", resolver);
		OrgNode fileNode = file.getOrgNode(resolver);
		
		prepareActivityWithNode(fileNode, EditActivity.ACTIONMODE_ADDCHILD);
		OrgNode locationNode = locationFragment.getLocationSelection();
				
		file.removeFile();
		assertEquals(fileNode.name, locationNode.name);
		assertEquals(fileNode.id, locationNode.id);
		assertEquals(fileNode.fileId, locationNode.fileId);
	}
	
	public void test_Addchild_ToplevelFileWithAddChild() {
		OrgNode fileNode = OrgProviderUtil.getOrCreateCaptureFile(resolver).getOrgNode(resolver);
		
		prepareActivityWithNode(fileNode, EditActivity.ACTIONMODE_ADDCHILD);
		locationFragment.addChild(null, "");
		OrgNode locationNode = locationFragment.getLocationSelection();

		assertEquals(fileNode.id, locationNode.id);
		assertEquals(fileNode.fileId, locationNode.fileId);
	}
	
	public void test_Addchild_NestedChild() {
		OrgNode node = OrgTestUtils.setupParentScenario(resolver);
		
		prepareActivityWithNode(node, EditActivity.ACTIONMODE_ADDCHILD);
		OrgNode locationNode = locationFragment.getLocationSelection();
		
		OrgTestUtils.cleanupParentScenario(resolver);
		assertEquals(node.id, locationNode.id);
		assertEquals(node.fileId, locationNode.fileId);
	}

	public void test_Edit_NestedChild() {
		OrgNode node = OrgTestUtils.setupParentScenario(resolver);
		
		prepareActivityWithNode(node, EditActivity.ACTIONMODE_EDIT);
		OrgNode locationNode = locationFragment.getLocationSelection();
		
		OrgTestUtils.cleanupParentScenario(resolver);
		assertEquals(node.parentId, locationNode.id);
		assertEquals(node.fileId, locationNode.fileId);
	}
}
