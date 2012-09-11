package com.matburt.mobileorg.Gui.Capture;

import java.util.ArrayList;

import android.content.ContentResolver;
import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;

import com.actionbarsherlock.app.SherlockFragment;
import com.matburt.mobileorg.R;
import com.matburt.mobileorg.OrgData.OrgNode;
import com.matburt.mobileorg.OrgData.OrgProviderUtil;

public class HeadingFragment extends SherlockFragment {
	private EditText titleView;
	private Spinner priorityView;
	private Spinner todoStateView;
	
	private LinearLayout locationView;
	private LocationTableRow locationTableRow;

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
		
        setHasOptionsMenu(true);

		return view;
	}

	@Override
	public void onStart() {
		super.onStart();
		
		EditActivity activity = ((EditActivity)getActivity());
		
		this.resolver = activity.getContentResolver();
		this.node = activity.getOrgNode();
		
		if (node.getParent(resolver) != null)
			this.locationTableRow = new LocationTableRow(
					node.getParent(resolver), getActivity(), locationView,
					resolver);
		
		updateDisplay();
	}
	
	public void updateDisplay() {
		if(node == null)
			return;
		titleView.setText(node.name);
		titleView.setSelection(node.name.length());

		setupSpinner(getActivity(), todoStateView,
				OrgProviderUtil.getTodos(resolver), node.todo);
		setupSpinner(getActivity(), priorityView,
				OrgProviderUtil.getPriorities(resolver), node.priority);
	}


	@Override
	public void onCreateOptionsMenu(com.actionbarsherlock.view.Menu menu,
			com.actionbarsherlock.view.MenuInflater inflater) {
		super.onCreateOptionsMenu(menu, inflater);
		inflater.inflate(R.menu.edit_heading, menu);
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
	
	public OrgNode getLocation() {
		return locationTableRow.getParentNodeId();
	}
	
	public static void setupSpinner(Context context, Spinner spinner, ArrayList<String> data,
			String selection) {		
		if(!TextUtils.isEmpty(selection) && !data.contains(selection))
			data.add(selection);
		data.add("");
		
		ArrayAdapter<String> adapter = new ArrayAdapter<String>(context,
				android.R.layout.simple_spinner_item, data);
		adapter.setDropDownViewResource(R.layout.edit_spinner_layout);
		spinner.setAdapter(adapter);
		int pos = data.indexOf(selection);
		if (pos < 0) {
			pos = 0;
		}
		spinner.setSelection(pos);
	}
}
