package com.matburt.mobileorg.Gui;

import java.util.ArrayList;

import android.app.SearchManager;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;

import com.actionbarsherlock.app.SherlockActivity;
import com.matburt.mobileorg.R;
import com.matburt.mobileorg.Gui.Outline.OutlineAdapter;
import com.matburt.mobileorg.Gui.Outline.OutlineListView;
import com.matburt.mobileorg.OrgData.OrgNode;
import com.matburt.mobileorg.OrgData.OrgProviderUtils;
import com.matburt.mobileorg.util.OrgUtils;

public class SearchActivity extends SherlockActivity {

	private OutlineListView listView;
	private OutlineAdapter listAdapter;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		OrgUtils.setTheme(this);
		super.onCreate(savedInstanceState);
		setContentView(R.layout.search);
		
		this.listView = (OutlineListView) findViewById(R.id.search_list);
		this.listAdapter = (OutlineAdapter) listView.getAdapter();
		listAdapter.clear();
		
		listView.setActivity(this);
		Intent intent = getIntent();
		handleIntent(intent);
	}

	@Override
	protected void onNewIntent(Intent intent) {
		handleIntent(intent);
	}
	
	private void handleIntent(Intent intent) {
		if (Intent.ACTION_VIEW.equals(intent.getAction())) {
			// This handles clicking on search suggestions
		} else if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
			String query = intent.getStringExtra(SearchManager.QUERY);
			doSearch(query);
		}
	}

	private void doSearch(String query) {
		Cursor result = OrgProviderUtils.search("%" + query.trim() + "%",
				getContentResolver());
		ArrayList<OrgNode> data = OrgProviderUtils
				.orgDataCursorToArrayList(result);

		if(result.getCount() == 0) {
			getSupportActionBar().setTitle(
					"No results found for " + " \"" + query.trim()
							+ "\"");
		} else {
			listAdapter.insertAll(data, 0);
			getSupportActionBar().setTitle(
				getString(R.string.search_results_for) + " \"" + query.trim()
						+ "\"");
		}
	}
}
