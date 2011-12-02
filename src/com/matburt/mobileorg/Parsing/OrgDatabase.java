package com.matburt.mobileorg.Parsing;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteDiskIOException;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.util.Log;

import com.matburt.mobileorg.R;
import com.matburt.mobileorg.Error.ErrorReporter;

public class OrgDatabase {
	private Context appcontext;
	private String lastStorageMode = "";
	private Resources r;
	public SQLiteDatabase appdb;
	public SharedPreferences appSettings;
	public static final String LT = "MobileOrg";

	public OrgDatabase(Context appctxt) {
		this.appcontext = appctxt;
		this.appSettings = PreferenceManager
				.getDefaultSharedPreferences(appctxt);
		this.r = appctxt.getResources();
		this.initialize();
	}

	private void initialize() {
		String storageMode = this.appSettings.getString("storageMode", "");
		if (storageMode.equals("internal") || storageMode.equals(""))
			this.appdb = this.appcontext.openOrCreateDatabase("MobileOrg", 0,
					null);
		else if (storageMode.equals("sdcard")) {
			File sdcard = Environment.getExternalStorageDirectory();
			File morgDir = new File(sdcard, "mobileorg");
			if (!morgDir.exists()) {
				morgDir.mkdir();
			}
			File morgFile = new File(morgDir, "mobileorg.db");
			try {
				this.appdb = SQLiteDatabase
						.openOrCreateDatabase(morgFile, null);
			} catch (Exception e) {
				ErrorReporter.displayError(this.appcontext,
						r.getString(R.string.error_opening_database));
			}
		} else {
			ErrorReporter.displayError(this.appcontext,
					r.getString(R.string.error_opening_database));
			return;
		}
		this.wrapExecSQL("CREATE TABLE IF NOT EXISTS files"
				+ " (file VARCHAR, name VARCHAR," + " checksum VARCHAR)");
		this.wrapExecSQL("CREATE TABLE IF NOT EXISTS todos"
				+ " (tdgroup int, name VARCHAR," + " isdone INT)");
		this.wrapExecSQL("CREATE TABLE IF NOT EXISTS priorities"
				+ " (tdgroup int, name VARCHAR," + " isdone INT)");
		this.lastStorageMode = storageMode;
	}

	private void wrapExecSQL(String sqlText) {
		try {
			this.appdb.execSQL(sqlText);
		} catch (SQLiteDiskIOException e) {
			ErrorReporter.displayError(this.appcontext,
					r.getString(R.string.error_sqlio));
		} catch (Exception e) {
			ErrorReporter.displayError(this.appcontext,
					r.getString(R.string.error_generic_database, e.toString()));
		}
	}

	private Cursor wrapRawQuery(String sqlText) {
		Cursor result = null;
		try {
			result = this.appdb.rawQuery(sqlText, null);
		} catch (SQLiteDiskIOException e) {
			ErrorReporter.displayError(this.appcontext,
					r.getString(R.string.error_sqlio));
		} catch (Exception e) {
			ErrorReporter.displayError(this.appcontext,
					r.getString(R.string.error_generic_database, e.toString()));
		}
		return result;
	}

	private void checkStorageMode() {
		String storageMode = this.appSettings.getString("storageMode", "");
		if (storageMode != this.lastStorageMode) {
			this.close();
			this.initialize();
		}
	}

	public HashMap<String, String> getOrgFiles() {
		this.checkStorageMode();
		HashMap<String, String> allFiles = new HashMap<String, String>();
		Cursor result = this.wrapRawQuery("SELECT file, name FROM files");
		if (result != null) {
			if (result.getCount() > 0) {
				result.moveToFirst();
				do {
					allFiles.put(result.getString(0), result.getString(1));
				} while (result.moveToNext());
			}
			result.close();
		}
		return allFiles;
	}

	public HashMap<String, String> getChecksums() {
		this.checkStorageMode();
		HashMap<String, String> fchecks = new HashMap<String, String>();
		Cursor result = this.wrapRawQuery("SELECT file, checksum FROM files");
		if (result != null) {
			if (result.getCount() > 0) {
				result.moveToFirst();
				do {
					fchecks.put(result.getString(0), result.getString(1));
				} while (result.moveToNext());
			}
			result.close();
		}
		return fchecks;
	}

	public void removeFile(String filename) {
		this.checkStorageMode();
		this.wrapExecSQL("DELETE FROM files " + "WHERE file = '" + filename
				+ "'");
		Log.i(LT, "Finished deleting files " + filename);
	}

	public void addOrUpdateFile(String filename, String name, String checksum) {
		this.checkStorageMode();
		Cursor result = this.wrapRawQuery("SELECT * FROM files "
				+ "WHERE file = '" + filename + "'");
		if (result != null) {
			if (result.getCount() > 0) {
				this.wrapExecSQL("UPDATE files set name = '" + name + "', "
						+ "checksum = '" + checksum + "' where file = '"
						+ filename + "'");
			} else {
				this.wrapExecSQL("INSERT INTO files (file, name, checksum) "
						+ "VALUES ('" + filename + "','" + name + "','"
						+ checksum + "')");
			}
			result.close();
		}
	}

	public void setTodoList(ArrayList<HashMap<String, Boolean>> newList) {
		this.clearTodos();
		int grouping = 0;
		for (HashMap<String, Boolean> entry : newList) {
			for (String key : entry.keySet()) {
				String isDone = "0";
				if (entry.get(key))
					isDone = "1";
				this.wrapExecSQL("INSERT INTO todos (tdgroup, name, isdone) "
						+ "VALUES (" + grouping + "," + "        '" + key
						+ "'," + "        " + isDone + ")");
			}
			grouping++;
		}
	}
	
	public ArrayList<String> getTodods() {
		ArrayList<String> allTodos = new ArrayList<String>();
		Cursor resultCursor = this.wrapRawQuery("SELECT tdgroup, name, isdone "
				+ "FROM todos order by tdgroup");

		if (resultCursor != null && resultCursor.getCount() > 0) {		
			for (resultCursor.moveToFirst(); resultCursor.isAfterLast() == false; 
					resultCursor.moveToNext()) {
				allTodos.add(resultCursor.getString(1));
			}		
			resultCursor.close();
		}
		return allTodos;
	}
	
	public ArrayList<HashMap<String, Integer>> getGroupedTodods() {
		ArrayList<HashMap<String, Integer>> allTodos = new ArrayList<HashMap<String, Integer>>();
		Cursor resultCursor = this.wrapRawQuery("SELECT tdgroup, name, isdone "
				+ "FROM todos order by tdgroup");

		if (resultCursor != null && resultCursor.getCount() > 0) {
			HashMap<String, Integer> grouping = new HashMap<String, Integer>();
			int resultgroup = 0;
			
			for (resultCursor.moveToFirst(); resultCursor.isAfterLast() == false; 
					resultCursor.moveToNext()) {
				// If new result group, create new grouping
				if (resultgroup != resultCursor.getInt(0)) {
					resultgroup = resultCursor.getInt(0);
					allTodos.add(grouping);
					grouping = new HashMap<String, Integer>();		
				}
				// Add item to grouping 
				grouping.put(resultCursor.getString(1), resultCursor.getInt(2));
			}
			
			allTodos.add(grouping);
			resultCursor.close();
		}
		return allTodos;
	}
	
	public ArrayList<String> getPriorities() {
		ArrayList<String> allPriorities = new ArrayList<String>();
		Cursor resultCursor = this
				.wrapRawQuery("SELECT tdgroup, name FROM priorities order by tdgroup");

		if (resultCursor != null && resultCursor.getCount() > 0) {
			for (resultCursor.moveToFirst(); resultCursor.isAfterLast() == false;
					resultCursor.moveToNext()) {
				allPriorities.add(resultCursor.getString(1));
			}
			resultCursor.close();
		}
		return allPriorities;
	}


	public ArrayList<ArrayList<String>> getGroupedPriorities() {
		ArrayList<ArrayList<String>> allPriorities = new ArrayList<ArrayList<String>>();
		Cursor resultCursor = this
				.wrapRawQuery("SELECT tdgroup, name FROM priorities order by tdgroup");

		if (resultCursor != null && resultCursor.getCount() > 0) {
			ArrayList<String> grouping = new ArrayList<String>();
			int resultgroup = 0;

			for (resultCursor.moveToFirst(); resultCursor.isAfterLast() == false;
					resultCursor.moveToNext()) {
				// If new result group, create new grouping
				if (resultgroup != resultCursor.getInt(0)) {
					resultgroup = resultCursor.getInt(0);
					allPriorities.add(grouping);
					grouping = new ArrayList<String>();
				}
				// Add item to grouping
				grouping.add(resultCursor.getString(1));
			}

			allPriorities.add(grouping);
			resultCursor.close();
		}
		return allPriorities;
	}

	public void setPriorityList(ArrayList<ArrayList<String>> newList) {
		this.clearPriorities();
		for (int idx = 0; idx < newList.size(); idx++) {
			for (int jdx = 0; jdx < newList.get(idx).size(); jdx++) {
				this.wrapExecSQL("INSERT INTO priorities (tdgroup, name, isdone) "
						+ "VALUES ("
						+ Integer.toString(idx)
						+ ","
						+ "        '"
						+ newList.get(idx).get(jdx)
						+ "',"
						+ "        0)");
			}
		}
	}

	@SuppressWarnings("unused")
	private void clearData() {
		this.checkStorageMode();
		this.wrapExecSQL("DELETE FROM data");
	}

	private void clearTodos() {
		this.checkStorageMode();
		this.wrapExecSQL("DELETE from todos");
	}

	private void clearPriorities() {
		this.checkStorageMode();
		this.wrapExecSQL("DELETE from priorities");
	}

	public void close() {
		this.appdb.close();
	}
}