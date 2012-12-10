package com.matburt.mobileorg.Gui.Wizard.Wizards;

import java.io.File;

import android.content.Context;
import android.util.Log;

import com.matburt.mobileorg.Gui.Wizard.DirectoryBrowser;
import com.matburt.mobileorg.Synchronizers.UbuntuOneSynchronizer;

public class UbuntuOneDirectoryBrowser extends DirectoryBrowser<String> {
	private UbuntuOneSynchronizer synchronizer;

	public UbuntuOneDirectoryBrowser(Context context, UbuntuOneSynchronizer synchronizer) {
		super(context);
		this.synchronizer = synchronizer;

		browseTo(File.separator);
	}

	@Override
	public boolean isCurrentDirectoryRoot() {
		return currentDirectory.equals(File.separator);
	}

	@Override
	public void browseTo(int position) {
		browseTo(getDir(position));
	}

	@Override
	public void browseTo(String directory) {
		currentDirectory = directory;
		directoryNames.clear();
		directoryListing.clear();
		if (!isCurrentDirectoryRoot()) {
			directoryNames.add(upOneLevel);
			directoryListing.add(getParentPath(currentDirectory));
			Log.d("MobileOrg", "Current directory: " + currentDirectory);
			Log.d("MobileOrg", "Parent path: "
					+ getParentPath(currentDirectory));
		}
		for (String item : synchronizer.getDirectoryList(directory)) {
			directoryNames.add(item);
			directoryListing.add(item);
		}
	}
	
	private static String getParentPath(String path) {
		if (path.charAt(path.length() - 1) == File.separatorChar) {
			path = path.substring(0, path.length() - 1);
		}
		int ind = path.lastIndexOf(File.separatorChar);
		return path.substring(0, ind + 1);
	}
}
