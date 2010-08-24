package com.matburt.mobileorg;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.DefaultHttpClient;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.content.res.Resources.NotFoundException;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.util.Log;

public class WebDAVSynchronizer implements Synchronizer
{
    private SharedPreferences appSettings;
    private Activity rootActivity;
    private MobileOrgDatabase appdb;
    private Resources r;
    private static final String LT = "MobileOrg";

    WebDAVSynchronizer(Activity parentActivity) {
        this.rootActivity = parentActivity;
        this.r = this.rootActivity.getResources();
        this.appdb = new MobileOrgDatabase((Context)parentActivity);
        this.appSettings = PreferenceManager.getDefaultSharedPreferences(
                                   parentActivity.getBaseContext());
    }

    public void close() {
        this.appdb.close();
    }

    public void push() throws NotFoundException, ReportableError {
        String urlActual = this.getRootUrl() + "mobileorg.org";
        String storageMode = this.appSettings.getString("storageMode", "");
        BufferedReader reader = null;
        String fileContents = "";

        if (storageMode.equals("internal") || storageMode == null) {
            FileInputStream fs;
            try {
                fs = rootActivity.openFileInput("mobileorg.org");
                reader = new BufferedReader(new InputStreamReader(fs));
            }
            catch (java.io.FileNotFoundException e) {
            	Log.i(LT, "Did not find mobileorg.org file, not pushing.");
                return;
            }
        }
        else if (storageMode.equals("sdcard")) {
            try {
                File root = Environment.getExternalStorageDirectory();
                File morgDir = new File(root, "mobileorg");
                File morgFile = new File(morgDir, "mobileorg.org");
                if (!morgFile.exists()) {
                    Log.i(LT, "Did not find mobileorg.org file, not pushing.");
                    return;
                }
                FileReader orgFReader = new FileReader(morgFile);
                reader = new BufferedReader(orgFReader);
            }
            catch (java.io.IOException e) {
                throw new ReportableError(
                		r.getString(R.string.error_file_read, "mobileorg.org"),
                		e);
            }
        }
        else {
        	throw new ReportableError(
        			r.getString(R.string.error_local_storage_method_unknown, storageMode),
        			null);
        }

        String thisLine = "";
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
        this.appdb.removeFile("mobileorg.org");

        if (storageMode.equals("internal") || storageMode == null) {
            this.rootActivity.deleteFile("mobileorg.org");
        }
        else if (storageMode.equals("sdcard")) {
            File root = Environment.getExternalStorageDirectory();
            File morgDir = new File(root, "mobileorg");
            File morgFile = new File(morgDir, "mobileorg.org");
            morgFile.delete();
        }
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
        String masterStr = this.fetchOrgFile(url);
        if (masterStr.equals("")) {
            throw new ReportableError(
            		r.getString(R.string.error_file_not_found, url),
            		null);
        }
        HashMap<String, String> masterList;
        masterList = this.getOrgFilesFromMaster(masterStr);
        String urlActual = this.getRootUrl();

        //Get checksums file
        masterStr = this.fetchOrgFile(urlActual + "checksums.dat");

        //Get other org files
        for (String key : masterList.keySet()) {
            Log.d(LT, "Fetching: " +
                  key + ": " + urlActual + masterList.get(key));
            String fileContents = this.fetchOrgFile(urlActual +
                                                    masterList.get(key));
            String storageMode = this.appSettings.getString("storageMode", "");
            BufferedWriter writer = new BufferedWriter(new StringWriter());

            if (storageMode.equals("internal") || storageMode == null) {
                FileOutputStream fs;
                try {
                    String normalized = masterList.get(key).replace("/", "_");
                    fs = rootActivity.openFileOutput(normalized, 0);
                    writer = new BufferedWriter(new OutputStreamWriter(fs));
                }
                catch (java.io.FileNotFoundException e) {
                	throw new ReportableError(
                    		r.getString(R.string.error_file_not_found, key),
                    		e);
                }
            }
            else if (storageMode.equals("sdcard")) {

                try {
                    File root = Environment.getExternalStorageDirectory();
                    File morgDir = new File(root, "mobileorg");
                    morgDir.mkdir();
                    if (morgDir.canWrite()){
                        File orgFileCard = new File(morgDir, masterList.get(key));
                        File orgDirCard = orgFileCard.getParentFile();
                        orgDirCard.mkdirs();
                        FileWriter orgFWriter = new FileWriter(orgFileCard);
                        writer = new BufferedWriter(orgFWriter);
                    }
                    else {
                        throw new ReportableError(
                        		r.getString(R.string.error_file_permissions, morgDir.getAbsolutePath()),
                        		null);
                    }
                } catch (java.io.IOException e) {
                    throw new ReportableError(
                    		"IO Exception initializing writer on sdcard file",
                    		e);
                }
            }
            else {
                throw new ReportableError(
                		r.getString(R.string.error_local_storage_method_unknown, storageMode),
                		null);
            }

            try {
            	writer.write(fileContents);
            	this.appdb.addOrUpdateFile(masterList.get(key), key);
                writer.flush();
                writer.close();
            }
            catch (java.io.IOException e) {
                throw new ReportableError(
                		r.getString(R.string.error_file_write, masterList.get(key)),
                		e);
            }
        }
    }

    private String fetchOrgFile(String orgUrl) throws NotFoundException, ReportableError {
        DefaultHttpClient httpC = this.createConnection(
                                      this.appSettings.getString("webUser", ""),
                                      this.appSettings.getString("webPass", ""));
        InputStream mainFile;
		mainFile = this.getUrlStream(orgUrl, httpC);
        String masterStr = "";
        try {
            if (mainFile == null) {
                Log.w(LT, "Stream is null");
                
                //Do we really want to raise an exception here? (John O)
                return ""; //Raise exception
            }
            masterStr = this.ReadInputStream(mainFile);
            Log.d(LT, masterStr);
        }
        catch (IOException e) {
            throw new ReportableError(
            		r.getString(R.string.error_url_fetch, orgUrl),
            		e);
        }
        return masterStr;
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

    private HashMap<String, String> getOrgFilesFromMaster(String master) {
        Pattern getOrgFiles = Pattern.compile("\\[file:(.*?\\.org)\\]\\[(.*?)\\]\\]");
        Matcher m = getOrgFiles.matcher(master);
        HashMap<String, String> allOrgFiles = new HashMap<String, String>();
        while (m.find()) {
            allOrgFiles.put(m.group(2), m.group(1));
        }

        return allOrgFiles;
    }

    private DefaultHttpClient createConnection(String user, String password) {
        DefaultHttpClient httpClient = new DefaultHttpClient();
        UsernamePasswordCredentials bCred = new UsernamePasswordCredentials(user, password);
        BasicCredentialsProvider cProvider = new BasicCredentialsProvider();
        cProvider.setCredentials(AuthScope.ANY, bCred);
        httpClient.setCredentialsProvider(cProvider);
        return httpClient;
    }

    private InputStream getUrlStream(String url, DefaultHttpClient httpClient) throws NotFoundException, ReportableError {
        try {
            HttpResponse res = httpClient.execute(new HttpGet(url));
            
            StatusLine status = res.getStatusLine();
            if(status.getStatusCode() < 200 || status.getStatusCode() > 299) {
            	throw new ReportableError(
            			r.getString(R.string.error_url_fetch_detail, url, status.getReasonPhrase()),
            			null);
            }
            
            return res.getEntity().getContent();
        }
        catch (IOException e) {
            Log.e(LT, e.toString());
            Log.w(LT, "Failed to get URL");
            return null; //handle exception
        }
    }

    private void putUrlFile(String url,
                           DefaultHttpClient httpClient,
                           String content) throws NotFoundException, ReportableError {
        try {
            HttpPut httpPut = new HttpPut(url);
            httpPut.setEntity(new StringEntity(content));
            HttpResponse response = httpClient.execute(httpPut);
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
    	String originalContent = this.fetchOrgFile(url);
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

