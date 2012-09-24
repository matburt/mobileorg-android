package com.matburt.mobileorg.test.OrgData;

import java.util.ArrayList;

import android.test.AndroidTestCase;

import com.matburt.mobileorg.OrgData.OrgNode;
import com.matburt.mobileorg.OrgData.OrgNodeParser;

public class OrgNodeParserTest extends AndroidTestCase {
	public void testParseLineIntoNodeSimple() {
		OrgNode node = new OrgNode();
		node.name = "my simple test";
		node.todo = "";
		node.level = 3;
		final String testHeading = "*** my simple test";
		
		OrgNodeParser orgNodeParser = new OrgNodeParser( new ArrayList<String>());
		OrgNode parsedNode = orgNodeParser.parseLine(testHeading, 3);
		
		assertEquals(node.todo, parsedNode.todo);
		assertEquals(node.name, parsedNode.name);
	}
	
	public void testParseLineIntoNodeWithTodo() {
		OrgNode node = new OrgNode();
		node.name = "my simple test";
		node.todo = "TODO";
		node.level = 3;
		final String testHeading = "*** TODO my simple test";
		
		ArrayList<String> todos = new ArrayList<String>();
		todos.add(node.todo);
		OrgNodeParser orgNodeParser = new OrgNodeParser(todos);
		OrgNode parsedNode = orgNodeParser.parseLine(testHeading, 3);
		
		assertEquals(node.todo, parsedNode.todo);
		assertEquals(node.name, parsedNode.name);
	}

	public void testParseLineIntoNodeInvalidTodo() {
		OrgNode node = new OrgNode();
		node.name = "BLA my simple test";
		node.todo = "";
		node.level = 3;
		final String testHeading = "*** BLA my simple test";
		
		OrgNodeParser orgNodeParser = new OrgNodeParser( new ArrayList<String>());
		OrgNode parsedNode = orgNodeParser.parseLine(testHeading, 3);
		
		assertEquals(node.todo, parsedNode.todo);
		assertEquals(node.name, parsedNode.name);
	}
	
	public void testParseLineIntoNodeComplicatedTodo() {
		OrgNode node = new OrgNode();
		node.name = "my simple test";
		node.todo = "find_me";
		node.level = 3;
		final String testHeading = "*** find_me my simple test";
		
		ArrayList<String> todos = new ArrayList<String>();
		todos.add(node.todo);
		OrgNodeParser orgNodeParser = new OrgNodeParser(todos);
		OrgNode parsedNode = orgNodeParser.parseLine(testHeading, 3);
		
		assertEquals(node.todo, parsedNode.todo);
		assertEquals(node.name, parsedNode.name);
	}
	
	public void testParseLineIntoNodeLinkTitle() {
		OrgNode node = new OrgNode();
		node.name = "[[MobileOrg][MobileOrg]]";
		node.todo = "";
		node.level = 3;
		final String testHeading = "*** [[MobileOrg][MobileOrg]]";
		
		OrgNodeParser orgNodeParser = new OrgNodeParser(new ArrayList<String>());
		OrgNode parsedNode = orgNodeParser.parseLine(testHeading, 3);
		
		assertEquals(node.todo, parsedNode.todo);
		assertEquals(node.name, parsedNode.name);
	}
	
	public void testParseLineIntoNodePriority() {
		OrgNode node = new OrgNode();
		node.name = "my todo";
		node.todo = "TODO";
		node.priority = "A";
		node.level = 3;
		final String testHeading = "*** TODO [#A] my todo";
		
		ArrayList<String> todos = new ArrayList<String>();
		todos.add(node.todo);
		OrgNodeParser orgNodeParser = new OrgNodeParser(todos);
		OrgNode parsedNode = orgNodeParser.parseLine(testHeading, 3);
		
		assertEquals(node.todo, parsedNode.todo);
		assertEquals(node.name, parsedNode.name);
		assertEquals(node.priority, parsedNode.priority);
	}
	
	public void testParseLineIntoNodeTags() {
		OrgNode node = new OrgNode();
		node.name = "Archive";
		node.level = 3;
		node.tags = "tag1:tag2";
		final String testHeading = "*** Archive      :tag1:tag2:";
		
		OrgNodeParser orgNodeParser = new OrgNodeParser(new ArrayList<String>());
		OrgNode parsedNode = orgNodeParser.parseLine(testHeading, 3);
		
		assertEquals(node.todo, parsedNode.todo);
		assertEquals(node.tags, parsedNode.tags);
		assertEquals(node.name, parsedNode.name);
	}
	
	
	public void testParseLineIntoNodeWithSimpleScheduled() {
		OrgNode node = new OrgNode();
		node.name = "my simple test";
		node.todo = "TODO";
		node.level = 3;
		final String testHeading = "***  TODO my simple test";
		ArrayList<String> todos = new ArrayList<String>();
		todos.add(node.todo);
		OrgNodeParser orgNodeParser = new OrgNodeParser(todos);
		OrgNode parsedNode = orgNodeParser.parseLine(testHeading, 3);
		
		assertEquals(node.todo, parsedNode.todo);
		assertEquals(node.name, parsedNode.name);
	}
	
	public void testParseLineIntoNodeAgendaTitle() {
		final String expectedTitle = "Home Core>Home";
		final String testHeading = "* Home <after>KEYS=h#2 TITLE: Home Core</after>";

		OrgNodeParser orgNodeParser = new OrgNodeParser(new ArrayList<String>());
		OrgNode parsedNode = orgNodeParser.parseLine(testHeading, 1);
		
		assertEquals(expectedTitle, parsedNode.name);
	}
	
	public void testParseLineIntoNodeAgendaTitleWithoutSpace() {
		final String expectedTitle = "Home Core>Agenda";
		final String testHeading = "* Agenda<after>KEYS=h#2 TITLE: Home Core</after>";

		OrgNodeParser orgNodeParser = new OrgNodeParser(new ArrayList<String>());
		OrgNode parsedNode = orgNodeParser.parseLine(testHeading, 1);
		
		assertEquals(expectedTitle, parsedNode.name);
	}
}
