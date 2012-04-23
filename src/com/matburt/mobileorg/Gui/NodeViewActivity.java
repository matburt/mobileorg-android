package com.matburt.mobileorg.Gui;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.FragmentActivity;
import android.support.v4.view.Window;
import android.util.Log;
import android.view.MenuInflater;
import android.view.WindowManager;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.matburt.mobileorg.R;
import com.matburt.mobileorg.Gui.Capture.EditActivity;
import com.matburt.mobileorg.Parsing.MobileOrgApplication;
import com.matburt.mobileorg.Parsing.NodeWrapper;
import com.matburt.mobileorg.Synchronizers.Synchronizer;

public class NodeViewActivity extends FragmentActivity {
	private WebView display;
	private MobileOrgApplication appInst;
	private SynchServiceReceiver syncReceiver;
	private long node_id;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		SharedPreferences appSettings = PreferenceManager
				.getDefaultSharedPreferences(getBaseContext());
		if (appSettings.getBoolean("fullscreen", true)) {
			requestWindowFeature(Window.FEATURE_NO_TITLE);
			getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
					WindowManager.LayoutParams.FLAG_FULLSCREEN);
		}
		
		setContentView(R.layout.viewnode);
		
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
			data = "<html><body>" + getString(R.string.node_view_error_loading_node) + "</body></html>";
		else
			data = convertToHTML();
		this.display.loadDataWithBaseURL(null, data, "text/html", "UTF-8", null);
	}
	
	@Override
	public boolean onCreateOptionsMenu(android.support.v4.view.Menu menu) {
		MenuInflater inflater = getMenuInflater();
	    inflater.inflate(R.menu.nodeview, menu);
	    
	    if(this.appInst.getDB().isNodeEditable(node_id) == false)
	    	menu.findItem(R.id.viewmenu_edit).setVisible(false);

		return true;
	}

	@Override
	public boolean onOptionsItemSelected(android.support.v4.view.MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
			finish();
			break;
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
		Intent intent = new Intent(this, EditActivity.class);
		intent.putExtra("actionMode", EditActivity.ACTIONMODE_EDIT);
		intent.putExtra("node_id", this.node_id);
		startActivity(intent);
	}
	
	private void runEditNewNodeActivity() {
		Intent intent = new Intent(this, EditActivity.class);
		intent.putExtra("actionMode", EditActivity.ACTIONMODE_CREATE);
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

			text = text.replaceAll("\\n(\\s*[-\\+])", "<br/>\n$1");		// wrap unordered lists
			text = text.replaceAll("\\n(\\s*\\d+[\\)\\.])", "<br/>\n$1"); // wrap ordered lists
			
			text = text.replaceAll("((\\s*\\|[^\\n]*\\|\\s*(?:<br/>)?\\n)+)", "<pre>$1</pre>");

			Log.d("MobileOrg", text);
			text = "<html><body>" + text + "</body></html>";
		} else {
			text = text.replaceAll("\\n", "<br/>\n");
			text = "<html><body><pre>" + text + "</pre></body></html>";
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

		for (NodeWrapper child : node.getChildren()) {
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
			String payload = node.getCleanedPayload();
			if (PreferenceManager.getDefaultSharedPreferences(this).getBoolean(
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