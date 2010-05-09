package com.matburt.mobileorg;

import android.app.Activity;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.content.Intent;
import android.widget.EditText;
import android.widget.Button;
import android.view.View;
import android.view.View.OnClickListener;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import java.io.OutputStreamWriter;
import java.io.BufferedWriter;
import java.io.StringWriter;
import java.io.FileWriter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.FileNotFoundException;
import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class Capture extends Activity implements OnClickListener
{
    private EditText orgEditDisplay;
    private Button saveButton;
    private Map<String, String> appSettings;
    public static final String LT = "MobileOrg";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.appSettings = new HashMap<String, String>();
        this.populateApplicationSettings();
        setContentView(R.layout.simpleedittext);
        this.saveButton = (Button)this.findViewById(R.id.captureSave);
        this.orgEditDisplay = (EditText)this.findViewById(R.id.orgEditTxt);
        this.saveButton.setOnClickListener(this);
    }

    public boolean onSave() {
        String newNote = this.transformBuffer(this.orgEditDisplay.getText().toString());
        String storageMode = this.appSettings.get("storage");
        BufferedWriter writer = new BufferedWriter(new StringWriter());
        
        if (storageMode.equals("internal") || storageMode == null) {
            FileOutputStream fs;
            try {
                fs = this.openFileOutput("mobileorg.org", MODE_APPEND);
                writer = new BufferedWriter(new OutputStreamWriter(fs));
            }
            catch (java.io.FileNotFoundException e) {
                Log.e(LT, "Caught FNFE trying to open mobileorg.org file");
            }
            catch (java.io.IOException e) {
                Log.e(LT, "IO Exception initializing writer on file mobileorg.org");
            }
        }
        else if (storageMode.equals("sdcard")) {
            try {
                File root = Environment.getExternalStorageDirectory();
                File morgDir = new File(root, "mobileorg");
                morgDir.mkdir();
                if (morgDir.canWrite()){
                    File orgFileCard = new File(morgDir, "mobileorg.org");
                    FileWriter orgFWriter = new FileWriter(orgFileCard, true);
                    writer = new BufferedWriter(orgFWriter);
                }
                else {
                    Log.e(LT, "Write permission denied");
                    return false;
                }
            } catch (java.io.IOException e) {
                Log.e(LT, "IO Exception initializing writer on sdcard file");
                return false;
            }
        }
        else {
            Log.e(LT, "Unknown storage mechanism " + storageMode);
            return false;
        }

        try {
            writer.write(newNote);
            this.addOrUpdateFile("mobileorg.org", "MobileOrg");
            writer.flush();
            writer.close();
        }
        catch (java.io.IOException e) {
            Log.e(LT, "IO Exception trying to write file mobileorg.org");
            return false;
        }
        this.finish();
        return true;
    }

    public void onClick(View v) {
        if (!this.onSave()) {
            Log.e(LT, "Failed to save file");
        }
    }

    public String transformBuffer(String givenText) {
        String xformed = "";
        if (!givenText.substring(0,1).equals("*")) {
            xformed = "* " + givenText;
        }
        else {
            xformed = givenText;
        }
        return xformed + "\n\n";
    }

    public void addOrUpdateFile(String filename, String name) {
        SQLiteDatabase appdb = openOrCreateDatabase("MobileOrg",
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

    public int populateApplicationSettings() {
        SQLiteDatabase appdb = this.openOrCreateDatabase(
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
}