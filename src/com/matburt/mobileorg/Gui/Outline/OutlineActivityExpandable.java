package com.matburt.mobileorg.Gui.Outline;

import android.database.Cursor;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;

import com.actionbarsherlock.app.SherlockListActivity;
import com.matburt.mobileorg.OrgData.OrgContract.OrgData;

public class OutlineActivityExpandable extends SherlockListActivity {
	
	private OutlineAdapter adapter;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
//		Cursor cursor = getContentResolver().query(
//				OrgData.buildChildrenUri(-1),
//				OrgData.DEFAULT_COLUMNS, null, null, OrgData.DEFAULT_SORT);
		//OutlineTreeCursorAdapter adapter = new OutlineTreeCursorAdapter(cursor, this);
		//this.adapter = new OutlineCustomTreeAdapter(this);
		this.adapter = new OutlineAdapter(this);
		setListAdapter(adapter);
		getListView().setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> parent, View v, int position,
					long id) {
				adapter.collapseExpand(position);
			}
		});
	}

	
}
