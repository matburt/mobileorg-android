package com.matburt.mobileorg.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.content.ContentResolver;
import android.content.Context;
import android.preference.PreferenceManager;

import com.matburt.mobileorg.Gui.Theme.DefaultTheme;
import com.matburt.mobileorg.OrgData.OrgNode;

public class OrgNode2Html {

	private ContentResolver resolver;
	
	public boolean wrapLines = false;
	public boolean viewApplyFormating = true;

	public String fontColor = "white";

	public OrgNode2Html(ContentResolver resolver, Context context) {
		this.resolver = resolver;
		setupConfig(context);
	}
	
	private void setupConfig(Context context) {
		this.wrapLines = PreferenceManager.getDefaultSharedPreferences(context)
				.getBoolean("viewWrapLines", false);
		this.viewApplyFormating = PreferenceManager.getDefaultSharedPreferences(context).getBoolean(
				"viewApplyFormating", true);
		
		this.fontColor = DefaultTheme.getTheme(context).defaultFontColor;
	}
	
	public String toHTML(OrgNode node) {
		return toHTML(node, 0);
	}
	
	public String toHTML(OrgNode node, int levelOfRecursion) {
		String text = nodeToHTMLRecursive(node, levelOfRecursion);
		return convertToHTML(text);
	}
			
	public String toHTML(String text) {

		return convertToHTML(text);
	}
	public String payloadToHTML(OrgNode node) {
		return convertToHTML(node.getCleanedPayload());
	}
	
	private String convertToHTML(String text) {
		if(text == null || text.trim().equals(""))
			return "<html><body><font color='" + fontColor + "'><pre>" + text + "</pre></font></body></html>";
		
		text = convertLinks(text);

		if (wrapLines) {
			text = text.replaceAll("\\n\\n", "<br/>\n<br/>\n");		// wrap "paragraphs"

			text = text.replaceAll("\\n(\\s*[-\\+])", "<br/>\n$1");		// wrap unordered lists
			text = text.replaceAll("\\n(\\s*\\d+[\\)\\.])", "<br/>\n$1"); // wrap ordered lists
			
			text = text.replaceAll("((\\s*\\|[^\\n]*\\|\\s*(?:<br/>)?\\n)+)", "<pre>$1</pre>");

			text = "<html><body><font color='" + fontColor + "'>" + text + "</font></body></html>";
		} else {
			text = text.replaceAll("\\n", "<br/>\n");
			text = "<html><body><font color='" + fontColor + "'><pre>" + text + "</pre></font></body></html>";
		}

		return text;
	}

	
	private String nodeToHTMLRecursive(OrgNode node, int level) {
		StringBuilder result = new StringBuilder();
		result.append(nodeToHTML(node, level));

		if (level <= 0)
			return result.toString();
		level--;

		for (OrgNode child : node.getChildren(resolver))
			result.append(nodeToHTMLRecursive(child, level));
		
		return result.toString();
	}
	
	private String nodeToHTML(OrgNode node, int headingLevel) {
		StringBuilder result = new StringBuilder();

		int fontSize = 3 + headingLevel;
		result.append("<font size=\"");
		result.append(fontSize);
		result.append("\"> <b>");
		result.append(node.name);
		
		if(headingLevel == 0 && node.hasChildren(resolver))
			result.append("...");
		
		result.append("</b></font> <hr />");
		
		if (!node.getCleanedPayload().equals("")) {
			String payload = node.getCleanedPayload();
			if (viewApplyFormating)
				payload = applyFormating(payload);
			result.append(payload);
			result.append("\n<br/>\n");
		}

		result.append("<br/>\n");
		return result.toString();
	}

	private static String convertLinks(String text) {
		Pattern linkPattern = Pattern.compile("\\[\\[([^\\]]*)\\]\\[([^\\]]*)\\]\\]");
		Matcher matcher = linkPattern.matcher(text);
		text = matcher.replaceAll("<a href=\"$1\">$2</a>");
		
		Pattern urlPattern = Pattern.compile("[^(?:<a href=\"\\s*)](http[s]?://\\S+)");
		matcher = urlPattern.matcher(text);
		text = matcher.replaceAll("<a href=\"$1\">$1</a>");
		
		return text;
	}
	
	private static String applyFormating(String text) {
		text = getFormatingRegex("*", "b", text);
		text = getFormatingRegex("/", "i", text);
		text = getFormatingRegex("_", "u", text);
		text = getFormatingRegex("+", "strike", text);
		return text;
	}
	
	private static String getFormatingRegex(String character, String tag, String text) {
		return text.replaceAll(
				"(\\s)\\" + character + 
				"(\\S[\\S\\s]*\\S)" + 
				"\\" + character + "(\\s)"
				, "$1<" + tag + ">$2</" + tag + ">$3");
	}

}
