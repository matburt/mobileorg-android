package com.matburt.mobileorg.Parsing;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.EmptyStackException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Stack;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.content.Context;
import android.util.Log;


public class OrgFileParser {

    private Context context;
    private MobileOrgApplication appInst;
    private static final String LT = "MobileOrg";

    private ArrayList<HashMap<String, Integer>> todos = null;

	public OrgFileParser(Context context, MobileOrgApplication appInst) {
		this.appInst = appInst;
		this.context = context;
	}

	/**
	 * This function will return a Node that contains an entry for each org
	 * file. All files are added with {@link Node.parsed} equal to false,
	 * indicating that it should be parsed before accessing.
	 */
	public Node prepareRootNode() {
		Node rootNode = new Node("");
		
		HashMap<String, String> orgPathFileMap = this.appInst.getOrgFiles();

		for (String key : orgPathFileMap.keySet()) {
			Node fileNode = new Node(key, rootNode);
			// fileNode.altNodeTitle = orgPathFileMap.get(key);
			fileNode.parsed = false;

			if (key.endsWith(".gpg") || key.endsWith(".pgp")
					|| key.endsWith(".enc"))
				fileNode.encrypted = true;
		}

		rootNode.sortChildren();
		return rootNode;
	}

	/**
	 * This causes the given filename to be parsed and the resulting node will
	 * be returned. An optional root node can be given, resulting in it's entry
	 * of the node being updated.
	 */
	public Node parseFile(String filename, Node rootNode) {
		OrgFile orgfile = new OrgFile(filename, context);
		BufferedReader breader = orgfile.getReader();

		if (breader == null)
			return null;

		Node node;
		if (rootNode != null) {
			node = rootNode.getChild(filename);

			if (node == null)
				node = new Node(filename, rootNode);
			else
				node.getChildren().clear();
		} else
			node = new Node(filename);

		parse(node, breader);
		node.parsed = true;

		try { breader.close(); } catch (IOException e) {}
		return node;
	}

	private static Pattern titlePattern = null;
	private Stack<Node> nodeStack;
	private Stack<Integer> starStack;
	private Stack<Long> parentIdStack;

	Pattern editTitlePattern = Pattern
			.compile("F\\((edit:.*?)\\) \\[\\[(.*?)\\]\\[(.*?)\\]\\]");

	public void parse(Node fileNode, BufferedReader breader) {
		this.todos = this.appInst.getGroupedTodods();

		this.nodeStack = new Stack<Node>();
		this.starStack = new Stack<Integer>();
		this.parentIdStack = new Stack<Long>();
		
		this.nodeStack.push(fileNode);
		this.starStack.push(0);
		this.parentIdStack.push(new Long(0));

		try {
			String currentLine;
			int lineLength;

			while ((currentLine = breader.readLine()) != null) {

				lineLength = currentLine.length();
				
				if (lineLength == 0)
					continue;

				// Find title fields and set title for file node
				if (currentLine.charAt(0) == '#') {
					if (currentLine.indexOf("#+TITLE:") != -1) {
//						fileNode.altNodeTitle = currentLine.substring(
//								currentLine.indexOf("#+TITLE:") + 8).trim();
					}
				}

				int numstars = numberOfStars(currentLine, lineLength);
				if (numstars > 0) {
					parseHeading(currentLine, numstars);
				} else {
					nodeStack.peek().payload.add(currentLine);
				}
			}

		} catch (IOException e) {}
		
		if(fileNode.name.equals(OrgFile.CAPTURE_FILE)) {
			deleteEditNodes(fileNode);
		}
	}

	private void deleteEditNodes(Node fileNode) {
		for(Iterator<Node> it = fileNode.getChildren().iterator(); it.hasNext();) {
			final Node child = it.next();
			if(child.name.startsWith("F(edit"))
				it.remove();
		}
	}
	
	private static int numberOfStars(String thisLine, int lineLength) {
		int numstars = 0;

		for (int idx = 0; idx < lineLength; idx++) {
			if (thisLine.charAt(idx) != '*')
				break;

			numstars++;
		}

		if (numstars >= lineLength || thisLine.charAt(numstars) != ' ')
			numstars = 0;

		return numstars;
	}
    
	private void parseHeading(String thisLine, int numstars) {

		if (numstars == starStack.peek()) {
			//nodeStack.pop();
			starStack.pop();
			parentIdStack.pop();
		} else if (numstars < starStack.peek()) {
			while (numstars <= starStack.peek()) {
				//nodeStack.pop();
				starStack.pop();
				parentIdStack.pop();
			}
		}
        
		long newId = parseLineIntoNode(thisLine, numstars);

        try {
           // nodeStack.peek().addChild(newNode);
        } catch (EmptyStackException e) {}
        
        //nodeStack.push(newNode);
        starStack.push(numstars);
        this.parentIdStack.push(newId);
        
    }
    
    private long parseLineIntoNode (String thisLine, int numstars) {
    	String heading = stripHeading(thisLine, numstars).trim();
    	
        Node newNode = new Node("");
    	
    	Pattern pattern = prepareTitlePattern();
    	Matcher m = pattern.matcher(heading);
		if (m.find()) {
			if (m.group(1) != null) {
				String tempTodo = m.group(1).trim();
				if (tempTodo.length() > 0 && isValidTodo(tempTodo)) {
					newNode.todo = tempTodo;
				} else {
					newNode.name = tempTodo + " ";
				}
			}
			if (m.group(2) != null) {
				newNode.priority = m.group(2);
				newNode.priority = newNode.priority.replace("#", "");
				newNode.priority = newNode.priority.replace("[", "");
				newNode.priority = newNode.priority.replace("]", "");
			}
			newNode.name += m.group(3);
			String tempTags = m.group(4);
			if (tempTags != null) {
				for (String tag : tempTags.split(":")) {
					newNode.addTag(tag);
				}
			}
		} else {
			Log.w(LT, "Title not matched: " + heading);
			newNode.name = heading;
		}
    	
        Long nodeId = this.appInst.getDB().addNode(this.parentIdStack.peek(), newNode.name, newNode.todo, newNode.priority, null);
    	return nodeId;
    }

    final static Pattern titlePattern2 = Pattern.compile("<before.*</before>|<after.*</after>");
    private String stripHeading(String line, int numstars) {
        String heading = line.substring(numstars+1);

        Matcher titleMatcher = titlePattern2.matcher(heading);
        String newHeading = "";
        if (titleMatcher.find()) {
            newHeading += heading.substring(0, titleMatcher.start());
            newHeading += heading.substring(titleMatcher.end(), heading.length());
        }
        else
            newHeading = heading;

        // Hack to strip out * from habits
        if(this.nodeStack.get(0).name.equals("agendas.org"))
        	newHeading = newHeading.replaceAll("\\*", "");
        
        return newHeading;
    }
 
    private Pattern prepareTitlePattern() {
    	if (OrgFileParser.titlePattern == null) {
    		StringBuffer pattern = new StringBuffer();
    		pattern.append("^(?:([A-Z]{2,}:?\\s+");
    		pattern.append(")\\s*)?");
    		pattern.append("(\\[\\#.*\\])?(.*?)");
    		pattern.append("\\s*(?::([^\\s]+):)?");
    		// TODO Line beneath should filter out habit stuff from agenda.org, but it doesn't seem to filter *
    		pattern.append("(\\s*[\\*!])?$");
    		OrgFileParser.titlePattern = Pattern.compile(pattern.toString());
    	}
		return OrgFileParser.titlePattern;
    }
	
	private boolean isValidTodo(String todo) {
		for(HashMap<String, Integer> aTodo : this.todos) {
			if(aTodo.containsKey(todo)) return true;
		}
		return false;
	}


    public ArrayList<EditNode> parseEdits() {
        Pattern editTitlePattern = Pattern.compile("F\\((edit:.*?)\\) \\[\\[(.*?)\\]\\[(.*?)\\]\\]");
        Pattern createTitlePattern = Pattern.compile("^\\*\\s+(.*)");
 
        ArrayList<EditNode> edits = new ArrayList<EditNode>();
        OrgFile orgfile = new OrgFile(OrgFile.CAPTURE_FILE, context);
        BufferedReader breader = orgfile.getReader();
        if (breader == null)
            return edits;

        String thisLine;
        boolean awaitingOldVal = false;
        boolean awaitingNewVal = false;
        EditNode thisNode = null;

        try {
            while ((thisLine = breader.readLine()) != null) {
                Matcher editm = editTitlePattern.matcher(thisLine);
                Matcher createm = createTitlePattern.matcher(thisLine);
                if (editm.find()) {
                    thisNode = new EditNode();
                    if (editm.group(1) != null)
                        thisNode.editType = editm.group(1).split(":")[1];
                    if (editm.group(2) != null)
                        thisNode.nodeId = editm.group(2).split(":")[1];
                    if (editm.group(3) == null)
                        thisNode.title = editm.group(3);
                }
                else if (createm.find()) {
                }
                else {
                    if (thisLine.indexOf("** Old value") != -1) {
                        awaitingOldVal = true;
                        continue;
                    }
                    else if (thisLine.indexOf("** New value") != -1) {
                        awaitingOldVal = false;
                        awaitingNewVal = true;
                        continue;
                    }
                    else if (thisLine.indexOf("** End of edit") != -1) {
                        awaitingNewVal = false;
                        edits.add(thisNode);
                    }

                    if (awaitingOldVal) {
                        thisNode.oldVal += thisLine;
                    }
                    if (awaitingNewVal) {
                        thisNode.newVal += thisLine;
                    }
                }
            }
        }
        catch (java.io.IOException e) {
            return null;
        }
        return edits;
    }
}
