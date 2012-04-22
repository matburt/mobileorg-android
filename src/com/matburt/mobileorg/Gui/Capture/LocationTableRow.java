package com.matburt.mobileorg.Gui.Capture;

import java.util.ArrayList;

import android.content.Context;
import android.content.DialogInterface;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TableRow;

import com.matburt.mobileorg.Parsing.MobileOrgApplication;
import com.matburt.mobileorg.Parsing.NodeWrapper;
import com.matburt.mobileorg.Parsing.OrgDatabase;
import com.matburt.mobileorg.Parsing.OrgFile;

public class LocationTableRow {
	private NodeWrapper node;
	private OrgDatabase db;
	private LinearLayout locationView;
	
	public LocationTableRow(Context context, NodeWrapper node, LinearLayout locationView) {
		this.locationView = locationView;
		
		MobileOrgApplication appInst = (MobileOrgApplication) context.getApplicationContext();
		this.db = appInst.getDB();
		this.node = node;
		
		initLocationView(context);
	}
	
	private void initLocationView(Context context) {
		if(node.getId() < 0) {
			LocationEntry entry = new LocationEntry(context,
					new NodeWrapper(-1, db), null);
			entry.setupSpinner2(OrgFile.CAPTURE_FILE);
			locationView.addView(entry);
			return;
		}
		
		NodeWrapper node = this.node.getParent();
		LocationEntry entry = null;
		while(node != null) {
			entry = new LocationEntry(context, node, entry);
			entry.setupSpinner(node.getName());
			locationView.addView(entry, 0);
			node = node.getParent();
		}	
	}
	
	public long getParentNodeId() {
		return -1;
	}
		
	private class LocationEntry extends Spinner {
		private LocationEntry child = null;
		private NodeWrapper node;

		public LocationEntry(Context context, NodeWrapper node, LocationEntry child) {
			super(context);
			this.child = child;
			this.node = node;
		}
		
		public void setupSpinner(String name) {
			ArrayList<String> children = node.getSiblings();
			EditDetailsFragment.setupSpinner(getContext(), this, children, name);
		}
		
		public void setupSpinner2(String name) {
			if (node != null) {
				Log.d("MobileOrg", "setupSpinner2() :" + node.getName());
				ArrayList<String> children = node.getChildrenStringArray();
				EditDetailsFragment.setupSpinner(getContext(), this, children, name);
			}
		}

		public void removeChild() {
			locationView.removeView(child);
			
			if(child != null) {
				child.removeChild();
				child = null;
			}
		}
		
		@Override
		public void onClick(DialogInterface dialog, int which) {
			super.onClick(dialog, which);
			String selectedItem = (String) this.getItemAtPosition(which);
			node = node.getChild(selectedItem);
			
			if(child != null)
				removeChild();
			
			child = new LocationEntry(getContext(), node, null);
			child.setupSpinner2("");
			locationView.addView(child);
		}
	}
}
