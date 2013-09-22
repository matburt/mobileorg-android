package com.matburt.mobileorg.Gui.Wizard.Wizards;

import com.matburt.mobileorg.R;
import com.matburt.mobileorg.Gui.Wizard.WizardView;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;

public class NullWizard extends Wizard {

	public NullWizard(WizardView wizardView, Context context) {
		super(wizardView, context);
	}
	

	@Override
	public void setupFirstPage() {
		createNullConfig();
	}
	
	public void createNullConfig() {		
		View view = LayoutInflater.from(context).inflate(
				R.layout.wizard_null, null);

		setupDoneButton(view);
		wizardView.addPage(view);
		wizardView.setNavButtonStateOnPage(1, true, WizardView.LAST_PAGE);
		wizardView.enablePage(1);
	}
	
	@Override
	public void saveSettings() {
		SharedPreferences appSettings = PreferenceManager
				.getDefaultSharedPreferences(context);
		SharedPreferences.Editor editor = appSettings.edit();
		
		editor.putString("syncSource", "null");
		editor.commit();
	}
}
