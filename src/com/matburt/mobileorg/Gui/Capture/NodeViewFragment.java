package com.matburt.mobileorg.Gui.Capture;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.content.ActivityNotFoundException;
import android.content.ContentResolver;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.actionbarsherlock.app.SherlockFragment;
import com.matburt.mobileorg.R;
import com.matburt.mobileorg.OrgData.OrgFile;
import com.matburt.mobileorg.OrgData.OrgNode;

public class NodeViewFragment extends SherlockFragment {
	
	private ContentResolver resolver;
	private WebView display;
	private OrgNode node;
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.viewnode, container);
		
		this.display = (WebView) view.findViewById(R.id.viewnode_webview);
		this.display.setWebViewClient(new InternalWebViewClient());
		this.display.getSettings().setBuiltInZoomControls(true);

		return view;
	}
	
	@Override
	public void onStart() {
		super.onStart();
		this.resolver = getActivity().getContentResolver();
		
		EditActivity editActivity = (EditActivity) getActivity();
		this.node = editActivity.getOrgNode();
		this.display.setBackgroundColor(0x00000000);
		
		refreshDisplay();
	}

	private void refreshDisplay() {
		String data;
		
//		if(this.node.id == -1)
			data = "<html><body><font color='white'>" + getString(R.string.node_view_error_loading_node) + "</font></body></html>";
//		else
//			data = convertToHTML();
		this.display.loadDataWithBaseURL(null, data, "text/html", "UTF-8", null);
	}

	private String convertToHTML() {
		int levelOfRecursion = Integer.parseInt(PreferenceManager
				.getDefaultSharedPreferences(getActivity()).getString(
						"viewRecursionMax", "0"));

		String text = nodeToHTMLRecursive(this.node, levelOfRecursion);
		text = convertLinks(text);

		boolean wrapLines = PreferenceManager.getDefaultSharedPreferences(
				getActivity()).getBoolean("viewWrapLines", false);
		if (wrapLines) {
			text = text.replaceAll("\\n\\n", "<br/>\n<br/>\n");		// wrap "paragraphs"

			text = text.replaceAll("\\n(\\s*[-\\+])", "<br/>\n$1");		// wrap unordered lists
			text = text.replaceAll("\\n(\\s*\\d+[\\)\\.])", "<br/>\n$1"); // wrap ordered lists
			
			text = text.replaceAll("((\\s*\\|[^\\n]*\\|\\s*(?:<br/>)?\\n)+)", "<pre>$1</pre>");

			Log.d("MobileOrg", text);
			text = "<html><body><font color='white'>" + text + "</font></body></html>";
		} else {
			text = text.replaceAll("\\n", "<br/>\n");
			text = "<html><body><font color='white'><pre>" + text + "</pre></font></body></html>";
		}

		return text;
	}

	private String convertLinks(String text) {
		Pattern linkPattern = Pattern.compile("\\[\\[([^\\]]*)\\]\\[([^\\]]*)\\]\\]");
		Matcher matcher = linkPattern.matcher(text);
		text = matcher.replaceAll("<a href=\"$1\">$2</a>");
		
		Pattern urlPattern = Pattern.compile("[^(?:<a href=\"\\s*)](http[s]?://\\S+)");
		matcher = urlPattern.matcher(text);
		text = matcher.replaceAll("<a href=\"$1\">$1</a>");
		
		return text;
	}

	private class InternalWebViewClient extends WebViewClient {
		@Override
		public boolean shouldOverrideUrlLoading(WebView view, String url) {
			try {
				URL urlObj = new URL(url);
				if (urlObj.getProtocol().equals("file")) {
					handleInternalOrgUrl(url);
					return true;
				}
			} catch (MalformedURLException e) {
				Log.d("MobileOrg", "Malformed url :" + url);
			}

			Intent intent = new Intent(Intent.ACTION_VIEW);
			intent.setData(Uri.parse(url));
			try {
				startActivity(intent);
			} catch(ActivityNotFoundException e) {}
			return true;
		}

		@Override
		public void onReceivedError(WebView view, int errorCode,
				String description, String failingUrl) {
		}

	}
	
	private void handleInternalOrgUrl(String url) {		
		long nodeId = getNodeFromPath(url);
				
		Intent intent = new Intent(getActivity(), NodeViewFragment.class);
		intent.putExtra("node_id", nodeId);
		startActivity(intent);
	}

	private long getNodeFromPath(String path) {
		String filename = path.substring("file://".length(), path.length());
		
		// TODO Handle links to headings instead of simply stripping it out
		if(filename.indexOf(":") > -1)
			filename = filename.substring(0, filename.indexOf(":"));
				
		OrgFile file = new OrgFile(filename, resolver);
		return file.nodeId;
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
			if (PreferenceManager.getDefaultSharedPreferences(getActivity()).getBoolean(
					"viewApplyFormating", true))
				payload = applyFormating(payload);
			result.append(payload);
			result.append("\n<br/>\n");
		}

		result.append("<br/>\n");
		return result.toString();
	}
	
	private String getFormatingRegex(String character, String tag, String text) {
		return text.replaceAll(
				"(\\s)\\" + character + 
				"(\\S[\\S\\s]*\\S)" + 
				"\\" + character + "(\\s)"
				, "$1<" + tag + ">$2</" + tag + ">$3");
	}
	
	private String applyFormating(String text) {
		text = getFormatingRegex("*", "b", text);
		text = getFormatingRegex("/", "i", text);
		text = getFormatingRegex("_", "u", text);
		text = getFormatingRegex("+", "strike", text);

		return text;
	}

}