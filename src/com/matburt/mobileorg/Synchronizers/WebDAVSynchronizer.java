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
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.matburt.mobileorg.R;
import com.matburt.mobileorg.Parsing.NodeWriter;

public class WebDAVSynchronizer extends Synchronizer {

	private String remoteIndexPath;
	private String remotePath;
	
	private String username;
	private String password;

	WebDAVSynchronizer(Context parentContext) {
		super(parentContext);

		SharedPreferences sharedPreferences = PreferenceManager
				.getDefaultSharedPreferences(context);

		this.remoteIndexPath = sharedPreferences.getString("webUrl", "");
		this.remotePath = getRootUrl();

		this.username = sharedPreferences.getString("webUser", "");
		this.password = sharedPreferences.getString("webPass", "");
	}

	public boolean isConfigured() {
		if (this.remoteIndexPath.equals(""))
			return false;

		Pattern checkUrl = Pattern.compile("http.*\\.(?:org|txt)$");
		if (!checkUrl.matcher(this.remoteIndexPath).find()) {
			return false;
		}

		return true;
	}

	protected void putRemoteFile(String filename, String contents) throws IOException {
		DefaultHttpClient httpC = this.createConnection();
		String urlActual = this.getRootUrl() + filename;
		putUrlFile(urlActual, httpC, contents);
	}

	protected BufferedReader getRemoteFile(String filename) throws IOException {
		String orgUrl = this.remotePath + filename;
		DefaultHttpClient httpC = this.createConnection();
		InputStream mainFile = this.getUrlStream(orgUrl, httpC);

		if (mainFile == null) {
			return null;
		}
		return new BufferedReader(new InputStreamReader(mainFile));
	}

	private DefaultHttpClient createConnection() {
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
				username, password);
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
			throw new FileNotFoundException(r.getString(
					R.string.error_url_fetch_detail, url,
					"Invalid username or password"));
		}
		if (status.getStatusCode() == 404) {
			return null;
		}

		if (status.getStatusCode() < 200 || status.getStatusCode() > 299) {
			throw new IOException(r.getString(R.string.error_url_fetch_detail,
					url, status.getReasonPhrase()));
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
				throw new IOException(r.getString(
						R.string.error_url_put_detail, url,
						"Server returned code: " + Integer.toString(statCode)));
			}
			httpClient.getConnectionManager().shutdown();
		} catch (UnsupportedEncodingException e) {
			throw new IOException(r.getString(
					R.string.error_unsupported_encoding, NodeWriter.ORGFILE));
		}
	}

	private String getRootUrl() {
		URL manageUrl;
		try {
			manageUrl = new URL(this.remoteIndexPath);
		} catch (MalformedURLException e) {
			return "";
		}

		String urlPath = manageUrl.getPath();
		String[] pathElements = urlPath.split("/");
		String directoryActual = "/";

		if (pathElements.length > 1) {
			for (String pathElement : pathElements) {
				if (pathElement.length() > 0) {
					directoryActual += pathElement + "/";
				}
			}
		}
		return manageUrl.getProtocol() + "://" + manageUrl.getAuthority()
				+ directoryActual;
	}
}
