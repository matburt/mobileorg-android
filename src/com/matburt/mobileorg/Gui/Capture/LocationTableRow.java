package com.matburt.mobileorg.Gui.Capture;

import java.util.ArrayList;

import android.content.Context;
import android.content.DialogInterface;
import android.text.TextUtils;
import android.util.Log;
import android.widget.LinearLayout;
import android.widget.Spinner;

import com.matburt.mobileorg.Parsing.MobileOrgApplication;
import com.matburt.mobileorg.Parsing.NodeWrapper;
import com.matburt.mobileorg.Parsing.OrgDatabaseOld;
import com.matburt.mobileorg.util.FileUtils;

public class LocationTableRow {
	private NodeWrapper node;
	private OrgDatabaseOld db;
	private LinearLayout locationView;
	private LocationEntry lastEntry;
	
	public LocationTableRow(Context context, NodeWrapper node, LinearLayout locationView) {
		this.locationView = locationView;
		
		MobileOrgApplication appInst = (MobileOrgApplication) context.getApplicationContext();
		this.db = appInst.getDB();
		this.node = node;
		
		initLocationView(context);
	}
	
	private void initLocationView(Context context) {
		if(node == null) {
			LocationEntry entry = new LocationEntry(context, null, null);
			entry.setupSpinner();
			locationView.addView(entry);
			this.lastEntry = entry;
			return;
		}
		
		NodeWrapper currentNode = this.node;
		LocationEntry entry = null;
		while(currentNode != null) {
			entry = new LocationEntry(context, currentNode.getParent(), entry);
			entry.setupSpinner(currentNode.getName());
			locationView.addView(entry, 0);
			
			if(this.lastEntry == null)
				this.lastEntry = entry;
			
			currentNode = currentNode.getParent();
		}	
		
		getParentNodeId();
	}
	
	public NodeWrapper getParentNodeId() {
		NodeWrapper node = this.lastEntry.getNode();
		if(node != null)
			Log.d("MobileOrg", "getParentNodeId() : " + node.getName());
		else
			Log.d("MobileOrg", "getParentNodeId() : Top level");

		return this.lastEntry.getNode();
	}
		
	private class LocationEntry extends Spinner {
		private LocationEntry child = null;
		private NodeWrapper node;
		
		public LocationEntry(Context context, NodeWrapper node, LocationEntry child) {
			super(context);
			this.child = child;
			this.node = node;
		}
		
		public void setupSpinner() {
			if(this.node == null && this.child == null)
				setupSpinner(FileUtils.CAPTURE_FILE);
			else
				setupSpinner(this.node.getName());
		}
		
		private void setupSpinner(String name) {
			ArrayList<String> children = new ArrayList<String>();
			if (this.node == null) {// Toplevel node
				children = db.cursorToArrayList(db.getFileCursor());
				Log.d("MobileOrg", "setupSpinner(): this.node = null!");
			}
			else {
				children = node.getChildrenStringArray();
				Log.d("MobileOrg", "setupSpinner(): this.node = " + this.node.getName());
			}
			
			EditDetailsFragment
					.setupSpinner(getContext(), this, children, name);
		}
		
		@Override
		public void onClick(DialogInterface dialog, int which) {
			super.onClick(dialog, which);
			String selectedItem = (String) this.getItemAtPosition(which);

			removeChildren();
			
			if(TextUtils.isEmpty(selectedItem) == false)
				addChild(selectedItem);
			
			getParentNodeId();
		}
		
		private void addChild(String selectedItem) {
			NodeWrapper childNode;
			if(this.node == null) {
				Log.d("MobileOrg", "addChild(): this.node = null!");
				childNode = new NodeWrapper(db.getFileNodeId(selectedItem), db);
			}
			else {
				Log.d("MobileOrg", "addChild(): this.node = " + this.node.getName());
				childNode = this.node.getChild(selectedItem);
				
				if(childNode == null)
					Log.d("MobileOrg", "addChild(): generated null node!");
				
				if(childNode.getChildren().size() == 0)
					return;
			}
			
			child = new LocationEntry(getContext(), childNode, null);
			child.setupChildSpinner();
			locationView.addView(child);
			lastEntry = child;
		}
		
		private void setupChildSpinner() {
			EditDetailsFragment
			.setupSpinner(getContext(), this, node.getChildrenStringArray(), "");
		}

		private void removeChildren() {	
			if(child != null) {
				locationView.removeView(child);
				child.removeChildren();
				child = null;
			}
			lastEntry = this;
		}
		
		public NodeWrapper getNode() {
			String selection = (String) this.getSelectedItem();
			
			if(TextUtils.isEmpty(selection))
				return this.node;
			
			if(this.node == null)
				return new NodeWrapper(db.getFileNodeId(selection), db);
			else
				return this.node.getChild(selection);
		}
	}
}
