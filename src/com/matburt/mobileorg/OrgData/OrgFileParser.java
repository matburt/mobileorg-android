package com.matburt.mobileorg.OrgData;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Stack;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.matburt.mobileorg.OrgData.OrgContract.OrgData;
import com.matburt.mobileorg.util.OrgFileNotFoundException;

import android.content.ContentResolver;
import android.text.TextUtils;
import android.util.Log;
import android.util.Pair;

public class OrgFileParser {

    private OrgDatabase db;
    private ContentResolver resolver;    
 
    private ParseStack parseStack;
	private StringBuilder payload;
	private OrgFile orgFile;
	private HashSet<String> todos;
	
	public OrgFileParser(OrgDatabase db, ContentResolver resolver) {
		this.db = db;
		this.resolver = resolver;
	}

	private void init(OrgFile orgFile) {
		orgFile.removeFile(resolver);
		orgFile.addFile(resolver);
		this.orgFile = orgFile;

		this.parseStack = new ParseStack();
		this.parseStack.add(0, orgFile.nodeId);

		this.todos = new HashSet<String>(OrgProviderUtils.getTodos(resolver));
		
		this.payload = new StringBuilder();
	}
	
	public void parse(OrgFile orgFile, BufferedReader breader) {
		init(orgFile);
		db.beginTransaction();
		try {
			String currentLine;
			while ((currentLine = breader.readLine()) != null)
				parseLine(currentLine);
			
			// Add payload to the final node
			db.fastInsertNodePayload(parseStack.getCurrentNodeId(), this.payload.toString());

		} catch (IOException e) {}
		
		db.endTransaction();

		if(orgFile.filename.equals(OrgFile.AGENDA_FILE)) {
			try {
				combineBlockAgendas();
			} catch (OrgFileNotFoundException e) {}
		}
	}

	private void parseLine(String line) {
		if (TextUtils.isEmpty(line))
			return;

		int numstars = numberOfStars(line);
		if (numstars > 0) {
			db.fastInsertNodePayload(parseStack.getCurrentNodeId(), this.payload.toString());
			this.payload = new StringBuilder();
			parseHeading(line, numstars);
		} else {
			payload.append(line).append("\n");
		}
	}
	
    
	private void parseHeading(String thisLine, int numstars) {
		if (numstars == parseStack.getCurrentLevel()) { // Node on same level
			parseStack.pop();
		} else if (numstars < parseStack.getCurrentLevel()) { // Node on lower level
			while (numstars <= parseStack.getCurrentLevel())
				parseStack.pop();
		}
        
		final OrgNode node = new OrgNode();
		node.parseLine(thisLine, numstars, this.todos);
		node.fileId = orgFile.id;
		node.parentId = parseStack.getCurrentNodeId();
		long newId = db.fastInsertNode(node);
		parseStack.add(numstars, newId);      
    }

	// TODO Replace with Regex matching
	private static int numberOfStars(String thisLine) {
		int numstars = 0;
		int lineLength = thisLine.length();

		for (int idx = 0; idx < lineLength; idx++) {
			if (thisLine.charAt(idx) != '*')
				break;
			numstars++;
		}

		if (numstars >= lineLength || thisLine.charAt(numstars) != ' ')
			numstars = 0;

		return numstars;
	}
    
	private class ParseStack {
		private Stack<Pair<Integer, Long>> parseStack;

		public ParseStack() {
			this.parseStack = new Stack<Pair<Integer, Long>>();
		}
		
		public void add(int level, long nodeId) {
			parseStack.push(new Pair<Integer, Long>(level, nodeId));
		}
		
		public void pop() {
			this.parseStack.pop();
		}
		
		public int getCurrentLevel() {
			return parseStack.peek().first;
		}
		
		public long getCurrentNodeId() {
			return parseStack.peek().second;
		}
	}

	public static final String BLOCK_SEPARATOR_PREFIX = "#HEAD#";	
	private void combineBlockAgendas() throws OrgFileNotFoundException {		
		OrgNode agendaFile = OrgProviderUtils.getOrgNodeFromFilename(
				OrgFile.AGENDA_FILE, resolver);
		
		String previousBlockTitle = "";
		OrgNode previousBlockNode = null;
		
		for(OrgNode node: agendaFile.getChildren(resolver)) {
			if(node.name.indexOf(">") == -1)
				continue;
			
			String blockTitle = node.name.substring(0, node.name.indexOf(">"));
			String blockEntryName = node.name.substring(node.name.indexOf(">") + 1);
			
			if(TextUtils.isEmpty(blockTitle) == false) { // Is a block agenda
				if(blockTitle.equals(previousBlockTitle) == false) { // Create new node to contain block agenda	
					previousBlockTitle = blockTitle;

					previousBlockNode = new OrgNode();
					previousBlockNode.fileId = agendaFile.fileId;
					previousBlockNode.name = blockTitle;
					previousBlockNode.parentId = agendaFile.id;
					previousBlockNode.level = agendaFile.level + 1;
					previousBlockNode.id = db.fastInsertNode(previousBlockNode);
				}
				
				ArrayList<OrgNode> children = node.getChildren(resolver);
				if(blockTitle.startsWith("Day-agenda") || blockTitle.startsWith("Week-agenda")) {
					for(OrgNode node2: children)
						for (OrgNode childNode: node2.getChildren(resolver))
							cloneChildren(childNode, previousBlockNode, node2.name);
				} else
					cloneChildren(node, previousBlockNode, blockEntryName); // Normal cloning
				
				resolver.delete(OrgData.buildIdUri(node.id), null, null);
			}
		}
	}

	private void cloneChildren(OrgNode node, OrgNode parent, String blockTitle) {
		Log.d("MobileOrg", "cloning " + node.name);
		OrgNode blockSeparator = new OrgNode();
		blockSeparator.name = BLOCK_SEPARATOR_PREFIX + blockTitle;
		blockSeparator.fileId = parent.fileId;
		blockSeparator.parentId = parent.id;
		blockSeparator.level = parent.level + 1;
		db.fastInsertNode(blockSeparator);
		
		for(OrgNode child: node.getChildren(resolver)) {
			Log.d("MobileOrg", "cloning child" + child.name);
			OrgNode clonedChild = new OrgNode(child);
			clonedChild.parentId = parent.id;
			clonedChild.fileId = parent.fileId;
			clonedChild.level = parent.level + 1;
			db.fastInsertNode(clonedChild);
			resolver.delete(OrgData.buildIdUri(node.id), null, null);
		}
	}
    
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
		final String fileMatchPattern = "\\[file:(.*?)\\]\\[(.*?)\\]\\]";
		Pattern indexOrgFilePattern = Pattern.compile(fileMatchPattern);
		Matcher indexOrgFileMatcher = indexOrgFilePattern.matcher(filecontents);
		HashMap<String, String> allOrgFiles = new HashMap<String, String>();
		
		while (indexOrgFileMatcher.find()) {
			allOrgFiles.put(indexOrgFileMatcher.group(1), indexOrgFileMatcher.group(2));
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
