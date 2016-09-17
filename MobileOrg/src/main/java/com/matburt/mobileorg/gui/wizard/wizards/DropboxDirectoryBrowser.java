package com.matburt.mobileorg.gui.wizard.wizards;

import android.content.Context;

import com.dropbox.client2.DropboxAPI;
import com.dropbox.client2.DropboxAPI.Entry;
import com.dropbox.client2.android.AndroidAuthSession;
import com.dropbox.client2.exception.DropboxException;
import com.matburt.mobileorg.gui.wizard.DirectoryBrowser;

import java.io.File;

public class DropboxDirectoryBrowser extends DirectoryBrowser<String> {
	
	private DropboxAPI<AndroidAuthSession> dropbox;

	DropboxDirectoryBrowser(Context context,
			DropboxAPI<AndroidAuthSession> dropboxApi) {
		super(context);
		this.dropbox = dropboxApi;
		
		browseTo(File.separator);
	}

	private static String getParentPath(String path) {
		int ind = path.lastIndexOf(File.separatorChar);
		return path.substring(0, ind + 1);
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
		}

		try {
			Entry entries = dropbox.metadata(directory, 1000, null, true, null);

			for (Entry e : entries.contents) {
				if (e.isDir) {
					directoryNames.add(e.fileName());
					directoryListing.add(e.path);
				}
			}
		} catch (DropboxException e) {
//			Log.d("MobileOrg",	"Failed to list directory for dropbox: " + e.toString());
		}
	}
}
