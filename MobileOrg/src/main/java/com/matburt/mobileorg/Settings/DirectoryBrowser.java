package com.matburt.mobileorg.Settings;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;

import android.content.Context;
import android.util.Log;

import com.dropbox.client2.DropboxAPI;
import com.dropbox.client2.DropboxAPI.Entry;
import com.dropbox.client2.android.AndroidAuthSession;
import com.dropbox.client2.exception.DropboxException;
import com.matburt.mobileorg.R;
import com.matburt.mobileorg.Synchronizers.UbuntuOneSynchronizer;

public interface DirectoryBrowser {

	public void browseTo(String directory);
	
	public void browseTo(int position);
	
	public String get(int position);
	
	public String getAbsolutePath(int position);
	
	public boolean isCurrentDirectoryRoot();
	
	public ArrayList<String> list();
	
	//Class for browsing local file system
	
	public class LocalDirectoryBrowser implements DirectoryBrowser {
	    ArrayList<String> directoryNames = new ArrayList<String>();
	    ArrayList<File> directoryListing = new ArrayList<File>();
	    File curDirectory;
	    Context context;
	    String upOneLevel = "Up one level";
		
		LocalDirectoryBrowser() {
			browseTo(File.separator);
		}
		
	    LocalDirectoryBrowser(Context context) {
		setContext(context);
		setLocale();
		browseTo(File.separator);
	    }

	    public void setContext(Context context) { this.context = context; }

	    public void setLocale() { 
		upOneLevel = context.getString(R.string.up_one_level); 
	    }

		public ArrayList<String> list() { return directoryNames; }
		
		public String get(int position) { return directoryNames.get(position); }
		
		public File getDirectory(int position) { return directoryListing.get(position); }
		
		public String getAbsolutePath(int position) { 
			return getDirectory(position).getAbsolutePath(); }
		
		public boolean isCurrentDirectoryRoot() { return curDirectory.getParent() == null; }
		
		public void browseTo(int position) {
			File newdir = getDirectory(position);
			browseTo( newdir.getAbsolutePath() );
		}
		
		public void browseTo(String directory) {
			curDirectory = new File(directory);
			directoryNames.clear();
			directoryListing.clear();
			if ( curDirectory.getParent() != null ) {
				directoryNames.add( upOneLevel );
				directoryListing.add( curDirectory.getParentFile() );
			}
			File[] tmpListing = curDirectory.listFiles();
			//default list order doesn't seem to be alpha
			Arrays.sort(tmpListing);
			for(File dir:tmpListing) {
				if ( dir.isDirectory() 
						&& dir.canWrite() ) {
					directoryNames.add( dir.getName() );
					directoryListing.add( dir );
				}
			}
		}
	}

    public class UbuntuOneDirectoryBrowser implements DirectoryBrowser {
        UbuntuOneSynchronizer onesync;

		ArrayList<String> directoryNames = new ArrayList<String>();
		ArrayList<String> directoryListing = new ArrayList<String>();
		String curDirectory;
		Context context;
		String upOneLevel = "Up one level";

        UbuntuOneDirectoryBrowser(Context context, UbuntuOneSynchronizer uos) {
            onesync = uos;
            setContext(context);
            setLocale();
            browseTo("/");
        }

		public void setContext(Context context) { this.context = context; }

		public void setLocale() { 
			upOneLevel = context.getString(R.string.up_one_level); 
		}

		public ArrayList<String> list() { return directoryNames; }

		public String get(int position) { return directoryNames.get(position); }

		public String getDirectory(int position) { return directoryListing.get(position); } 

		public String getAbsolutePath(int position) { return directoryListing.get(position); } 

		public boolean isCurrentDirectoryRoot() { return curDirectory.equals("/"); }

		static public String getParentPath(String path) {
            if (path.charAt(path.length()-1) == '/') {
                path = path.substring(0, path.length()-1);
            }
			int ind = path.lastIndexOf('/');
			return path.substring(0, ind+1);
		}
		
		public void browseTo(int position) { browseTo( getDirectory(position) ); }

		public void browseTo(String directory) {
			curDirectory = directory;
			directoryNames.clear();
			directoryListing.clear();
			if ( !isCurrentDirectoryRoot() ) {
				directoryNames.add( upOneLevel );
				directoryListing.add( getParentPath(curDirectory) );
                Log.d("MobileOrg", "Current directory: " + curDirectory);
                Log.d("MobileOrg", "Parent path: " + getParentPath(curDirectory));
			}
            for (String item : onesync.getDirectoryList(directory)) {
                directoryNames.add(item);
                directoryListing.add(item);
            }
		}
    }
	
	public class DropboxDirectoryBrowser implements DirectoryBrowser {
		DropboxAPI<AndroidAuthSession> dropbox;
		//array list for adapter
		ArrayList<String> directoryNames = new ArrayList<String>();
		//array list containing full path names 
		ArrayList<String> directoryListing = new ArrayList<String>();
		String curDirectory;
		Context context;
		String upOneLevel = "Up one level";

		DropboxDirectoryBrowser(Context context, DropboxAPI dropboxApi) {
			this.dropbox = dropboxApi;
			setContext(context);
			setLocale();
			browseTo("/");
		}

		public void setContext(Context context) { this.context = context; }

		public void setLocale() { 
			upOneLevel = context.getString(R.string.up_one_level); 
		}

		public ArrayList<String> list() { return directoryNames; }

		public String get(int position) { return directoryNames.get(position); }

		public String getDirectory(int position) { return directoryListing.get(position); } 

		public String getAbsolutePath(int position) { return directoryListing.get(position); } 

		public boolean isCurrentDirectoryRoot() { return curDirectory.equals("/"); }

		static public String getParentPath(String path) {
			int ind = path.lastIndexOf('/');
			return path.substring(0, ind+1);
		}
		
		public void browseTo(int position) { browseTo( getDirectory(position) ); }

		public void browseTo(String directory) {
			curDirectory = directory;
			directoryNames.clear();
			directoryListing.clear();
			if ( !isCurrentDirectoryRoot() ) {
				directoryNames.add( upOneLevel );
				directoryListing.add( getParentPath(curDirectory) );
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
                Log.d("MobileOrg", "Failed to list directory for dropbox: " + e.toString());
            }
		}
	}
}

