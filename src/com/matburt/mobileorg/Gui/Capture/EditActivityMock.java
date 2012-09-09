package com.matburt.mobileorg.Gui.Capture;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;

import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.matburt.mobileorg.OrgData.OrgNode;

public class EditActivityMock extends SherlockFragmentActivity {
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		this.setContentView(com.matburt.mobileorg.R.layout.edit);
		EditDetailsFragment detailsFragment = new EditDetailsFragment();
		OrgNode node = new OrgNode();
		node.name = "initial";
		//detailsFragment.setNode(node, "", getContentResolver());
		addFragment(detailsFragment);
	}
	
	public void addFragment(EditDetailsFragment detailsFragment) {
		FragmentManager fragmentManager = getSupportFragmentManager();
		FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
		fragmentTransaction.add(com.matburt.mobileorg.R.id.editnode_fragment_container, detailsFragment, "details");
		fragmentTransaction.commit();
	}
	
	public void replaceFragment(OrgNode node) {
		FragmentManager fragmentManager = getSupportFragmentManager();
		Fragment fragment = fragmentManager.findFragmentByTag("details");
		FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
		fragmentTransaction.remove(fragment).commit();
		
		fragmentTransaction = fragmentManager.beginTransaction();
		EditDetailsFragment detailsFragment = new EditDetailsFragment();
		//detailsFragment.setNode(node, "", getContentResolver());
		fragmentTransaction.add(com.matburt.mobileorg.R.id.editnode_fragment_container, detailsFragment, "details2");
		fragmentTransaction.commit();
		
		fragmentManager.executePendingTransactions();
	}
}
