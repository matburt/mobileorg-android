package com.matburt.mobileorg.Parsing;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EmptyStackException;
import java.util.HashMap;
import java.util.Stack;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.matburt.mobileorg.MobileOrgApplication;
import com.matburt.mobileorg.Error.ErrorReporter;


public class OrgFileParser {

    public Node rootNode = new Node("");
    private HashMap<String, String> orgPathFileMap;
    private Context context;
    private OrgDatabase appdb;
	private ArrayList<HashMap<String, Integer>> todos = null;
    private static final String LT = "MobileOrg";

	public OrgFileParser(Context context, MobileOrgApplication appInst) {
		this.appdb = new OrgDatabase(context);
		this.context = context;
		
		HashMap<String, String> orgPathFileMap = this.appdb.getOrgFiles();
		if (orgPathFileMap.isEmpty())
			return;
		this.orgPathFileMap = orgPathFileMap;
	}

	public void runParser(SharedPreferences appSettings,
			MobileOrgApplication appInst) {
		parseAllFiles();
		appInst.rootNode = rootNode;
		appInst.edits = parseEdits();
		Collections.sort(appInst.rootNode.children, Node.comparator);
	}
	
    public void parseAllFiles() {
        Stack<Node> nodeStack = new Stack<Node>();
        nodeStack.push(this.rootNode);

        for (String key : this.orgPathFileMap.keySet()) {
            String altName = this.orgPathFileMap.get(key);
            Log.d(LT, "Parsing: " + key);
            //if file is encrypted just add a placeholder node to be parsed later
            if(key.endsWith(".gpg") ||
               key.endsWith(".pgp") ||
               key.endsWith(".enc"))
            {
                Node nnode = new Node(key, true);
                nnode.altNodeTitle = altName;
                nnode.setParentNode(nodeStack.peek());
                nodeStack.peek().addChild(nnode);
                continue;
            }

            Node fileNode = new Node(key, false);
            fileNode.setParentNode(nodeStack.peek());
            fileNode.altNodeTitle = altName;
            nodeStack.peek().addChild(fileNode);
            nodeStack.push(fileNode);
            
        	OrgFile orgfile = new OrgFile(fileNode.name, context);
            BufferedReader breader = orgfile.getReader();
            
            if(breader == null) {
            	ErrorReporter.displayError(context, new Exception("breader == null!"));
            }
            
            parseFile(fileNode, breader);
            try {
				breader.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
            
            nodeStack.pop();
        }
    }

    private Pattern titlePattern = null;
    private Stack<Node> nodeStack = new Stack<Node>();
    private Stack<Integer> starStack = new Stack<Integer>();

    private static int numberOfStars(String thisLine) {
        int numstars = 0;
    	
        for (int idx = 0; idx < thisLine.length(); idx++) {
            if (thisLine.charAt(idx) != '*') {
                break;
            }
            numstars++;
        }

        if (numstars >= thisLine.length() || thisLine.charAt(numstars) != ' ')
            numstars = 0;
        
        return numstars;
    }
    
	
	Pattern editTitlePattern = Pattern
			.compile("F\\((edit:.*?)\\) \\[\\[(.*?)\\]\\[(.*?)\\]\\]");

	public void parseFile(Node fileNode, BufferedReader breader) {
		this.todos = appdb.getGroupedTodods();

		nodeStack.push(fileNode);
		starStack.push(0);

		try {
			String thisLine;

			while ((thisLine = breader.readLine()) != null) {

				// Skip edit fields
				Matcher editm = editTitlePattern.matcher(thisLine);
				if (thisLine.length() < 1 || editm.find())
					continue;

				// Find title fields and set title for file node
				if (thisLine.charAt(0) == '#') {
					if (thisLine.indexOf("#+TITLE:") != -1) {
						fileNode.altNodeTitle = thisLine.substring(
								thisLine.indexOf("#+TITLE:") + 8).trim();
					}
				}

				int numstars = numberOfStars(thisLine);
				if (numstars > 0) {
					parseHeading(thisLine, numstars);
				} else {
					nodeStack.peek().addPayload(thisLine);
				}
			}

			while (starStack.peek() > 0) {
				nodeStack.pop();
				starStack.pop();
			}

			fileNode.parsed = true;
		} catch (IOException e) {
			Log.e(LT, "IO Exception on readerline: " + e.getMessage());
		}
	}

    private void parseHeading(String thisLine, int numstars) {
        String title = thisLine.substring(numstars+1);
        
        Node newNode = parseTitle(this.stripTitle(title));

        if (numstars > starStack.peek()) {
            try {
                Node lastNode = nodeStack.peek();
                newNode.setParentNode(lastNode);
                lastNode.addChild(newNode);
            } catch (EmptyStackException e) {
            }
            nodeStack.push(newNode);
            starStack.push(numstars);
        }
        else if (numstars == starStack.peek()) {
            nodeStack.pop();
            starStack.pop();
            nodeStack.peek().addChild(newNode);
            newNode.setParentNode(nodeStack.peek());
            nodeStack.push(newNode);
            starStack.push(numstars);
        }
        else if (numstars < starStack.peek()) {
            while (numstars <= starStack.peek()) {
                nodeStack.pop();
                starStack.pop();
            }

            Node lastNode = nodeStack.peek();
            newNode.setParentNode(lastNode);
            lastNode.addChild(newNode);
            nodeStack.push(newNode);
            starStack.push(numstars);
        }
    }
    
    private Pattern prepareTitlePattern() {
    	if (this.titlePattern == null) {
    		StringBuffer pattern = new StringBuffer();
    		pattern.append("^(?:([A-Z]{2,}:?\\s+");
    		pattern.append(")\\s*)?");
    		pattern.append("(\\[\\#.*\\])?(.*?)");
    		pattern.append("\\s*(?::([^\\s]+):)?$");
    		this.titlePattern = Pattern.compile(pattern.toString());
    	}
		return this.titlePattern;
    }
	
	private boolean isValidTodo(String todo) {
		for(HashMap<String, Integer> aTodo : this.todos) {
			if(aTodo.containsKey(todo)) return true;
		}
		return false;
	}

    private Node parseTitle (String orgTitle) {
    	String title = orgTitle.trim();
    	
        Node newNode = new Node("");
    	
    	Pattern pattern = prepareTitlePattern();
    	Matcher m = pattern.matcher(title);
    	if (m.find()) {
    		if (m.group(1) != null) {
				String tempTodo = m.group(1).trim();
				if(tempTodo.length() > 0 && isValidTodo(tempTodo)) {
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
    		Log.w(LT, "Title not matched: " + title);
    		newNode.name = title;
    	}
    	
        newNode.setTitle(this.stripTitle(title));

    	return newNode;
    }
 
    Pattern titlePattern2 = Pattern.compile("<before.*</before>|<after.*</after>");
    private String stripTitle(String orgTitle) {
    	// TODO Strip links to only display real title: [[foo][bar]] should be bar 
        Matcher titleMatcher = titlePattern2.matcher(orgTitle);
        String newTitle = "";
        if (titleMatcher.find()) {
            newTitle += orgTitle.substring(0, titleMatcher.start());
            newTitle += orgTitle.substring(titleMatcher.end(), orgTitle.length());
        }
        else {
            newTitle = orgTitle;
        }
        return newTitle;
    }

    private ArrayList<EditNode> parseEdits() {
        Pattern editTitlePattern = Pattern.compile("F\\((edit:.*?)\\) \\[\\[(.*?)\\]\\[(.*?)\\]\\]");
        Pattern createTitlePattern = Pattern.compile("^\\*\\s+(.*)");
 
        ArrayList<EditNode> edits = new ArrayList<EditNode>();
        OrgFile orgfile = new OrgFile(NodeWriter.ORGFILE, context);
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
