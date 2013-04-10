package com.matburt.mobileorg.Gui;

import android.content.ContentResolver;
import android.content.Intent;
import android.os.Bundle;

import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.actionbarsherlock.view.SubMenu;
import com.matburt.mobileorg.R;
import com.matburt.mobileorg.OrgData.OrgNode;
import com.matburt.mobileorg.util.OrgNodeNotFoundException;
import com.matburt.mobileorg.util.OrgUtils;
import com.matburt.mobileorg.util.PreferenceUtils;

public class ViewActivity extends SherlockFragmentActivity {
	public static String NODE_ID = "node_id";

	private ContentResolver resolver;
	private ViewFragment nodeViewFragment;

	private long nodeId;
	private OrgNode node;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		OrgUtils.setTheme(this);
		super.onCreate(savedInstanceState);

		setContentView(R.layout.view);
		this.resolver = getContentResolver();

		Intent intent = getIntent();
		this.nodeId = intent.getLongExtra(NODE_ID, -1);
	}
	
	@Override
	protected void onStart() {
		super.onStart();

		this.nodeViewFragment = ((ViewFragment) getSupportFragmentManager()
				.findFragmentById(R.id.view_fragment));

		try {
			this.node = new OrgNode(nodeId, resolver);
			viewNode(PreferenceUtils.getLevelOfRecursion());
		} catch (OrgNodeNotFoundException e) {
			nodeViewFragment.displayError();
		}
	}

	public void viewNode(int levelOfRecursion) {
		if(node != null) {
			nodeViewFragment.display(node, levelOfRecursion, resolver);
			String path = node.getOlpId(resolver);
			if(path.startsWith("olp:"))
				path = path.substring("olp:".length());
			getSupportActionBar().setTitle(path);
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		
		SubMenu subMenu = menu.addSubMenu(R.string.menu_advanced);
		MenuItem subMenuItem = subMenu.getItem();
		subMenuItem.setIcon(R.drawable.ic_menu_view);
		subMenuItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
		
		setupRecursionSubmenu(subMenu);
		
		return true;
	}
	
	private void setupRecursionSubmenu(SubMenu subMenu) {
		String[] recursionStrings = getResources().getStringArray(R.array.viewRecursionLevels);
		
		for (int i = 0; i < recursionStrings.length; i++) {
			MenuItem item = subMenu.add(R.string.menu_advanced,
					R.string.contextmenu_view, i, recursionStrings[i]);
			item.setIcon(R.drawable.ic_menu_view);
		}
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getOrder()) {
		case 0:
			viewNode(0);
			break;
		case 1:
			viewNode(1);
			break;
		case 2:
			viewNode(2);
			break;
		case 3:
			viewNode(3);
			break;
		case 4:
			viewNode(4);
			break;
		default:
			return super.onOptionsItemSelected(item);
		}

		return true;
	}
}
