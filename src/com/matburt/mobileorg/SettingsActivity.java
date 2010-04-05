package com.matburt.mobileorg;

import java.util.HashMap;
import java.util.Map;
import java.util.ArrayList;
import android.app.Activity;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Button;
import android.view.View;
import android.view.View.OnClickListener;
import android.database.Cursor;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;

public class SettingsActivity extends Activity implements OnClickListener
{
    private Map<String, String> settings;
    private EditText webUrl;
    private EditText webUser;
    private EditText webPass;
    private Button saveButton;
    RadioGroup storegrp;
    RadioButton sdcard;
    RadioButton internal;

    private SQLiteDatabase appdb;

    public static String settingsList[] = {"webUrl", "webUser", "webPass", "storage"};

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings);
        this.settings = new HashMap<String, String>();
        this.webUrl = (EditText)this.findViewById(R.id.webURL);
        this.webUser = (EditText)this.findViewById(R.id.webUser);
        this.webPass = (EditText)this.findViewById(R.id.webPass);
        this.saveButton = (Button)this.findViewById(R.id.settingsSave);
        this.storegrp = (RadioGroup) findViewById(R.id.storegrp); 
        this.internal = (RadioButton) findViewById(R.id.internalopt);
        this.sdcard = (RadioButton) findViewById(R.id.sdcardopt);
        this.saveButton.setOnClickListener(this);
        this.initializeSettings();
    }

    public void initializeSettings() {
        this.appdb = this.openOrCreateDatabase("MobileOrg",
                                               MODE_PRIVATE, null);
        Cursor result = this.appdb.rawQuery("SELECT *" +
                                         " FROM settings",
                                         null);
        this.settings.clear();
        if (result != null) {
            if (result.getCount() > 0) {
                result.moveToFirst();
                do {
                    this.settings.put(result.getString(0),
                                      result.getString(1));
                } while (result.moveToNext());
                for (int idx = 0; idx < this.settingsList.length; idx++) {
                    if (!this.settings.containsKey(this.settingsList[idx])) {
                        if (this.settingsList[idx].equals("storage")) {
                            this.settings.put("storage", "internal");
                        }
                        else {
                            this.settings.put(this.settingsList[idx], "");
                        }
                        this.appdb.execSQL("INSERT INTO settings (key, val)" +
                                           " VALUES ('" + this.settingsList[idx] +"','"+
                                           this.settings.get(this.settingsList[idx]) + "');");
                    }
                }
                this.populateDisplay();
            }
            else {
                this.appdb.execSQL("INSERT INTO settings (key, val)" +
                                   " VALUES ('webUrl', '')");
                this.appdb.execSQL("INSERT INTO settings (key, val)" +
                                   " VALUES ('webUser', '')");
                this.appdb.execSQL("INSERT INTO settings (key, val)" +
                                   " VALUES ('webPass', '')");
                this.appdb.execSQL("INSERT INTO settings (key, val)" +
                                   " VALUES ('storage', 'internal')");
            }
        }
        result.close();
        this.appdb.close();
    }

    public void populateDisplay() {
        this.webUrl.setText(this.settings.get("webUrl"));
        this.webUser.setText(this.settings.get("webUser"));
        this.webPass.setText(this.settings.get("webPass"));
        if (this.settings.get("storage").equals("internal")) {
            this.storegrp.check(this.internal.getId());
        }
        else if (this.settings.get("storage").equals("sdcard")) {
            this.storegrp.check(this.sdcard.getId());
        }
    }

    public void onClick(View v) {
        this.onSave();
    }

    public void onSave() {
        this.appdb = this.openOrCreateDatabase("MobileOrg",
                                               MODE_PRIVATE, null);
        this.appdb.execSQL("UPDATE settings set val = '" + this.webUrl.getText() + "' where key = 'webUrl'");
        this.appdb.execSQL("UPDATE settings set val = '" + this.webUser.getText() + "' where key = 'webUser'");
        this.appdb.execSQL("UPDATE settings set val = '" + this.webPass.getText() + "' where key = 'webPass'");
        int opId = this.storegrp.getCheckedRadioButtonId();
        if (opId == this.internal.getId()) {
            this.appdb.execSQL("UPDATE settings set val = 'internal' " +
                               "where key = 'storage'");
        }
        else if (opId == this.sdcard.getId()) {
            this.appdb.execSQL("UPDATE settings set val = 'sdcard' " +
                               "where key = 'storage'");
        }
        this.appdb.close();
        this.finish();
    }
}