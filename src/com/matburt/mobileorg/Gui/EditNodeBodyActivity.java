package com.matburt.mobileorg.Gui;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import com.matburt.mobileorg.R;
import com.matburt.mobileorg.Parsing.CreateEditNote;

public class EditNodeBodyActivity extends Activity
{
    private EditText orgEditDisplay;
    
    private String editType = null;
    private String srcText = null;
    private CreateEditNote noteCreator = null;
    public static final String LT = "MobileOrg";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.editnode_body);
  
        this.noteCreator = new CreateEditNote(this);

        this.orgEditDisplay = (EditText)this.findViewById(R.id.orgEditTxt);
    
        populateDisplay();
        
        Button button = (Button)this.findViewById(R.id.save);
        button.setOnClickListener(saveListener);
        button = (Button)findViewById(R.id.cancel);
        button.setOnClickListener(cancelListener);
    }

    private void populateDisplay() {
        Intent txtIntent = getIntent();

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

    
	View.OnClickListener saveListener = new View.OnClickListener() {
		public void onClick(View v) {
			if (orgEditDisplay.getText().toString().length() > 0) {
				if (editType == null) {
					noteCreator.writeNewNote(orgEditDisplay.getText()
							.toString());
				}
			}
			Intent result = new Intent();
			result.putExtra("text", orgEditDisplay.getText().toString());
			setResult(RESULT_OK, result);
			finish();
		}
	};

	View.OnClickListener cancelListener = new View.OnClickListener() {
		public void onClick(View v) {
			setResult(RESULT_CANCELED);
			finish();
		}
	};

	@Override
	public void onDestroy() {
		super.onDestroy();
		if (this.noteCreator != null) {
			this.noteCreator.close();
		}
	}
}
