package com.matburt.mobileorg.Gui.Agenda;

import java.util.ArrayList;
import java.util.Arrays;

import android.os.Bundle;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;

import com.actionbarsherlock.app.SherlockActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.matburt.mobileorg.R;
import com.matburt.mobileorg.OrgData.OrgProviderUtils;

public class AgendaEntrySetting extends SherlockActivity {
	public static final String AGENDA_NUMBER = "agenda_number";
	public static final String ENTRY_NUMBER = "entry_number";
	
	private int agendaPos;
	private int entryPos;
	
	private EditText titleView;
	private EditText payloadView;
	private EditText todoView;
	private EditText priorityView;
	private EditText tagsView;
	private CheckBox filterHabitsView;

	private CheckBox activeTodosView;
	private LinearLayout fileListView;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.agenda_entry_setting);
		
		this.titleView = (EditText) findViewById(R.id.agenda_entry_title);
		this.priorityView = (EditText) findViewById(R.id.agenda_entry_priority);
		this.todoView = (EditText) findViewById(R.id.agenda_entry_todo);
		this.tagsView = (EditText) findViewById(R.id.agenda_entry_tag);
		this.payloadView = (EditText) findViewById(R.id.agenda_entry_payload);

		this.filterHabitsView = (CheckBox) findViewById(R.id.agenda_entry_habits);
		this.activeTodosView = (CheckBox) findViewById(R.id.agenda_entry_active_todos);

		this.agendaPos = getIntent().getIntExtra(AGENDA_NUMBER, -1);
		this.entryPos = getIntent().getIntExtra(ENTRY_NUMBER, -1);
	
		this.fileListView = (LinearLayout) findViewById(R.id.agenda_entry_files);
		
		setupSettings(OrgAgenda.getAgendaEntry(agendaPos, entryPos, this));
	}
	
	public void setupSettings(OrgQueryBuilder agenda) {
		titleView.setText(agenda.title);
		payloadView.setText(combineToString(agenda.payloads));
		todoView.setText(combineToString(agenda.todos));
		priorityView.setText(combineToString(agenda.priorities));
		tagsView.setText(combineToString(agenda.tags));
		filterHabitsView.setChecked(agenda.filterHabits);
		activeTodosView.setChecked(agenda.activeTodos);

		setupFileList(agenda);
	}

	public OrgQueryBuilder getQueryFromSettings() {
		OrgQueryBuilder agenda = new OrgQueryBuilder(titleView.getText().toString());
		
		agenda.tags = splitToArrayList(tagsView.getText().toString());
		agenda.payloads = splitToArrayList(payloadView.getText().toString());
		agenda.priorities = splitToArrayList(priorityView.getText().toString());
		agenda.todos = splitToArrayList(todoView.getText().toString());
		agenda.filterHabits = filterHabitsView.isChecked();
		agenda.activeTodos = activeTodosView.isChecked();
		
		agenda.files = getFileList();
		
		return agenda;
	}
	
	private String combineToString(ArrayList<String> list) {
		if(list.size() == 0)
			return "";
		
		StringBuilder builder = new StringBuilder();
		for(String item: list)
			builder.append(item).append(":");
		
		builder.delete(builder.length() - 1, builder.length());
		return builder.toString();
	}
	
	private ArrayList<String> splitToArrayList(String string) {
		ArrayList<String> result = new ArrayList<String>();
		
		if(string == null || string.trim().length() == 0)
			return result;
		
		String[] split = string.split(":");
		return new ArrayList<String>(Arrays.asList(split));
	}
	
	private void setupFileList(OrgQueryBuilder agenda) {
		ArrayList<String> filenames = OrgProviderUtils
				.getFilenames(getContentResolver());
		for (String filename : filenames) {
			CheckBox checkBox = new CheckBox(this);
			checkBox.setText(filename);
			checkBox.setChecked(agenda.files.contains(filename));

			fileListView.addView(checkBox);
		}
	}

	private ArrayList<String> getFileList() {
		ArrayList<String> files = new ArrayList<String>();

		int fileSize = fileListView.getChildCount();
		for (int i = 0; i < fileSize; i++) {
			CheckBox checkBox = (CheckBox) fileListView.getChildAt(i);
			if (checkBox.isChecked())
				files.add(checkBox.getText().toString());
		}

		return files;
	}


	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		getSupportMenuInflater().inflate(R.menu.agenda_entry, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch(item.getItemId()) {
		case R.id.agenda_entry_save:
			OrgAgenda.writeAgendaEntry(getQueryFromSettings(), agendaPos,
					entryPos, this);
			finish();
			break;
		case R.id.agenda_entry_cancel:
			finish();
			break;
		
		default:
			return super.onOptionsItemSelected(item);
		}
		
		return true;
	}
}
