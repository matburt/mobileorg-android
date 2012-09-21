package com.matburt.mobileorg.Gui.Capture;

import java.util.ArrayList;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.util.AttributeSet;
import android.view.View;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.Spinner;

import com.matburt.mobileorg.R;
import com.matburt.mobileorg.OrgData.OrgNode;
import com.matburt.mobileorg.util.OrgUtils;

public class LocationEntry extends Spinner {
	
	public final String createHeading = "Create new";
	
	private OrgNode node;
	private LocationFragment locationFragment;

	private ArrayList<String> data;
	private boolean disableSpinnerListener = false;
	
	public LocationEntry(Context context) {
		super(context);
	}
	
	public LocationEntry(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public void init(OrgNode node, LocationFragment locationFragment,
			ArrayList<String> data, String selection) {
		this.node = node;
		this.locationFragment = locationFragment;
		this.data = data;
		
		// TODO Renable to allow adding of new files
		if(node == null) // This is a top level node
			;//this.data.add(createHeading);
		else
			this.data.add("");
		
		setupSpinner(selection);
		setOnItemSelectedListener(listener);
	}
	
	private OnItemSelectedListener listener = new OnItemSelectedListener() {

		@Override
		public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position,
				long id) {
			if(disableSpinnerListener)
				return;
			
			String selection = (String) getSelectedItem();
			
			if(selection.equals(createHeading))
				promptForNewFile();
			else
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
	
	private void promptForNewFile() {
		AlertDialog.Builder alert = new AlertDialog.Builder(getContext());

		alert.setTitle(R.string.edit_create_new_entry).setMessage(
				R.string.edit_create_new_entry_body);

		final EditText input = new EditText(getContext());
		alert.setView(input);

		alert.setPositiveButton(R.string.ok,
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int whichButton) {
						String value = input.getText().toString();
						data.add(value);
						setupSpinner(value);
					}
				});

		alert.setNegativeButton(R.string.cancel,
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int whichButton) {
					}
				});

		alert.show();
	}
		
	private void setupSpinner(String selection) {
		this.disableSpinnerListener = true;
		OrgUtils.setupSpinner(this, data, selection);
		this.disableSpinnerListener = false;
	}
	
	public OrgNode getOrgNode() {
		return this.node;
	}
}
