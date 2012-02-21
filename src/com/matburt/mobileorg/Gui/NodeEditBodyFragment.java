package com.matburt.mobileorg.Gui;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.view.Menu;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

import com.matburt.mobileorg.R;

public class NodeEditBodyFragment extends Fragment {
	public static final String DISPLAY_STRING = "text";
	public static final String RESULT_STRING = "text";
    private EditText editDisplay;
	private String content;
	
    public NodeEditBodyFragment(String content) {
    	this.content = content;
    }
    
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
    	super.onCreate(savedInstanceState);
        View view = inflater.inflate(R.layout.editnodebody, container, false);
        this.editDisplay = (EditText) view.findViewById(R.id.textDisplay);
        
        setText(content);
        setHasOptionsMenu(true);
        return view;
    }

    public void setText(String text) {
        this.editDisplay.setText(text);
    }
    
    public String getText() {
    	return this.editDisplay.getText().toString();
    }
        
	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		super.onCreateOptionsMenu(menu, inflater);
		inflater.inflate(R.menu.nodeeditbody, menu);
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
		this.editDisplay.append(NodeEditActivity.getTimestamp());
	}
}
