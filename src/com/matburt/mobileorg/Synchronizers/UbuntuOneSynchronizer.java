package com.matburt.mobileorg.Synchronizers;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;

import oauth.signpost.commonshttp.CommonsHttpOAuthConsumer;
import oauth.signpost.exception.OAuthException;
import oauth.signpost.signature.HmacSha1MessageSigner;

import org.apache.http.HttpEntity;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.matburt.mobileorg.util.OrgUtils;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Build;
import android.preference.PreferenceManager;
import android.util.Log;

public class UbuntuOneSynchronizer implements SynchronizerInterface {

private static final String BASE_TOKEN_NAME = "Ubuntu One @ MobileOrg:";
	private static final String CONSUMER_KEY = "consumer_key";
	private static final String CONSUMER_SECRET = "consumer_secret";
	private static final String ACCESS_TOKEN = "token";
	private static final String TOKEN_SECRET = "token_secret";

    private static final String BASE_PATH = "root_node_path";
    private static final String BYTES_USED = "used_bytes";
    private static final String MAX_BYTES = "max_bytes";

	private static final String LOGIN_HOST = "login.ubuntu.com";
	private static final int LOGIN_PORT = 443;
	private static final String LOGIN_URL = "https://" + LOGIN_HOST + ":"
			+ LOGIN_PORT + "/api/1.0/authentications"
			+ "?ws.op=authenticate&token_name=";
    private static final String FILES_BASE = "https://files.one.ubuntu.com";
    private static final String FILES_URL = "https://one.ubuntu.com/api/file_storage/v1";
	private static final String PING_URL = "https://one.ubuntu.com/oauth/sso-finished-so-get-tokens/";
	private static final String UTF8 = "UTF-8";

	public String remoteIndexPath;
	public String remotePath;
    public String username;
    public String password;

    public String consumer_key;
    public String consumer_secret;
    public String access_token;
    public String token_secret;

    public String root_path;
    public long bytes_used;
    public long max_bytes;

    private CommonsHttpOAuthConsumer consumer;
	private Context context;    

    public UbuntuOneSynchronizer(Context context) {
    	this.context = context;
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
		this.remoteIndexPath = sharedPreferences.getString("ubuntuOnePath", "");
        //		this.remotePath = getRootUrl();

		this.username = sharedPreferences.getString("ubuntuOneUser", "");
		this.password = ""; //we don't store this, it's just set to be populated by wizard

        consumer_key = sharedPreferences.getString("ubuntuConsumerKey", "");
        consumer_secret = sharedPreferences.getString("ubuntuConsumerSecret", "");
        access_token = sharedPreferences.getString("ubuntuAccessToken", "");
        token_secret = sharedPreferences.getString("ubuntuTokenSecret", "");
    }

    public void invalidate() {
        this.consumer = null;
    }

    public String testConnection(String user, String pass) {
        return "";
    }

    public boolean isConfigured() {
        if (this.consumer_key.equals(""))
            return false;
        return true;
    }

	public void signRequest(HttpRequest request) {
		int retries = 3;

		if (consumer == null) {
			buildConsumer();
		}

		while (retries-- > 0) {
			try {
				if (consumer != null) {
					// We need to remove the previous Authorization header
					// because signpost fails to sign a second time otherwise. 
					request.removeHeaders("Authorization");
					consumer.sign(request);
					return;
				}
			} catch (OAuthException e) {
				e.printStackTrace();
			}
			login();
		}
	}

    public void putRemoteFile(String filename, String contents) throws IOException {
        try {
            buildConsumer();
            String latterPart = remoteIndexPath + filename;
            latterPart = latterPart.replaceAll("/{2,}", "/");
            String files_url = FILES_URL + root_path + latterPart;

            URL url = new URL(files_url);
            URI uri = new URI(url.getProtocol(), url.getUserInfo(), url.getHost(),
                              url.getPort(), url.getPath(), url.getQuery(), url.getRef());
            url = uri.toURL();

            HttpPut request = new HttpPut(url.toString());
            JSONObject createFile = new JSONObject();
            createFile.put("kind", "file");
            StringEntity se = new StringEntity(createFile.toString());  
            //se.setContentType(new BasicHeader(HTTP.CONTENT_TYPE, "application/json"));
            request.setEntity(se);
            DefaultHttpClient httpClient = new DefaultHttpClient();
            signRequest(request);
            HttpResponse response = httpClient.execute(request);
            verifyResponse(response);
            JSONObject fileData = responseToJson(response);

            String content_path = fileData.getString("content_path");
            String content_url = FILES_BASE + content_path;

            url = new URL(content_url);
            uri = new URI(url.getProtocol(), url.getUserInfo(), url.getHost(),
                              url.getPort(), url.getPath(), url.getQuery(), url.getRef());
            url = uri.toURL();

            request = new HttpPut(url.toString());
            request.setEntity(new StringEntity(contents, "UTF-8"));
            httpClient = new DefaultHttpClient();

            signRequest(request);
            response = httpClient.execute(request);
            verifyResponse(response);

        } catch (Exception e) {
            Log.e("MobileOrg", "Exception in Ubuntu One Put File: " + e.toString());
			throw new IOException("Uploading: " + filename + ": " + e.toString());
        }
    }

    public BufferedReader getRemoteFile(String filename) {
        try { 
            buildConsumer();
            String latterPart = remoteIndexPath + filename;
            latterPart = latterPart.replaceAll("/{2,}", "/");
            String files_url = FILES_URL + root_path + latterPart;
            URL url = new URL(files_url);
            URI uri = new URI(url.getProtocol(), url.getUserInfo(), url.getHost(),
                              url.getPort(), url.getPath(), url.getQuery(), url.getRef());
            url = uri.toURL();
            HttpGet request = new HttpGet(url.toString());
            DefaultHttpClient httpClient = new DefaultHttpClient();
            signRequest(request);
            HttpResponse response = httpClient.execute(request);
            verifyResponse(response);
            JSONObject fileData = responseToJson(response);

            String content_path = fileData.getString("content_path");
            String content_url = FILES_BASE + content_path;
            url = new URL(content_url);
            uri = new URI(url.getProtocol(), url.getUserInfo(), url.getHost(),
                              url.getPort(), url.getPath(), url.getQuery(), url.getRef());
            url = uri.toURL();
            request = new HttpGet(url.toString());
            httpClient = new DefaultHttpClient();
            signRequest(request);
            response = httpClient.execute(request);
            verifyResponse(response);
            return new BufferedReader(new InputStreamReader(response.getEntity().getContent()));
        } catch (Exception e) {
            Log.e("MobileOrg", "Exception in Ubuntu One Fetch File: " + e.toString());
        }
        return null;
    }

    public ArrayList<String> getDirectoryList(String directory) {
        ArrayList<String> directories = new ArrayList<String>();
		try {
            buildConsumer();
            String latterPart = root_path + directory + "?include_children=true";
            latterPart = latterPart.replaceAll("/{2,}", "/");
			String files_url = FILES_URL + latterPart;
            URL url = new URL(files_url);
            URI uri = new URI(url.getProtocol(), url.getUserInfo(), url.getHost(),
                              url.getPort(), url.getPath(), url.getQuery(), url.getRef());
            url = uri.toURL();
            Log.d("MobileOrg", "Getting directory list for: " + url.toString());
			HttpGet request = new HttpGet(url.toString());
			DefaultHttpClient httpClient = new DefaultHttpClient();
            signRequest(request);
            HttpResponse response = httpClient.execute(request);
            verifyResponse(response);
            JSONObject dirData = responseToJson(response);
            JSONArray jsA = dirData.getJSONArray("children");
            if (jsA != null) { 
                for (int i = 0; i < jsA.length(); i++){
                    JSONObject node = jsA.getJSONObject(i);
                    if (node.getString("kind").equals("directory")) {
                        directories.add(node.getString("path"));
                    }
                } 
            }
        } catch (Exception e) {
            Log.e("MobileOrg", "Exception in Ubuntu One Fetch Directories: " + e.toString());
        }
        return directories;
    }

    public void getBaseUser() {
		try {
            buildConsumer();
			String files_url = FILES_URL;
			HttpGet request = new HttpGet(files_url);
			DefaultHttpClient httpClient = new DefaultHttpClient();
            signRequest(request);
            HttpResponse response = httpClient.execute(request);
            verifyResponse(response);
            JSONObject dirData = responseToJson(response);
            root_path = dirData.getString(BASE_PATH);
            max_bytes = dirData.getLong(MAX_BYTES);
            bytes_used = dirData.getLong(BYTES_USED);
        } catch (Exception e) {
            Log.e("MobileOrg", "Exception in Ubuntu One Fetch Directories: " + e.toString());
        }
    }

    @Override
	public void postSynchronize() {
    }

	public boolean login() {
		invalidate();
		try {
            Log.i("MobileOrg", "Logging into Ubuntu One");
			DefaultHttpClient httpClient = new DefaultHttpClient();
            final HttpParams httpParameters = httpClient.getParams();
            HttpConnectionParams.setConnectionTimeout(httpParameters, 60000);
            HttpConnectionParams.setSoTimeout(httpParameters, 60000);
			httpClient.getCredentialsProvider().setCredentials(
					new AuthScope(LOGIN_HOST, LOGIN_PORT),
					new UsernamePasswordCredentials(this.username, this.password));
			HttpUriRequest request = new HttpGet(buildLoginUrl());
			HttpResponse response = httpClient.execute(request);
			verifyResponse(response);
			JSONObject loginData = responseToJson(response);
            consumer_key = loginData.getString(CONSUMER_KEY);
            consumer_secret = loginData.getString(CONSUMER_SECRET);
            access_token = loginData.getString(ACCESS_TOKEN);
            token_secret = loginData.getString(TOKEN_SECRET);

            SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
            Editor edit = sharedPreferences.edit();
            edit.putString("ubuntuConsumerKey", consumer_key);
            edit.putString("ubuntuConsumerSecret", consumer_secret);
            edit.putString("ubuntuAccessToken", access_token);
            edit.putString("ubuntuTokenSecret", token_secret);
            Log.i("MobileOrg", "Logged in to Ubuntu One: " + consumer_key);
            edit.commit();

			buildConsumer();
            ping_u1_url(this.username);
            return true;
		} catch (ClientProtocolException e) {
            Log.e("MobileOrg", "Protocol Exception: " + e.toString());
		} catch (IOException e) {
			Log.e("MobileOrg", "IO Exception: " + e.toString());
		} catch (JSONException e) {
            Log.e("MobileOrg", "JSONException: " + e.toString());
		}
        return false;
	}

	private InputStream getUrl(String url) throws Exception {
		HttpGet request = new HttpGet(url);
		HttpResponse response = executeRequest(request);
		HttpEntity entity = response.getEntity();
		if (entity != null) {
			InputStream instream = entity.getContent();
            return instream;
		}
		return null;
	}

    private void putUrl(String url, String data) throws Exception {
        HttpPut put = new HttpPut(url);
        put.setEntity(new StringEntity(data));
        HttpResponse response = executeRequest(put);
    }

	protected HttpResponse executeRequest(HttpUriRequest request)
			throws ClientProtocolException, IOException {
		DefaultHttpClient httpClient = new DefaultHttpClient();
        final HttpParams httpParameters = httpClient.getParams();
        HttpConnectionParams.setConnectionTimeout(httpParameters, 60000);
        HttpConnectionParams.setSoTimeout(httpParameters, 60000);
		HttpResponse response = null;
		int retries = 3;

		while (retries-- > 0) {
			this.signRequest(request);
			response = httpClient.execute(request);
			int statusCode = response.getStatusLine().getStatusCode();
			if (statusCode == 400 || statusCode == 401) {
				invalidate();
			} else {
				return response;
			}
		}
		return response;
	}

	private void buildConsumer() {
		if (consumer_key != null && consumer_secret != null
				&& access_token != null && token_secret != null) {
			consumer = new CommonsHttpOAuthConsumer(consumer_key, consumer_secret);
			consumer.setMessageSigner(new HmacSha1MessageSigner());
			consumer.setTokenWithSecret(access_token, token_secret);
		}
	}

	private void verifyResponse(HttpResponse response) throws IOException {
		int statusCode = response.getStatusLine().getStatusCode();
		if (statusCode < 200 || statusCode > 299) {
			throw new IOException("Bad Auth Response: " + Integer.toString(statusCode));
		}
	}

	private JSONObject responseToJson(HttpResponse response)
			throws UnsupportedEncodingException, IOException, JSONException {
		BufferedReader reader = new BufferedReader(new InputStreamReader(
				response.getEntity().getContent(), "UTF-8"));
		StringBuilder builder = new StringBuilder();
		for (String line = null; (line = reader.readLine()) != null;) {
			builder.append(line).append("\n");
		}
		return new JSONObject(builder.toString());
	}

	private String buildLoginUrl() {
		String token_name = BASE_TOKEN_NAME + Build.MODEL;
		String login_url = LOGIN_URL;
		try {
			login_url += URLEncoder.encode(token_name, UTF8);
		} catch (UnsupportedEncodingException e) {
			login_url += "Android";
		}
		return login_url;
	}

	private void ping_u1_url(String username) {
		try {
			String ping_url = PING_URL + username;
			HttpGet request = new HttpGet(ping_url);
			DefaultHttpClient httpClient = new DefaultHttpClient();
            final HttpParams httpParameters = httpClient.getParams();
            HttpConnectionParams.setConnectionTimeout(httpParameters, 60000);
            HttpConnectionParams.setSoTimeout(httpParameters, 60000);
			HttpResponse response = null;
			int retries = 3;

			while (retries-- > 0) {
				signRequest(request);
				response = httpClient.execute(request);
				int statusCode = response.getStatusLine().getStatusCode();
				if (statusCode == 400 || statusCode == 401) {
                    Log.e("MobileOrg", "Ping failed");
					invalidate();
				} else {
					return;
				}
			}
        } catch (Exception e) {
            Log.e("MobileOrg", "Exception in Ubuntu One Ping: " + e.toString());
        }
		// } catch (UnsupportedEncodingException e) {
		// 	e.printStackTrace();
		// } catch (ClientProtocolException e) {
		// 	e.printStackTrace();
		// } catch (IOException e) {
		// 	e.printStackTrace();
		// }
	}

	@Override
	public boolean isConnectable() {
		return OrgUtils.isNetworkOnline(context);
	}
}
		
