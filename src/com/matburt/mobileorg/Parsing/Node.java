package com.matburt.mobileorg.Parsing;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.regex.Pattern;

public class Node implements Cloneable {

    static int HEADER = 0;
    static int HEADING = 1;
    static int COMMENT = 2;
    static int DATA = 3;

    public ArrayList<Node> subNodes = new ArrayList<Node>();
    public ArrayList<String> tags = new ArrayList<String>();
    HashMap<String, String> properties = new HashMap<String, String>();

    public String nodeName = "";
    public String todo = "";
    public String priority = "";
    public String nodeId = "";
    int nodeType;
    public String nodePayload = "";
    public String nodeTitle = "";
    public String tagString = "";
    public String altNodeTitle = null;
    public Date schedule = null;
    public Date deadline = null;
    public boolean encrypted = false;
    public boolean parsed = false;
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

    public Node findChildNode(String regex) {
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
    
    public void applyEdit(EditNode e) {
    	if (e.editType.equals("todo"))
    		todo = e.newVal;
    	else if (e.editType.equals("priority"))
    		priority = e.newVal;
    	else if (e.editType.equals("heading")) 
    		nodeName = e.newVal;
    	else if (e.editType.equals("body"))
    		nodePayload = e.newVal;
    }
    
    public void applyEdits(ArrayList<EditNode> edits) {
    	if (edits != null) {
    		for (EditNode e : edits) {
    			this.applyEdit(e);
    		}
    	}
    }
}