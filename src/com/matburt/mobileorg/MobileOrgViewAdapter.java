package com.matburt.mobileorg;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;

import android.content.Context;
import android.graphics.Color;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.matburt.mobileorg.Parsing.EditNode;
import com.matburt.mobileorg.Parsing.Node;

class MobileOrgViewAdapter extends BaseAdapter {

	public Node topNode;
	public ArrayList<Integer> nodeSelection;
	public ArrayList<EditNode> edits = new ArrayList<EditNode>();
	public ArrayList<HashMap<String, Integer>> allTodos = new ArrayList<HashMap<String, Integer>>();
	private Context context;
	private LayoutInflater lInflator;

	public MobileOrgViewAdapter(Context context, Node ndx,
			ArrayList<Integer> selection, ArrayList<EditNode> edits,
			ArrayList<HashMap<String, Integer>> allTodos) {

		this.topNode = ndx;
		this.lInflator = LayoutInflater.from(context);
		this.nodeSelection = selection;
		this.edits = edits;
		this.context = context;
		this.allTodos = allTodos;

		Log.d("MobileOrg" + this,
				"startup path=" + MobileOrgApplication.nodeSelectionStr(selection));
		if (selection != null) {
			for (int idx = 0; idx < selection.size(); idx++) {
				try {
					this.topNode = this.topNode.children.get(selection
							.get(idx));
				} catch (IndexOutOfBoundsException e) {
					Log.d("MobileOrg" + this, "IndexOutOfBounds on selection "
							+ selection.get(idx).toString() + " in node "
							+ this.topNode.name);
					return;
				}
			}
		}
	}

	public int getCount() {
		if (this.topNode == null || this.topNode.children == null)
			return 0;
		return this.topNode.children.size();
	}

	public Object getItem(int position) {
		//return new Node();
		return this.topNode.children.get(position);
	}

	public long getItemId(int position) {
		return position;
	}

	public ArrayList<EditNode> findEdits(String nodeId) {
		ArrayList<EditNode> thisEdits = new ArrayList<EditNode>();
		if (this.edits == null)
			return thisEdits;
		for (int idx = 0; idx < this.edits.size(); idx++) {
			String compareS = "";
			if (nodeId.indexOf("olp:") == 0)
				compareS = "olp:" + this.edits.get(idx).nodeId;
			else
				compareS = this.edits.get(idx).nodeId;
			if (compareS.equals(nodeId)) {
				thisEdits.add(this.edits.get(idx));
			}
		}
		return thisEdits;
	}

	public Integer findTodoState(String todoItem) {
		for (HashMap<String, Integer> group : this.allTodos) {
			for (String key : group.keySet()) {
				if (key.equals(todoItem))
					return group.get(key);
			}
		}
		return 0;
	}

	public View getView(int position, View convertView, ViewGroup parent) {
		if (convertView == null) {
			convertView = this.lInflator.inflate(R.layout.main, null);
		}
		TextView thisView = (TextView) convertView.findViewById(R.id.orgItem);
		TextView todoView = (TextView) convertView.findViewById(R.id.todoState);
		TextView priorityView = (TextView) convertView
				.findViewById(R.id.priorityState);
		LinearLayout tagsLayout = (LinearLayout) convertView
				.findViewById(R.id.tagsLayout);
		TextView dateView = (TextView) convertView.findViewById(R.id.dateInfo);
		ArrayList<EditNode> thisEdits = this.findEdits(this.topNode.children
				.get(position).nodeId);
		String todo = this.topNode.children.get(position).todo;
		String priority = this.topNode.children.get(position).priority;
		String dateInfo = "";
		thisView.setText(this.topNode.children.get(position).name);

		for (EditNode e : thisEdits) {
			if (e.editType.equals("todo"))
				todo = e.newVal;
			else if (e.editType.equals("priority"))
				priority = e.newVal;
			else if (e.editType.equals("heading")) {
				thisView.setText(e.newVal);
			}
		}

		SimpleDateFormat formatter = new SimpleDateFormat("<yyyy-MM-dd EEE>");
		if (this.topNode.children.get(position).deadline != null) {
			dateInfo += "DEADLINE: "
					+ formatter
							.format(this.topNode.children.get(position).deadline)
					+ " ";
		}

		if (this.topNode.children.get(position).schedule != null) {
			dateInfo += "SCHEDULED: "
					+ formatter
							.format(this.topNode.children.get(position).schedule)
					+ " ";
		}

		tagsLayout.removeAllViews();
		for (String tag : this.topNode.children.get(position).getTags()) {
			TextView tagView = new TextView(this.context);
			tagView.setText(tag);
			tagView.setTextColor(Color.LTGRAY);
			tagView.setPadding(0, 0, 5, 0);
			tagsLayout.addView(tagView);
		}

		if (TextUtils.isEmpty(todo)) {
			todoView.setVisibility(View.GONE);
		} else {
			todoView.setText(todo);
			Integer todoState = this.findTodoState(todo);
			if (todoState > 0)
				todoView.setBackgroundColor(Color.GREEN);
			else
				todoView.setBackgroundColor(Color.RED);
			todoView.setTextColor(Color.WHITE);
			todoView.setVisibility(View.VISIBLE);
		}

		if (TextUtils.isEmpty(priority)) {
			priorityView.setVisibility(View.GONE);
		} else {
			priorityView.setText(priority);
			priorityView.setVisibility(View.VISIBLE);
		}

		if (TextUtils.isEmpty(dateInfo)) {
			dateView.setVisibility(View.GONE);
		} else {
			dateView.setText(dateInfo);
			dateView.setVisibility(View.VISIBLE);
		}

		convertView.setTag(thisView);
		return convertView;
	}
}