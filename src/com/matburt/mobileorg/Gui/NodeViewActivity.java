package com.matburt.mobileorg.Gui;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.matburt.mobileorg.R;
import com.matburt.mobileorg.Parsing.MobileOrgApplication;
import com.matburt.mobileorg.Parsing.NodeWrapper;
import com.matburt.mobileorg.Synchronizers.Synchronizer;

public class NodeViewActivity extends Activity {
	private WebView display;
	private MobileOrgApplication appInst;
	private SynchServiceReceiver syncReceiver;
	private long node_id;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.viewnode);
		OutlineActivity.setupActionbar(this);
		
		Intent intent = getIntent();
		this.node_id = intent.getLongExtra("node_id", -1);
		
		this.display = (WebView) this.findViewById(R.id.viewnode_webview);
		this.display.setWebViewClient(new InternalWebViewClient());
		this.display.getSettings().setBuiltInZoomControls(true);

		this.appInst = (MobileOrgApplication) this.getApplication();
		
        this.syncReceiver = new SynchServiceReceiver();
		registerReceiver(this.syncReceiver, new IntentFilter(
				Synchronizer.SYNC_UPDATE));
        
		refreshDisplay();
	}
	
	@Override
	public void onDestroy() {
		unregisterReceiver(this.syncReceiver);
		this.display.destroy();
		super.onDestroy();
	}
	
	private void refreshDisplay() {
		String data;
		
		if(this.node_id == -1)
			data = "<html><body>" + "Error loading node" + "</body></html>";
		else
			data = convertToHTML();
		this.display.loadDataWithBaseURL(null, data, "text/html", "UTF-8", null);
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
	    inflater.inflate(R.menu.nodeview_menu, menu);
	    
	    if(this.appInst.getDB().isNodeEditable(node_id) == false)
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
		intent.putExtra("node_id", this.node_id);
		startActivity(intent);
	}
	
	private void runEditNewNodeActivity() {
		Intent intent = new Intent(this, NodeEditActivity.class);
		intent.putExtra("actionMode", NodeEditActivity.ACTIONMODE_CREATE);
		startActivity(intent);
	}

	private String convertToHTML() {
		int levelOfRecursion = Integer.parseInt(PreferenceManager
				.getDefaultSharedPreferences(getApplicationContext()).getString(
						"viewRecursionMax", "0"));

		String text = nodeToHTMLRecursive(
				new NodeWrapper(node_id, appInst.getDB()), levelOfRecursion);
		text = convertLinks(text);

		boolean wrapLines = PreferenceManager.getDefaultSharedPreferences(
				getApplicationContext()).getBoolean("viewWrapLines", false);
		if (wrapLines) {
			text = text.replaceAll("\\n\\n", "<br/>\n<br/>\n");		// wrap "paragraphs"
			text = text.replaceAll("\\n(\\s*\\|)", "<br/>\n$1");		// wrap tables

			text = text.replaceAll("\\n(\\s*[-\\+])", "<br/>\n$1");		// wrap unordered lists
			text = text.replaceAll("\\n(\\s*\\d+[\\)\\.])", "<br/>\n$1"); // wrap ordered lists
			
			text = "<html><body>" + text + "</body></html>";
		} else {
			text = text.replaceAll("\\n", "<br/>\n");
			text = "<html><body><pre>" + text + "</pre></body></html>";
		}

		return text;
	}
	
	
	@SuppressWarnings("unused")
	private String fancyLists(String text) {
		try {
			BufferedReader reader = new BufferedReader(new StringReader(text));
			StringBuilder newText = new StringBuilder();
			String line;
			int listLevel = 0;
			while((line = reader.readLine()) != null) {
				Log.d("MobileOrg", "Line: " + line);
				if(line.startsWith("- ")) {
					if(listLevel == 0) {
						line = "<ul>\n" + "<li>" + line + "</li>";
						listLevel++;
					} else
						line = "<li>" + line + "</li>";
				}
				else {
					while(listLevel > 0) {
						listLevel--;
						line = "</ul>" + line;
					}
				}
				newText.append(line);
			}
			
			Log.d("MobileOrg", "Result :\n" + newText.toString());
			text = newText.toString();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
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
		long nodeId = appInst.getDB().getNodeFromPath(url);
				
		Intent intent = new Intent(this, NodeViewActivity.class);
		intent.putExtra("node_id", nodeId);
		startActivity(intent);
	}

	private class SynchServiceReceiver extends BroadcastReceiver {
		@Override
		public void onReceive(Context context, Intent intent) {
			if (intent.getBooleanExtra(Synchronizer.SYNC_DONE, false)) {
				refreshDisplay();
			}
		}
	}

	private String nodeToHTMLRecursive(NodeWrapper node, int level) {
		StringBuilder result = new StringBuilder();
		result.append(nodeToHTML(node, level));

		if (level <= 0)
			return result.toString();
		level--;

		for (NodeWrapper child : node.getChildren(appInst.getDB())) {
			result.append(nodeToHTMLRecursive(child, level));
			child.close();
		}
		
		node.close();
		return result.toString();
	}
	
	private String nodeToHTML(NodeWrapper node, int headingLevel) {
		StringBuilder result = new StringBuilder();

		int fontSize = 3 + headingLevel;
		result.append("<font size=\"");
		result.append(fontSize);
		result.append("\"> <b>");
		result.append(node.getName());
		
		if(headingLevel == 0 && this.appInst.getDB().hasNodeChildren(node_id))
			result.append("...");
		
		result.append("</b></font> <hr />");

		if (!node.getCleanedPayload().equals("")) {
			result.append(node.getCleanedPayload());
			result.append("\n<br/>");
		}

		result.append("\n<br/>\n");
		return result.toString();
	}

}