package com.matburt.mobileorg.gui.wizard;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.RadioButton;
import android.widget.RadioGroup;

import com.matburt.mobileorg.OrgNodeListActivity;
import com.matburt.mobileorg.gui.wizard.wizards.DropboxWizard;
import com.matburt.mobileorg.gui.wizard.wizards.SDCardWizard;
import com.matburt.mobileorg.gui.wizard.wizards.SSHWizard;
import com.matburt.mobileorg.gui.wizard.wizards.UbuntuOneWizard;
import com.matburt.mobileorg.gui.wizard.wizards.WebDAVWizard;
import com.matburt.mobileorg.R;

public class WizardActivity extends AppCompatActivity {

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.wizard_choose_synchronizer);

		final RadioGroup syncGroup = (RadioGroup) findViewById(R.id.sync_group);

		SharedPreferences srcPrefs = PreferenceManager
				.getDefaultSharedPreferences(this);
		String syncSource = srcPrefs.getString("syncSource", "nullSync");
		Log.v("sync", "source : "+ syncSource);
		int id = getResources().getIdentifier(syncSource, "id", getPackageName() );
		RadioButton radioButton = (RadioButton) findViewById(id);
		if(radioButton != null) radioButton.setChecked(true);

		findViewById(R.id.ok).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				int id = syncGroup.getCheckedRadioButtonId();
				if(id < 0) return;

				// Saving selected synchronizer
				SharedPreferences appSettings = PreferenceManager
						.getDefaultSharedPreferences(WizardActivity.this);
				SharedPreferences.Editor editor = appSettings.edit();
				String syncName = getResources().getResourceEntryName(id);
				editor.putString("syncSource", syncName);
				editor.apply();
				switch (syncName) {
					case "webdav":
						startActivity(new Intent(WizardActivity.this, WebDAVWizard.class));
						break;
					case "dropbox":
						startActivity(new Intent(WizardActivity.this, DropboxWizard.class));
						break;
					case "ubuntuone":
						startActivity(new Intent(WizardActivity.this, UbuntuOneWizard.class));
						break;
					case "sdcard":
						startActivity(new Intent(WizardActivity.this, SDCardWizard.class));
						break;
					case "ssh":
						startActivity(new Intent(WizardActivity.this, SSHWizard.class));
						break;
					default:
						startActivity(new Intent(WizardActivity.this, OrgNodeListActivity.class));
						break;
				}
			}
		});

	}
}
