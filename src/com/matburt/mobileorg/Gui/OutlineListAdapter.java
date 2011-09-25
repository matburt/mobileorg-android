package com.matburt.mobileorg.Gui;

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

import com.matburt.mobileorg.MobileOrgApplication;
import com.matburt.mobileorg.R;
import com.matburt.mobileorg.Parsing.EditNode;
import com.matburt.mobileorg.Parsing.Node;

class OutlineListAdapter extends BaseAdapter {

	public Node topNode;
	public ArrayList<EditNode> edits = new ArrayList<EditNode>();
	public ArrayList<HashMap<String, Integer>> allTodos = new ArrayList<HashMap<String, Integer>>();
	private Context context;
	private LayoutInflater lInflator;

	public OutlineListAdapter(Context context, Node node,
			ArrayList<Integer> selection, ArrayList<EditNode> edits,
			ArrayList<HashMap<String, Integer>> allTodos) {

		this.topNode = node;
		this.lInflator = LayoutInflater.from(context);
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

	@Override
	public int getCount() {
		if (this.topNode == null || this.topNode.children == null)
			return 0;
		return this.topNode.children.size();
	}

	@Override
	public Object getItem(int position) {
		return this.topNode.children.get(position);
	}

	@Override
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

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		ViewHolder holder;
		
		if (convertView == null) {
			convertView = this.lInflator.inflate(R.layout.outline_new, null);
			
			holder = new ViewHolder();
			holder.orgItem = (TextView) convertView.findViewById(R.id.orgItem);
			holder.todoState = (TextView) convertView.findViewById(R.id.todoState);
			holder.priorityState = (TextView) convertView
					.findViewById(R.id.priorityState);
			holder.tagsLayout = (LinearLayout) convertView
					.findViewById(R.id.tagsLayout);
			holder.dateInfo = (TextView) convertView.findViewById(R.id.dateInfo);
			
			convertView.setTag(holder);
		} else {
			holder = (ViewHolder) convertView.getTag();
		}
		
		Node node = this.topNode.children.get(position);

		String name = node.name;
		String todo = node.todo;
		String priority = node.priority;
		String dateInfo = node.formatDate();

		// Apply all EditNodes
		ArrayList<EditNode> thisEdits = this.findEdits(node.nodeId);
		for (EditNode e : thisEdits) {
			if (e.editType.equals("todo"))
				todo = e.newVal;
			else if (e.editType.equals("priority"))
				priority = e.newVal;
			else if (e.editType.equals("heading")) {
				name = e.newVal;
			}
		}

		setupHolder(holder, node, name, todo, priority, dateInfo);

		return convertView;
	}
	

	
	private void setupHolder(ViewHolder holder, Node node, String name, String todo,
			String priority, String dateInfo) {
		holder.orgItem.setText(node.name);
		
		// Setup todo state view
		if (TextUtils.isEmpty(todo)) {
			holder.todoState.setVisibility(View.GONE);
		} else {
			holder.todoState.setText(todo);
			Integer todoState = this.findTodoState(todo);
			if (todoState > 0)
				holder.todoState.setBackgroundColor(Color.GREEN);
			else
				holder.todoState.setBackgroundColor(Color.RED);
			holder.todoState.setTextColor(Color.WHITE);
			holder.todoState.setVisibility(View.VISIBLE);
		}

		// Setup priority view
		if (TextUtils.isEmpty(priority)) {
			holder.priorityState.setVisibility(View.GONE);
		} else {
			holder.priorityState.setText(priority);
			holder.priorityState.setVisibility(View.VISIBLE);
		}

		// Setup date view
		//if (TextUtils.isEmpty(dateInfo)) {
			holder.dateInfo.setVisibility(View.GONE);
//		} else {
//			holder.dateInfo.setText(dateInfo);
//			holder.dateInfo.setVisibility(View.VISIBLE);
//		}
		
		// Add tag view(s)
		holder.tagsLayout.removeAllViews();
		for (String tag : node.getTags()) {
			TextView tagView = new TextView(this.context);
			tagView.setText(tag);
			tagView.setTextColor(Color.LTGRAY);
			tagView.setPadding(0, 0, 5, 0);
			holder.tagsLayout.addView(tagView);
		}
	}
	
	/**
	 * Used as part of the holding pattern.
	 * 
	 * The idea is to save the findViewById()'s into this container object to
	 * speed up the list adapter. setTag() and getTag() are used to bind and
	 * retrieve the container.
	 * 
	 */
	static class ViewHolder {
		TextView orgItem;
		TextView todoState;
		TextView priorityState;
		LinearLayout tagsLayout;
		TextView dateInfo;
	}
}