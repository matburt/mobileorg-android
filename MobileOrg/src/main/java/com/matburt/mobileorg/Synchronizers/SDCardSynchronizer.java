package com.matburt.mobileorg.Synchronizers;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;

import android.content.Context;
import android.preference.PreferenceManager;

public class SDCardSynchronizer implements SynchronizerInterface {	

	private String remoteIndexPath;
	private String remotePath;

    public SDCardSynchronizer(Context context) {
		this.remoteIndexPath = PreferenceManager.getDefaultSharedPreferences(
				context).getString("indexFilePath", "");
	
		this.remotePath = new File(remoteIndexPath) + "/";
	}
    

    public boolean isConfigured() {
        if (remoteIndexPath.equals(""))
            return false;
        return true;
    }

	public void putRemoteFile(String filename, String contents) throws IOException {
		String outfilePath = this.remotePath + filename;
		
		File file = new File(outfilePath);
		BufferedWriter writer = new BufferedWriter(new FileWriter(file, true));
		writer.write(contents);
		writer.close();
	}

	public BufferedReader getRemoteFile(String filename) throws FileNotFoundException {
		String filePath = this.remotePath + filename;
		File file = new File(filePath);
		FileInputStream fileIS = new FileInputStream(file);
		return new BufferedReader(new InputStreamReader(fileIS));
	}


	@Override
	public void postSynchronize() {		
	}


	@Override
	public boolean isConnectable() {
		return true;
	}
}