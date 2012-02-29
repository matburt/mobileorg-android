package com.matburt.mobileorg.Gui.Capture;

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.view.Menu;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TableLayout;

import com.matburt.mobileorg.R;
import com.matburt.mobileorg.Parsing.MobileOrgApplication;
import com.matburt.mobileorg.Parsing.NodeWrapper;
import com.matburt.mobileorg.Parsing.OrgDatabase;

public class EditDetailsFragment extends Fragment {
	private EditText titleView;
	private Spinner priorityView;
	private Spinner todoStateView;
	private TableLayout tagsView;
	private TableLayout datesView;

	private OrgDatabase orgDB;

	private NodeWrapper node;
	private String actionMode;
	
	private String title;
	ArrayList<TagTableRow> tagEntries = new ArrayList<TagTableRow>();
	private String defaultTodo;
	private DateTableRow scheduledEntry = null;
	private DateTableRow deadlineEntry = null;

	public void init(NodeWrapper node, String actionMode, String defaultTodo, String title) {
		init(node, actionMode, defaultTodo);
		this.title = title;
	}
	
	public void init(NodeWrapper node, String actionMode, String defaultTodo) {
		this.node = node;
		this.actionMode = actionMode;
		this.defaultTodo = defaultTodo;
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		View view = inflater.inflate(R.layout.edit_details, container,
				false);
		
		this.titleView = (EditText) view.findViewById(R.id.title);
		this.priorityView = (Spinner) view.findViewById(R.id.priority);
		this.todoStateView = (Spinner) view.findViewById(R.id.todo_state);
		this.tagsView = (TableLayout) view.findViewById(R.id.tags);
		this.datesView = (TableLayout) view.findViewById(R.id.dates);

		this.orgDB = ((MobileOrgApplication) getActivity().getApplication()).getDB();
		
        setHasOptionsMenu(true);
        initDisplay();
		return view;
	}
	
	private void initDisplay() {
		if(this.actionMode == null) {
			this.actionMode = EditActivity.ACTIONMODE_CREATE;
			titleView.setText(title);

			setupSpinner(getActivity(), todoStateView, orgDB.getTodos(), defaultTodo);
			setupSpinner(getActivity(), priorityView, orgDB.getPriorities(), "");
		}
		else if (this.actionMode.equals(EditActivity.ACTIONMODE_CREATE)) {
			titleView.setText("");

			setupSpinner(getActivity(), todoStateView, orgDB.getTodos(), defaultTodo);
			setupSpinner(getActivity(), priorityView, orgDB.getPriorities(), "");
		} else if (this.actionMode.equals(EditActivity.ACTIONMODE_EDIT)) {
			titleView.setText(node.getName());
			
			setupTags(orgDB.getTags());
			setupDates();
			setupSpinner(getActivity(), todoStateView, orgDB.getTodos(), node.getTodo());
			setupSpinner(getActivity(), priorityView, orgDB.getPriorities(), node.getPriority());
		}
	}
	
	private void setupTags(ArrayList<String> tagList) {		
		for(String tag: node.getTagList()) {
			if(TextUtils.isEmpty(tag)) { 
				// NodeWrapper found a :: entry, meaning all tags so far where unmodifiable
				for(TagTableRow entry: tagEntries)
					entry.setUnmodifiable();
				if(tagEntries.size() > 0)
					tagEntries.get(tagEntries.size() - 1).setLast();
			}
			else
				addTag(tag);
		}
	}
	
	private void setupDates() {
		this.scheduledEntry = setupDate(node.getScheduled(this.orgDB), "SCHEDULED", scheduledRemoveListener);
		this.deadlineEntry = setupDate(node.getDeadline(this.orgDB), "DEADLINE", deadlineRemoveListener);
	}
	
	private DateTableRow setupDate(String date, String title, View.OnClickListener removeListener) {
		final Pattern schedulePattern = Pattern
				.compile("((\\d{4})-(\\d{1,2})-(\\d{1,2}))(?:\\s+\\w+)?\\s*((\\d{1,2})\\:(\\d{2}))?");
		Matcher propm = schedulePattern.matcher(date);
		DateTableRow dateEntry = null;

		if(propm.find()) {
			// TODO Fix guards
			
			if(propm.group(6) != null && propm.group(7) != null) {
				dateEntry = new DateTableRow(getActivity(), this, datesView, removeListener,
						title, Integer.parseInt(propm.group(2)),
						Integer.parseInt(propm.group(3)),
						Integer.parseInt(propm.group(4)),
						Integer.parseInt(propm.group(6)),
						Integer.parseInt(propm.group(7)));
			} else if(propm.groupCount() >= 4) {
			dateEntry = new DateTableRow(getActivity(), this, datesView, removeListener,
					title, Integer.parseInt(propm.group(2)),
					Integer.parseInt(propm.group(3)), Integer.parseInt(propm
							.group(4)));
			}
		}
		return dateEntry;
	}
	
	private View.OnClickListener scheduledRemoveListener = new View.OnClickListener() {
		public void onClick(View v) {
			datesView.removeView(scheduledEntry);
			scheduledEntry = null;
		}
	};
	
	private View.OnClickListener deadlineRemoveListener = new View.OnClickListener() {
		public void onClick(View v) {
			datesView.removeView(deadlineEntry);
			deadlineEntry = null;
		}
	};
	
    
	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		super.onCreateOptionsMenu(menu, inflater);
		inflater.inflate(R.menu.edit_details, menu);
	}
	
	@Override
	public void onPrepareOptionsMenu(Menu menu) {
		super.onPrepareOptionsMenu(menu);
		
		if(this.scheduledEntry != null)
			menu.findItem(R.id.menu_nodeedit_scheduled).setVisible(false);
		else
			menu.findItem(R.id.menu_nodeedit_scheduled).setVisible(true);
						
		if(this.deadlineEntry != null)
			menu.findItem(R.id.menu_nodeedit_deadline).setVisible(false);
		else
			menu.findItem(R.id.menu_nodeedit_deadline).setVisible(true);
	}

	@Override
	public boolean onOptionsItemSelected(android.support.v4.view.MenuItem item) {
		switch (item.getItemId()) {

		case R.id.menu_nodeedit_tag:
			addTag("");
			return true;

		case R.id.menu_nodeedit_scheduled:
			addDateScheduled();
			return true;

		case R.id.menu_nodeedit_deadline:
			addDateDeadline();
			return true;
		}
		return false;
	}
	
	private void addDateScheduled() {
		DateTableRow dateEntry = new DateTableRow(getActivity(), this, datesView, scheduledRemoveListener,
				"SCHEDULED");
		this.scheduledEntry = dateEntry;
	}
	
	private void addDateDeadline() {
		DateTableRow dateEntry = new DateTableRow(getActivity(), this, datesView, deadlineRemoveListener,
				"DEADLINE");
		this.deadlineEntry = dateEntry;
	}

	private void addTag(String tag) {
		TagTableRow tagEntry = new TagTableRow(getActivity(), tagsView, orgDB.getTags(), tag, this);
		tagsView.addView(tagEntry);
		tagEntries.add(tagEntry);
	}

	
	public boolean hasEdits() {
		String newTitle = titleView.getText().toString();
		String newTodo = todoStateView.getSelectedItem().toString();
		String newPriority = priorityView.getSelectedItem().toString();
		String newTags = getTags();
		if (this.actionMode.equals(EditActivity.ACTIONMODE_CREATE)) {
			if (newTitle.length() == 0)
				return false;
		} else if (this.actionMode.equals(EditActivity.ACTIONMODE_EDIT)) {
			
			if (newTitle.equals(node.getName())
					&& newTodo.equals(node.getTodo())
					&& newTags.equals(node.getTags())
					&& newPriority.equals(node.getPriority()))
				return false;
		}	
		return true;
	}
	
	
	public String getTags() {
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
	
	public String getScheduled() {
		if(this.scheduledEntry == null)
			return "";
		else
			return "SCHEDULED: <" + this.scheduledEntry.getDate() + ">";
	}

	public String getDeadline() {
		if(this.scheduledEntry == null)
			return "";
		else
			return "DEADLINE: <" + this.deadlineEntry.getDate() + ">";
	}
	
	public String getTitle() {
		return this.titleView.getText().toString();
	}
	
	public String getTodo() {
		return todoStateView.getSelectedItem().toString();
	}
	
	public String getPriority() {
		return priorityView.getSelectedItem().toString();
	}
	
	static void setupSpinner(Context context, Spinner view, ArrayList<String> data,
			String selection) {
		data.add("");

		ArrayAdapter<String> adapter = new ArrayAdapter<String>(context,
				android.R.layout.simple_spinner_item, data);
		adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		view.setAdapter(adapter);
		int pos = data.indexOf(selection);
		if (pos < 0) {
			pos = 0;
		}
		view.setSelection(pos);
	}
}
