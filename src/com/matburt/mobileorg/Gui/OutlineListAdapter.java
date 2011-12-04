package com.matburt.mobileorg.Gui;

import java.util.ArrayList;
import java.util.HashMap;

import android.content.Context;
import android.graphics.Color;
import android.text.TextUtils;
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

class OutlineListAdapter extends BaseAdapter 
{
	private Node node;
	private  ArrayList<EditNode> edits;
	private ArrayList<HashMap<String, Integer>> allTodos;
	private Context context;
	private LayoutInflater lInflator;

	public OutlineListAdapter(Context context, Node node) {
		this.context = context;

		MobileOrgApplication appInst = (MobileOrgApplication) context.getApplicationContext();
		this.edits = appInst.edits;
		
		this.node = node;
		this.lInflator = LayoutInflater.from(context);
		this.allTodos = appInst.getGroupedTodods();
	}

	@Override
	public int getCount() {
		if (this.node == null || this.node.hasChildren() == false)
			return 0;
		return this.node.getChildren().size();
	}

	@Override
	public Object getItem(int position) {
		return this.node.getChildren().get(position);
	}

	@Override
	public long getItemId(int position) {
		return position;
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
		
		Node node = this.node.getChildren().get(position);
		node.applyEdits(this.edits);
		
		String name = node.name;
		String todo = node.todo;
		String priority = node.priority;
		String dateInfo = node.payload.datesToString();

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