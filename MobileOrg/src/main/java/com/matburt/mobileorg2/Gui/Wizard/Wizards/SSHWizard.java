package com.matburt.mobileorg2.Gui.Wizard.Wizards;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.matburt.mobileorg2.Gui.Wizard.WizardView;
import com.matburt.mobileorg2.R;
import com.matburt.mobileorg2.Synchronizers.AuthData;
import com.matburt.mobileorg2.Synchronizers.JGitWrapper;

public class SSHWizard extends Wizard {

	public static final int SSH_CHOOSE_PUB = 1;
	ProgressDialog mProgressDialog;
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

		setupDoneButton(view);
		wizardView.addPage(view);
		wizardView.setNavButtonStateOnPage(1, true, WizardView.LAST_PAGE);
		wizardView.enablePage(1);
		return view;
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
		final String pathActual = sshPath.getText().toString();
		final String userActual = sshUser.getText().toString();
		final String hostActual = sshHost.getText().toString();
		String portActual = sshPort.getText().toString();
		if(portActual.equals("")) portActual = "22";

		SharedPreferences appSettings = PreferenceManager
				.getDefaultSharedPreferences(context);


		SharedPreferences.Editor editor = appSettings.edit();

		editor.putString("syncSource", "scp");

		editor.putString("scpPath", pathActual);
		editor.putString("scpUser", userActual);
		editor.putString("scpHost", hostActual);
		editor.putString("scpPort", portActual);
		editor.putString("scpPubFile", sshPubFileActual.getText().toString());
		editor.putString("scpPass", sshPass.getText().toString());
		editor.apply();


		AuthData.getInstance(context).setPassword(sshPass.getText().toString());
		Log.v("git","host0 : "+hostActual);

		JGitWrapper.CloneGitRepoTask task = new JGitWrapper.CloneGitRepoTask(context);
		task.execute(pathActual, sshPass.getText().toString(), userActual, hostActual, portActual);
	}



}
