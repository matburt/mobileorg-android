package com.matburt.mobileorg.Synchronizers;

import android.content.Context;
import android.content.res.Resources.NotFoundException;
import android.preference.PreferenceManager;
import android.util.Log;
import com.matburt.mobileorg.Error.ReportableError;
import com.matburt.mobileorg.MobileOrgDatabase;
import com.matburt.mobileorg.R;
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

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.regex.Pattern;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLSession;

public class WebDAVSynchronizer extends Synchronizer
{
    private boolean pushedStageFile = false;

    public WebDAVSynchronizer(Context parentContext) {
        this.rootContext = parentContext;
        this.r = this.rootContext.getResources();
        this.appdb = new MobileOrgDatabase((Context)parentContext);
        this.appSettings = PreferenceManager.getDefaultSharedPreferences(
                                   parentContext.getApplicationContext());
    }

    public void push() throws NotFoundException, ReportableError {
        String urlActual = this.getRootUrl() + "mobileorg.org";
        String storageMode = this.appSettings.getString("storageMode", "");
        BufferedReader reader = this.getReadHandle("mobileorg.org");
        String fileContents = "";
        this.pushedStageFile = false;
        String thisLine = "";

        if (reader == null) {
            return;
        }

        try {
            while ((thisLine = reader.readLine()) != null) {
                fileContents += thisLine + "\n";
            }
        }
        catch (java.io.IOException e) {
        	throw new ReportableError(
            		r.getString(R.string.error_file_read, "mobileorg.org"),
            		e);
        }

        DefaultHttpClient httpC = this.createConnection(
                                    this.appSettings.getString("webUser", ""),
                                    this.appSettings.getString("webPass", ""));

        this.appendUrlFile(urlActual, httpC, fileContents);

        if (this.pushedStageFile) {
            this.removeFile("mobileorg.org");
        }
    }

    public boolean checkReady() {
        if (this.appSettings.getString("webUrl","").equals(""))
            return false;
        return true;
    }

    public void pull() throws NotFoundException, ReportableError {
        Pattern checkUrl = Pattern.compile("http.*\\.(?:org|txt)$");
        String url = this.appSettings.getString("webUrl", "");
        if (!checkUrl.matcher(url).find()) {
        	throw new ReportableError(
            		r.getString(R.string.error_bad_url, url),
            		null);
        }

        //Get the index org file
        String masterStr = this.fetchOrgFileString(url);
        if (masterStr.equals("")) {
            throw new ReportableError(
            		r.getString(R.string.error_file_not_found, url),
            		null);
        }
        HashMap<String, String> masterList = this.getOrgFilesFromMaster(masterStr);
        ArrayList<HashMap<String, Boolean>> todoLists = this.getTodos(masterStr);
        ArrayList<ArrayList<String>> priorityLists = this.getPriorities(masterStr);
        this.appdb.setTodoList(todoLists);
        this.appdb.setPriorityList(priorityLists);
        String urlActual = this.getRootUrl();

        //Get checksums file
        masterStr = this.fetchOrgFileString(urlActual + "checksums.dat");
        HashMap<String, String> newChecksums = this.getChecksums(masterStr);
        HashMap<String, String> oldChecksums = this.appdb.getChecksums();

        //Get other org files
        for (String key : masterList.keySet()) {
            if (oldChecksums.containsKey(key) &&
                newChecksums.containsKey(key) &&
                oldChecksums.get(key).equals(newChecksums.get(key)))
                continue;
            Log.d(LT, "Fetching: " +
                  key + ": " + urlActual + masterList.get(key));
            this.fetchAndSaveOrgFile(urlActual + masterList.get(key),
                                     masterList.get(key));
            this.appdb.addOrUpdateFile(masterList.get(key),
                                       key,
                                       newChecksums.get(key));
        }

        // the key-value semantics is switched: in getOrgFiles(), the HashMap has filenames as keys
        // while in masterList (getOrgFilesFromMaster) the file aliases are keys
        HashSet<String> filesInDb = new HashSet<String>(this.appdb.getOrgFiles().keySet());
        HashSet<String> filesInIndexFile = new HashSet<String>(masterList.values());
        filesInDb.removeAll(filesInIndexFile); //now contains stale DB files
        if (filesInDb.size() > 0) {
            Object[] arrObj = filesInDb.toArray();
            for (int i = 0; i < arrObj.length; i++) {
                Log.i(LT, "Orphaned file: " + (String)arrObj[i]);
                removeFile((String)arrObj[i]);
            }
        }
    }

    public BufferedReader fetchOrgFile(String orgUrl) throws NotFoundException, ReportableError {
        DefaultHttpClient httpC = this.createConnection(
                                    this.appSettings.getString("webUser", ""),
                                    this.appSettings.getString("webPass", ""));
        InputStream mainFile;
        try {
            mainFile = this.getUrlStream(orgUrl, httpC);
        }
        catch (IllegalArgumentException e) {
            throw new ReportableError(
                    r.getString(R.string.error_invalid_url, orgUrl),
                    e);
        }
        if (mainFile == null) {
            return null;
        }
        return new BufferedReader(new InputStreamReader(mainFile));
    }

    private String getRootUrl() throws NotFoundException, ReportableError {
        URL manageUrl = null;
        try {
            manageUrl = new URL(this.appSettings.getString("webUrl", ""));
        }
        catch (MalformedURLException e) {
            throw new ReportableError(
            		r.getString(R.string.error_bad_url,
            				(manageUrl == null) ? "" : manageUrl.toString()),
            		e);
        }

        String urlPath =  manageUrl.getPath();
        String[] pathElements = urlPath.split("/");
        String directoryActual = "/";
        if (pathElements.length > 1) {
            for (int idx = 0; idx < pathElements.length - 1; idx++) {
                if (pathElements[idx].length() > 0) {
                    directoryActual += pathElements[idx] + "/";
                }
            }
        }
        return manageUrl.getProtocol() + "://" +
            manageUrl.getAuthority() + directoryActual;
    }

    private DefaultHttpClient createConnection(String user, String password) {
        DefaultHttpClient httpClient = new DefaultHttpClient();
        HttpParams params = httpClient.getParams();
        SchemeRegistry schemeRegistry = new SchemeRegistry();
        schemeRegistry.register (new Scheme ("http",
                                             PlainSocketFactory.getSocketFactory (), 80));
        SSLSocketFactory sslSocketFactory = SSLSocketFactory.getSocketFactory();
        sslSocketFactory.setHostnameVerifier(SSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);
        schemeRegistry.register (new Scheme ("https",
                                             sslSocketFactory, 443));
        ThreadSafeClientConnManager cm = new ThreadSafeClientConnManager (
                                                  params, schemeRegistry);

        UsernamePasswordCredentials bCred = new UsernamePasswordCredentials(user, password);
        BasicCredentialsProvider cProvider = new BasicCredentialsProvider();
        cProvider.setCredentials(AuthScope.ANY, bCred);

        params.setBooleanParameter(CoreProtocolPNames.USE_EXPECT_CONTINUE, false);
        httpClient.setParams(params);

        DefaultHttpClient nHttpClient = new DefaultHttpClient(cm, params);
        nHttpClient.setCredentialsProvider(cProvider);
        return nHttpClient;
    }

    private InputStream getUrlStream(String url, DefaultHttpClient httpClient) throws NotFoundException, ReportableError {
        try {
            HttpResponse res = httpClient.execute(new HttpGet(url));
            StatusLine status = res.getStatusLine();
            if (status.getStatusCode() == 401) {
                throw new ReportableError(r.getString(R.string.error_url_fetch_detail,
                                                      url,
                                                      "Invalid username or password"),
                                          null);
            }
            if (status.getStatusCode() == 404) {
                return null;
            }

            if (status.getStatusCode() < 200 || status.getStatusCode() > 299) {
            	throw new ReportableError(
            			r.getString(R.string.error_url_fetch_detail,
                                    url,
                                    status.getReasonPhrase()),
            			null);
            }
            return res.getEntity().getContent();
        }
        catch (IOException e) {
            Log.e(LT, e.toString());
            Log.w(LT, "Failed to get URL");
            throw new ReportableError(r.getString(R.string.error_url_fetch_detail,
                                                  url,
                                                  e.getMessage()),
                                      e);
        }
    }

    private void putUrlFile(String url,
                           DefaultHttpClient httpClient,
                           String content) throws NotFoundException, ReportableError {
        try {
            HttpPut httpPut = new HttpPut(url);
            httpPut.setEntity(new StringEntity(content, "UTF-8"));
            HttpResponse response = httpClient.execute(httpPut);
            StatusLine statResp = response.getStatusLine();
            int statCode = statResp.getStatusCode();
            if (statCode >= 400) {
                this.pushedStageFile = false;
                throw new ReportableError(r.getString(R.string.error_url_put_detail,
                                                      url,
                                                      "Server returned code: " + Integer.toString(statCode)),
                                          null);
            } else {
                this.pushedStageFile = true;
            }

            httpClient.getConnectionManager().shutdown();
        }
        catch (UnsupportedEncodingException e) {
        	throw new ReportableError(
        			r.getString(R.string.error_unsupported_encoding, "mobileorg.org"),
        			e);
        }
        catch (IOException e) {
        	throw new ReportableError(
        			r.getString(R.string.error_url_put, url),
        			e);
        }
    }

    private void appendUrlFile(String url,
    							DefaultHttpClient httpClient,
    							String content) throws NotFoundException, ReportableError {
    	String originalContent = this.fetchOrgFileString(url);
    	String newContent = originalContent + '\n' + content;
    	this.putUrlFile(url, httpClient, newContent);
    }

    private String ReadInputStream(InputStream in) throws IOException {
        StringBuffer stream = new StringBuffer();
        byte[] b = new byte[4096];
        for (int n; (n = in.read(b)) != -1;) {
            stream.append(new String(b, 0, n));
        }
        return stream.toString();
    }
}

