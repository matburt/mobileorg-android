package com.matburt.mobileorg.Gui.Capture;

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
import android.widget.TableLayout;

import com.actionbarsherlock.app.SherlockFragment;
import com.actionbarsherlock.view.MenuItem;
import com.matburt.mobileorg.R;
import com.matburt.mobileorg.Gui.Capture.DateTableRow.OrgTimeDate;
import com.matburt.mobileorg.provider.OrgNode;
import com.matburt.mobileorg.provider.OrgProviderUtil;

public class EditDetailsFragment extends SherlockFragment {
	private EditText titleView;
	private Spinner priorityView;
	private Spinner todoStateView;
	private TableLayout tagsView;
	private TableLayout datesView;
	private LinearLayout locationView;
	private LocationTableRow locationTableRow;

	private OrgNode node;
	
	ArrayList<TagTableRow> tagEntries = new ArrayList<TagTableRow>();
	private String defaultTodo;
	private DateTableRow scheduledEntry = null;
	private DateTableRow deadlineEntry = null;
	
	private ArrayList<String> tagsToRestore = null;
	private ContentResolver resolver;
	
	public void init(OrgNode node, String defaultTodo, ContentResolver resolver) {
		this.defaultTodo = defaultTodo;
		this.resolver = resolver;
		this.node = node;
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
		this.locationView = (LinearLayout) view.findViewById(R.id.location);
		
        setHasOptionsMenu(true);
        //updateDisplay();
        
		if (savedInstanceState != null) {
			setupScheduled(savedInstanceState.getString("scheduled"));
			setupDeadline(savedInstanceState.getString("deadline"));
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
		updateDisplay();
	}
	
	
	
	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
        outState.putString("scheduled", getScheduled());
        outState.putString("deadline", getDeadline());
        
        ArrayList<String> tags = new ArrayList<String>();
        for(TagTableRow tag: tagEntries) {
        	tags.add(tag.getSelection());
        }
        outState.putStringArrayList("tags", tags);
	}

	public void updateDisplay() {
		assert(node != null);
		assert(node.name != null);
		assert(titleView != null);
		titleView.setText(node.name);
		titleView.setSelection(node.name.length());

		// TODO Fix taglist
		// setupTags(node.getTagList());
		setupDates();
		setupSpinner(getActivity(), todoStateView,
				OrgProviderUtil.getTodos(resolver), node.todo);
		setupSpinner(getActivity(), priorityView,
				OrgProviderUtil.getPriorities(resolver), node.priority);
		if (node.getParent(resolver) != null)
			this.locationTableRow = new LocationTableRow(
					node.getParent(resolver), getActivity(), locationView, resolver);
	}
	
	private void setupTags(ArrayList<String> tagList) {		
		for(String tag: tagList) {
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
	
	private void setupScheduled(String scheduled) {
		if(scheduled != null)
			this.scheduledEntry = setupDate(scheduled, "SCHEDULED", scheduledRemoveListener);
	}
	
	private void setupDeadline(String deadline) {
		if(deadline != null)
			this.deadlineEntry = setupDate(deadline, "DEADLINE", deadlineRemoveListener);
	}
	
	private void setupDates() {
		this.scheduledEntry = setupDate(node.getPayload().getScheduled(), "SCHEDULED", scheduledRemoveListener);
		this.deadlineEntry = setupDate(node.getPayload().getDeadline(), "DEADLINE", deadlineRemoveListener);
	}
	
	private DateTableRow setupDate(String date, String title, View.OnClickListener removeListener) {
		final Pattern schedulePattern = Pattern
				.compile("((\\d{4})-(\\d{1,2})-(\\d{1,2}))(?:[^\\d]*)" 
						+ "((\\d{1,2})\\:(\\d{2}))?(-((\\d{1,2})\\:(\\d{2})))?");
		Matcher propm = schedulePattern.matcher(date);
		DateTableRow dateEntry = null;

		if (propm.find()) {
			OrgTimeDate timeDate = new OrgTimeDate();

			try {
				timeDate.year = Integer.parseInt(propm.group(2));
				timeDate.monthOfYear = Integer.parseInt(propm.group(3));
				timeDate.dayOfMonth = Integer.parseInt(propm.group(4));

				timeDate.startTimeOfDay = Integer.parseInt(propm.group(6));
				timeDate.startMinute = Integer.parseInt(propm.group(7));

				timeDate.endTimeOfDay = Integer.parseInt(propm.group(10));
				timeDate.endMinute = Integer.parseInt(propm.group(11));
			} catch (NumberFormatException e) {
			}
			dateEntry = new DateTableRow(getActivity(), this, datesView,
					removeListener, title, timeDate);
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
	public void onCreateOptionsMenu(com.actionbarsherlock.view.Menu menu,
			com.actionbarsherlock.view.MenuInflater inflater) {
		// TODO Auto-generated method stub
		super.onCreateOptionsMenu(menu, inflater);
		inflater.inflate(R.menu.edit_details, menu);
	}
	
	@Override
	public void onPrepareOptionsMenu(com.actionbarsherlock.view.Menu menu) {
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
	public boolean onOptionsItemSelected(MenuItem item) {
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
	
	public String getScheduled() {
		if(this.scheduledEntry == null || TextUtils.isEmpty(this.scheduledEntry.getDate()))
			return "";
		else
			return "SCHEDULED: <" + this.scheduledEntry.getDate() + ">";
	}

	public String getDeadline() {
		if(this.deadlineEntry == null || TextUtils.isEmpty(this.deadlineEntry.getDate()))
			return "";
		else
			return "DEADLINE: <" + this.deadlineEntry.getDate() + ">";
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
