package com.matburt.mobileorg.Gui.Capture;

import java.util.ArrayList;
import java.util.Collections;

import android.content.ContentResolver;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.Spinner;

import com.actionbarsherlock.app.SherlockFragment;
import com.matburt.mobileorg.R;
import com.matburt.mobileorg.OrgData.OrgFile;
import com.matburt.mobileorg.OrgData.OrgNode;
import com.matburt.mobileorg.OrgData.OrgProviderUtil;

public class LocationFragment extends SherlockFragment {
	private final String NODE_ID = "nodeId";
	
	private LinearLayout locationView;
	private ContentResolver resolver;

	private OrgNode node = null;
	private ArrayList<LocationEntry> locations = new ArrayList<LocationEntry>();
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		this.locationView = (LinearLayout) inflater.inflate(
				R.layout.edit_location, container, false);
		return locationView;
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		
		EditActivity activity = (EditActivity) getActivity();
		this.resolver = activity.getContentResolver();

		restoreFromBundle(savedInstanceState);

		if(this.node == null)
			this.node = activity.getOrgNode();

		initLocationView();
	}
	
	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putLong(NODE_ID, getLocationSelection());
	}

	public void restoreFromBundle(Bundle savedInstanceState) {
		if (savedInstanceState != null) {
			locationView.removeAllViews();
			locations.clear();
			long nodeId = savedInstanceState.getLong(NODE_ID, -1);
			if(nodeId > -1)
				this.node = new OrgNode(nodeId, resolver);
		}
	}
	
	private void initLocationView() {
		if(this.node != null && (this.node.id >= 0 || this.node.parentId >= 0))
			setupLocation();
		else {
			LocationEntry topEntry = getTopLevelNode(OrgFile.CAPTURE_FILE);
			locationView.addView(topEntry);
		}
	}
	
	private void setupLocation() {
		OrgNode currentNode = this.node;
		
		while(currentNode != null) {
			OrgNode spinnerNode = currentNode.getParent(resolver);
			String selection = currentNode.name;
			
			if (currentNode.getParent(resolver) != null) {
				ArrayList<String> data = currentNode.getSiblingsStringArray(resolver);
				getLocationEntry(spinnerNode, data, selection);
				currentNode = currentNode.getParent(resolver);
			} else {
				getTopLevelNode(selection);
				currentNode = null;
			}
		}
		
		locationView.removeAllViews();
		Collections.reverse(locations);
		for(Spinner location: locations)
			locationView.addView(location);
	}
	
	private LocationEntry getLocationEntry(OrgNode node, ArrayList<String> data, String selection) {
		LocationEntry location = new LocationEntry(getActivity());
		location.init(node, this, data, selection);
		locations.add(location);
		return location;
	}
	
	private LocationEntry getTopLevelNode(String selection) {
		ArrayList<String> data = OrgProviderUtil.getFilenames(resolver);
		data.remove(OrgFile.AGENDA_FILE);
		LocationEntry entry = getLocationEntry(null, data, selection);
		return entry;
	}
	
	public void addChild(OrgNode spinnerNode, String spinnerSelection) {
		OrgNode childNode;
		if (spinnerNode != null) {
			childNode = spinnerNode.getChild(spinnerSelection, resolver);
			if(childNode == null)
				return;
		} else {
			try {
			OrgFile file = new OrgFile(spinnerSelection, resolver);
			childNode = new OrgNode(file.nodeId, resolver);
			} catch (IllegalArgumentException e) {
				return;
			}
		}

		ArrayList<String> childData = childNode.getChildrenStringArray(resolver);
		
		LocationEntry location = getLocationEntry(childNode, childData, "");
		locationView.addView(location);
	}

	public void removeChildren(LocationEntry location) {
		int index = locations.indexOf(location);
		if(index == -1) // Element not found
			return;
		
		if((index + 1) == locations.size()) // Last spinner in list
			return;
		
		ArrayList<LocationEntry> locationsToDelete = new ArrayList<LocationEntry>(
				locations.subList(index + 1, locations.size()));
		
		for(Spinner spinner: locationsToDelete)
			locationView.removeView(spinner);
		
		locations.removeAll(locationsToDelete);
	}
	
	public long getLocationSelection() {
		if (locations.size() > 1) {
			LocationEntry lastEntry = locations.get(locations.size() - 1);
			OrgNode parent = lastEntry.getOrgNode();
			if(parent != null) {
				OrgNode child = parent.getChild((String) lastEntry.getSelectedItem(), resolver);
				if(child != null)
					return child.id;
				else
					return parent.id;
			}
		}
		
		return -1;
	}
	
	public OrgNode getLocation() {
		if(locations.size() < 2)
			return OrgProviderUtil.getOrCreateCaptureFileOrgNode(resolver);
		else
			return locations.get(locations.size() - 1).getOrgNode();
	}
}
