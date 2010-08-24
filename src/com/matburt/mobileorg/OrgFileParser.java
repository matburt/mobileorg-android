package com.matburt.mobileorg;

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
import android.content.ContentValues;
import android.database.sqlite.SQLiteDatabase;

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
    Node rootNode = new Node("MobileOrg", Node.HEADING);
    MobileOrgDatabase appdb;
    public static final String LT = "MobileOrg";
    public static final String ORG_DIR = "/sdcard/mobileorg/";
    OrgFileParser(ArrayList<String> orgpaths,
                  String storageMode,
                  MobileOrgDatabase appdb) {
        this.appdb = appdb;
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

    public long createEntry(String heading, int nodeType,
                            String content, long parentId) {
        ContentValues recValues = new ContentValues(); 
        recValues.put("heading", heading);
        recValues.put("type", nodeType);
        recValues.put("content", content);
        recValues.put("parentid", parentId);
        return this.appdb.appdb.insert("data", null, recValues);
    }

    public void addContent(long nodeId, String content) {
        ContentValues recValues = new ContentValues();
        recValues.put("content", content + "\n");
        this.appdb.appdb.update("data", recValues, "id = ?", new String[] {Long.toString(nodeId)});
    }

    public void parse(Node fileNode)
    {
        try
        {
            String thisLine;
            Stack<Node> nodeStack = new Stack();
            BufferedReader breader = this.getHandle(fileNode.nodeName);
            nodeStack.push(fileNode);
            int nodeDepth = 0;

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
                    Node newNode = new Node(titleComp.title,
                                            Node.HEADING);
                    newNode.setFullTitle(this.stripTitle(title));
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
                    if (thisLine.indexOf(":ID:") != -1) {
                        String trimmedLine = thisLine.substring(thisLine.indexOf(":ID:")+4).trim();
                        lastNode.addProperty("ID", trimmedLine);
                    }
                    lastNode.addPayload(thisLine);
                }
            }
            for (;nodeDepth > 0; nodeDepth--) {
                nodeStack.pop();
            }
            fileNode.parsed = true;        
            breader.close();
        }
        catch (IOException e) {
            Log.e(LT, "IO Exception on readerline: " + e.getMessage());
        }

    }

    public void parse() {
        Stack<Node> nodeStack = new Stack();
        nodeStack.push(this.rootNode);

        for (int jdx = 0; jdx < this.orgPaths.size(); jdx++) {
            Log.d(LT, "Parsing: " + orgPaths.get(jdx));
            //if file is encrypted just add a placeholder node to be parsed later
            if(!orgPaths.get(jdx).endsWith(".org"))
            {
                nodeStack.peek().addChildNode(new Node(orgPaths.get(jdx),
                                                       Node.HEADING,
                                                       true));
                continue;
            }

            Node fileNode = new Node(this.orgPaths.get(jdx),
                                     Node.HEADING,
                                     false);
            nodeStack.peek().addChildNode(fileNode);
            nodeStack.push(fileNode);
            parse(fileNode);
            nodeStack.pop();
        }
    }

    public BufferedReader getHandle(String filename) {
        BufferedReader breader = null;
        try {
            if (this.storageMode == null || this.storageMode.equals("internal")) {
                String normalized = filename.replace("/", "_");
                this.fstream = new FileInputStream("/data/data/com.matburt.mobileorg/files/" + normalized);
            }
            else if (this.storageMode.equals("sdcard")) {
                this.fstream = new FileInputStream(ORG_DIR + filename);
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

    public static String getRawFileData(String filename)
    {
        try {
            FileInputStream fstream = new FileInputStream(ORG_DIR + filename);
            BufferedReader breader = new BufferedReader(new InputStreamReader(new DataInputStream(fstream)));
            String line = null;
            String buffer = "";
            while ((line = breader.readLine()) != null) {
                buffer += line;
            }
            return buffer;
        }
        catch (Exception e) {
            Log.e(LT, "Error: " + e.getMessage() + " in file " + filename);
            return null;
        }
    }
}
