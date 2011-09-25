package com.matburt.mobileorg.Gui;

import java.util.ArrayList;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

import com.matburt.mobileorg.MobileOrgApplication;
import com.matburt.mobileorg.R;
import com.matburt.mobileorg.Parsing.CreateEditNote;
import com.matburt.mobileorg.Parsing.MobileOrgDatabase;
import com.matburt.mobileorg.Parsing.Node;

public class EditNodeActivity extends Activity {
	private ArrayList<Integer> mNodePath;
	private EditText titleView;
	private TextView payloadView;
	private Spinner priorityView;
	private Spinner todoStateView;
	private EditText tagsView;
	private Node node;
	private String actionMode;

	private static int EDIT_BODY = 1;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.editnode);

		this.titleView = (EditText) this.findViewById(R.id.title);
		this.priorityView = (Spinner) this.findViewById(R.id.priority);
		this.todoStateView = (Spinner) this.findViewById(R.id.todo_state);
		this.tagsView = (EditText) this.findViewById(R.id.tags);

		this.payloadView = (TextView) this.findViewById(R.id.body);
		payloadView.setOnClickListener(editBodyListener);
		
		Intent intent = getIntent();
		this.mNodePath = intent.getIntegerArrayListExtra("nodePath");
		this.actionMode = intent.getStringExtra("actionMode");
	
		initDisplay();
		
		Button button = (Button) this.findViewById(R.id.cancel);
		button.setOnClickListener(cancelListener);
		button = (Button) this.findViewById(R.id.save);
		button.setOnClickListener(saveNodeListener);		
	}

	private void initDisplay() {
		MobileOrgApplication appInst = (MobileOrgApplication) this
				.getApplication();

		if (this.actionMode.equals("create")) {
			node = new Node();
		} else if (this.actionMode.equals("edit")) {
			node = appInst.getNode(mNodePath);
			node.applyEdits(appInst.findEdits(node.nodeId));

			titleView.setText(node.name);
			payloadView.setText(node.payload);
			tagsView.setText(node.getTagString());
			appInst.popSelection();
		}
		
		MobileOrgDatabase appdb = new MobileOrgDatabase(this);
		setSpinner(todoStateView, appdb.getTodods(), node.todo);
		setSpinner(priorityView, appdb.getPriorities(), node.priority);
		appdb.close();
	}
	
	private void setSpinner(Spinner view, ArrayList<String> data, String selection) {
		data.add("");
		
		ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,
				android.R.layout.simple_spinner_item, data);
		adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		view.setAdapter(adapter);
		int pos = data.indexOf(selection);
		if (pos < 0) {
			pos = 0;
		}
		view.setSelection(pos);
	}


	View.OnClickListener saveNodeListener = new View.OnClickListener() {
		public void onClick(View v) {
			save();
			setResult(RESULT_OK);
			finish();
		}
	};
	
	View.OnClickListener cancelListener = new View.OnClickListener() {
	    public void onClick(View v) {
	    	setResult(RESULT_CANCELED);
	    	finish();
	    }
	  };
	
	View.OnClickListener editBodyListener = new View.OnClickListener() {
		public void onClick(View v) {
			Intent intent = new Intent(v.getContext(), EditNodeBodyActivity.class);
			intent.putExtra(EditNodeBodyActivity.DISPLAY_STRING, node.payload);
			startActivityForResult(intent, EDIT_BODY);
		}
	};

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == EDIT_BODY) {
			if (resultCode == RESULT_OK) {
				String result = data
						.getStringExtra(EditNodeBodyActivity.RESULT_STRING);
				node.payload = result;
				payloadView.setText(result);
			}
		}
	}

	private void save() {
		CreateEditNote creator = new CreateEditNote(this);
		String newTitle = titleView.getText().toString();
		String newTodo = null;
		String newPriority = null;

		Object tdSelected = todoStateView.getSelectedItem();
		Object priSelected = priorityView.getSelectedItem();

		if (tdSelected != null) {
			newTodo = tdSelected.toString();
		}

		if (priSelected != null) {
			newPriority = priSelected.toString();
		}

		if (this.actionMode.equals("edit")) {
			if (!node.name.equals(newTitle)) {
				creator.editNote("heading", node.nodeId, newTitle, node.name,
						newTitle);
				node.name = newTitle;
			}
			if (newTodo != null && !node.todo.equals(newTodo)) {
				creator.editNote("todo", node.nodeId, newTitle, node.todo,
						newTodo);
				node.todo = newTodo;
			}
			if (newPriority != null && !node.priority.equals(newPriority)) {
				creator.editNote("priority", node.nodeId, newTitle,
						node.priority, newPriority);
				node.priority = newPriority;
			}
			if (!node.payload.equals(payloadView.getText().toString())) {
				creator.editNote("body", node.nodeId, newTitle, node.payload,
						payloadView.getText().toString());
				node.payload = payloadView.getText().toString();
			}
		} else if (this.actionMode.equals("create")) {
			node.name = newTitle;
			node.todo = newTodo;
			node.priority = newPriority;
			node.payload = payloadView.getText().toString();
			
			creator.writeNote(node.generateNoteEntry());
		}
		creator.close();
	}
}
