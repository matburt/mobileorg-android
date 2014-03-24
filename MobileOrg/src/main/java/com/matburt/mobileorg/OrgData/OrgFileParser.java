package com.matburt.mobileorg.OrgData;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Stack;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.content.ContentResolver;
import android.content.Context;
import android.text.TextUtils;
import android.util.Pair;

import com.matburt.mobileorg.OrgData.OrgContract.OrgData;
import com.matburt.mobileorg.util.OrgFileNotFoundException;
import com.matburt.mobileorg.util.PreferenceUtils;

public class OrgFileParser {

	private ContentResolver resolver;    
    private OrgDatabase db;
 
    private boolean combineAgenda = false;

    private ParseStack parseStack;
	private StringBuilder payload;
	private OrgFile orgFile;
	private OrgNodeParser orgNodeParser;
	private HashSet<String> excludedTags;
	
	public OrgFileParser(OrgDatabase db, ContentResolver resolver) {
		this.db = db;
		this.resolver = resolver;
	}

	private void init(OrgFile orgFile) {
		orgFile.removeFile(resolver);
		orgFile.addFile(resolver);
		this.orgFile = orgFile;

		this.parseStack = new ParseStack();
		this.parseStack.add(0, orgFile.nodeId, "");
		
		this.payload = new StringBuilder();
		
		this.orgNodeParser = new OrgNodeParser(
				OrgProviderUtils.getTodos(resolver));
	}
	
	public void parse(OrgFile orgFile, BufferedReader breader, Context context) {
		this.combineAgenda = PreferenceUtils.getCombineBlockAgendas();
		this.excludedTags = PreferenceUtils.getExcludedTags();
		
		parse(orgFile, breader);
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

		if(combineAgenda && orgFile.filename.equals(OrgFile.AGENDA_FILE)) {
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
        
		OrgNode node = this.orgNodeParser.parseLine(thisLine, numstars);
		node.tags_inherited = parseStack.getCurrentTags();
		node.fileId = orgFile.id;
		node.parentId = parseStack.getCurrentNodeId();
		long newId = db.fastInsertNode(node);
		parseStack.add(numstars, newId, node.tags);      
    }

	private static final Pattern starPattern = Pattern.compile("^(\\**)\\s");
	private static int numberOfStars(String thisLine) {
		Matcher matcher = starPattern.matcher(thisLine);
		if(matcher.find()) {
			return matcher.end(1) - matcher.start(1);
		} else
			return 0;
	}
	
    
	private class ParseStack {
		private Stack<Pair<Integer, Long>> parseStack;
		private Stack<String> tagStack;

		public ParseStack() {
			this.parseStack = new Stack<Pair<Integer, Long>>();
			this.tagStack = new Stack<String>();
		}
		
		public void add(int level, long nodeId, String tags) {
			parseStack.push(new Pair<Integer, Long>(level, nodeId));
			tagStack.push(stripTags(tags));
		}
		
		private String stripTags(String tags) {
			if (excludedTags == null || TextUtils.isEmpty(tags))
				return tags;
			
			StringBuilder result = new StringBuilder();
			for (String tag: tags.split(":")) {
				if (excludedTags.contains(tag) == false) {
					result.append(tag);
					result.append(":");
				}
			}
			
			if(!TextUtils.isEmpty(result))
				result.deleteCharAt(result.lastIndexOf(":"));
			
			return result.toString();
		}
		
		public void pop() {
			this.parseStack.pop();
			this.tagStack.pop();
		}
		
		public int getCurrentLevel() {
			return parseStack.peek().first;
		}
		
		public long getCurrentNodeId() {
			return parseStack.peek().second;
		}
		
		public String getCurrentTags() {
			return tagStack.peek();
		}
	}
	
	
	public static final String BLOCK_SEPARATOR_PREFIX = "#HEAD#";	
	private void combineBlockAgendas() throws OrgFileNotFoundException {		
		OrgNode agendaFile = OrgProviderUtils.getOrgNodeFromFilename(
				OrgFile.AGENDA_FILE, resolver);
		
		String previousAgendaBlockTitle = "";
		OrgNode previousBlockNode = null;
		
		for(OrgNode node: agendaFile.getChildren(resolver)) {
			if(node.name.indexOf(">") == -1)
				continue;
			
			String agendaBlockName = node.name.substring(0, node.name.indexOf(">"));
			String blockEntryName = node.name.substring(node.name.indexOf(">") + 1);
			
			if(TextUtils.isEmpty(agendaBlockName) == false) { // Is a block agenda
				if(agendaBlockName.equals(previousAgendaBlockTitle) == false) { // Create new node to contain block agenda	
					previousAgendaBlockTitle = agendaBlockName;

					previousBlockNode = new OrgNode();
					previousBlockNode.fileId = agendaFile.fileId;
					previousBlockNode.name = agendaBlockName;
					previousBlockNode.parentId = agendaFile.id;
					previousBlockNode.level = agendaFile.level + 1;
					previousBlockNode.id = db.fastInsertNode(previousBlockNode);
				}
				
				ArrayList<OrgNode> children = node.getChildren(resolver);
				if(blockEntryName.startsWith("Day-agenda") || blockEntryName.startsWith("Week-agenda")) {
					for(OrgNode child: children)
						cloneChildren(child, previousBlockNode, child.name);
				} else
					cloneChildren(node, previousBlockNode, blockEntryName); // Normal cloning
				
				resolver.delete(OrgData.buildIdUri(node.id), null, null);
			}
		}
	}

	private void cloneChildren(OrgNode node, OrgNode parent, String blockTitle) {
		OrgNode blockSeparator = new OrgNode();
		blockSeparator.name = BLOCK_SEPARATOR_PREFIX + blockTitle;
		blockSeparator.fileId = parent.fileId;
		blockSeparator.parentId = parent.id;
		blockSeparator.level = parent.level + 1;
		db.fastInsertNode(blockSeparator);
		
		for(OrgNode child: node.getChildren(resolver)) {
			OrgNode clonedChild = new OrgNode(child);
			clonedChild.parentId = parent.id;
			clonedChild.fileId = parent.fileId;
			clonedChild.level = parent.level + 1;
			long id = db.fastInsertNode(clonedChild);
			db.fastInsertNodePayload(id, child.getPayload());
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
			String[] chksTuple = line.split("  ", 2);
			if(chksTuple.length == 2)
				checksums.put(chksTuple[1], chksTuple[0]);
		}
		return checksums;
	}
	
	private static final String fileMatchPattern = "\\[file:(.*?)\\]\\[(.*?)\\]\\]";
	/**
	 * Parses the file list from index file.
	 * @return HashMap with Filename->Filename Alias
	 */
	public static HashMap<String, String> getFilesFromIndex(String filecontents) {
		Pattern indexOrgFilePattern = Pattern.compile(fileMatchPattern);
		Matcher indexOrgFileMatcher = indexOrgFilePattern.matcher(filecontents);
		HashMap<String, String> allOrgFiles = new HashMap<String, String>();
				
		while (indexOrgFileMatcher.find()) {
			allOrgFiles.put(indexOrgFileMatcher.group(1), indexOrgFileMatcher.group(2));
		}

		return allOrgFiles;
	}
	
	
	private static final Pattern getTodos = Pattern
			.compile("#\\+TODO:([^\\|]+)(\\| ([^\\n]*))*");
	public static ArrayList<HashMap<String, Boolean>> getTodosFromIndex(String filecontents) {
		Matcher m = getTodos.matcher(filecontents);
		ArrayList<HashMap<String, Boolean>> todoList = new ArrayList<HashMap<String, Boolean>>();
		while (m.find()) {
			String lastTodo = "";
			HashMap<String, Boolean> holding = new HashMap<String, Boolean>();
			Boolean isDone = false;
			for (int idx = 1; idx <= m.groupCount(); idx++) {
				if (m.group(idx) != null && m.group(idx).trim().length() > 0) {
					if (m.group(idx).indexOf("|") != -1) {
						isDone = true;
						continue;
					}
					String[] grouping = m.group(idx).trim().split("\\s+");
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
	
	private static final Pattern getPriorities = Pattern
			.compile("#\\+ALLPRIORITIES:([^\\n]+)");
	public static ArrayList<String> getPrioritiesFromIndex(String filecontents) {
		Matcher t = getPriorities.matcher(filecontents);

		ArrayList<String> priorities = new ArrayList<String>();

		if (t.find()) {
			if (t.group(1) != null && t.group(1).trim().length() > 0) {
				String[] grouping = t.group(1).trim().split("\\s+");
				for (String group : grouping)
					priorities.add(group.trim());
			}
		}
		return priorities;
	}
	
	
	private static final Pattern getTags = Pattern.compile("#\\+TAGS:([^\\n]+)");
	public static ArrayList<String> getTagsFromIndex(String filecontents) {
		Matcher matcher = getTags.matcher(filecontents);
		ArrayList<String> tagList = new ArrayList<String>();
		
		if(matcher.find()) {
			String tags = matcher.group(1).trim().replaceAll("[\\{\\}]", "");
			String[] split = tags.split("\\s+");
			
			if(split.length == 1 && split[0].equals(""))
				return tagList;
			
			for(String tag: split)
				tagList.add(tag);
		}
		
		return tagList;
	}
}
