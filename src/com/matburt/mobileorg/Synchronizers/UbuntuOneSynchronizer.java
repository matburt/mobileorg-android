package com.matburt.mobileorg.Synchronizers;

import com.matburt.mobileorg.R;
import com.matburt.mobileorg.Parsing.MobileOrgApplication;
import com.matburt.mobileorg.Parsing.OrgFile;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.preference.PreferenceManager;
import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.Authenticator;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.PasswordAuthentication;
import java.net.URL;
import java.net.URLEncoder;

import javax.net.ssl.HttpsURLConnection;

import org.json.JSONException;
import org.json.JSONObject;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.HttpEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.entity.StringEntity;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.ClientProtocolException;
import android.os.Build;

import oauth.signpost.commonshttp.CommonsHttpOAuthConsumer;
import oauth.signpost.signature.HmacSha1MessageSigner;
import oauth.signpost.OAuthConsumer;
import oauth.signpost.basic.DefaultOAuthConsumer;
import oauth.signpost.exception.OAuthCommunicationException;
import oauth.signpost.exception.OAuthException;
import oauth.signpost.exception.OAuthExpectationFailedException;
import oauth.signpost.exception.OAuthMessageSignerException;

public class UbuntuOneSynchronizer extends Synchronizer {

private static final String BASE_TOKEN_NAME = "MobileOrg on ";
	private static final String CONSUMER_KEY = "consumer_key";
	private static final String CONSUMER_SECRET = "consumer_secret";
	private static final String ACCESS_TOKEN = "token";
	private static final String TOKEN_SECRET = "token_secret";

	private static final String LOGIN_HOST = "login.ubuntu.com";
	private static final int LOGIN_PORT = 443;
	private static final String LOGIN_URL = "https://" + LOGIN_HOST + ":"
			+ LOGIN_PORT + "/api/1.0/authentications"
			+ "?ws.op=authenticate&token_name=";
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

    private CommonsHttpOAuthConsumer consumer;

    public UbuntuOneSynchronizer(Context parentContext, MobileOrgApplication appInst) {
        super(parentContext, appInst);

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

    protected void putRemoteFile(String filename, String contents) throws IOException {

    }

    protected BufferedReader getRemoteFile(String filename) {
        return null;
    }

    @Override
    protected void postSynchronize() {
    }

	public boolean login() {
		invalidate();
		try {
            Log.i("MobileOrg", "Logging into Ubuntu One");
			DefaultHttpClient httpClient = new DefaultHttpClient();
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
			consumer = new CommonsHttpOAuthConsumer(consumer_key,
					consumer_secret);
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
        Log.i("MobileOrg", "Login Url: " + login_url);
		return login_url;
	}

	private void ping_u1_url(String username) {
		try {
			String ping_url = PING_URL + username;
			HttpGet request = new HttpGet(ping_url);
			DefaultHttpClient httpClient = new DefaultHttpClient();
			HttpResponse response = null;
			int retries = 3;

			while (retries-- > 0) {
				signRequest(request);
				response = httpClient.execute(request);
				int statusCode = response.getStatusLine().getStatusCode();
				if (statusCode == 400 || statusCode == 401) {
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
}
		
