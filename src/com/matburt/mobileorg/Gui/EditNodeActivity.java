package com.matburt.mobileorg.Gui;

import java.util.ArrayList;
import java.util.HashMap;

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
	private EditText mTitle;
	private TextView mBody;
	private Spinner mPriority;
	private Spinner mTodoState;
	private EditText mTags;
	private Node node;
	private String actionMode;

	private static int EDIT_BODY = 1;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.editnode);

		this.mTitle = (EditText) this.findViewById(R.id.title);
		this.mPriority = (Spinner) this.findViewById(R.id.priority);
		this.mTodoState = (Spinner) this.findViewById(R.id.todo_state);
		this.mTags = (EditText) this.findViewById(R.id.tags);

		this.mBody = (TextView) this.findViewById(R.id.body);
		mBody.setOnClickListener(editBodyListener);
		
		Intent txtIntent = getIntent();
		this.mNodePath = txtIntent.getIntegerArrayListExtra("nodePath");
		this.actionMode = txtIntent.getStringExtra("actionMode");
			
		
		initDisplay();
		
		Button button = (Button) this.findViewById(R.id.cancel);
		button.setOnClickListener(cancelListener);
		button = (Button) this.findViewById(R.id.save);
		button.setOnClickListener(saveNodeListener);		
	}

	private void initDisplay() {
		MobileOrgApplication appInst = (MobileOrgApplication) this
				.getApplication();
		if (this.actionMode.equals("edit")) {
			node = appInst.getNode(mNodePath);
			node.applyEdits(appInst.findEdits(node.nodeId));

			mTitle.setText(node.name);
			mBody.setText(node.payload);
			mTags.setText(node.getTagString());
			appInst.popSelection();
		}
		if (this.actionMode.equals("create")) {
			node = new Node();
		}
		
		MobileOrgDatabase mOrgDb = new MobileOrgDatabase(this);
		setSpinner(mTodoState, mOrgDb.getTodos(), node.todo);
		setSpinner(mPriority, mOrgDb.getPriorities(), node.priority);
		mOrgDb.close();
	}
	
	private void setSpinner(Spinner view, ArrayList<?> data, String selection) {
		// I can't use a simple cursor here because the todos table does not
		// store an _id yet.
		// Instead, we'll retrieve the todos from the database, and we'll use an
		// array adapter.
		ArrayList<String> choices = new ArrayList<String>();
		choices.add("");
		for (Object group : data) {
			if (group instanceof HashMap) {
				for (String key : ((HashMap<String, Integer>) group).keySet()) {
					choices.add(key);
				}
			} else if (group instanceof ArrayList) {
				for (String key : (ArrayList<String>) group) {
					choices.add(key);
				}
			}
		}
		ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,
				android.R.layout.simple_spinner_item, choices);
		adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		view.setAdapter(adapter);
		int pos = choices.indexOf(selection);
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
				mBody.setText(result);
			}
		}
	}

	private void save() {
		CreateEditNote creator = new CreateEditNote(this);
		String newTitle = mTitle.getText().toString();
		String newTodo = null;
		String newPriority = null;

		Object tdSelected = mTodoState.getSelectedItem();
		Object priSelected = mPriority.getSelectedItem();

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
			if (!node.payload.equals(mBody.getText().toString())) {
				creator.editNote("body", node.nodeId, newTitle, node.payload,
						mBody.getText().toString());
				node.payload = mBody.getText().toString();
			}
		} else if (this.actionMode.equals("create")) {
			node.name = newTitle;
			node.todo = newTodo;
			node.priority = newPriority;
			node.payload = mBody.getText().toString();
			
			creator.writeNote(node.generateNoteEntry());
		}
		creator.close();
	}
}
