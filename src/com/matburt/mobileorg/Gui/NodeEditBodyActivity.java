package com.matburt.mobileorg.Gui;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

import com.matburt.mobileorg.R;

public class NodeEditBodyActivity extends Fragment {
	public static final String DISPLAY_STRING = "text";
	public static final String RESULT_STRING = "text";
    private EditText editDisplay;
      
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
	}
	
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
    	super.onCreate(savedInstanceState);
        View view = inflater.inflate(R.layout.editnodebody, container, false);
        this.editDisplay = (EditText) view.findViewById(R.id.textDisplay);
        
        setText("W00t");
        return view;
    }

    public void setText(String text) {
        this.editDisplay.setText(text);
    }
    
//	@Override
//	public boolean onOptionsItemSelected(android.support.v4.view.MenuItem item) {
//		switch (item.getItemId()) {
//		case R.id.nodeeditbody_timestamp:
//			insertTimestamp();
//			return true;
//		}
//		return false;
//	}
//	
//	private void insertTimestamp() {
//		this.editDisplay.append(NodeEditActivity.getTimestamp());
//	}


}
