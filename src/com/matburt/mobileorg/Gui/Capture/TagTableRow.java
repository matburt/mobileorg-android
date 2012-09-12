package com.matburt.mobileorg.Gui.Capture;

import java.util.ArrayList;

import android.view.View;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TableLayout;
import android.widget.TableRow;

import com.matburt.mobileorg.R;
import com.matburt.mobileorg.util.OrgUtils;

class TagTableRow extends TableRow {
	private TagsFragment activity;
	private TableLayout parent;
	private Spinner spinner;
	private Button button;
	
	private String selectionExtra = ""; // used to carry ::

	public TagTableRow(TableLayout parent, final ArrayList<String> tags,
			String selectedTag, TagsFragment activity) {
		super(parent.getContext());

		this.parent = parent;
		this.activity = activity;

		View tagsView = View.inflate(getContext(), R.layout.edit_tagsrow, this);
		this.parent.addView(tagsView);
		
		button = (Button) tagsView.findViewById(R.id.edit_tag_remove);
		button.setOnClickListener(removeListener);
		
		if(selectedTag.endsWith(":")) {
			selectionExtra = ":";
			selectedTag = selectedTag.replace(":", "");
		}

		spinner = (Spinner) tagsView.findViewById(R.id.edit_tag_list);
		OrgUtils.setupSpinner(spinner, tags, selectedTag);
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
