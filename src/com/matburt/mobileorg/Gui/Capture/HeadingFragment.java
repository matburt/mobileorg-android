package com.matburt.mobileorg.Gui.Capture;

import android.content.ContentResolver;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Spinner;

import com.actionbarsherlock.app.SherlockFragment;
import com.matburt.mobileorg.R;
import com.matburt.mobileorg.OrgData.OrgNode;
import com.matburt.mobileorg.OrgData.OrgProviderUtil;
import com.matburt.mobileorg.util.OrgUtils;

public class HeadingFragment extends SherlockFragment {
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
	public void onStart() {
		super.onStart();
		
		EditActivity activity = ((EditActivity)getActivity());
		
		this.resolver = activity.getContentResolver();
		this.node = activity.getOrgNode();
		
		updateDisplay();
	}
	
	public void updateDisplay() {
		if(node == null)
			return;
		titleView.setText(node.name);
		titleView.setSelection(node.name.length());

		OrgUtils.setupSpinner(todoStateView, OrgProviderUtil.getTodos(resolver),
				node.todo);
		OrgUtils.setupSpinner(priorityView, OrgProviderUtil.getPriorities(resolver),
				node.priority);
	}
	
	public boolean hasEdits() {
		String newTitle = titleView.getText().toString();
		String newTodo = todoStateView.getSelectedItem().toString();
		String newPriority = priorityView.getSelectedItem().toString();
		String newTags = ""; // getTags();

		if (newTitle.equals(node.name) && newTodo.equals(node.todo)
				&& newTags.equals(node.tags)
				&& newPriority.equals(node.priority))
			return false;
		return true;
	}
	
	public OrgNode getEditedOrgNode() {
		OrgNode orgNode = new OrgNode();
		orgNode.name = getTitle();
		orgNode.todo = getTodo();
		orgNode.priority = getPriority();
//		orgNode.tags = getTags();
		return orgNode;
	}

	
	private String getTitle() {
		return titleView.getText().toString();
	}
	
	private String getTodo() {
		return todoStateView.getSelectedItem().toString();
	}
	
	private String getPriority() {
		return priorityView.getSelectedItem().toString();
	}
}
