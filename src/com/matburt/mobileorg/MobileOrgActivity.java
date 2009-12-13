package com.matburt.mobileorg;

import android.app.ListActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListView;
import android.widget.ArrayAdapter;
import android.content.Intent;
import android.util.Log;
import java.util.ArrayList;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;

public class MobileOrgActivity extends ListActivity
{
    private static final int OP_MENU_SETTINGS = 1;
    private static final int OP_MENU_SYNC = 2;
    private static final int OP_MENU_OUTLINE = 3;
    private static final int OP_MENU_CAPTURE = 4;
    private static final String LT = "MobileOrg";
    private ArrayList<String> menuList;
    private Synchronizer appSync;

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        this.initializeTables();
        menuList = new ArrayList<String>();
    }

    @Override
    public void onResume() {
        super.onResume();
        String[] allOrgList = this.getOrgFiles();
        menuList.clear();
        for (int idx = 0; idx < allOrgList.length; idx++) {
            menuList.add(allOrgList[idx]);
        }
        setListAdapter(new ArrayAdapter<String>(this,
                       android.R.layout.simple_list_item_1, allOrgList));
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        menu.add(0, MobileOrgActivity.OP_MENU_OUTLINE, 0, "Outline");
        menu.add(0, MobileOrgActivity.OP_MENU_CAPTURE, 0, "Capture");
        menu.add(0, MobileOrgActivity.OP_MENU_SYNC, 0, "Sync");
        menu.add(0, MobileOrgActivity.OP_MENU_SETTINGS, 0, "Settings");
        return true;
    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        String thisText = this.menuList.get(position);
        Intent dispIntent = new Intent();
        dispIntent.setClassName("com.matburt.mobileorg",
                                "com.matburt.mobileorg.SimpleTextDisplay");
        dispIntent.putExtra("fileValue", thisText);
        startActivity(dispIntent);
    }

    public boolean onShowSettings() {
        Intent settingsIntent = new Intent();
        settingsIntent.setClassName("com.matburt.mobileorg",
                                    "com.matburt.mobileorg.SettingsActivity");
        startActivity(settingsIntent);
        return true;
    }

    /* Handles item selections */
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case MobileOrgActivity.OP_MENU_SYNC:
            appSync = new Synchronizer(this);
            appSync.pull();
            return true;
        case MobileOrgActivity.OP_MENU_SETTINGS:
            return this.onShowSettings();
        case MobileOrgActivity.OP_MENU_OUTLINE:
            return true;
        case MobileOrgActivity.OP_MENU_CAPTURE:
            return true;
        }
        return false;
    }

    public String[] getOrgFiles() {
        ArrayList<String> allFiles = new ArrayList<String>();
        SQLiteDatabase appdb = this.openOrCreateDatabase("MobileOrg",
                                                         MODE_PRIVATE, null);
        Cursor result = appdb.rawQuery("SELECT name FROM files", null);
        if (result != null) {
            if (result.getCount() > 0) {
                result.moveToFirst();
                do {
                    Log.d(LT, "pulled " + result.getString(0));
                    allFiles.add(result.getString(0));
                } while(result.moveToNext());
            }
        }
        appdb.close();
        result.close();
        return (String[])allFiles.toArray(new String[0]);
    }

    public void initializeTables() {
        SQLiteDatabase appdb = this.openOrCreateDatabase("MobileOrg",
                                                         MODE_PRIVATE, null);
        appdb.execSQL("CREATE TABLE IF NOT EXISTS settings"
                      + " (key VARCHAR, val VARCHAR)");
        appdb.execSQL("CREATE TABLE IF NOT EXISTS files"
                      + " (file VARCHAR, name VARCHAR,"
                      + " checksum VARCHAR);");
        appdb.close();

    }
}
