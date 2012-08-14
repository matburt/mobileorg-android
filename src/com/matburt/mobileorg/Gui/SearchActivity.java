package com.matburt.mobileorg.Gui;

import android.annotation.SuppressLint;
import android.app.ListActivity;
import android.app.SearchManager;
import android.content.Intent;
import android.database.Cursor;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.SimpleCursorAdapter;

import com.matburt.mobileorg.Parsing.MobileOrgApplication;

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
		MobileOrgApplication appInst = (MobileOrgApplication) this
				.getApplication();
		
		Cursor result = appInst.getDB().search("%"+ query.trim() + "%");
		
		adapter = new OutlineCursorAdapter(this, result, appInst.getDB());
		this.setListAdapter(adapter);
				
		this.getListView().setOnItemClickListener(showNode);
	}
	
	private OnItemClickListener showNode = new AdapterView.OnItemClickListener() {
		@Override
		public void onItemClick(AdapterView<?> parent, View view, int position,
				long id) {
			Intent intent = new Intent(getApplicationContext(), NodeViewActivity.class);
			intent.putExtra("node_id", id);
			startActivity(intent);
		}
	};
	
	@SuppressLint("NewApi")
	@Override
	public void finish() {
		super.finish();
		// Disable transitions if configured
		if (Build.VERSION.SDK_INT >= 5 && !PreferenceManager.getDefaultSharedPreferences(this).getBoolean("viewAnimateTransitions", true)) {
			overridePendingTransition(0, 0);
		}	
	}
}
