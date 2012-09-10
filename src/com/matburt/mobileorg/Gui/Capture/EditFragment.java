package com.matburt.mobileorg.Gui.Capture;

import java.util.ArrayList;

import android.content.ContentResolver;
import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TableLayout;

import com.actionbarsherlock.app.SherlockFragment;
import com.actionbarsherlock.view.MenuItem;
import com.matburt.mobileorg.R;
import com.matburt.mobileorg.OrgData.OrgNode;
import com.matburt.mobileorg.OrgData.OrgProviderUtil;

public class EditFragment extends SherlockFragment {
	private EditText titleView;
	private Spinner priorityView;
	private Spinner todoStateView;
	private TableLayout tagsView;
	private LinearLayout locationView;
	private LocationTableRow locationTableRow;
	private LinearLayout payloadView;

	private OrgNode node;
	
	ArrayList<TagTableRow> tagEntries = new ArrayList<TagTableRow>();

	
	private ArrayList<String> tagsToRestore = null;
	private ContentResolver resolver;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		View view = inflater.inflate(R.layout.edit_details, container,
				false);
		
		this.titleView = (EditText) view.findViewById(R.id.edit_title);
		this.priorityView = (Spinner) view.findViewById(R.id.edit_priority);
		this.todoStateView = (Spinner) view.findViewById(R.id.edit_todo);
		this.tagsView = (TableLayout) view.findViewById(R.id.edit_tags);
		this.locationView = (LinearLayout) view.findViewById(R.id.edit_location);
		this.payloadView = (LinearLayout) view.findViewById(R.id.edit_payload);
		this.payloadView.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				payloadView.removeAllViews();
				EditText payloadEdit = new EditText(getActivity());
				payloadEdit.setText(node.getRawPayload());
				payloadView.addView(payloadEdit);
			}
		});
		
        setHasOptionsMenu(true);
        
		if (savedInstanceState != null) {

			tagsToRestore = savedInstanceState.getStringArrayList("tags");
		}
        
		return view;
	}

	@Override
	public void onStart() {
		super.onStart();
		
		if(tagsToRestore != null) {
			tagsView.removeAllViews();
			tagEntries.clear();
			setupTags(tagsToRestore);
			tagsToRestore = null;
		}
		
		EditActivity activity = ((EditActivity)getActivity());
		
		this.resolver = activity.getContentResolver();
		this.node = activity.getOrgNode();
		updateDisplay();
	}
	
	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
        
        ArrayList<String> tags = new ArrayList<String>();
        for(TagTableRow tag: tagEntries) {
        	tags.add(tag.getSelection());
        }
        outState.putStringArrayList("tags", tags);
	}
	
	public void updateDisplay() {
		if(node == null)
			return;
		titleView.setText(node.name);
		titleView.setSelection(node.name.length());

		setupTags(node.getTags());
		setupSpinner(getActivity(), todoStateView,
				OrgProviderUtil.getTodos(resolver), node.todo);
		setupSpinner(getActivity(), priorityView,
				OrgProviderUtil.getPriorities(resolver), node.priority);
		if (node.getParent(resolver) != null)
			this.locationTableRow = new LocationTableRow(
					node.getParent(resolver), getActivity(), locationView,
					resolver);
	}
	
	private void setupTags(ArrayList<String> tagList) {		
		for(String tag: tagList) {
			if(TextUtils.isEmpty(tag)) { 
				// OrgNode found a :: entry, meaning all tags so far where unmodifiable
				for(TagTableRow entry: tagEntries)
					entry.setUnmodifiable();
				if(tagEntries.size() > 0)
					tagEntries.get(tagEntries.size() - 1).setLast();
			}
			else
				addTag(tag);
		}
	}
	

	@Override
	public void onCreateOptionsMenu(com.actionbarsherlock.view.Menu menu,
			com.actionbarsherlock.view.MenuInflater inflater) {
		super.onCreateOptionsMenu(menu, inflater);
		inflater.inflate(R.menu.edit_details, menu);
	}
	
	@Override
	public void onPrepareOptionsMenu(com.actionbarsherlock.view.Menu menu) {
		super.onPrepareOptionsMenu(menu);
			

	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {

		case R.id.menu_nodeedit_tag:
			addTag("");
			return true;

		}
		return false;
	}


	private void addTag(String tag) {
		TagTableRow tagEntry = new TagTableRow(getActivity(), tagsView, OrgProviderUtil.getTags(resolver), tag, this);
		tagsView.addView(tagEntry);
		tagEntries.add(tagEntry);
	}

	
	public boolean hasEdits() {
		String newTitle = titleView.getText().toString();
		String newTodo = todoStateView.getSelectedItem().toString();
		String newPriority = priorityView.getSelectedItem().toString();
		String newTags = getTags();

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
		orgNode.tags = getTags();
		return orgNode;
	}
	
	private String getTags() {
		StringBuilder result = new StringBuilder();
		for(TagTableRow entry: tagEntries) {
			String selection = entry.getSelection();
			if(TextUtils.isEmpty(selection) == false) {
				result.append(selection);
				result.append(":");
			}
		}
		
		if(TextUtils.isEmpty(result))
			return "";
		else
			return result.deleteCharAt(result.lastIndexOf(":")).toString();
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
	
	public static void setupSpinner(Context context, Spinner view, ArrayList<String> data,
			String selection) {		
		if(!TextUtils.isEmpty(selection) && !data.contains(selection))
			data.add(selection);
		data.add("");
		
		ArrayAdapter<String> adapter = new ArrayAdapter<String>(context,
				android.R.layout.simple_spinner_item, data);
		adapter.setDropDownViewResource(R.layout.edit_spinner_layout);
		view.setAdapter(adapter);
		int pos = data.indexOf(selection);
		if (pos < 0) {
			pos = 0;
		}
		view.setSelection(pos);
	}
}
