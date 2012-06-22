package com.matburt.mobileorg.Gui.Capture;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.view.Menu;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.text.InputFilter;

import com.matburt.mobileorg.R;

public class EditPayloadFragment extends Fragment {
	public static final String DISPLAY_STRING = "text";
	public static final String RESULT_STRING = "text";
    private EditText editDisplay;

    private String orig_content;
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
            return (getText() != content);
        }
        
	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		super.onCreateOptionsMenu(menu, inflater);
		inflater.inflate(R.menu.edit_body, menu);
	}

	@Override
	public boolean onOptionsItemSelected(android.support.v4.view.MenuItem item) {
		switch (item.getItemId()) {
		case R.id.nodeeditbody_timestamp:
			insertTimestamp();
			return true;
		}
		return false;
	}
	
	public void insertTimestamp() {
		this.editDisplay.append(EditActivity.getTimestamp());
	}
}
