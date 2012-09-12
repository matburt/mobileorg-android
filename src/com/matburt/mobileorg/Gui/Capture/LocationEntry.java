package com.matburt.mobileorg.Gui.Capture;

import java.util.ArrayList;

import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.database.Cursor;
import android.text.TextUtils;
import android.util.Log;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.Spinner;

import com.matburt.mobileorg.OrgData.OrgContract.OrgData;
import com.matburt.mobileorg.OrgData.OrgNode;
import com.matburt.mobileorg.OrgData.OrgProviderUtil;
import com.matburt.mobileorg.util.FileUtils;
import com.matburt.mobileorg.util.OrgUtils;

public class LocationEntry extends Spinner {
	private LocationEntry child = null;
	private OrgNode node;
	private LocationEntry lastEntry;
	private ViewGroup locationView;
	private ContentResolver resolver;

	public LocationEntry(Context context, OrgNode node, LocationEntry child) {
		super(context);
		this.child = child;
		this.node = node;
	}

	public void setupSpinner() {
		if (this.node == null && this.child == null)
			setupSpinner(FileUtils.CAPTURE_FILE);
		else
			setupSpinner(this.node.name);
	}

	private void setupSpinner(String name) {
		ArrayList<String> children = new ArrayList<String>();
		if (this.node == null) {// Toplevel node
			Cursor cursor = resolver.query(
					OrgData.buildIdUri(Integer.toString(-1)),
					OrgData.DEFAULT_COLUMNS, null, null, null);
			children = OrgProviderUtil.cursorToArrayList(cursor);
			cursor.close();
			Log.d("MobileOrg", "setupSpinner(): this.node = null!");
		} else {
			children = node.getChildrenStringArray(resolver);
			Log.d("MobileOrg", "setupSpinner(): this.node = " + this.node.name);
		}

		OrgUtils.setupSpinner(this, children, name);
	}

	@Override
	public void onClick(DialogInterface dialog, int which) {
		super.onClick(dialog, which);
		String selectedItem = (String) this.getItemAtPosition(which);

		removeChildren();

		if (TextUtils.isEmpty(selectedItem) == false)
			addChild(selectedItem);

		//getParentNodeId();
	}

	private void addChild(String selectedItem) {
		OrgNode childNode;
		if (this.node == null) {
			Log.d("MobileOrg", "addChild(): this.node = null!");
			childNode = OrgProviderUtil.getOrgNodeFromFilename(selectedItem,
					resolver);
		} else {
			Log.d("MobileOrg", "addChild(): this.node = " + this.node.name);
			childNode = this.node.getChild(selectedItem, resolver);

			if (childNode == null)
				Log.d("MobileOrg", "addChild(): generated null node!");

			if (childNode.getChildren(resolver).size() == 0)
				return;
		}

		child = new LocationEntry(getContext(), childNode, null);
		child.setupChildSpinner();
		locationView.addView(child);
		lastEntry = child;
	}

	private void setupChildSpinner() {
		OrgUtils.setupSpinner(this, node.getChildrenStringArray(resolver), "");
	}

	private void removeChildren() {
		if (child != null) {
			locationView.removeView(child);
			child.removeChildren();
			child = null;
		}
		lastEntry = this;
	}

	public OrgNode getNode() {
		String selection = (String) this.getSelectedItem();

		if (TextUtils.isEmpty(selection))
			return this.node;

		if (this.node == null) {
			return OrgProviderUtil.getOrgNodeFromFilename(selection, resolver);
		} else
			return this.node.getChild(selection, resolver);
	}

}
