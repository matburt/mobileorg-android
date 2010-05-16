package com.matburt.mobileorg;

import java.util.HashMap;
import java.util.Map;
import java.util.ArrayList;
import java.util.Stack;
import java.util.EmptyStackException;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.io.FileInputStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.FileNotFoundException;
import android.text.TextUtils;
import android.util.Log;

class OrgFileParser {
	
	class TitleComponents {
		String title;
		String todo;
		ArrayList<String> tags = new ArrayList<String>();
	}

    ArrayList<String> orgPaths;
    ArrayList<Node> nodeList = new ArrayList<Node>();
    ArrayList<String> todoKeywords = new ArrayList<String>();
    String storageMode = null;
    Pattern titlePattern = null;
    FileInputStream fstream;
    Node rootNode = new Node("MobileOrg", Node.NodeType.HEADING);
    public static final String LT = "MobileOrg";

    OrgFileParser(ArrayList<String> orgpaths, String storageMode) {
        this.storageMode = storageMode;
        this.orgPaths = orgpaths;
        this.todoKeywords.add("TODO");
        this.todoKeywords.add("DONE");
    }
    
    private Pattern prepareTitlePattern () {
    	if (this.titlePattern == null) {
    		StringBuffer pattern = new StringBuffer();
    		pattern.append("^(?:(");
    		pattern.append(TextUtils.join("|", todoKeywords));
    		pattern.append(")\\s*)?");
    		pattern.append("(.*?)");
    		pattern.append("\\s*(?::([^\\s]+):)?$");
    		this.titlePattern = Pattern.compile(pattern.toString());
    	}
		return this.titlePattern;
    }
    
    private TitleComponents parseTitle (String orgTitle) {
    	TitleComponents component = new TitleComponents();
    	String title = orgTitle.trim();
    	Pattern pattern = prepareTitlePattern();
    	Matcher m = pattern.matcher(title);
    	if (m.find()) {
    		if (m.group(1) != null)
    			component.todo = m.group(1);
    		component.title = m.group(2);
    		String tags = m.group(3);
    		if (tags != null) {
    			for (String tag : tags.split(":")) {
    				component.tags.add(tag);
				}
    		}
    	} else {
    		Log.w(LT, "Title not matched: " + title);
    		component.title = title;
    	}
    	return component;
    }

    private String stripTitle(String orgTitle) {
        Pattern titlePattern = Pattern.compile("<before.*</before>|<after.*</after>");
        Matcher titleMatcher = titlePattern.matcher(orgTitle);
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

    public void parse() {
        String thisLine;
        Stack<Node> nodeStack = new Stack();
        nodeStack.push(this.rootNode);
        int nodeDepth = 0;

        for (int jdx = 0; jdx < this.orgPaths.size(); jdx++) {
            try {
                Log.d(LT, "Parsing: " + orgPaths.get(jdx));
                BufferedReader breader = this.getHandle(this.orgPaths.get(jdx));
                Node fileNode = new Node(this.orgPaths.get(jdx),
                                         Node.NodeType.HEADING);

                nodeStack.peek().addChildNode(fileNode);
                nodeStack.push(fileNode);
                while ((thisLine = breader.readLine()) != null) {
                    int numstars = 0;

                    if (thisLine.length() < 1 || thisLine.charAt(0) == '#') {
                        continue;
                    }

                    for (int idx = 0; idx < thisLine.length(); idx++) {
                        if (thisLine.charAt(idx) != '*') {
                            break;
                        }
                        numstars++;
                    }

                    if (numstars >= thisLine.length() || thisLine.charAt(numstars) != ' ') {
                        numstars = 0;
                    }

                    //headings
                    if (numstars > 0) {
                        String title = thisLine.substring(numstars+1);
                        TitleComponents titleComp = parseTitle(this.stripTitle(title));
                        Node newNode = new Node(titleComp.title, Node.NodeType.HEADING);
                        newNode.todo = titleComp.todo;
                        newNode.tags.addAll(titleComp.tags);
                        if (numstars > nodeDepth) {
                            try {
                                Node lastNode = nodeStack.peek();
                                lastNode.addChildNode(newNode);
                            } catch (EmptyStackException e) {
                            }
                            nodeStack.push(newNode);
                            nodeDepth++;
                        }
                        else if (numstars == nodeDepth) {
                            nodeStack.pop();
                            nodeStack.peek().addChildNode(newNode);
                            nodeStack.push(newNode);
                        }
                        else if (numstars < nodeDepth) {
                            for (;numstars <= nodeDepth; nodeDepth--) {
                                nodeStack.pop();
                            }

                            Node lastNode = nodeStack.peek();
                            lastNode.addChildNode(newNode);
                            nodeStack.push(newNode);
                            nodeDepth++;
                        }
                    }
                    //content
                    else {
                        Node lastNode = nodeStack.peek();
                        lastNode.addPayload(thisLine);
                    }
                }
                for (;nodeDepth > 0; nodeDepth--) {
                    nodeStack.pop();
                }
                nodeStack.pop();
                breader.close();
            }
            catch (IOException e) {
                Log.e(LT, "IO Exception on readerline: " + e.getMessage());
            }
        }
    }

    public BufferedReader getHandle(String filename) {
        BufferedReader breader = null;
        try {
            if (this.storageMode == null || this.storageMode.equals("internal")) {
                this.fstream = new FileInputStream("/data/data/com.matburt.mobileorg/files/" + filename);
            }
            else if (this.storageMode.equals("sdcard")) {
                this.fstream = new FileInputStream("/sdcard/mobileorg/" + filename);
            }
            else {
                Log.e(LT, "[Parse] Unknown storage mechanism: " + this.storageMode);
                this.fstream = null;
            }
            DataInputStream in = new DataInputStream(this.fstream);
            breader = new BufferedReader(new InputStreamReader(in));
        }
        catch (Exception e) {
            Log.e(LT, "Error: " + e.getMessage() + " in file " + filename);
        }
        return breader;
    }

}
