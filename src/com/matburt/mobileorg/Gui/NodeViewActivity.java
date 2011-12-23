package com.matburt.mobileorg.Gui;

import java.net.MalformedURLException;
import java.net.URL;

import android.app.Activity;
import android.os.Bundle;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.matburt.mobileorg.R;
import com.matburt.mobileorg.Parsing.MobileOrgApplication;
import com.matburt.mobileorg.Parsing.Node;

public class NodeViewActivity extends Activity {
	private WebView orgDisplay;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.viewnode);
		this.orgDisplay = (WebView) this.findViewById(R.id.orgTxt);
		orgDisplay.setWebViewClient(new InternalWebViewClient());
		orgDisplay.setWebChromeClient(new InternalWebChromeClient());
		this.populateDisplay();
	}

	private void populateDisplay() {
		MobileOrgApplication appInst = (MobileOrgApplication) this
				.getApplication();
		Node node = appInst.nodestackTop();
		
		this.setTitle(node.name);
		String srcText = node.payload.getContent();
		srcText = convertToHtml(srcText);
		this.orgDisplay.loadData(srcText, "text/html", "UTF-8");
	}

	private String convertToHtml(String srcText) {
		int i1 = 0, i2 = 0;
		while (true) {
			i1 = srcText.indexOf("[[", i2 + 1);
			if (i1 < 0)
				break;
			i2 = srcText.indexOf("]]", i1);
			if (i2 < 0)
				break;
			int i3 = srcText.indexOf("][", i1);
			String linkUrl;
			String linkText;
			if (i3 >= 0 && i3 < i2) {
				linkUrl = srcText.substring(i1 + 2, i3);
				linkText = srcText.substring(i3 + 2, i2);
			} else {
				linkUrl = srcText.substring(i1 + 2, i2);
				linkText = linkUrl;

			}
			srcText = srcText.substring(0, i1) + "<a href=\"" + linkUrl + "\">"
					+ linkText + "</a>" + srcText.substring(i2 + 2);
		}
		String result = "<html><body><pre>"
				+ srcText.replaceAll("\\n", "<br/>\n") + "</pre></body></html>";
		return result;
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
				// ignore?
			}
			return false;
		}

		@Override
		public void onReceivedError(WebView view, int errorCode,
				String description, String failingUrl) {
		}

	}

	private static class InternalWebChromeClient extends WebChromeClient {
		// TODO empty; unused right now...
	}

}