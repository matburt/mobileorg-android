package com.matburt.mobileorg.Synchronizers;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;

import android.content.Context;
import android.preference.PreferenceManager;

import com.matburt.mobileorg.Parsing.OrgFile;

public class SDCardSynchronizer extends Synchronizer
{	
    SDCardSynchronizer(Context context) {
    	super(context);
		this.remoteIndexPath = PreferenceManager.getDefaultSharedPreferences(
				context).getString("indexFilePath", "");
		
		this.remotePath = new File(remoteIndexPath).getParent() + "/";
	}
    

    public boolean isConfigured() {
        if (remoteIndexPath.equals(""))
            return false;
        return true;
    }

	protected void putRemoteFile(String filename, String contents) throws IOException {
		String outfilePath = this.remotePath + filename;
		
		OrgFile orgfileOut = new OrgFile(outfilePath, context);
		orgfileOut.write(outfilePath, contents);
	}

	protected BufferedReader getRemoteFile(String filename) throws FileNotFoundException {
		String filePath = this.remotePath + filename;
		File file = new File(filePath);
		FileInputStream fileIS = new FileInputStream(file);
		return new BufferedReader(new InputStreamReader(fileIS));
	}
}