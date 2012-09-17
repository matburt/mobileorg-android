package com.matburt.mobileorg.Gui.Capture;

import java.util.ArrayList;
import java.util.Collections;

import android.content.ContentResolver;
import android.os.Bundle;
import android.text.TextUtils;
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
		super.onCreateView(inflater, container, savedInstanceState);
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
			this.node = activity.getParentOrgNode();

		initLocationView();
		
		if(activity.isNodeModifiable() == false)
			setUnmodifiable();
	}
	
	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putLong(NODE_ID, getLocationSelection().id);
	}

	public void restoreFromBundle(Bundle savedInstanceState) {
		if (savedInstanceState != null) {
			locationView.removeAllViews();
			locations.clear();
			long nodeId = savedInstanceState.getLong(NODE_ID, -1);
			if(nodeId >= 0)
				this.node = new OrgNode(nodeId, resolver);
		}
	}
	
	private void initLocationView() {		
		if(this.node != null) {
			if(this.node.parentId == -2) // Editing top node; can't be refiled
				return;

			setupLocation();
		}
		else {
			LocationEntry topEntry = getTopLevelNode(OrgFile.CAPTURE_FILE_ALIAS);
			locationView.addView(topEntry);
		}
	}
	
	private void setupLocation() {
		if(this.node != null)
			getLocationEntry(this.node,
					this.node.getChildrenStringArray(resolver), "");
		
		OrgNode currentNode = this.node;
		while(currentNode != null) {
			OrgNode spinnerNode = currentNode.getParent(resolver);
			String selection = currentNode.name;
			
			if (spinnerNode != null) {
				ArrayList<String> data = currentNode.getSiblingsStringArray(resolver);
				getLocationEntry(spinnerNode, data, selection);
				currentNode = spinnerNode;
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
	
	public void setUnmodifiable() {
		for(LocationEntry entry: locations)
			entry.setEnabled(false);
	}
	
	private LocationEntry getLocationEntry(OrgNode node, ArrayList<String> data, String selection) {
		if(this.node != null && node != null && this.node.id == node.id) {
			String editNodeName = ((EditActivity) getActivity()).getOrgNode().name;
			if(TextUtils.isEmpty(editNodeName) == false)
				data.remove(editNodeName); // Prevents refiling node "under itself"
		}
		
		LocationEntry location = new LocationEntry(getActivity());
		location.init(node, this, data, selection);
		locations.add(location);
		return location;
	}
	
	private LocationEntry getTopLevelNode(String selection) {
		ArrayList<String> data = OrgProviderUtil.getFileAliases(resolver);
		data.remove(OrgFile.AGENDA_FILE);
		data.remove(OrgFile.AGENDA_FILE_ALIAS);
		LocationEntry entry = getLocationEntry(null, data, selection);
		return entry;
	}
	
	public void addChild(OrgNode spinnerNode, String spinnerSelection) {
		OrgNode childNode;
		if (spinnerNode != null) {
			childNode = spinnerNode.getChild(spinnerSelection, resolver);
			
			if(childNode == null || childNode.getChildren(resolver).size() == 0)
				return;
		} else {
			try {
				childNode = OrgProviderUtil.getOrgNodeFromFileAlias(
						spinnerSelection, resolver);
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
	
	
	public OrgNode getLocationSelection() {
		OrgNode result = null;
		
		switch (locations.size()) {
		case 0:
			result = null;
			break;
		case 1:
			result = getSelectedTopNodeId();
			break;
		case 2:
			result = getSelectedNodeId(locations.size() - 1);
			break;
		default:
			result = getSelectedNodeId(locations.size() - 1);
			break;
		}
		
		if(result == null)
			result = new OrgNode();
		return result;
	}
	
	private OrgNode getSelectedTopNodeId() {
		if(locations.size() < 1)
			return null;
				
		String selection = (String) locations.get(0)
				.getSelectedItem();
		
		if (TextUtils.isEmpty(selection) == false) {
			return OrgProviderUtil.getOrCreateFileFromAlias(selection, resolver).getOrgNode(resolver);
		} else
			throw new IllegalStateException("Can't determine location");
	}
	
	private OrgNode getSelectedNodeId(int index) {
		if(index < 1)
			return getSelectedTopNodeId();
		
		if(locations.size() < 2)
			return null;
				
		String selection = (String) locations.get(index)
				.getSelectedItem();
		if (TextUtils.isEmpty(selection) == false) {
			OrgNode parent = locations.get(index).getOrgNode();
			try {
				OrgNode child = parent.getChild(selection, resolver);
				return child;
			} catch (IllegalArgumentException e) {
				throw new IllegalStateException("Can't determine location");
			}
		} else {
			return getSelectedNodeId(--index);
		}
	}
}
