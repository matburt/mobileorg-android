package com.matburt.mobileorg.gui.wizard.wizards;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import com.matburt.mobileorg.R;

public class WebDAVWizard extends AppCompatActivity {

	private EditText webdavUser;
	private EditText webdavPass;
	private EditText webdavUrl;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.wizard_webdav);
		
		webdavUser = (EditText) findViewById(R.id.wizard_webdav_username);
		webdavPass = (EditText) findViewById(R.id.wizard_webdav_password);
		webdavUrl = (EditText) findViewById(R.id.wizard_webdav_url);
		Button webdavLoginButton = (Button) findViewById(R.id.wizard_webdav_login_button);
		webdavLoginButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
//				loginWebdav();
			}
		});
	}
	


//	public void loginWebdav() {
////		Log.v("webdav", "login");
//		final String urlActual = webdavUrl.getText().toString();
//		final String passActual = webdavPass.getText().toString();
//		final String userActual = webdavUser.getText().toString();
//		progress.show();
//
//		Thread uiThread = new HandlerThread("UIHandler");
//		uiThread.start();
//		uiHandler = new UIHandler(((HandlerThread) uiThread).getLooper());
//
//		Thread loginThread = new Thread() {
//			public void run() {
//				WebDAVSynchronizer wds = new WebDAVSynchronizer(context);
//				String extra = wds.testConnection(urlActual, userActual,
//						passActual);
//				if (extra != null) {
//					showToastRemote("Login failed: " + extra);
//					return;
//				}
//				showToastRemote("Login succeeded");
//			}
//		};
//		loginThread.start();
//	}
//
//	public void saveSettings() {
//		SharedPreferences appSettings = PreferenceManager
//				.getDefaultSharedPreferences(context);
//		SharedPreferences.Editor editor = appSettings.edit();
//
//		editor.putString("syncSource", "webdav");
//
//		editor.putString("webUrl", webdavUrl.getText().toString());
//		editor.putString("webPass", webdavPass.getText().toString());
//		editor.putString("webUser", webdavUser.getText().toString());
//
//		editor.apply();
//		((Activity) context).finish();
//	}
}
