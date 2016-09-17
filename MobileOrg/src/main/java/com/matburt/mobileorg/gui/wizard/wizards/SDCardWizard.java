package com.matburt.mobileorg.gui.wizard.wizards;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.widget.Button;
import android.widget.ListView;

import com.matburt.mobileorg.gui.wizard.FolderAdapter;
import com.matburt.mobileorg.gui.wizard.LocalDirectoryBrowser;
import com.matburt.mobileorg.R;

public class SDCardWizard extends AppCompatActivity {
	
	private FolderAdapter directoryAdapter;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.wizard_folder_pick_list);

		LocalDirectoryBrowser directory = new LocalDirectoryBrowser(this);
		directoryAdapter = new FolderAdapter(this, R.layout.folder_adapter_row,
				directory.listFiles());
		directoryAdapter
				.setDoneButton((Button) findViewById(R.id.wizard_done_button));
		directoryAdapter.setDirectoryBrowser(directory);

		ListView folderList = (ListView) findViewById(R.id.wizard_folder_list);
		folderList.setAdapter(directoryAdapter);
		directoryAdapter.notifyDataSetChanged();
	}
	
	public void saveSettings() {
		SharedPreferences appSettings = PreferenceManager
				.getDefaultSharedPreferences(this);
		SharedPreferences.Editor editor = appSettings.edit();
		
		editor.putString("syncSource", "sdcard");
		editor.putString("indexFilePath",
				directoryAdapter.getCheckedDirectory());
		
		editor.apply();
		finish();
	}
}
