package com.matburt.mobileorg.test.Gui;

import android.app.Instrumentation;
import android.content.ContentResolver;
import android.content.Intent;
import android.test.ActivityInstrumentationTestCase2;

import com.matburt.mobileorg.Gui.Capture.EditActivity;
import com.matburt.mobileorg.Gui.Capture.LocationFragment;
import com.matburt.mobileorg.OrgData.OrgContract.OrgData;
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
		this.nodeId = node.id;

		Intent intent = new Intent();
		intent.putExtra(EditActivity.ACTIONMODE, actionMode);
		intent.putExtra(EditActivity.NODE_ID, node.id);
		setActivityIntent(intent);
		
		setActivityInitialTouchMode(false);
		this.activity = getActivity();
		
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
	
	public void testCreateSimple() {
		OrgNode node = new OrgNode();
		prepareActivityWithNode(node, EditActivity.ACTIONMODE_CREATE);
		
		OrgNode location = locationFragment.getLocation();;
		OrgNode captureNode = OrgProviderUtil
				.getOrCreateCaptureFileOrgNode(resolver);
		// TODO Y u no work?! Race condition?
		assertEquals(captureNode.id, location.id);
	}
	
	public void testAddToplevelFile() {
		OrgNode fileNode = OrgProviderUtil.getOrCreateCaptureFileOrgNode(resolver);
		prepareActivityWithNode(fileNode, EditActivity.ACTIONMODE_ADDCHILD);

		OrgNode location = locationFragment.getLocation();
		assertTrue(location.id >= 0);
		assertEquals(fileNode.id, location.id);
	}
	
	public void testAddToplevelFileWithAddChild() {
		OrgNode fileNode = OrgProviderUtil.getOrCreateCaptureFileOrgNode(resolver);
		prepareActivityWithNode(fileNode, EditActivity.ACTIONMODE_ADDCHILD);

		locationFragment.addChild(null, "");
		OrgNode location = locationFragment.getLocation();
		assertEquals(fileNode.id, location.id);
	}
	
	public void testEditNestedChild() {
		OrgNode node = OrgTestUtils.setupParentScenario(resolver);
		prepareActivityWithNode(node, EditActivity.ACTIONMODE_EDIT);

		OrgNode location = locationFragment.getLocation();
		OrgNode parent = node.getParent(resolver);
		assertEquals(parent.id, location.id);
		OrgTestUtils.cleanupParentScenario(resolver);
	}
}
