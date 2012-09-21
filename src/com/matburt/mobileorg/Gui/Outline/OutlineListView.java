package com.matburt.mobileorg.Gui.Outline;

import android.content.ContentResolver;
import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;

import com.actionbarsherlock.app.SherlockActivity;
import com.actionbarsherlock.view.ActionMode;
import com.matburt.mobileorg.OrgData.OrgNode;

public class OutlineListView extends ListView {

	private Context context;
	private SherlockActivity activity;
	private ContentResolver resolver;

	private OutlineAdapter adapter;
	private OutlineActionMode actionMode;
	private ActionMode activeActionMode = null;
	
	public OutlineListView(Context context, AttributeSet atts) {
		super(context, atts);
		this.context = activity;
		this.resolver = context.getContentResolver();
		setChoiceMode(ListView.CHOICE_MODE_SINGLE);
		setOnItemClickListener(outlineClickListener);
		setOnItemLongClickListener(outlineLongClickListener);
		this.actionMode = new OutlineActionMode(context);
		this.adapter = new OutlineAdapter(context);
		setAdapter(adapter);
	}
	
	public void setActivity(SherlockActivity activity) {
		this.activity = activity;
		this.context = activity;
	}

	public void refresh() {
		this.adapter.init();
	}
	
	public long getCheckedNodeId() {
		if(getCheckedItemPosition() == ListView.INVALID_POSITION)
			return -1;
		else {
			int position = getCheckedItemPosition();
			return adapter.getNodeId(position);
		}
	}
	
	private OnItemClickListener outlineClickListener = new OnItemClickListener() {
		@Override
		public void onItemClick(AdapterView<?> parent, View v, int position,
				long id) {
			if(activeActionMode != null)
				activeActionMode.finish();
			setItemChecked(position, true);
			
			OrgNode node = adapter.getItem(position);
			if(node.hasChildren(resolver)) {
				adapter.collapseExpand(position);
			}
			else
				OutlineActionMode.runEditNodeActivity(context, node.id);
		}
	};
	
	private OnItemLongClickListener outlineLongClickListener = new OnItemLongClickListener() {
		@Override
		public boolean onItemLongClick(AdapterView<?> parent, View v, int position,
				long id) {
			if(activity != null) {
				actionMode.initActionMode(OutlineListView.this, position);
				activeActionMode = activity.startActionMode(actionMode);
			}
			return true;
		}
	};
}
