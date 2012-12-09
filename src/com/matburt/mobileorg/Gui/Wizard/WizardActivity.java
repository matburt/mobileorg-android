package com.matburt.mobileorg.Gui.Wizard;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.RadioButton;
import android.widget.RadioGroup;

import com.matburt.mobileorg.R;
import com.matburt.mobileorg.Gui.Wizard.Wizards.SSHWizard;
import com.matburt.mobileorg.Gui.Wizard.Wizards.Wizard;
import com.matburt.mobileorg.util.OrgUtils;

public class WizardActivity extends Activity implements RadioGroup.OnCheckedChangeListener {

	private WizardView wizardView;
	private Wizard activeWizard;

	private int syncWebDav, syncDropBox, syncUbuntuOne, syncSdCard, syncNull, syncSSH;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		OrgUtils.setTheme(this);
		super.onCreate(savedInstanceState);
		setContentView(R.layout.wizard);
		
		wizardView = (WizardView) findViewById(R.id.wizard_parent);
		// when wizard first starts can't go to next page
		wizardView.setNavButtonStateOnPage(0, false, WizardView.FIRST_PAGE);

		RadioGroup syncGroup = (RadioGroup) findViewById(R.id.sync_group);
		syncGroup.clearCheck();
		syncGroup.setOnCheckedChangeListener(this);

		syncWebDav = ((RadioButton) findViewById(R.id.sync_webdav)).getId();
		syncDropBox = ((RadioButton) findViewById(R.id.sync_dropbox)).getId();
		syncUbuntuOne = ((RadioButton) findViewById(R.id.sync_ubuntuone))
				.getId();
		syncSdCard = ((RadioButton) findViewById(R.id.sync_sdcard)).getId();
		syncNull = ((RadioButton) findViewById(R.id.sync_null)).getId();
		syncSSH = ((RadioButton) findViewById(R.id.sync_ssh)).getId();
	}


	@Override
	protected void onPause() {
		super.onPause();
		wizardView.saveCurrentPage();
	}

	@Override
	protected void onResume() {
		super.onResume();
		
		if(activeWizard != null)
			activeWizard.refresh();
	}
	

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == SSHWizard.SSH_CHOOSE_PUB) {
			if (resultCode == RESULT_OK) {
				String filePath = data.getData().getPath();
				
				if (activeWizard instanceof SSHWizard)
					((SSHWizard) activeWizard).setPubFile(filePath);
			}
		}
	}

	@Override
	public void onCheckedChanged(RadioGroup arg, int checkedId) {
		this.activeWizard = Wizard.getWizard(getWizardType(checkedId), wizardView,
				this);
		// allow scrolling to next page
		wizardView.enablePage(0);
	}
	
	public Wizard.TYPE getWizardType(int checkedId) {
		if (checkedId == syncWebDav) {
			return Wizard.TYPE.WebDAV;
		} else if (checkedId == syncDropBox) {
			return Wizard.TYPE.Dropbox;
		} else if (checkedId == syncUbuntuOne) {
			return Wizard.TYPE.Ubuntu;
		} else if (checkedId == syncSdCard) {
			return Wizard.TYPE.SDCard;
		} else if (checkedId == syncSSH) {
			return Wizard.TYPE.SSH;
		} else if (checkedId == syncNull) {
			return Wizard.TYPE.Null;
		}
		
		return Wizard.TYPE.Null;
	}
}
