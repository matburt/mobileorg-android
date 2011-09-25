package com.matburt.mobileorg.Gui;

import java.io.IOException;
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
import com.matburt.mobileorg.Parsing.NodeWriter;
import com.matburt.mobileorg.Parsing.OrgDatabase;
import com.matburt.mobileorg.Parsing.Node;

public class NodeEditActivity extends Activity {
	public final static String ACTIONMODE_CREATE = "create";
	public final static String ACTIONMODE_EDIT = "edit";

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

		if (this.actionMode.equals(ACTIONMODE_CREATE)) {
			node = new Node();
		} else if (this.actionMode.equals(ACTIONMODE_EDIT)) {
			node = appInst.getNode(mNodePath);
			titleView.setText(node.name);
			payloadView.setText(node.payload);
			tagsView.setText(node.getTagString());
			
			appInst.popSelection();
		}

		OrgDatabase appdb = new OrgDatabase(this);
		setSpinner(todoStateView, appdb.getTodods(), node.todo);
		setSpinner(priorityView, appdb.getPriorities(), node.priority);
		appdb.close();
	}

	private void setSpinner(Spinner view, ArrayList<String> data,
			String selection) {
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
			Intent intent = new Intent(v.getContext(),
					NodeEditBodyActivity.class);
			intent.putExtra(NodeEditBodyActivity.DISPLAY_STRING, node.payload);
			startActivityForResult(intent, EDIT_BODY);
		}
	};

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == EDIT_BODY) {
			if (resultCode == RESULT_OK) {
				String result = data
						.getStringExtra(NodeEditBodyActivity.RESULT_STRING);
				node.payload = result;
				payloadView.setText(result);
			}
		}
	}

	private void save() {
		String newTitle = titleView.getText().toString();
		String newTodo = todoStateView.getSelectedItem().toString();
		String newPriority = priorityView.getSelectedItem().toString();
		String newPayload = payloadView.getText().toString();

		NodeWriter writer = new NodeWriter(this);
		
		if (this.actionMode.equals(ACTIONMODE_CREATE)) {
			node.name = newTitle;
			node.todo = newTodo;
			node.priority = newPriority;
			node.payload = newPayload;

			try {
				writer.write(node);		
			} catch (IOException e) {
			}
		} else if (this.actionMode.equals(ACTIONMODE_EDIT)) {
			try {
				writer.editNode(node, newTitle, newTodo, newPriority,
						newPayload);
			} catch (IOException e) {
			}
		}
		writer.close();
	}
}
