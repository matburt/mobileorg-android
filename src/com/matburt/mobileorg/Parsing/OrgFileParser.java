package com.matburt.mobileorg.Parsing;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EmptyStackException;
import java.util.HashMap;
import java.util.Stack;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.matburt.mobileorg.MobileOrgApplication;
import android.content.ContentValues;
import android.content.SharedPreferences;
import android.os.Environment;
import android.util.Log;


public class OrgFileParser {

	class TitleComponents {
		String title = "";
		String todo = "";
        String priority = "";
		ArrayList<String> tags = new ArrayList<String>();
	}

    HashMap<String, String> orgPaths;
    ArrayList<Node> nodeList = new ArrayList<Node>();
    String storageMode = null;
    String userSynchro = null;
    Pattern titlePattern = null;
    FileInputStream fstream;
    public Node rootNode = new Node("");
    MobileOrgDatabase appdb;
	ArrayList<HashMap<String, Integer>> todos = null;
    public static final String LT = "MobileOrg";
    public String orgDir = Environment.getExternalStorageDirectory() +
                           "/mobileorg/";

    public OrgFileParser(HashMap<String, String> orgpaths,
                         String storageMode,
                         String userSynchro,
                         MobileOrgDatabase appdb,
                         String orgBasePath) {
    	this.orgPaths = orgpaths;
        this.appdb = appdb;
        this.storageMode = storageMode;
        this.userSynchro = userSynchro;
        this.orgDir = orgBasePath;
    }
    
	public OrgFileParser(SharedPreferences appSettings,
			MobileOrgApplication appInst, MobileOrgDatabase appdb) {
		this.appdb = appdb;

		HashMap<String, String> allOrgList = this.appdb.getOrgFiles();
		if (allOrgList.isEmpty())
			return;
		this.orgPaths = allOrgList;

		this.storageMode = appSettings.getString("storageMode", "");
		this.userSynchro = appSettings.getString("syncSource", "");

		String orgBasePath = "";

		if (userSynchro.equals("sdcard")) {
			String indexFile = appSettings.getString("indexFilePath", "");
			File fIndexFile = new File(indexFile);
			orgBasePath = fIndexFile.getParent() + "/";
		} else {
			orgBasePath = Environment.getExternalStorageDirectory()
					.getAbsolutePath() + "/mobileorg/";
		}

		this.orgDir = orgBasePath;

	}
    
	public void runParser(SharedPreferences appSettings,
			MobileOrgApplication appInst) {
		parse();
		appInst.rootNode = rootNode;
		appInst.edits = parseEdits();
		Collections.sort(appInst.rootNode.children, Node.comparator);
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

    private TitleComponents parseTitle (String orgTitle) {
    	TitleComponents component = new TitleComponents();
    	String title = orgTitle.trim();
    	Pattern pattern = prepareTitlePattern();
    	Matcher m = pattern.matcher(title);
    	if (m.find()) {
    		if (m.group(1) != null) {
				String todo = m.group(1).trim();
				if(todo.length() > 0 && isValidTodo(todo)) {
					component.todo = todo;
				} else {
					component.title = todo + " ";
				}
			}
            if (m.group(2) != null) {
                component.priority = m.group(2);
                component.priority = component.priority.replace("#", "");
                component.priority = component.priority.replace("[", "");
                component.priority = component.priority.replace("]", "");
            }
    		component.title += m.group(3);
    		String tags = m.group(4);
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

    public long createEntry(String heading, String content, long parentId) {
        ContentValues recValues = new ContentValues();
        recValues.put("heading", heading);
        recValues.put("content", content);
        recValues.put("parentid", parentId);
        return this.appdb.appdb.insert("data", null, recValues);
    }

    public void addContent(long nodeId, String content) {
        ContentValues recValues = new ContentValues();
        recValues.put("content", content + "\n");
        this.appdb.appdb.update("data", recValues, "id = ?",
                                new String[] {Long.toString(nodeId)});
    }

    public String getNodePath(Node baseNode) {
        String npath = baseNode.name;
        Node pnode = baseNode;
        while ((pnode = pnode.parent) != null) {
            if (pnode.name.length() > 0) {
                npath = pnode.name + "/" + npath;
            }
        }
        npath = "olp:" + npath;
        return npath;
    }

    public void parse(Node fileNode, BufferedReader breader)
    {
        Pattern editTitlePattern =
            Pattern.compile("F\\((edit:.*?)\\) \\[\\[(.*?)\\]\\[(.*?)\\]\\]");
        try
        {
			this.todos = appdb.getTodos();

            String thisLine;
            Stack<Node> nodeStack = new Stack<Node>();
            Stack<Integer> starStack = new Stack<Integer>();
            Pattern propertiesLine = Pattern.compile("^\\s*:[A-Z]+:");
            if(breader == null)
            {
                breader = this.getHandle(fileNode.name);
            }
            nodeStack.push(fileNode);
            starStack.push(0);

            while ((thisLine = breader.readLine()) != null) {
                int numstars = 0;
                Matcher editm = editTitlePattern.matcher(thisLine);
                if (thisLine.length() < 1 || editm.find())
                    continue;
                if (thisLine.charAt(0) == '#') {
                    if (thisLine.indexOf("#+TITLE:") != -1) {
                        fileNode.altNodeTitle = thisLine.substring(
                             thisLine.indexOf("#+TITLE:") + 8).trim();

                    }
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
                    Node newNode = new Node(titleComp.title);
                    newNode.setTitle(this.stripTitle(title));
                    newNode.todo = titleComp.todo;
                    newNode.priority = titleComp.priority;
                    newNode.setTags(titleComp.tags);
                    if (numstars > starStack.peek()) {
                        try {
                            Node lastNode = nodeStack.peek();
                            newNode.setParentNode(lastNode);
                            newNode.nodeId = this.getNodePath(newNode);
                            newNode.addProperty("ID", newNode.nodeId);
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
                        newNode.nodeId = this.getNodePath(newNode);
                        newNode.addProperty("ID", newNode.nodeId);
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
                        newNode.nodeId = this.getNodePath(newNode);
                        newNode.addProperty("ID", newNode.nodeId);
                        lastNode.addChild(newNode);
                        nodeStack.push(newNode);
                        starStack.push(numstars);
                    }
                }
                //content
                else {
                    Matcher propm = propertiesLine.matcher(thisLine);
                    Node lastNode = nodeStack.peek();
                    if (thisLine.indexOf(":ID:") != -1) {
                        String trimmedLine = thisLine.substring(thisLine.indexOf(":ID:")+4).trim();
                        lastNode.addProperty("ID", trimmedLine);
                        lastNode.nodeId = trimmedLine;
                        continue;
                    }
                    else if (thisLine.indexOf(":ORIGINAL_ID:") != -1) {
                    	String trimmedLine = thisLine.substring(thisLine.indexOf(":ORIGINAL_ID:")+13).trim();
                        lastNode.addProperty("ORIGINAL_ID", trimmedLine);
                        lastNode.nodeId = trimmedLine;
                        continue;
                    }
                    else if (propm.find()) {
                        continue;
                    }
                    else if (thisLine.indexOf("DEADLINE:") != -1 ||
                             thisLine.indexOf("SCHEDULED:") != -1) {
                        try {
                            Pattern deadlineP = Pattern.compile(
                                                  "^.*(DEADLINE: <.+?>)");
                            Matcher deadlineM = deadlineP.matcher(thisLine);
                            Pattern schedP = Pattern.compile(
                                                  "^.*(SCHEDULED: <.+?>)");
                            Matcher schedM = schedP.matcher(thisLine);
                            SimpleDateFormat dFormatter = new SimpleDateFormat(
                                                            "'DEADLINE': <yyyy-MM-dd EEE>");
                            SimpleDateFormat sFormatter = new SimpleDateFormat(
                                                            "'SCHEDULED': <yyyy-MM-dd EEE>");
                            if (deadlineM.find()) {
                                lastNode.deadline = dFormatter.parse(deadlineM.group(1));
                            }
                            if (schedM.find()) {
                                lastNode.schedule = sFormatter.parse(schedM.group(1));
                            }
                        }
                        catch (java.text.ParseException e) {
                            Log.e(LT, "Could not parse deadline");
                        }
                        continue;
                    }
                    lastNode.addPayload(thisLine);
                }
            }
            while (starStack.peek() > 0) {
                nodeStack.pop();
                starStack.pop();
            }
            fileNode.parsed = true;
            breader.close();
        }
        catch (IOException e) {
            Log.e(LT, "IO Exception on readerline: " + e.getMessage());
        }

    }

    public void parse() {
        Stack<Node> nodeStack = new Stack<Node>();
        nodeStack.push(this.rootNode);

        for (String key : this.orgPaths.keySet()) {
            String altName = this.orgPaths.get(key);
            Log.d(LT, "Parsing: " + key);
            //if file is encrypted just add a placeholder node to be parsed later
            if(key.endsWith(".gpg") ||
               key.endsWith(".pgp") ||
               key.endsWith(".enc"))
            {
                Node nnode = new Node(key, true);
                nnode.altNodeTitle = altName;
                nnode.setParentNode(nodeStack.peek());
                nnode.addProperty("ID", this.getNodePath(nnode));
                nodeStack.peek().addChild(nnode);
                continue;
            }

            Node fileNode = new Node(key, false);
            fileNode.setParentNode(nodeStack.peek());
            fileNode.addProperty("ID", this.getNodePath(fileNode));
            fileNode.altNodeTitle = altName;
            nodeStack.peek().addChild(fileNode);
            nodeStack.push(fileNode);
            parse(fileNode, null);
            nodeStack.pop();
        }
    }

    public ArrayList<EditNode> parseEdits() {
        Pattern editTitlePattern = Pattern.compile("F\\((edit:.*?)\\) \\[\\[(.*?)\\]\\[(.*?)\\]\\]");
        Pattern createTitlePattern = Pattern.compile("^\\*\\s+(.*)");
        ArrayList<EditNode> edits = new ArrayList<EditNode>();
        BufferedReader breader = this.getHandle("mobileorg.org");
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
            Log.e(LT, "IO Exception caught trying to read edits file");
        }
        return edits;
    }

    public BufferedReader getHandle(String filename) {
        BufferedReader breader = null;
        try {
            // If user is sync'ing from the SDCard, read directly from that
            // location, regardless of storage mode.
            if ("sdcard".equals(this.userSynchro)
                    || "sdcard".equals(this.storageMode)) {
                String dirActual = "";
                if (filename.equals("mobileorg.org")) {
                    dirActual = Environment.getExternalStorageDirectory().getAbsolutePath() +
                                "/mobileorg/";
                }
                else {
                    dirActual = this.orgDir;
                }
                this.fstream = new FileInputStream(dirActual + filename);
            }
            else if (this.storageMode == null || this.storageMode.equals("internal")) {
                String normalized = filename.replace("/", "_");
                this.fstream = new FileInputStream("/data/data/com.matburt.mobileorg/files/" +
                                                   normalized);
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

    public static byte[] getRawFileData(String baseDir, String filename)
    {
        try {
            File file = new File(baseDir + filename);
            FileInputStream is = new FileInputStream(file);
            byte[] buffer = new byte[(int)file.length()];
            int offset = 0;
            int numRead = 0;
            while (offset < buffer.length
                   && (numRead=is.read(buffer, offset, buffer.length-offset)) >= 0) 
            {
                offset += numRead;
            }
            is.close();
            if (offset < buffer.length) {
                throw new IOException("Could not completely read file "+file.getName());
            }
            return buffer;
        }
        catch (Exception e) {
            Log.e(LT, "Error: " + e.getMessage() + " in file " + filename);
            return null;
        }
    }

}
