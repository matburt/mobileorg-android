package com.matburt.mobileorg.Gui.Capture;

import java.util.ArrayList;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TableLayout;
import android.widget.TableRow;

import com.matburt.mobileorg.R;

class TagTableRow extends TableRow {
	EditDetailsFragment activity;
	TableLayout parent;
	Spinner spinner;
	Button button;
	
	String selectionExtra = ""; // used to carry ::

	public TagTableRow(Context context, TableLayout parent,
			final ArrayList<String> tags, String selection, EditDetailsFragment activity) {
		super(context);

		this.parent = parent;
		this.activity = activity;

		LayoutInflater layoutInflater = (LayoutInflater) context
				.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		TableRow row = (TableRow) layoutInflater.inflate(
				R.layout.edit_tagsrow, this);

		button = (Button) findViewById(R.id.editnode_tag_remove);
		button.setOnClickListener(removeListener);
		
		if(selection.endsWith(":")) {
			selectionExtra = ":";
			selection = selection.replace(":", "");
		}

		spinner = (Spinner) row.findViewById(R.id.editnode_tag_list);
		EditDetailsFragment.setupSpinner(context, spinner, tags, selection);
	}
	
	public void setUnmodifiable() {
		button.setVisibility(INVISIBLE);
		spinner.setEnabled(false);
	}
	
	public void setLast() {
		selectionExtra = ":";
	}
	
	public String getSelection() {
		return spinner.getSelectedItem().toString() + selectionExtra;
	}
	
	private void remove() {
		parent.removeView(this);
		activity.tagEntries.remove(this);
	}
	
	private View.OnClickListener removeListener = new View.OnClickListener() {
		public void onClick(View v) {
			remove();
		}
	};
}
