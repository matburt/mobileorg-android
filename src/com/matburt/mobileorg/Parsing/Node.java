package com.matburt.mobileorg.Parsing;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Node implements Cloneable {
	Node parent = null;
	public ArrayList<Node> children = new ArrayList<Node>();

	public String name = "";
	public String nodeTitle = "";
	public String altNodeTitle = null;
	
	public String todo = "";
	public String priority = "";
	private ArrayList<String> tags = new ArrayList<String>();
	
	public String payload = "";

	Date schedule = null;
	Date deadline = null;
	
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
	
    private boolean parseContent(String thisLine) {
    	Pattern propertiesLine = Pattern.compile("^\\s*:[A-Z]+:");
        Matcher propm = propertiesLine.matcher(thisLine);
        Node lastNode = this;
        
        // ID field
        if (thisLine.indexOf(":ID:") != -1) {
            String trimmedLine = thisLine.substring(thisLine.indexOf(":ID:")+4).trim();
            lastNode.addProperty("ID", trimmedLine);
            lastNode.setNodeId(trimmedLine);
            return true;
        }
        // Original ID field
        else if (thisLine.indexOf(":ORIGINAL_ID:") != -1) {
        	String trimmedLine = thisLine.substring(thisLine.indexOf(":ORIGINAL_ID:")+13).trim();
            lastNode.addProperty("ORIGINAL_ID", trimmedLine);
            lastNode.setNodeId(trimmedLine);
            return true;
        }
        // Property
        else if (propm.find()) {
            return true;
        }
        // Scheduled or Deadline
        else if (thisLine.indexOf("DEADLINE:") != -1 ||
                 thisLine.indexOf("SCHEDULED:") != -1) {
        	parseDates(thisLine, lastNode);
            return true;
        }
        
        lastNode.addPayload(thisLine);
        return false;
    }
    
    private void parseDates(String thisLine, Node lastNode) {
    	Pattern deadlineP = Pattern.compile("^.*DEADLINE: <(\\S+ \\S+)( \\S+)?>");
    	Pattern schedP = Pattern.compile("^.*SCHEDULED: <(\\S+ \\S+)( \\S+)?>");

        try {
            Matcher deadlineM = deadlineP.matcher(thisLine);
            SimpleDateFormat dFormatter = new SimpleDateFormat(
                                            "yyyy-MM-dd EEE");
            if (deadlineM.find()) {
                lastNode.deadline = dFormatter.parse(deadlineM.group(1));
            }

            SimpleDateFormat sFormatter = new SimpleDateFormat(
                    "yyyy-MM-dd EEE");
            Matcher schedM = schedP.matcher(thisLine);                            
            if (schedM.find()) {
                lastNode.schedule = sFormatter.parse(schedM.group(1));
            }
        }
        catch (java.text.ParseException e) {
           // Log.e(LT, "Could not parse deadline");
        }
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
	
	void addPayload(String npayload) {
		this.payload += npayload + "\n";
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