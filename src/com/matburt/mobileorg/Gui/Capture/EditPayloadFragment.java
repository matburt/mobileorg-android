package com.matburt.mobileorg.Gui.Capture;

import android.os.Bundle;
import android.text.InputFilter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

import com.actionbarsherlock.app.SherlockFragment;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import com.matburt.mobileorg.R;
import com.matburt.mobileorg.util.OrgUtils;

public class EditPayloadFragment extends SherlockFragment {
	public static final String DISPLAY_STRING = "text";
	public static final String RESULT_STRING = "text";
    private EditText editDisplay;

    private String content;
	private boolean enabled;
    
    public void init(String content, boolean enabled) {
    	this.content = content;
    	this.enabled = enabled;
    }
	
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
    	super.onCreate(savedInstanceState);
        View view = inflater.inflate(R.layout.edit_body, container, false);
        this.editDisplay = (EditText) view.findViewById(R.id.textDisplay);

        setText(this.content);
        this.editDisplay.setEnabled(enabled);
        
        setHasOptionsMenu(enabled);
        return view;
    }

    public void setText(String text) {
        // work around Samsung's default limit of 9000 chars per text field
        this.editDisplay.setFilters(new InputFilter[0]);
        this.editDisplay.setText(text);
        this.editDisplay.setSelection(text.length());
    }
    
    public String getText() {
    	return this.editDisplay.getText().toString();
    }

	public boolean hasEdits() {
            return (!getText().equals(content));
    }
	
	
	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		super.onCreateOptionsMenu(menu, inflater);
		inflater.inflate(R.menu.edit_body, menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// TODO Auto-generated method stub
		switch (item.getItemId()) {
		case R.id.nodeeditbody_timestamp:
			insertTimestamp();
			return true;
		}
		return false;	
	}
	
	public void insertTimestamp() {
		this.editDisplay.append(OrgUtils.getTimestamp());
	}
}
