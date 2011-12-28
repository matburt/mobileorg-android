package com.matburt.mobileorg.Gui;

import java.net.MalformedURLException;
import java.net.URL;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.matburt.mobileorg.R;
import com.matburt.mobileorg.Parsing.MobileOrgApplication;
import com.matburt.mobileorg.Parsing.Node;
import com.matburt.mobileorg.Synchronizers.Synchronizer;

public class NodeViewActivity extends Activity {
	private WebView display;
	private MobileOrgApplication appInst;
	private SynchServiceReceiver syncReceiver;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.viewnode);

		this.display = (WebView) this.findViewById(R.id.viewnode_webview);
		display.setWebViewClient(new InternalWebViewClient());
		display.setWebChromeClient(new InternalWebChromeClient());
		display.getSettings().setBuiltInZoomControls(true);

		this.appInst = (MobileOrgApplication) this.getApplication();
		
        this.syncReceiver = new SynchServiceReceiver();
		registerReceiver(this.syncReceiver, new IntentFilter(
				Synchronizer.SYNC_UPDATE));
        
		refreshDisplay();
	}
	
	@Override
	public void onDestroy() {
		unregisterReceiver(this.syncReceiver);
		super.onDestroy();
	}
	
	private void refreshDisplay() {
		String data = convertToHTML();
		this.display.loadData(data, "text/html", "UTF-8");
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
	    inflater.inflate(R.menu.nodeview_menu, menu);
	    
	    if(this.appInst.nodestackSize() <= 2)
	    	menu.findItem(R.id.viewmenu_edit).setVisible(false);

		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.viewmenu_edit:
			runEditNodeActivity();
			break;
			
		case R.id.viewmenu_capture:
			runEditNewNodeActivity();
			break;
		}
		return false;
	}
	
	private void runEditNodeActivity() {
		Intent intent = new Intent(this, NodeEditActivity.class);
		intent.putExtra("actionMode", NodeEditActivity.ACTIONMODE_EDIT);
		startActivity(intent);
	}
	
	private void runEditNewNodeActivity() {
		Intent intent = new Intent(this, NodeEditActivity.class);
		intent.putExtra("actionMode", NodeEditActivity.ACTIONMODE_CREATE);
		startActivity(intent);
	}

	private String convertToHTML() {
		Node node = this.appInst.nodestackTop();

		this.setTitle(node.name);

		int levelOfRecursion = Integer.parseInt(PreferenceManager
				.getDefaultSharedPreferences(this).getString(
						"viewRecursionMax", "0"));

		String text = nodeToHTMLRecursive(node, levelOfRecursion);
		text = convertLinks(text);

		boolean wrapLines = PreferenceManager.getDefaultSharedPreferences(this)
				.getBoolean("viewWrapLines", false);
		if (wrapLines) {
			// TODO Improve custom line wrapping
			text = text.replaceAll("\\n\\n", "<br/>\n<br/>\n");
			text = text.replaceAll("\\n-", "<br/>\n-");
			text = "<html><body>" + text + "</body></html>";
		} else {
			text = text.replaceAll("\\n", "<br/>\n");
			text = "<html><body><pre>" + text + "</pre></body></html>";
		}

		return text;
	}

	private String nodeToHTMLRecursive(Node node, int level) {
		StringBuilder result = new StringBuilder();
		result.append(nodeToHTML(node, level));

		if (level <= 0) {
			if (node.hasChildren())
				result.append("<b>...</b><br/>");

			return result.toString();
		}
		level--;

		for (Node child : node.getChildren())
			result.append(nodeToHTMLRecursive(child, level));

		return result.toString();
	}

	private String nodeToHTML(Node node, int headingLevel) {
		StringBuilder result = new StringBuilder();

		int fontSize = 3 + headingLevel;
		result.append("<font size=\"");
		result.append(fontSize);
		result.append("\"> <b>");
		result.append(node.name);
		result.append("</b></font> <hr />");

		if (!node.payload.getContent().equals("")) {
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

	private class SynchServiceReceiver extends BroadcastReceiver {
		@Override
		public void onReceive(Context context, Intent intent) {
			if (intent.getBooleanExtra(Synchronizer.SYNC_DONE, false)) {
				refreshDisplay();
			}
		}
	}
}