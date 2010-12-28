package com.matburt.mobileorg;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Date;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.util.Comparator;

class EditNode {
    public String editType;
    public String nodeId;
    public String title;
    public String oldVal = "";
    public String newVal = "";
}

class Node {

    static int HEADER = 0;
    static int HEADING = 1;
    static int COMMENT = 2;
    static int DATA = 3;

    ArrayList<Node> subNodes = new ArrayList<Node>();
    ArrayList<String> tags = new ArrayList<String>();
    HashMap<String, String> properties = new HashMap<String, String>();

    String nodeName = "";
    String todo = "";
    String priority = "";
    String nodeId = "";
    int nodeType;
    String nodePayload = "";
    String nodeTitle = "";
    String altNodeTitle = null;
    Date schedule = null;
    Date deadline = null;
    boolean encrypted = false;
    boolean parsed = false;
    Node parentNode = null;

    Node(String heading, int ntype) {
        this(heading, ntype, false);
    }

    Node(String heading, int ntype, boolean encrypted) {
        this.nodeName = heading;
        this.nodeType = ntype;
        this.encrypted = encrypted;
    }
	
	public static Comparator<Node> comparator = new Comparator<Node>() {
		@Override
		public int compare(Node node1, Node node2) {
			return node1.nodeName.compareToIgnoreCase(node2.nodeName);
		}
	};

    void setFullTitle(String title) {
        this.nodeTitle = title;
    }

    Node findChildNode(String regex) {
        Pattern findNodePattern = Pattern.compile(regex);
        for (int idx = 0; idx < this.subNodes.size(); idx++) {
            if (findNodePattern.matcher(this.subNodes.get(idx).nodeName).matches()) {
                return this.subNodes.get(idx);
            }
        }
        return null;
    }

    void setParentNode(Node pnode) {
        this.parentNode = pnode;
    }

    void addPayload(String npayload) {
        this.nodePayload += npayload + "\n";
    }

    void addChildNode(Node childNode) {
        this.subNodes.add(childNode);
    }

    void clearNodes() {
        this.subNodes.clear();
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
}