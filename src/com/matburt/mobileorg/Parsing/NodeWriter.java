package com.matburt.mobileorg.Parsing;

import java.io.BufferedWriter;
import java.io.IOException;

import android.app.Activity;
import android.content.Context;

import com.matburt.mobileorg.MobileOrgApplication;

public class NodeWriter {
	private Activity appActivity;
	private Context context;
	public static final String ORGFILE = "mobileorg.org";

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
	
	private void writeNode(String message) throws IOException {
		OrgFile orgfile = new OrgFile(ORGFILE, context);
		BufferedWriter writer = orgfile.getWriter();
		writer.write(message);
	
		MobileOrgApplication appInst = (MobileOrgApplication) 
				this.appActivity.getApplication();
		appInst.addOrUpdateFile(ORGFILE, "New Notes", "");
		// TODO Parse ORGFILE to update data structures.
		writer.close();
	}
}