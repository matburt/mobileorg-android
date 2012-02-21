package com.matburt.mobileorg.Gui;

import java.util.ArrayList;

import android.content.Context;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.view.Menu;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TableLayout;
import android.widget.TableRow;

import com.matburt.mobileorg.R;
import com.matburt.mobileorg.Parsing.MobileOrgApplication;
import com.matburt.mobileorg.Parsing.NodeWrapper;
import com.matburt.mobileorg.Parsing.OrgDatabase;

public class NodeEditDetailsFragment extends Fragment {
	private EditText titleView;
	private Spinner priorityView;
	private Spinner todoStateView;
	private TableLayout tagsView;

	private OrgDatabase orgDB;

	private NodeWrapper node;
	private String actionMode;
	
	private String title;
	private ArrayList<TagEntry> tagEntries = new ArrayList<TagEntry>();
	
	
	public NodeEditDetailsFragment(String tile, String actionMode) {
		this.title = tile;
		this.node = new NodeWrapper(null);
	}
	
	public NodeEditDetailsFragment(NodeWrapper node, String actionMode) {
		this.node = node;
		this.actionMode = actionMode;
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		View view = inflater.inflate(R.layout.editnode_details, container,
				false);
		
		this.titleView = (EditText) view.findViewById(R.id.title);
		this.priorityView = (Spinner) view.findViewById(R.id.priority);
		this.todoStateView = (Spinner) view.findViewById(R.id.todo_state);
		this.tagsView = (TableLayout) view.findViewById(R.id.tags);
		
		this.orgDB = ((MobileOrgApplication) getActivity().getApplication()).getDB();
		
        setHasOptionsMenu(true);
		initDisplay(view);
		return view;
	}
     
	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		super.onCreateOptionsMenu(menu, inflater);
		inflater.inflate(R.menu.nodeedit_details, menu);
	}

	@Override
	public boolean onOptionsItemSelected(android.support.v4.view.MenuItem item) {
		switch (item.getItemId()) {

		case R.id.nodeedit_tag:
			addTag("");
			return true;
		}
		return false;
	}
	
	private void initDisplay(View view) {
		String defaultTodo = PreferenceManager.getDefaultSharedPreferences(
				getActivity().getApplicationContext()).getString("defaultTodo", "");

		if(this.actionMode == null) {
			this.actionMode = NodeEditActivity.ACTIONMODE_CREATE;
			titleView.setText(title);

			setSpinner(todoStateView, orgDB.getTodos(), defaultTodo);
			setSpinner(priorityView, orgDB.getPriorities(), "");
		}
		else if (this.actionMode.equals(NodeEditActivity.ACTIONMODE_CREATE)) {
			titleView.setText("");

			setSpinner(todoStateView, orgDB.getTodos(), defaultTodo);
			setSpinner(priorityView, orgDB.getPriorities(), "");
		} else if (this.actionMode.equals(NodeEditActivity.ACTIONMODE_EDIT)) {
			titleView.setText(node.getName());
			
			setupTags(orgDB.getTags());
			setSpinner(todoStateView, orgDB.getTodos(), node.getTodo());
			setSpinner(priorityView, orgDB.getPriorities(), node.getPriority());
		}
	}
	
	private void setupTags(ArrayList<String> tagList) {
		ArrayList<String> tags = node.getTagList();
		
		for(String tag: tags) {
			if(TextUtils.isEmpty(tag)) { // NodeWrapper found a :: entry, meaning all tags so far where unmodifiable
				for(TagEntry entry: tagEntries)
					entry.setUnmodifiable();
				if(tagEntries.size() > 0)
					tagEntries.get(tagEntries.size() - 1).setLast();
			}
			else
				addTag(tag);
		}
	}
	
	private void addTag(String tag) {
		TagEntry tagEntry = new TagEntry(getActivity(), tagsView, orgDB.getTags(), tag);
		tagsView.addView(tagEntry);
		tagEntries.add(tagEntry);
	}
 
	private void setSpinner(Spinner view, ArrayList<String> data,
			String selection) {
		data.add("");

		ArrayAdapter<String> adapter = new ArrayAdapter<String>(getActivity(),
				android.R.layout.simple_spinner_item, data);
		adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		view.setAdapter(adapter);
		int pos = data.indexOf(selection);
		if (pos < 0) {
			pos = 0;
		}
		view.setSelection(pos);
	}
	
	private String getTagsSelection() {
		StringBuilder result = new StringBuilder();
		for(TagEntry entry: tagEntries) {
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
	
	public boolean hasEdits() {
		String newTitle = titleView.getText().toString();
		String newTodo = todoStateView.getSelectedItem().toString();
		String newPriority = priorityView.getSelectedItem().toString();
		String newTags = getTagsSelection();
				
		if (this.actionMode.equals(NodeEditActivity.ACTIONMODE_CREATE)) {
			if (newTitle.length() == 0)
				return false;
		} else if (this.actionMode.equals(NodeEditActivity.ACTIONMODE_EDIT)) {
			if (newTitle.equals(node.getName())
					&& newTodo.equals(node.getTodo())
					&& newTags.equals(node.getTags())
					&& newPriority.equals(node.getPriority()))
				return false;
		}
		
		return true;
	}
	
	private class TagEntry extends TableRow {		
		TableLayout parent;
		Spinner spinner;
		Button button;
		
		String selectionExtra = ""; // used to carry ::

		public TagEntry(Context context, TableLayout parent,
				final ArrayList<String> tags, String selection) {
			super(context);

			this.parent = parent;

			LayoutInflater layoutInflater = (LayoutInflater) context
					.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			TableRow row = (TableRow) layoutInflater.inflate(
					R.layout.editnode_tagslayout, this);

			button = (Button) findViewById(R.id.editnode_remove);
			button.setOnClickListener(removeListener);
			
			if(selection.endsWith(":")) {
				selectionExtra = ":";
				selection = selection.replace(":", "");
			}

			spinner = (Spinner) row.findViewById(R.id.tagslist);
			setSpinner(spinner, tags, selection);
		}
		
		public void setUnmodifiable() {
			button.setVisibility(INVISIBLE);
			spinner.setEnabled(false);
		}
		
		public void setLast() {
			selectionExtra = ":";
		}
		
		public String getSelection() {
			return spinner.getSelectedItem().toString() + selectionExtra;
		}
		
		private void remove() {
			parent.removeView(this);
			tagEntries.remove(this);
		}
		
		private View.OnClickListener removeListener = new View.OnClickListener() {
			public void onClick(View v) {
				remove();
			}
		};
	}
}
