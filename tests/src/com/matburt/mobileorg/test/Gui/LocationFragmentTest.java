package com.matburt.mobileorg.test.Gui;

import android.app.Instrumentation;
import android.content.ContentResolver;
import android.content.Intent;
import android.os.Bundle;
import android.test.ActivityInstrumentationTestCase2;

import com.matburt.mobileorg.Gui.Capture.EditActivity;
import com.matburt.mobileorg.Gui.Capture.LocationFragment;
import com.matburt.mobileorg.Gui.Capture.TagsFragment;
import com.matburt.mobileorg.OrgData.OrgContract.OrgData;
import com.matburt.mobileorg.OrgData.OrgNode;
import com.matburt.mobileorg.test.util.OrgTestUtils;

public class LocationFragmentTest extends ActivityInstrumentationTestCase2<EditActivity> {
	private final String LOCATION_FRAGMENT = "locationFragment";
	
	
	private EditActivity activity;
	private ContentResolver resolver;
	private Instrumentation instrumentation;
	
	private OrgNode node;
	private long nodeId;
	
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
		resolver.delete(OrgData.buildIdUri(nodeId), null, null);
		super.tearDown();
	}
	
	private void prepareActivityWithNode(String tags) {
		this.node = OrgTestUtils.getDefaultOrgNode();
		this.node.tags = tags;
		this.node.write(resolver);
		this.nodeId = node.id;

		Intent intent = new Intent();
		intent.putExtra(EditActivity.ACTIONMODE, EditActivity.ACTIONMODE_EDIT);
		intent.putExtra(EditActivity.NODE_ID, node.id);
		setActivityIntent(intent);
		
		setActivityInitialTouchMode(false);
		this.activity = getActivity();
	}
	
	private void saveAndRestoreState(final TagsFragment tagsFragment) {
		activity.runOnUiThread(new Runnable() {
			public void run() {
				Bundle outState = new Bundle();
				tagsFragment.onSaveInstanceState(outState);
				tagsFragment.restoreFromBundle(outState);
			}
		});
		instrumentation.waitForIdleSync();
	}
	
	public void testSetup() {
		prepareActivityWithNode("");
		assertNotNull(this.activity);
		
		LocationFragment locationFragment = ((LocationFragment) this.activity
				.getSupportFragmentManager().findFragmentByTag(LOCATION_FRAGMENT));
		assertNotNull(locationFragment);
	}
}
