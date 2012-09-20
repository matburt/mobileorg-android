package com.matburt.mobileorg.Gui;

import android.app.ListActivity;
import android.app.SearchManager;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.SimpleCursorAdapter;

import com.matburt.mobileorg.OrgData.OrgProviderUtils;

public class SearchActivity extends ListActivity {

	private SimpleCursorAdapter adapter;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

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
		Cursor result = OrgProviderUtils.search("%"+ query.trim() + "%", getContentResolver());
		
		adapter = new OutlineCursorAdapter(this, result, getContentResolver());
		this.setListAdapter(adapter);
				
		this.getListView().setOnItemClickListener(showNode);
	}
	
	private OnItemClickListener showNode = new AdapterView.OnItemClickListener() {
		@Override
		public void onItemClick(AdapterView<?> parent, View view, int position,
				long id) {
			Intent intent = new Intent(getApplicationContext(), ViewFragment.class);
			intent.putExtra("node_id", id);
			startActivity(intent);
		}
	};
}
