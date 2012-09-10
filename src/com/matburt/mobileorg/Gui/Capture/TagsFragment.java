package com.matburt.mobileorg.Gui.Capture;

import java.util.ArrayList;

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
import com.matburt.mobileorg.OrgData.OrgProviderUtil;

public class TagsFragment extends SherlockFragment {

	private ContentResolver resolver;
	
	private TableLayout tagsView;
	ArrayList<TagTableRow> tagEntries = new ArrayList<TagTableRow>();
	private ArrayList<String> tagsToRestore = null;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		this.tagsView = new TableLayout(getActivity());

		if (savedInstanceState != null) {
			tagsToRestore = savedInstanceState.getStringArrayList("tags");
		}
        		
		return tagsView;
	}
	
	@Override
	public void onStart() {
		super.onStart();
		
		this.resolver = getActivity().getContentResolver();
		
		if(tagsToRestore != null) {
			tagsView.removeAllViews();
			tagEntries.clear();
			setupTags(tagsToRestore);
			tagsToRestore = null;
		}
		
		OrgNode node = new OrgNode();
		setupTags(node.getTags());
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);

        
        ArrayList<String> tags = new ArrayList<String>();
        for(TagTableRow tag: tagEntries) {
        	tags.add(tag.getSelection());
        }
        outState.putStringArrayList("tags", tags);
	}
	
	
	
	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		// TODO Auto-generated method stub
		super.onCreateOptionsMenu(menu, inflater);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {

		case R.id.menu_nodeedit_tag:
			addTag("");
			return true;

		default:
			return super.onOptionsItemSelected(item);
		}
	}

	private void setupTags(ArrayList<String> tagList) {		
		for(String tag: tagList) {
			if(TextUtils.isEmpty(tag)) { 
				// OrgNode found a :: entry, meaning all tags so far where unmodifiable
				for(TagTableRow entry: tagEntries)
					entry.setUnmodifiable();
				if(tagEntries.size() > 0)
					tagEntries.get(tagEntries.size() - 1).setLast();
			}
			else
				addTag(tag);
		}
	}
	
	private void addTag(String tag) {
		TagTableRow tagEntry = new TagTableRow(getActivity(), tagsView, OrgProviderUtil.getTags(resolver), tag, this);
		tagsView.addView(tagEntry);
		tagEntries.add(tagEntry);
	}
	
	private String getTags() {
		StringBuilder result = new StringBuilder();
		for(TagTableRow entry: tagEntries) {
			String selection = entry.getSelection();
			if(TextUtils.isEmpty(selection) == false) {
				result.append(selection);
				result.append(":");
			}
		}
		
		if(TextUtils.isEmpty(result))
			return "";
		else
			return result.deleteCharAt(result.lastIndexOf(":")).toString();
	}
}
