package com.matburt.mobileorg.Gui.Capture;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Spinner;

import com.matburt.mobileorg.OrgData.OrgNode;

public class LocationEntry extends Spinner {
	
	private OrgNode node;
	private LocationFragment locationFragment;
	
	
	public LocationEntry(Context context) {
		super(context);
	}
	
	public LocationEntry(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public void init(OrgNode node, LocationFragment locationFragment) {
		this.node = node;
		this.locationFragment = locationFragment;
		
		this.setOnItemSelectedListener(listener);
	}
	
	private OnItemSelectedListener listener = new OnItemSelectedListener() {

		@Override
		public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position,
				long id) {
			String selection = (String) getSelectedItem();
			updateLocations(selection);
		}

		@Override
		public void onNothingSelected(AdapterView<?> parentView) {			
		}
	};
	
	
	private void updateLocations(String selection) {
		this.locationFragment.removeChildren(this);
		this.locationFragment.addChild(node, selection);
	}
		
	public OrgNode getOrgNode() {
		return this.node;
	}
}
