package com.matburt.mobileorg.util;

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
import android.content.SharedPreferences;
import android.os.Environment;
import android.preference.PreferenceManager;

public class FileUtils {

	public static final String CAPTURE_FILE = "mobileorg.org";
	public static final String CAPTURE_FILE_ALIAS = "Captures";

	private Context context;
	private String fileName;

	public FileUtils(String file, Context context) {
		this.context = context;
		this.fileName = file.replace("/", "_");
	}

	public String read() throws IOException {
		return read(getReader());
	}

	public static String read(BufferedReader reader) throws IOException {
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

	public void write(String filePath, String content) throws IOException {
		File file = new File(filePath);
		BufferedWriter writer = new BufferedWriter(new FileWriter(file, true));
		writer.write(content);
		writer.close();
	}

	public BufferedReader getReader() {
		String storageMode = getStorageMode();
		String synchMode = getSynchMode();
		BufferedReader reader = null;

		try {
			if (storageMode.equals("sdcard") || synchMode.equals("sdcard")) {
				File root = Environment.getExternalStorageDirectory();
				File morgDir = new File(root, "mobileorg");
				File morgFile = new File(morgDir, fileName);
				if (!morgFile.exists()) {
					return null;
				}
				FileReader freader = new FileReader(morgFile);
				reader = new BufferedReader(freader);
			} else if (storageMode.equals("internal") || storageMode.equals("")) {
				String dirActual = this.fileName;

				FileInputStream fs;
				fs = context.openFileInput(dirActual);
				reader = new BufferedReader(new InputStreamReader(fs));
			}
		} catch (FileNotFoundException e) {
			return null;
		}

		return reader;
	}
	
	public BufferedWriter getWriter() throws IOException {
		return getWriter(false);
	}
	
	public BufferedWriter getWriter(boolean append) throws IOException {
		String storageMode = getStorageMode();
		BufferedWriter writer = null;

		if (storageMode.equals("internal") || storageMode.equals("")) {
			FileOutputStream fs;
			String normalized = fileName.replace("/", "_");
			if(append)
				fs = context.openFileOutput(normalized, Context.MODE_APPEND);
			else
				fs = context.openFileOutput(normalized, Context.MODE_PRIVATE);
			writer = new BufferedWriter(new OutputStreamWriter(fs));

		} else if (storageMode.equals("sdcard")) {
			File root = Environment.getExternalStorageDirectory();
			File morgDir = new File(root, "mobileorg");
			morgDir.mkdir();
			if (morgDir.canWrite()) {
				File orgFileCard = new File(morgDir, fileName);
				FileWriter orgFWriter = new FileWriter(orgFileCard, append);
				writer = new BufferedWriter(orgFWriter);
			}
		}

		return writer;
	}

	public File getFile() {
		String storageMode = getStorageMode();
		if (storageMode.equals("internal") || storageMode.equals("")) {
			File morgFile = new File("/data/data/com.matburt.mobileorg/files",
					fileName);
			return morgFile;
		} else if (storageMode.equals("sdcard")) {
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

	/**
	 * Read everything from the given reader and write to it {@link #fileName}
	 */
	public void fetch(BufferedReader reader) throws IOException {
		BufferedWriter writer = getWriter();

		final int BUFFER_SIZE = 23 * 1024;
		char[] baf = new char[BUFFER_SIZE];
		int actual = 0;

		while (actual != -1) {
			writer.write(baf, 0, actual);
			actual = reader.read(baf, 0, BUFFER_SIZE);
		}
		writer.close();
	}

	public void remove() {
		String storageMode = getStorageMode();
		if (storageMode.equals("internal") || storageMode.equals("")) {
			context.deleteFile(this.fileName);
		} else if (storageMode.equals("sdcard")) {
			File root = Environment.getExternalStorageDirectory();
			File morgDir = new File(root, "mobileorg");
			File morgFile = new File(morgDir, this.fileName);
			morgFile.delete();
		}
	}

	private String getStorageMode() {
		return PreferenceManager.getDefaultSharedPreferences(this.context)
				.getString("storageMode", "");
	}
	
	private String getSynchMode() {
		return PreferenceManager.getDefaultSharedPreferences(this.context)
				.getString("storageMode", "");		
	}
	
	public String getBasePath() {
		SharedPreferences appSettings = PreferenceManager
				.getDefaultSharedPreferences(this.context);
		String orgBasePath = "";

		if (getStorageMode().equals("sdcard")) {
			String indexFile = appSettings.getString("indexFilePath", "");
			File fIndexFile = new File(indexFile);
			orgBasePath = fIndexFile.getParent() + "/";
		} else {
			orgBasePath = Environment.getExternalStorageDirectory()
					.getAbsolutePath() + "/mobileorg/";
		}

		return orgBasePath;
	}
	
	// Used by encryption
	public byte[] getRawFileData() {
		try {
			File file = getFile();
			FileInputStream is = new FileInputStream(file);
			byte[] buffer = new byte[(int) file.length()];
			int offset = 0;
			int numRead = 0;
			while (offset < buffer.length
					&& (numRead = is.read(buffer, offset, buffer.length
							- offset)) >= 0) {
				offset += numRead;
			}
			is.close();
			if (offset < buffer.length) {
				throw new IOException("Could not completely read file "
						+ file.getName());
			}
			return buffer;
		} catch (IOException e) {
			return null;
		}
	}
}
