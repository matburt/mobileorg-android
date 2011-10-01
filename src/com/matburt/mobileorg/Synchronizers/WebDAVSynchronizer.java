package com.matburt.mobileorg.Synchronizers;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.regex.Pattern;

import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.params.CoreProtocolPNames;
import org.apache.http.params.HttpParams;

import android.content.Context;
import android.preference.PreferenceManager;

import com.matburt.mobileorg.R;
import com.matburt.mobileorg.Parsing.NodeWriter;
import com.matburt.mobileorg.Parsing.OrgFile;

public class WebDAVSynchronizer extends Synchronizer {
	private boolean pushedStageFile = false;

	private String url;

	public WebDAVSynchronizer(Context parentContext) {
		super(parentContext);

		this.url = PreferenceManager.getDefaultSharedPreferences(context)
				.getString("webUrl", "");
	}

	public boolean isConfigured() {
		if (this.url.equals(""))
			return false;

		Pattern checkUrl = Pattern.compile("http.*\\.(?:org|txt)$");
		if (!checkUrl.matcher(this.url).find()) {
			return false;
		}

		return true;
	}

	public void push() throws IOException {
		String urlActual = this.getRootUrl() + "mobileorg.org";
		this.pushedStageFile = false;

		String fileContents = OrgFile.fileToString(NodeWriter.ORGFILE, context);

		DefaultHttpClient httpC = this.createConnection(
				this.appSettings.getString("webUser", ""),
				this.appSettings.getString("webPass", ""));

		this.appendUrlFile(urlActual, httpC, fileContents);

		if (this.pushedStageFile) {
			OrgFile.removeFile("mobileorg.org", context, appdb);
		}
	}

	public void pull() throws IOException {
		updateFiles(this.url, getRootUrl());
	}

	protected BufferedReader getFile(String orgUrl) throws IOException {
		DefaultHttpClient httpC = this.createConnection(
				this.appSettings.getString("webUser", ""),
				this.appSettings.getString("webPass", ""));
		InputStream mainFile;
		mainFile = this.getUrlStream(orgUrl, httpC);

		if (mainFile == null) {
			return null;
		}
		return new BufferedReader(new InputStreamReader(mainFile));
	}

	private String getRootUrl() throws IOException {
		URL manageUrl = null;
		try {
			manageUrl = new URL(this.appSettings.getString("webUrl", ""));
		} catch (MalformedURLException e) {
			String url = r.getString(R.string.error_bad_url,
					(manageUrl == null) ? "" : manageUrl.toString());
			throw new FileNotFoundException(url);
		}

		String urlPath = manageUrl.getPath();
		String[] pathElements = urlPath.split("/");
		String directoryActual = "/";
		if (pathElements.length > 1) {
			for (int idx = 0; idx < pathElements.length - 1; idx++) {
				if (pathElements[idx].length() > 0) {
					directoryActual += pathElements[idx] + "/";
				}
			}
		}
		return manageUrl.getProtocol() + "://" + manageUrl.getAuthority()
				+ directoryActual;
	}

	private DefaultHttpClient createConnection(String user, String password) {
		DefaultHttpClient httpClient = new DefaultHttpClient();
		HttpParams params = httpClient.getParams();
		SchemeRegistry schemeRegistry = new SchemeRegistry();
		schemeRegistry.register(new Scheme("http", PlainSocketFactory
				.getSocketFactory(), 80));
		SSLSocketFactory sslSocketFactory = SSLSocketFactory.getSocketFactory();
		sslSocketFactory
				.setHostnameVerifier(SSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);
		schemeRegistry.register(new Scheme("https", sslSocketFactory, 443));
		ThreadSafeClientConnManager cm = new ThreadSafeClientConnManager(
				params, schemeRegistry);

		UsernamePasswordCredentials bCred = new UsernamePasswordCredentials(
				user, password);
		BasicCredentialsProvider cProvider = new BasicCredentialsProvider();
		cProvider.setCredentials(AuthScope.ANY, bCred);

		params.setBooleanParameter(CoreProtocolPNames.USE_EXPECT_CONTINUE,
				false);
		httpClient.setParams(params);

		DefaultHttpClient nHttpClient = new DefaultHttpClient(cm, params);
		nHttpClient.setCredentialsProvider(cProvider);
		return nHttpClient;
	}

	private InputStream getUrlStream(String url, DefaultHttpClient httpClient)
			throws IOException {
		HttpResponse res = httpClient.execute(new HttpGet(url));
		StatusLine status = res.getStatusLine();
		if (status.getStatusCode() == 401) {
			// throw new
			// ReportableError(r.getString(R.string.error_url_fetch_detail,
			// url,
			// "Invalid username or password"),
			// null);
		}
		if (status.getStatusCode() == 404) {
			return null;
		}

		if (status.getStatusCode() < 200 || status.getStatusCode() > 299) {
			// throw new ReportableError(
			// r.getString(R.string.error_url_fetch_detail,
			// url,
			// status.getReasonPhrase()),
			// null);
		}
		return res.getEntity().getContent();
	}

	private void putUrlFile(String url, DefaultHttpClient httpClient,
			String content) throws IOException {
		try {
			HttpPut httpPut = new HttpPut(url);
			httpPut.setEntity(new StringEntity(content, "UTF-8"));
			HttpResponse response = httpClient.execute(httpPut);
			StatusLine statResp = response.getStatusLine();
			int statCode = statResp.getStatusCode();
			if (statCode >= 400) {
				this.pushedStageFile = false;
				// throw new
				// ReportableError(r.getString(R.string.error_url_put_detail,
				// url,
				// "Server returned code: " + Integer.toString(statCode)),
				// null);
			} else {
				this.pushedStageFile = true;
			}

			httpClient.getConnectionManager().shutdown();
		} catch (UnsupportedEncodingException e) {
			// throw new ReportableError(
			// r.getString(R.string.error_unsupported_encoding,
			// "mobileorg.org"),
			// e);
		}
	}

	private void appendUrlFile(String url, DefaultHttpClient httpClient,
			String content) throws IOException {
		String originalContent = OrgFile.fileToString(getFile(url));
		String newContent = originalContent + '\n' + content;
		this.putUrlFile(url, httpClient, newContent);
	}
}
