package com.matburt.mobileorg.Parsing;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Stack;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.text.TextUtils;
import android.util.Log;


public class OrgFileParser {

    private static final String LT = "MobileOrg";

    private ArrayList<HashMap<String, Integer>> todos = null;

	private static Pattern titlePattern = null;
	private Stack<Integer> starStack;
	private Stack<Long> parentIdStack;
	private StringBuilder payload;
	
	Pattern editTitlePattern = Pattern
			.compile("F\\((edit:.*?)\\) \\[\\[(.*?)\\]\\[(.*?)\\]\\]");
    
	public OrgFileParser(OrgDatabase appdb) {
	}

	long file_id;
	
	public void parse(String filename, BufferedReader breader, OrgDatabase orgdb, long file_id) {
		this.file_id = file_id;
		this.todos = orgdb.getGroupedTodos();

		this.starStack = new Stack<Integer>();
		this.parentIdStack = new Stack<Long>();
		
		this.starStack.push(0);
		Long fileID = orgdb.getFileNodeId(filename);
		this.parentIdStack.push(fileID);

		this.payload = new StringBuilder();
		
		orgdb.getDB().beginTransaction();
		
		try {
			String currentLine;

			while ((currentLine = breader.readLine()) != null) {

				if (currentLine.isEmpty())
					continue;

				int lineLength = currentLine.length();
				int numstars = numberOfStars(currentLine, lineLength);
				if (numstars > 0) {
					parseHeading(currentLine, numstars, orgdb);
				} else {
					payload.append(currentLine);
					payload.append("\n");
				}
			}

		} catch (IOException e) {}
		
		orgdb.getDB().setTransactionSuccessful();
		orgdb.getDB().endTransaction();
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
    
	private void parseHeading(String thisLine, int numstars, OrgDatabase orgdb) {
		orgdb.addNodePayload(this.parentIdStack.peek(), this.payload.toString());
		
		this.payload = new StringBuilder();
		
		if (numstars == starStack.peek()) { // Node on same level
			starStack.pop();
			parentIdStack.pop();
		} else if (numstars < starStack.peek()) { // Node on lower level
			while (numstars <= starStack.peek()) {
				starStack.pop();
				parentIdStack.pop();
			}
		}
        
		long newId = parseLineIntoNode(thisLine, numstars, orgdb);
        this.parentIdStack.push(newId);
        starStack.push(numstars);        
    }
    
    private long parseLineIntoNode (String thisLine, int numstars, OrgDatabase orgdb) {
    	String heading = stripHeading(thisLine, numstars).trim();
    	
        String name = "";
        String priority = "";
        String todo = "";
        String tags = "";
        
    	Pattern pattern = prepareTitlePattern();
    	Matcher m = pattern.matcher(heading);
		if (m.find()) {
			if (m.group(1) != null) {
				String tempTodo = m.group(1).trim();
				if (tempTodo.length() > 0 && isValidTodo(tempTodo)) {
					todo = tempTodo;
				} else {
					name = tempTodo + " ";
				}
			}
			if (m.group(2) != null) {
				priority = m.group(2);
				priority = priority.replace("#", "");
				priority = priority.replace("[", "");
				priority = priority.replace("]", "");
			}
			name += m.group(3);
			String tempTags = m.group(4);
			if (tempTags != null) {
					tags = tempTags;
			}
		} else {
			Log.w(LT, "Title not matched: " + heading);
			name = heading;
		}
    	
		long nodeId = orgdb.addNode(this.parentIdStack.peek(), name, todo, priority, tags, this.file_id);
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

        return newHeading;
    }
 
    private Pattern prepareTitlePattern() {
    	if (OrgFileParser.titlePattern == null) {
    		StringBuffer pattern = new StringBuffer();
    		pattern.append("^(?:([A-Z]{2,}:?\\s+");
    		pattern.append(")\\s*)?"); 
    		pattern.append("(\\[\\#.*\\])?(.*?)");
    		pattern.append("\\s*(?::([^\\s]+):)?"); // tags
    		pattern.append("(\\s*[!\\*])*$"); // habits
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


//    public ArrayList<EditNode> parseEdits() {
//        Pattern editTitlePattern = Pattern.compile("F\\((edit:.*?)\\) \\[\\[(.*?)\\]\\[(.*?)\\]\\]");
//        Pattern createTitlePattern = Pattern.compile("^\\*\\s+(.*)");
// 
//        ArrayList<EditNode> edits = new ArrayList<EditNode>();
//        OrgFile orgfile = new OrgFile(OrgFile.CAPTURE_FILE, context);
//        BufferedReader breader = orgfile.getReader();
//        if (breader == null)
//            return edits;
//
//        String thisLine;
//        boolean awaitingOldVal = false;
//        boolean awaitingNewVal = false;
//        EditNode thisNode = null;
//
//        try {
//            while ((thisLine = breader.readLine()) != null) {
//                Matcher editm = editTitlePattern.matcher(thisLine);
//                Matcher createm = createTitlePattern.matcher(thisLine);
//                if (editm.find()) {
//                    thisNode = new EditNode();
//                    if (editm.group(1) != null)
//                        thisNode.editType = editm.group(1).split(":")[1];
//                    if (editm.group(2) != null)
//                        thisNode.nodeId = editm.group(2).split(":")[1];
//                    if (editm.group(3) == null)
//                        thisNode.title = editm.group(3);
//                }
//                else if (createm.find()) {
//                }
//                else {
//                    if (thisLine.indexOf("** Old value") != -1) {
//                        awaitingOldVal = true;
//                        continue;
//                    }
//                    else if (thisLine.indexOf("** New value") != -1) {
//                        awaitingOldVal = false;
//                        awaitingNewVal = true;
//                        continue;
//                    }
//                    else if (thisLine.indexOf("** End of edit") != -1) {
//                        awaitingNewVal = false;
//                        edits.add(thisNode);
//                    }
//
//                    if (awaitingOldVal) {
//                        thisNode.oldVal += thisLine;
//                    }
//                    if (awaitingNewVal) {
//                        thisNode.newVal += thisLine;
//                    }
//                }
//            }
//        }
//        catch (java.io.IOException e) {
//            return null;
//        }
//        return edits;
//    }
    
	/**
	 * Parses the checksum file.
	 * @return HashMap with Filename->checksum
	 */
	public static HashMap<String, String> getChecksums(String filecontents) {
		HashMap<String, String> checksums = new HashMap<String, String>();
		for (String line : filecontents.split("[\\n\\r]+")) {
			if (TextUtils.isEmpty(line))
				continue;
			String[] chksTuple = line.split("\\s+");
			if(chksTuple.length >= 2)
				checksums.put(chksTuple[1], chksTuple[0]);
		}
		return checksums;
	}
	
	/**
	 * Parses the file list from index file.
	 * @return HashMap with Filename->Filename Alias
	 */
	public static HashMap<String, String> getFilesFromIndex(String filecontents) {
		Pattern getOrgFiles = Pattern.compile("\\[file:(.*?)\\]\\[(.*?)\\]\\]");
		Matcher m = getOrgFiles.matcher(filecontents);
		HashMap<String, String> allOrgFiles = new HashMap<String, String>();
		while (m.find()) {
			allOrgFiles.put(m.group(1), m.group(2));
		}

		return allOrgFiles;
	}


	public static ArrayList<HashMap<String, Boolean>> getTodosFromIndex(String filecontents) {
		Pattern getTodos = Pattern
				.compile("#\\+TODO:\\s+([\\s\\w-]*)(\\| ([\\s\\w-]*))*");
		Matcher m = getTodos.matcher(filecontents);
		ArrayList<HashMap<String, Boolean>> todoList = new ArrayList<HashMap<String, Boolean>>();
		while (m.find()) {
			String lastTodo = "";
			HashMap<String, Boolean> holding = new HashMap<String, Boolean>();
			Boolean isDone = false;
			for (int idx = 1; idx <= m.groupCount(); idx++) {
				if (m.group(idx) != null && m.group(idx).length() > 0) {
					if (m.group(idx).indexOf("|") != -1) {
						isDone = true;
						continue;
					}
					String[] grouping = m.group(idx).split("\\s+");
					for (String group : grouping) {
						lastTodo = group.trim();
						holding.put(group.trim(), isDone);
					}
				}
			}
			if (!isDone) {
				holding.put(lastTodo, true);
			}
			todoList.add(holding);
		}
		return todoList;
	}
	
	public static ArrayList<String> getPrioritiesFromIndex(String filecontents) {
		Pattern getPriorities = Pattern
				.compile("#\\+ALLPRIORITIES:\\s+([A-Z\\s]*)");
		Matcher t = getPriorities.matcher(filecontents);

		ArrayList<String> priorities = new ArrayList<String>();

		if (t.find() && t.group(1) != null && t.group(1).length() > 0) {
			String[] grouping = t.group(1).split("\\s+");
			for (String group : grouping) {
				priorities.add(group.trim());
			}
		}
		return priorities;
	}
	
	
}
