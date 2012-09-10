package com.matburt.mobileorg.test.Gui;


import android.R;
import android.content.ContentResolver;
import android.content.Intent;
import android.support.v4.app.Fragment;
import android.test.ActivityInstrumentationTestCase;
import android.test.ActivityInstrumentationTestCase2;
import android.test.ActivityUnitTestCase;
import android.test.UiThreadTest;
import android.test.mock.MockContentResolver;
import android.widget.EditText;

import com.matburt.mobileorg.Gui.Capture.EditActivity;
import com.matburt.mobileorg.Gui.Capture.EditFragment;
import com.matburt.mobileorg.OrgData.OrgNode;
import com.matburt.mobileorg.test.util.OrgTestUtils;

public class EditActivityTest extends ActivityInstrumentationTestCase2<EditActivity> {

	private EditActivity activity;
	private ContentResolver resolver;
	
	public EditActivityTest() {
		super(EditActivity.class);
	}

	@Override
	public void setUp() throws Exception {
		super.setUp();
		
		setActivityInitialTouchMode(false);
		//Intent intent = new Intent();
//		intent.putExtra(EditActivity.ACTIONMODE, EditActivity.ACTIONMODE_EDIT);
//		intent.putExtra(EditActivity.NODE_ID, 100);
		//setActivityIntent(intent);
		this.activity = getActivity();
		
		this.resolver = new MockContentResolver();
	}
	
	@Override
	public void tearDown() throws Exception {
		super.tearDown();
	}
	
	public void testMock() {
		assertNotNull(this.activity);
		
		
//		final OrgNode node = OrgTestUtils.getDefaultOrgNode();
//		activity.setOrgNode(node, "");
		
//		activity.runOnUiThread(new Runnable (){
//			@Override
//			public void run() {
//				activity.setOrgNode(node, "");
//			}
//		});
		
//		OrgNode editedNode = activity.getOrgNode();
//		
//		assertEquals(node.name, editedNode.name);
	}
	
	
//	public void testFragmentSimple() {
//		OrgNode node = OrgTestUtils.getDefaultOrgNode();
//
//		EditDetailsFragment details = new EditDetailsFragment();
//		//details.onCreateView(activity.getLayoutInflater(), activity.onCreateView(getName(), context, null), null);
//		details.init(node, "", null);
//		details.updateDisplay();
//		
//		OrgNode editedNode = details.getEditedOrgNode();
//		assertEquals(node.name, editedNode.name);
//	}
	
//	public void testFragment() {
//		Fragment fragment = activity.getSupportFragmentManager().findFragmentByTag(EditActivity.FRAGMENT_DETAILS_TAG);
//		assertNotNull(fragment);
//		final EditDetailsFragment detailsFragment = (EditDetailsFragment) fragment;
//		//detailsFragment.init(node, EditActivity.ACTIONMODE_EDIT, "TODO", activity.getContentResolver());
//		
//		activity.runOnUiThread(new Runnable() {
//	          public void run() {
//	        	  detailsFragment.updateDisplay();
//	          }
//	      });
//		
//		OrgNode node = OrgTestUtils.getDefaultOrgNode();
//		this.activity.setupOrgNode(node, "");
//		
//
//		OrgNode editedNode = detailsFragment.getEditedOrgNode();		
//		assertEquals(node.name, editedNode.name);
//	}
//	
	public void testTest() {
		EditText titleView = (EditText) activity.findViewById(com.matburt.mobileorg.R.id.edit_title);
		assertNotNull(titleView);
		assertTrue(titleView.getText().equals(""));	
	}
//	
//	
//	@UiThreadTest
//	public void testTest3() {
//		EditText titleView = (EditText) activity.findViewById(R.id.title);
//		assertNotNull(titleView);
//		assertTrue(titleView.getText().equals(""));	
//	}
//	
//	public void testTest2() {
//		Fragment detailsFragment = activity.getSupportFragmentManager().findFragmentByTag(EditActivity.FRAGMENT_DETAILS_TAG);
//		assertNotNull(detailsFragment);
//		EditText titleView = (EditText) detailsFragment.getView().findViewById(R.id.title);
//		assertNotNull(titleView);
//		assertTrue(titleView.getText().equals(""));
//	}

}
