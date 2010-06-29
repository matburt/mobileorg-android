package com.matburt.mobileorg;

import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.content.Context;
import android.app.Application;
import java.util.ArrayList;
import android.util.Log;

public class MobileOrgDatabase {
    private Context appcontext;
    public SQLiteDatabase appdb;
    public static final String LT = "MobileOrg";

    public MobileOrgDatabase(Context appctxt) {
        this.appcontext = appctxt;
        this.initialize();
    }

    public void initialize() {
        this.appdb = this.appcontext.openOrCreateDatabase("MobileOrg", 0, null);
        this.appdb.execSQL("CREATE TABLE IF NOT EXISTS files"
                           + " (file VARCHAR, name VARCHAR,"
                           + " checksum VARCHAR);");
        this.appdb.execSQL("CREATE TABLE IF NOT EXISTS data"
                      + "  (id INTEGER PRIMARY KEY AUTOINCREMENT,"
                      + "   heading VARCHAR, type INTEGER,"
                      + "   content VARCHAR, parentid INTEGER);");
    }

    public ArrayList<String> getOrgFiles() {
        ArrayList<String> allFiles = new ArrayList<String>();
        Cursor result = this.appdb.rawQuery("SELECT file FROM files", null);
        if (result != null) {
            if (result.getCount() > 0) {
                result.moveToFirst();
                do {
                    Log.d(LT, "pulled " + result.getString(0));
                    allFiles.add(result.getString(0));
                } while(result.moveToNext());
            }
        }
        result.close();
        return allFiles;
    }

    public void removeFile(String filename) {
        this.appdb.execSQL("DELETE FROM files " +
                           "WHERE file = '"+filename+"'");
        Log.i(LT, "Finished deleting from files");
    }

    public void clearData() {
        this.appdb.execSQL("DELETE FROM data");
    }

    public void addOrUpdateFile(String filename, String name) {
        Cursor result = this.appdb.rawQuery("SELECT * FROM files " +
                                       "WHERE file = '"+filename+"'", null);
        if (result != null) {
            if (result.getCount() > 0) {
                this.appdb.execSQL("UPDATE files set name = '"+name+"', "+
                              "checksum = '' where file = '"+filename+"'");
            }
            else {
                this.appdb.execSQL("INSERT INTO files (file, name, checksum) " +
                              "VALUES ('"+filename+"','"+name+"','')");
            }
        }
        result.close();
    }
}