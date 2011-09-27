package com.matburt.mobileorg.Parsing;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.StringWriter;

import com.matburt.mobileorg.MobileOrgApplication;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Environment;
import android.preference.PreferenceManager;

public class NodeWriter {
	private SharedPreferences appSettings;
	private Activity appActivity;
	public static final String ORGFILE = "mobileorg.org";

	public NodeWriter(Activity parentActivity) {
		this.appActivity = parentActivity;
		this.appSettings = PreferenceManager
				.getDefaultSharedPreferences(parentActivity.getBaseContext());
	}
	
	public void write(Node node) throws IOException {
		writeNode(node.generateNoteEntry());
	}
	
	/**
	 * Takes a Node and four strings, representing edits to the node.
	 * This function will generate a new edit entry for each value that was 
	 * changed.
	 */
	public void editNode(Node node, String newTitle, String newTodo,
			String newPriority, String newPayload) throws IOException {

		if (!node.name.equals(newTitle)) {
			editNode("heading", node.nodeId, newTitle, node.name, newTitle);
			node.name = newTitle;
		}
		if (newTodo != null && !node.todo.equals(newTodo)) {
			editNode("todo", node.nodeId, newTitle, node.todo, newTodo);
			node.todo = newTodo;
		}
		if (newPriority != null && !node.priority.equals(newPriority)) {
			editNode("priority", node.nodeId, newTitle, node.priority,
					newPriority);
			node.priority = newPriority;
		}
		if (!node.payload.equals(newPayload)) {
			editNode("body", node.nodeId, newTitle, node.payload, newPayload);
			node.payload = newPayload;
		}
	}

	private void editNode(String edittype, String nodeId, String nodeTitle,
			String oldValue, String newValue) throws IOException {
		EditNode editNode = new EditNode(edittype, nodeId, nodeTitle, oldValue,
				newValue);
		
		writeNode(editNode.toString());
	}

	private void writeNode(String message) throws IOException {
		String storageMode = this.appSettings.getString("storageMode", "");
		BufferedWriter writer = new BufferedWriter(new StringWriter());

		if (storageMode.equals("internal") || storageMode.equals("")) {
			FileOutputStream fs = this.appActivity.openFileOutput(ORGFILE,
					Context.MODE_APPEND);
			writer = new BufferedWriter(new OutputStreamWriter(fs));
		} else if (storageMode.equals("sdcard")) {
			File root = Environment.getExternalStorageDirectory();
			File morgDir = new File(root, "mobileorg");
			morgDir.mkdir();

			File orgFileCard = new File(morgDir, ORGFILE);
			FileWriter orgFWriter = new FileWriter(orgFileCard, true);
			writer = new BufferedWriter(orgFWriter);
		}
		writer.write(message);
		
		
		MobileOrgApplication appInst = (MobileOrgApplication) 
				this.appActivity.getApplication();
		appInst.addOrUpdateFile(ORGFILE, "New Notes", "");
		// TODO Parse ORGFILE to update data structures.
		writer.close();
	}
}