package com.matburt.mobileorg.Parsing;

import java.text.SimpleDateFormat;
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
	private String nodeId = "";
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
		for (Node child: children) {
			if (findNodePattern.matcher(child.name).matches()) {
				return child;
			}
		}
		return null;
	}

	public void setTags(ArrayList<String> todoList) {
		this.tags.clear();
		this.tagString = "";
		this.tags.addAll(todoList);
		for (String titem : todoList) {
			this.tagString += titem + " ";
		}
		this.tagString = this.tagString.trim();
	}
	
	/**
	 * This applies an edit to the Node, modifying the data structure and
	 * removing the applied edits from the list.
	 */
	public void applyEdits(ArrayList<EditNode> edits) {
		if(edits != null) {
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
				this.payload = e.newVal;
				break;
			}
		}
	}
    
	private ArrayList<EditNode> findEdits(ArrayList<EditNode> edits) {
		ArrayList<EditNode> thisEdits = new ArrayList<EditNode>();
		
		for (EditNode editNode: edits) {		
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
	
	public String getNodeId() {
		if(this.nodeId.length() < 0) {
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

	public void setNodeId(String nodeId) {
		this.nodeId = nodeId;
	}

	void setParentNode(Node pnode) {
		this.parent = pnode;
	}
	
	public void addChild(Node childNode) {
		this.children.add(childNode);
		childNode.parent = this;
	}

	void clearChildren() {
		this.children.clear();
	}

	public ArrayList<String> getTags() {
		return tags;
	}
	
	void addPayload(String npayload) {
		this.payload += npayload + "\n";
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
	
	public boolean hasChildren() {
		if(children.size() > 0)
			return true;
		else
			return false;
	}
	
	public boolean isSimple() {
		if (this.todo.equals("") && this.priority.equals(""))
			return true;
		else
			return false;
				
	}
}