package com.matburt.mobileorg.gui.wizard.wizards;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.HandlerThread;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;

import com.matburt.mobileorg.gui.wizard.FolderAdapter;
import com.matburt.mobileorg.R;
import com.matburt.mobileorg.synchronizers.UbuntuOneSynchronizer;

public class UbuntuOneWizard extends AppCompatActivity {
	
	private EditText ubuntuoneEmail;
	private EditText ubuntuonePass;
	private Button loginButton;
	
	private boolean isLoggedIn = false;

	private FolderAdapter directoryAdapter;


	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.wizard_ubuntuone);
		
		ubuntuoneEmail = (EditText) findViewById(R.id.wizard_ubuntu_email);
		ubuntuonePass = (EditText) findViewById(R.id.wizard_ubuntu_password);
		loginButton = (Button) findViewById(R.id.wizard_ubuntu_login_button);
		loginButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				if (isLoggedIn == false) {
					loginUbuntuOne();
				}
			}
		});
	}


	private void loginUbuntuOne() {
		final UbuntuOneSynchronizer uos = new UbuntuOneSynchronizer(
				this);
		uos.username = ubuntuoneEmail.getText().toString();
		uos.password = ubuntuonePass.getText().toString();

		// move this into another thread, so we don't get an ANR if the network
		// is unavailable
		Thread uiThread = new HandlerThread("UIHandler");
		uiThread.start();
//		uiHandler = new UIHandler(((HandlerThread) uiThread).getLooper());
//		Toast.makeText(this, "Logging in, please wait", Toast.LENGTH_SHORT).show();
//		if (uos.login()) {
//			showToastRemote("Login Successfull");
//			loginButton.setEnabled(false);
//			wizardView.enablePage(1);
//			uos.getBaseUser();
//			createUbuntuOneList();
//		} else {
//			showToastRemote("Login Failed");
//		}
	}


	private View createUbuntuOneList() {
		View view = LayoutInflater.from(this).inflate(
				R.layout.wizard_folder_pick_list, null);

		UbuntuOneSynchronizer uos = new UbuntuOneSynchronizer(this);
		uos.getBaseUser();
		UbuntuOneDirectoryBrowser directory = new UbuntuOneDirectoryBrowser(this, uos);

		directoryAdapter = new FolderAdapter(this, R.layout.folder_adapter_row,
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
		return view;
	}
	
	public void saveSettings() {
		SharedPreferences appSettings = PreferenceManager
				.getDefaultSharedPreferences(this);
		SharedPreferences.Editor editor = appSettings.edit();
		
		editor.putString("syncSource", "ubuntu");
		editor.putString("ubuntuOnePath",
				directoryAdapter.getCheckedDirectory() + "/");
		
		editor.apply();
		this.finish();
	}
}
