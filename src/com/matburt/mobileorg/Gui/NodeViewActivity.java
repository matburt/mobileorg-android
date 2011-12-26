package com.matburt.mobileorg.Gui;

import java.net.MalformedURLException;
import java.net.URL;

import android.app.Activity;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.matburt.mobileorg.R;
import com.matburt.mobileorg.Parsing.MobileOrgApplication;
import com.matburt.mobileorg.Parsing.Node;

public class NodeViewActivity extends Activity {
	private WebView display;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.viewnode);
		
		this.display = (WebView) this.findViewById(R.id.viewnode_webview);
		display.setWebViewClient(new InternalWebViewClient());
		display.setWebChromeClient(new InternalWebChromeClient());
		display.getSettings().setBuiltInZoomControls(true);

		String data = convertToHTML();
		this.display.loadData(data, "text/html", "UTF-8");
	}


	private String convertToHTML() {
		MobileOrgApplication appInst = (MobileOrgApplication) this
				.getApplication();
		Node node = appInst.nodestackTop();
		
		this.setTitle(node.name);
		
		
//		String level = PreferenceManager.getDefaultSharedPreferences(this)
//				.getString("viewRecursionMax", "0");
		int levelOfRecursion = 0; //Integer.getInteger(level);
		
		String text = nodeToHTMLRecursive(node, levelOfRecursion, true);
		text = convertLinks(text);
		
		String result;
		boolean wrapLines = PreferenceManager.getDefaultSharedPreferences(this)
				.getBoolean("viewWrapLines", false);
		if (wrapLines) {
			text = text.replaceAll("\\n\\n", "<br/>\n<br/>\n");
			text = text.replaceAll("\\n-", "<br/>\n-");
			result = "<html><body>" + text + "</body></html>";
		} else {
			text = text.replaceAll("\\n", "<br/>\n");
			result = "<html><body><pre>" + text + "</pre></body></html>";
		}
		
		return result;
	}

	private String nodeToHTMLRecursive(Node node, int level, boolean hideHeading) {
		StringBuilder result = new StringBuilder();
		result.append(nodeToHTML(node, level, hideHeading));
		
		if(level <= 0) {
			if(node.hasChildren())
				result.append("<b>...</b><br/>");

			return result.toString();
		}
		level--;

		for(Node child: node.getChildren())
			result.append(nodeToHTMLRecursive(child, level, false));
			
		return result.toString();
	}
	
	private String nodeToHTML(Node node, int headingLevel, boolean hideHeading) {
		StringBuilder result = new StringBuilder();

		if (!hideHeading) {
			int fontSize = 3 + headingLevel;
			result.append("<font size=\"");
			result.append(fontSize);
			result.append("\"> <b>");
			result.append(node.name);
			result.append("</b></font> <hr />");
		}

		if(!node.payload.getContent().equals("")) {
			result.append(node.payload.getContent());
			result.append("<br/>\n");
		}
		
		result.append("<br/>\n");
		return result.toString();
	}

	private String convertLinks(String text) {
		int i1 = 0, i2 = 0;
		while (true) {
			i1 = text.indexOf("[[", i2 + 1);
			if (i1 < 0)
				break;
			i2 = text.indexOf("]]", i1);
			if (i2 < 0)
				break;
			int i3 = text.indexOf("][", i1);
			String linkUrl;
			String linkText;
			if (i3 >= 0 && i3 < i2) {
				linkUrl = text.substring(i1 + 2, i3);
				linkText = text.substring(i3 + 2, i2);
			} else {
				linkUrl = text.substring(i1 + 2, i2);
				linkText = linkUrl;

			}
			text = text.substring(0, i1) + "<a href=\"" + linkUrl + "\">"
					+ linkText + "</a>" + text.substring(i2 + 2);
		}
		return text;
	}

	private static class InternalWebViewClient extends WebViewClient {

		@Override
		public boolean shouldOverrideUrlLoading(WebView view, String urlStr) {
			try {
				URL url = new URL(urlStr);
				if ("file".equals(url.getProtocol())) {
					return true;
				}
			} catch (MalformedURLException e) {
			}
			return false;
		}

		@Override
		public void onReceivedError(WebView view, int errorCode,
				String description, String failingUrl) {
		}

	}

	private static class InternalWebChromeClient extends WebChromeClient {
	}

}