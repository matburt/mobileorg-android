package com.matburt.mobileorg.Parsing;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.regex.Pattern;

public class Node implements Cloneable {
	private Node parent = null;
	private ArrayList<Node> children = new ArrayList<Node>();

	public String name = "";
	public String todo = "";
	public String priority = "";
	private ArrayList<String> tags = new ArrayList<String>();
	public NodePayload payload = new NodePayload();

	public boolean encrypted = false;
	public boolean parsed = true;

	public Node(String name, Node parent) {
		this.name = name;
		parent.addChild(this);
	}
	public Node(String name) {
		this.name = name;
	}


	public void addChild(Node childNode) {
		this.children.add(childNode);
		childNode.parent = this;
	}
	
	public boolean removeChild(String childName) {
		Node child = getChild(childName);
		
		if(child != null) {
			this.children.remove(child);
			return true;
		} else
			return false;
	}
	
	public boolean hasChildren() {
		if (children.isEmpty())
			return false;
		else
			return true;
	}
	
	public Node getChild(String childName) {
		for(Node child: this.children) {
			if(child.name.equals(childName))
				return child;
		}
		
		return null;
	}
	
	public ArrayList<Node> getChildren() {
		return this.children;
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

	public void sortChildren() {
		Collections.sort(this.children, Node.comparator);
	}
	
	public static Comparator<Node> comparator = new Comparator<Node>() {
		@Override
		public int compare(Node node1, Node node2) {
			return node1.name.compareToIgnoreCase(node2.name);
		}
	};
	

	public void setTags(ArrayList<String> todoList) {
		this.tags.clear();
		this.tags.addAll(todoList);
	}

	public ArrayList<String> getTags() {
		return this.tags;
	}

	public void addTag(String tag) {
		this.tags.add(tag);
	}
	
	public String getTagString() {
		StringBuilder tagString = new StringBuilder();
		for (String titem : this.tags) {
			tagString.append(titem + " ");
		}
		return tagString.toString().trim();
	}

	
	/**
	 * This applies an edit to the Node, modifying the data structure and
	 * removing the applied edits from the list.
	 */
	public void applyEdits(ArrayList<EditNode> edits) {
		if (edits == null || edits.size() == 0)
				return;

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
				this.payload.setContent(e.newVal);
				break;
			}
		}
	}

	private ArrayList<EditNode> findEdits(ArrayList<EditNode> edits) {
		ArrayList<EditNode> thisEdits = new ArrayList<EditNode>();

		for (EditNode editNode : edits) {
			if (editNode.getNodeId().equals(this.payload.getNodeId())) {
				thisEdits.add(editNode);
				edits.remove(editNode);
			}
		}
		return thisEdits;
	}

	public String getNodeId() {
		if (this.payload.getNodeId() == null) {
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
			return this.payload.getNodeId();
		}
	}
	
	public boolean isSimple() {
		if (this.todo.equals("") && this.priority.equals(""))
			return true;
		else
			return false;

	}

	public String toString() {
		String noteStr = "* ";

		if (!todo.equals(""))
			noteStr += todo + " ";

		if (!priority.equals(""))
			noteStr += "[#" + priority + "] ";

		noteStr += this.name + "\n";

		if (this.payload.getContent().length() > 0)
			noteStr += this.payload + "\n";

		noteStr += "\n";
		return noteStr;
	}
}
