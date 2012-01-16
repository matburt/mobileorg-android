package com.matburt.mobileorg.Parsing;

import java.io.BufferedWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import android.app.Activity;
import android.content.Context;
import android.preference.PreferenceManager;


public class NodeWriter {
	private Activity appActivity;
	private Context context;

	public NodeWriter(Activity parentActivity) {
		this.appActivity = parentActivity;
		this.context = parentActivity.getBaseContext();
	}
	
	public void write(Node node) throws IOException {
		writeNode(node.toString());
	}
	
	/**
	 * Takes a Node and four strings, representing edits to the node.
	 * This function will generate a new edit entry for each value that was 
	 * changed.
	 */
	public void editNode(Node node, String newTitle, String newTodo,
			String newPriority, String newPayload) throws IOException {

		if (!node.name.equals(newTitle)) {
			editNode("heading", node.getNodeId(), newTitle, node.name, newTitle);
			node.name = newTitle;
		}
		if (newTodo != null && !node.todo.equals(newTodo)) {
			editNode("todo", node.getNodeId(), newTitle, node.todo, newTodo);
			node.todo = newTodo;
		}
		if (newPriority != null && !node.priority.equals(newPriority)) {
			editNode("priority", node.getNodeId(), newTitle, node.priority,
					newPriority);
			node.priority = newPriority;
		}
		if (!node.payload.getContent().equals(newPayload)) {
			editNode("body", node.getNodeId(), newTitle, node.payload.getContent(), newPayload);
			node.payload.setContent(newPayload);
		}
	}

	private void editNode(String edittype, String nodeId, String nodeTitle,
			String oldValue, String newValue) throws IOException {
		EditNode editNode = new EditNode(edittype, nodeId, nodeTitle, oldValue,
				newValue);
		
		writeNode(editNode.toString());
	}
	
	private String addTimestamp(String message) {
		SimpleDateFormat sdf = new SimpleDateFormat("[yyyy-MM-dd EEE HH:mm]");		
		return message + sdf.format(new Date()) + "\n";
	}
	
	private void writeNode(String message) throws IOException {
		OrgFile orgfile = new OrgFile(OrgFile.CAPTURE_FILE, context);
		BufferedWriter writer = orgfile.getWriter(true);
		
		boolean addTimestamp = PreferenceManager.getDefaultSharedPreferences(
				this.context).getBoolean("captureWithTimestamp", false);
		if(addTimestamp)
			message = addTimestamp(message);
		
		writer.append(message);
		writer.close();
	
		MobileOrgApplication appInst = (MobileOrgApplication) 
				this.appActivity.getApplication();
		appInst.addOrUpdateFile(OrgFile.CAPTURE_FILE, "New Notes", "");
	}
}