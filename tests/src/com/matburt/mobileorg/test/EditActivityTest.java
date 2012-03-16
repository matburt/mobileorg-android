package com.matburt.mobileorg.test;

import com.jayway.android.robotium.solo.Solo;
import com.matburt.mobileorg.Gui.Capture.EditActivity;

import android.content.Context;
import android.content.Intent;
import android.test.ActivityInstrumentationTestCase2;
import android.test.UiThreadTest;
import android.widget.Spinner;
import android.widget.TextView;


public class EditActivityTest extends
		ActivityInstrumentationTestCase2<EditActivity> {

	private EditActivity mActivity;
	private Solo solo;
	private TextView mTitleView;
	private Spinner mTodoState;

	public EditActivityTest() {
		super(EditActivity.class);
	}
	
	@Override
	protected void setUp() throws Exception {
		super.setUp();
		Intent intent = new Intent();
		intent.putExtra("actionMode", EditActivity.ACTIONMODE_CREATE);
		this.setActivityIntent(intent);
		this.setActivityInitialTouchMode(false);
		this.mActivity = this.getActivity();
		
		this.mTitleView = (TextView) mActivity.findViewById(com.matburt.mobileorg.R.id.title);
		this.mTodoState = (Spinner) mActivity.findViewById(com.matburt.mobileorg.R.id.todo_state);

		this.solo = new Solo(getInstrumentation(), getActivity());
	}

	@UiThreadTest
	public void testTitleSimple() {
		final String titleTest = "Hej";
	    mTitleView.setText(titleTest);
		assertEquals(titleTest, mTitleView.getText().toString());

		//		this.sendKeys("H E J");
	}
	
	@UiThreadTest
	public void testTodo() {
		String defaultTodo = mActivity.getSharedPreferences("", Context.MODE_PRIVATE).getString("defaultTodo", "");
		assertEquals(defaultTodo, this.mTodoState.getSelectedItem().toString());
	}
	
	public void testTags() {
		solo.clickOnMenuItem("Add Tag");
		solo.setActivityOrientation(0);
	}
}
