package com.matburt.mobileorg.test.Gui;

import android.content.ContentResolver;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.test.ActivityInstrumentationTestCase2;
import android.test.ActivityUnitTestCase;
import android.test.mock.MockContentResolver;

import com.matburt.mobileorg.Gui.Capture.EditActivity;
import com.matburt.mobileorg.Gui.Capture.EditActivityMock;
import com.matburt.mobileorg.Gui.Capture.EditDetailsFragment;
import com.matburt.mobileorg.OrgData.OrgNode;
import com.matburt.mobileorg.test.util.OrgTestUtils;

public class EditActivityTestMock extends ActivityInstrumentationTestCase2<EditActivityMock> {

	private EditActivityMock activity;
	private ContentResolver resolver;
	
	public EditActivityTestMock() {
		super(EditActivityMock.class);
	}

	@Override
	public void setUp() throws Exception {
		super.setUp();
		
		this.resolver = new MockContentResolver();
		this.activity = getActivity();
	}
	
	@Override
	public void tearDown() throws Exception {
		super.tearDown();
	}
	
//	public void testTestMock() {
//		assertNotNull(this.activity);
//		
//		//getInstrumentation().callActivityOnCreate(activity, null);
//		
//		final OrgNode node = OrgTestUtils.getDefaultOrgNode();
//		//replaceFragment(node);
//		//activity.setOrgNode(node);
//		//activity.replaceFragment(node);
//		callReplace(node);
//
//		EditDetailsFragment detailsFragment = (EditDetailsFragment)activity.getSupportFragmentManager().findFragmentByTag("details2");
//		OrgNode editedNode = detailsFragment.getEditedOrgNode();
//		assertEquals(node.name, editedNode.name);
//	}
	
	public void callReplace(final OrgNode node) {
		activity.runOnUiThread(new Runnable (){
		@Override
		public void run() {
			getActivity().replaceFragment(node);
		}
	});
	}
	
	public void replaceFragment(OrgNode node) {
		EditDetailsFragment fragment = new EditDetailsFragment();
		//fragment.setNode(node, "", null);
		FragmentManager fragmentManager = activity.getSupportFragmentManager();
		FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
		fragmentTransaction.replace(com.matburt.mobileorg.R.id.editnode_fragment_container, fragment, "details");
		fragmentTransaction.commit();
	}
}
