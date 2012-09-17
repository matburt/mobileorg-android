package com.matburt.mobileorg.Gui.Capture;

import java.util.ArrayList;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TableLayout;
import android.widget.TableRow;

import com.matburt.mobileorg.R;
import com.matburt.mobileorg.util.OrgUtils;

class TagTableRow extends TableRow {
	// Each spinner needs its own id to get correct behavior
	private static int spinnerId = 0;
	
	private Spinner spinner;
	private Button button;
	
	private String selectionExtra = ""; // used to carry ::

	private TableLayout parentView;
	private TagsFragment tagsFragment;

	public TagTableRow(Context context, AttributeSet attrs) {
		super(context, attrs);
	}
	
	public void setTags(String selectedTag, ArrayList<String> tags) {
		if(selectedTag.endsWith(":")) {
			selectionExtra = ":";
			selectedTag = selectedTag.replace(":", "");
		}

		spinner = (Spinner) findViewById(R.id.edit_tag_list);
		spinner.setId(spinnerId++);
		OrgUtils.setupSpinnerWithEmpty(spinner, tags, selectedTag);
		
		button = (Button) findViewById(R.id.edit_tag_remove);
		button.setOnClickListener(removeListener);
	}
	
	public void setParents(TableLayout parentView, TagsFragment tagsFragment) {
		this.parentView = parentView;
		this.tagsFragment = tagsFragment;
	}
	
	public void setModifiable(boolean enabled) {
		if(enabled)
			button.setVisibility(VISIBLE);
		else
			button.setVisibility(INVISIBLE);
		spinner.setEnabled(enabled);
	}
	
	public void setLast() {
		selectionExtra = ":";
	}
	
	public String getSelection() {
		return spinner.getSelectedItem().toString() + selectionExtra;
	}
	
	private void remove() {
		if(parentView != null)
			parentView.removeView(this);
		if(tagsFragment != null)
			tagsFragment.tagEntries.remove(this);
	}
	
	private View.OnClickListener removeListener = new View.OnClickListener() {
		public void onClick(View v) {
			remove();
		}
	};
}
