package com.matburt.mobileorg.Settings;
import java.util.HashMap;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.Preference;
import android.preference.PreferenceManager;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.matburt.mobileorg.R;
import com.matburt.mobileorg.Settings.Synchronizers.SDCardSettingsActivity;
import com.matburt.mobileorg.Settings.Synchronizers.ScpSettingsActivity;
import com.matburt.mobileorg.Settings.Synchronizers.UbuntuOneSettingsActivity;
import com.matburt.mobileorg.Settings.Synchronizers.WebDAVSettingsActivity;

public class SynchronizerPreferences extends Preference {
	private TextView mDetails;
	private Activity mParentActivity;

	public static HashMap<String,Intent> syncIntents = new HashMap<String,Intent>();
	public SynchronizerPreferences(Context context) {
		super(context);
	}

	public SynchronizerPreferences(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public SynchronizerPreferences(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
	}

	public void setParentActivity(Activity activity) {
		this.mParentActivity = activity;
	}

	@Override
	protected View onCreateView(ViewGroup parent){

		LinearLayout layout = new LinearLayout(getContext());
		LinearLayout.LayoutParams params1 = new LinearLayout.LayoutParams(
				LinearLayout.LayoutParams.WRAP_CONTENT,
				LinearLayout.LayoutParams.WRAP_CONTENT);
		params1.weight  = 1.0f;
		layout.setPadding(15, 10, 10, 10);
		layout.setOrientation(LinearLayout.VERTICAL);
		TextView view = new TextView(getContext());
		view.setText(R.string.configure_synchronizer_settings);
		view.setLayoutParams(params1);
		view.setTextAppearance(getContext(), android.R.style.TextAppearance_Large);
		layout.addView(view);

		mDetails = new TextView(getContext());
		mDetails.setText(getSyncPreferenceString(PreferenceManager.getDefaultSharedPreferences(getContext())));
		mDetails.setTextAppearance(getContext(), android.R.style.TextAppearance_Small);
		layout.addView(mDetails);

		this.setOnPreferenceClickListener(new OnPreferenceClickListener() {
			public boolean onPreferenceClick(Preference arg0) {
				SharedPreferences appSettings = PreferenceManager.getDefaultSharedPreferences(getContext());
				String synchroMode = appSettings.getString("syncSource","");
				if(syncIntents.containsKey(synchroMode))
				{
					mParentActivity.startActivityForResult(syncIntents.get(synchroMode), SettingsActivity.SYNCHRONIZER_PREFERENCES);
				}
				else {
					//throw new ReportableError(R.string.error_synchronizer_type_unknown,
					//                          synchroMode);
				}
				return true;
			}
		});

		layout.setId(android.R.id.widget_frame);
		return layout; 
	}

	private String getSyncPreferenceString(SharedPreferences sharedPreferences) {
		String syncSource = sharedPreferences.getString(SettingsActivity.KEY_SYNC_SOURCE, "");
		// Summarize based on KEY_SYNC_SOURCE
		if (syncSource.equals("scp")) {
			String s = "";
			if (sharedPreferences.getString(ScpSettingsActivity.KEY_SCP_USER, null) != null) {
				s = sharedPreferences.getString(ScpSettingsActivity.KEY_SCP_USER, null)
						+ "@";
			}
			if (sharedPreferences.getString(ScpSettingsActivity.KEY_SCP_HOST, null) != null) {
				s += sharedPreferences.getString(ScpSettingsActivity.KEY_SCP_HOST, null);
			}
			if (sharedPreferences.getString(ScpSettingsActivity.KEY_SCP_PORT, null) != null) {
				s += ":" + sharedPreferences.getString(ScpSettingsActivity.KEY_SCP_PORT, null);
			}
			if (sharedPreferences.getString(ScpSettingsActivity.KEY_SCP_PATH, null) != null) {
				s += sharedPreferences.getString(ScpSettingsActivity.KEY_SCP_PATH, null);
			}
			return s;
		} else if (syncSource.equals("sdcard")) {
			String value = sharedPreferences.getString(SDCardSettingsActivity.KEY_INDEX_FILE_PATH, "");
			return value;
		} else if (syncSource.equals("ubuntu")) {
			return sharedPreferences.getString(UbuntuOneSettingsActivity.KEY_UBUNTUONE_PATH, "");
		} else if (syncSource.equals("webdav")) {
			return sharedPreferences.getString(WebDAVSettingsActivity.KEY_WEB_URL, "");
		}
		return null;
	}

	public void setPreferenceSummary() {
		mDetails.setText(getSyncPreferenceString(PreferenceManager.getDefaultSharedPreferences(getContext())));
	}
}
