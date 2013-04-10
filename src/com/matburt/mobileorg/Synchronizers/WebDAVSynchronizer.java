package com.matburt.mobileorg.Synchronizers;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.regex.Pattern;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLHandshakeException;
import javax.net.ssl.SSLSession;
import javax.net.ssl.X509TrustManager;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.res.Resources;
import android.preference.PreferenceManager;
import android.util.Base64;
import android.util.Log;

import com.matburt.mobileorg.R;
import com.matburt.mobileorg.Gui.CertificateConflictActivity;
import com.matburt.mobileorg.util.FileUtils;
import com.matburt.mobileorg.util.OrgUtils;

public class WebDAVSynchronizer implements SynchronizerInterface {

    class IntelligentX509TrustManager implements X509TrustManager {
        Context c;

        public IntelligentX509TrustManager(Context c) {
            super();
            this.c = c;
        }

        public boolean validateCertificate(int hash, String description) {
            SharedPreferences appSettings = 
                PreferenceManager.getDefaultSharedPreferences(this.c);
            Editor edit = appSettings.edit();
            int existingHash = appSettings.getInt("webCertHash", 0);
            if (existingHash == 0) {
                Log.i("MobileOrg", "Storing new certificate");
                edit.putInt("webCertHash", hash);
                edit.putString("webCertDescr", description);
                edit.commit();
                return true;
            }
            else if (existingHash != hash) {
                Log.i("MobileOrg", "Conflicting Certificate Hash");
                edit.putInt("webConflictHash", hash);
                edit.putString("webConflictHashDesc", description);
                edit.commit();
                return true;
                //WARNING: NOTE: This disables the requirement to validate the certificate.
                //               a better way is needed for those certs whose hash changes
                //               all the time
                //return false;
            }
            Log.i("MobileOrg", "Certificates match");
            return true;
        }

        public void checkClientTrusted(X509Certificate[] chain,
                                       String authType) throws CertificateException {}

        public void checkServerTrusted(X509Certificate[] chain,
                                       String authType) throws CertificateException {
            for (int i = 0; i < chain.length; i++) {
                String descr = chain[i].toString();
                int hash = chain[i].hashCode();
                Log.i("MobileOrg", "Validating certificate hash");
                if (!this.validateCertificate(hash, descr)) {
                    throw new CertificateException("Conflicting certificate found with hash " + 
                                                   Integer.toString(hash));
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
	private Context context;
	private Resources r;
	
	public WebDAVSynchronizer(Context parentContext) {
		this.context = parentContext;
		this.r = context.getResources();
		SharedPreferences sharedPreferences = PreferenceManager
				.getDefaultSharedPreferences(parentContext);

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


    private void handleChangedCertificate() {
        Intent i = new Intent(this.context, CertificateConflictActivity.class);
        i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        this.context.startActivity(i);
    }
	
	public void putRemoteFile(String filename, String contents) throws IOException {
		String urlActual = this.getRootUrl() + filename;
		putUrlFile(urlActual, contents);
	}

	public BufferedReader getRemoteFile(String filename) throws IOException, CertificateException,
                                                                   SSLHandshakeException {
		String orgUrl = this.remotePath + filename;
        InputStream mainFile = null;
        try {
            mainFile = this.getUrlStream(orgUrl);

            if (mainFile == null) {
                return null;
            } 

            return new BufferedReader(new InputStreamReader(mainFile));
        }
        catch (CertificateException e) {
            Log.w("MobileOrg", "Conflicting certificate found: " + e.toString());
            handleChangedCertificate();
            throw e;
        }
        catch (SSLHandshakeException e) {
            Log.e("MobileOrg", "SSLHandshakeException Exception in getRemoteFile: " + e.toString());
            handleChangedCertificate();
            throw e;
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
            context.init(null, new X509TrustManager[]{new IntelligentX509TrustManager(c)}, new SecureRandom());
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
        con.setReadTimeout(60000);
        con.setConnectTimeout(60000);
        con.addRequestProperty("Expect", "100-continue");
        con.addRequestProperty("Authorization",
                               "Basic "+Base64.encodeToString((this.username + ":" + this.password).getBytes(),
                                                      Base64.NO_WRAP));
        return con;
	}

	private InputStream getUrlStream(String url) throws IOException, CertificateException {
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
        if (con.getResponseCode() == HttpURLConnection.HTTP_FORBIDDEN) {
            throw new FileNotFoundException(r.getString(
                    R.string.error_url_fetch_detail, url,
                    "Server reported 'Forbidden'"));
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
					R.string.error_unsupported_encoding, FileUtils.CAPTURE_FILE));
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
	public void postSynchronize() {		
	}

	@Override
	public boolean isConnectable() {
		return OrgUtils.isNetworkOnline(context);
	}
}
