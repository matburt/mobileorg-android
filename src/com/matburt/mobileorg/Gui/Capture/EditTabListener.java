package com.matburt.mobileorg.Gui.Capture;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;

import com.actionbarsherlock.app.ActionBar.Tab;
import com.actionbarsherlock.app.ActionBar.TabListener;
import com.matburt.mobileorg.R;

class EditTabListener implements TabListener {
	Fragment fragment;
	FragmentManager manager;

	public EditTabListener(Fragment fragment, String tag, FragmentManager manager) {
		this.fragment = fragment;
		this.manager = manager;

		FragmentTransaction fragmentTransaction = manager.beginTransaction();

		if (fragment != null && fragment.isAdded() == false) {
			fragmentTransaction.add(R.id.editnode_fragment_container,
					fragment, tag);
		}

		fragmentTransaction.hide(fragment).commit();
	}

	@Override
	public void onTabSelected(Tab tab, FragmentTransaction ft) {			
		FragmentTransaction fragmentTransaction = manager.beginTransaction();
	    fragmentTransaction.show(fragment).commit();
	}

	@Override
	public void onTabUnselected(Tab tab, FragmentTransaction ft) {
		if (fragment != null) {
			FragmentTransaction fragmentTransaction = manager.beginTransaction();
			fragmentTransaction.hide(fragment).commit();
		}
	}
	
	public void replaceFragment(Fragment fragment, String tag) {
		FragmentTransaction fragmentTransaction = manager.beginTransaction();
		fragmentTransaction.remove(this.fragment);
		fragmentTransaction.add(R.id.editnode_fragment_container, fragment, tag);
		fragmentTransaction.commit();
	}

	@Override
	public void onTabReselected(Tab tab, FragmentTransaction ft) {
	}
};
