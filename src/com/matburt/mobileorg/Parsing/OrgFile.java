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

	private Context context;
	private String fileName;

	public OrgFile(String file, Context context) {
		this.context = context;
		this.fileName = file;
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

	private BufferedReader getReader() throws FileNotFoundException {
		String storageMode = getStorageMode();
		BufferedReader reader = null;

		try {
			if (storageMode.equals("internal") || storageMode.equals("")) {
				FileInputStream fs;
				fs = context.openFileInput(fileName);
				reader = new BufferedReader(new InputStreamReader(fs));

			} else if (storageMode.equals("sdcard")) {
				File root = Environment.getExternalStorageDirectory();
				File morgDir = new File(root, "mobileorg");
				File morgFile = new File(morgDir, fileName);
				if (!morgFile.exists()) {
					return null;
				}
				reader = new BufferedReader(new FileReader(morgFile));
			}
		} catch (FileNotFoundException e) {
			return null;
		}

		return reader;
	}
	
	public BufferedWriter getWriter() throws IOException {
		String storageMode = getStorageMode();
		BufferedWriter writer = null;

		if (storageMode.equals("internal") || storageMode.equals("")) {
			FileOutputStream fs;
			String normalized = fileName.replace("/", "_");
			fs = context.openFileOutput(normalized, Context.MODE_PRIVATE);
			writer = new BufferedWriter(new OutputStreamWriter(fs));

		} else if (storageMode.equals("sdcard")) {
			File root = Environment.getExternalStorageDirectory();
			File morgDir = new File(root, "mobileorg");
			morgDir.mkdir();
			if (morgDir.canWrite()) {
				File orgFileCard = new File(morgDir, fileName);
				FileWriter orgFWriter = new FileWriter(orgFileCard, false);
				writer = new BufferedWriter(orgFWriter);
			}
		}

		return writer;
	}

	public File getFile() {
		String storageMode = getStorageMode();
		if (storageMode.equals("internal") || storageMode == null) {
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

		char[] baf = new char[BUFFER_SIZE];
		int actual = 0;

		while (actual != -1) {
			writer.write(baf, 0, actual);
			actual = reader.read(baf, 0, BUFFER_SIZE);
		}
		writer.close();
	}

	public void remove(OrgDatabase appdb) {
		appdb.removeFile(fileName);
		String storageMode = getStorageMode();
		if (storageMode.equals("internal") || storageMode.equals("")) {
			context.deleteFile(fileName);
		} else if (storageMode.equals("sdcard")) {
			File root = Environment.getExternalStorageDirectory();
			File morgDir = new File(root, "mobileorg");
			File morgFile = new File(morgDir, fileName);
			morgFile.delete();
		}
	}

	private String getStorageMode() {
		return PreferenceManager.getDefaultSharedPreferences(context)
				.getString("storageMode", "");
	}
}
