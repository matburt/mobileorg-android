package com.matburt.mobileorg.Gui.Wizard;

import java.io.File;
import java.util.ArrayList;

import android.content.Context;

import com.matburt.mobileorg.R;

public abstract class DirectoryBrowser<T> {

	protected Context context;
	
	protected T currentDirectory;
	protected ArrayList<T> directoryListing = new ArrayList<T>();
	protected ArrayList<String> directoryNames = new ArrayList<String>();	

	protected String upOneLevel;

	public abstract void browseTo(int position);
	protected abstract void browseTo(String directory);
	public abstract boolean isCurrentDirectoryRoot();

	public DirectoryBrowser(Context context) {
		this.context = context;
		this.upOneLevel = context.getString(R.string.up_one_level);
	}
	
	
	public ArrayList<String> listFiles() {
		return directoryNames;
	}
	
	public String getDirectoryName(int position) {
		return directoryNames.get(position);
	}
	
	protected T getDir(int position) {
		return directoryListing.get(position);
	}
	
	public String getAbsolutePath(int position) {
		T directory = directoryListing.get(position);
		
		if (directory instanceof String)
			return (String) directory;
		
		if (directory instanceof File)
			return ((File) directory).getAbsolutePath();
		
		return "";
	}
}
