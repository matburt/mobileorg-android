package com.matburt.mobileorg.Gui.Wizard;

import java.io.File;
import java.util.Arrays;

import android.content.Context;

public class LocalDirectoryBrowser extends DirectoryBrowser<File> {

	public LocalDirectoryBrowser(Context context) {
		super(context);
		
		browseTo(File.separator);
	}
	
	@Override
	public boolean isCurrentDirectoryRoot() {
		return currentDirectory.getParent() == null;
	}

	@Override
	public void browseTo(int position) {
		File newdir = getDir(position);
		browseTo(newdir.getAbsolutePath());
	}

	@Override
	public void browseTo(String directory) {
		currentDirectory = new File(directory);
		directoryNames.clear();
		directoryListing.clear();
		if (currentDirectory.getParent() != null) {
			directoryNames.add(upOneLevel);
			directoryListing.add(currentDirectory.getParentFile());
		}
		File[] tmpListing = currentDirectory.listFiles();
		// default list order doesn't seem to be alpha
		Arrays.sort(tmpListing);
		for (File dir : tmpListing) {
			if (dir.isDirectory() && dir.canWrite()) {
				directoryNames.add(dir.getName());
				directoryListing.add(dir);
			}
		}
	}
}
