package com.matburt.mobileorg.Gui.Wizard;

import java.util.ArrayList;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.TextView;

import com.matburt.mobileorg.R;

public class FolderAdapter extends ArrayAdapter<String> {
	private int currentChecked = -1;
	private DirectoryBrowser<?> directory;
	private Button doneButton;

	public FolderAdapter(Context context, int resource, ArrayList<String> list) {
		super(context, resource, list);
	}

	public void setDirectoryBrowser(DirectoryBrowser<?> d) {
		directory = d;
	}

	public String getCheckedDirectory() {
		if (currentChecked == -1)
			return "";
		// (Toast.makeText(context, directory.getAbsolutePath(currentChecked),
		// Toast.LENGTH_LONG)).show();
		return directory.getAbsolutePath(currentChecked);
	}

	public void setDoneButton(Button b) {
		doneButton = b;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		View row = convertView;
		TextView folder = null;
		CheckBox check = null;
		if (row == null) {
			LayoutInflater inflater = LayoutInflater.from(getContext());
			row = inflater.inflate(R.layout.folder_adapter_row, parent, false);
			folder = (TextView) row.findViewById(R.id.folder);
			folder.setOnClickListener(new View.OnClickListener() {

				@Override
				public void onClick(View folder) {
					int position = (Integer) folder.getTag();
					// FolderAdapter.this.clear();
					directory.browseTo(position);
					currentChecked = -1;
					FolderAdapter.this.notifyDataSetChanged();
					doneButton.setEnabled(false);
				}
			});
			check = (CheckBox) row.findViewById(R.id.checkbox);
			check.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {

				public void onCheckedChanged(CompoundButton buttonView,
						boolean isChecked) {
					// update last checked position
					int position = (Integer) buttonView.getTag();
					if (isChecked)
						currentChecked = position;
					else if (currentChecked == position)
						currentChecked = -1;
					FolderAdapter.this.notifyDataSetChanged();
					if (isChecked)
						doneButton.setEnabled(true);
				}
			});
		}
		folder = (TextView) row.findViewById(R.id.folder);
		folder.setText(directory.getDirectoryName(position));
		folder.setTag(Integer.valueOf(position));
		check = (CheckBox) row.findViewById(R.id.checkbox);
		// disable the "Up one level" checkbox; otherwise make sure its enabled
		if (position == 0 && !directory.isCurrentDirectoryRoot())
			check.setEnabled(false);
		else
			check.setEnabled(true);
		check.setTag(Integer.valueOf(position));
		// set check state. only one can be checked
		boolean status = (currentChecked == position) ? true : false;
		check.setChecked(status);
		return (row);
	}
}
