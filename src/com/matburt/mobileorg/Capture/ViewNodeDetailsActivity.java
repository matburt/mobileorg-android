package com.matburt.mobileorg;

import java.util.ArrayList;
import java.util.HashMap;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

public class ViewNodeDetailsActivity extends Activity implements OnClickListener {
	protected ArrayList<Integer> mNodePath;
	protected EditText mTitle;
	protected TextView mBody;
	protected Spinner mPriority;
	protected Spinner mTodoState;
	protected EditText mTags;
	protected Button mViewAsDocument;
	protected Node mNode;
	protected MobileOrgDatabase mOrgDb;
    protected String actionMode;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
	    super.onCreate(savedInstanceState);
        setContentView(R.layout.node_details);
        Intent txtIntent = getIntent();
        this.mNodePath = txtIntent.getIntegerArrayListExtra("nodePath");
        this.actionMode = txtIntent.getStringExtra("actionMode");
        this.mTitle = (EditText) this.findViewById(R.id.title);
        this.mBody = (TextView) this.findViewById(R.id.body);
        this.mPriority = (Spinner) this.findViewById(R.id.priority);
        this.mTodoState = (Spinner) this.findViewById(R.id.todo_state);
        this.mTags = (EditText) this.findViewById(R.id.tags);
        this.mViewAsDocument = (Button) this.findViewById(R.id.view_as_document);
        this.mOrgDb = new MobileOrgDatabase(this);
        this.populateDisplay();
        
        mViewAsDocument.setOnClickListener(this);
        mBody.setOnClickListener(this);
    }
	
	public void setSpinner(Spinner view, ArrayList data, String selection) {
		// I can't use a simple cursor here because the todos table does not store an _id yet.
		// Instead, we'll retrieve the todos from the database, and we'll use an array adapter.
		ArrayList choices = new ArrayList();
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
		ArrayAdapter adapter = new ArrayAdapter(this, android.R.layout.simple_spinner_item,
				choices);
		adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		view.setAdapter(adapter);
		int pos = choices.indexOf(selection);
		if (pos < 0) { pos = 0; }
		view.setSelection(pos);
	}
	
	public void populateDisplay() {
		MobileOrgApplication appInst = (MobileOrgApplication)this.getApplication();
        if (this.actionMode.equals("edit")) {
                mNode = appInst.getNode(mNodePath);
                mNode.applyEdits(appInst.findEdits(mNode.nodeId));
        
                Node parent = appInst.getParent(mNodePath);
                mTitle.setText(mNode.nodeName);
                mBody.setText(mNode.nodePayload);
                mTags.setText(mNode.tagString);
                setSpinner(mTodoState, this.mOrgDb.getTodos(), mNode.todo);
                setSpinner(mPriority, this.mOrgDb.getPriorities(),
                           mNode.priority);
                appInst.popSelection();
        }
    }

	@Override
	protected void onPause() {
		super.onPause();
		save();
	}

	@Override
	public void onClick(View v) {
		if (v.equals(mViewAsDocument)) {
			Intent intent = new Intent(this, SimpleTextDisplay.class);
			intent.putExtra("txtValue", mNode.nodePayload);
			this.startActivity(intent);
		}
		if (v.equals(mBody)) {
			save();
            //Capture Change:
            //Have it return the text from the capture to:
            //Create the new note with the body OR
            //Apply the body edit like the other elements
			Intent intent = new Intent(this, Capture.class);
			if (mNode.nodeId != null && mNode.nodeId.length() > 0) {
				intent.putExtra("nodeId", mNode.nodeId);	
			}
			intent.putExtra("editType", "body");
			intent.putExtra("txtValue", mNode.nodePayload);
			intent.putExtra("nodeTitle", mNode.nodeTitle);
			startActivityForResult(intent, EDIT_BODY);
		}
	}
	
	public void save() {
		CreateEditNote creator = new CreateEditNote(this);
		String newTitle = mTitle.getText().toString();
		String newTodo = mTodoState.getSelectedItem().toString();
		String newPriority = mPriority.getSelectedItem().toString();
		if (!mNode.nodeName.equals(newTitle)) {
        	creator.editNote("heading", mNode.nodeId, newTitle, mNode.nodeName, newTitle);
        	mNode.nodeName = newTitle;
		}
        if (!mNode.todo.equals(newTodo)) {
        	creator.editNote("todo", mNode.nodeId, newTitle, mNode.todo, newTodo);
        	mNode.todo = newTodo;
		}
       if (!mNode.nodePayload.equals(mNode.nodePayload)) {
        	creator.editNote("priority", mNode.nodeId, newTitle, mNode.priority, newPriority);
        	mNode.todo = newPriority;
		}
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == EDIT_BODY) {
			String body = data.getStringExtra("text");
			mNode.nodePayload = body;
			populateDisplay();
		}
	}
	private static int EDIT_BODY = 1;
}
