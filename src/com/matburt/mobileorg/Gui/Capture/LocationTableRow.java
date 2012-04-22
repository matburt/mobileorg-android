package com.matburt.mobileorg.Gui.Capture;

import java.util.ArrayList;

import android.content.Context;
import android.util.Log;
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
		if(node.getId() < 0)
			this.addView(new LocationEntry(getContext(), db
					.cursorToArrayList(db.getFileCursor()), OrgFile.CAPTURE_FILE));
		
		NodeWrapper node = this.node.getParent();
		
		while(node != null) {
			Log.d("MobileOrg", "init running with " + node.getName());
			this.addView(new LocationEntry(getContext(), node), 0);
			node = node.getParent();
		}	
	}
	
	public long getParentNodeId() {
		return -1;
	}
	
	private class LocationEntry extends Spinner {

		public LocationEntry(Context context, NodeWrapper node) {
			super(context);
			ArrayList<String> children = node.getSiblings();
			EditDetailsFragment.setupSpinner(getContext(), this, children, node.getName());
		}
		
		public LocationEntry(Context context, ArrayList<String> children, String selection) {
			super(context);
			EditDetailsFragment.setupSpinner(getContext(), this, children, selection);
		}
		
	}
}
