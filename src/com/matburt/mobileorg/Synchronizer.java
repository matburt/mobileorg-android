package com.matburt.mobileorg;

import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.auth.AuthScope;
import java.util.HashMap;
import java.util.Map;
import java.io.InputStream;
import java.io.IOException;
import android.app.Activity;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

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

    public boolean pull() {
        DefaultHttpClient httpC = this.createConnection(
                                        this.appSettings.get("webUser"),
                                        this.appSettings.get("webPass"));
        InputStream mainFile = this.getUrlStream(this.appSettings.get("webUrl"),
                                                 httpC);
        try {
            if (mainFile == null) {
                Log.w(LT, "Stream is null");
                return false;
            }
            Log.d(LT, this.ReadInputStream(mainFile));
        }
        catch (IOException e) {
            Log.e(LT, "Error reading input stream for URL");
        }
        return true;
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