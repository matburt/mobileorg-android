package com.matburt.mobileorg;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.content.Intent;
import android.widget.TextView;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import java.io.InputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.FileNotFoundException;

public class SimpleTextDisplay extends Activity
{
    private TextView orgDisplay;
    public static final String LT = "MobileOrg";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.simpletext);
        this.orgDisplay = (TextView)this.findViewById(R.id.orgTxt);
        this.poplateDisplay();
    }

    public void poplateDisplay() {
        Intent txtIntent = getIntent();
        String srcName = txtIntent.getStringExtra("fileValue");
        SQLiteDatabase appdb = this.openOrCreateDatabase("MobileOrg",
                                                         MODE_PRIVATE, null);
        Cursor result = appdb.rawQuery("SELECT file from files" +
                                       " WHERE name = '" + srcName + "'", null);
        String displayTxt;
        if (result != null) {
            if (result.getCount() > 0) {
                result.moveToFirst();
                Log.d(LT, "Reading file: " + result.getString(0));
                displayTxt = this.readOrgFile(result.getString(0));
                Log.d(LT, "Running file through Parser");
                OrgFileParser ofp = new OrgFileParser(result.getString(0));
                ofp.parse();
            }
            else {
                displayTxt = "File not found";
            }
        }
        else {
            displayTxt = "Database error";
        }
        Log.d(LT, "Setting text to: " + displayTxt);
        this.orgDisplay.setText(displayTxt);
    }

    public String readOrgFile(String orgFile) {
        FileInputStream fs;
        String output = "";
        try {
            fs = this.openFileInput(orgFile);
        }
        catch (java.io.FileNotFoundException e) {
            Log.e(LT, "Could not read file");
            return "Could not read file";
        }
        Log.d(LT, "File opened");
        try {
            byte[] reader = new byte[fs.available()];
            Log.d(LT, "Read loop");
            fs.read(reader);
            Log.d(LT, "Read all data");
            output = new String(reader);
        } catch(IOException e) {
            Log.e(LT, e.getMessage());
        } finally {
            if (fs != null) {
                try {
                    fs.close();
                } catch (IOException e) {
                    Log.e(LT, e.getMessage());
                }
            }
        }
        Log.d(LT, "Returning data");
        return output;
    }
}