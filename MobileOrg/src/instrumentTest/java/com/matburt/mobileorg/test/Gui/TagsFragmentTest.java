package com.matburt.mobileorg.test.Gui;

import android.app.Instrumentation;
import android.content.ContentResolver;
import android.content.Intent;
import android.os.Bundle;
import android.test.ActivityInstrumentationTestCase2;

import com.matburt.mobileorg.Gui.Capture.EditActivity;
import com.matburt.mobileorg.Gui.Capture.EditActivityController;
import com.matburt.mobileorg.Gui.Capture.TagsFragment;
import com.matburt.mobileorg.OrgData.OrgContract.OrgData;
import com.matburt.mobileorg.OrgData.OrgNode;
import com.matburt.mobileorg.test.util.OrgTestUtils;

public class TagsFragmentTest extends ActivityInstrumentationTestCase2<EditActivity> {
	private final String TAGS_FRAGMENT = "tagsFragment";
	
	private EditActivity activity;
	private ContentResolver resolver;
	private Instrumentation instrumentation;
	
	private OrgNode node;
	private long nodeId;
	
	public TagsFragmentTest() {
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
	
	private void prepareActivityWithTags(String tags) {
		this.node = OrgTestUtils.getDefaultOrgNode();
		this.node.tags = tags;
		this.node.write(resolver);
		this.nodeId = node.id;

		Intent intent = new Intent();
		intent.putExtra(EditActivityController.ACTIONMODE, EditActivityController.ACTIONMODE_EDIT);
		intent.putExtra(EditActivityController.NODE_ID, node.id);
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
		prepareActivityWithTags("");
		assertNotNull(this.activity);
		
		TagsFragment tagsFragment = ((TagsFragment) this.activity
				.getSupportFragmentManager().findFragmentByTag(TAGS_FRAGMENT));
		assertNotNull(tagsFragment);
	}
	
	public void testSimple() {
		final String tags = "tag1:tag2";
		prepareActivityWithTags(tags);
		
		TagsFragment tagsFragment = ((TagsFragment) this.activity
				.getSupportFragmentManager().findFragmentByTag(TAGS_FRAGMENT));		
		String resultTags = tagsFragment.getTags();
		assertEquals(tags, resultTags);
	}
	
	public void testSaveAndRestore() {
		final String tags = "tag1:tag2::tag4:tag10";
		prepareActivityWithTags(tags);
		
		final TagsFragment tagsFragment = ((TagsFragment) this.activity
				.getSupportFragmentManager().findFragmentByTag(TAGS_FRAGMENT));
		saveAndRestoreState(tagsFragment);
				
		String resultTags = tagsFragment.getTags();
		assertEquals(tags, resultTags);
	}
	
	public void testAddEntry() {
		String tags = "tag1:tag4::tag2:tag10";
		prepareActivityWithTags(tags);
		
		final TagsFragment tagsFragment = ((TagsFragment) this.activity
				.getSupportFragmentManager().findFragmentByTag(TAGS_FRAGMENT));
		final String addedTag = "hello";
		activity.runOnUiThread(new Runnable() {
			public void run() {
				tagsFragment.addTagEntry(addedTag);
			}
		});
		instrumentation.waitForIdleSync();
		
		tags += ":" + addedTag;
		String resultTags = tagsFragment.getTags();
		assertEquals(tags, resultTags);
	}
	
	public void testAddEntryAndSaveAndRestore() {
		String tags = "tag1:tag4::tag2:tag10";
		prepareActivityWithTags(tags);
		
		final TagsFragment tagsFragment = ((TagsFragment) this.activity
				.getSupportFragmentManager().findFragmentByTag("tagsFragment"));
		final String addedTag = "hello";
		activity.runOnUiThread(new Runnable() {
			public void run() {
				tagsFragment.addTagEntry(addedTag);
			}
		});
		instrumentation.waitForIdleSync();
		saveAndRestoreState(tagsFragment);
		
		tags += ":" + addedTag;
		String resultTags = tagsFragment.getTags();
		assertEquals(tags, resultTags);
	}
}
