package com.matburt.mobileorg.Gui;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import com.matburt.mobileorg.R;

public class NodeEditBodyActivity extends Activity
{
	public static final String DISPLAY_STRING = "text";
	public static final String RESULT_STRING = "text";
    private EditText editDisplay;
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.editnode_body);
      
        Intent intent = getIntent();
        String srcText = intent.getStringExtra(DISPLAY_STRING);
        this.editDisplay = (EditText)this.findViewById(R.id.textDisplay);
        this.editDisplay.setText(srcText);
        
        Button button = (Button)this.findViewById(R.id.save);
        button.setOnClickListener(saveListener);
        button = (Button)findViewById(R.id.cancel);
        button.setOnClickListener(cancelListener);
    }
    
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
	    inflater.inflate(R.menu.nodeeditbody_menu, menu);
		return true;
	}
    
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.nodeeditbody_timestamp:
			insertTimestamp();
			return true;
		}
		return false;
	}
	
	private void insertTimestamp() {
		this.editDisplay.append(NodeEditActivity.getTimestamp());
	}
	
	View.OnClickListener saveListener = new View.OnClickListener() {
		public void onClick(View v) {
			Intent result = new Intent();
			result.putExtra(RESULT_STRING, editDisplay.getText().toString());
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
}
