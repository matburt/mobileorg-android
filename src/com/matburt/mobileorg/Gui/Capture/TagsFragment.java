package com.matburt.mobileorg.Gui.Capture;

import java.util.ArrayList;

import android.content.ContentResolver;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
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
import com.matburt.mobileorg.OrgData.OrgProviderUtil;

public class TagsFragment extends SherlockFragment {
	private static String BUNDLE_TAGS = "tags";
	
	private ContentResolver resolver;
	
	private TableLayout tagsView;
	ArrayList<TagTableRow> tagEntries = new ArrayList<TagTableRow>();

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		setHasOptionsMenu(true);
		this.tagsView = new TableLayout(getActivity());
		return tagsView;
	}
	
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		this.resolver = getActivity().getContentResolver();
		
		if(savedInstanceState != null)
			restoreFromBundle(savedInstanceState);
		else {
			OrgNode node = ((EditActivity) getActivity()).getOrgNode();
			setupTagEntries(node.getTags());
		}
	}
	
	public void restoreFromBundle(Bundle savedInstanceState) {
		if (savedInstanceState != null) {
			ArrayList<String> tagsToRestore = savedInstanceState.getStringArrayList(BUNDLE_TAGS);
						
			if(tagsToRestore != null)
				setupTagEntries(tagsToRestore);
		}
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);

        ArrayList<String> tags = new ArrayList<String>();
        for(TagTableRow tagTableRow: tagEntries) {
        	tags.add(tagTableRow.getSelection());
        }
        outState.putStringArrayList(BUNDLE_TAGS, tags);
	}


	private void setupTagEntries(ArrayList<String> tagList) {
		tagsView.removeAllViews();
		this.tagEntries.clear();
		for (String tag : tagList) {
			Log.d("MobileOrg", "Setting up: " + tag);

			if (TextUtils.isEmpty(tag)) { // found a :: entry
				for (TagTableRow entry : tagEntries) // all tags so far where unmodifiable
					entry.setUnmodifiable();
			} else
				addTagEntry(tag);
		}
		
		if (tagEntries.size() > 0) 
			tagEntries.get(tagEntries.size() - 1).setLast();
	}
	
	public void addTagEntry(String tag) {
		TagTableRow tagEntry = new TagTableRow(getActivity(), tagsView,
				OrgProviderUtil.getTags(resolver), tag, this);
		tagEntries.add(tagEntry);
		tagsView.addView(tagEntry);
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
		super.onCreateOptionsMenu(menu, inflater);
		inflater.inflate(R.menu.edit_tags, menu);
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
