package com.matburt.mobileorg.Gui.Capture;

import java.util.ArrayList;

import android.content.Context;
import android.content.DialogInterface;
import android.util.Log;
import android.view.View;
import android.widget.Spinner;
import android.widget.TableRow;

import com.matburt.mobileorg.Parsing.MobileOrgApplication;
import com.matburt.mobileorg.Parsing.NodeWrapper;
import com.matburt.mobileorg.Parsing.OrgDatabase;
import com.matburt.mobileorg.Parsing.OrgFile;

public class LocationTableRow extends TableRow {
	private NodeWrapper node;
	private OrgDatabase db;
	
	public LocationTableRow(Context context, NodeWrapper node) {
		super(context);
		this.setOrientation(HORIZONTAL);
		
		MobileOrgApplication appInst = (MobileOrgApplication) context.getApplicationContext();
		this.db = appInst.getDB();
		this.node = node;
		
		initLocationView();
	}
	
	private void initLocationView() {
		if(node.getId() < 0) {
			LocationEntry entry = new LocationEntry(getContext(),
					new NodeWrapper(db.getFileCursor(), db), null);
			entry.setupSpinner2(OrgFile.CAPTURE_FILE);
			addView(entry);
			return;
		}
		
		NodeWrapper node = this.node.getParent();
		LocationEntry entry = null;
		while(node != null) {
			entry = new LocationEntry(getContext(), node, entry);
			entry.setupSpinner(node.getName());
			this.addView(entry, 0);
			node = node.getParent();
		}	
	}
	
	public long getParentNodeId() {
		return -1;
	}
	
	public void addEntry(View v) {
		addView(v);
	}
	
	public void removeEntry(View v) {
		removeView(v);
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
			ArrayList<String> children = node.getChildrenStringArray();
			EditDetailsFragment.setupSpinner(getContext(), this, children, name);
		}


		public void removeChild() {
			removeEntry(child);
			
			if(child != null) {
				child.removeChild();
				child = null;
			}
		}
		
		@Override
		public void onClick(DialogInterface dialog, int which) {
			super.onClick(dialog, which);
			String selectedItem = (String) this.getItemAtPosition(which);
			node = node.getSilbing(selectedItem);
			
			if(child != null)
				removeChild();
			
			child = new LocationEntry(getContext(), node, null);
			child.setupSpinner2("");
			addEntry(child);
		}
	}
}
