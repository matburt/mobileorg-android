package com.matburt.mobileorg.Parsing;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.regex.Pattern;

public class Node implements Cloneable {
	Node parent = null;
	public ArrayList<Node> children = new ArrayList<Node>();

	public String name = "";
	public String nodeTitle = "";
	public String altNodeTitle = null;
	public String todo = "";
	public String priority = "";
	public String nodeId = "";
	public String payload = "";

	public Date schedule = null;
	public Date deadline = null;
	public boolean encrypted = false;
	public boolean parsed = false;

	ArrayList<String> tags = new ArrayList<String>();
	String tagString = "";
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

	public static Comparator<Node> comparator = new Comparator<Node>() {
		@Override
		public int compare(Node node1, Node node2) {
			return node1.name.compareToIgnoreCase(node2.name);
		}
	};

	public Node findChildNode(String regex) {
		Pattern findNodePattern = Pattern.compile(regex);
		for (int idx = 0; idx < this.children.size(); idx++) {
			if (findNodePattern.matcher(this.children.get(idx).name).matches()) {
				return this.children.get(idx);
			}
		}
		return null;
	}

	void setTags(ArrayList<String> todoList) {
		this.tags.clear();
		this.tagString = "";
		this.tags.addAll(todoList);
		for (String titem : todoList) {
			this.tagString += titem + " ";
		}
		this.tagString = this.tagString.trim();
	}

	public void applyEdits(ArrayList<EditNode> edits) {
		if (edits != null) {
			for (EditNode e : edits) {
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
					this.payload = e.newVal;
					break;
				}
			}
		}
	}

	public String generateNoteEntry() {
		String noteStr = "* ";
		if (todo != null && !todo.equals("")) {
			noteStr += todo + " ";
		}
		if (priority != null && !priority.equals("")) {
			noteStr += "[#" + priority + "] ";
		}
		noteStr += this.name + "\n";
		noteStr += this.payload + "\n\n";
		return noteStr;
	}

	public ArrayList<String> getTags() {
		return tags;
	}
	

	void setParentNode(Node pnode) {
		this.parent = pnode;
	}

	void addPayload(String npayload) {
		this.payload += npayload + "\n";
	}

	void addChild(Node childNode) {
		this.children.add(childNode);
	}

	void clearChildren() {
		this.children.clear();
	}

	void addProperty(String key, String val) {
		this.properties.put(key, val);
	}

	String getProperty(String key) {
		return this.properties.get(key);
	}

	boolean hasProperty(String key) {
		return this.properties.containsKey(key);
	}
	
	void setTitle(String title) {
		this.nodeTitle = title;
	}
	
	public String getTagString() {
		return this.tagString;
	}
}