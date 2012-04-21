package com.matburt.mobileorg.Gui.Capture;

import java.util.ArrayList;

import com.matburt.mobileorg.Parsing.MobileOrgApplication;
import com.matburt.mobileorg.Parsing.NodeWrapper;
import com.matburt.mobileorg.Parsing.OrgDatabase;

import android.content.Context;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TableRow;

public class LocationTableRow extends TableRow {

	private OrgDatabase orgDB;

	public LocationTableRow(Context context, long parentId) {
		super(context);
		
		this.setOrientation(HORIZONTAL);
		
		MobileOrgApplication appInst = (MobileOrgApplication) context.getApplicationContext();
		this.orgDB = appInst.getDB();
				
		long id = parentId;
		NodeWrapper node = new NodeWrapper(id, orgDB);
		
		while(id > 0) {
			Log.d("MobileOrg", "It: " + Long.toString(id));
			id = node.getParentId();
			node.close();
			if(id > 0) {
				node = new NodeWrapper(id, orgDB);
				this.addView(new LocationEntry(this.getContext(), node), 0);
			}
		}
	}

	public long getParentNodeId() {
		return -1;
	}
	
	private class LocationEntry extends Spinner {		
		private NodeWrapper node;

		public LocationEntry(Context context, NodeWrapper node) {
			super(context);
			//this.node = new NodeWrapper(node.getParentId(), orgDB);
			ArrayList<String> children = wrapperToStringArray(node.getChildren(orgDB));
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
