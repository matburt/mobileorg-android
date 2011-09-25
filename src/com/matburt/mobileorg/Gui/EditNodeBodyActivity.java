package com.matburt.mobileorg.Gui;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;

import com.matburt.mobileorg.R;
import com.matburt.mobileorg.Parsing.CreateEditNote;

public class EditNodeBodyActivity extends Activity implements OnClickListener
{
    private EditText orgEditDisplay;
    private Button saveButton;
    private Button advancedButton;
    
    private String editType = null;
    private String srcText = null;
    private CreateEditNote noteCreator = null;
    public static final String LT = "MobileOrg";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.editnode_body);
        this.noteCreator = new CreateEditNote(this);
        this.saveButton = (Button)this.findViewById(R.id.captureSave);
        this.advancedButton = (Button)this.findViewById(R.id.captureAdvanced);
        this.orgEditDisplay = (EditText)this.findViewById(R.id.orgEditTxt);
        this.saveButton.setOnClickListener(this);
        this.advancedButton.setOnClickListener(this);
        this.populateDisplay();
    }

    private void populateDisplay() {
        Intent txtIntent = getIntent();
        String actionMode =  txtIntent.getStringExtra("actionMode");
        if (actionMode == null) {
            this.advancedButton.setVisibility(View.GONE);
        }
        this.srcText = txtIntent.getStringExtra("txtValue");
		if(this.srcText == null || this.srcText.length() == 0) {
			String subject = txtIntent.getStringExtra("android.intent.extra.SUBJECT");
			String text = txtIntent.getStringExtra("android.intent.extra.TEXT");

			if(subject == null) {
				subject = "";
			} else {
				subject += "\n";
			}

			if(text == null) {
				text = "";
			}

			if(text.startsWith("http")) {
				this.srcText = "[["+text.trim()+"]["+subject.trim()+"]]";
			} else {
				this.srcText = subject + text;
			}
		}
        //txtIntent.getStringExtra("nodeId");
        this.editType = txtIntent.getStringExtra("editType");
        this.orgEditDisplay.setText(this.srcText);
        //txtIntent.getStringExtra("nodeTitle");
    }
    
    @Override
    public void onDestroy() {
        super.onDestroy();
        if (this.noteCreator != null) {
            this.noteCreator.close();
        }
    }

    @Override
    public void onClick(View v) {
        if (v == this.advancedButton) {
            Log.i(LT, "Advanced");
            Intent advancedIntent = new Intent(this, EditNodeActivity.class);
            advancedIntent.putExtra("actionMode", "create");
            startActivity(advancedIntent);
            this.finish();
        }
        else if (v == this.saveButton) {
            if (!this.onSave()) {
                Log.e(LT, "Failed to save file");
            }
        }
    }

    
    private boolean onSave() {
        if (this.orgEditDisplay.getText().toString().length() > 0) {
            if (this.editType == null) {
                this.noteCreator.writeNewNote(this.orgEditDisplay.getText().toString());
            }
        }
        Intent result = new Intent();
        result.putExtra("text", this.orgEditDisplay.getText().toString());
        this.setResult(RESULT_OK, result);
        this.finish();
        return true;
    }
}
