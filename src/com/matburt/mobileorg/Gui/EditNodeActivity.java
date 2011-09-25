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
	private Node mNode;
	private MobileOrgDatabase mOrgDb;
	private String actionMode;

	private static int EDIT_BODY = 1;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.editnode);

		this.mTitle = (EditText) this.findViewById(R.id.title);
		this.mBody = (TextView) this.findViewById(R.id.body);
		this.mPriority = (Spinner) this.findViewById(R.id.priority);
		this.mTodoState = (Spinner) this.findViewById(R.id.todo_state);
		this.mTags = (EditText) this.findViewById(R.id.tags);

		Intent txtIntent = getIntent();
		this.mNodePath = txtIntent.getIntegerArrayListExtra("nodePath");
		this.actionMode = txtIntent.getStringExtra("actionMode");
			
		this.mOrgDb = new MobileOrgDatabase(this);
		this.populateDisplay();
		
		Button button = (Button) this
				.findViewById(R.id.cancel);
		button.setOnClickListener(cancelListener);
		button = (Button) this.findViewById(R.id.save);
		button.setOnClickListener(saveNodeListener);		
		mBody.setOnClickListener(editBodyListener);
	}

	private void populateDisplay() {
		MobileOrgApplication appInst = (MobileOrgApplication) this
				.getApplication();
		if (this.actionMode.equals("edit")) {
			mNode = appInst.getNode(mNodePath);
			mNode.applyEdits(appInst.findEdits(mNode.nodeId));

			mTitle.setText(mNode.name);
			mBody.setText(mNode.payload);
			mTags.setText(mNode.getTagString());
			appInst.popSelection();
		}
		if (this.actionMode.equals("create")) {
			mNode = new Node();
		}
		setSpinner(mTodoState, this.mOrgDb.getTodos(), mNode.todo);
		setSpinner(mPriority, this.mOrgDb.getPriorities(), mNode.priority);
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
			if (mNode.nodeId != null && mNode.nodeId.length() > 0) {
				intent.putExtra("nodeId", mNode.nodeId);
			}
			intent.putExtra("editType", "body");
			intent.putExtra("txtValue", mNode.payload);
			intent.putExtra("nodeTitle", mNode.nodeTitle);
			startActivityForResult(intent, EDIT_BODY);
		}
	};


	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == EDIT_BODY) {
			if (data == null || data.getStringExtra("text") == null) {
				return;
			}
			String newBody = data.getStringExtra("text");
			mNode.payload = newBody;
			mBody.setText(newBody);
			populateDisplay();
		}
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
			if (!mNode.name.equals(newTitle)) {
				creator.editNote("heading", mNode.nodeId, newTitle, mNode.name,
						newTitle);
				mNode.name = newTitle;
			}
			if (newTodo != null && !mNode.todo.equals(newTodo)) {
				creator.editNote("todo", mNode.nodeId, newTitle, mNode.todo,
						newTodo);
				mNode.todo = newTodo;
			}
			if (newPriority != null && !mNode.priority.equals(newPriority)) {
				creator.editNote("priority", mNode.nodeId, newTitle,
						mNode.priority, newPriority);
				mNode.priority = newPriority;
			}
			if (!mNode.payload.equals(mBody.getText().toString())) {
				creator.editNote("body", mNode.nodeId, newTitle, mNode.payload,
						mBody.getText().toString());
				mNode.payload = mBody.getText().toString();
			}
		} else if (this.actionMode.equals("create")) {
			mNode.name = newTitle;
			mNode.todo = newTodo;
			mNode.priority = newPriority;
			mNode.payload = mBody.getText().toString();
			
			creator.writeNote(mNode.generateNoteEntry());
		}
		creator.close();
	}

	@Override
	public void onDestroy() {
		this.mOrgDb.close();
		super.onDestroy();
	}

}
