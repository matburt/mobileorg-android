package com.matburt.mobileorg.Gui;

import android.os.Bundle;
import android.support.v4.app.ActionBar;
import android.support.v4.app.ActionBar.Tab;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;
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
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        return inflater.inflate(R.layout.editnode_body, container, false);
    }

    public void setText(String text) {
        this.editDisplay = (EditText) getActivity().findViewById(R.id.textDisplay);
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
