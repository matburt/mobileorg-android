package com.matburt.mobileorg;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.content.Intent;
import android.content.SharedPreferences;
import android.widget.EditText;
import android.widget.Button;
import android.view.View;
import android.view.View.OnClickListener;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.preference.PreferenceManager;
import android.content.Context;

public class Capture extends Activity implements OnClickListener
{
    private EditText orgEditDisplay;
    private Button saveButton;
    private boolean editMode = false;
    private String id = null;
    private String editType = null;
    private String srcText = null;
    private String nodeTitle = null;
    private CreateEditNote noteCreator = null;
    private MobileOrgApplication appinst;
    public static final String LT = "MobileOrg";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.simpleedittext);
        this.noteCreator = new CreateEditNote(this);
        this.saveButton = (Button)this.findViewById(R.id.captureSave);
        this.orgEditDisplay = (EditText)this.findViewById(R.id.orgEditTxt);
        this.saveButton.setOnClickListener(this);
        this.populateDisplay();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (this.noteCreator != null) {
            this.noteCreator.close();
        }
    }

    public boolean onSave() {
        if (this.orgEditDisplay.getText().toString().length() > 0) {
            if (this.editType == null) {
                this.noteCreator.writeNewNote(this.orgEditDisplay.getText().toString());
            } else {
                this.noteCreator.editNote(this.editType,
                                          this.id,
                                          this.nodeTitle,
                                          this.srcText,
                                          this.orgEditDisplay.getText().toString());
            }
        }

        //triggers a refresh of the main display
        this.appinst.rootNode = null;
        this.finish();
        return true;
    }

    public void onClick(View v) {
        if (!this.onSave()) {
            Log.e(LT, "Failed to save file");
        }
    }

    public void populateDisplay() {
        Intent txtIntent = getIntent();
        this.srcText = txtIntent.getStringExtra("txtValue");
        this.id = txtIntent.getStringExtra("nodeId");
        this.editType = txtIntent.getStringExtra("editType");
        this.orgEditDisplay.setText(this.srcText);
        this.nodeTitle = txtIntent.getStringExtra("nodeTitle");
        this.appinst = (MobileOrgApplication)this.getApplication();
    }
}
