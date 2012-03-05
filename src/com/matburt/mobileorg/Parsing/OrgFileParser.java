package com.matburt.mobileorg.Parsing;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Stack;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.matburt.mobileorg.Services.CalendarSyncService;

import android.content.Context;
import android.database.Cursor;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.Log;

public class OrgFileParser {

    private static final String LT = "MobileOrg";

    private OrgDatabase db;
    
    private ArrayList<HashMap<String, Integer>> todos = null;

	private long file_id;
	private static Pattern titlePattern = null;
	private Stack<Integer> starStack;
	private Stack<Long> parentIdStack;
	private StringBuilder payload;
	
	/**
	 * This toggles the use of the <after>TITLE: </after> field. It is now
	 * disabled, see https://github.com/matburt/mobileorg-android/issues/114
	 */
	private boolean useTitleField = false;
	
	public OrgFileParser(OrgDatabase db) {
		this.db = db;
	}
	
	private long addNewFile(String filename, String fileIdentActual,
				String remoteChecksum, boolean visable) {
		db.removeFile(filename);
		return db.addOrUpdateFile(filename, fileIdentActual, remoteChecksum,
				visable);
	}
	
	public void parse(String filename, String filenameAlias, String checksum,
			BufferedReader breader, Context context) {
		this.useTitleField = PreferenceManager.getDefaultSharedPreferences(context)
				.getBoolean("useAgendaTitle", false);
		
		this.file_id = addNewFile(filename, filenameAlias, checksum, true);

		this.todos = db.getGroupedTodos();

		this.starStack = new Stack<Integer>();
		this.parentIdStack = new Stack<Long>();
		
		this.starStack.push(0);
		Long fileID = db.getFileNodeId(filename);
		this.parentIdStack.push(fileID);

		this.payload = new StringBuilder();
		
		db.getDB().beginTransaction();
		
		try {
			String currentLine;

			while ((currentLine = breader.readLine()) != null) {

				if (TextUtils.isEmpty(currentLine))
					continue;

				int lineLength = currentLine.length();
				int numstars = numberOfStars(currentLine, lineLength);
				if (numstars > 0) {
					parseHeading(currentLine, numstars);
				} else {
					payload.append(currentLine);
					payload.append("\n");
				}
			}
			
			// Add payload to the final node
			db.addNodePayload(this.parentIdStack.peek(), this.payload.toString());

		} catch (IOException e) {}
		
		if(filename.equals("agendas.org") && PreferenceManager.getDefaultSharedPreferences(context)
				.getBoolean("combineBlockAgendas", false) && useTitleField) {
			combineBlockAgendas();
		}
 		
		db.getDB().setTransactionSuccessful();
		db.getDB().endTransaction();
		
		updateCalendar(filename, context);
	}
	
	private void updateCalendar(String filename, Context context) {
		if (filename.equals("agendas.org") == false
				&& PreferenceManager.getDefaultSharedPreferences(context)
						.getBoolean("enableCalendar", false)) {
			try {
				CalendarSyncService cal = new CalendarSyncService(this.db, context);
				cal.syncFile(filename);
			} catch(IllegalArgumentException e) {
				Log.d("MobileOrg", "Failed to sync calendar");
			}	
		}
	}

	public static final String BLOCK_SEPARATOR_PREFIX = "#HEAD#";
	
	private void combineBlockAgendas() {
		final String filename = "agendas.org";
		long agendaFileNodeID = db.getFileNodeId(filename);
		Cursor cursor = db.getNodeChildren(agendaFileNodeID);
		
		cursor.moveToFirst();
		
		String previousBlockTitle = "";
		long previousBlockNode = -1;
				
		while(cursor.isAfterLast() == false) {
			String name = cursor.getString(cursor.getColumnIndex("name"));
			
			if(name.indexOf(">") == -1)
				continue;
			
			String blockTitle = name.substring(0, name.indexOf(">"));
			
			if(TextUtils.isEmpty(blockTitle) == false) { // Is a block agenda
				
				if(blockTitle.equals(previousBlockTitle) == false) { // Create new node to contain block agenda	
					previousBlockNode = db.addNode(agendaFileNodeID, blockTitle,
							"", "", "", db.getFileId(filename));
				}

				String blockEntryName = name.substring(name.indexOf(">") + 1);

				long nodeId = cursor.getLong(cursor.getColumnIndex("_id"));


				Cursor children = db.getNodeChildren(nodeId);
				children.moveToFirst();
						
				if(blockEntryName.startsWith("Day-agenda") && children.getCount() == 1) {
					blockEntryName = children.getString(children
							.getColumnIndex("name"));
					children = db.getNodeChildren(children.getLong(children
							.getColumnIndex("_id")));
					children.moveToFirst();
					cloneChildren(children, previousBlockNode, agendaFileNodeID,
							blockEntryName, filename);
				} else if(blockEntryName.startsWith("Week-agenda")) {
					while (children.isAfterLast() == false) {
						blockEntryName = children.getString(children
								.getColumnIndex("name"));
						Cursor children2 = db.getNodeChildren(children
								.getLong(children.getColumnIndex("_id")));
						children2.moveToFirst();
						cloneChildren(children2, previousBlockNode,
								agendaFileNodeID, blockEntryName, filename);
						children2.close();
						children.moveToNext();
					}
				} else
					cloneChildren(children, previousBlockNode, agendaFileNodeID,
							blockEntryName, filename);
				
				previousBlockTitle = blockTitle;
				db.deleteNode(cursor.getLong(cursor.getColumnIndex("_id")));
				children.close();
			}
			
			cursor.moveToNext();
		}
		
		cursor.close();
	}
	
	private void cloneChildren(Cursor children, long previousBlockNode,
			Long agendaNodeFileID, String blockEntryName, String filename) {
		db.addNode(previousBlockNode, BLOCK_SEPARATOR_PREFIX
				+ blockEntryName, "", "", "", db.getFileId(filename));
		
		while(children.isAfterLast() == false) {
			db.cloneNode(
					children.getLong(children.getColumnIndex("_id")),
					previousBlockNode, db.getFileId("agendas.org"));
			children.moveToNext();
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
		db.addNodePayload(this.parentIdStack.peek(), this.payload.toString());
		
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
        
		long newId = parseLineIntoNode(thisLine, numstars);
        this.parentIdStack.push(newId);
        starStack.push(numstars);        
    }
    
    private long parseLineIntoNode (String thisLine, int numstars) {
        String heading = thisLine.substring(numstars+1);

        String name = "";
        String priority = "";
        String todo = "";
        String tags = "";
        
    	Pattern pattern = prepareTitlePattern();
    	Matcher matcher = pattern.matcher(heading);
		if (matcher.find()) {
			if (matcher.group(TODO_GROUP) != null) {
				String tempTodo = matcher.group(TODO_GROUP).trim();
				if (tempTodo.length() > 0 && isValidTodo(tempTodo)) {
					todo = tempTodo;
				} else {
					name = tempTodo + " ";
				}
			}
			if (matcher.group(PRIORITY_GROUP) != null)
				priority = matcher.group(PRIORITY_GROUP);
			
			name += matcher.group(TITLE_GROUP);
			
			if(this.useTitleField && matcher.group(AFTER_GROUP) != null) {
				int start = matcher.group(AFTER_GROUP).indexOf("TITLE:");
				int end = matcher.group(AFTER_GROUP).indexOf("</after>");
				
				if(start > -1 && end > -1) {
					String title = matcher.group(AFTER_GROUP).substring(
							start + 7, end);
					
					name = title + ">" + name;
				}
			}
			
			tags = matcher.group(TAGS_GROUP);
			if (tags == null)
					tags = "";
			
		} else {
			Log.w(LT, "Title not matched: " + heading);
			name = heading;
		}
    	
		long nodeId = db.addNode(this.parentIdStack.peek(), name, todo, priority, tags, this.file_id);
    	return nodeId;
    }
 
    private static final int TODO_GROUP = 1;
    private static final int PRIORITY_GROUP = 2;
    private static final int TITLE_GROUP = 3;
    private static final int TAGS_GROUP = 4;
    private static final int AFTER_GROUP = 7;
    
    private Pattern prepareTitlePattern() {
    	if (OrgFileParser.titlePattern == null) {
    		StringBuffer pattern = new StringBuffer();
    		pattern.append("^\\s?(?:([A-Z]{2,}:?\\s+)\\s*)?"); 	// Todo
    		pattern.append("(?:\\[\\#(.*)\\])?"); 				// Priority
    		pattern.append("(.*?)"); 						// Title
    		pattern.append("\\s*(?::([^\\s]+):)?"); 		// Tags
    		pattern.append("(\\s*[!\\*])*"); 				// Habits
    		pattern.append("(<before>.*</before>)?");		// Before
    		pattern.append("(<after>.*</after>)?");			// After
    		pattern.append("$");							// End of line
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


//	private Pattern editTitlePattern = Pattern
//			.compile("F\\((edit:.*?)\\) \\[\\[(.*?)\\]\\[(.*?)\\]\\]");
//    
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
				.compile("#\\+TODO:\\s+([^\\|]*)(\\| ([^\\n]*))*");
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
	
	public static ArrayList<String> getTagsFromIndex(String filecontents) {
		Pattern getTags = Pattern.compile("#\\+TAGS:\\s+([^\\n]*)");
		
		Matcher matcher = getTags.matcher(filecontents);
		ArrayList<String> tagList = new ArrayList<String>();
		
		if(matcher.find()) {
			String tags = matcher.group(1).replaceAll("[\\{\\}]", "");
			String[] split = tags.split("\\s+");
			for(String tag: split)
				tagList.add(tag);
		}
		
		return tagList;
	}
}





