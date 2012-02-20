package com.matburt.mobileorg.Gui;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import com.matburt.mobileorg.R;
import com.matburt.mobileorg.Parsing.MobileOrgApplication;
import com.matburt.mobileorg.Parsing.NodeWrapper;
import com.matburt.mobileorg.Parsing.OrgDatabase;
import com.matburt.mobileorg.Parsing.OrgFile;
import com.matburt.mobileorg.Synchronizers.Synchronizer;

public class NodeEditActivity extends Activity {
	public final static String ACTIONMODE_CREATE = "create";
	public final static String ACTIONMODE_EDIT = "edit";
	private final static int EDIT_BODY = 1;

	private EditText titleView;
	private TextView payloadView;
	private Spinner priorityView;
	private Spinner todoStateView;
	private TableLayout tagsView;
	private ArrayList<TagEntry> tagEntries = new ArrayList<TagEntry>();

	private NodeWrapper node;
	private String actionMode;
	
	
	private OrgDatabase orgDB;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.editnode);

		this.titleView = (EditText) this.findViewById(R.id.title);
		this.priorityView = (Spinner) this.findViewById(R.id.priority);
		this.todoStateView = (Spinner) this.findViewById(R.id.todo_state);
		this.tagsView = (TableLayout) this.findViewById(R.id.tags);

		this.payloadView = (TextView) this.findViewById(R.id.body);
		payloadView.setOnClickListener(editBodyListener);

		Intent intent = getIntent();
		this.actionMode = intent.getStringExtra("actionMode");

		MobileOrgApplication appInst = (MobileOrgApplication) this.getApplication();
		this.orgDB = appInst.getDB();
		
		initDisplay();

		Button button = (Button) this.findViewById(R.id.cancel);
		button.setOnClickListener(cancelListener);
		button = (Button) this.findViewById(R.id.save);
		button.setOnClickListener(saveNodeListener);
	}

	private void initDisplay() {
		MobileOrgApplication appInst = (MobileOrgApplication) this
				.getApplication();

		Intent intent = getIntent();
		
		String defaultTodo = PreferenceManager.getDefaultSharedPreferences(
				getApplicationContext()).getString("defaultTodo", "");

		if(this.actionMode == null) {
			this.actionMode = ACTIONMODE_CREATE;

			String subject = intent
					.getStringExtra("android.intent.extra.SUBJECT");
			String text = intent.getStringExtra("android.intent.extra.TEXT");
			titleView.setText(subject);

			node = new NodeWrapper(null);
			payloadView.setText(text);
			setSpinner(todoStateView, appInst.getDB().getTodos(), defaultTodo);
			setSpinner(priorityView, appInst.getDB().getPriorities(), "");
		}
		else if (this.actionMode.equals(ACTIONMODE_CREATE)) {
			titleView.setText("");
			node = new NodeWrapper(null);

			payloadView.setText("");
			setSpinner(todoStateView, appInst.getDB().getTodos(), defaultTodo);
			setSpinner(priorityView, appInst.getDB().getPriorities(), "");
		} else if (this.actionMode.equals(ACTIONMODE_EDIT)) {
			long nodeId = intent.getLongExtra("node_id", 0);

			Cursor cursor = appInst.getDB().getNode(nodeId);
			cursor.moveToFirst();
			node = new NodeWrapper(cursor);
			
			titleView.setText(node.getName());
			payloadView.setText(node.getCleanedPayload(this.orgDB));
			//payloadView.setText(node.getRawPayload(this.orgDB));
			
			setupTags(appInst.getDB().getTags());

			setSpinner(todoStateView, appInst.getDB().getTodos(), node.getTodo());
			setSpinner(priorityView, appInst.getDB().getPriorities(), node.getPriority());
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
		TagEntry tagEntry = new TagEntry(this, tagsView, orgDB.getTags(), tag);
		tagsView.addView(tagEntry);
		tagEntries.add(tagEntry);
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

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
	    inflater.inflate(R.menu.nodeedit_menu, menu);
		return true;
	}
    
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.nodeedit_tag:
			addTag("");
			return true;
		}
		return false;
	}
	
	private View.OnClickListener saveNodeListener = new View.OnClickListener() {
		public void onClick(View v) {
			save();
			setResult(RESULT_OK);
			finish();
		}
	};

	private View.OnClickListener editBodyListener = new View.OnClickListener() {
		public void onClick(View v) {
			Intent intent = new Intent(v.getContext(),
					NodeEditBodyActivity.class);
			intent.putExtra(NodeEditBodyActivity.DISPLAY_STRING, payloadView.getText().toString());
			startActivityForResult(intent, EDIT_BODY);
		}
	};

	private View.OnClickListener cancelListener = new View.OnClickListener() {
		public void onClick(View v) {
			doCancel();
		}
	};
	
	@Override
	public void onBackPressed() {
		doCancel();
	}
	
	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == EDIT_BODY) {
			if (resultCode == RESULT_OK) {
				String result = data
						.getStringExtra(NodeEditBodyActivity.RESULT_STRING);
				payloadView.setText(result);
			}
		}
	}


	private void doCancel() {
		if(!hasEdits()) {
			setResult(RESULT_CANCELED);
			finish();
			return;
		}
		
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setMessage(R.string.node_edit_prompt)
				.setCancelable(false)
				.setPositiveButton(R.string.yes,
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int id) {
								setResult(RESULT_CANCELED);
								finish();
							}
						})
				.setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int id) {
						dialog.cancel();
					}
				});
		builder.create().show();
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
	
	private boolean hasEdits() {
		String newPayload = payloadView.getText().toString();
		String newTitle = titleView.getText().toString();
		String newTodo = todoStateView.getSelectedItem().toString();
		String newPriority = priorityView.getSelectedItem().toString();
		String newTags = getTagsSelection();
		
		Log.d("MobileOrg", "Got tags: " + newTags + " | " + node.getTags());
		
		if (this.actionMode.equals(ACTIONMODE_CREATE)) {
			if (newPayload.length() == 0 && newTitle.length() == 0)
				return false;
		} else if (this.actionMode.equals(ACTIONMODE_EDIT)) {
			if (newPayload.equals(node.getCleanedPayload(this.orgDB))
					&& newTitle.equals(node.getName())
					&& newTodo.equals(node.getTodo())
					&& newTags.equals(node.getTags())
					&& newPriority.equals(node.getPriority()))
				return false;
		}
		
		return true;
	}
	
	public static String getTimestamp() {
		SimpleDateFormat sdf = new SimpleDateFormat("[yyyy-MM-dd EEE HH:mm]");		
		return sdf.format(new Date());
	}
	
	private void save() {
		String newTitle = titleView.getText().toString();
		String newTodo = todoStateView.getSelectedItem().toString();
		String newPriority = priorityView.getSelectedItem().toString();
		String newPayload = payloadView.getText().toString();
		String newTags = getTagsSelection();
		
		if (this.actionMode.equals(ACTIONMODE_CREATE)) {
			MobileOrgApplication appInst = (MobileOrgApplication) this.getApplication();
			OrgDatabase orgDB = appInst.getDB();
			long file_id = orgDB.addOrUpdateFile(OrgFile.CAPTURE_FILE, "Captures", "", true);
			Long parent = orgDB.getFileNodeId(OrgFile.CAPTURE_FILE);
			long node_id = orgDB.addNode(parent, newTitle, newTodo, newPriority, newTags, file_id);
			
			boolean addTimestamp = PreferenceManager.getDefaultSharedPreferences(
					this).getBoolean("captureWithTimestamp", false);
			if(addTimestamp)
				newPayload += "\n" + getTimestamp() + "\n";
			
			orgDB.addNodePayload(node_id, newPayload);
			
		} else if (this.actionMode.equals(ACTIONMODE_EDIT)) {
			try {
				editNode(newTitle, newTodo, newPriority, newPayload, newTags);
			} catch (IOException e) {
			}
		}
		Intent intent = new Intent(Synchronizer.SYNC_UPDATE);
		intent.putExtra(Synchronizer.SYNC_DONE, true);
		intent.putExtra("showToast", false);
		sendBroadcast(intent);
	}
	
	/**
	 * Takes a Node and four strings, representing edits to the node.
	 * This function will generate a new edit entry for each value that was 
	 * changed.
	 */
	private void editNode(String newTitle, String newTodo,
			String newPriority, String newPayload, String newTags) throws IOException {
		if (!node.getName().equals(newTitle)) {
			if(node.getFileName(orgDB).equals(OrgFile.CAPTURE_FILE) == false)
				orgDB.addEdit("heading", node.getNodeId(orgDB), newTitle, node.getName(), newTitle);
			node.setName(newTitle, orgDB);
		}
		if (newTodo != null && !node.getTodo().equals(newTodo)) {
			if(node.getFileName(orgDB).equals(OrgFile.CAPTURE_FILE) == false)
				orgDB.addEdit("todo", node.getNodeId(orgDB), newTitle, node.getTodo(), newTodo);
			node.setTodo(newTodo, orgDB);
		}
		if (newPriority != null && !node.getPriority().equals(newPriority)) {
			if(node.getFileName(orgDB).equals(OrgFile.CAPTURE_FILE) == false)
				orgDB.addEdit("priority", node.getNodeId(orgDB), newTitle, node.getPriority(),
					newPriority);
			node.setPriority(newPriority, orgDB);
		}
		if (!node.getCleanedPayload(orgDB).equals(newPayload)) {
			String newRawPayload = node.getPayloadResidue(orgDB) + newPayload;
	
			if(node.getFileName(orgDB).equals(OrgFile.CAPTURE_FILE) == false)
				orgDB.addEdit("body", node.getNodeId(orgDB), newTitle, node.getRawPayload(orgDB), newRawPayload);
			node.setPayload(newRawPayload, orgDB);
		}
		if(!node.getTags().equals(newTags)) {
			if(node.getFileName(orgDB).equals(OrgFile.CAPTURE_FILE) == false) {
				orgDB.addEdit("tags", node.getNodeId(orgDB), newTitle,
						node.getTagsWithoutInheritet(),
						NodeWrapper.getTagsWithoutInheritet(newTags));
			}
			node.setTags(newTags, orgDB);
		}
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
	};
}
