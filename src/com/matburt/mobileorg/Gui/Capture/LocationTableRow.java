package com.matburt.mobileorg.Gui.Capture;

import java.util.ArrayList;

import android.content.Context;
import android.util.Log;
import android.widget.Spinner;
import android.widget.TableRow;

import com.matburt.mobileorg.Parsing.MobileOrgApplication;
import com.matburt.mobileorg.Parsing.NodeWrapper;
import com.matburt.mobileorg.Parsing.OrgDatabase;

public class LocationTableRow extends TableRow {

	private OrgDatabase orgDB;

	public LocationTableRow(Context context, NodeWrapper node) {
		super(context);
		
		this.setOrientation(HORIZONTAL);
		
		MobileOrgApplication appInst = (MobileOrgApplication) context.getApplicationContext();
		this.orgDB = appInst.getDB();

		node.getParent().getParent();
		long id = 0;
		
		while(id > 0) {
			this.addView(new LocationEntry(this.getContext(), node), 0);
			id = node.getParentId();
			node.close();
			node = new NodeWrapper(id, orgDB);
		}
	}

	
	
	public long getParentNodeId() {
		return -1;
	}
	
	private class LocationEntry extends Spinner {		
		private NodeWrapper node;

		public LocationEntry(Context context, NodeWrapper node) {
			super(context);
			ArrayList<String> children = wrapperToStringArray(node.getChildren());
			EditDetailsFragment.setupSpinner(getContext(), this, children, node.getName());
		}
	}
	
	private ArrayList<String> wrapperToStringArray(ArrayList<NodeWrapper> wrappers) {
		ArrayList<String> result = new ArrayList<String>();
		for(NodeWrapper wrapper: wrappers)
			result.add(wrapper.getName());
			
		return result;
	}
}
