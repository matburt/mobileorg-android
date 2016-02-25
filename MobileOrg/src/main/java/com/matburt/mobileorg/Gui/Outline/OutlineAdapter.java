package com.matburt.mobileorg.Gui.Outline;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.matburt.mobileorg.Gui.Theme.DefaultTheme;
import com.matburt.mobileorg.OrgData.OrgNode;
import com.matburt.mobileorg.OrgData.OrgProviderUtils;
import com.matburt.mobileorg.OrgNodeDetailActivity;
import com.matburt.mobileorg.OrgNodeDetailFragment;
import com.matburt.mobileorg.R;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class OutlineAdapter extends RecyclerView.Adapter<OutlineItem> {

	private ContentResolver resolver;
	private boolean mTwoPanes = false;
	private List<Boolean> expanded = new ArrayList<Boolean>();
	public List<OrgNode> items = new ArrayList<OrgNode>();

	private DefaultTheme theme;

	private boolean levelIndentation = true;

	public void setHasTwoPanes(boolean _hasTwoPanes){
		mTwoPanes = _hasTwoPanes;
	}

	public OutlineAdapter(Context context) {
		super();
		this.resolver = context.getContentResolver();

		this.theme = DefaultTheme.getTheme(context);
		init();
	}

	public void init() {
		clear();

		for (OrgNode node : OrgProviderUtils.getOrgNodeChildren(-1, resolver))
			add(node);

        notifyDataSetChanged();
	}

	@Override public int getItemCount() {
		return items.size();
	}

    @Override public OutlineItem onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.outline_item, parent, false);
        return new OutlineItem(v);
    }


    @Override public void onBindViewHolder(OutlineItem holder, int position) {
		holder.titleView.setText(items.get(position).name);
//		holder.mContentView.setText(items.get(position).levelView.getText());

		final long itemId = getItemId(position);
		holder.mView.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {

				if (mTwoPanes) {
					Bundle arguments = new Bundle();
					arguments.putLong(OrgNodeDetailFragment.NODE_ID, itemId);
					OrgNodeDetailFragment fragment = new OrgNodeDetailFragment();
					fragment.setArguments(arguments);

					AppCompatActivity activity = (AppCompatActivity)v.getContext();
					activity.getSupportFragmentManager().beginTransaction()
							.replace(R.id.orgnode_detail_container, fragment)
							.commit();
				} else {
					Context context = v.getContext();
					Intent intent = new Intent(context, OrgNodeDetailActivity.class);
					intent.putExtra(OrgNodeDetailFragment.NODE_ID, itemId);
					context.startActivity(intent);
				}
			}
		});
    }


	public void refresh() {
		ArrayList<Long> expandedNodeIds = new ArrayList<Long>();
		int size = this.expanded.size();
		for(int i = 0; i < size; i++) {
			if(this.expanded.get(i))
				expandedNodeIds.add(getItemId(i));
		}
		
		init();

        expandNodes(expandedNodeIds);
	}

	private void expandNodes(ArrayList<Long> nodeIds) {
		while (nodeIds.size() != 0) {
			Long nodeId = nodeIds.get(0);
			for (int nodesPosition = 0; nodesPosition < getItemCount(); nodesPosition++) {
				if (getItemId(nodesPosition) == nodeId) {
					expand(nodesPosition);
					break;
				}
			}
			nodeIds.remove(0);
		}
	}
	
//	@Override
//	public View getView(int position, View convertView, ViewGroup parent) {
//		OutlineItem outlineItem = (OutlineItem) convertView;
//		if (convertView == null)
//			outlineItem = new OutlineItem(getContext());
//
//		outlineItem.setLevelFormating(levelIndentation);
//		outlineItem.setup(getItem(position), this.expanded.get(position), theme, resolver);
//		return outlineItem;
//	}

	public void setLevelIndentation(boolean enabled) {
		this.levelIndentation = enabled;
	}
	
	public void clear() {
		this.items.clear();
		this.expanded.clear();
	}

	public void add(OrgNode node) {
		this.items.add(node);
		this.expanded.add(false);
	}

	public void insert(OrgNode node, int index) {
		this.items.add(index, node);
		this.expanded.add(index, false);
	}
	
	public void insertAll(ArrayList<OrgNode> nodes, int position) {
		Collections.reverse(nodes);
		for(OrgNode node: nodes)
			insert(node, position);
//		notifyDataSetInvalidated();
	}

	public void remove(OrgNode node) {
		int position = items.indexOf(node);
		if(position > -1) {
			this.expanded.remove(position);
			this.items.remove(position);
		}
	}

	public boolean getExpanded(int position) {
		if(position < 0 || position > this.expanded.size())
			return false;
		
		return this.expanded.get(position);
	}
	
	public void collapseExpand(int position) {
		if(position >= getItemCount() || position >= this.expanded.size() || position < 0)
			return;
		
		if(this.expanded.get(position))
			collapse(items.get(position), position);
		else
			expand(position);
	}
	
	public void collapse(OrgNode node, int position) {
		int activePos = position + 1;
		while(activePos < this.expanded.size()) {
			if(items.get(activePos).level <= node.level)
				break;
			collapse(items.get(activePos), activePos);
			remove(items.get(activePos));
		}
		this.expanded.set(position, false);
	}

	public void expand(OrgNode node) {
		int index = items.indexOf(node);
		if(index>-1) expand(index);
	}

	public void expand(int position) {
		OrgNode node = items.get(position);
		insertAll(node.getChildren(resolver), position + 1);
		this.expanded.set(position, true);
	}
	
	@Override
	public long getItemId(int position) {
		OrgNode node = items.get(position);
		return node.id;
	}
	
	public int findParent(int position) {
		if(position >= getItemCount() || position < 0)
			return -1;
		
		long currentLevel = items.get(position).level;
		for(int activePos = position - 1; activePos >= 0; activePos--) {
			if(items.get(activePos).level < currentLevel)
				return activePos;
		}
		
		return -1;
	}
}
