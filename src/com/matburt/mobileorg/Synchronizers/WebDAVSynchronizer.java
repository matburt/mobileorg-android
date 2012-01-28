package com.matburt.mobileorg.Synchronizers;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.util.regex.Pattern;

import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.Authenticator;
import java.net.PasswordAuthentication;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.X509TrustManager;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;
import android.util.Base64;
import com.matburt.mobileorg.R;
import com.matburt.mobileorg.Parsing.MobileOrgApplication;
import com.matburt.mobileorg.Parsing.OrgFile;
import com.matburt.mobileorg.Parsing.OrgDatabase;

public class WebDAVSynchronizer extends Synchronizer {

    class IntelligentX509TrustManager implements X509TrustManager {
        OrgDatabase db;

        public IntelligentX509TrustManager(OrgDatabase db) {
            super();
            this.db = db;
        }

        public void checkClientTrusted(X509Certificate[] chain,
                                       String authType) throws CertificateException {}

        public void checkServerTrusted(X509Certificate[] chain,
                                       String authType) throws CertificateException {
            for (int i = 0; i < chain.length; i++) {
                String descr = chain[i].toString();
                int hash = chain[i].hashCode();
                if (!this.db.certificateExists(hash)) {
                    Log.i("MobileOrg", "New Certificate Found: " + Integer.toString(chain[i].hashCode()));
                    this.db.addCertificate(hash, descr);
                    //We don't trust any certificates at first
                    throw new CertificateException("Untrusted New Certificate: " + Integer.toString(hash));
                }
                else {
                    if (!this.db.certificateTrusted(hash)) {
                        throw new CertificateException("Untrusted Known Certificate: " + Integer.toString(hash));
                    }
                }
            }
        }

        public X509Certificate[] getAcceptedIssuers() {
            return new X509Certificate[0];
        }
    }

	private String remoteIndexPath;
	private String remotePath;
    private String username;
    private String password;
	
	public WebDAVSynchronizer(Context parentContext, MobileOrgApplication appInst) {
		super(parentContext, appInst);

		SharedPreferences sharedPreferences = PreferenceManager
				.getDefaultSharedPreferences(context);

		this.remoteIndexPath = sharedPreferences.getString("webUrl", "");
		this.remotePath = getRootUrl();

		this.username = sharedPreferences.getString("webUser", "");
		this.password = sharedPreferences.getString("webPass", "");
        this.handleTrustRelationship(parentContext);
	}

    public String testConnection(String url, String user, String pass) {
        this.remoteIndexPath = url;
        this.remotePath = getRootUrl();
        this.username = user;
        this.password = pass;

        if (!this.isConfigured()) {
            Log.i("MobileOrg", "Test Connection Failed for not being configured");
            return "Invalid URL must match: 'http://url.com/path/index.org'";
        }

        try {
            HttpURLConnection dhc = this.createConnection(this.remoteIndexPath);
            if (dhc == null) {
                Log.i("MobileOrg", "Test Connection is null");
                return "Connection could not be established";
            }

            Log.i("MobileOrg", "Test Path: " + this.remoteIndexPath);
            InputStream mainFile = null;
            try {
                mainFile = this.getUrlStream(this.remoteIndexPath);

                if (mainFile == null) {
                    return "File '" + this.remoteIndexPath + "' doesn't appear to exist";
                }

                return null;
            }
            catch (FileNotFoundException e) {
                Log.i("MobileOrg", "Got FNF");
                throw e;
            }
            catch (Exception e) {
                throw e;
            }
        }
        catch (Exception e) {
            Log.i("MobileOrg", "Test Exception: " + e.getMessage());
            return "Test Exception: " + e.getMessage();
        }
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
		String urlActual = this.getRootUrl() + filename;
		putUrlFile(urlActual, contents);
	}

	protected BufferedReader getRemoteFile(String filename) throws IOException {
		String orgUrl = this.remotePath + filename;
        InputStream mainFile = null;
        try {
            mainFile = this.getUrlStream(orgUrl);

            if (mainFile == null) {
                return null;
            } 

            return new BufferedReader(new InputStreamReader(mainFile));
        }
        catch (Exception e) {
            Log.e("MobileOrg", "Exception occurred in getRemoteFile: " + e.toString());
            return null;
        }
	}

    /* See: http://stackoverflow.com/questions/1217141/self-signed-ssl-acceptance-android */
    private void handleTrustRelationship(Context c) {
        try {
            HttpsURLConnection.setDefaultHostnameVerifier(new HostnameVerifier(){
                    public boolean verify(String hostname, SSLSession session) {
                        return true;
                    }});
            SSLContext context = SSLContext.getInstance("TLS");
            context.init(null, new X509TrustManager[]{new IntelligentX509TrustManager(new OrgDatabase(c))}, new SecureRandom());
            HttpsURLConnection.setDefaultSSLSocketFactory(
                                                          context.getSocketFactory());
        } catch (Exception e) { // should never happen
            e.printStackTrace();
        }
    }

	private HttpURLConnection createConnection(String url) {
        URL newUrl = null;
		try {
            newUrl = new URL(url);
		} catch (MalformedURLException e) {
			return null;
		}

        HttpURLConnection con = null;
        try {
            con = (HttpURLConnection) newUrl.openConnection();
        } catch (IOException e) {
            return null;
        }
        con.setReadTimeout(4000);
        con.setConnectTimeout(6000);
        con.addRequestProperty("Expect", "100-continue");
        con.addRequestProperty("Authorization",
                               "Basic "+Base64.encodeToString((this.username + ":" + this.password).getBytes(),
                                                      Base64.NO_WRAP));
        return con;
	}

	private InputStream getUrlStream(String url) throws IOException {
        Log.i("MobileOrg", "Fetching " + url);
        HttpURLConnection con = this.createConnection(url);
        con.setRequestMethod("GET");
        con.setDoInput(true);
        con.connect();
		if (con.getResponseCode() == HttpURLConnection.HTTP_UNAUTHORIZED) {
			throw new FileNotFoundException(r.getString(
					R.string.error_url_fetch_detail, url,
					"Invalid username or password"));
		}
		if (con.getResponseCode()== HttpURLConnection.HTTP_NOT_FOUND) {
            throw new FileNotFoundException(r.getString(
                            R.string.error_url_fetch_detail, url,
                            "File not found: " + url));
		}

		if (con.getResponseCode() < HttpURLConnection.HTTP_OK || con.getResponseCode() > 299) {
			throw new IOException(r.getString(R.string.error_url_fetch_detail,
                                              url, con.getResponseMessage()));
		}
		return con.getInputStream();
	}

	private void putUrlFile(String url, String content) throws IOException {
		try {
            HttpURLConnection con = this.createConnection(url);
            con.setRequestMethod("PUT");
            con.setDoOutput(true);
            OutputStreamWriter out = new OutputStreamWriter(
                                              con.getOutputStream());
            out.write(content);
            out.flush();
            out.close();
            con.getInputStream();
            if (con.getResponseCode() < HttpURLConnection.HTTP_OK || con.getResponseCode() > 299) {
                throw new IOException(r.getString(R.string.error_url_fetch_detail,
                                                  url, con.getResponseMessage()));
            }
        } catch (UnsupportedEncodingException e) {
			throw new IOException(r.getString(
					R.string.error_unsupported_encoding, OrgFile.CAPTURE_FILE));
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
			for (int i = 0; i < pathElements.length - 1; i++) {
				if (pathElements[i].length() > 0) {
					directoryActual += pathElements[i] + "/";
				}
			}
		}
		return manageUrl.getProtocol() + "://" + manageUrl.getAuthority()
				+ directoryActual;
	}

	@Override
	protected void postSynchronize() {		
	}
}
