package com.matburt.mobileorg.Gui.Outline;

import java.util.ArrayList;
import java.util.Collections;

import android.content.ContentResolver;
import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;

import com.matburt.mobileorg.R;
import com.matburt.mobileorg.Gui.Theme.DefaultTheme;
import com.matburt.mobileorg.OrgData.OrgNode;
import com.matburt.mobileorg.OrgData.OrgProviderUtils;
import com.matburt.mobileorg.util.OrgNodeNotFoundException;

public class OutlineAdapter extends ArrayAdapter<OrgNode> {

	private ContentResolver resolver;
	
	private ArrayList<Boolean> expanded = new ArrayList<Boolean>();

	private DefaultTheme theme;
	
	private boolean levelIndentation = true;
	
	public OutlineAdapter(Context context) {
		super(context, R.layout.outline_item);
		this.resolver = context.getContentResolver();

		this.theme = DefaultTheme.getTheme(context);
		init();
	}

	public void init() {
		clear();
		
		for (OrgNode node : OrgProviderUtils.getOrgNodeChildren(-1, resolver))
			add(node);
		
		notifyDataSetInvalidated();
	}
	
	
	public long[] getState() {
		int count = getCount();
		long[] state = new long[count];
		
		for(int i = 0; i < count; i++)
			state[i] = getItem(i).id;
		
		return state;
	}
	
	public void setState(long[] state) {
		clear();
		
		for(int i = 0; i < state.length; i++) {
			try {
				OrgNode node = new OrgNode(state[i], resolver);
				add(node);
			} catch (OrgNodeNotFoundException e) {}
		}
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
			for (int nodesPosition = 0; nodesPosition < getCount(); nodesPosition++) {
				if (getItemId(nodesPosition) == nodeId) {
					expand(nodesPosition);
					break;
				}
			}
			nodeIds.remove(0);
		}
	}
	
	@Override
	public View getView(int position, View convertView, ViewGroup parent) {				
		OutlineItem outlineItem = (OutlineItem) convertView;
		if (convertView == null)
			outlineItem = new OutlineItem(getContext());

		outlineItem.setLevelFormating(levelIndentation);
		outlineItem.setup(getItem(position), this.expanded.get(position), theme, resolver);
		return outlineItem;
	}

	public void setLevelIndentation(boolean enabled) {
		this.levelIndentation = enabled;
	}
	
	@Override
	public void clear() {
		super.clear();
		this.expanded.clear();
	}

	@Override
	public void add(OrgNode node) {
		super.add(node);
		this.expanded.add(false);
	}

	@Override
	public void insert(OrgNode node, int index) {
		super.insert(node, index);
		this.expanded.add(index, false);
	}
	
	public void insertAll(ArrayList<OrgNode> nodes, int position) {
		Collections.reverse(nodes);
		for(OrgNode node: nodes)
			insert(node, position);
		notifyDataSetInvalidated();
	}

	@Override
	public void remove(OrgNode node) {
		int position = getPosition(node);
		this.expanded.remove(position);
		super.remove(node);
	}

	public boolean getExpanded(int position) {
		if(position < 0 || position > this.expanded.size())
			return false;
		
		return this.expanded.get(position);
	}
	
	public void collapseExpand(int position) {
		if(position >= getCount() || position >= this.expanded.size() || position < 0)
			return;
		
		if(this.expanded.get(position))
			collapse(getItem(position), position);
		else
			expand(position);
	}
	
	public void collapse(OrgNode node, int position) {
		int activePos = position + 1;
		while(activePos < this.expanded.size()) {
			if(getItem(activePos).level <= node.level)
				break;
			collapse(getItem(activePos), activePos);
			remove(getItem(activePos));
		}
		this.expanded.set(position, false);
	}
	
	public void expand(int position) {
		OrgNode node = getItem(position);
		insertAll(node.getChildren(resolver), position + 1);
		this.expanded.set(position, true);
	}
	
	@Override
	public long getItemId(int position) {
		OrgNode node = getItem(position);
		return node.id;
	}
	
	public int findParent(int position) {
		if(position >= getCount() || position < 0)
			return -1;
		
		long currentLevel = getItem(position).level;
		for(int activePos = position - 1; activePos >= 0; activePos--) {
			if(getItem(activePos).level < currentLevel)
				return activePos;
		}
		
		return -1;
	}
}
