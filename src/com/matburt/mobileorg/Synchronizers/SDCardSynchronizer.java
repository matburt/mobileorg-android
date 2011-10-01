package com.matburt.mobileorg.Synchronizers;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;

import android.content.Context;
import android.preference.PreferenceManager;

import com.matburt.mobileorg.Parsing.NodeWriter;
import com.matburt.mobileorg.Parsing.OrgFile;

public class SDCardSynchronizer extends Synchronizer
{
	private String indexFilePath;
	private String outfile;
	private String basePath;
	
    public SDCardSynchronizer(Context context) {
    	super(context);
		this.indexFilePath = PreferenceManager.getDefaultSharedPreferences(
				context).getString("indexFilePath", "");
		
		String indexFileDirectory = new File(indexFilePath).getParent();
		this.outfile = indexFileDirectory + "/" + NodeWriter.ORGFILE;
		
		File fIndexFile = new File(indexFilePath);
		this.basePath = fIndexFile.getParent();
	}
    

    public boolean isConfigured() {
        if (indexFilePath.equals(""))
            return false;
        return true;
    }

	public void push() throws IOException {
		String fileContents = OrgFile.fileToString(NodeWriter.ORGFILE, context);
		String originalContent = OrgFile.fileToString(getFile(this.outfile));
		originalContent += "\n";

		String newContent = originalContent + fileContents;
		OrgFile.putFile(this.outfile, newContent);
	}

	public void pull() throws IOException {
		updateFiles(indexFilePath, basePath);
    }

	protected BufferedReader getFile(String filePath) throws FileNotFoundException {
		FileInputStream readerIS;
		BufferedReader fReader;
		File inpfile = new File(filePath);

		readerIS = new FileInputStream(inpfile);
		fReader = new BufferedReader(new InputStreamReader(readerIS));
		
		return fReader;
	}
}