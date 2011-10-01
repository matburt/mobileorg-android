package com.matburt.mobileorg.Parsing;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;

import android.content.Context;
import android.os.Environment;
import android.preference.PreferenceManager;

public class OrgFile {

	final private static int BUFFER_SIZE = 23 * 1024;

	String filePath;
	
	public OrgFile(String filePath) {
		this.filePath = filePath;
	}
	

	public static String fileToString (String filename, Context context) throws IOException {
		BufferedReader reader = getReadHandle(filename, context);	
		return fileToString(reader);
	}
	
	public static String fileToString(BufferedReader reader) throws IOException {
		if (reader == null) {
			return "";
		}
		
		StringBuilder fileContents = new StringBuilder();
		String line;
		
		while ((line = reader.readLine()) != null) {
		    fileContents.append(line);
		    fileContents.append("\n");
		}

		return fileContents.toString();
	}
	

	private static BufferedReader getReadHandle(String filename, Context context) throws FileNotFoundException {
		String storageMode = getStorageMode(context);
		BufferedReader reader = null;
		
		if (storageMode.equals("internal") || storageMode.equals("")) {
			FileInputStream fs;
			fs = context.openFileInput(filename);
			reader = new BufferedReader(new InputStreamReader(fs));

		} else if (storageMode.equals("sdcard")) {
			File root = Environment.getExternalStorageDirectory();
			File morgDir = new File(root, "mobileorg");
			File morgFile = new File(morgDir, filename);
			if (!morgFile.exists()) {
				return null;
			}
			FileReader orgFReader = new FileReader(morgFile);
			reader = new BufferedReader(orgFReader);
		}
		
		return reader;
	}

	public static void putFile(String path,
                         String content) throws IOException {
        BufferedWriter fWriter;

            File fMobileOrgFile = new File(path);
            FileWriter orgFWriter = new FileWriter(fMobileOrgFile, true);
            fWriter = new BufferedWriter(orgFWriter);
            fWriter.write(content);
            fWriter.close();
    }


    public static File getFile(String fileName, Context context) {
        String storageMode = getStorageMode(context);
        if (storageMode.equals("internal") || storageMode == null) {
            File morgFile = new File("/data/data/com.matburt.mobileorg/files", fileName);
            return morgFile;
        }
        else if (storageMode.equals("sdcard")) {
            File root = Environment.getExternalStorageDirectory();
            File morgDir = new File(root, "mobileorg");
            File morgFile = new File(morgDir, fileName);
            if (!morgFile.exists()) {
                return null;
            }
            return morgFile;
        }
        
        return null;
    }


	public static BufferedWriter getWriteHandle(String filename, Context context) throws IOException {
		String storageMode = getStorageMode(context);
		BufferedWriter writer = null;

		if (storageMode.equals("internal") || storageMode.equals("")) {
			FileOutputStream fs;
			String normalized = filename.replace("/", "_");
			fs = context.openFileOutput(normalized,
					Context.MODE_PRIVATE);
			writer = new BufferedWriter(new OutputStreamWriter(fs));

		} else if (storageMode.equals("sdcard")) {
			File root = Environment.getExternalStorageDirectory();
			File morgDir = new File(root, "mobileorg");
			morgDir.mkdir();
			if (morgDir.canWrite()) {
				File orgFileCard = new File(morgDir, filename);
				FileWriter orgFWriter = new FileWriter(orgFileCard, false);
				writer = new BufferedWriter(orgFWriter);
			}
		}
		
		return writer;
	}

	public static void fetchAndSaveOrgFile(BufferedReader reader, String destPath, Context context)
			throws IOException {
		BufferedWriter writer = getWriteHandle(destPath, context);

		char[] baf = new char[BUFFER_SIZE];
		int actual = 0;

		while (actual != -1) {
			writer.write(baf, 0, actual);
			actual = reader.read(baf, 0, BUFFER_SIZE);
		}
		writer.close();
	}
	
	public static void removeFile(String filePath, Context context, OrgDatabase appdb) {
		
		appdb.removeFile(filePath);
		String storageMode = getStorageMode(context);
		if (storageMode.equals("internal") || storageMode.equals("")) {
			context.deleteFile(filePath);
		} else if (storageMode.equals("sdcard")) {
			File root = Environment.getExternalStorageDirectory();
			File morgDir = new File(root, "mobileorg");
			File morgFile = new File(morgDir, filePath);
			morgFile.delete();
		}
	}
	
	
	private static String getStorageMode(Context context) {
		return PreferenceManager.getDefaultSharedPreferences(context).getString("storageMode", "");
	}
}
