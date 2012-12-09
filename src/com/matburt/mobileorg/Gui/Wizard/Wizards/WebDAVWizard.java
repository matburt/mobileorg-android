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

import com.matburt.mobileorg.R;
import com.matburt.mobileorg.Gui.Wizard.WizardView;
import com.matburt.mobileorg.Synchronizers.WebDAVSynchronizer;

public class WebDAVWizard extends Wizard {

	private EditText webdavUser;
	private EditText webdavPass;
	private EditText webdavUrl;

	public WebDAVWizard(WizardView wizardView, Context context) {
		super(wizardView, context);
	}
	

	@Override
	public void setupFirstPage() {
		createWebDAVConfig();
	}

	public View createWebDAVConfig() {
		wizardView.removePagesAfter(1);
		
		View view = LayoutInflater.from(context).inflate(
				R.layout.wizard_webdav, null);
		
		webdavUser = (EditText) view
				.findViewById(R.id.wizard_webdav_username);
		webdavPass = (EditText) view
				.findViewById(R.id.wizard_webdav_password);
		webdavUrl = (EditText) view.findViewById(R.id.wizard_webdav_url);
		Button webdavLoginButton = (Button) view
				.findViewById(R.id.wizard_webdav_login_button);
		webdavLoginButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				loginWebdav();
			}
		});
		
		setupDoneButton(view);
		wizardView.addPage(view);
		wizardView.setNavButtonStateOnPage(1, true, WizardView.LAST_PAGE);
		wizardView.enablePage(1);
		return view;
	}
	


	public void loginWebdav() {
		final String urlActual = webdavUrl.getText().toString();
		final String passActual = webdavPass.getText().toString();
		final String userActual = webdavUser.getText().toString();
		progress.show();

		Thread uiThread = new HandlerThread("UIHandler");
		uiThread.start();
		uiHandler = new UIHandler(((HandlerThread) uiThread).getLooper());

		Thread loginThread = new Thread() {
			public void run() {
				WebDAVSynchronizer wds = new WebDAVSynchronizer(context);
				String extra = wds.testConnection(urlActual, userActual,
						passActual);
				if (extra != null) {
					showToastRemote("Login failed: " + extra);
					return;
				}
				showToastRemote("Login succeeded");
			}
		};
		loginThread.start();
	}
	
	public void saveSettings() {
		SharedPreferences appSettings = PreferenceManager
				.getDefaultSharedPreferences(context);
		SharedPreferences.Editor editor = appSettings.edit();
		
		editor.putString("syncSource", "webdav");
		
		editor.putString("webUrl", webdavUrl.getText().toString());
		editor.putString("webPass", webdavPass.getText().toString());
		editor.putString("webUser", webdavUser.getText().toString());
		
		editor.commit();
	}
}
