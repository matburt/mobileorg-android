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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.content.Context;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.text.TextUtils;

public class OrgFile {

	final private static int BUFFER_SIZE = 23 * 1024;



	public static BufferedReader getReadHandle(String filename, Context context) throws FileNotFoundException {
		String storageMode = PreferenceManager.getDefaultSharedPreferences(context).getString("storageMode", "");
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
	
	private static String getStorageMode(Context context) {
		return PreferenceManager.getDefaultSharedPreferences(context).getString("storageMode", "");
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

	public static void fetchAndSaveOrgFile(String orgPath, String destPath, Context context)
			throws IOException {
		BufferedReader reader = fetchOrgFile(orgPath);
		BufferedWriter writer = getWriteHandle(destPath, context);

		char[] baf = new char[BUFFER_SIZE];
		int actual = 0;

		while (actual != -1) {
			writer.write(baf, 0, actual);
			actual = reader.read(baf, 0, BUFFER_SIZE);
		}
		writer.close();
	}
	
	public static BufferedReader fetchOrgFile(String orgPath) throws IOException {
		return null;
	}

	public static String fetchOrgFileString(String orgPath) throws IOException {
		BufferedReader reader = fetchOrgFile(orgPath);
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

	
	
	public static HashMap<String, String> getOrgFilesFromMaster(String master) {
		Pattern getOrgFiles = Pattern.compile("\\[file:(.*?)\\]\\[(.*?)\\]\\]");
		Matcher m = getOrgFiles.matcher(master);
		HashMap<String, String> allOrgFiles = new HashMap<String, String>();
		while (m.find()) {
			allOrgFiles.put(m.group(2), m.group(1));
		}

		return allOrgFiles;
	}

	public static HashMap<String, String> getChecksums(String master) {
		HashMap<String, String> chksums = new HashMap<String, String>();
		for (String eachLine : master.split("[\\n\\r]+")) {
			if (TextUtils.isEmpty(eachLine))
				continue;
			String[] chksTuple = eachLine.split("\\s+");
			chksums.put(chksTuple[1], chksTuple[0]);
		}
		return chksums;
	}

	public static ArrayList<HashMap<String, Boolean>> getTodos(String master) {
		Pattern getTodos = Pattern
				.compile("#\\+TODO:\\s+([\\s\\w-]*)(\\| ([\\s\\w-]*))*");
		Matcher m = getTodos.matcher(master);
		ArrayList<HashMap<String, Boolean>> todoList = new ArrayList<HashMap<String, Boolean>>();
		while (m.find()) {
			String lastTodo = "";
			HashMap<String, Boolean> holding = new HashMap<String, Boolean>();
			Boolean isDone = false;
			for (int idx = 1; idx <= m.groupCount(); idx++) {
				if (m.group(idx) != null && m.group(idx).length() > 0) {
					if (m.group(idx).indexOf("|") != -1) {
						isDone = true;
						continue;
					}
					String[] grouping = m.group(idx).split("\\s+");
					for (int jdx = 0; jdx < grouping.length; jdx++) {
						lastTodo = grouping[jdx].trim();
						holding.put(grouping[jdx].trim(), isDone);
					}
				}
			}
			if (!isDone) {
				holding.put(lastTodo, true);
			}
			todoList.add(holding);
		}
		return todoList;
	}

	public static ArrayList<ArrayList<String>> getPriorities(String master) {
		Pattern getPriorities = Pattern
				.compile("#\\+ALLPRIORITIES:\\s+([A-Z\\s]*)");
		Matcher t = getPriorities.matcher(master);
		
		ArrayList<ArrayList<String>> priorityList = new ArrayList<ArrayList<String>>();
		
		while (t.find()) {
			ArrayList<String> holding = new ArrayList<String>();
			if (t.group(1) != null && t.group(1).length() > 0) {
				String[] grouping = t.group(1).split("\\s+");
				for (int jdx = 0; jdx < grouping.length; jdx++) {
					holding.add(grouping[jdx].trim());
				}
			}
			priorityList.add(holding);
		}
		return priorityList;
	}
}
