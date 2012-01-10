package com.matburt.mobileorg.Gui;

import android.app.ListActivity;
import android.app.SearchManager;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;

import com.matburt.mobileorg.R;
import com.matburt.mobileorg.Parsing.MobileOrgApplication;

public class SearchActivity extends ListActivity {

	private OutlineCursorAdapter adapter;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.search_outline);

		// Get the intent, verify the action and get the query
		Intent intent = getIntent();
		if (Intent.ACTION_VIEW.equals(intent.getAction())) {
			String query = intent.getStringExtra(SearchManager.QUERY);
			doSearch(query);
		} else if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
			String query = intent.getStringExtra(SearchManager.QUERY);
			doSearch(query);
		}

	}

	@Override
	protected void onNewIntent(Intent intent) {
		setIntent(intent);
		doSearch("");
	}

	void doSearch(String query) {
		MobileOrgApplication appInst = (MobileOrgApplication) this
				.getApplication();
		Cursor result = appInst.getDB().search(query);

		adapter = new OutlineCursorAdapter(this, result);
		this.setListAdapter(adapter);
	}
}
