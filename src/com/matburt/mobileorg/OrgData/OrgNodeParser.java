package com.matburt.mobileorg.OrgData;

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.util.Log;

public class OrgNodeParser {
	private static final int TODO_GROUP = 1;
	private static final int PRIORITY_GROUP = 2;
	private static final int NAME_GROUP = 3;
	private static final int TAGS_GROUP = 4;
	private static final int AFTER_GROUP = 5;

	private Pattern pattern;

	private static final String patternStart = "^\\s?";
	private static final String patternEnd = "(?:\\[\\#([^]]+)\\]\\s)?" + // Priority
			"(.*?)" + // Title
			"\\s*" + "(?::([^\\s]+):)?" + // Tags (without trailing spaces)
			"(?:\\s*[!\\*])*" + // Habits
			"(?:<before>.*</before>)?" + // Before
			"(?:<after>.*TITLE:(.*)</after>)?" + // After
			"$"; // End of line

	public OrgNodeParser(ArrayList<String> todos) {
		final String patternString = patternStart + "(?:("
				+ getTodoRegex(todos) + ")\\s)?" + patternEnd;
		this.pattern = Pattern.compile(patternString);
	}

	private String getTodoRegex(ArrayList<String> todos) {
		if (todos.isEmpty())
			return "";

		StringBuilder result = new StringBuilder();
		for (String todo : todos)
			result.append(todo).append("|");
		result.deleteCharAt(result.length() - 1);

		return result.toString();
	}

	public OrgNode parseLine(final String line, int numberOfStars) {
		OrgNode node = new OrgNode();
		node.level = numberOfStars;

		Matcher matcher = pattern.matcher(line);
		matcher.region(numberOfStars + 1, line.length());
		if (matcher.find()) {
			if (matcher.group(TODO_GROUP) != null)
				node.todo = matcher.group(TODO_GROUP);

			node.name = matcher.group(NAME_GROUP);

			if (matcher.group(PRIORITY_GROUP) != null)
				node.priority = matcher.group(PRIORITY_GROUP);

			if (matcher.group(TAGS_GROUP) != null)
				node.tags = matcher.group(TAGS_GROUP);

			if (matcher.group(AFTER_GROUP) != null)
				node.name = matcher.group(AFTER_GROUP).trim()
						+ ">" + node.name.trim();

		} else {
			Log.w("MobileOrg", "Title not matched: " + line);
			node.name = line;
		}

		return node;
	}
}
