package com.matburt.mobileorg.Gui.Outline;

import android.content.ContentResolver;
import android.content.Context;
import android.preference.PreferenceManager;
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
		setAdapter(new OutlineAdapter(context));
	}
	
	public void setAdapter(OutlineAdapter adapter) {
		this.adapter = adapter;
		super.setAdapter(adapter);
	}


	public void setActivity(SherlockActivity activity) {
		this.activity = activity;
		this.context = activity;
	}

	public long[] getState() {
		return this.adapter.getState();
	}
	
	public void setState(long[] state) {
		this.adapter.setState(state);
	}
	
	public void refresh() {
		int position = getFirstVisiblePosition();
		this.adapter.refresh();
		setSelection(position);
	}
	
	public long getCheckedNodeId() {
		if(getCheckedItemPosition() == ListView.INVALID_POSITION)
			return -1;
		else {
			int position = getCheckedItemPosition();
			return adapter.getItemId(position);
		}
	}
	
	private OnItemClickListener outlineClickListener = new OnItemClickListener() {
		@Override
		public void onItemClick(AdapterView<?> parent, View v, int position,
				long id) {
			if(activeActionMode != null)
				activeActionMode.finish();
			
			OrgNode node = adapter.getItem(position);
			if(node.hasChildren(resolver)) {
				adapter.collapseExpand(position);
			}
			else {
				boolean viewOnClick = PreferenceManager
						.getDefaultSharedPreferences(context).getBoolean(
								"viewOnClick", false);
				
				if (viewOnClick)
					OutlineActionMode.runViewNodeActivity(node.id, context);
				else
					OutlineActionMode.runEditNodeActivity(node.id, context);
				//setParentChecked(position);
			}
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
	
	@SuppressWarnings("unused")
	private void setParentChecked(int position) {
		int parentPos = adapter.findParent(position);
		if(parentPos >= 0)
			setItemChecked(parentPos, true);
	}

	public void collapseCurrent() {
		int position = getCheckedItemPosition();

		if (position == ListView.INVALID_POSITION)
			return;

		if (adapter.getExpanded(position)) // Item is expanded, collapse it
			adapter.collapseExpand(position);
		else {
			if(adapter.getItem(position).level == 0) { // Top level, collapse all entries
				adapter.init();
				setItemChecked(position, false);
			} else {									// Collapse parent
				int parent = adapter.findParent(position);

				if (parent >= 0) {
					adapter.collapseExpand(parent);
					setItemChecked(parent, true);
				}
			}
		}

		ensureCheckedItemVisible();
	}
	
	public void ensureCheckedItemVisible() {
		int position = getCheckedItemPosition();
		if(position == ListView.INVALID_POSITION)
			return;
		
		if(!(getLastVisiblePosition() >= position && getFirstVisiblePosition() <= position))
			setSelection(position - 2);
	}
}
