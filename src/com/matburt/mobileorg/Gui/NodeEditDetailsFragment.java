package com.matburt.mobileorg.Gui;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.app.DatePickerDialog;
import android.app.DatePickerDialog.OnDateSetListener;
import android.app.Dialog;
import android.app.TimePickerDialog;
import android.app.TimePickerDialog.OnTimeSetListener;
import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.Menu;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.TimePicker;

import com.matburt.mobileorg.R;
import com.matburt.mobileorg.Parsing.MobileOrgApplication;
import com.matburt.mobileorg.Parsing.NodeWrapper;
import com.matburt.mobileorg.Parsing.OrgDatabase;

public class NodeEditDetailsFragment extends Fragment {
	private EditText titleView;
	private Spinner priorityView;
	private Spinner todoStateView;
	private TableLayout tagsView;
	private TableLayout datesView;

	private OrgDatabase orgDB;

	private NodeWrapper node;
	private String actionMode;
	
	private String title;
	private ArrayList<TagEntry> tagEntries = new ArrayList<TagEntry>();
	private String defaultTodo;
	private DateEntry scheduledEntry = null;
	private DateEntry deadlineEntry = null;

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
		View view = inflater.inflate(R.layout.editnode_details, container,
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
     
	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		super.onCreateOptionsMenu(menu, inflater);
		inflater.inflate(R.menu.nodeedit_details, menu);
		
		if(this.scheduledEntry != null)
			menu.findItem(R.id.menu_nodeedit_scheduled).setVisible(false);
		
		if(this.deadlineEntry != null)
			menu.findItem(R.id.menu_nodeedit_deadline).setVisible(false);
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
	
	private void initDisplay() {
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
			setupDates();
			setSpinner(todoStateView, orgDB.getTodos(), node.getTodo());
			setSpinner(priorityView, orgDB.getPriorities(), node.getPriority());
		}
	}
	
	private void setupDates() {
		final Pattern schedulePattern = Pattern
				.compile("((\\d{4})-(\\d{2})-(\\d{2}))(?:\\s+\\w+)?\\s*((\\d{1,2})\\:(\\d{2}))?");
		Matcher propm = schedulePattern.matcher(node.getScheduled(this.orgDB));
		
		if(propm.find()) {
			DateEntry dateEntry = null;
			// TODO Fix guards
			
			if(propm.group(6) != null && propm.group(7) != null) {
				dateEntry = new DateEntry(getActivity(), datesView,
						"SCHEDULED", Integer.parseInt(propm.group(2)),
						Integer.parseInt(propm.group(3)),
						Integer.parseInt(propm.group(4)),
						Integer.parseInt(propm.group(6)),
						Integer.parseInt(propm.group(7)));
			} else if(propm.groupCount() >= 4) {
			dateEntry = new DateEntry(getActivity(), datesView,
					"SCHEDULED", Integer.parseInt(propm.group(2)),
					Integer.parseInt(propm.group(3)), Integer.parseInt(propm
							.group(4)));
			}
			
			this.scheduledEntry = dateEntry;
		}
	}
	
	private void addDateScheduled() {
		DateEntry dateEntry = new DateEntry(getActivity(), datesView,
				"SCHEDULED");
		this.scheduledEntry = dateEntry;
	}
	
	private void addDateDeadline() {
		DateEntry dateEntry = new DateEntry(getActivity(), datesView,
				"DEADLINE");
		this.deadlineEntry = dateEntry;
	}
	
	
	private class DateEntry extends TableRow {
		private TableLayout parent;
		private Button dateButton;
		private Button timeButton;
		
		private int year;
		private int monthOfYear;
		private int dayOfMonth;
		private int timeOfDay;
		private int minute;
		
		public DateEntry(Context context, TableLayout parent, String title) {
			super(context);
			final Calendar c = Calendar.getInstance();
			this.year = c.get(Calendar.YEAR);
			this.monthOfYear = c.get(Calendar.MONTH);
			this.dayOfMonth = c.get(Calendar.DAY_OF_MONTH);
			this.minute = 0;
			this.timeOfDay = 0;
			init(context, parent, title);
		}

		public DateEntry(Context context, TableLayout parent, String title,
				int year, int monthOfYear, int dayOfMonth) {
			super(context);
			this.year = year;
			this.monthOfYear = monthOfYear;
			this.dayOfMonth = dayOfMonth;
			this.minute = 0;
			this.timeOfDay = 0;
			init(context, parent, title);
		}
		
		public DateEntry(Context context, TableLayout parent, String title,
				int year, int monthOfYear, int dayOfMonth, int timeOfDay, int minute) {
			super(context);
			this.year = year;
			this.monthOfYear = monthOfYear;
			this.dayOfMonth = dayOfMonth;
			this.timeOfDay = timeOfDay;
			this.minute = minute;
			init(context, parent, title);
		}
		
		private void init(Context context, TableLayout parent, String title) {
			this.parent = parent;

			LayoutInflater layoutInflater = (LayoutInflater) context
					.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			TableRow row = (TableRow) layoutInflater.inflate(
					R.layout.editnode_datelayout, this);
			parent.addView(row);

			Button removeButton = (Button) findViewById(R.id.dateRemove);
			removeButton.setOnClickListener(removeListener);
			
			TextView textView = (TextView) findViewById(R.id.dateText);
			textView.setText(title);

			dateButton = (Button) findViewById(R.id.dateButton);
			dateButton.setOnClickListener(new View.OnClickListener() {
				public void onClick(View v) {
					FragmentTransaction ft = getSupportFragmentManager()
							.beginTransaction();
					DialogFragment newFragment = new DatePickerDialogFragment(
							dateChangeListener);
					newFragment.show(ft, "dialog");
				}
			});
			
			timeButton = (Button) findViewById(R.id.dateTimeButton);
			timeButton.setOnClickListener(new View.OnClickListener() {
				public void onClick(View v) {
					FragmentTransaction ft = getSupportFragmentManager()
							.beginTransaction();
					DialogFragment newFragment = new TimePickerDialogFragment(
							timeChangeListener);
					newFragment.show(ft, "dialog");
				}
			});
			
			setDate();
			setTime();
		}
		
		public String getDate() {
			return title + ": <" + dateButton.getText().toString() + ">";
		}

		
		private void setTime(int timeOfDay, int minute) {
			this.timeOfDay = timeOfDay;
			this.minute = minute;
			timeButton.setText(String.format("%02d:%02d", timeOfDay, minute));
		}
		
		private void setTime() {
			timeButton.setText(String.format("%02d:%02d", timeOfDay, minute));
		}
		
		private void setDate() {
			dateButton.setText(String.format("%d-%02d-%02d", year, monthOfYear, dayOfMonth));
		}
		
		private void setDate(int year, int monthOfYear, int dayOfMonth) {
			this.year = year;
			this.monthOfYear = monthOfYear;
			this.dayOfMonth = dayOfMonth;
			
			dateButton.setText(year + "-" + monthOfYear + "-" + dayOfMonth);
		}

		private DatePickerDialog.OnDateSetListener dateChangeListener = new DatePickerDialog.OnDateSetListener() {
			public void onDateSet(DatePicker view, int year, int monthOfYear,
					int dayOfMonth) {
				setDate(year, monthOfYear + 1, dayOfMonth);
			}
		};
		
		private TimePickerDialog.OnTimeSetListener timeChangeListener = new TimePickerDialog.OnTimeSetListener() {
			public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
				setTime(hourOfDay, minute);
			}
		};

		
		private class TimePickerDialogFragment extends DialogFragment {
			private OnTimeSetListener callback;

			public TimePickerDialogFragment(OnTimeSetListener callback) {
				this.callback = callback;
			}

			public Dialog onCreateDialog(Bundle savedInstanceState) {
				return new TimePickerDialog(getActivity(), callback, timeOfDay, minute, true);
			}
		}
		
		private class DatePickerDialogFragment extends DialogFragment {
			private OnDateSetListener callback;

			public DatePickerDialogFragment(OnDateSetListener callback) {
				this.callback = callback;
			}

			public Dialog onCreateDialog(Bundle savedInstanceState) {
				return new DatePickerDialog(getActivity(), callback, year,
						monthOfYear - 1, dayOfMonth);
			}
		}
		
		private void remove() {
			parent.removeView(this);
		}

		private View.OnClickListener removeListener = new View.OnClickListener() {
			public void onClick(View v) {
				remove();
			}
		};
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
	
	public String getTagsSelection() {
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
	
	public String getTitle() {
		return this.titleView.getText().toString();
	}
	
	public String getTodo() {
		return todoStateView.getSelectedItem().toString();
	}
	
	public String getPriority() {
		return priorityView.getSelectedItem().toString();
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

			button = (Button) findViewById(R.id.editnode_tag_remove);
			button.setOnClickListener(removeListener);
			
			if(selection.endsWith(":")) {
				selectionExtra = ":";
				selection = selection.replace(":", "");
			}

			spinner = (Spinner) row.findViewById(R.id.editnode_tag_list);
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
