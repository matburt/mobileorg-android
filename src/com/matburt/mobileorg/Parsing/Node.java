package com.matburt.mobileorg.Parsing;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// TODO Clean up this mess
public class Node implements Cloneable {
	Node parent = null;
	public ArrayList<Node> children = new ArrayList<Node>();

	public String name = "";
	public String nodeTitle = "";
	public String altNodeTitle = null;

	public String todo = "";
	public String priority = "";
	private ArrayList<String> tags = new ArrayList<String>();

	private StringBuilder payload = new StringBuilder();

	private Date schedule = null;
	private Date deadline = null;

	public boolean encrypted = false;
	public boolean parsed = false;
	private String nodeId = "";

	HashMap<String, String> properties = new HashMap<String, String>();

	public Node() {
		this("", false);
	}

	public Node(String heading) {
		this(heading, false);
	}

	public Node(String heading, boolean encrypted) {
		this.name = heading;
		this.encrypted = encrypted;
	}

	public String getPayload() {
		return this.payload.toString();
	}	
	public void setPayload(String payload) {
		this.payload = new StringBuilder(payload);
	}
	void addPayload(String payload) {
		this.payload.append(payload + "\n");
	}
	
	public Node findChildNode(String regex) {
		Pattern findNodePattern = Pattern.compile(regex);
		for (Node child : children) {
			if (findNodePattern.matcher(child.name).matches()) {
				return child;
			}
		}
		return null;
	}

	public void setTags(ArrayList<String> todoList) {
		this.tags.clear();
		this.tags.addAll(todoList);
	}

	public void addTag(String tag) {
		this.tags.add(tag);
	}

	/**
	 * This applies an edit to the Node, modifying the data structure and
	 * removing the applied edits from the list.
	 */
	public void applyEdits(ArrayList<EditNode> edits) {
		if (edits != null) {
			if (edits.size() == 0)
				return;
		}

		ArrayList<EditNode> nodeEdits = findEdits(edits);

		for (EditNode e : nodeEdits) {
			switch (e.getType()) {
			case TODO:
				this.todo = e.newVal;
				break;
			case PRIORITY:
				this.priority = e.newVal;
				break;
			case NAME:
				this.name = e.newVal;
				break;
			case PAYLOAD:
				this.setPayload(e.newVal);
				break;
			}
		}
	}

	private ArrayList<EditNode> findEdits(ArrayList<EditNode> edits) {
		ArrayList<EditNode> thisEdits = new ArrayList<EditNode>();

		for (EditNode editNode : edits) {
			if (editNode.getNodeId().equals(this.nodeId)) {
				thisEdits.add(editNode);
				edits.remove(editNode);
			}
		}
		return thisEdits;
	}

	/**
	 * Generates string which can be used to write node to file.
	 */
	public String generateNoteEntry() {
		String noteStr = "* ";

		if (!todo.equals(""))
			noteStr += todo + " ";

		if (!priority.equals(""))
			noteStr += "[#" + priority + "] ";

		noteStr += this.name + "\n";

		if (this.payload.length() > 0)
			noteStr += this.payload + "\n";

		noteStr += "\n";
		return noteStr;
	}


	public String getNodeId() {
		if (this.nodeId.length() < 0) {
			String npath = this.name;
			Node pnode = this;
			while ((pnode = pnode.parent) != null) {
				if (pnode.name.length() > 0) {
					npath = pnode.name + "/" + npath;
				}
			}
			npath = "olp:" + npath;
			return npath;
		} else {
			return this.nodeId;
		}
	}

	
	// This function does nothing yet, It's a reminder of how to find properties
//	private void findProperties() {
//		Pattern propertiesLine = Pattern.compile("^\\s*:[A-Z]+:");
//		Matcher propm = propertiesLine.matcher(this.payload);
//		
//		propm.find();
//	}
	
	public String getOriginalId() {
		if (payload.indexOf(":ORIGINAL_ID:") != -1) {
			String trimmedLine = payload.substring(
					payload.indexOf(":ORIGINAL_ID:") + 13).trim();
			this.addProperty("ORIGINAL_ID", trimmedLine);
			this.setNodeId(trimmedLine);
			return trimmedLine;
		} else
			return "";
	}

	public String getId() {
		if (payload.indexOf(":ID:") != -1) {
			String trimmedLine = payload.substring(
					payload.indexOf(":ID:") + 4).trim();
			this.addProperty("ID", trimmedLine);
			this.setNodeId(trimmedLine);
			return trimmedLine;
		} else
			return "";
	}
	
	// TODO Make more efficient
	public Date getDeadline() {
		//payload.indexOf("DEADLINE:");
		
		Pattern deadlineP = Pattern
				.compile("^.*DEADLINE: <(\\S+ \\S+)( \\S+)?>");

		try {
			Matcher deadlineM = deadlineP.matcher(this.payload);
			SimpleDateFormat dFormatter = new SimpleDateFormat("yyyy-MM-dd EEE");
			if (deadlineM.find()) {
				this.deadline = dFormatter.parse(deadlineM.group(1));
			}

		} catch (java.text.ParseException e) {
			// Log.e(LT, "Could not parse deadline");
		}

		return this.deadline;
	}
	
	// TODO Make more efficient
	public Date getScheduled() {
		//this.payload.indexOf("SCHEDULED:");
		
		Pattern schedP = Pattern.compile("^.*SCHEDULED: <(\\S+ \\S+)( \\S+)?>");

		SimpleDateFormat sFormatter = new SimpleDateFormat("yyyy-MM-dd EEE");
		Matcher schedM = schedP.matcher(this.payload);
		if (schedM.find()) {
			try {
				this.schedule = sFormatter.parse(schedM.group(1));
			} catch (ParseException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		return this.schedule;
	}

	public String formatDate() {
		String dateInfo = "";

		// Format Deadline and scheduled
		SimpleDateFormat formatter = new SimpleDateFormat("<yyyy-MM-dd EEE>");
		if (this.deadline != null)
			dateInfo += "DEADLINE: " + formatter.format(this.deadline) + " ";

		if (this.schedule != null)
			dateInfo += "SCHEDULED: " + formatter.format(this.schedule) + " ";

		return dateInfo;
	}

	public void setNodeId(String nodeId) {
		this.nodeId = nodeId;
	}

	public void setParentNode(Node pnode) {
		this.parent = pnode;
	}

	public void addChild(Node childNode) {
		this.children.add(childNode);
		childNode.parent = this;
	}

	public ArrayList<String> getTags() {
		return tags;
	}

	void addProperty(String key, String val) {
		this.properties.put(key, val);
	}

	public void setTitle(String title) {
		this.nodeTitle = title;
	}

	public String getTagString() {
		StringBuilder tagString = new StringBuilder();
		for (String titem : this.tags) {
			tagString.append(titem + " ");
		}
		return tagString.toString().trim();
	}

	public boolean hasChildren() {
		if (children.isEmpty())
			return false;
		else
			return true;
	}

	public boolean isSimple() {
		if (this.todo.equals("") && this.priority.equals(""))
			return true;
		else
			return false;

	}

	public static Comparator<Node> comparator = new Comparator<Node>() {
		@Override
		public int compare(Node node1, Node node2) {
			return node1.name.compareToIgnoreCase(node2.name);
		}
	};
}