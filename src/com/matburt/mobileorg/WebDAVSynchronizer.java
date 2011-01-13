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
import java.io.BufferedInputStream;
import java.net.MalformedURLException;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.ArrayList;
import java.lang.IllegalArgumentException;

import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.net.Authenticator;
import java.net.PasswordAuthentication;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.content.res.Resources.NotFoundException;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.util.Log;
import android.text.TextUtils;
import org.apache.http.params.CoreProtocolPNames;
import org.apache.http.params.HttpParams;

public class WebDAVSynchronizer extends Synchronizer
{
    private boolean pushedStageFile = false;

    WebDAVSynchronizer(Context parentContext) {
        this.rootContext = parentContext;
        this.r = this.rootContext.getResources();
        this.appdb = new MobileOrgDatabase((Context)parentContext);
        this.appSettings = PreferenceManager.getDefaultSharedPreferences(
                                   parentContext.getApplicationContext());
    }

    public void signOn(String mUser, String mPass) {
        final String tUser = mUser;
        final String tPass = mPass;
        Authenticator.setDefault(new Authenticator() {
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(tUser, tPass.toCharArray());
                }});
    }

    public void push() throws NotFoundException, ReportableError {
        String urlActual = this.getRootUrl() + "mobileorg.org";
        String storageMode = this.appSettings.getString("storageMode", "");
        this.signOn(this.appSettings.getString("webUser", ""),
                    this.appSettings.getString("webPass", ""));
        BufferedReader reader = null;
        String fileContents = "";
        this.pushedStageFile = false;

        if (storageMode.equals("internal") || storageMode == null) {
            FileInputStream fs;
            try {
                fs = rootContext.openFileInput("mobileorg.org");
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

        this.appendUrlFile(urlActual, fileContents);

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
        this.signOn(this.appSettings.getString("webUser", ""),
                    this.appSettings.getString("webPass", ""));
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
        HashMap<String, String> masterList = this.getOrgFilesFromMaster(masterStr);
        ArrayList<HashMap<String, Boolean>> todoLists = this.getTodos(masterStr);
        ArrayList<ArrayList<String>> priorityLists = this.getPriorities(masterStr);
        this.appdb.setTodoList(todoLists);
        this.appdb.setPriorityList(priorityLists);
        String urlActual = this.getRootUrl();

        //Get checksums file
        masterStr = this.fetchOrgFile(urlActual + "checksums.dat");
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
            String fileContents = this.fetchOrgFile(urlActual +
                                                    masterList.get(key));
            String storageMode = this.appSettings.getString("storageMode", "");
            BufferedWriter writer = new BufferedWriter(new StringWriter());

            if (storageMode.equals("internal") || storageMode == null) {
                FileOutputStream fs;
                try {
                    String normalized = masterList.get(key).replace("/", "_");
                    fs = rootContext.openFileOutput(normalized, 0);
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
            	this.appdb.addOrUpdateFile(masterList.get(key), key, newChecksums.get(key));
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
        InputStream mainFile;
        try {
            mainFile = this.getUrlStream(orgUrl, true);
        }
        catch (IllegalArgumentException e) {
            throw new ReportableError(
                    r.getString(R.string.error_invalid_url, orgUrl),
                    e);
        }

        String masterStr = "";
        try {
            if (mainFile == null) {
                Log.w(LT, "Stream is null");
                return "";
            }
            masterStr = this.ReadInputStream(mainFile);
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

    private InputStream getUrlStream(String mUrl, boolean retryOnce) throws NotFoundException, ReportableError {
        URL url;
        try {
            url = URI.create(mUrl).toURL();
        } catch (Exception e) {
            throw new ReportableError(r.getString(R.string.error_invalid_url, mUrl), e);
        }

        HttpURLConnection.setFollowRedirects(true);
        HttpURLConnection huc;
        try {
            huc = (HttpURLConnection) url.openConnection();
        } catch (Exception e) {
            throw new ReportableError(r.getString(R.string.error_url_fetch_detail,
                                                  mUrl, e.getMessage()), e);
        }

        try {
            huc.connect();

            int status = huc.getResponseCode();
            if (retryOnce && status == -1) {
                return getUrlStream(mUrl, false);
            }
            if (status != 200) {
                throw new ReportableError(r.getString(R.string.error_url_fetch_detail,
                                                      mUrl, "Response code is: " + status), null);
            }
            return new BufferedInputStream(huc.getInputStream());
        } catch (Exception e) {
            throw new ReportableError(r.getString(R.string.error_url_fetch_detail,
                                                  mUrl, e.getMessage()), e);
        }
    }

    private void putUrlFile(String mUrl, String content) throws NotFoundException, ReportableError {
        URL url;

        try {
            url = URI.create(mUrl).toURL();
        } catch (Exception e) {
            throw new ReportableError(r.getString(R.string.error_invalid_url, mUrl), e);
        }

        HttpURLConnection huc;
        try {
            huc = (HttpURLConnection) url.openConnection();
        } catch (Exception e) {
            throw new ReportableError(r.getString(R.string.error_url_put_detail, mUrl, e.getMessage()), e);
        }

        huc.setDoOutput(true);
        OutputStreamWriter out;
        try {
            huc.setRequestMethod("PUT");
            out = new OutputStreamWriter(huc.getOutputStream());
            out.write(content);
            out.close();
        } catch (Exception e) {
            throw new ReportableError(r.getString(R.string.error_url_put_detail, mUrl, e.getMessage()), e);
        }
    }

    private void appendUrlFile(String url,
                               String content) throws NotFoundException, ReportableError {
    	String originalContent = this.fetchOrgFile(url);
    	String newContent = originalContent + '\n' + content;
    	this.putUrlFile(url, newContent);
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

