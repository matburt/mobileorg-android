package com.matburt.mobileorg.Gui.Wizard.Wizards;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.HandlerThread;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.matburt.mobileorg.R;
import com.matburt.mobileorg.Gui.Wizard.WizardView;
import com.matburt.mobileorg.Synchronizers.SSHSynchronizer;

public class SSHWizard extends Wizard {
	public static final int SSH_CHOOSE_PUB = 1;
	
	private EditText sshUser;
	private EditText sshPass;
	private EditText sshPath;
	private EditText sshHost;
	private EditText sshPort;
	
	private TextView sshPubFileActual;

	public SSHWizard(WizardView wizardView, Context context) {
		super(wizardView, context);
	}
	

	@Override
	public void setupFirstPage() {
		createSSHConfig();
	}
	
	public View createSSHConfig() {				
		View view = LayoutInflater.from(context).inflate(
				R.layout.wizard_ssh, null);
		
		sshUser = (EditText) view.findViewById(R.id.wizard_ssh_username);
		sshPass = (EditText) view.findViewById(R.id.wizard_ssh_password);
		sshPath = (EditText) view.findViewById(R.id.wizard_ssh_path);
		sshHost = (EditText) view.findViewById(R.id.wizard_ssh_host);
		sshPort = (EditText) view.findViewById(R.id.wizard_ssh_port);
		sshPubFileActual = (TextView) view
				.findViewById(R.id.wizard_ssh_pub_file_actual);
		Button sshPubFileSelect = (Button) view
				.findViewById(R.id.wizard_ssh_choose_pub_file);
		sshPubFileSelect.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				try {
					Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
					intent.setType("file/*");
					((Activity)context).startActivityForResult(intent, SSH_CHOOSE_PUB);
				} catch (Exception e) {
				}
			}
		});
		
		loadSettings();
		
		Button webdavLoginButton = (Button) view
				.findViewById(R.id.wizard_ssh_login_button);
		webdavLoginButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				loginSSH();
			}
		});
		
		setupDoneButton(view);
		wizardView.addPage(view);
		wizardView.setNavButtonStateOnPage(1, true, WizardView.LAST_PAGE);
		wizardView.enablePage(1);
		return view;
	}
	
	private void loginSSH() {
		final String pathActual = sshPath.getText().toString();
		final String passActual = sshPass.getText().toString();
		final String userActual = sshUser.getText().toString();
		final String hostActual = sshHost.getText().toString();
        final String pubFileActual = sshPubFileActual.getText().toString();
		String portNumGiven = sshPort.getText().toString();
		int portNum;
		if (portNumGiven.trim().equals("")) {
			portNum = 22;
		} else {
			portNum = Integer.parseInt(portNumGiven);
		}
		final int portActual = portNum;

		progress.show();

		Thread uiThread = new HandlerThread("UIHandler");
		uiThread.start();
		uiHandler = new UIHandler(((HandlerThread) uiThread).getLooper());

		Thread loginThread = new Thread() {
			public void run() {
				SSHSynchronizer sds = new SSHSynchronizer(context);
				String extra = sds.testConnection(pathActual, userActual,
						passActual, hostActual, portActual, pubFileActual);
				if (extra != null) {
					showToastRemote("Login failed: " + extra);
					return;
				}
				showToastRemote("Login succeeded");
			}
		};
		loginThread.start();
	}
	
	public void setPubFile(String pubfile) {
		this.sshPubFileActual.setText(pubfile);
	}
	
	private void loadSettings() {
		SharedPreferences appSettings = PreferenceManager
				.getDefaultSharedPreferences(context);
		sshPath.setText(appSettings.getString("scpPath", ""));
		sshUser.setText(appSettings.getString("scpUser", ""));
		sshPass.setText(appSettings.getString("scpPass", ""));
		sshHost.setText(appSettings.getString("scpHost", ""));
		sshPort.setText(appSettings.getString("scpPort", ""));
		sshPubFileActual.setText(appSettings.getString("scpPubFile", ""));
	}
	
	public void saveSettings() {
		SharedPreferences appSettings = PreferenceManager
				.getDefaultSharedPreferences(context);
		SharedPreferences.Editor editor = appSettings.edit();
		
		editor.putString("syncSource", "scp");

		editor.putString("scpPath", sshPath.getText().toString());
		editor.putString("scpUser", sshUser.getText().toString());
		editor.putString("scpPass", sshPass.getText().toString());
		editor.putString("scpHost", sshHost.getText().toString());
		if (sshPort.getText().toString().trim().equals("")) {
			editor.putString("scpPort", "22");
		} else {
			editor.putString("scpPort", sshPort.getText().toString());
		}
		
		editor.putString("scpPubFile", sshPubFileActual.getText().toString());
		
		editor.commit();
	}
}
