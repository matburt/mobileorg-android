package com.matburt.mobileorg.Gui.Outline;

import java.util.ArrayList;
import java.util.Collections;

import android.content.ContentResolver;
import android.content.Context;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;

import com.matburt.mobileorg.R;
import com.matburt.mobileorg.Gui.Outline.Theme.DefaultTheme;
import com.matburt.mobileorg.OrgData.OrgNode;
import com.matburt.mobileorg.OrgData.OrgProviderUtils;

public class OutlineAdapter extends ArrayAdapter<OrgNode> {

	private ContentResolver resolver;
	
	private ArrayList<Boolean> expanded = new ArrayList<Boolean>();

	private DefaultTheme theme;
	
	public OutlineAdapter(Context context) {
		super(context, R.layout.outline_item);
		this.resolver = context.getContentResolver();

		this.theme = new DefaultTheme();
		init();
	}
	
	public void init() {
		clear();
		this.expanded.clear();
		
		for (OrgNode node : OrgProviderUtils.getOrgNodeChildren(-1, resolver))
			add(node);
		
		notifyDataSetInvalidated();
	}
	
	@Override
	public View getView(int position, View convertView, ViewGroup parent) {				
		OutlineItem outlineItem = (OutlineItem) convertView;
		if (convertView == null)
			outlineItem = new OutlineItem(getContext());

		outlineItem.setup(getItem(position), theme, resolver);
		return outlineItem;
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

	
	public void collapseExpand(int position) {
		if((position + 1) > getCount() || (position + 1) > this.expanded.size())
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
		Log.d("MobileOrg", "Adapter tried to expand with : " + node.getChildren(resolver).size());
		this.expanded.set(position, true);
	}
	
	public long getNodeId(int position) {
		OrgNode node = getItem(position);
		return node.id;
	}
}
