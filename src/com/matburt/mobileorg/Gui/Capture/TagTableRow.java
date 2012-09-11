package com.matburt.mobileorg.Gui.Capture;

import java.util.ArrayList;

import android.content.Context;
import android.view.View;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TableLayout;
import android.widget.TableRow;

import com.matburt.mobileorg.R;

class TagTableRow extends TableRow {
	private TagsFragment activity;
	private TableLayout parent;
	private Spinner spinner;
	private Button button;
	
	private String selectionExtra = ""; // used to carry ::

	public TagTableRow(Context context, TableLayout parent,
			final ArrayList<String> tags, String selection, TagsFragment activity) {
		super(context);

		this.parent = parent;
		this.activity = activity;

		View tagsView = View.inflate(getContext(), R.layout.edit_tagsrow, this);
		
		button = (Button) tagsView.findViewById(R.id.edit_tag_remove);
		button.setOnClickListener(removeListener);
		
		if(selection.endsWith(":")) {
			selectionExtra = ":";
			selection = selection.replace(":", "");
		}

		spinner = (Spinner) tagsView.findViewById(R.id.edit_tag_list);
		HeadingFragment.setupSpinner(spinner, tags, selection);
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
