package com.matburt.mobileorg.Gui.Capture;

import java.util.ArrayList;
import java.util.Arrays;

import android.content.ContentResolver;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TableLayout;

import com.actionbarsherlock.app.SherlockFragment;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import com.matburt.mobileorg.R;
import com.matburt.mobileorg.OrgData.OrgNode;
import com.matburt.mobileorg.OrgData.OrgProviderUtils;

public class TagsFragment extends SherlockFragment {
	private static String BUNDLE_TAGS = "tags";
	
	private ContentResolver resolver;
	
	private TableLayout tagsView;
	ArrayList<TagTableRow> tagEntries = new ArrayList<TagTableRow>();

	private boolean isModifiable = true;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		super.onCreateView(inflater, container, savedInstanceState);
		setHasOptionsMenu(true);
		this.tagsView = new TableLayout(getActivity());
		return tagsView;
	}
	
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		this.resolver = getActivity().getContentResolver();
		
		EditHost host = (EditHost) getActivity();
		
		if(savedInstanceState != null)
			restoreFromBundle(savedInstanceState);
		else {
			OrgNode node = host.getController().getOrgNode();
			setupTagEntries(node.getTags());
		}
		
		setModifiable(host.getController().isNodeEditable());
		
		try {
			EditActivity activity = (EditActivity) host;
			activity.invalidateOptionsMenu();
		}
		catch (ClassCastException e) {}
	}
	
	public void restoreFromBundle(Bundle savedInstanceState) {
		if (savedInstanceState != null) {
			String tags = savedInstanceState.getString(BUNDLE_TAGS);
			ArrayList<String> tagsList = new ArrayList<String>(Arrays.asList(tags.split(":")));
						
			if(tagsList != null)
				setupTagEntries(tagsList);
		}
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
        outState.putString(BUNDLE_TAGS, getTags());
	}


	private void setupTagEntries(ArrayList<String> tagList) {
		this.tagsView.removeAllViews();
		this.tagEntries.clear();
		for (String tag : tagList) {
			if (TextUtils.isEmpty(tag)) { // found a :: entry
				for (TagTableRow entry : tagEntries) // all tags so far where unmodifiable
					entry.setModifiable(false);
				
				if (tagEntries.size() > 0) 
					tagEntries.get(tagEntries.size() - 1).setLast();
			} else
				addTagEntry(tag);
		}
	}
	
	public void setModifiable(boolean enabled) {
		for(TagTableRow entry: tagEntries)
			entry.setModifiable(enabled);
		
		this.isModifiable = enabled;
	}
	
	public void addTagEntry(String selectedTag) {
		LayoutInflater inflater = LayoutInflater.from(getActivity());
		TagTableRow tagsTableEntry =
		                (TagTableRow) inflater.inflate(R.layout.edit_tagsrow, tagsView, false);
		tagsTableEntry.setTags(selectedTag, OrgProviderUtils.getTags(resolver));
		tagsTableEntry.setParents(tagsView, this);
		tagsView.addView(tagsTableEntry);
		tagEntries.add(tagsTableEntry);
	}

	public String getTags() {
		StringBuilder result = new StringBuilder();
		for(TagTableRow entry: tagEntries) {
			String selection = entry.getSelection();
			if(TextUtils.isEmpty(selection) == false) {
				result.append(selection);
				result.append(":");
			}
		}
		
		if(!TextUtils.isEmpty(result))
			result.deleteCharAt(result.lastIndexOf(":"));
		
		return result.toString();
	}
	
	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		inflater.inflate(R.menu.edit_tags, menu);
		super.onCreateOptionsMenu(menu, inflater);
	}

	@Override
	public void onPrepareOptionsMenu(Menu menu) {
		super.onPrepareOptionsMenu(menu);
		if(this.isModifiable)
			menu.findItem(R.id.menu_edit_addtag).setVisible(true);
		else
			menu.findItem(R.id.menu_edit_addtag).setVisible(false);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {

		case R.id.menu_edit_addtag:
			addTagEntry("");
			return true;

		default:
			return super.onOptionsItemSelected(item);
		}
	}
}
