package com.matburt.mobileorg.Gui;

import android.content.ContentResolver;
import android.content.Intent;
import android.os.Bundle;
import android.preference.PreferenceManager;

import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.matburt.mobileorg.R;
import com.matburt.mobileorg.OrgData.OrgNode;
import com.matburt.mobileorg.util.OrgNodeNotFoundException;

public class ViewActivity extends SherlockFragmentActivity {
	public static String NODE_ID = "node_id";

	private ContentResolver resolver;
	private ViewFragment nodeViewFragment;

	private long node_id;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.view);
		this.resolver = getContentResolver();

		Intent intent = getIntent();
		this.node_id = intent.getLongExtra(NODE_ID, -1);
	}
	
	@Override
	protected void onStart() {
		super.onStart();

		this.nodeViewFragment = ((ViewFragment) getSupportFragmentManager()
				.findFragmentById(R.id.view_fragment));

		refreshDisplay();
	}

	private void refreshDisplay() {
		try {
			OrgNode node = new OrgNode(this.node_id, resolver);
			int levelOfRecursion = Integer.parseInt(PreferenceManager
					.getDefaultSharedPreferences(this).getString(
							"viewRecursionMax", "0"));
			nodeViewFragment.display(node, levelOfRecursion, resolver);
		} catch (OrgNodeNotFoundException e) {
			nodeViewFragment.displayError();
		}
	}
}
