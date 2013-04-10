package com.matburt.mobileorg.Gui.Capture;

import android.content.ContentResolver;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.Spinner;

import com.actionbarsherlock.app.SherlockFragment;
import com.matburt.mobileorg.R;
import com.matburt.mobileorg.OrgData.OrgNode;
import com.matburt.mobileorg.OrgData.OrgProviderUtils;
import com.matburt.mobileorg.util.OrgUtils;

public class HeadingFragment extends SherlockFragment {
	private final String HEADING_TITLE = "headingTitle";
	private final String HEADING_TODO = "headingTodo";
	private final String HEADING_PRIORITY = "headingPriority";
	
	private EditText titleView;
	private Spinner priorityView;
	private Spinner todoStateView;

	private OrgNode node;

	private ContentResolver resolver;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		View view = inflater.inflate(R.layout.edit_heading, container,
				false);
		
		this.titleView = (EditText) view.findViewById(R.id.edit_title);
		this.priorityView = (Spinner) view.findViewById(R.id.edit_priority);
		this.todoStateView = (Spinner) view.findViewById(R.id.edit_todo);
		
		return view;
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		
		EditHost host = ((EditHost)getActivity());
		
		this.resolver = getActivity().getContentResolver();
		this.node = host.getController().getOrgNode();
		
		if(savedInstanceState != null)
			restoreInstanceState(savedInstanceState);
		else
			updateDisplay(this.node);
		
		setModifiable(host.getController().isNodeEditable());
		
		String actionMode = host.getController().getActionMode();
		
		if (actionMode.equals(EditActivityController.ACTIONMODE_ADDCHILD)
				|| actionMode.equals(EditActivityController.ACTIONMODE_CREATE)) {
			this.titleView.requestFocus();
			getActivity().getWindow().setSoftInputMode(
				    WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
		}
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
        outState.putString(HEADING_TITLE, getTitle());
        outState.putString(HEADING_TODO, getTodo());
        outState.putString(HEADING_PRIORITY, getPriority());
	}
	
	public void restoreInstanceState(Bundle savedInstanceState) {
		if(savedInstanceState != null) {
			String title = savedInstanceState.getString(HEADING_TITLE);
			String todo = savedInstanceState.getString(HEADING_TODO);
			String priority = savedInstanceState.getString(HEADING_PRIORITY);
			updateDisplay(title, todo, priority);
		}
	}
	
	public void updateDisplay(OrgNode node) {
		if(node != null)
			updateDisplay(node.name, node.todo, node.priority);
	}
	
	public void updateDisplay(String title, String todo, String priority) {
		titleView.setText(title);
		titleView.setSelection(title.length());
		
		OrgUtils.setupSpinnerWithEmpty(todoStateView, OrgProviderUtils.getTodos(resolver),
				todo);
		OrgUtils.setupSpinnerWithEmpty(priorityView, OrgProviderUtils.getPriorities(resolver),
				priority);
	}
	
	public void setModifiable(boolean enabled) {
		this.titleView.setEnabled(enabled);
		this.priorityView.setEnabled(enabled);
		this.todoStateView.setEnabled(enabled);
	}
	
	public boolean hasEdits() {
		String newTitle = titleView.getText().toString();
		String newTodo = todoStateView.getSelectedItem().toString();
		String newPriority = priorityView.getSelectedItem().toString();

		if (newTitle.equals(node.name) && newTodo.equals(node.todo)
				&& newPriority.equals(node.priority))
			return false;
		return true;
	}
	
	public OrgNode getEditedOrgNode() {
		OrgNode orgNode = new OrgNode();
		orgNode.name = getTitle();
		orgNode.todo = getTodo();
		orgNode.priority = getPriority();
		return orgNode;
	}

	
	public String getTitle() {
		return titleView.getText().toString();
	}
	
	public String getTodo() {
		return todoStateView.getSelectedItem().toString();
	}
	
	public String getPriority() {
		return priorityView.getSelectedItem().toString();
	}
}
