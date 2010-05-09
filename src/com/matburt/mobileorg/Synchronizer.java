package com.matburt.mobileorg;

import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.HttpEntity;
import org.apache.http.entity.StringEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.auth.AuthScope;
import java.util.HashMap;
import java.util.Map;
import java.util.ArrayList;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.net.URL;
import java.net.MalformedURLException;
import java.io.BufferedWriter;
import java.io.BufferedReader;
import java.io.StringWriter;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.FileOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.FileNotFoundException;
import java.io.OutputStreamWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.FileReader;
import java.io.UnsupportedEncodingException;

import android.app.Activity;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;
import android.os.Environment;

public class Synchronizer
{
    private Map<String, String> appSettings;
    private Activity rootActivity;
    private static final String LT = "MobileOrg";

    Synchronizer(Activity parentActivity) {
        this.rootActivity = parentActivity;
        this.appSettings = new HashMap<String, String>();
        if (this.populateApplicationSettings() == -1) {
            Log.e(LT, "Failed to fetch settings"); //show an error
        }
    }

    public boolean push() {
        String urlActual = this.getRootUrl() + "mobileorg.org";
        String storageMode = this.appSettings.get("storage");
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
                return true;
            }
        }
        else if (storageMode.equals("sdcard")) {
            try {
                File root = Environment.getExternalStorageDirectory();
                File morgDir = new File(root, "mobileorg");
                File morgFile = new File(morgDir, "mobileorg.org");
                if (!morgFile.exists()) {
                    Log.i(LT, "Did not find mobileorg.org file, not pushing.");
                    return true;
                }
                FileReader orgFReader = new FileReader(morgFile);
                reader = new BufferedReader(orgFReader);
            }
            catch (java.io.IOException e) {
                Log.e(LT, "IO Exception initilizing reader on sdcard file");
            }
        }
        else {
            Log.e(LT, "[Push] Unknown storage mechanism: " + storageMode);
            return false;
        }

        String thisLine = "";
        try {
            while ((thisLine = reader.readLine()) != null) {
                fileContents += thisLine + "\n";
            }
        }
        catch (java.io.IOException e) {
            Log.e(LT, "IO Exception trying to read from mobileorg.org file");
            return false;
        }

        DefaultHttpClient httpC = this.createConnection(this.appSettings.get("webUser"),
                                                        this.appSettings.get("webPass"));
        this.putUrlFile(urlActual, httpC, fileContents);
        this.removeFile("mobileorg.org");

        if (storageMode.equals("internal") || storageMode == null) {
            this.rootActivity.deleteFile("mobileorg.org");
        }
        else if (storageMode.equals("sdcard")) {
            File root = Environment.getExternalStorageDirectory();
            File morgDir = new File(root, "mobileorg");
            File morgFile = new File(morgDir, "mobileorg.org");
            morgFile.delete();
        }            
        return true;
    }

    public boolean pull() {
        Pattern checkUrl = Pattern.compile("http.*\\.(?:org|txt)$");
        if (!checkUrl.matcher(this.appSettings.get("webUrl")).find()) {
            Log.e(LT, "Bad URL");
            return false;
        }

        //Get the index org file
        String masterStr = this.fetchOrgFile(this.appSettings.get("webUrl"));
        if (masterStr == "") {
            Log.e(LT, "Failure getting main org file");
            return false;
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
            String storageMode = this.appSettings.get("storage");
            BufferedWriter writer = new BufferedWriter(new StringWriter());

            if (storageMode.equals("internal") || storageMode == null) {
                FileOutputStream fs;
                try {
                    fs = rootActivity.openFileOutput(
                                                     masterList.get(key), 0);
                    writer = new BufferedWriter(new OutputStreamWriter(fs));
                }
                catch (java.io.FileNotFoundException e) {
                    Log.e(LT, "Could not write to file: " +
                          masterList.get(key));
                    return false;
                }
                catch (java.io.IOException e) {
                    Log.e(LT, "IO Exception initializing writer on file " +
                          masterList.get(key));
                }
            }
            else if (storageMode.equals("sdcard")) {

                try {
                    File root = Environment.getExternalStorageDirectory();
                    File morgDir = new File(root, "mobileorg");
                    morgDir.mkdir();
                    if (morgDir.canWrite()){
                        File orgFileCard = new File(morgDir, masterList.get(key));
                        FileWriter orgFWriter = new FileWriter(orgFileCard);
                        writer = new BufferedWriter(orgFWriter);
                    }
                    else {
                        Log.e(LT, "Write permission denied");
                    }
                } catch (java.io.IOException e) {
                    Log.e(LT, "IO Exception initializing writer on sdcard file");
                }
            }
            else {
                Log.e(LT, "[Sync] Unknown storage mechanism: " + storageMode);
                return false;
            }

            try {
            	writer.write(fileContents);
            	this.addOrUpdateFile(masterList.get(key), key);
                writer.flush();
                writer.close();
            }
            catch (java.io.IOException e) {
                Log.e(LT, "IO Exception trying to write file " +
                      masterList.get(key));
                return false;
            }
        }

        return true;
    }

    public void removeFile(String filename) {
        SQLiteDatabase appdb = this.rootActivity.openOrCreateDatabase("MobileOrg",
                                          0, null);
        appdb.execSQL("DELETE FROM files " +
                      "WHERE file = '"+filename+"'");
        Log.i(LT, "Finished deleting from files");
        appdb.close();
    }

    public void addOrUpdateFile(String filename, String name) {
        SQLiteDatabase appdb = this.rootActivity.openOrCreateDatabase("MobileOrg",
                                          0, null);
        Cursor result = appdb.rawQuery("SELECT * FROM files " +
                                       "WHERE file = '"+filename+"'", null);
        if (result != null) {
            if (result.getCount() > 0) {
                appdb.execSQL("UPDATE files set name = '"+name+"', "+
                              "checksum = '' where file = '"+filename+"'");
            }
            else {
                appdb.execSQL("INSERT INTO files (file, name, checksum) " +
                              "VALUES ('"+filename+"','"+name+"','')");
            }
        }
        result.close();
        appdb.close();
    }

    public String fetchOrgFile(String orgUrl) {
        DefaultHttpClient httpC = this.createConnection(
                                        this.appSettings.get("webUser"),
                                        this.appSettings.get("webPass"));
        InputStream mainFile = this.getUrlStream(orgUrl, httpC);
        String masterStr = "";
        try {
            if (mainFile == null) {
                Log.w(LT, "Stream is null");
                return ""; //Raise exception
            }
            masterStr = this.ReadInputStream(mainFile);
            Log.d(LT, masterStr);
        }
        catch (IOException e) {
            Log.e(LT, "Error reading input stream for URL");
            return ""; //Raise exception
        }
        return masterStr;
    }

    public String getRootUrl() {
        URL manageUrl;
        try {
            manageUrl = new URL(this.appSettings.get("webUrl"));
        }
        catch (MalformedURLException e) {
            Log.e(LT, "Malformed URL");
            return ""; //raise exception
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

    public HashMap<String, String> getOrgFilesFromMaster(String master) {
        Pattern getOrgFiles = Pattern.compile("\\[file:(.*?\\.org)\\]\\[(.*?)\\]\\]");
        Matcher m = getOrgFiles.matcher(master);
        HashMap<String, String> allOrgFiles = new HashMap<String, String>();
        while (m.find()) {
            allOrgFiles.put(m.group(2), m.group(1));
        }

        return allOrgFiles;
    }

    public DefaultHttpClient createConnection(String user, String password) {
        DefaultHttpClient httpClient = new DefaultHttpClient();
        UsernamePasswordCredentials bCred = new UsernamePasswordCredentials(user, password);
        BasicCredentialsProvider cProvider = new BasicCredentialsProvider();
        cProvider.setCredentials(AuthScope.ANY, bCred);
        httpClient.setCredentialsProvider(cProvider);
        return httpClient;
    }

    public InputStream getUrlStream(String url, DefaultHttpClient httpClient) {
        try {
            HttpResponse res = httpClient.execute(new HttpGet(url));
            return res.getEntity().getContent();
        }
        catch (IOException e) {
            Log.e(LT, e.toString());
            Log.w(LT, "Failed to get URL");
            return null; //handle exception
        }
    }

    public void putUrlFile(String url,
                           DefaultHttpClient httpClient,
                           String content) {
        try {
            HttpPut httpPut = new HttpPut(url);
            httpPut.setEntity(new StringEntity(content));
            HttpResponse response = httpClient.execute(httpPut);
            httpClient.getConnectionManager().shutdown();
        }
        catch (UnsupportedEncodingException e) {
            Log.e(LT, "Encountered unsupported encoding pushing mobileorg.org file");
        }
        catch (IOException e) {
            Log.e(LT, "Encountered IO Exception pushing mobileorg.org file");
        }
    }

    public int populateApplicationSettings() {
        SQLiteDatabase appdb = this.rootActivity.openOrCreateDatabase(
                                     "MobileOrg", 0, null);
        Cursor result = appdb.rawQuery("SELECT * FROM settings", null);
        int rc = 0;
        if (result != null) {
            if (result.getCount() > 0) {
                result.moveToFirst();
                do {
                    this.appSettings.put(result.getString(0),
                                         result.getString(1));
                } while (result.moveToNext());
            }
            else {
                rc = -1;// need to start settings display
            }
        }
        else {
            rc =  -1;// need to start settings display
        }
        result.close();
        appdb.close();
        return rc;
    }

    public String ReadInputStream(InputStream in) throws IOException {
        StringBuffer stream = new StringBuffer();
        byte[] b = new byte[4096];
        for (int n; (n = in.read(b)) != -1;) {
            stream.append(new String(b, 0, n));
        }
        return stream.toString();
    }
}