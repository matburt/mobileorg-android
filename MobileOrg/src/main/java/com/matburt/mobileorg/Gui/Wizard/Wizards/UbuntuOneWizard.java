package com.matburt.mobileorg.Gui.Wizard.Wizards;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.HandlerThread;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import com.matburt.mobileorg.R;
import com.matburt.mobileorg.Gui.Wizard.FolderAdapter;
import com.matburt.mobileorg.Gui.Wizard.WizardView;
import com.matburt.mobileorg.Synchronizers.UbuntuOneSynchronizer;

public class UbuntuOneWizard extends Wizard {
	
	private EditText ubuntuoneEmail;
	private EditText ubuntuonePass;
	private Button loginButton;
	
	private boolean isLoggedIn = false;

	private FolderAdapter directoryAdapter;

	public UbuntuOneWizard(WizardView wizardView, Context context) {
		super(wizardView, context);
	}
	

	@Override
	public void setupFirstPage() {
		createUbuntuLogin();
	}

	private View createUbuntuLogin() {		
		View view = LayoutInflater.from(context).inflate(
				R.layout.wizard_ubuntuone, null);
		
		ubuntuoneEmail = (EditText) view.findViewById(R.id.wizard_ubuntu_email);
		ubuntuonePass = (EditText) view
				.findViewById(R.id.wizard_ubuntu_password);
		loginButton = (Button) view
				.findViewById(R.id.wizard_ubuntu_login_button);
		loginButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				if (isLoggedIn == false) {
					loginUbuntuOne();
				}
			}
		});
		
		wizardView.addPage(view);
		wizardView.setNavButtonStateOnPage(1, true, WizardView.MIDDLE_PAGE);
		wizardView.disableAllNextActions(1);
		return view;
	}


	private void loginUbuntuOne() {
		final UbuntuOneSynchronizer uos = new UbuntuOneSynchronizer(
				(Context) context);
		uos.username = ubuntuoneEmail.getText().toString();
		uos.password = ubuntuonePass.getText().toString();

		// move this into another thread, so we don't get an ANR if the network
		// is unavailable
		Thread uiThread = new HandlerThread("UIHandler");
		uiThread.start();
		uiHandler = new UIHandler(((HandlerThread) uiThread).getLooper());
		Toast.makeText(context, "Logging in, please wait", Toast.LENGTH_SHORT).show();
		if (uos.login()) {
			showToastRemote("Login Successfull");
			loginButton.setEnabled(false);
			wizardView.enablePage(1);
			uos.getBaseUser();
			createUbuntuOneList();
		} else {
			showToastRemote("Login Failed");
		}
	}


	private View createUbuntuOneList() {
		View view = LayoutInflater.from(context).inflate(
				R.layout.wizard_folder_pick_list, null);

		UbuntuOneSynchronizer uos = new UbuntuOneSynchronizer(context);
		uos.getBaseUser();
		UbuntuOneDirectoryBrowser directory = new UbuntuOneDirectoryBrowser(context, uos);

		directoryAdapter = new FolderAdapter(context, R.layout.folder_adapter_row,
				directory.listFiles());
		directoryAdapter
				.setDoneButton((Button) view.findViewById(R.id.wizard_done_button));

		directoryAdapter.setDirectoryBrowser(directory);

		ListView folderList = (ListView) view.findViewById(R.id.wizard_folder_list);
		folderList.setAdapter(directoryAdapter);
		directoryAdapter.notifyDataSetChanged();
		// debug
		// TODO Technically, this should be an async task app may crash
		// when list of root items is very long and network connection
		// is slow

		setupDoneButton(view);
		wizardView.addPage(view);
		wizardView.enablePage(1);
		wizardView.setNavButtonStateOnPage(2, true, WizardView.LAST_PAGE);
		return view;
	}
	
	public void saveSettings() {
		SharedPreferences appSettings = PreferenceManager
				.getDefaultSharedPreferences(context);
		SharedPreferences.Editor editor = appSettings.edit();
		
		editor.putString("syncSource", "ubuntu");
		editor.putString("ubuntuOnePath",
				directoryAdapter.getCheckedDirectory() + "/");
		
		editor.commit();
	}
}
