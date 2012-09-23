package com.matburt.mobileorg.test.OrgData;

import java.util.HashSet;

import android.test.AndroidTestCase;

import com.matburt.mobileorg.OrgData.OrgNode;

public class OrgNodeParserTest extends AndroidTestCase {
	public void testParseLineIntoNodeSimple() {
		OrgNode node = new OrgNode();
		node.name = "my simple test";
		node.todo = "";
		node.level = 3;
		OrgNode parsedNode = new OrgNode();
		final String testHeading = "*** my simple test";
		HashSet<String> todos = new HashSet<String>();
		parsedNode.parseLine(testHeading, 3, todos);
		
		assertEquals(node.todo, parsedNode.todo);
		assertEquals(node.name, parsedNode.name);
	}
	
	public void testParseLineIntoNodeWithTodo() {
		OrgNode node = new OrgNode();
		node.name = "my simple test";
		node.todo = "TODO";
		node.level = 3;
		OrgNode parsedNode = new OrgNode();
		final String testHeading = "*** TODO my simple test";
		HashSet<String> todos = new HashSet<String>();
		todos.add(node.todo);
		parsedNode.parseLine(testHeading, 3, todos);
		
		assertEquals(node.todo, parsedNode.todo);
		assertEquals(node.name, parsedNode.name);
	}
	
	public void testParseLineIntoNodeInvalidTodo() {
		OrgNode node = new OrgNode();
		node.name = "BLA my simple test";
		node.todo = "";
		node.level = 3;
		OrgNode parsedNode = new OrgNode();
		final String testHeading = "*** BLA my simple test";
		HashSet<String> todos = new HashSet<String>();
		parsedNode.parseLine(testHeading, 3, todos);
		
		assertTrue(node.equals(parsedNode));
	}
	
	public void testParseLineIntoNodeComplicatedTodo() {
		OrgNode node = new OrgNode();
		node.name = "my simple test";
		node.todo = "find_me";
		node.level = 3;
		OrgNode parsedNode = new OrgNode();
		final String testHeading = "*** find_me my simple test";
		HashSet<String> todos = new HashSet<String>();
		todos.add(node.todo);
		parsedNode.parseLine(testHeading, 3, todos);
		
		assertEquals(node.todo, parsedNode.todo);
		assertEquals(node.name, parsedNode.name);
	}
	
	public void testParseLineIntoNodeTags() {
		OrgNode node = new OrgNode();
		node.name = "Archive";
		node.level = 3;
		node.tags = "tag1:tag2";
		OrgNode parsedNode = new OrgNode();
		final String testHeading = "*** Archive      :tag1:tag2:";
		HashSet<String> todos = new HashSet<String>();
		parsedNode.parseLine(testHeading, 3, todos);
		
		assertEquals(node.todo, parsedNode.todo);
		assertEquals(node.tags, parsedNode.tags);
		assertEquals(node.name, parsedNode.name);
	}
	
	
	public void testParseLineIntoNodeWithSimpleScheduled() {
		OrgNode node = new OrgNode();
		node.name = "my simple test";
		node.todo = "TODO";
		node.level = 3;
		OrgNode parsedNode = new OrgNode();
		final String testHeading = "***  TODO my simple test";
		HashSet<String> todos = new HashSet<String>();
		todos.add(node.todo);
		parsedNode.parseLine(testHeading, 3, todos);
		
		assertEquals(node.todo, parsedNode.todo);
		assertEquals(node.name, parsedNode.name);
	}
	
	public void testParseLineIntoNodeAgendaTitle() {
		final String expectedTitle = "Home Core>Home";
		final String testHeading = "* Home <after>KEYS=h#2 TITLE: Home Core</after>";
		HashSet<String> todos = new HashSet<String>();

		OrgNode parsedNode = new OrgNode();
		parsedNode.parseLine(testHeading, 1, todos);
		
		assertEquals(expectedTitle, parsedNode.name);
	}
	
	public void testParseLineIntoNodeAgendaTitleWithoutSpace() {
		final String expectedTitle = "Home Core>Agenda";
		final String testHeading = "* Agenda<after>KEYS=h#2 TITLE: Home Core</after>";
		HashSet<String> todos = new HashSet<String>();

		OrgNode parsedNode = new OrgNode();
		parsedNode.parseLine(testHeading, 1, todos);
		
		assertEquals(expectedTitle, parsedNode.name);
	}
}
