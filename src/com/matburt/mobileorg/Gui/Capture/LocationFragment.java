package com.matburt.mobileorg.Gui.Capture;

import android.content.ContentResolver;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.Spinner;

import com.actionbarsherlock.app.SherlockFragment;
import com.matburt.mobileorg.R;
import com.matburt.mobileorg.OrgData.OrgNode;

public class LocationFragment extends SherlockFragment {
	
	private LocationEntry locationTableRow;
	private OrgNode node = null;
	private LinearLayout locationView;
	private LocationEntry lastEntry;
	private ContentResolver resolver;
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.edit_location, container,
				false);
		return view;
	}

	@Override
	public void onStart() {
		super.onStart();
		
		EditActivity activity = (EditActivity) getActivity();
		ContentResolver resolver = activity.getContentResolver();
		OrgNode node = activity.getOrgNode();
		if (node.getParent(resolver) != null)
			;
//			this.locationTableRow = new LocationEntry(
//					node.getParent(resolver), getActivity(), locationView,
//					resolver);
	}

	private void initLocationView(Context context) {		
		OrgNode currentNode = this.node;
		LocationEntry entry = null;
		while(currentNode != null) {
			entry = new LocationEntry(context, currentNode.getParent(resolver), entry);
			//entry.setupSpinner(currentNode.name);
			
			if(locationView != null)
				locationView.addView(entry, 0);
			
			if(this.lastEntry == null)
				this.lastEntry = entry;
			
			currentNode = currentNode.getParent(resolver);
		}	
		
		getParentNodeId();
	}
	
	public OrgNode getParentNodeId() {
		OrgNode node = this.lastEntry.getNode();
		if(node != null)
			Log.d("MobileOrg", "getParentNodeId() : " + node.name);
		else
			Log.d("MobileOrg", "getParentNodeId() : Top level");

		return this.lastEntry.getNode();
	}
	
	public OrgNode getLocation() {
		return null;
	}
}
