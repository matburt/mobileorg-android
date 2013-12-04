package com.matburt.mobileorg.Gui.Wizard.Wizards;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ListView;

import com.matburt.mobileorg.R;
import com.matburt.mobileorg.Gui.Wizard.FolderAdapter;
import com.matburt.mobileorg.Gui.Wizard.LocalDirectoryBrowser;
import com.matburt.mobileorg.Gui.Wizard.WizardView;

public class SDCardWizard extends Wizard {
	
	private FolderAdapter directoryAdapter;

	public SDCardWizard(WizardView wizardView, Context context) {
		super(wizardView, context);
	}
	

	@Override
	public void setupFirstPage() {
		createSDcardFolderSelector();
	}
	
	public View createSDcardFolderSelector() {		
		View view = LayoutInflater.from(context).inflate(
				R.layout.wizard_folder_pick_list, null);

		LocalDirectoryBrowser directory = new LocalDirectoryBrowser(context);
		directoryAdapter = new FolderAdapter(context, R.layout.folder_adapter_row,
				directory.listFiles());
		directoryAdapter
				.setDoneButton((Button) view.findViewById(R.id.wizard_done_button));
		directoryAdapter.setDirectoryBrowser(directory);

		ListView folderList = (ListView) view.findViewById(R.id.wizard_folder_list);
		folderList.setAdapter(directoryAdapter);
		directoryAdapter.notifyDataSetChanged();

		setupDoneButton(view);
		wizardView.addPage(view);
		wizardView.setNavButtonStateOnPage(1, true, WizardView.LAST_PAGE);
		return view;
	}
	
	public void saveSettings() {
		SharedPreferences appSettings = PreferenceManager
				.getDefaultSharedPreferences(context);
		SharedPreferences.Editor editor = appSettings.edit();
		
		editor.putString("syncSource", "sdcard");
		editor.putString("indexFilePath",
				directoryAdapter.getCheckedDirectory());
		
		editor.commit();
	}
}
