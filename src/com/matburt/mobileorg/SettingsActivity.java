package com.matburt.mobileorg;

import java.util.HashMap;
import java.util.Map;
import android.app.Activity;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Button;
import android.view.View;
import android.view.View.OnClickListener;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;

public class SettingsActivity extends Activity implements OnClickListener
{
    private Map<String, String> settings;
    private EditText webUrl;
    private EditText webUser;
    private EditText webPass;
    private Button saveButton;

    private SQLiteDatabase appdb;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings);
        this.settings = new HashMap<String, String>();
        this.webUrl = (EditText)this.findViewById(R.id.webURL);
        this.webUser = (EditText)this.findViewById(R.id.webUser);
        this.webPass = (EditText)this.findViewById(R.id.webPass);
        this.saveButton = (Button)this.findViewById(R.id.settingsSave);
        this.saveButton.setOnClickListener(this);
        this.initializeSettings();
    }

    public void initializeSettings() {
        this.appdb = this.openOrCreateDatabase("DatabaseName",
                                               MODE_PRIVATE, null);
        this.appdb.execSQL("CREATE TABLE IF NOT EXISTS settings"
                           + " (key VARCHAR, val VARCHAR);");
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
                this.populateDisplay();
            }
            else {
                this.appdb.execSQL("INSERT INTO settings (key, val)" +
                                   " VALUES ('webUrl', '')");
                this.appdb.execSQL("INSERT INTO settings (key, val)" +
                            " VALUES ('webUser', '')");
                this.appdb.execSQL("INSERT INTO settings (key, val)" +
                            " VALUES ('webPass', '')");
            }
        }
        this.appdb.close();
    }

    public void populateDisplay() {
        this.webUrl.setText(this.settings.get("webUrl"));
        this.webUser.setText(this.settings.get("webUser"));
        this.webPass.setText(this.settings.get("webPass"));
    }

    @Override
    public void onClick(View v) {
        this.onSave();
    }

    public void onSave() {
        this.appdb = this.openOrCreateDatabase("DatabaseName",
                                               MODE_PRIVATE, null);
        this.appdb.execSQL("UPDATE settings set val = '" + this.webUrl.getText() + "' where key = 'webUrl'");
        this.appdb.execSQL("UPDATE settings set val = '" + this.webUser.getText() + "' where key = 'webUser'");
        this.appdb.execSQL("UPDATE settings set val = '" + this.webPass.getText() + "' where key = 'webPass'");
        this.appdb.close();
        this.finish();
    }
}